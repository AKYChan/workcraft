package org.workcraft.plugins.circuit.tools;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.tasks.CheckCircuitTask;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class CheckCircuitTool implements Tool {
	private final Framework framework;

	public CheckCircuitTool(Framework framework, Workspace ws) {
		this.framework = framework;
	}

	public String getDisplayName() {
		return "Check circuit for deadlocks and hazards (reuse unfolding data)";
	}


	@Override
	public String getSection() {
		return "Verification";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return we.getModelEntry().getMathModel() instanceof Circuit;
	}

	@Override
	public void run(WorkspaceEntry we) {
		final CheckCircuitTask task = new CheckCircuitTask(we, framework);
		String description = "MPSat tool chain";
		String title = we.getModelEntry().getModel().getTitle();
		if (!title.isEmpty()) {
			description += "(" + title +")";
		}
		framework.getTaskManager().queue(task, description, new MpsatChainResultHandler(task));
	}

}
