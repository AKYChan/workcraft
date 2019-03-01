package org.workcraft.plugins.wtg;

import org.workcraft.plugins.CompatibilityManager;
import org.workcraft.Framework;
import org.workcraft.plugins.Plugin;
import org.workcraft.plugins.PluginManager;
import org.workcraft.Version;
import org.workcraft.utils.ScriptableCommandUtils;
import org.workcraft.plugins.wtg.commands.*;
import org.workcraft.plugins.wtg.serialisation.GuardDeserialiser;
import org.workcraft.plugins.wtg.serialisation.GuardSerialiser;

@SuppressWarnings("unused")
public class WtgPlugin implements Plugin {

    @Override
    public String getDescription() {
        return "Waveform Transition Graph plugin";
    }

    @Override
    public void init() {
        initPluginManager();
        initCompatibilityManager();
    }

    private void initPluginManager() {
        final Framework framework = Framework.getInstance();
        final PluginManager pm = framework.getPluginManager();

        pm.registerModelDescriptor(WtgDescriptor.class);
        pm.registerSettings(WtgSettings.class);

        pm.registerXmlSerialiser(GuardSerialiser.class);
        pm.registerXmlDeserialiser(GuardDeserialiser.class);

        ScriptableCommandUtils.register(WtgToStgConversionCommand.class, "convertWtgToStg",
                "convert the given WTG 'work' into a new STG work");

        ScriptableCommandUtils.register(WtgSoundnessVerificationCommand.class, "checkWtgSoundness",
                "check the given WTG 'work' for soundness and consistency");

        ScriptableCommandUtils.register(WtgReachabilityVerificationCommand.class, "checkWtgReachability",
                "check the given WTG 'work' for reachability of nodes and transitions");

        ScriptableCommandUtils.register(WtgInputPropernessVerificationCommand.class, "checkWtgInputProperness",
                "check the given WTG 'work' for input properness");

        ScriptableCommandUtils.register(StructureWaveformTransformationCommand.class, "transformWtgStructureWaveform",
                "transform the given WTG 'work' by structuring the waveforms");

        ScriptableCommandUtils.register(WtgSynthesisGuidelinesVerificationCommand.class, "checkWtgSynthesisGuidelines",
                "check the given WTG 'work' for compliance with the synthesis guidelines");
    }

    private void initCompatibilityManager() {
        final Framework framework = Framework.getInstance();
        final CompatibilityManager cm = framework.getCompatibilityManager();

        Version v321 = new Version(3, 2, 1, Version.Status.RELEASE);

        // TransitionSignalEvent
        cm.registerGlobalReplacement(v321, Wtg.class.getName(),
                "<node class=\"org.workcraft.plugins.dtd.SignalTransition\" ref=",
                "<node class=\"org.workcraft.plugins.dtd.TransitionEvent\" ref=");

        cm.registerContextualReplacement(v321, Wtg.class.getName(), "SignalTransition",
                "<property class=\"org.workcraft.plugins.dtd.SignalTransition\\$Direction\" enum-class=\"org.workcraft.plugins.dtd.SignalTransition\\$Direction\" name=\"direction\" value=\"(.*?)\"/>",
                "<property class=\"org.workcraft.plugins.dtd.TransitionEvent\\$Direction\" enum-class=\"org.workcraft.plugins.dtd.TransitionEvent\\$Direction\" name=\"direction\" value=\"$1\"/>");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<node class=\"org.workcraft.plugins.dtd.VisualSignalTransition\" ref=",
                "<node class=\"org.workcraft.plugins.dtd.VisualTransitionEvent\" ref=");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<VisualSignalTransition ref=\"(.*?)\"/>",
                "<VisualTransitionEvent ref=\"$1\"/>");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<VisualSignalTransition ref=\"(.*?)\">",
                "<VisualTransitionEvent ref=\"$1\">");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "</VisualSignalTransition>",
                "</VisualTransitionEvent>");

        cm.registerContextualReplacement(v321, VisualWtg.class.getName(), "VisualSignalTransition",
                "<property class=\"org.workcraft.plugins.dtd.SignalTransition\\$Direction\" enum-class=\"org.workcraft.plugins.dtd.SignalTransition\\$Direction\" name=\"direction\" value=\"(.*?)\"/>",
                "");

        // EntrySignalEvent
        cm.registerGlobalReplacement(v321, Wtg.class.getName(),
                "<node class=\"org.workcraft.plugins.dtd.SignalEntry\" ref=",
                "<node class=\"org.workcraft.plugins.dtd.EntryEvent\" ref=");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<node class=\"org.workcraft.plugins.dtd.VisualSignalEntry\" ref=",
                "<node class=\"org.workcraft.plugins.dtd.VisualEntryEvent\" ref=");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<VisualSignalEntry ref=\"(.*?)\"/>",
                "<VisualEntryEvent ref=\"$1\"/>");

        // ExitSignalEvent
        cm.registerGlobalReplacement(v321, Wtg.class.getName(),
                "<node class=\"org.workcraft.plugins.dtd.SignalExit\" ref=",
                "<node class=\"org.workcraft.plugins.dtd.ExitEvent\" ref=");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<node class=\"org.workcraft.plugins.dtd.VisualSignalExit\" ref=",
                "<node class=\"org.workcraft.plugins.dtd.VisualExitEvent\" ref=");

        cm.registerGlobalReplacement(v321, VisualWtg.class.getName(),
                "<VisualSignalExit ref=\"(.*?)\"/>",
                "<VisualExitEvent ref=\"$1\"/>");
    }

}
