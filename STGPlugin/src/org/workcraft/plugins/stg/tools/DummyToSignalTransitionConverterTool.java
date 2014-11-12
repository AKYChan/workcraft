package org.workcraft.plugins.stg.tools;

import java.util.HashSet;
import java.util.LinkedList;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.stg.STG;
import org.workcraft.plugins.stg.SignalTransition.Direction;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.VisualDummyTransition;
import org.workcraft.plugins.stg.VisualSTG;
import org.workcraft.plugins.stg.VisualSignalTransition;
import org.workcraft.workspace.WorkspaceEntry;

public class DummyToSignalTransitionConverterTool implements Tool {
	private final Framework framework;

	public DummyToSignalTransitionConverterTool(Framework framework) {
		this.framework = framework;
	}

	@Override
	public String getDisplayName() {
		return "Convert selected dummies to signal transitions";
	}

	@Override
	public String getSection() {
		return "Transformations";
	}

	@Override
	public boolean isApplicableTo(WorkspaceEntry we) {
		return we.getModelEntry().getMathModel() instanceof STG;
	}

	@Override
	public void run(WorkspaceEntry we) {
		final VisualSTG stg = (VisualSTG)we.getModelEntry().getVisualModel();
		HashSet<VisualDummyTransition> dummyTransitions = new HashSet<VisualDummyTransition>(stg.getVisualDummyTransitions());
		dummyTransitions.retainAll(stg.getSelection());
		if (!dummyTransitions.isEmpty()) {
			we.saveMemento();
			HashSet<VisualSignalTransition> signalTransitions = new HashSet<VisualSignalTransition>(dummyTransitions.size());
			for (VisualTransition dummyTransition: dummyTransitions) {
				VisualSignalTransition signalTransition = convertDummyToSignalTransition(stg, dummyTransition);
				signalTransitions.add(signalTransition);
			}
			stg.select(new LinkedList<Node>(signalTransitions));
			stg.remove(new LinkedList<Node>(dummyTransitions));
		}
	}

	private VisualSignalTransition convertDummyToSignalTransition(VisualSTG stg, VisualTransition dummyTransition) {
		Container container = (Container)dummyTransition.getParent();
		VisualSignalTransition signalTransition = stg.createSignalTransition(null, Type.INTERNAL, Direction.TOGGLE, container);
		signalTransition.setPosition(dummyTransition.getPosition());
		for (Node pred: stg.getPreset(dummyTransition)) {
			try {
				VisualConnection oldPredConnection = (VisualConnection)stg.getConnection(pred, dummyTransition);
				VisualConnection newPredConnection = stg.connect(pred, signalTransition);
				oldPredConnection.copyProperties(newPredConnection);
				oldPredConnection.copyGeometry(newPredConnection);
			} catch (InvalidConnectionException e) {
				e.printStackTrace();
			}
		}
		for (Node succ: stg.getPostset(dummyTransition)) {
			try {
				VisualConnection oldSuccConnection = (VisualConnection)stg.getConnection(dummyTransition, succ);
				VisualConnection newSuccConnection = stg.connect(signalTransition, succ);
				oldSuccConnection.copyProperties(newSuccConnection);
				oldSuccConnection.copyGeometry(newSuccConnection);
			} catch (InvalidConnectionException e) {
				e.printStackTrace();
			}
		}
		return signalTransition;
	}

}
