package org.workcraft.plugins.fsm;

import org.workcraft.annotations.VisualClass;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.AbstractMathModel;
import org.workcraft.dom.references.HierarchicalUniqueNameReferenceManager;
import org.workcraft.observation.HierarchyEvent;
import org.workcraft.observation.HierarchySupervisor;
import org.workcraft.observation.NodesDeletingEvent;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.observation.StateEvent;
import org.workcraft.observation.StateSupervisor;
import org.workcraft.serialisation.References;
import org.workcraft.util.Func;
import org.workcraft.util.Hierarchy;

@VisualClass(org.workcraft.plugins.fsm.VisualFsm.class)
public class Fsm extends AbstractMathModel {

	public Fsm() {
		this(null, null);
	}

	public Fsm(Container root) {
		this(root, null);
	}

	public Fsm(Container root, References refs) {
		super(root, new HierarchicalUniqueNameReferenceManager(refs, new Func<Node, String>() {
			@Override
			public String eval(Node arg) {
				if (arg instanceof State) return "s";
				if (arg instanceof Event) return "t";
				return "node";
			}
		}));

		// Move the initial property to another state on state removal
		new HierarchySupervisor() {
			@Override
			public void handleEvent(HierarchyEvent e) {
				if (e instanceof NodesDeletingEvent) {
					for (Node node: e.getAffectedNodes()) {
						if (node instanceof State) {
							handleStateRemoval((State)node);
						}
					}
				}
			}
		}.attach(getRoot());

		// Update all the states on a change of the initial property
		new StateSupervisor() {
			@Override
			public void handleEvent(StateEvent e) {
				if (e instanceof PropertyChangedEvent) {
					Object object = e.getSender();
					if (object instanceof State) {
						PropertyChangedEvent pce = (PropertyChangedEvent)e;
						if (pce.getPropertyName().equals("initial")) {
							handleStateChange((State)object);
						}
					}
				}
			}
		}.attach(getRoot());
	}

	private void handleStateRemoval(State state) {
		if (state.isInitial()) {
			for (State s: Hierarchy.getChildrenOfType(state.getParent(), State.class)) {
				if (s != state) {
					s.setInitial(true);
					break;
				}
			}
		}
	}

	private void handleStateChange(State state) {
		for (State s: Hierarchy.getChildrenOfType(state.getParent(), State.class)) {
			if (s != state) {
				if (state.isInitial()) {
					s.setInitial(false);
				} else {
					s.setInitial(true);
					break;
				}
			}
		}
	}

	public Event connect(State first, State second) {
		Event con = new Event(first, second);
		Hierarchy.getNearestContainer(first, second).add(con);
		return con;
	}

}
