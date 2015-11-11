package org.workcraft.plugins.petrify.tools;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.plugins.petrify.tasks.TransformationResultHandler;
import org.workcraft.plugins.petrify.tasks.TransformationTask;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class PetrifyCscConflictResolution implements Tool {

	@Override
	public String getSection() {
		return "Encoding conflicts";
	}

	@Override
	public String getDisplayName() {
		return "Resolve CSC conflicts [Petrify]";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return WorkspaceUtils.canHas(we, STGModel.class);
	}

	@Override
	public void run(WorkspaceEntry we) {
		final TransformationTask task = new TransformationTask(we, "CSC conflicts resolution", new String[] {"-csc"});
		final Framework framework = Framework.getInstance();
		framework.getTaskManager().queue(task, "Petrify CSC conflicts resolution", new TransformationResultHandler(we));
	}
}
