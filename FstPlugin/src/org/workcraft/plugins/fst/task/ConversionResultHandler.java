package org.workcraft.plugins.fst.task;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.workcraft.Framework;
import org.workcraft.gui.ExceptionDialog;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.fst.Fst;
import org.workcraft.plugins.fst.FstModelDescriptor;
import org.workcraft.plugins.shared.tasks.ExternalProcessResult;
import org.workcraft.tasks.DummyProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class ConversionResultHandler extends DummyProgressMonitor<ConversionResult> {
	private final ConversionTask task;

	public ConversionResultHandler(ConversionTask task) {
		this.task = task;
	}

	@Override
	public void finished(final Result<? extends ConversionResult> result, String description) {

		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run() {
				final Framework framework = Framework.getInstance();
				WorkspaceEntry we = task.getWorkspaceEntry();
				Path<String> path = we.getWorkspacePath();
				if (result.getOutcome() == Outcome.FINISHED) {
					Fst model = result.getReturnValue().getResult();
					final Workspace workspace = framework.getWorkspace();
					final Path<String> directory = path.getParent();
					final String name = FileUtils.getFileNameWithoutExtension(new File(path.getNode()));;
					final ModelEntry me = new ModelEntry(new FstModelDescriptor() , model);
					workspace.add(directory, name, me, true, true);
				} else if (result.getOutcome() != Outcome.CANCELLED) {
					MainWindow mainWindow = framework.getMainWindow();
					if (result.getCause() == null) {
						Result<? extends ExternalProcessResult> petrifyResult = result.getReturnValue().getPetrifyResult();
						JOptionPane.showMessageDialog(mainWindow,
								"Petrify output: \n\n" + new String(petrifyResult.getReturnValue().getErrors()),
								"Conversion failed", JOptionPane.WARNING_MESSAGE);
					} else {
						ExceptionDialog.show(mainWindow, result.getCause());
					}
				}
			}
		});
	}
}
