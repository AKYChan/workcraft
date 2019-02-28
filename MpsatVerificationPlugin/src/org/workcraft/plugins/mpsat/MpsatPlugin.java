package org.workcraft.plugins.mpsat;

import org.workcraft.Framework;
import org.workcraft.plugins.Plugin;
import org.workcraft.plugins.PluginManager;
import org.workcraft.utils.ScriptableCommandUtils;
import org.workcraft.plugins.mpsat.commands.*;

@SuppressWarnings("unused")
public class MpsatPlugin implements Plugin {

    @Override
    public String getDescription() {
        return "MPSat verification plugin";
    }

    @Override
    public void init() {
        final Framework framework = Framework.getInstance();
        PluginManager pm = framework.getPluginManager();

        pm.registerSettings(MpsatVerificationSettings.class);

        ScriptableCommandUtils.register(MpsatCscConflictResolutionCommand.class, "resolveCscConflictMpsat",
                "resolve complete state coding conflicts with MPSat backend");

        ScriptableCommandUtils.register(MpsatCombinedVerificationCommand.class, "checkStgCombined",
                "combined check of the STG 'work' for consistency, deadlock freeness, input properness, output persistency, and mutex implementability");
        ScriptableCommandUtils.register(MpsatConsistencyVerificationCommand.class, "checkStgConsistency",
                "check the STG 'work' for consistency");
        ScriptableCommandUtils.register(MpsatDeadlockFreenessVerificationCommand.class, "checkStgDeadlockFreeness",
                "check the STG (or Petri net) 'work' for deadlock freeness");
        ScriptableCommandUtils.register(MpsatInputPropernessVerificationCommand.class, "checkStgInputProperness",
                "check the STG 'work' for input properness");
        ScriptableCommandUtils.register(MpsatOutputPersistencyVerificationCommand.class, "checkStgOutputPersistency",
                "check the STG 'work' for output persistency");
        ScriptableCommandUtils.register(MpsatCscVerificationCommand.class, "checkStgCsc",
                "check the STG 'work' for complete state coding");
        ScriptableCommandUtils.register(MpsatUscVerificationCommand.class, "checkStgUsc",
                "check the STG 'work' for unique state coding");
        ScriptableCommandUtils.register(MpsatDiInterfaceVerificationCommand.class, "checkStgDiInterface",
                "check the STG 'work' for delay-insensitive interface");
        ScriptableCommandUtils.register(MpsatNormalcyVerificationCommand.class, "checkStgNormalcy",
                "check the STG 'work' for normalcy");
        ScriptableCommandUtils.register(MpsatMutexImplementabilityVerificationCommand.class, "checkStgMutexImplementability",
                "check the STG 'work' for implementability of its mutex places");
        ScriptableCommandUtils.register(MpsatOutputDeterminacyVerificationCommand.class, "checkStgOutputDeterminacy",
                "check the STG 'work' for output determinacy");
        ScriptableCommandUtils.register(MpsatConformationNwayVerificationCommand.class, "checkStgNwayConformation",
                "check all the open STG works for N-way conformation ('null' should be passed as the parameter)");

        // TODO: Need a way to pass the list of places from the JavaScript wrapper
        pm.registerCommand(MpsatPlaceRedundancyVerificationCommand.class);
        // TODO: Need a way to pass the environment file from the JavaScript wrapper
        pm.registerCommand(MpsatConformationVerificationCommand.class);
        pm.registerCommand(MpsatPropertyVerificationCommand.class);
        pm.registerCommand(MpsatAssertionVerificationCommand.class);
    }

}
