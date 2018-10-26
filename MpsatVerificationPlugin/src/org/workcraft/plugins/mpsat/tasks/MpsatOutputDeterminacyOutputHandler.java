package org.workcraft.plugins.mpsat.tasks;

import org.workcraft.Framework;
import org.workcraft.gui.graph.tools.Trace;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.mpsat.utils.EnablednessUtils;
import org.workcraft.plugins.pcomp.ComponentData;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.utils.PetriUtils;
import org.workcraft.plugins.shared.tasks.ExportOutput;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

import java.util.*;

class MpsatOutputDeterminacyOutputHandler extends MpsatReachabilityOutputHandler {

    MpsatOutputDeterminacyOutputHandler(WorkspaceEntry we, ExportOutput exportOutput,
            PcompOutput pcompOutput, MpsatOutput mpsatOutput, MpsatParameters settings) {

        super(we, exportOutput, pcompOutput, mpsatOutput, settings);
    }

    @Override
    public List<MpsatSolution> processSolutions(WorkspaceEntry we, List<MpsatSolution> solutions) {
        List<MpsatSolution> result = new LinkedList<>();

        StgModel compStg = getMpsatOutput().getInputStg();
        StgModel stg = getSrcStg(we);
        ComponentData devData = getCompositionData(0);
        ComponentData envData = getCompositionData(1);
        Map<String, String> substitutions = getSubstitutions(we);

        for (MpsatSolution solution: solutions) {
            Trace compTrace = solution.getMainTrace();
            LogUtils.logMessage("Violation trace of the auto-composition: " + compTrace.toText());

            Trace devTrace = getProjectedTrace(compTrace, devData, substitutions);
            Trace envTrace = getProjectedTrace(compTrace, envData, substitutions);
            LogUtils.logMessage("Projected pair of traces:\n    " + devTrace.toText() + "\n    " + envTrace.toText());

            Enabledness compEnabledness = EnablednessUtils.getOutputEnablednessAfterTrace(compStg, compTrace);
            MpsatSolution projectedSolution = new MpsatSolution(devTrace, envTrace);
            MpsatSolution processedSolution = processSolution(stg, projectedSolution, compEnabledness);
            if (processedSolution != null) {
                result.add(processedSolution);
            }
        }
        return result;
    }

    private MpsatSolution processSolution(StgModel stg, MpsatSolution solution, Enabledness compEnabledness) {
        // Execute trace to potentially interesting state
        HashMap<Place, Integer> marking = PetriUtils.getMarking(stg);
        Trace trace = solution.getMainTrace();
        if (!PetriUtils.fireTrace(stg, trace)) {
            PetriUtils.setMarking(stg, marking);
            throw new RuntimeException("Cannot execute projected trace: " + trace.toText());
        }
        // Check if any output can be fired that is not enabled in the composition
        HashSet<String> suspiciousSignals = EnablednessUtils.getEnabledSignals(stg, Signal.Type.OUTPUT);
        suspiciousSignals.retainAll(compEnabledness.getUnknownSet());
        if (suspiciousSignals.size() == 1) {
            compEnabledness.alter(Collections.emptySet(), suspiciousSignals, Collections.emptySet());
        }

        SignalTransition problematicTransition = null;
        for (SignalTransition transition: stg.getSignalTransitions(Signal.Type.OUTPUT)) {
            String signalRef = stg.getSignalReference(transition);
            if (stg.isEnabled(transition) && compEnabledness.isDisabled(signalRef)) {
                problematicTransition = transition;
                break;
            }
        }
        String comment = null;
        if (problematicTransition != null) {
            String ref = stg.getSignalReference(problematicTransition);
            LogUtils.logWarning("Output '" + ref + "' is non-deterministically enabled");
            comment = "Non-deterministic enabling of output '" + ref + "'";
        } else if (!suspiciousSignals.isEmpty()) {
            String refs = String.join(", ", suspiciousSignals);
            LogUtils.logWarning("One of these outputs is non-deterministically enabled (via internal signals or dummies):\n" + refs);
            comment = "Non-deterministic enabling of one of the outputs: " + refs;
        }
        PetriUtils.setMarking(stg, marking);
        return new MpsatSolution(solution.getMainTrace(), solution.getBranchTrace(), comment);
    }

    @Override
    public StgModel getSrcStg(WorkspaceEntry we) {
        Framework framework = Framework.getInstance();
        ModelEntry me = framework.cloneModel(we.getModelEntry());
        return WorkspaceUtils.getAs(me, StgModel.class);
    }

}
