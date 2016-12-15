package org.workcraft.plugins.mpsat;

import org.workcraft.Framework;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Command;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.plugins.mpsat.tools.ComplexGateSynthesisCommand;
import org.workcraft.plugins.mpsat.tools.GeneralisedCelementSynthesisCommand;
import org.workcraft.plugins.mpsat.tools.StandardCelementSynthesisCommand;
import org.workcraft.plugins.mpsat.tools.TechnologyMappingSynthesisCommand;

public class MpsatSynthesisModule implements Module {

    @Override
    public void init() {
        final Framework framework = Framework.getInstance();
        PluginManager pm = framework.getPluginManager();

        pm.registerClass(Command.class, ComplexGateSynthesisCommand.class);
        pm.registerClass(Command.class, GeneralisedCelementSynthesisCommand.class);
        pm.registerClass(Command.class, StandardCelementSynthesisCommand.class);
        pm.registerClass(Command.class, TechnologyMappingSynthesisCommand.class);
        pm.registerClass(Settings.class, MpsatSynthesisUtilitySettings.class);
    }

    @Override
    public String getDescription() {
        return "MPSat synthesis support";
    }
}
