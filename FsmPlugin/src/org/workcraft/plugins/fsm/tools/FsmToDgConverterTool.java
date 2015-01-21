package org.workcraft.plugins.fsm.tools;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.fsm.Fsm;
import org.workcraft.plugins.fsm.VisualFsm;
import org.workcraft.plugins.graph.Graph;
import org.workcraft.plugins.graph.GraphModelDescriptor;
import org.workcraft.plugins.graph.VisualGraph;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class FsmToDgConverterTool implements Tool {

	@Override
	public String getDisplayName() {
		return "Directed Graph";
	}

	@Override
	public String getSection() {
		return "Conversion";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return (we.getModelEntry().getMathModel() instanceof Fsm);
	}

	@Override
	public void run(WorkspaceEntry we) {
		final VisualFsm fsm = (VisualFsm)we.getModelEntry().getVisualModel();
		final VisualGraph dg = new VisualGraph(new Graph());
		final FsmToDgConverter converter = new FsmToDgConverter(fsm, dg);
		final Framework framework = Framework.getInstance();
		final Workspace workspace = framework.getWorkspace();
		final Path<String> directory = we.getWorkspacePath().getParent();
		final String desiredName = we.getWorkspacePath().getNode();
		final ModelEntry me = new ModelEntry(new GraphModelDescriptor(), converter.getDstModel());
		workspace.add(directory, desiredName, me, false, true);
	}
}
