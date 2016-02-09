package org.workcraft.plugins.punf;

import org.workcraft.Framework;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.gui.propertyeditor.Settings;

public class PunfModule implements Module {

    @Override
    public void init() {
        final Framework framework = Framework.getInstance();
        PluginManager pm = framework.getPluginManager();
        pm.registerClass(Settings.class, PunfUtilitySettings.class);
    }

    @Override
    public String getDescription() {
        return "Punf unfolding support";
    }
}
