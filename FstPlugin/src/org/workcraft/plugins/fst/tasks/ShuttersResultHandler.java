package org.workcraft.plugins.fst.tasks;

import java.io.File;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.tasks.BasicProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.util.FileUtils;

public class ShuttersResultHandler extends BasicProgressMonitor<ShuttersResult>  {

    private final File tmpDir;

    public ShuttersResultHandler(ShuttersTask shuttersTask, File tmpDir) {
        this.tmpDir = tmpDir;
    }

    @Override
    public void finished(Result<? extends ShuttersResult> result) {
        super.finished(result);
        if (result.getOutcome() == Outcome.SUCCESS) {
            System.out.println(result.getReturnValue().getStdout());
        } else if (result.getOutcome() == Outcome.FAILURE) {
            FileUtils.deleteOnExitRecursively(tmpDir);
            String errorMessage = result.getReturnValue().getError();
            Framework framework = Framework.getInstance();
            JOptionPane.showMessageDialog(framework.getMainWindow(), errorMessage, "Shutters error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
