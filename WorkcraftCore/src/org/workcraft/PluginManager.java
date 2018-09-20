package org.workcraft;

import org.workcraft.commands.Command;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.exceptions.PluginInstantiationException;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.gui.propertyeditor.PropertyClassProvider;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.PluginInfo;
import org.workcraft.serialisation.ModelDeserialiser;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.xml.XMLDeserialiser;
import org.workcraft.serialisation.xml.XMLSerialiser;
import org.workcraft.util.ListMap;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.FileHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PluginManager implements PluginProvider {

    public static final String VERSION_STAMP = "d971444cbd86148695f3427118632aca";

    private final ListMap<Class<?>, PluginInfo<?>> plugins = new ListMap<>();

    public static class PluginInstanceHolder<T> implements PluginInfo<T> {
        private final Initialiser<? extends T> initialiser;
        private T instance;

        public PluginInstanceHolder(Initialiser<? extends T> initialiser) {
            this.initialiser = initialiser;
        }

        @Override
        public T newInstance() {
            return initialiser.create();
        }

        @Override
        public T getSingleton() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }
    }

    private boolean initModules() {
        boolean result = true;
        for (PluginInfo<? extends Module> info : getPlugins(Module.class)) {
            try {
                final Module module = info.newInstance();
                try {
                    LogUtils.logMessage("  Loading module: " + module.getDescription());
                    module.init();
                } catch (Throwable th) {
                    LogUtils.logWarning("Failed to initialise module '" + module.toString() + "'.");
                    result = false;
                }
            } catch (Exception e) {
                LogUtils.logWarning("Failed to load module implementation: " + e.getMessage());
                result = false;
            }
        }
        return result;
    }

    private void processLegacyPlugin(Class<?> cls, LegacyPluginInfo info) throws PluginInstantiationException {
        for (String interfaceName : info.getInterfaces()) {
            try {
                PluginInstanceHolder<Object> pih = new PluginInstanceHolder<Object>(info);
                plugins.put(Class.forName(interfaceName), pih);
            } catch (ClassNotFoundException e) {
                String className = info.getClassName();
                LogUtils.logWarning("Class '" + className + "' implements unknown interface '" + interfaceName + "'. Skipping.");
            }
        }
    }

    public void initPlugins() throws PluginInstantiationException {
        LogUtils.logMessage("Initialising plugins...");
        plugins.clear();

        String classPass = System.getProperty("java.class.path");
        String pathSeparator = System.getProperty("path.separator");
        String[] classPathLocations = classPass.split(pathSeparator);

        List<Class<?>> classes = new ArrayList<>();
        ArrayList<LegacyPluginInfo> pluginInfos = new ArrayList<>();

        for (String s : classPathLocations) {
            LogUtils.logMessage("  Processing class path entry: " + s);
            classes.addAll(PluginFinder.search(new File(s)));
        }
        LogUtils.logMessage("" + classes.size() + " plugin(s) found.");

        for (Class<?> cls : classes) {
            final LegacyPluginInfo info = new LegacyPluginInfo(cls);
            pluginInfos.add(info);
            processLegacyPlugin(cls, info);
        }

        initModules();
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<PluginInfo<? extends T>> getPlugins(Class<T> interf) {
        return (Collection<PluginInfo<? extends T>>) (Collection<?>) Collections.unmodifiableCollection(plugins.get(interf));
    }

    public void registerModel(final Class<? extends ModelDescriptor> cls) {
        registerClass(ModelDescriptor.class, cls);
    }

    public void registerSettings(final Class<? extends Settings> cls) {
        registerClass(Settings.class, cls);
    }

    public void registerModelSerialiser(final Class<? extends ModelSerialiser> cls) {
        registerClass(ModelSerialiser.class, cls);
    }

    public void registerModelDeserialiser(final Class<? extends ModelDeserialiser> cls) {
        registerClass(ModelDeserialiser.class, cls);
    }

    public void registerXmlSerialiser(final Class<? extends XMLSerialiser> cls) {
        registerClass(XMLSerialiser.class, cls);
    }

    public void registerXmlDeserialiser(final Class<? extends XMLDeserialiser> cls) {
        registerClass(XMLDeserialiser.class, cls);
    }

    public void registerImporter(final Class<? extends Importer> cls) {
        registerClass(Importer.class, cls);
    }

    public void registerExporter(final Class<? extends Exporter> cls) {
        registerClass(Exporter.class, cls);
    }

    public void registerFileHandler(final Class<? extends FileHandler> cls) {
        registerClass(FileHandler.class, cls);
    }

    public void registerProperty(final Class<? extends PropertyClassProvider> cls) {
        registerClass(PropertyClassProvider.class, cls);
    }

    public void registerGlobalTool(final Class<? extends GraphEditorTool> cls) {
        registerClass(GraphEditorTool.class, cls);
    }

    public void registerCommand(final Class<? extends Command> cls) {
        registerClass(Command.class, cls);
    }

    private <T> void registerClass(Class<T> interf, final Class<? extends T> cls) {
        registerClass(interf, () -> {
            try {
                return cls.newInstance();
            } catch (InstantiationException e) {
                Throwable q = e;
                System.err.println(cls.getCanonicalName());
                while (q != null) {
                    q.printStackTrace();
                    q = q.getCause();
                }
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void registerGlobalTool(Initialiser<? extends GraphEditorTool> initialiser) {
        registerClass(GraphEditorTool.class, initialiser);
    }

    public <T> void registerClass(Class<T> interf, Initialiser<? extends T> initialiser) {
        if (!interf.isInterface()) {
            throw new RuntimeException("'interf' argument must be an interface");
        }
        final PluginInfo<T> pluginInfo = new PluginInstanceHolder<>(initialiser);
        plugins.put(interf, pluginInfo);
    }

}
