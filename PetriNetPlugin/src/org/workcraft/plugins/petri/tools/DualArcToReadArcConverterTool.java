package org.workcraft.plugins.petri.tools;

import java.util.HashSet;
import java.util.LinkedList;

import org.workcraft.TransformationTool;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.PetriNetUtils;
import org.workcraft.plugins.petri.VisualReadArc;
import org.workcraft.util.Pair;
import org.workcraft.workspace.WorkspaceEntry;

public class DualArcToReadArcConverterTool extends TransformationTool {

	@Override
	public String getDisplayName() {
		return "Convert selected dual producing/consuming arcs to read-arcs";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return we.getModelEntry().getMathModel() instanceof PetriNetModel;
	}

	@Override
	public Position getPosition() {
		return Position.TOP;
	}

	@Override
	public void run(WorkspaceEntry we) {
		final VisualModel visualModel = we.getModelEntry().getVisualModel();
		HashSet<Pair<VisualConnection, VisualConnection>> dualArcs = PetriNetUtils.getSelectedDualArcs(visualModel);
		if ( !dualArcs.isEmpty() ) {
			we.saveMemento();
			HashSet<VisualReadArc> readArcs = PetriNetUtils.convertDualArcsToReadArcs(visualModel, dualArcs);
			visualModel.select(new LinkedList<Node>(readArcs));
		}
	}

}
