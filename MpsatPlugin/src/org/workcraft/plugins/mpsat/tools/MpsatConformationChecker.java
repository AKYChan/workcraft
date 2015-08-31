package org.workcraft.plugins.mpsat.tools;

import java.io.File;

import javax.swing.JFileChooser;

import org.workcraft.Framework;
import org.workcraft.VerificationTool;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.mpsat.tasks.MpsatConformationTask;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class MpsatConformationChecker extends VerificationTool {

	@Override
	public String getDisplayName() {
		return "Conformation (without dummies) [MPSat]...";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return WorkspaceUtils.canHas(we, STGModel.class);
	}

	@Override
	public final void run(WorkspaceEntry we) {
		final Framework framework = Framework.getInstance();
		JFileChooser fc = framework.getMainWindow().createOpenDialog("Open environment file", false, null);
		if (fc.showDialog(null, "Open") == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			if (framework.checkFile(file, null)) {
				final MpsatConformationTask mpsatTask = new MpsatConformationTask(we, file);

				String description = "MPSat tool chain";
				String title = we.getTitle();
				if (!title.isEmpty()) {
					description += "(" + title +")";
				}
				MpsatChainResultHandler monitor = new MpsatChainResultHandler(mpsatTask);

				framework.getTaskManager().queue(mpsatTask, description, monitor);
			}
		}
	}
}
