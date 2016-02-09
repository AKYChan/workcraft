/**
 *
 */
package org.workcraft.plugins.mpsat;

import java.util.List;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.plugins.mpsat.gui.ReachibilityDialog;
import org.workcraft.plugins.mpsat.gui.Solution;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.tasks.Result;
import org.workcraft.util.GUI;
import org.workcraft.workspace.WorkspaceEntry;

final class MpsatDeadlockResultHandler implements Runnable {

    private final WorkspaceEntry we;
    private final Result<? extends ExternalProcessResult> result;

    MpsatDeadlockResultHandler(WorkspaceEntry we, Result<? extends ExternalProcessResult> result) {
        this.we = we;
        this.result = result;
    }

    @Override
    public void run() {
        MpsatResultParser mdp = new MpsatResultParser(result.getReturnValue());
        List<Solution> solutions = mdp.getSolutions();
        String title = "Verification results";
        if (solutions.isEmpty()) {
            String message = "The system is deadlock-free.";
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        } else if ( !Solution.hasTraces(solutions) ) {
            String message = "The system has a deadlock.";
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
        } else {
            String message = "<html><br>&#160;The system has a deadlock after the following trace(s):<br><br></html>";
            final ReachibilityDialog solutionsDialog = new ReachibilityDialog(we, title, message, solutions);
            GUI.centerToParent(solutionsDialog, Framework.getInstance().getMainWindow());
            solutionsDialog.setVisible(true);
        }
    }
}
