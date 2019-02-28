package org.workcraft.plugins.mpsat.tasks;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.workcraft.gui.tools.Trace;
import org.workcraft.plugins.mpsat.MpsatParameters;
import org.workcraft.plugins.pcomp.ComponentData;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.tasks.ExportOutput;
import org.workcraft.plugins.stg.LabelParser;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.utils.LogUtils;
import org.workcraft.types.Triple;
import org.workcraft.workspace.WorkspaceEntry;

class MpsatConsistencyOutputHandler extends MpsatReachabilityOutputHandler {

    MpsatConsistencyOutputHandler(WorkspaceEntry we,
            ExportOutput exportOutput, PcompOutput pcompOutput, MpsatOutput mpsatOutput, MpsatParameters settings) {

        super(we, exportOutput, pcompOutput, mpsatOutput, settings);
    }

    @Override
    public List<MpsatSolution> processSolutions(WorkspaceEntry we, List<MpsatSolution> solutions) {
        List<MpsatSolution> result = new LinkedList<>();
        ComponentData data = getCompositionData(we);
        Map<String, String> substitutions = getSubstitutions(we);
        for (MpsatSolution solution: solutions) {
            LogUtils.logMessage("Processing reported trace: " + solution.getMainTrace());
            Trace trace = getProjectedTrace(solution.getMainTrace(), data, substitutions);
            int size = trace.size();
            if (size <= 0) {
                LogUtils.logMessage("No consistency violation detected");
            } else {
                String lastTransitionRef = trace.get(size - 1);
                final Triple<String, SignalTransition.Direction, Integer> r = LabelParser.parseSignalTransition(lastTransitionRef);
                if (r == null) continue;
                String signalRef = r.getFirst();
                String comment = "Signal '" + signalRef + "' is inconsistent";
                LogUtils.logWarning(comment + " after trace: " + trace);
                MpsatSolution processedSolution = new MpsatSolution(trace, null, comment);
                result.add(processedSolution);
            }
        }
        return result;
    }

}
