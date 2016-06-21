package org.workcraft.plugins.petrify.tools;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.gui.DesktopApi;
import org.workcraft.gui.MainWindow;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petrify.tasks.DrawSgResult;
import org.workcraft.plugins.petrify.tasks.DrawSgTask;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class ShowSg implements Tool {
    private static final String TITLE = "State graph synthesis";
    private static final String ERROR_CAUSE_PREFIX = "\n\n";

    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, PetriNetModel.class);
    }

    @Override
    public String getSection() {
        return "External visualiser";
    }

    @Override
    public String getDisplayName() {
        return isBinary() ? "State graph (binary-encoded) [write_sg + draw_astg]" : "State graph (basic) [write_sg + draw_astg]";
    }

    @Override
    public void run(WorkspaceEntry we) {
        DrawSgTask task = new DrawSgTask(we, isBinary());
        final Framework framework = Framework.getInstance();

        ProgressMonitor<DrawSgResult> monitor = new ProgressMonitor<DrawSgResult>() {
            @Override
            public void progressUpdate(double completion) {
            }

            @Override
            public void stdout(byte[] data) {
            }

            @Override
            public void stderr(byte[] data) {
            }

            @Override
            public boolean isCancelRequested() {
                return false;
            }

            @Override
            public void finished(Result<? extends DrawSgResult> result, String description) {
                if (result.getOutcome() == Outcome.FINISHED) {
                    DesktopApi.open(result.getReturnValue().getFile());
                } else  if (result.getOutcome() != Outcome.CANCELLED) {
                    String errorMessage = "Petrify tool chain execution failed.";
                    Throwable cause = result.getCause();
                    if (cause != null) {
                        errorMessage += ERROR_CAUSE_PREFIX + cause.toString();
                    } else {
                        DrawSgResult returnValue = result.getReturnValue();
                        if (returnValue != null) {
                            errorMessage += ERROR_CAUSE_PREFIX + returnValue.getErrorMessages();
                        }
                    }
                    MainWindow mainWindow = Framework.getInstance().getMainWindow();
                    JOptionPane.showMessageDialog(mainWindow, errorMessage, TITLE, JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        framework.getTaskManager().queue(task, "Show state graph", monitor);
    }

}
