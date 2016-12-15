/*
 *
 * Copyright 2008,2009 Newcastle University
 *
 * This file is part of Workcraft.
 *
 * Workcraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Workcraft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.workcraft;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.workcraft.dom.Model;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.FormatException;
import org.workcraft.exceptions.ModelValidationException;
import org.workcraft.exceptions.OperationCancelledException;
import org.workcraft.exceptions.PluginInstantiationException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.gui.DesktopApi;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.plugins.serialisation.XMLModelDeserialiser;
import org.workcraft.plugins.serialisation.XMLModelSerialiser;
import org.workcraft.serialisation.DeserialisationResult;
import org.workcraft.serialisation.Format;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.ReferenceProducer;
import org.workcraft.tasks.DefaultTaskManager;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.ProgressMonitorArray;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Task;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.Commands;
import org.workcraft.util.DataAccumulator;
import org.workcraft.util.Export;
import org.workcraft.util.FileUtils;
import org.workcraft.util.Import;
import org.workcraft.util.LogUtils;
import org.workcraft.util.XmlUtil;
import org.workcraft.workspace.Memento;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;
import org.xml.sax.SAXException;

public final class Framework {
    private static final String SETTINGS_DIRECTORY_NAME = "workcraft";
    private static final String CONFIG_FILE_NAME = "config.xml";
    private static final String PLUGINS_FILE_NAME = "plugins.xml";
    private static final String UILAYOUT_FILE_NAME = "uilayout.xml";

    public static final String SETTINGS_DIRECTORY_PATH = DesktopApi.getConfigPath() + File.separator + SETTINGS_DIRECTORY_NAME;
    public static final String CONFIG_FILE_PATH = SETTINGS_DIRECTORY_PATH + File.separator + CONFIG_FILE_NAME;
    public static final String PLUGINS_FILE_PATH = SETTINGS_DIRECTORY_PATH + File.separator + PLUGINS_FILE_NAME;
    public static final String UILAYOUT_FILE_PATH = SETTINGS_DIRECTORY_PATH + File.separator + UILAYOUT_FILE_NAME;

    private static Framework instance = null;

    class ExecuteScriptAction implements ContextAction {
        private final String script;
        private final Scriptable scope;

        ExecuteScriptAction(String script, Scriptable scope) {
            this.script = script;
            this.scope = scope;
        }

        @Override
        public Object run(Context cx) {
            return cx.evaluateString(scope, script, "<string>", 1, null);
        }
    }

    class ExecuteCompiledScriptAction implements ContextAction {
        private final Script script;
        private final Scriptable scope;

        ExecuteCompiledScriptAction(Script script, Scriptable scope) {
            this.script = script;
            this.scope = scope;
        }

        @Override
        public Object run(Context cx) {
            return script.exec(cx, scope);
        }
    }

    class CompileScriptFromReaderAction implements ContextAction {
        private final String sourceName;
        private final BufferedReader reader;

        CompileScriptFromReaderAction(BufferedReader reader, String sourceName) {
            this.sourceName = sourceName;
            this.reader = reader;
        }

        @Override
        public Object run(Context cx) {
            try {
                return cx.compileReader(reader, sourceName, 1, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class CompileScriptAction implements ContextAction {
        private final String source, sourceName;

        CompileScriptAction(String source, String sourceName) {
            this.source = source;
            this.sourceName = sourceName;
        }

        @Override
        public Object run(Context cx) {
            return cx.compileString(source, sourceName, 1, null);
        }
    }

    class SetArgs implements ContextAction {
        Object[] args;

        public void setArgs(Object[] args) {
            this.args = args;
        }

        @Override
        public Object run(Context cx) {
            Object scriptable = Context.javaToJS(args, systemScope);
            ScriptableObject.putProperty(systemScope, "args", scriptable);
            systemScope.setAttributes("args", ScriptableObject.READONLY);
            return null;

        }
    }

    private final PluginManager pluginManager;
    private final TaskManager taskManager;
    private final CompatibilityManager compatibilityManager;
    private final Workspace workspace;

    private Config config;
    private ScriptableObject systemScope;
    private ScriptableObject globalScope;

    private boolean inGuiMode = false;
    private boolean shutdownRequested = false;
    private boolean guiRestartRequested = false;
    private final ContextFactory contextFactory = new ContextFactory();
    private File workingDirectory = null;
    private MainWindow mainWindow;
    public Memento clipboard;

    private Framework() {
        pluginManager = new PluginManager();
        taskManager = new DefaultTaskManager() {
            @Override
            public <T> Result<? extends T> execute(Task<T> task, String description, ProgressMonitor<? super T> observer) {
                if (SwingUtilities.isEventDispatchThread()) {
                    OperationCancelDialog<T> cancelDialog = new OperationCancelDialog<>(mainWindow, description);

                    ProgressMonitorArray<T> observers = new ProgressMonitorArray<>();
                    if (observer != null) {
                        observers.add(observer);
                    }
                    observers.add(cancelDialog);

                    this.queue(task, description, observers);
                    cancelDialog.setVisible(true);
                    return cancelDialog.result;
                } else {
                    return super.execute(task, description, observer);
                }
            }
        };
        compatibilityManager = new CompatibilityManager();
        config = new Config();
        workspace = new Workspace();
    }

    public static Framework getInstance() {
        if (instance == null) {
            instance = new Framework();
        }
        return instance;
    }

    private void loadConfigPlugins() {
        for (PluginInfo<? extends Settings> info : pluginManager.getPlugins(Settings.class)) {
            info.getSingleton().load(config);
        }
    }

    private void saveConfigPlugins() {
        for (PluginInfo<? extends Settings> info : pluginManager.getPlugins(Settings.class)) {
            info.getSingleton().save(config);
        }
    }

    public void resetConfig() {
        config = new Config();
        loadConfigPlugins();
    }

    public void loadConfig() {
        File file = new File(CONFIG_FILE_PATH);
        LogUtils.logMessageLine("Loading global preferences from " + file.getAbsolutePath());
        config.load(file);
        loadConfigPlugins();
    }

    public void saveConfig() {
        saveConfigPlugins();
        File file = new File(CONFIG_FILE_PATH);
        LogUtils.logMessageLine("Saving global preferences to " + file.getAbsolutePath());
        config.save(file);
    }

    public void setConfigCoreVar(String key, String value) {
        // Set a core variable, that does not require updating plugin settings.
        config.set(key, value);
    }

    public void setConfigVar(String key, String value) {
        setConfigCoreVar(key, value);
        // For consistency, update plugin settings.
        loadConfigPlugins();
    }

    public String getConfigCoreVar(String key) {
        // Get a core variable, that does not require flushing plugin settings.
        return config.get(key);
    }

    public String getConfigVar(String key) {
        // For consistency, flush plugin settings.
        saveConfigPlugins();
        return getConfigCoreVar(key);
    }

    public void initJavaScript() {
        LogUtils.logMessageLine("Initialising javascript...");
        contextFactory.call(new ContextAction() {
            @Override
            public Object run(Context cx) {
                ImporterTopLevel importer = new ImporterTopLevel();
                importer.initStandardObjects(cx, false);
                systemScope = importer;

                Object frameworkScriptable = Context.javaToJS(Framework.this, systemScope);
                ScriptableObject.putProperty(systemScope, "framework", frameworkScriptable);
                systemScope.setAttributes("framework", ScriptableObject.READONLY);

                globalScope = (ScriptableObject) cx.newObject(systemScope);
                globalScope.setPrototype(systemScope);
                globalScope.setParentScope(null);

                return null;
            }
        });
    }

    public void updateJavaScript(WorkspaceEntry we) {
        ScriptableObject jsGlobalScope = getJavaScriptGlobalScope();
        ModelEntry modelEntry = we.getModelEntry();

        VisualModel visualModel = modelEntry.getVisualModel();
        setJavaScriptProperty("visualModel", visualModel, jsGlobalScope, true);

        MathModel mathModel = modelEntry.getMathModel();
        setJavaScriptProperty("mathModel", mathModel, jsGlobalScope, true);
    }

    public ScriptableObject getJavaScriptGlobalScope() {
        return globalScope;
    }

    public void setJavaScriptProperty(final String name, final Object object,
            final ScriptableObject scope, final boolean readOnly) {

        deleteJavaScriptProperty(name, scope);

        contextFactory.call(new ContextAction() {
            @Override
            public Object run(Context arg0) {
                Object scriptable = Context.javaToJS(object, scope);
                ScriptableObject.putProperty(scope, name, scriptable);
                if (readOnly) {
                    scope.setAttributes(name, ScriptableObject.READONLY);
                }
                return scriptable;
            }
        });
    }

    public void deleteJavaScriptProperty(final String name, final ScriptableObject scope) {
        contextFactory.call(new ContextAction() {
            public Object run(Context arg0) {
                return ScriptableObject.deleteProperty(scope, name);
            }
        });
    }

    public Object execJavaScript(File file) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        return execJavaScript(compileJavaScript(reader, file.getPath()));
    }

    public Object execJavaScript(Script script) {
        return execJavaScript(script, globalScope);
    }

    static class JavascriptPassThroughException extends RuntimeException {
        private static final long serialVersionUID = 8906492547355596206L;
        private final String scriptTrace;

        JavascriptPassThroughException(Throwable wrapped, String scriptTrace) {
            super(wrapped);
            this.scriptTrace = scriptTrace;
        }

        @Override
        public String getMessage() {
            return String.format("Java %s was unhandled in javascript. \nJavascript stack trace: %s",
                    getCause().getClass().getSimpleName(), getScriptTrace());
        }

        public String getScriptTrace() {
            return scriptTrace;
        }
    }

    public Object execJavaScript(String script) {
        return execJavaScript(script, globalScope);
    }

    public Object execJavaScript(Script script, Scriptable scope) {
        return doContextAction(new ExecuteCompiledScriptAction(script, scope));
    }

    public Object execJavaScript(String script, Scriptable scope) {
        return doContextAction(new ExecuteScriptAction(script, scope));
    }

    private Object doContextAction(ContextAction action) {
        try {
            return contextFactory.call(action);
        } catch (JavaScriptException ex) {
            System.out.println("Script stack trace: " + ex.getScriptStackTrace());
            Object value = ex.getValue();
            if (value instanceof NativeJavaObject) {
                Object wrapped = ((NativeJavaObject) value).unwrap();
                if (wrapped instanceof Throwable) {
                    throw new JavascriptPassThroughException((Throwable) wrapped, ex.getScriptStackTrace());
                }
            }
            throw ex;
        }
    }

    public void execJavaScriptResource(String resourceName) throws IOException {
        execJavaScript(FileUtils.readAllTextFromSystemResource(resourceName));
    }

    public void execJavaScriptFile(String filePath) throws IOException {
        execJavaScript(FileUtils.readAllText(new File(filePath)), globalScope);
    }

    public Script compileJavaScript(String source, String sourceName) {
        return (Script) doContextAction(new CompileScriptAction(source, sourceName));
    }

    public Script compileJavaScript(BufferedReader source, String sourceName) {
        return (Script) doContextAction(new CompileScriptFromReaderAction(source, sourceName));
    }

    public void startGUI() {
        if (inGuiMode) {
            System.out.println("Already in GUI mode");
            return;
        }
        guiRestartRequested = false;
        System.out.println("Switching to GUI mode...");

        if (SwingUtilities.isEventDispatchThread()) {
            mainWindow = new MainWindow();
            mainWindow.startup();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        mainWindow = new MainWindow();
                        mainWindow.startup();
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        contextFactory.call(new ContextAction() {
            @Override
            public Object run(Context cx) {
                Object guiScriptable = Context.javaToJS(mainWindow, systemScope);
                ScriptableObject.putProperty(systemScope, "mainWindow", guiScriptable);
                systemScope.setAttributes("mainWindow", ScriptableObject.READONLY);
                return null;
            }
        });

        inGuiMode = true;
    }

    public void shutdownGUI() throws OperationCancelledException {
        if (isInGuiMode()) {
            mainWindow.shutdown();
            mainWindow.dispose();
            mainWindow = null;
            inGuiMode = false;

            contextFactory.call(new ContextAction() {
                @Override
                public Object run(Context cx) {
                    ScriptableObject.deleteProperty(systemScope, "mainWindow");
                    return null;
                }
            });
        }
    }

    public void shutdown() {
        shutdownRequested = true;
    }

    public boolean shutdownRequested() {
        return shutdownRequested;
    }

    public void abortShutdown() {
        shutdownRequested = false;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public CompatibilityManager getCompatibilityManager() {
        return compatibilityManager;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public boolean isInGuiMode() {
        return inGuiMode;
    }

    public WorkspaceEntry getWorkspaceEntry(ModelEntry me) {
        return getWorkspace().getWorkspaceEntry(me);
    }

    public void setArgs(List<String> args) {
        SetArgs setargs = new SetArgs();
        setargs.setArgs(args.toArray());
        contextFactory.call(setargs);
    }

    public ModelEntry loadModel(String path) throws DeserialisationException {
        File file = getFileByAbsoluteOrRelativePath(path);
        if (checkFileMessageLog(file, null)) {
            return loadModel(file);
        }
        return null;
    }

    public ModelEntry loadModel(File file) throws DeserialisationException {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayInputStream bis = compatibilityManager.process(fis);
            return loadModel(bis);
        } catch (FileNotFoundException e) {
            throw new DeserialisationException(e);
        }
    }

    public ModelEntry loadModel(InputStream is) throws DeserialisationException {
        try {
            // load meta data
            byte[] bi = DataAccumulator.loadStream(is);
            Document metaDoc = FrameworkUtils.loadMetaDoc(bi);
            ModelDescriptor descriptor = FrameworkUtils.loadMetaDescriptor(metaDoc);

            // load math model
            InputStream mathData = FrameworkUtils.getMathData(bi, metaDoc);
            XMLModelDeserialiser mathDeserialiser = new XMLModelDeserialiser(getPluginManager());
            DeserialisationResult mathResult = mathDeserialiser.deserialise(mathData, null, null);
            mathData.close();

            // load visual model (if present)
            InputStream visualData = FrameworkUtils.getVisualData(bi, metaDoc);
            if (visualData == null) {
                return new ModelEntry(descriptor, mathResult.model);
            }
            XMLModelDeserialiser visualDeserialiser = new XMLModelDeserialiser(getPluginManager());
            DeserialisationResult visualResult = visualDeserialiser.deserialise(visualData,
                    mathResult.references, mathResult.model);

            // load current level and selection
            if (visualResult.model instanceof VisualModel) {
                FrameworkUtils.loadVisualModelState(bi, (VisualModel) visualResult.model, visualResult.references);
            }
            return new ModelEntry(descriptor, visualResult.model);
        } catch (IOException | ParserConfigurationException | SAXException |
                InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new DeserialisationException(e);
        }
    }

    public ModelEntry loadModel(Memento memento) {
        try {
            return loadModel(memento.getStream());
        } catch (DeserialisationException e) {
            throw new RuntimeException(e);
        }
    }

    public ModelEntry loadModel(InputStream is1, InputStream is2) throws DeserialisationException {
        ModelEntry me1 = loadModel(is1);
        ModelEntry me2 = loadModel(is2);

        VisualModel vmodel1 = me1.getVisualModel();
        VisualModel vmodel2 = me2.getVisualModel();

        if (!me1.getDescriptor().getDisplayName().equals(me2.getDescriptor().getDisplayName())) {
            throw new DeserialisationException("Incompatible models cannot be merged");
        }

        Collection<Node> children = new HashSet<>(vmodel2.getRoot().getChildren());

        vmodel1.selectNone();
        vmodel1.reparent(vmodel1.getCurrentLevel(), vmodel2, vmodel2.getRoot(), null);
        vmodel1.select(children);

        // FIXME: Dirty hack to avoid any hanging observers (serialise and deserialise the model).
        Memento memo = saveModel(me1);
        return loadModel(memo);
    }

    public void saveModel(ModelEntry modelEntry, String path) throws SerialisationException {
        if (modelEntry == null) return;
        File file = getFileByAbsoluteOrRelativePath(path);
        saveModel(modelEntry, file);
    }

    public void saveModel(ModelEntry modelEntry, File file) throws SerialisationException {
        if (modelEntry == null) return;
        try {
            FileOutputStream stream = new FileOutputStream(file);
            saveModel(modelEntry, stream);
            stream.close();
        } catch (IOException e) {
            throw new SerialisationException(e);
        }
    }

    public void saveModel(ModelEntry modelEntry, OutputStream out) throws SerialisationException {
        Model model = modelEntry.getModel();
        VisualModel visualModel = (model instanceof VisualModel) ? (VisualModel) model : null;
        Model mathModel = (visualModel == null) ? model : visualModel.getMathModel();
        ZipOutputStream zos = new ZipOutputStream(out);
        try {
            ModelSerialiser mathSerialiser = new XMLModelSerialiser(getPluginManager());
            // serialise math model
            String mathEntryName = "model" + mathSerialiser.getExtension();
            zos.putNextEntry(new ZipEntry(mathEntryName));
            ReferenceProducer refResolver = mathSerialiser.serialise(mathModel, zos, null);
            zos.closeEntry();
            // serialise visual model
            String visualEntryName = null;
            ModelSerialiser visualSerialiser = null;
            if (visualModel != null) {
                visualSerialiser = new XMLModelSerialiser(getPluginManager());

                visualEntryName = "visualModel" + visualSerialiser.getExtension();
                zos.putNextEntry(new ZipEntry(visualEntryName));
                ReferenceProducer visualRefs = visualSerialiser.serialise(visualModel, zos, refResolver);
                zos.closeEntry();
                // serialise visual model selection state
                zos.putNextEntry(new ZipEntry("state.xml"));
                FrameworkUtils.saveSelectionState(visualModel, zos, visualRefs);
                zos.closeEntry();
            }
            // serialise meta data
            zos.putNextEntry(new ZipEntry("meta"));
            Document metaDoc = XmlUtil.createDocument();
            Element metaRoot = metaDoc.createElement("workcraft-meta");
            metaDoc.appendChild(metaRoot);

            Element metaDescriptor = metaDoc.createElement("descriptor");
            metaDescriptor.setAttribute("class", modelEntry.getDescriptor().getClass().getCanonicalName());
            metaRoot.appendChild(metaDescriptor);

            Element mathElement = metaDoc.createElement("math");
            mathElement.setAttribute("entry-name", mathEntryName);
            mathElement.setAttribute("format-uuid", mathSerialiser.getFormatUUID().toString());
            metaRoot.appendChild(mathElement);

            if (visualModel != null) {
                Element visualElement = metaDoc.createElement("visual");
                visualElement.setAttribute("entry-name", visualEntryName);
                visualElement.setAttribute("format-uuid", visualSerialiser.getFormatUUID().toString());
                metaRoot.appendChild(visualElement);
            }

            XmlUtil.writeDocument(metaDoc, zos);
            zos.closeEntry();
            zos.close();
        } catch (ParserConfigurationException | IOException e) {
            throw new SerialisationException(e);
        }
    }

    public Memento saveModel(ModelEntry modelEntry) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            saveModel(modelEntry, os);
        } catch (SerialisationException e) {
            throw new RuntimeException(e);
        }
        return new Memento(os.toByteArray());
    }

    public ModelEntry importModel(String path) throws DeserialisationException {
        File file = getFileByAbsoluteOrRelativePath(path);
        return importModel(file);
    }

    public ModelEntry importModel(File file) throws DeserialisationException {
        try {
            final Importer importer = Import.chooseBestImporter(getPluginManager(), file);
            return Import.importFromFile(importer, file);
        } catch (IOException e) {
            throw new DeserialisationException(e);
        }
    }

    public void exportModel(ModelEntry modelEntry, String path, String format) throws SerialisationException {
        if (modelEntry == null) return;
        File file = getFileByAbsoluteOrRelativePath(path);
        UUID uuid = Format.getUUID(format);
        if (uuid == null) {
            LogUtils.logErrorLine("'" + format + "' format is not supported.");
        } else {
            exportModel(modelEntry, file, uuid);
        }
    }

    public void exportModel(ModelEntry modelEntry, File file, UUID uuid) throws SerialisationException {
        if (modelEntry == null) return;
        Exporter exporter = Export.chooseBestExporter(getPluginManager(), modelEntry.getMathModel(), uuid);
        if (exporter == null) {
            String modelName = modelEntry.getMathModel().getDisplayName();
            String formatDescription = Format.getDescription(uuid);
            LogUtils.logErrorLine("Cannot find exporter to " + formatDescription + " for " + modelName + ".");
        } else {
            try {
                Export.exportToFile(exporter, modelEntry.getModel(), file);
            } catch (IOException | ModelValidationException e) {
                throw new SerialisationException(e);
            }
        }
    }

    public ModelEntry runCommand(ModelEntry me, String className) {
        if (className != null) {
            for (Command tool: Commands.getApplicableCommands(me)) {
                if (className.equals(tool.getClass().getSimpleName())) {
                    WorkspaceEntry we = getWorkspaceEntry(me);
                    if (we == null) {
                        we = new WorkspaceEntry(null);
                        we.setModelEntry(me);
                    }
                    tool.run(we);
                    break;
                }
            }
        }
        return null;
    }

    public void initPlugins() {
        try {
            pluginManager.loadManifest();
        } catch (IOException | FormatException | PluginInstantiationException e) {
            e.printStackTrace();
        }
    }

    public void restartGUI() throws OperationCancelledException {
        guiRestartRequested = true;
        shutdownGUI();
    }

    public boolean isGUIRestartRequested() {
        return guiRestartRequested;
    }

    public void loadWorkspace(File file) throws DeserialisationException {
        workspace.load(file);
    }

    public Config getConfig() {
        return config;
    }

    public boolean checkFileMessageLog(File file, String title) {
        boolean result = true;
        if (title == null) {
            title = "File access error";
        }
        if (!file.exists()) {
            LogUtils.logErrorLine(title + ": The path  \"" + file.getPath() + "\" does not exisit.");
            result = false;
        } else if (!file.isFile()) {
            LogUtils.logErrorLine(title + ": The path  \"" + file.getPath() + "\" is not a file.");
            result = false;
        } else if (!file.canRead()) {
            LogUtils.logErrorLine(title + ": The file  \"" + file.getPath() + "\" cannot be read.");
            result = false;
        }
        return result;
    }

    public File getFileByAbsoluteOrRelativePath(String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(getWorkingDirectory(), path);
        }
        return file;
    }

    public void setWorkingDirectory(String path) {
        workingDirectory = new File(path);
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void repaintCurrentEditor() {
        if (isInGuiMode()) {
            final GraphEditorPanel editor = mainWindow.getCurrentEditor();
            editor.repaint();
        }
    }

}
