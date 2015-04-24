package org.workcraft.plugins.circuit.stg;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.visual.Movable;
import org.workcraft.dom.visual.TransformHelper;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.circuit.CircuitSettings;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualCircuitConnection;
import org.workcraft.plugins.circuit.VisualContact;
import org.workcraft.plugins.circuit.VisualFunctionContact;
import org.workcraft.plugins.cpog.optimisation.BooleanFormula;
import org.workcraft.plugins.cpog.optimisation.BooleanVariable;
import org.workcraft.plugins.cpog.optimisation.Literal;
import org.workcraft.plugins.cpog.optimisation.booleanvisitors.FormulaToString;
import org.workcraft.plugins.cpog.optimisation.dnf.Dnf;
import org.workcraft.plugins.cpog.optimisation.dnf.DnfClause;
import org.workcraft.plugins.cpog.optimisation.dnf.DnfGenerator;
import org.workcraft.plugins.cpog.optimisation.expressions.DumbBooleanWorker;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.stg.STG;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.VisualImplicitPlaceArc;
import org.workcraft.plugins.stg.SignalTransition.Direction;
import org.workcraft.plugins.stg.VisualSTG;
import org.workcraft.plugins.stg.VisualSignalTransition;
import org.workcraft.util.Geometry;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.TwoWayMap;

public class CircuitToStgConverter {
	private static final String NAME_SUFFIX_0 = "_0";
	private static final String NAME_SUFFIX_1 = "_1";
	private static final String LABEL_SUFFIX_0 = "=0";
	private static final String LABEL_SUFFIX_1 = "=1";

	private static final double SCALE_X = 4.0;
	private static final double SCALE_Y = 4.0;
	private static final Point2D OFFSET_P1 = new Point2D.Double(0.0, -1.0);
	private static final Point2D OFFSET_P0 = new Point2D.Double(0.0, 1.0);
	private static final Point2D OFFSET_INIT_PLUS = new Point2D.Double(0.0, -1.0);
	private static final Point2D OFFSET_INIT_MINUS = new Point2D.Double(0.0, 1.0);
	private static final Point2D OFFSET_INC_PLUS = new Point2D.Double(0.0, -2.0);
	private static final Point2D OFFSET_INC_MINUS = new Point2D.Double(0.0, 2.0);

	private final VisualCircuit circuit;
	private final VisualSTG stg;

	private final HashMap<String, Container> refToPageMap;
	private final Map<VisualNode, VisualContact> nodeToDriverMap;
	private final TwoWayMap<VisualContact, SignalStg> driverToStgMap;

	public CircuitToStgConverter(VisualCircuit circuit) {
		this.circuit = circuit;
		this.stg = new VisualSTG(new STG());
		HashSet<VisualContact> drivers = identifyDrivers();
		this.nodeToDriverMap = associateDrivers(drivers);
		this.refToPageMap = convertPages();
		this.driverToStgMap = convertDrivers(drivers);
		connectDrivers(drivers);
		if (CircuitSettings.getSimplifyStg()) {
			simplifyDrivers(drivers); // remove dead transitions
		}
		positionDrivers(drivers);
		groupDrivers(drivers);
	}

	public CircuitToStgConverter(VisualCircuit circuit, VisualSTG stg) {
		this.circuit = circuit;
		this.stg = stg;
		HashSet<VisualContact> drivers = identifyDrivers();
		this.nodeToDriverMap = associateDrivers(drivers);
		this.refToPageMap = convertPages();
		this.driverToStgMap = assignDrivers(drivers);
		if (CircuitSettings.getSimplifyStg()) {
			simplifyDrivers(drivers); // remove dead transitions
		}
		positionDrivers(drivers);
		groupDrivers(drivers);
	}


	public VisualSTG getStg() {
		return stg;
	}

	public boolean isDriver(VisualContact contact) {
		return driverToStgMap.containsKey(contact);

	}

	public SignalStg getSignalStg(VisualNode node) {
		SignalStg result = null;
		VisualContact driver = nodeToDriverMap.get(node);
		if (driver != null) {
			result = driverToStgMap.getValue(driver);
		}
		return result;
	}

	private HashMap<String, Container> convertPages() {
		NamespaceHelper.copyPageStructure(circuit, stg);
		return NamespaceHelper.getRefToPageMapping(stg);
	}

	private Container getContainer(VisualContact contact) {
		String nodeReference = circuit.getMathModel().getNodeReference(contact.getReferencedComponent());
		String parentReference = NamespaceHelper.getParentReference(nodeReference);
		Container container = (Container)refToPageMap.get(parentReference);
		while (container==null) {
			parentReference = NamespaceHelper.getParentReference(parentReference);
			container = (Container)refToPageMap.get(parentReference);
		}
		return container;
	}

	private HashSet<VisualContact> identifyDrivers() {
		HashSet<VisualContact> result = new HashSet<>();
		for (VisualContact contact : Hierarchy.getDescendantsOfType(circuit.getRoot(), VisualContact.class)) {
			VisualContact driver = null;
			if (!contact.isDriver()) {
				driver = CircuitUtils.findDriver(circuit, contact);
			}
			if (driver == null) {
				driver = contact;
			}
			result.add(driver);
		}
		return result;
	}

	private HashMap<VisualNode, VisualContact> associateDrivers(HashSet<VisualContact> driverSet) {
		HashMap<VisualNode, VisualContact> result = new HashMap<>();
		for (VisualContact driver: driverSet) {
			propagateDriver(driver, driver, result);
		}
		return result;
	}

	private void propagateDriver(VisualNode node, VisualContact driver, HashMap<VisualNode, VisualContact> nodeToDriverMap) {
		if (!nodeToDriverMap.containsKey(node)) {
			nodeToDriverMap.put(node, driver);

			for (Connection connection: circuit.getConnections(node)) {
				if ((connection.getFirst() == node) && (connection instanceof VisualCircuitConnection)) {
					nodeToDriverMap.put((VisualCircuitConnection)connection, driver);
					Node succNode = connection.getSecond();
					if (succNode instanceof VisualNode) {
						propagateDriver((VisualNode)succNode, driver, nodeToDriverMap);
					}
				}
			}
		}
	}

	private TwoWayMap<VisualContact, SignalStg> convertDrivers(HashSet<VisualContact> drivers) {
		TwoWayMap<VisualContact, SignalStg> result = new TwoWayMap<>();
		for (VisualContact driver: drivers) {
			Container container = getContainer(driver);
			String contactName = CircuitUtils.getContactName(circuit, driver);

			VisualPlace zeroPlace = stg.createPlace(contactName + NAME_SUFFIX_0, container);
			zeroPlace.setLabel(contactName + LABEL_SUFFIX_0);
			if (!driver.getReferencedContact().getInitToOne()) {
				zeroPlace.getReferencedPlace().setTokens(1);
			}

			VisualPlace onePlace = stg.createPlace(contactName + NAME_SUFFIX_1, container);
			onePlace.setLabel(contactName + LABEL_SUFFIX_1);
			if (driver.getReferencedContact().getInitToOne()) {
				onePlace.getReferencedPlace().setTokens(1);
			}

			SignalStg signalStg = new SignalStg(zeroPlace, onePlace);
			result.put(driver, signalStg);
		}
		return result;
	}

	private void connectDrivers(HashSet<VisualContact> drivers) {
		for (VisualContact driver: drivers) {
			BooleanFormula setFunc = null;
			BooleanFormula resetFunc = null;
			if (driver instanceof VisualFunctionContact) {
				// Determine signal type
				VisualFunctionContact contact = (VisualFunctionContact)driver;
				if (contact.isOutput() && contact.isPort()) {
					// Driver of the primary output port
					VisualContact outputDriver = CircuitUtils.findDriver(circuit, contact);
					if (outputDriver != null) {
						setFunc = outputDriver.getReferencedContact();
					}
				} else {
					// Function based driver
					setFunc = contact.getSetFunction();
					resetFunc = contact.getResetFunction();
				}
			}
			// Create complementary set/reset if only one of them is defined
			if ((setFunc != null) && (resetFunc == null)) {
				resetFunc = new DumbBooleanWorker().not(setFunc);
			} else if ((setFunc == null) && (resetFunc != null)) {
				setFunc = new DumbBooleanWorker().not(resetFunc);
			}
			Dnf setDnf = DnfGenerator.generate(setFunc);
			createSignalStgTransitions(driver, setDnf, Direction.PLUS);

			Dnf resetDnf = DnfGenerator.generate(resetFunc);
			createSignalStgTransitions(driver, resetDnf, Direction.MINUS);
		}
	}

	private void createSignalStgTransitions(VisualContact driver, Dnf dnf, Direction direction) {
		SignalStg driverStg = driverToStgMap.getValue(driver);
		VisualPlace predPlace = (direction == Direction.PLUS ? driverStg.P0 : driverStg.P1);
		VisualPlace succPlace = (direction == Direction.PLUS ? driverStg.P1 : driverStg.P0);
		HashSet<VisualSignalTransition> transitions = (direction == Direction.PLUS ? driverStg.Rs : driverStg.Fs);

		TreeSet<DnfClause> clauses = new TreeSet<DnfClause>(
				new Comparator<DnfClause>() {
					@Override
					public int compare(DnfClause arg0, DnfClause arg1) {
						String st1 = FormulaToString.toString(arg0);
						String st2 = FormulaToString.toString(arg1);
						return st1.compareTo(st2);
					}
				});

		clauses.addAll(dnf.getClauses());

		Container container = getContainer(driver);
		String signalName = CircuitUtils.getContactName(circuit, driver);
		SignalTransition.Type signalType = CircuitUtils.getSignalType(circuit, driver);
		for(DnfClause clause : clauses) {
			VisualSignalTransition transition = stg.createSignalTransition(signalName, signalType, direction, container);
			transition.setLabel(FormulaToString.toString(clause));
			transitions.add(transition);

			try {
				stg.connect(predPlace, transition);
				stg.connect(transition, succPlace);
			} catch (InvalidConnectionException e) {
				throw new RuntimeException(e);
			}

			HashSet<VisualPlace> placesToRead = new HashSet<VisualPlace>();
			for (Literal literal : clause.getLiterals()) {
				BooleanVariable variable = literal.getVariable();
				VisualContact sourceContact = CircuitUtils.getVisualContact(circuit, (Contact)variable);
				VisualContact sourceDriver = nodeToDriverMap.get(sourceContact);
				SignalStg sourceDriverStg = driverToStgMap.getValue(sourceDriver);
				if (sourceDriverStg == null) {
					throw new RuntimeException("No source for " + circuit.getMathName(sourceContact) + " while generating " + signalName);
				}
				VisualPlace place = literal.getNegation() ? sourceDriverStg.P0 : sourceDriverStg.P1;
				placesToRead.add(place);
			}

			if (placesToRead.remove(predPlace)) {
				System.out.println("warning: signal " + signalName + " depends on itself");
			}

			for (VisualPlace place : placesToRead) {
				// FIXME: Why duplicate arcs would be created in the first place?
				try {
					if(stg.getConnection(place, transition) == null) {
						stg.connect(place, transition);
					}
					if(stg.getConnection(transition, place) == null) {
						stg.connect(transition, place);
					}
				} catch (InvalidConnectionException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}


	private TwoWayMap<VisualContact, SignalStg> assignDrivers(HashSet<VisualContact> drivers) {
		TwoWayMap<VisualContact, SignalStg> result = new TwoWayMap<>();

		for (VisualContact driver: drivers) {
			String contactName = CircuitUtils.getContactName(circuit, driver);

			VisualPlace zeroPlace = null;
			VisualPlace onePlace = null;
			String zeroName = contactName + NAME_SUFFIX_0;
			String oneName = contactName + NAME_SUFFIX_1;
			for (VisualPlace place: stg.getVisualPlaces()) {
				if (zeroName.equals(stg.getMathName(place))) {
					zeroPlace = place;
				}
				if (oneName.equals(stg.getMathName(place))) {
					onePlace = place;
				}
			}

			VisualSignalTransition plusTransition = null;
			VisualSignalTransition minusTransition = null;
			for (VisualSignalTransition transition: stg.getVisualSignalTransitions()) {
				if (contactName.equals(transition.getSignalName())) {
					if (transition.getDirection() == Direction.PLUS) {
						plusTransition = transition;
					}
					if (transition.getDirection() == Direction.MINUS) {
						minusTransition = transition;
					}
				}
			}
			if (zeroPlace == null) {
				Connection connection = stg.getConnection(minusTransition, plusTransition);
				if (connection instanceof VisualImplicitPlaceArc) {
					VisualImplicitPlaceArc implicitPlace = (VisualImplicitPlaceArc)connection;
					zeroPlace = stg.makeExplicit(implicitPlace);
					stg.setMathName(zeroPlace, zeroName);
				}
			}

			if (onePlace == null) {
				Connection connection = stg.getConnection(plusTransition, minusTransition);
				if (connection instanceof VisualImplicitPlaceArc) {
					VisualImplicitPlaceArc implicitPlace = (VisualImplicitPlaceArc)connection;
					onePlace = stg.makeExplicit(implicitPlace);
					stg.setMathName(onePlace, oneName);
				}
			}

			if ((zeroPlace != null) && (onePlace != null)) {
				zeroPlace.setLabel(contactName + LABEL_SUFFIX_0);
				onePlace.setLabel(contactName + LABEL_SUFFIX_1);
				SignalStg signalStg = new SignalStg(zeroPlace, onePlace);
				result.put(driver, signalStg);
				for (VisualSignalTransition transition: stg.getVisualSignalTransitions()) {
					if (contactName.equals(transition.getSignalName())) {
						if (transition.getDirection() == Direction.PLUS) {
							signalStg.Rs.add(transition);
						}
						if (transition.getDirection() == Direction.MINUS) {
							signalStg.Fs.add(transition);
						}
					}
				}
			}
		}
		return result;
	}

	private void simplifyDrivers(HashSet<VisualContact> drivers) {
		for (VisualContact driver: drivers) {
			SignalStg signalStg = driverToStgMap.getValue(driver);
			if (signalStg != null) {
				HashSet<Node> deadPostset = new HashSet<Node>(stg.getPostset(signalStg.P0));
				deadPostset.retainAll(stg.getPostset(signalStg.P1));
				for (Node node: deadPostset) {
					if (node instanceof VisualTransition) {
						signalStg.Rs.remove(node);
						signalStg.Fs.remove(node);
						stg.remove(node);
					}
				}
			}
		}
	}

	private void positionDrivers(HashSet<VisualContact> drivers) {
		for (VisualContact driver: drivers) {
			SignalStg signalStg = driverToStgMap.getValue(driver);
			if (signalStg != null) {
				Point2D centerPosition = getPosition(driver);
				setPosition(signalStg.P0, Geometry.add(centerPosition, OFFSET_P0));
				setPosition(signalStg.P1, Geometry.add(centerPosition, OFFSET_P1));

				centerPosition = Geometry.add(centerPosition, getDirectionOffset(driver));
				Point2D plusPosition = Geometry.add(centerPosition, OFFSET_INIT_PLUS);
				for (VisualSignalTransition transition: signalStg.Rs) {
					setPosition(transition, plusPosition);
					plusPosition = Geometry.add(plusPosition, OFFSET_INC_PLUS);
				}

				Point2D minusPosition = Geometry.add(centerPosition, OFFSET_INIT_MINUS);
				for (VisualSignalTransition transition: signalStg.Fs) {
					setPosition(transition, minusPosition);
					minusPosition = Geometry.add(minusPosition, OFFSET_INC_MINUS);
				}
			}
		}
	}

	private Point2D getPosition(VisualContact contact) {
		AffineTransform transform = TransformHelper.getTransformToRoot(contact);
		Point2D position = new Point2D.Double(
				SCALE_X * (transform.getTranslateX() + contact.getX()),
				SCALE_Y * (transform.getTranslateY() + contact.getY()));
		return position;
	}

	private void setPosition(Movable node, Point2D point) {
		TransformHelper.applyTransform(node, AffineTransform.getTranslateInstance(point.getX(), point.getY()));
	}

	private Point2D getDirectionOffset(VisualContact contact) {
		VisualContact.Direction direction = contact.getDirection();
		if (contact.isInput()) {
			direction = direction.flip();
		}
		switch (direction) {
			case WEST: return new Point2D.Double( 6.0, 0.0);
			case EAST: return new Point2D.Double(-6.0, 0.0);
			case NORTH: return new Point2D.Double( 6.0, 0.0);
			case SOUTH: return new Point2D.Double(-6.0, 0.0);
			default: return new Point2D.Double( 0.0, 0.0);
		}
	}

	private void groupDrivers(HashSet<VisualContact> drivers) {
		for (VisualContact driver: drivers) {
			SignalStg signalStg = driverToStgMap.getValue(driver);
			if (signalStg != null) {
				Collection<Node> nodesToGroup = new LinkedList<Node>();
				nodesToGroup.add(signalStg.P1);
				nodesToGroup.add(signalStg.P0);
				nodesToGroup.addAll(signalStg.Rs);
				nodesToGroup.addAll(signalStg.Fs);

				Container currentLevel = null;
				Container oldLevel = stg.getCurrentLevel();
				for (Node node:nodesToGroup) {
					if (currentLevel == null) {
						currentLevel = (Container)node.getParent();
					}
					if (currentLevel != node.getParent()) {
						throw new RuntimeException("Current level is not the same among the processed nodes");
					}
				}

				stg.setCurrentLevel(currentLevel);
				stg.select(nodesToGroup);
				stg.groupSelection();
				stg.setCurrentLevel(oldLevel);
			}
		}
	}

}
