package org.workcraft.plugins.stg.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.hierarchy.NamespaceProvider;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.references.HierarchicalUniqueNameReferenceManager;
import org.workcraft.dom.references.NameManager;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.petri.tools.TransitionContractorTool;
import org.workcraft.plugins.stg.STG;
import org.workcraft.plugins.stg.STGPlace;
import org.workcraft.plugins.stg.VisualImplicitPlaceArc;
import org.workcraft.plugins.stg.VisualSTG;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.Pair;
import org.workcraft.workspace.WorkspaceEntry;

public class NamedTransitionContractorTool extends TransitionContractorTool {

	HashSet<VisualPlace> implicitPlaces = new HashSet<>();

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return we.getModelEntry().getMathModel() instanceof STG;
	}

	@Override
	public void beforeContraction(VisualModel visualModel, VisualTransition visualTransition) {
		super.beforeContraction(visualModel, visualTransition);
		implicitPlaces.clear();
		if (visualModel instanceof VisualSTG) {
			VisualSTG visualStg = (VisualSTG)visualModel;
			Set<Connection> adjacentConnections = new HashSet<>(visualModel.getConnections(visualTransition));
			for (Connection connection: adjacentConnections) {
				if (connection instanceof VisualImplicitPlaceArc) {
					VisualPlace formerImplicitPlace = visualStg.makeExplicit((VisualImplicitPlaceArc)connection);
					implicitPlaces.add(formerImplicitPlace);
				}
			}
		}
	}

	@Override
	public void afterContraction(VisualModel visualModel, VisualTransition visualTransition,
			HashMap<VisualPlace, Pair<VisualPlace, VisualPlace>> productPlaces) {
		super.afterContraction(visualModel, visualTransition, productPlaces);
		if (visualModel instanceof VisualSTG) {
			VisualSTG visualStg = (VisualSTG)visualModel;
			for (VisualPlace productPlace: productPlaces.keySet()) {
				Pair<VisualPlace, VisualPlace> originalPlacePair = productPlaces.get(productPlace);
				VisualPlace predPlace = originalPlacePair.getFirst();
				VisualPlace succPlace = originalPlacePair.getSecond();
				if (implicitPlaces.contains(predPlace) && implicitPlaces.contains(succPlace)) {
					visualStg.maybeMakeImplicit(productPlace, true);
				}
			}
		}
	}

	@Override
	public VisualPlace createProductPlace(VisualModel visualModel, VisualPlace predPlace, VisualPlace succPlace) {
		Container visualContainer = (Container)Hierarchy.getCommonParent(predPlace, succPlace);
		Container mathContainer = NamespaceHelper.getMathContainer(visualModel, visualContainer);
		MathModel mathModel = visualModel.getMathModel();
		HierarchicalUniqueNameReferenceManager refManager = (HierarchicalUniqueNameReferenceManager)mathModel.getReferenceManager();
		NameManager nameManagerer = refManager.getNameManager((NamespaceProvider)mathContainer);
		String predName = visualModel.getMathName(predPlace);
		String succName = visualModel.getMathName(succPlace);
		String productName = nameManagerer.getDerivedName(null, predName + succName);
		STGPlace mathPlace = mathModel.createNode(productName, mathContainer, STGPlace.class);
		return visualModel.createVisualComponent(mathPlace, visualContainer, VisualPlace.class);
	}

}
