package org.workcraft.plugins.mpsat;

import java.util.LinkedList;
import java.util.List;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.Toolbox;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.gui.graph.tools.SimulationTool;
import org.workcraft.plugins.mpsat.tasks.MpsatChainResult;
import org.workcraft.plugins.mpsat.tasks.MpsatCombinedChainResult;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatUtils {

    public static List<MpsatSolution> getCombinedChainSolutions(Result<? extends MpsatCombinedChainResult> combinedChainResult) {
        LinkedList<MpsatSolution> solutions = null;
        if ((combinedChainResult != null) && (combinedChainResult.getOutcome() == Outcome.SUCCESS)) {
            solutions = new LinkedList<>();
            MpsatCombinedChainResult returnValue = combinedChainResult.getReturnValue();
            if (returnValue != null) {
                List<Result<? extends ExternalProcessResult>> mpsatResultList = returnValue.getMpsatResultList();
                for (int index = 0; index < mpsatResultList.size(); ++index) {
                    Result<? extends ExternalProcessResult> mpsatResult = mpsatResultList.get(index);
                    solutions.addAll(getSolutions(mpsatResult));
                }
            }
        }
        return solutions;
    }

    public static List<MpsatSolution> getChainSolutions(Result<? extends MpsatChainResult> chainResult) {
        LinkedList<MpsatSolution> solutions = null;
        if ((chainResult != null) && (chainResult.getOutcome() == Outcome.SUCCESS)) {
            solutions = new LinkedList<>();
            MpsatChainResult returnValue = chainResult.getReturnValue();
            if (returnValue != null) {
                Result<? extends ExternalProcessResult> mpsatResult = returnValue.getMpsatResult();
                if (mpsatResult != null) {
                    solutions.addAll(getSolutions(mpsatResult));
                }
            }
        }
        return solutions;
    }

    public static List<MpsatSolution> getSolutions(Result<? extends ExternalProcessResult> result) {
        LinkedList<MpsatSolution> solutions = null;
        if ((result != null) && (result.getOutcome() == Outcome.SUCCESS)) {
            solutions = new LinkedList<>();
            ExternalProcessResult returnValue = result.getReturnValue();
            if (returnValue != null) {
                solutions.addAll(getSolutions(returnValue));
            }
        }
        return solutions;
    }

    public static List<MpsatSolution> getSolutions(ExternalProcessResult value) {
        MpsatResultParser mdp = new MpsatResultParser(value);
        return mdp.getSolutions();
    }

    public static Boolean getCombinedChainOutcome(Result<? extends MpsatCombinedChainResult> combinedChainResult) {
        List<MpsatSolution> solutions = getCombinedChainSolutions(combinedChainResult);
        if (solutions != null) {
            return !hasTraces(solutions);
        }
        return null;
    }

    public static Boolean getChainOutcome(Result<? extends MpsatChainResult> chainResult) {
        List<MpsatSolution> solutions = getChainSolutions(chainResult);
        if (solutions != null) {
            return !hasTraces(solutions);
        }
        return null;
    }

    public static boolean hasTraces(List<MpsatSolution> solutions) {
        if (solutions != null) {
            for (MpsatSolution solution : solutions) {
                if ((solution.getMainTrace() != null) || (solution.getBranchTrace() != null)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static  String getToolchainDescription(String title) {
        return "MPSat tool chain" + (title.isEmpty() ? "" : " (" + title + ")");
    }

    public static void playTrace(WorkspaceEntry we, MpsatSolution solution) {
        final Framework framework = Framework.getInstance();
        final MainWindow mainWindow = framework.getMainWindow();
        GraphEditorPanel editor = mainWindow.getEditor(we);
        final Toolbox toolbox = editor.getToolBox();
        final SimulationTool tool = toolbox.getToolInstance(SimulationTool.class);
        toolbox.selectTool(tool);
        tool.setTrace(solution.getMainTrace(), solution.getBranchTrace(), editor);
        String comment = solution.getComment();
        if ((comment != null) && !comment.isEmpty()) {
            comment = comment.replaceAll("\\<.*?>", "");
            LogUtils.logWarning(comment);
        }
    }

}
