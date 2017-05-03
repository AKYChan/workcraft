package org.workcraft.plugins.stg;

import org.workcraft.CompatibilityManager;
import org.workcraft.Framework;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Version;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.graph.commands.Command;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.stg.commands.ContractNamedTransitionTransformationCommand;
import org.workcraft.plugins.stg.commands.DummyToSignalTransitionTransformationCommand;
import org.workcraft.plugins.stg.commands.ExpandHandshakeCtrlSanTransformationCommand;
import org.workcraft.plugins.stg.commands.ExpandHandshakeReqAckTransformationCommand;
import org.workcraft.plugins.stg.commands.ExpandHandshakeTransformationCommand;
import org.workcraft.plugins.stg.commands.ExplicitPlaceTransformationCommand;
import org.workcraft.plugins.stg.commands.ImplicitPlaceTransformationCommand;
import org.workcraft.plugins.stg.commands.InsertDummyTransformationCommand;
import org.workcraft.plugins.stg.commands.MergeTransitionTransformationCommand;
import org.workcraft.plugins.stg.commands.MirrorSignalTransformationCommand;
import org.workcraft.plugins.stg.commands.MirrorTransitionTransformationCommand;
import org.workcraft.plugins.stg.commands.PetriToStgConversionCommand;
import org.workcraft.plugins.stg.commands.SignalToDummyTransitionTransformationCommand;
import org.workcraft.plugins.stg.commands.StgToPetriConversionCommand;
import org.workcraft.plugins.stg.interop.DotGExporter;
import org.workcraft.plugins.stg.interop.DotGImporter;
import org.workcraft.plugins.stg.interop.LpnExporter;
import org.workcraft.plugins.stg.interop.LpnImporter;
import org.workcraft.plugins.stg.serialisation.DotGSerialiser;
import org.workcraft.plugins.stg.serialisation.ImplicitPlaceArcDeserialiser;
import org.workcraft.plugins.stg.serialisation.ImplicitPlaceArcSerialiser;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.xml.XMLDeserialiser;
import org.workcraft.serialisation.xml.XMLSerialiser;

public class StgModule implements Module {

    @Override
    public String getDescription() {
        return "Signal Transition Graphs";
    }

    @Override
    public void init() {
        initPluginManager();
        initCompatibilityManager();
    }

    private void initPluginManager() {
        final Framework framework = Framework.getInstance();
        final PluginManager pm = framework.getPluginManager();
        pm.registerClass(ModelDescriptor.class, StgDescriptor.class);

        pm.registerClass(XMLSerialiser.class, ImplicitPlaceArcSerialiser.class);
        pm.registerClass(XMLDeserialiser.class, ImplicitPlaceArcDeserialiser.class);

        pm.registerClass(Exporter.class, DotGExporter.class);
        pm.registerClass(Exporter.class, LpnExporter.class);
        pm.registerClass(Importer.class, DotGImporter.class);
        pm.registerClass(Importer.class, LpnImporter.class);

        pm.registerClass(ModelSerialiser.class, DotGSerialiser.class);
        pm.registerClass(Settings.class, StgSettings.class);

        pm.registerClass(Command.class, MirrorSignalTransformationCommand.class);
        pm.registerClass(Command.class, MirrorTransitionTransformationCommand.class);
        pm.registerClass(Command.class, ImplicitPlaceTransformationCommand.class);
        pm.registerClass(Command.class, ExplicitPlaceTransformationCommand.class);
        pm.registerClass(Command.class, SignalToDummyTransitionTransformationCommand.class);
        pm.registerClass(Command.class, DummyToSignalTransitionTransformationCommand.class);
        pm.registerClass(Command.class, ContractNamedTransitionTransformationCommand.class);
        pm.registerClass(Command.class, PetriToStgConversionCommand.class);
        pm.registerClass(Command.class, StgToPetriConversionCommand.class);
        pm.registerClass(Command.class, MergeTransitionTransformationCommand.class);
        pm.registerClass(Command.class, InsertDummyTransformationCommand.class);
        pm.registerClass(Command.class, ExpandHandshakeTransformationCommand.class);
        pm.registerClass(Command.class, ExpandHandshakeReqAckTransformationCommand.class);
        pm.registerClass(Command.class, ExpandHandshakeCtrlSanTransformationCommand.class);
    }

    private void initCompatibilityManager() {
        final Framework framework = Framework.getInstance();
        final CompatibilityManager cm = framework.getCompatibilityManager();
        Version v310 = new Version(3, 1, 0, Version.Status.RELEASE);
        Version v314 = new Version(3, 1, 0, Version.Status.RELEASE);

        cm.registerMetaReplacement(v310,
                "<descriptor class=\"org.workcraft.plugins.stg.STGModelDescriptor\"/>",
                "<descriptor class=\"org.workcraft.plugins.stg.StgDescriptor\"/>");

        cm.registerModelReplacement(v310, "org.workcraft.plugins.stg.STG", Stg.class.getName());

        cm.registerModelReplacement(v310, "org.workcraft.plugins.stg.VisualSTG", VisualStg.class.getName());

        cm.registerGlobalReplacement(v310, Stg.class.getName(), "<STGPlace>", "<StgPlace>");

        cm.registerGlobalReplacement(v310, Stg.class.getName(), "</STGPlace>", "</StgPlace>");

        cm.registerGlobalReplacement(v310, Stg.class.getName(),
                "<node class=\"org.workcraft.plugins.stg.STGPlace\" ref=",
                "<node class=\"org.workcraft.plugins.stg.StgPlace\" ref=");

        cm.registerGlobalReplacement(v314, VisualStg.class.getName(),
                "<node class=\"org.workcraft.plugins.petri.VisualPlace\" ref=",
                "<node class=\"org.workcraft.plugins.stg.VisualStgPlace\" ref=");

        cm.registerGlobalReplacement(v314, VisualStg.class.getName(),
                "<VisualPlace ref=\"(.*?)\"/>",
                "<VisualStgPlace ref=\"$1\"/>");

        cm.registerGlobalReplacement(v314, VisualStg.class.getName(),
                "<VisualPlace ref=\"(.*?)\">",
                "<VisualStgPlace ref=\"$1\"/>\\n<VisualPlace>");
    }

}
