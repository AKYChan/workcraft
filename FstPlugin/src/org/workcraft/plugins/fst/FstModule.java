package org.workcraft.plugins.fst;

import org.workcraft.*;
import org.workcraft.commands.ScriptableCommandUtils;
import org.workcraft.plugins.fst.commands.ExtractWindowsCommand;
import org.workcraft.plugins.fst.commands.FsmToFstConversionCommand;
import org.workcraft.plugins.fst.commands.FstToFsmConversionCommand;
import org.workcraft.plugins.fst.commands.FstToStgConversionCommand;
import org.workcraft.plugins.fst.interop.SgExporter;
import org.workcraft.plugins.fst.interop.SgImporter;
import org.workcraft.plugins.fst.serialisation.SgSerialiser;

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
        pm.registerModel(FstDescriptor.class);
        pm.registerModelSerialiser(SgSerialiser.class);
        pm.registerSettings(ProcessWindowsSettings.class);

        pm.registerExporter(SgExporter.class);
        pm.registerImporter(SgImporter.class);

        ScriptableCommandUtils.register(FstToStgConversionCommand.class, "convertFstToStg",
                "convert the given FST 'work' into a new STG work");
        ScriptableCommandUtils.register(FsmToFstConversionCommand.class, "convertFsmToFst",
                "convert the given FSM 'work' into a new FST work");
        ScriptableCommandUtils.register(FstToFsmConversionCommand.class, "convertFstToFsm",
                "convert the given FST 'work' into a new FSM work");

        pm.registerCommand(ExtractWindowsCommand.class);
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
