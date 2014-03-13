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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.propertyeditor.SettingsPage;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.plugins.serialisation.XMLDualModelDeserialiser;
import org.workcraft.plugins.serialisation.XMLModelDeserialiser;
import org.workcraft.plugins.serialisation.XMLModelSerialiser;
import org.workcraft.serialisation.DeserialisationResult;
import org.workcraft.serialisation.DualDeserialisationResult;
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
import org.workcraft.util.XmlUtil;
import org.workcraft.workspace.Memento;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.xml.sax.SAXException;


public class Framework {
	public static final String FRAMEWORK_VERSION_MAJOR = "2";
	public static final String FRAMEWORK_VERSION_MINOR = "dev";

	class ExecuteScriptAction implements ContextAction {
		private String script;
		private Scriptable scope;

		public ExecuteScriptAction(String script, Scriptable scope) {
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

		public ExecuteCompiledScriptAction(Script script, Scriptable scope) {
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

		public CompileScriptFromReaderAction(BufferedReader reader, String sourceName) {
			this.sourceName = sourceName;
			this.reader = reader;
		}

		public Object run(Context cx) {
			try {
				return cx.compileReader(reader, sourceName, 1, null);
			} catch (IOException e) {
				throw new RuntimeException (e);
			}
		}
	}

	class CompileScriptAction implements ContextAction {
		private String source, sourceName;

		public CompileScriptAction(String source, String sourceName) {
			this.source = source;
			this.sourceName = sourceName;
		}

		public Object run(Context cx) {
			return cx.compileString(source, sourceName, 1, null);
		}
	}

	class SetArgs implements ContextAction {
		Object[] args;

		public void setArgs (Object[] args) {
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
	private Config config ;
	private Workspace workspace;

	private ScriptableObject systemScope;
	private ScriptableObject globalScope;

	private boolean inGUIMode = false;
	private boolean shutdownRequested = false;
	private boolean GUIRestartRequested = false;

	private ContextFactory contextFactory = new ContextFactory();

	private boolean silent = false;

	private MainWindow mainWindow;

	public Memento clipboard;

	public Framework() {
		pluginManager = new PluginManager(this);
		taskManager = new DefaultTaskManager()
		{
			public <T> Result<? extends T> execute(Task<T> task, String description, ProgressMonitor<? super T> observer) {
				if(SwingUtilities.isEventDispatchThread())
				{
					OperationCancelDialog<T> cancelDialog = new OperationCancelDialog<T>(mainWindow, description);

					ProgressMonitorArray<T> observers = new ProgressMonitorArray<T>();
					if(observer != null)
						observers.add(observer);
					observers.add(cancelDialog);

					this.queue(task, description, observers);

					cancelDialog.setVisible(true);

					return cancelDialog.result;
				}
				else
					return super.execute(task, description, observer);
			};
		};
		modelManager = new ModelManager();
		config = new Config();
		workspace = new Workspace(this);
	}

	public void loadConfig(String fileName) {
		config.load(fileName);

		for (PluginInfo<? extends SettingsPage> info : pluginManager.getPlugins(SettingsPage.class)) {
			info.getSingleton().load(config);
		}
	}

	public void saveConfig(String fileName) {
		for (PluginInfo<? extends SettingsPage> info : pluginManager.getPlugins(SettingsPage.class)) {
			info.getSingleton().save(config);
		}

		config.save(fileName);
	}

	public void setConfigVar (String key, String value) {
		config.set(key, value);
	}

	public void setConfigVar (String key, int value) {
		config.set(key, Integer.toString(value));
	}

	public void setConfigVar (String key, boolean value) {
		config.set(key, Boolean.toString(value));
	}

	public String getConfigVar (String key) {
		return config.get(key);
	}

	public int getConfigVarAsInt (String key, int defaultValue)  {
		String s = config.get(key);

		try {
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public boolean getConfigVarAsBool (String key, boolean defaultValue)  {
		String s = config.get(key);

		if (s == null)
			return defaultValue;
		else
			return Boolean.parseBoolean(s);
	}

	public String[] getModelNames() {
		LinkedList<Class<?>> list = modelManager.getModelList();
		String a[] = new String[list.size()];
		int i=0;
		for (Class<?> cls : list)
			a[i++] = cls.getName();
		return a;
	}

	public void initJavaScript() {
		if (!silent)
			System.out.println ("Initialising javascript...");
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

	public void setJavaScriptProperty (final String name, final Object object, final ScriptableObject scope, final boolean readOnly) {
		contextFactory.call(new ContextAction(){
			public Object run(Context arg0) {
				Object scriptable = Context.javaToJS(object, scope);
				ScriptableObject.putProperty(scope, name, scriptable);

				if (readOnly)
					scope.setAttributes(name, ScriptableObject.READONLY);

				return scriptable;
			}
		});
	}

	public void deleteJavaScriptProperty (final String name, final ScriptableObject scope) {
		contextFactory.call(new ContextAction(){
			public Object run(Context arg0) {
				return ScriptableObject.deleteProperty(scope, name);
			}
		});

	}

	public Object execJavaScript(File file) throws FileNotFoundException {
		BufferedReader reader = new BufferedReader (new FileReader(file));
		return execJavaScript(compileJavaScript(reader, file.getPath()));
	}

	public Object execJavaScript(Script script) {
		return execJavaScript (script, globalScope);
	}

	static class JavascriptPassThroughException extends RuntimeException
	{
		private static final long serialVersionUID = 8906492547355596206L;
		private final String scriptTrace;

		public JavascriptPassThroughException(Throwable wrapped, String scriptTrace)
		{
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

	public Object execJavaScript (String script) {
		return execJavaScript(script, globalScope);
	}

	public Object execJavaScript(Script script, Scriptable scope) {
		return doContextAction(new ExecuteCompiledScriptAction(script, scope));
	}

	public Object execJavaScript(String script, Scriptable scope) {
		return doContextAction(new ExecuteScriptAction(script, scope));
	}

	private Object doContextAction (ContextAction action) {
		try
		{
			return contextFactory.call(action);
		} catch(JavaScriptException ex)
		{
			System.out.println("Script stack trace: " + ex.getScriptStackTrace());
			Object value = ex.getValue();
			if(value instanceof NativeJavaObject)
			{
				Object wrapped = ((NativeJavaObject)value).unwrap();
				if(wrapped instanceof Throwable)
					throw new JavascriptPassThroughException((Throwable)wrapped, ex.getScriptStackTrace());
			}
			throw ex;
		}
	}

	public void execJSResource (String resourceName) throws IOException {
		execJavaScript(FileUtils.readAllTextFromSystemResource(resourceName));
	}

	public void execJSFile (String filePath) throws IOException {

		execJavaScript (FileUtils.readAllText(new File(filePath)), globalScope);
	}

	public Script compileJavaScript (String source, String sourceName) {
		return (Script) doContextAction(new CompileScriptAction(source, sourceName));
	}

	public Script compileJavaScript (BufferedReader source, String sourceName) {
		return (Script) doContextAction(new CompileScriptFromReaderAction(source, sourceName));	}

	public void startGUI() {
		if (inGUIMode) {
			System.out.println ("Already in GUI mode");
			return;
		}
		GUIRestartRequested = false;
		System.out.println ("Switching to GUI mode...");

		if (SwingUtilities.isEventDispatchThread()) {
			mainWindow = new MainWindow(Framework.this);
			mainWindow.startup();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						mainWindow = new MainWindow(Framework.this);
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
			public Object run(Context cx) {
				Object guiScriptable = Context.javaToJS(mainWindow, systemScope);
				ScriptableObject.putProperty(systemScope, "mainWindow", guiScriptable);
				systemScope.setAttributes("mainWindow", ScriptableObject.READONLY);
				return null;

			}
		});

		System.out.println ("Now in GUI mode.");
		inGUIMode = true;
	}

	public void shutdownGUI() throws OperationCancelledException {
		if (inGUIMode) {
			mainWindow.shutdown();
			mainWindow.dispose();
			mainWindow = null;
			inGUIMode = false;

			contextFactory.call(new ContextAction() {
				public Object run(Context cx) {
					ScriptableObject.deleteProperty(systemScope, "mainWindow");
					return null;
				}
			});
		}
		System.out.println ("Now in console mode.");
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

	public Workspace getWorkspace() {
		return workspace;
	}

	public boolean isInGUIMode() {
		return inGUIMode;
	}

	public boolean isSilent() {
		return silent;
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	public void setArgs(List<String> args) {
		SetArgs setargs = new SetArgs();
		setargs.setArgs(args.toArray());
		contextFactory.call(setargs);
	}

	public ModelEntry loadFile(File file) throws DeserialisationException {
		try {
			FileInputStream fis = new FileInputStream(file);
			return load(fis);
		} catch (FileNotFoundException e) {
			throw new DeserialisationException(e);
		}
	}

	public ModelEntry importFile(File file) throws DeserialisationException  {
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

		while ((ze = zis.getNextEntry()) != null)	{
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

	private InputStream getVisualData(byte[] bufferedInput, Document metaDoc)	throws IOException {
		Element visualElement = XmlUtil.getChildElement("visual", metaDoc.getDocumentElement());
		InputStream visualData = null;
		if (visualElement  != null) {
			InputStream is = new ByteArrayInputStream(bufferedInput);
			visualData = getUncompressedEntry(visualElement.getAttribute("entry-name"), is);
		}
		return visualData;
	}

	private ModelDescriptor loadMetaDescriptor(Document metaDoc)
			throws InstantiationException, IllegalAccessException,	ClassNotFoundException {
		Element descriptorElement = XmlUtil.getChildElement("descriptor", metaDoc.getDocumentElement());
		String descriptorClass = XmlUtil.readStringAttr(descriptorElement, "class");
		ModelDescriptor descriptor = (ModelDescriptor)Class.forName(descriptorClass).newInstance();
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
				model.setCurrentLevel((Container)currentLevel);
			}
			// selection
			Element selectionElement = XmlUtil.getChildElement("selection", stateElement);
			Set<Node> nodes = new HashSet<Node>();
			for (Element nodeElement: XmlUtil.getChildElements("node", selectionElement)) {
				Object node = references.getObject(nodeElement.getAttribute("ref"));
				if (node instanceof Node) {
					nodes.add((Node)node);
				}
			}
			model.addToSelection(nodes);
		}
	}

	public ModelEntry load(InputStream is) throws DeserialisationException   {
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
				loadVisualModelState(bi, (VisualModel)visualResult.model, visualResult.references);
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
		try {
			// load meta data
			byte[] bi1 = DataAccumulator.loadStream(is1);
			Document metaDoc1 = loadMetaDoc(bi1);
			ModelDescriptor descriptor1 = loadMetaDescriptor(metaDoc1);

			byte[] bi2 = DataAccumulator.loadStream(is2);
			Document metaDoc2 = loadMetaDoc(bi2);
			ModelDescriptor descriptor2 = loadMetaDescriptor(metaDoc2);

			// load math models
			InputStream mathData1 = getMathData(bi1, metaDoc1);
			InputStream mathData2 = getMathData(bi2, metaDoc2);
			if (!descriptor1.getDisplayName().equals(descriptor2.getDisplayName()) ) {
				// math models cannot be merged
				throw new DeserialisationException("incompatible models cannot be merged");
			}
			XMLDualModelDeserialiser mathDeserialiser = new XMLDualModelDeserialiser(getPluginManager());
			DualDeserialisationResult mathResult = mathDeserialiser.deserialise(mathData1, mathData2, null, null, null);
			mathData1.close();
			mathData2.close();

			// load visual models (if present)
			InputStream visualData1 = getVisualData(bi1, metaDoc1);
			InputStream visualData2 = getVisualData(bi2, metaDoc2);
			if (visualData1 == null && visualData2 == null) {
				return new ModelEntry(descriptor1, mathResult.model);
			}
			if (visualData1 == null || visualData2 == null) {
				// visual models cannot be merged
				throw new DeserialisationException("incompatible models cannot be merged");
			}
			XMLDualModelDeserialiser visualDeserialiser = new XMLDualModelDeserialiser(getPluginManager());
			DualDeserialisationResult visualResult = visualDeserialiser.deserialise(visualData1, visualData2,
					mathResult.references1, mathResult.references2, mathResult.model);

			// load current level and selection
			if (visualResult.model instanceof VisualModel) {
				VisualModel visualModel = (VisualModel)visualResult.model;
				loadVisualModelState(bi1, visualModel, visualResult.references1);
				// move the nodes of model2 into the current level of model1
				Collection<Node> nodes = new HashSet<Node>();
				for (Object obj: visualResult.references2.getObjects()) {
					if (obj instanceof Node) {
						Node node = (Node)obj;
						if (node.getParent() == visualModel.getRoot()) {
							nodes.add(node);
						}
					}
				}
				visualModel.getRoot().reparent(nodes, visualModel.getCurrentLevel());
				// select the nodes of model2
				visualModel.select(nodes);
			}
			return new ModelEntry(descriptor1, visualResult.model);
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

	public void save(ModelEntry model, String path) throws SerialisationException {
		File file = new File(path);
		try {
			FileOutputStream stream = new FileOutputStream(file);
			save (model, stream);
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

		Element levelElement = stateDoc.createElement("level");
		levelElement.setAttribute("ref", visualRefs.getReference(visualModel.getCurrentLevel()));
		stateRoot.appendChild(levelElement);

		Element selectionElement = stateDoc.createElement("selection");
		for (Node node: visualModel.getSelection()) {
			Element nodeElement = stateDoc.createElement("node");
			nodeElement.setAttribute("ref", visualRefs.getReference(node));
			selectionElement.appendChild(nodeElement);
		}
		stateRoot.appendChild(selectionElement);
		XmlUtil.writeDocument(stateDoc, os);
	}


public HashMap<String, String> saveToText(ModelEntry modelEntry) throws SerialisationException, IOException, ParserConfigurationException {

		HashMap<String, String> ret = new HashMap<String, String>();

		Model model = modelEntry.getModel();
		VisualModel visualModel = (model instanceof VisualModel)? (VisualModel)model : null ;
		Model mathModel = (visualModel == null) ? model : visualModel.getMathModel();

		ModelSerialiser mathSerialiser = new XMLModelSerialiser(getPluginManager());
		// serialise math model
		String mathEntryName = "model" + mathSerialiser.getExtension();

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		ReferenceProducer refResolver = mathSerialiser.serialise(mathModel, os, null);
		ret.put(mathEntryName, os.toString());
		os.close();

		// serialise visual model
		String visualEntryName = null;
		ModelSerialiser visualSerialiser = null;
		if (visualModel != null) {
			visualSerialiser = new XMLModelSerialiser(getPluginManager());

			visualEntryName = "visualModel" + visualSerialiser.getExtension();

			os = new ByteArrayOutputStream();
			ReferenceProducer visualRefs = visualSerialiser.serialise(visualModel, os, refResolver);
			ret.put(visualEntryName, os.toString());
			os.close();

			// serialise visual model selection state
			os = new ByteArrayOutputStream();

			saveSelectionState(visualModel, os, visualRefs);
			ret.put("state.xml", os.toString());

		}

		// serialise meta data
		os = new ByteArrayOutputStream();

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


		XmlUtil.writeDocument(metaDoc, os);
		ret.put("meta", os.toString());
		os.close();

		return ret;
	}

	/*
	 * converting a given model to a string for debug purposes
	 */
	public String saveToString(ModelEntry modelEntry) {
		try {
			HashMap<String, String> mod = saveToText(modelEntry);
			StringBuilder sb = new StringBuilder();

			for (Entry<String, String> en: mod.entrySet()) {
				sb.append("=="+en.getKey()+"\n");
				sb.append(en.getValue());
				sb.append("\n");
			}

			return sb.toString();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SerialisationException e) {
			throw new RuntimeException(e);
		}
	}


	public void save(ModelEntry modelEntry, OutputStream out) throws SerialisationException {
		Model model = modelEntry.getModel();
		VisualModel visualModel = (model instanceof VisualModel)? (VisualModel)model : null ;
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
		if (!silent) {
			System.out.println ("Loading plugins configuration...");
		}
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
		GUIRestartRequested = true;
		shutdownGUI();
	}

	public boolean isGUIRestartRequested() {
		return GUIRestartRequested;
	}

	public void loadWorkspace(File file) throws DeserialisationException {
		workspace.load(file);
	}

	public Config getConfig() {
		return config;
	}

}
