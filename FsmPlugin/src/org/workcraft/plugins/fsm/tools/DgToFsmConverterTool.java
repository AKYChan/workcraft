package org.workcraft.plugins.fsm.tools;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.gui.workspace.Path;
import org.workcraft.plugins.fsm.Fsm;
import org.workcraft.plugins.fsm.FsmModelDescriptor;
import org.workcraft.plugins.fsm.VisualFsm;
import org.workcraft.plugins.graph.Graph;
import org.workcraft.plugins.graph.VisualGraph;
import org.workcraft.plugins.shared.CommonEditorSettings;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.Workspace;
import org.workcraft.workspace.WorkspaceEntry;

public class DgToFsmConverterTool implements Tool {

	@Override
	public String getDisplayName() {
		return "Finite State Machine";
	}

	@Override
	public String getSection() {
		return "Conversion";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return (we.getModelEntry().getMathModel() instanceof Graph);
	}

	@Override
	public void run(WorkspaceEntry we) {
		final VisualGraph dg = (VisualGraph)we.getModelEntry().getVisualModel();
		final VisualFsm fsm = new VisualFsm(new Fsm());
		final DgToFsmConverter converter = new DgToFsmConverter(dg, fsm);
		final Framework framework = Framework.getInstance();
		final Workspace workspace = framework.getWorkspace();
		final Path<String> directory = we.getWorkspacePath().getParent();
		final String desiredName = we.getWorkspacePath().getNode();
		final ModelEntry me = new ModelEntry(new FsmModelDescriptor(), converter.getDstModel());
		boolean openInEditor = (me.isVisual() || CommonEditorSettings.getOpenNonvisual());
		workspace.add(directory, desiredName, me, false, openInEditor);
	}
}
