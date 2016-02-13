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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
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
import org.workcraft.dom.Container;
import org.workcraft.dom.Model;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.FormatException;
import org.workcraft.exceptions.OperationCancelledException;
import org.workcraft.exceptions.PluginInstantiationException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.gui.DesktopApi;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.plugins.serialisation.XMLModelDeserialiser;
import org.workcraft.plugins.serialisation.XMLModelSerialiser;
import org.workcraft.serialisation.DeserialisationResult;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.ReferenceProducer;
import org.workcraft.serialisation.References;
import org.workcraft.tasks.DefaultTaskManager;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.ProgressMonitorArray;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Task;
import org.workcraft.tasks.TaskManager;
import org.workcraft.util.DataAccumulator;
import org.workcraft.util.FileUtils;
import org.workcraft.util.Import;
import org.workcraft.util.LogUtils;
import org.workcraft.util.XmlUtil;
import org.workcraft.workspace.Memento;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.xml.sax.SAXException;

public final class Framework {
    private static final String SETTINGS_DIRECTORY_NAME = ".workcraft";
    private static final String CONFIG_FILE_NAME = "config.xml";
    private static final String PLUGINS_FILE_NAME = "plugins.xml";
    private static final String UILAYOUT_FILE_NAME = "uilayout.xml";

    public static final String SETTINGS_DIRECTORY_PATH = System.getProperty("user.home") + File.separator + SETTINGS_DIRECTORY_NAME;
    public static final String CONFIG_FILE_PATH =  SETTINGS_DIRECTORY_PATH + File.separator + CONFIG_FILE_NAME;
    public static final String PLUGINS_FILE_PATH =  SETTINGS_DIRECTORY_PATH + File.separator + PLUGINS_FILE_NAME;
    public static final String UILAYOUT_FILE_PATH =  SETTINGS_DIRECTORY_PATH + File.separator + UILAYOUT_FILE_NAME;

    private static Framework instance = null;

    class ExecuteScriptAction implements ContextAction {
        private String script;
        private Scriptable scope;

        ExecuteScriptAction(String script, Scriptable scope) {
            this.script = script;
            this.scope = scope;
        }

        public Object run(Context cx) {
            return cx.evaluateString(scope, script, "<string>", 1, null);
        }
    }

    class ExecuteCompiledScriptAction implements ContextAction {
        private Script script;
        private Scriptable scope;

        ExecuteCompiledScriptAction(Script script, Scriptable scope) {
            this.script = script;
            this.scope = scope;
        }

        public Object run(Context cx) {
            return script.exec(cx, scope);
        }
    }

    class CompileScriptFromReaderAction implements ContextAction {
        private String sourceName;
        private BufferedReader reader;

        CompileScriptFromReaderAction(BufferedReader reader, String sourceName) {
            this.sourceName = sourceName;
            this.reader = reader;
        }

        public Object run(Context cx) {
            try {
                return cx.compileReader(reader, sourceName, 1, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class CompileScriptAction implements ContextAction {
        private String source, sourceName;

        CompileScriptAction(String source, String sourceName) {
            this.source = source;
            this.sourceName = sourceName;
        }

        public Object run(Context cx) {
            return cx.compileString(source, sourceName, 1, null);
        }
    }

    class SetArgs implements ContextAction {
        Object[] args;

        public void setArgs(Object[] args) {
            this.args = args;
        }

        public Object run(Context cx) {
            Object scriptable = Context.javaToJS(args, systemScope);
            ScriptableObject.putProperty(systemScope, "args", scriptable);
            systemScope.setAttributes("args", ScriptableObject.READONLY);
            return null;

        }
    }

    private PluginManager pluginManager;
    private ModelManager modelManager;
    private TaskManager taskManager;
    private CompatibilityManager compatibilityManager;
    private Config config;
    private Workspace workspace;

    private ScriptableObject systemScope;
    private ScriptableObject globalScope;

    private boolean inGUIMode = false;
    private boolean shutdownRequested = false;
    private boolean guiRestartRequested = false;
    private ContextFactory contextFactory = new ContextFactory();
    private MainWindow mainWindow;
    public Memento clipboard;

    private Framework() {
        pluginManager = new PluginManager();
        taskManager = new DefaultTaskManager() {
            public <T> Result<? extends T> execute(Task<T> task, String description, ProgressMonitor<? super T> observer) {
                if (SwingUtilities.isEventDispatchThread()) {
                    OperationCancelDialog<T> cancelDialog = new OperationCancelDialog<T>(mainWindow, description);

                    ProgressMonitorArray<T> observers = new ProgressMonitorArray<T>();
                    if (observer != null)
                        observers.add(observer);
                    observers.add(cancelDialog);

                    this.queue(task, description, observers);

                    cancelDialog.setVisible(true);

                    return cancelDialog.result;
                } else {
                    return super.execute(task, description, observer);
                }
            }
        };
        modelManager = new ModelManager();
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

    public void resetConfig() {
        config = new Config();
        for (PluginInfo<? extends Settings> info : pluginManager.getPlugins(Settings.class)) {
            info.getSingleton().load(config);
        }
    }

    public void loadConfig() {
        File file = new File(CONFIG_FILE_PATH);
        LogUtils.logMessageLine("Loading global preferences from " + file.getAbsolutePath());
        config.load(file);
        for (PluginInfo<? extends Settings> info : pluginManager.getPlugins(Settings.class)) {
            info.getSingleton().load(config);
        }
    }

    public void saveConfig() {
        for (PluginInfo<? extends Settings> info : pluginManager.getPlugins(Settings.class)) {
            info.getSingleton().save(config);
        }
        File file = new File(CONFIG_FILE_PATH);
        LogUtils.logMessageLine("Saving global preferences to " + file.getAbsolutePath());
        config.save(file);
    }

    public void setConfigVar(String key, String value) {
        config.set(key, value);
    }

    public void setConfigVar(String key, int value) {
        config.set(key, Integer.toString(value));
    }

    public void setConfigVar(String key, boolean value) {
        config.set(key, Boolean.toString(value));
    }

    public String getConfigVar(String key) {
        return config.get(key);
    }

    public int getConfigVarAsInt(String key, int defaultValue) {
        String s = config.get(key);

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getConfigVarAsBool(String key, boolean defaultValue) {
        String s = config.get(key);

        if (s == null)
            return defaultValue;
        else
            return Boolean.parseBoolean(s);
    }

    public String[] getModelNames() {
        LinkedList<Class<?>> list = modelManager.getModelList();
        String[] a = new String[list.size()];
        int i = 0;
        for (Class<?> cls : list)
            a[i++] = cls.getName();
        return a;
    }

    public void initJavaScript() {
        LogUtils.logMessageLine("Initialising javascript...");
        contextFactory.call(new ContextAction() {
            public Object run(Context cx) {
                ImporterTopLevel importer = new ImporterTopLevel();
                importer.initStandardObjects(cx, false);
                systemScope = importer;

                Object frameworkScriptable = Context.javaToJS(Framework.this, systemScope);
                ScriptableObject.putProperty(systemScope, "framework", frameworkScriptable);
                //ScriptableObject.putProperty(systemScope, "importer", );
                systemScope.setAttributes("framework", ScriptableObject.READONLY);

                globalScope = (ScriptableObject) cx.newObject(systemScope);
                globalScope.setPrototype(systemScope);
                globalScope.setParentScope(null);

                return null;
            }
        });
    }

    public ScriptableObject getJavaScriptGlobalScope() {
        return globalScope;
    }

    public void setJavaScriptProperty(final String name, final Object object, final ScriptableObject scope, final boolean readOnly) {
        contextFactory.call(new ContextAction() {
            public Object run(Context arg0) {
                Object scriptable = Context.javaToJS(object, scope);
                ScriptableObject.putProperty(scope, name, scriptable);

                if (readOnly)
                    scope.setAttributes(name, ScriptableObject.READONLY);

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
            return String.format("Java %s was unhandled in javascript. \nJavascript stack trace: %s", getCause().getClass().getSimpleName(), getScriptTrace());
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
                if (wrapped instanceof Throwable)
                    throw new JavascriptPassThroughException((Throwable) wrapped, ex.getScriptStackTrace());
            }
            throw ex;
        }
    }

    public void execJSResource(String resourceName) throws IOException {
        execJavaScript(FileUtils.readAllTextFromSystemResource(resourceName));
    }

    public void execJSFile(String filePath) throws IOException {

        execJavaScript(FileUtils.readAllText(new File(filePath)), globalScope);
    }

    public Script compileJavaScript(String source, String sourceName) {
        return (Script) doContextAction(new CompileScriptAction(source, sourceName));
    }

    public Script compileJavaScript(BufferedReader source, String sourceName) {
        return (Script) doContextAction(new CompileScriptFromReaderAction(source, sourceName));
    }

    public void startGUI() {
        if (inGUIMode) {
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
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

        inGUIMode = true;
    }

    public void shutdownGUI() throws OperationCancelledException {
        if (inGUIMode) {
            mainWindow.shutdown();
            mainWindow.dispose();
            mainWindow = null;
            inGUIMode = false;

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

    public ModelManager getModelManager() {
        return modelManager;
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

    public boolean isInGUIMode() {
        return inGUIMode;
    }

    public void setArgs(List<String> args) {
        SetArgs setargs = new SetArgs();
        setargs.setArgs(args.toArray());
        contextFactory.call(setargs);
    }

    public ModelEntry loadFile(File file) throws DeserialisationException {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayInputStream bis = compatibilityManager.process(fis);
            return load(bis);
        } catch (FileNotFoundException e) {
            throw new DeserialisationException(e);
        }
    }

    public ModelEntry importFile(File file) throws DeserialisationException {
        try {
            final Importer importer = Import.chooseBestImporter(getPluginManager(), file);
            return Import.importFromFile(importer, file);
        } catch (IOException e) {
            throw new DeserialisationException(e);
        }
    }

    private InputStream getUncompressedEntry(String name, InputStream zippedData) throws IOException {
        ZipInputStream zis = new ZipInputStream(zippedData);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            if (ze.getName().equals(name)) {
                return zis;
            }
            zis.closeEntry();
        }
        zis.close();
        return null;
    }

    private InputStream getMathData(byte[] bufferedInput, Document metaDoc) throws IOException {
        Element mathElement = XmlUtil.getChildElement("math", metaDoc.getDocumentElement());
        InputStream mathData = null;
        if (mathElement != null) {
            InputStream is = new ByteArrayInputStream(bufferedInput);
            mathData = getUncompressedEntry(mathElement.getAttribute("entry-name"), is);
        }
        return mathData;
    }

    private InputStream getVisualData(byte[] bufferedInput, Document metaDoc)    throws IOException {
        Element visualElement = XmlUtil.getChildElement("visual", metaDoc.getDocumentElement());
        InputStream visualData = null;
        if (visualElement  != null) {
            InputStream is = new ByteArrayInputStream(bufferedInput);
            visualData = getUncompressedEntry(visualElement.getAttribute("entry-name"), is);
        }
        return visualData;
    }

    private ModelDescriptor loadMetaDescriptor(Document metaDoc)
            throws InstantiationException, IllegalAccessException,    ClassNotFoundException {
        Element descriptorElement = XmlUtil.getChildElement("descriptor", metaDoc.getDocumentElement());
        String descriptorClass = XmlUtil.readStringAttr(descriptorElement, "class");
        ModelDescriptor descriptor = (ModelDescriptor) Class.forName(descriptorClass).newInstance();
        return descriptor;
    }

    private Document loadMetaDoc(byte[] bufferedInput)
            throws IOException, DeserialisationException, ParserConfigurationException, SAXException {
        InputStream metaData = getUncompressedEntry("meta", new ByteArrayInputStream(bufferedInput));
        if (metaData == null) {
            throw new DeserialisationException("meta entry is missing in the ZIP file");
        }
        Document metaDoc = XmlUtil.loadDocument(metaData);
        metaData.close();
        return metaDoc;
    }

    private void loadVisualModelState(byte[] bi, VisualModel model, References references)
            throws IOException, ParserConfigurationException, SAXException {
        InputStream stateData = getUncompressedEntry("state.xml", new ByteArrayInputStream(bi));
        if (stateData != null) {
            Document stateDoc = XmlUtil.loadDocument(stateData);
            Element stateElement = stateDoc.getDocumentElement();
            // level
            Element levelElement = XmlUtil.getChildElement("level", stateElement);
            Object currentLevel = references.getObject(levelElement.getAttribute("ref"));
            if (currentLevel instanceof Container) {
                model.setCurrentLevel((Container) currentLevel);
            }
            // selection
            Element selectionElement = XmlUtil.getChildElement("selection", stateElement);
            Set<Node> nodes = new HashSet<Node>();
            for (Element nodeElement: XmlUtil.getChildElements("node", selectionElement)) {
                Object node = references.getObject(nodeElement.getAttribute("ref"));
                if (node instanceof Node) {
                    nodes.add((Node) node);
                }
            }
            model.addToSelection(nodes);
        }
    }

    public ModelEntry load(InputStream is) throws DeserialisationException {
        try {
            // load meta data
            byte[] bi = DataAccumulator.loadStream(is);
            Document metaDoc = loadMetaDoc(bi);
            ModelDescriptor descriptor = loadMetaDescriptor(metaDoc);

            // load math model
            InputStream mathData = getMathData(bi, metaDoc);
            XMLModelDeserialiser mathDeserialiser = new XMLModelDeserialiser(getPluginManager());
            DeserialisationResult mathResult = mathDeserialiser.deserialise(mathData, null, null);
            mathData.close();

            // load visual model (if present)
            InputStream visualData = getVisualData(bi, metaDoc);
            if (visualData == null) {
                return new ModelEntry(descriptor, mathResult.model);
            }
            XMLModelDeserialiser visualDeserialiser = new XMLModelDeserialiser(getPluginManager());
            DeserialisationResult visualResult = visualDeserialiser.deserialise(visualData,
                    mathResult.references, mathResult.model);

            // load current level and selection
            if (visualResult.model instanceof VisualModel) {
                loadVisualModelState(bi, (VisualModel) visualResult.model, visualResult.references);
            }
            return new ModelEntry(descriptor, visualResult.model);
        } catch (IOException e) {
            throw new DeserialisationException(e);
        } catch (ParserConfigurationException e) {
            throw new DeserialisationException(e);
        } catch (SAXException e) {
            throw new DeserialisationException(e);
        } catch (InstantiationException e) {
            throw new DeserialisationException(e);
        } catch (IllegalAccessException e) {
            throw new DeserialisationException(e);
        } catch (ClassNotFoundException e) {
            throw new DeserialisationException(e);
        }
    }

    public ModelEntry load(Memento memento) {
        try {
            return load(memento.getStream());
        } catch (DeserialisationException e) {
            throw new RuntimeException(e);
        }
    }

    public ModelEntry load(InputStream is1, InputStream is2) throws DeserialisationException {
        ModelEntry me1 = load(is1);
        ModelEntry me2 = load(is2);

        VisualModel vmodel1 = me1.getVisualModel();
        VisualModel vmodel2 = me2.getVisualModel();

        if (!me1.getDescriptor().getDisplayName().equals(me2.getDescriptor().getDisplayName())) {
            throw new DeserialisationException("Incompatible models cannot be merged");
        }

        Collection<Node> children = new HashSet<Node>(vmodel2.getRoot().getChildren());

        vmodel1.selectNone();
        vmodel1.reparent(vmodel1.getCurrentLevel(), vmodel2, vmodel2.getRoot(), null);
        vmodel1.select(children);

        // FIXME: Dirty hack to avoid any hanging observers (serialise and deserialise the model).
        Memento memo = save(me1);
        ModelEntry me3 = load(memo);
        return me3;
    }

    public void save(ModelEntry model, String path) throws SerialisationException {
        File file = new File(path);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            save(model, stream);
            stream.close();
        } catch (FileNotFoundException e) {
            throw new SerialisationException(e);
        } catch (IOException e) {
            throw new SerialisationException(e);
        }
    }

    private void saveSelectionState(VisualModel visualModel, OutputStream os, ReferenceProducer visualRefs)
        throws ParserConfigurationException, IOException {
        Document stateDoc = XmlUtil.createDocument();
        Element stateRoot = stateDoc.createElement("workcraft-state");
        stateDoc.appendChild(stateRoot);
        // level
        Element levelElement = stateDoc.createElement("level");
        levelElement.setAttribute("ref", visualRefs.getReference(visualModel.getCurrentLevel()));
        stateRoot.appendChild(levelElement);
        // selection
        Element selectionElement = stateDoc.createElement("selection");
        for (Node node: visualModel.getSelection()) {
            Element nodeElement = stateDoc.createElement("node");
            nodeElement.setAttribute("ref", visualRefs.getReference(node));
            selectionElement.appendChild(nodeElement);
        }
        stateRoot.appendChild(selectionElement);
        XmlUtil.writeDocument(stateDoc, os);
    }

    public void save(ModelEntry modelEntry, OutputStream out) throws SerialisationException {
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
                saveSelectionState(visualModel, zos, visualRefs);
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
        } catch (ParserConfigurationException e) {
            throw new SerialisationException(e);
        } catch (IOException e) {
            throw new SerialisationException(e);
        }
    }

    public Memento save(ModelEntry modelEntry) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try {
            save(modelEntry, s);
        } catch (SerialisationException e) {
            throw new RuntimeException(e);
        }
        return new Memento(s.toByteArray());
    }

    public void initPlugins() {
        try {
            pluginManager.loadManifest();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (PluginInstantiationException e) {
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

    public boolean checkFile(File f, String errorTitle) {
        boolean result = true;
        if (errorTitle == null) {
            errorTitle = "File access error";
        }
        if (!f.exists()) {
            JOptionPane.showMessageDialog(null,
                    "The path  \""    + f.getPath() + "\" does not exisit.\n",
                    errorTitle, JOptionPane.ERROR_MESSAGE);
            result = false;
        } else if (!f.isFile()) {
            JOptionPane.showMessageDialog(null,
                    "The path  \""    + f.getPath() + "\" is not a file.\n",
                    errorTitle, JOptionPane.ERROR_MESSAGE);
            result = false;
        } else if (!f.canRead()) {
            JOptionPane.showMessageDialog(null,
                    "The file  \""    + f.getPath() + "\" cannot be read.\n",
                    errorTitle, JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        return result;
    }

    public void openExternally(String fileName, String errorTitle) {
        File file = new File(fileName);
        if (checkFile(file, errorTitle)) {
            DesktopApi.open(file);
        }
    }

}
