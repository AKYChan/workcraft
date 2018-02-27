package org.workcraft.plugins.mpsat.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.Toolbox;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.stg.tools.Core;
import org.workcraft.plugins.stg.tools.EncodingConflictAnalyserTool;
import org.workcraft.util.ColorGenerator;
import org.workcraft.util.ColorUtils;
import org.workcraft.util.DialogUtils;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;

final class MpsatEncodingConflictOutputHandler implements Runnable {

    private final ColorGenerator colorGenerator = new ColorGenerator(ColorUtils.getHsbPalette(
            new float[]{0.45f, 0.15f, 0.70f, 0.25f, 0.05f, 0.80f, 0.55f, 0.20f, 075f, 0.50f},
            new float[]{0.30f}, new float[]{0.9f, 0.7f, 0.5f}));

    private final WorkspaceEntry we;
    private final MpsatOutput output;

    MpsatEncodingConflictOutputHandler(WorkspaceEntry we, MpsatOutput output) {
        this.we = we;
        this.output = output;
    }

    @Override
    public void run() {
        MpsatOutoutParser mdp = new MpsatOutoutParser(output);
        List<MpsatSolution> solutions = mdp.getSolutions();
        final Framework framework = Framework.getInstance();
        if (!MpsatUtils.hasTraces(solutions)) {
            DialogUtils.showInfo("No encoding conflicts.", "Verification results");
        } else if (framework.isInGuiMode()) {
            final MainWindow mainWindow = framework.getMainWindow();
            GraphEditorPanel currentEditor = mainWindow.getEditor(we);
            final Toolbox toolbox = currentEditor.getToolBox();
            final EncodingConflictAnalyserTool tool = toolbox.getToolInstance(EncodingConflictAnalyserTool.class);
            toolbox.selectTool(tool);
            ArrayList<Core> cores = new ArrayList<>(convertSolutionsToCores(solutions));
            Collections.sort(cores, new Comparator<Core>() {
                @Override
                public int compare(Core c1, Core c2) {
                    if (c1.size() > c2.size()) return 1;
                    if (c1.size() < c2.size()) return -1;
                    if (c1.toString().length() > c2.toString().length()) return 1;
                    if (c1.toString().length() < c2.toString().length()) return -1;
                    return 0;
                }
            });
            tool.setCores(cores);
        }
    }

    private LinkedHashSet<Core> convertSolutionsToCores(List<MpsatSolution> solutions) {
        LinkedHashSet<Core> cores = new LinkedHashSet<>();
        for (MpsatSolution solution: solutions) {
            Core core = new Core(solution.getMainTrace(), solution.getBranchTrace(), solution.getComment());
            boolean isDuplicateCore = cores.contains(core);
            if (!isDuplicateCore) {
                core.setColor(colorGenerator.updateColor());
                cores.add(core);
            }
            if (MpsatSettings.getDebugCores()) {
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
