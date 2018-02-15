package org.workcraft.plugins.mpsat.tasks;

import java.util.List;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.mpsat.MpsatResultParser;
import org.workcraft.plugins.mpsat.MpsatSolution;
import org.workcraft.plugins.mpsat.MpsatUtils;
import org.workcraft.plugins.mpsat.gui.MpsatReachibilityDialog;
import org.workcraft.plugins.shared.tasks.ExternalProcessOutput;
import org.workcraft.tasks.Result;
import org.workcraft.util.DialogUtils;
import org.workcraft.util.GUI;
import org.workcraft.workspace.WorkspaceEntry;

final class MpsatDeadlockResultHandler implements Runnable {
    private static final String TITLE = "Verification results";
    private final WorkspaceEntry we;
    private final Result<? extends ExternalProcessOutput> result;

    MpsatDeadlockResultHandler(WorkspaceEntry we, Result<? extends ExternalProcessOutput> result) {
        this.we = we;
        this.result = result;
    }

    @Override
    public void run() {
        Framework framework = Framework.getInstance();
        MpsatResultParser mdp = new MpsatResultParser(result.getPayload());
        List<MpsatSolution> solutions = mdp.getSolutions();
        if (solutions.isEmpty()) {
            DialogUtils.showInfo("The system is deadlock-free.", TITLE);
        } else if (!MpsatUtils.hasTraces(solutions)) {
            DialogUtils.showWarning("The system has a deadlock.", TITLE);
        } else if (framework.isInGuiMode()) {
            String message = "<html><br>&#160;The system has a deadlock after the following trace(s):<br><br></html>";
            MpsatReachibilityDialog solutionsDialog = new MpsatReachibilityDialog(we, TITLE, message, solutions);
            MainWindow mainWindow = framework.getMainWindow();
            GUI.centerToParent(solutionsDialog, mainWindow);
            solutionsDialog.setVisible(true);
        }
    }

}
