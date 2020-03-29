package org.workcraft.plugins.mpsat.tasks;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.Toolbox;
import org.workcraft.plugins.mpsat.MpsatVerificationSettings;
import org.workcraft.plugins.stg.tools.Core;
import org.workcraft.plugins.stg.tools.EncodingConflictAnalyserTool;
import org.workcraft.shared.ColorGenerator;
import org.workcraft.tasks.AbstractOutputInterpreter;
import org.workcraft.traces.Solution;
import org.workcraft.utils.ColorUtils;
import org.workcraft.utils.DialogUtils;
import org.workcraft.utils.LogUtils;
import org.workcraft.utils.TraceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

final class EncodingConflictOutputHandler extends AbstractOutputInterpreter<VerificationOutput, Boolean> {

    private final ColorGenerator colorGenerator = new ColorGenerator(ColorUtils.getHsbPalette(
            new float[]{0.45f, 0.15f, 0.70f, 0.25f, 0.05f, 0.80f, 0.55f, 0.20f, 075f, 0.50f},
            new float[]{0.30f}, new float[]{0.9f, 0.7f, 0.5f}));

    EncodingConflictOutputHandler(WorkspaceEntry we, VerificationOutput output, boolean interactive) {
        super(we, output, interactive);
    }

    @Override
    public Boolean interpret() {
        if (getOutput() == null) {
            return null;
        }
        String mpsatStdout = getOutput().getStdoutString();
        VerificationOutputParser mdp = new VerificationOutputParser(mpsatStdout);
        List<Solution> solutions = mdp.getSolutions();
        boolean propertyHolds = !TraceUtils.hasTraces(solutions);
        if (propertyHolds) {
            String msg = "No encoding conflicts.";
            if (isInteractive()) {
                DialogUtils.showInfo(msg, "Verification results");
            } else {
                LogUtils.logInfo(msg);
            }
        } else {
            ArrayList<Core> cores = new ArrayList<>(convertSolutionsToCores(solutions));
            if (isInteractive()) {
                Collections.sort(cores, (c1, c2) -> {
                    if (c1.size() > c2.size()) return 1;
                    if (c1.size() < c2.size()) return -1;
                    if (c1.toString().length() > c2.toString().length()) return 1;
                    if (c1.toString().length() < c2.toString().length()) return -1;
                    return 0;
                });
                final MainWindow mainWindow = Framework.getInstance().getMainWindow();
                final Toolbox toolbox = mainWindow.getEditor(getWorkspaceEntry()).getToolBox();
                final EncodingConflictAnalyserTool tool = toolbox.getToolInstance(EncodingConflictAnalyserTool.class);
                toolbox.selectTool(tool);
                tool.setCores(cores);
            }
        }
        return propertyHolds;
    }

    private LinkedHashSet<Core> convertSolutionsToCores(List<Solution> solutions) {
        LinkedHashSet<Core> cores = new LinkedHashSet<>();
        for (Solution solution: solutions) {
            Core core = new Core(solution.getMainTrace(), solution.getBranchTrace(), solution.getComment());
            boolean isDuplicateCore = cores.contains(core);
            if (!isDuplicateCore) {
                core.setColor(colorGenerator.updateColor());
                cores.add(core);
            }
            if (MpsatVerificationSettings.getDebugCores()) {
                if (solution.getComment() == null) {
                    LogUtils.logMessage("Encoding conflict:");
                } else {
                    LogUtils.logMessage("Encoding conflict for signal '" + solution.getComment() + "':");
                }
                LogUtils.logMessage("    Configuration 1: " + solution.getMainTrace());
                LogUtils.logMessage("    Configuration 2: " + solution.getBranchTrace());
                LogUtils.logMessage("    Conflict core" + (isDuplicateCore ? " (duplicate)" : "") + ": " + core);
                LogUtils.logMessage("");
            }
        }
        return cores;
    }

}
