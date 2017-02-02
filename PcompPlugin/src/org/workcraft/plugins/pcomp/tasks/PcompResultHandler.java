package org.workcraft.plugins.pcomp.tasks;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.tasks.DummyProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class PcompResultHandler extends DummyProgressMonitor<ExternalProcessResult> {
    private final boolean showInEditor;
    private final File outputFile;

    public PcompResultHandler(boolean showInEditor, File outputFile) {
        this.showInEditor = showInEditor;
        this.outputFile = outputFile;
    }

    @Override
    public void finished(final Result<? extends ExternalProcessResult> result, String description) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {

                    final Framework framework = Framework.getInstance();
                    MainWindow mainWindow = framework.getMainWindow();
                    if (result.getOutcome() == Outcome.FAILED) {
                        String message;
                        if (result.getCause() != null) {
                            message = result.getCause().getMessage();
                            result.getCause().printStackTrace();
                        } else {
                            message = "Pcomp errors:\n" + result.getReturnValue().getErrorsHeadAndTail();
                        }
                        JOptionPane.showMessageDialog(mainWindow, message, "Parallel composition failed", JOptionPane.ERROR_MESSAGE);
                    } else if (result.getOutcome() == Outcome.FINISHED) {
                        try {
                            if (showInEditor) {
                                WorkspaceEntry we = framework.loadWork(outputFile);
                                mainWindow.createEditorWindow(we);
                            } else {
                                final Workspace workspace = framework.getWorkspace();
                                Path<String> path = Path.fromString(outputFile.getName());
                                workspace.addMount(path, outputFile, true);
                            }
                        } catch (DeserialisationException e) {
                            JOptionPane.showMessageDialog(mainWindow, e.getMessage(), "Parallel composition failed", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
