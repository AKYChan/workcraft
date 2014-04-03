package org.workcraft.plugins.stg;

import java.util.Collection;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.references.ReferenceManager;
import org.workcraft.dom.references.UniqueNameManager;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.exceptions.DuplicateIDException;
import org.workcraft.observation.HierarchyEvent;
import org.workcraft.observation.HierarchySupervisor;
import org.workcraft.observation.NodesAddedEvent;
import org.workcraft.observation.NodesDeletedEvent;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.stg.SignalTransition.Direction;
import org.workcraft.serialisation.References;
import org.workcraft.util.Func;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.Identifier;
import org.workcraft.util.ListMap;
import org.workcraft.util.Pair;
import org.workcraft.util.Triple;

public class STGReferenceManager extends HierarchySupervisor implements ReferenceManager {
	private static final String inputTransitionName = "in";
	private static final String outputTransitionName = "out";
	private static final String internalTransitionName = "t";
	private static final String dummyTransitionName = "dum";

	private InstanceManager<Node> instancedNameManager;
	private UniqueNameManager<Node> defaultNameManager;
	private References existingReferences;
	private ListMap<String, SignalTransition> signalTransitions = new ListMap<String, SignalTransition>();
	private ListMap<String, DummyTransition> dummyTransitions = new ListMap<String, DummyTransition>();

	public STGReferenceManager(References existingReferences) {
		this.existingReferences = existingReferences;

		this.defaultNameManager = new UniqueNameManager<Node>(new Func<Node, String>() {
			@Override
			public String eval(Node arg) {
				if (arg instanceof STGPlace) {
					return "p";
				}
				if (arg instanceof Connection) {
					return "con";
				}
				if (arg instanceof Container) {
					return "group";
				}
				if (arg instanceof SignalTransition) {
					switch ( ((SignalTransition)arg).getSignalType() ) {
					case INPUT: return inputTransitionName;
					case OUTPUT: return outputTransitionName;
					case INTERNAL: return internalTransitionName;
					}
				}
				if (arg instanceof DummyTransition) {
					return dummyTransitionName;
				}
				return "node";
			}
		});

		this.instancedNameManager = new InstanceManager<Node>(new Func<Node, String>() {
			@Override
			public String eval(Node arg) {
				if (arg instanceof SignalTransition) {
					return ((SignalTransition) arg).getSignalName() + ((SignalTransition) arg).getDirection();
				} else if (arg instanceof DummyTransition) {
					return ((DummyTransition)arg).getName();
				} else {
					throw new RuntimeException ("Unexpected class " + arg.getClass().getName());
				}
			}
		});
	}

	@Override
	public void attach(Node root) {
		if (root == null) {
			throw new NullPointerException();
		}

		if (existingReferences != null) {
			setExistingReference(root);
			for (Node n: Hierarchy.getDescendantsOfType(root, Node.class)) {
				setExistingReference(n);
			}
			existingReferences = null;
		}
		super.attach(root);
	}

	private void setExistingReference(Node n) {
		final String reference = existingReferences.getReference(n);
		if (reference != null) {
			if (n instanceof STGPlace) {
				if (! ((STGPlace) n).isImplicit()) {
					setName (n, reference);
				}
			} else {
				setName (n, reference);
			}
		}
	}

	@Override
	public Node getNodeByReference(String reference) {
		Pair<String, Integer> instancedName = LabelParser.parseInstancedTransition(reference);
		if (instancedName != null)	{
			if (instancedName.getSecond() == null) {
				instancedName = Pair.of(instancedName.getFirst(), 0);
			}
			Node node = instancedNameManager.getObject(instancedName);
			if (node != null) {
				return node;
			}
		}
		return defaultNameManager.get(reference);
	}

	@Override
	public String getNodeReference(Node node) {
		if (node instanceof SignalTransition) {
			final SignalTransition st = (SignalTransition)node;
			final Integer instance = instancedNameManager.getInstance(st).getSecond();
			if (instance == 0)
				return st.getSignalName() + st.getDirection();
			else
				return st.getSignalName() + st.getDirection() + "/" + instance;
		} else if (node instanceof Transition) {
			final Transition t = (Transition)node;
			final Pair<String, Integer> name = instancedNameManager.getInstance(t);
			if (name.getSecond() == 0) {
				return name.getFirst();
			} else {
				return name.getFirst() + "/" + name.getSecond();
			}
		}
		return defaultNameManager.getName(node);
	}

	public Pair<String, Integer> getNamePair(Node node) {
		if (node instanceof Transition)
			return instancedNameManager.getInstance(node);
		return null;
	}

	public String getName (Node node) {
		if (node instanceof Transition) {
			Pair<String, Integer> instance = instancedNameManager.getInstance(node);
			return instance.getFirst() + "/" + instance.getSecond();
		} else
			return defaultNameManager.getName(node);
	}

	public Collection<SignalTransition> getSignalTransitions(String signalName) {
		return signalTransitions.get(signalName);
	}

	public Collection<DummyTransition> getDummyTransitions(String name) {
		return dummyTransitions.get(name);
	}

	public int getInstanceNumber (Node st) {
		return instancedNameManager.getInstance(st).getSecond();
	}

	public void setInstanceNumber (Node st, int number) {
		instancedNameManager.assign(st, number);
	}

	public void setName(Node node, String s) {
		setName(node, s, false);
	}

	public void setName(Node node, String s, boolean forceInstance) {
		if (node instanceof SignalTransition) {
			final SignalTransition st = (SignalTransition)node;
			try {
				final Triple<String, Direction, Integer> r = LabelParser.parseSignalTransition(s);
				if (r == null) {
					throw new ArgumentException (s + " is not a valid signal transition label");
				}
				instancedNameManager.assign(st, Pair.of(r.getFirst()+r.getSecond(), r.getThird()), forceInstance);
				signalTransitions.remove(st.getSignalName(), st);
				signalTransitions.put(r.getFirst(), st);
				st.setSignalName(r.getFirst());
				st.setDirection(r.getSecond());
			} catch (DuplicateIDException e) {
				throw new ArgumentException ("Instance number " + e.getId() + " is already taken.");
			} catch (ArgumentException e) {
				if (Identifier.isValid(s)) {
					instancedNameManager.assign(st, s + st.getDirection());
					signalTransitions.remove(st.getSignalName(), st);
					signalTransitions.put(s, st);
					st.setSignalName(s);
				} else {
					throw new ArgumentException ("\"" + s + "\" is not a valid signal transition label.");
				}
			}
		} else if (node instanceof DummyTransition) {
			final DummyTransition dt = (DummyTransition)node;
			try {
				final Pair<String,Integer> r = LabelParser.parseDummyTransition(s);
				if (r==null) {
					throw new ArgumentException (s + " is not a valid transition label");
				}
				if (r.getSecond() != null) {
					instancedNameManager.assign(dt, r, forceInstance);
				} else {
					instancedNameManager.assign(dt, r.getFirst());
				}
				dummyTransitions.remove(dt.getName(), dt);
				dummyTransitions.put(r.getFirst(), dt);
				dt.setName(r.getFirst());
			} catch (DuplicateIDException e) {
				throw new ArgumentException ("Instance number " + e.getId() + " is already taken.");
			}
		}
		else {
			defaultNameManager.setName(node, s);
		}
	}

	@Override
	public void handleEvent(HierarchyEvent e) {
		if(e instanceof NodesDeletedEvent)
			for(Node node : e.getAffectedNodes()) {
				nodeRemoved(node);
				for (Node n : Hierarchy.getDescendantsOfType(node, Node.class))
					nodeRemoved(n);
			}
		if(e instanceof NodesAddedEvent)
			for(Node node : e.getAffectedNodes()) {
				setDefaultNameIfUnnamed(node);
				for (Node n : Hierarchy.getDescendantsOfType(node, Node.class))
					setDefaultNameIfUnnamed(n);
			}
	}

	private SignalTransition.Type getSignalType(String signalName) {
		for (SignalTransition st : getSignalTransitions(signalName)) {
			return st.getSignalType();
		}
		return null;
	}

	private boolean isSignalName(String name) {
		return !getSignalTransitions(name).isEmpty();
	}

	private boolean isDummyName(String name) {
		return !getDummyTransitions(name).isEmpty();
	}

	private boolean isGoodSignalName(String name, SignalTransition.Type type) {
		if (type == null) {
			return false;
		}
		if (isDummyName(name)) {
			return false;
		}
		if (isSignalName(name)) {
			SignalTransition.Type expectedType = getSignalType(name);
			if (expectedType != null && !expectedType.equals(type)) {
				return false;
			}
		}
		return true;
	}

	private boolean isGoodDummyName(String name) {
		if (isSignalName(name)) {
			return false;
		}
		if (isDummyName(name)) {
			return false;
		}
		return true;
	}

	public void setDefaultNameIfUnnamed(Node node) {
		if (node instanceof SignalTransition) {
			final SignalTransition st = (SignalTransition)node;
			if (instancedNameManager.contains(st)) {
				return;
			}
			String prefix = defaultNameManager.getNodePrefix(node);
			Integer count = defaultNameManager.getPrefixCount(prefix);
			String name = prefix + count;
			while ( !isGoodSignalName(name, st.getSignalType()) ) {
				name = prefix + (++count);
			};
			defaultNameManager.setPrefixCount(prefix, count);
			st.setSignalName(name);
			signalTransitions.put(name, st);
			instancedNameManager.assign(st);
		} else if (node instanceof DummyTransition) {
			final DummyTransition dt = (DummyTransition)node;
			if (instancedNameManager.contains(dt)) {
				return;
			}
			String prefix = defaultNameManager.getNodePrefix(node);
			Integer count = defaultNameManager.getPrefixCount(prefix);
			String name;
			do {
				name = prefix + (count++);
			} while ( !isGoodDummyName(name) );
			dt.setName(name);
			dummyTransitions.put(name, dt);
			instancedNameManager.assign(dt);
		} else if (node instanceof STGPlace) {
			STGPlace p = (STGPlace)node;
			if (!p.isImplicit()) {
				defaultNameManager.setDefaultNameIfUnnamed(node);
			}
		} else {
			defaultNameManager.setDefaultNameIfUnnamed(node);
		}
	}

	private void nodeRemoved(Node node) {
		if (node instanceof SignalTransition) {
			final SignalTransition st = (SignalTransition)node;
			signalTransitions.remove(st.getSignalName(), st);
			instancedNameManager.remove(st);
		}
		if (node instanceof DummyTransition) {
			final DummyTransition dt = (DummyTransition)node;
			dummyTransitions.remove(dt.getName(), dt);
			instancedNameManager.remove(dt);
		} else {
			defaultNameManager.remove(node);
		}
	}

}
