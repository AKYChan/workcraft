package org.workcraft.plugins;

import org.workcraft.Framework;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.plugins.workspace.handlers.SystemOpen;
import org.workcraft.plugins.workspace.handlers.WorkcraftOpen;

public class BuiltinFileHandlers implements Module {

    @Override
    public void init() {
        final Framework framework = Framework.getInstance();
        final PluginManager pm = framework.getPluginManager();
        pm.registerFileHandler(() -> new WorkcraftOpen());
        pm.registerFileHandler(SystemOpen.class);
    }

    @Override
    public String getDescription() {
        return "Built-in file operations for Workspace";
    }

}
