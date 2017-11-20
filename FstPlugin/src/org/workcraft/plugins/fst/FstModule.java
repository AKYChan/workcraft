package org.workcraft.plugins.fst;

import org.workcraft.CompatibilityManager;
import org.workcraft.Framework;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Version;
import org.workcraft.commands.Command;
import org.workcraft.commands.ScriptableCommandUtils;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.fst.commands.ExtractWindowsCommand;
import org.workcraft.plugins.fst.commands.FsmToFstConversionCommand;
import org.workcraft.plugins.fst.commands.FstToFsmConversionCommand;
import org.workcraft.plugins.fst.commands.FstToStgConversionCommand;
import org.workcraft.plugins.fst.interop.SgExporter;
import org.workcraft.plugins.fst.interop.SgImporter;
import org.workcraft.plugins.fst.serialisation.SgSerialiser;
import org.workcraft.serialisation.ModelSerialiser;

public class FstModule implements Module {

    @Override
    public String getDescription() {
        return "Finite State Transducer";
    }

    @Override
    public void init() {
        initPluginManager();
        initCompatibilityManager();
    }

    private void initPluginManager() {
        final Framework framework = Framework.getInstance();
        final PluginManager pm = framework.getPluginManager();
        pm.registerClass(ModelDescriptor.class, FstDescriptor.class);
        pm.registerClass(ModelSerialiser.class, SgSerialiser.class);
        pm.registerClass(Settings.class, ProcessWindowsSettings.class);

        pm.registerClass(Exporter.class, SgExporter.class);
        pm.registerClass(Importer.class, SgImporter.class);

        ScriptableCommandUtils.register(FstToStgConversionCommand.class, "convertFstToStg",
                "convert the given FST 'work' into a new STG work");
        ScriptableCommandUtils.register(FsmToFstConversionCommand.class, "convertFsmToFst",
                "convert the given FSM 'work' into a new FST work");
        ScriptableCommandUtils.register(FstToFsmConversionCommand.class, "convertFstToFsm",
                "convert the given FST 'work' into a new FSM work");

        pm.registerClass(Command.class, ExtractWindowsCommand.class);
    }

    private void initCompatibilityManager() {
        final Framework framework = Framework.getInstance();
        final CompatibilityManager cm = framework.getCompatibilityManager();
        Version v310 = new Version(3, 1, 0, Version.Status.RELEASE);

        cm.registerMetaReplacement(v310,
                "<descriptor class=\"org.workcraft.plugins.fst.FstModelDescriptor\"/>",
                "<descriptor class=\"org.workcraft.plugins.fst.FstDescriptor\"/>");
    }

}
