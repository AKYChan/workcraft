package org.workcraft.plugins.wtg;

import org.workcraft.*;
import org.workcraft.commands.ScriptableCommandUtils;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.interop.Exporter;
import org.workcraft.plugins.wtg.commands.WtgToStgConversionCommand;
import org.workcraft.plugins.wtg.commands.WtgToStgWaverConversionCommand;
import org.workcraft.plugins.wtg.interop.WtgExporter;
import org.workcraft.plugins.wtg.serialisation.GuardDeserialiser;
import org.workcraft.plugins.wtg.serialisation.GuardSerialiser;
import org.workcraft.plugins.wtg.serialisation.WtgSerialiser;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.xml.XMLDeserialiser;
import org.workcraft.serialisation.xml.XMLSerialiser;

public class WtgModule  implements Module {

    @Override
    public String getDescription() {
        return "Waveform Transition Graph";
    }

    @Override
    public void init() {
        initPluginManager();
        initCompatibilityManager();
    }

    private void initPluginManager() {
        final Framework framework = Framework.getInstance();
        final PluginManager pm = framework.getPluginManager();

        pm.registerClass(ModelDescriptor.class, WtgDescriptor.class);
        pm.registerClass(Settings.class, WtgSettings.class);
        pm.registerClass(Settings.class, WaverSettings.class);
        pm.registerClass(ModelSerialiser.class, WtgSerialiser.class);
        pm.registerClass(XMLSerialiser.class, GuardSerialiser.class);
        pm.registerClass(XMLDeserialiser.class, GuardDeserialiser.class);

        pm.registerClass(Exporter.class, WtgExporter.class);

        ScriptableCommandUtils.register(WtgToStgConversionCommand.class, "convertWtgToStg",
                "convert the given WTG 'work' into a new STG work");

        ScriptableCommandUtils.register(WtgToStgWaverConversionCommand.class, "convertWtgToStgWaver",
                "convert the given WTG 'work' into a new STG work using Waver backend");
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
