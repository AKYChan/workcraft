package org.workcraft.plugins.pcomp;

import org.workcraft.Framework;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Command;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.plugins.pcomp.tools.PcompTool;

public class PcompModule implements Module {

    @Override
    public void init() {
        final Framework framework = Framework.getInstance();
        PluginManager pm = framework.getPluginManager();

        pm.registerClass(Command.class, PcompTool.class);
        pm.registerClass(Settings.class, PcompUtilitySettings.class);
    }

    @Override
    public String getDescription() {
        return "PComp parallel composition support";
    }
}
