package org.workcraft.plugins.dfs.tools;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.BoundingBoxHelper;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.connections.ControlPoint;
import org.workcraft.dom.visual.connections.Polyline;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.dom.visual.connections.VisualConnection.ConnectionType;
import org.workcraft.plugins.dfs.BinaryRegister.Marking;
import org.workcraft.plugins.dfs.ControlConnection;
import org.workcraft.plugins.dfs.ControlRegister;
import org.workcraft.plugins.dfs.ControlRegister.SynchronisationType;
import org.workcraft.plugins.dfs.Dfs;
import org.workcraft.plugins.dfs.Logic;
import org.workcraft.plugins.dfs.PopRegister;
import org.workcraft.plugins.dfs.PushRegister;
import org.workcraft.plugins.dfs.Register;
import org.workcraft.plugins.dfs.VisualControlConnection;
import org.workcraft.plugins.dfs.VisualControlRegister;
import org.workcraft.plugins.dfs.VisualDfs;
import org.workcraft.plugins.dfs.VisualLogic;
import org.workcraft.plugins.dfs.VisualPopRegister;
import org.workcraft.plugins.dfs.VisualPushRegister;
import org.workcraft.plugins.dfs.VisualRegister;
import org.workcraft.util.Hierarchy;

public class WaggingGenerator {
	private final VisualDfs dfs;
	private final int count;

	class WaggingData {
		HashSet<VisualComponent> dataComponents = new HashSet<VisualComponent>();
		HashSet<VisualPushRegister> pushRegisters = new HashSet<VisualPushRegister>();
		HashSet<VisualControlRegister> pushControls = new HashSet<VisualControlRegister>();
		HashSet<VisualPopRegister> popRegisters = new HashSet<VisualPopRegister>();
		HashSet<VisualControlRegister> popControls = new HashSet<VisualControlRegister>();
	}

	private final HashSet<VisualComponent> selectedComponents = new HashSet<VisualComponent>();
	private final HashSet<VisualConnection> selectedConnections = new HashSet<VisualConnection>();
	private HashMap<VisualComponent, VisualComponent> replicaToOriginalMap = new HashMap<VisualComponent, VisualComponent>();
	private ArrayList<WaggingData> wagging = new ArrayList<WaggingData>();

	public WaggingGenerator(VisualDfs dfs, int count) {
		this.dfs = dfs;
		this.count = count;

		for (Node node: dfs.getSelection()) {
			if (node instanceof VisualNode) {
				if (node instanceof VisualComponent) {
					selectedComponents.add((VisualComponent)node);
				} else if (node instanceof VisualConnection) {
					selectedConnections.add((VisualConnection)node);
				}
			}
		}
	}

	public void run() {
		replicateSelection();
		insertInterface();
		insertPushControl();
		insertPopControl();
		createGroups();
	}

	private void replicateSelection() {
		replicaToOriginalMap.clear();
		wagging.clear();
		Rectangle2D bb = null;
		for (VisualComponent component: selectedComponents) {
			bb = BoundingBoxHelper.union(bb, component.getBoundingBox());
		}
		double step = (double)Math.ceil(bb.getHeight());
		for (int i = 0; i < count; ++i) {
			HashMap<VisualComponent, VisualComponent> mapComponentToReplica = new HashMap<VisualComponent, VisualComponent>();
			WaggingData waggingData = new WaggingData();
			for (VisualComponent component: selectedComponents) {
				VisualComponent replicaComponenet = replicateComponent(component);
				replicaComponenet.setY(replicaComponenet.getY() + step * (2*i+1 - count)/2);
				mapComponentToReplica.put(component, replicaComponenet);
				replicaToOriginalMap.put(replicaComponenet, component);
				waggingData.dataComponents.add(replicaComponenet);
			}
			for (VisualConnection connection: selectedConnections) {
				replicateConnection(connection, mapComponentToReplica);
			}
			wagging.add(waggingData);
		}
	}

	private VisualComponent replicateComponent(VisualComponent component) {
		VisualComponent replica = null;
		if (component instanceof VisualLogic) {
			Logic ref = ((VisualLogic)component).getReferencedLogic();
			Logic replicaRef = new Logic();
			replicaRef.setEarlyEvaluation(ref.isEarlyEvaluation());
			replicaRef.setComputed(ref.isComputed());
			replica = new VisualLogic(replicaRef);
		} else if (component instanceof VisualRegister) {
			Register ref = ((VisualRegister)component).getReferencedRegister();
			Register replicaRef = new Register();
			replicaRef.setMarked(ref.isMarked());
			replica = new VisualRegister(replicaRef);
		}

		if (replica != null) {
			replica.setPosition(component.getPosition());
			replica.setLabel(component.getLabel());
			replica.setLabelColor(component.getLabelColor());
			replica.setLabelPositioning(component.getLabelPositioning());
			replica.setFillColor(component.getFillColor());
			replica.setForegroundColor(component.getForegroundColor());
			replica.setNameColor(component.getNameColor());
			replica.setNamePositioning(component.getNamePositioning());
			// postpone adding to the model so no notifications are sent too early
			Dfs mathDfs = (Dfs)dfs.getMathModel();
			mathDfs.add(replica.getReferencedComponent());
			Hierarchy.getNearestContainer(component).add(replica);
		}
		return replica;
	}

	private VisualConnection replicateConnection(VisualConnection connection, HashMap<VisualComponent, VisualComponent> c2c) {
		VisualConnection replica = null;
		VisualComponent first = c2c.get(connection.getFirst());
		VisualComponent second = c2c.get(connection.getSecond());
		if (first != null && second != null) {
			if (connection instanceof VisualControlConnection) {
				ControlConnection connectionRef = ((VisualControlConnection)connection).getReferencedControlConnection();
				replica = createControlConnection(first, second, connectionRef.isInverting());
			} else {
				replica = createConnection(first, second);
			}
		}
		return replica;
	}

	private void insertInterface() {
		for (WaggingData waggingData: wagging) {
			waggingData.pushRegisters.clear();
			waggingData.popRegisters.clear();
			for (VisualComponent cur: waggingData.dataComponents) {
				for (Node pred: dfs.getPreset(replicaToOriginalMap.get(cur))) {
					if (selectedComponents.contains(pred)) continue;
					VisualPushRegister push = createPushRegister(Hierarchy.getNearestContainer(cur, pred),
							new Point2D.Double((cur.getX() + ((VisualComponent)pred).getX())/2, cur.getY()));
					createConnection((VisualComponent)pred, push);
					createConnection(push, cur);
					waggingData.pushRegisters.add(push);
				}
				for (Node succ: dfs.getPostset(replicaToOriginalMap.get(cur))) {
					if (selectedComponents.contains(succ)) continue;
					VisualPopRegister pop = createPopRegister(Hierarchy.getNearestContainer(cur, succ),
							new Point2D.Double((cur.getX() + ((VisualComponent)succ).getX())/2, cur.getY()));
					createConnection(cur, pop);
					createConnection(pop, (VisualComponent)succ);
					waggingData.popRegisters.add(pop);
				}
			}
		}
	}

	private void insertPushControl() {
		Container container = getCommonContainer();
		Rectangle2D bb = getBoundingBox();
		double xPos = Math.floor(bb.getMinX());
		double yPos = Math.floor(bb.getMaxY());
		int iPos = 0;
		VisualControlRegister predReg1 = null;
		VisualControlRegister firstReg0 = null;
		for (WaggingData waggingData: wagging) {
			iPos++;
			// create control registers
			VisualControlRegister reg0 = createControlRegister(container,
					new Point2D.Double(xPos - 2.0, yPos + iPos * 2.0),
					(predReg1 == null ? Marking.TRUE_TOKEN : Marking.FALSE_TOKEN), SynchronisationType.AND);
			VisualControlRegister reg1 = createControlRegister(container,
					new Point2D.Double(xPos - 4.0, yPos + iPos * 2.0),
					Marking.EMPTY, SynchronisationType.PLAIN);
			VisualControlRegister reg2 = createControlRegister(container,
					new Point2D.Double(xPos - 6.0, yPos + iPos * 2.0),
					Marking.EMPTY, SynchronisationType.PLAIN);
			waggingData.pushControls.add(reg0);
			waggingData.pushControls.add(reg1);
			waggingData.pushControls.add(reg2);
			// connection within control layer
			VisualControlConnection con0 = createControlConnection(reg0, reg2, false);
			convertConnectionToPolyline(con0, 0.0, 1.0, 0.0, 1.0);
			createControlConnection(reg1, reg0, true);
			createControlConnection(reg2, reg1, false);
			// connection to the push registers
			for (VisualPushRegister push: waggingData.pushRegisters) {
				createControlConnection(reg0, push, false);
			}
			// connection between control layers
			if (predReg1 == null) {
				firstReg0 = reg0;
			} else {
				createControlConnection(predReg1, reg0, false);
			}
			predReg1 = reg1;
		}
		if (firstReg0 != null && predReg1 != null) {
			createControlConnection(predReg1, firstReg0, false);
		}
	}


	private void insertPopControl() {
		Container container = getCommonContainer();
		Rectangle2D bb = getBoundingBox();
		double xPos = Math.floor(bb.getMaxX());
		double yPos = Math.floor(bb.getMaxY());
		int iPos = 0;
		VisualControlRegister predReg1 = null;
		VisualControlRegister firstReg0 = null;
		for (WaggingData waggingData: wagging) {
			iPos++;
			// create control registers
			VisualControlRegister reg0 = createControlRegister(container,
					new Point2D.Double(xPos + 2.0, yPos + iPos * 2.0),
					(predReg1 == null ? Marking.TRUE_TOKEN : Marking.FALSE_TOKEN), SynchronisationType.AND);
			VisualControlRegister reg1 = createControlRegister(container,
					new Point2D.Double(xPos + 4.0, yPos + iPos * 2.0),
					Marking.EMPTY, SynchronisationType.PLAIN);
			VisualControlRegister reg2 = createControlRegister(container,
					new Point2D.Double(xPos + 6.0, yPos + iPos * 2.0),
					Marking.EMPTY, SynchronisationType.PLAIN);
			waggingData.popControls.add(reg0);
			waggingData.popControls.add(reg1);
			waggingData.popControls.add(reg2);
			// connection within control layer
			VisualControlConnection con0 = createControlConnection(reg0, reg2, false);
			convertConnectionToPolyline(con0, 0.0, 1.0, 0.0, 1.0);
			createControlConnection(reg1, reg0, true);
			createControlConnection(reg2, reg1, false);
			// connection to the pop registers
			for (VisualPopRegister pop: waggingData.popRegisters) {
				createControlConnection(reg0, pop, false);
			}
			// connection between control layers
			if (predReg1 == null) {
				firstReg0 = reg0;
			} else {
				createControlConnection(predReg1, reg0, false);
			}
			predReg1 = reg1;
		}
		if (firstReg0 != null && predReg1 != null) {
			createControlConnection(predReg1, firstReg0, false);
		}
	}

	private void createGroups() {
		dfs.deleteSelection();
		// data components
		{
			ArrayList<Node> dataNodes = new ArrayList<Node>();
			for (WaggingData waggingData: wagging) {
				dataNodes.addAll(waggingData.dataComponents);
				dataNodes.addAll(waggingData.pushRegisters);
				dataNodes.addAll(waggingData.popRegisters);
			}
			dfs.select(dataNodes);
			dfs.groupSelection();
		}
		// push control
		{
			ArrayList<Node> pushNodes = new ArrayList<Node>();
			for (WaggingData waggingData: wagging) {
				pushNodes.addAll(waggingData.pushControls);
			}
			dfs.select(pushNodes);
			dfs.groupSelection();
		}
		// pop control
		{
			ArrayList<Node> popNodes = new ArrayList<Node>();
			for (WaggingData waggingData: wagging) {
				popNodes.addAll(waggingData.popControls);
			}
			dfs.select(popNodes);
			dfs.groupSelection();
		}
	}

	private Rectangle2D getBoundingBox() {
		Rectangle2D bb = null;
		for (WaggingData waggingData: wagging) {
			for (VisualComponent component: waggingData.dataComponents) {
				bb = BoundingBoxHelper.union(bb, component.getBoundingBox());
			}
			for (VisualPushRegister push: waggingData.pushRegisters) {
				bb = BoundingBoxHelper.union(bb, push.getBoundingBox());
			}
			for (VisualPopRegister pop: waggingData.popRegisters) {
				bb = BoundingBoxHelper.union(bb, pop.getBoundingBox());
			}
		}
		return bb;
	}

	private Container getCommonContainer() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (WaggingData waggingData: wagging) {
			nodes.addAll(waggingData.dataComponents);
			nodes.addAll(waggingData.pushRegisters);
			nodes.addAll(waggingData.popRegisters);
		}
		return Hierarchy.getNearestContainer(nodes.toArray(new Node[nodes.size()]));
	}

	private void addComponent(VisualComponent component, Container container, Point2D position) {
		component.setPosition(position);
		// postpone adding to the model so no notifications are sent too early
		((Dfs)dfs.getMathModel()).add(component.getReferencedComponent());
		if (container == null) {
			container = dfs.getRoot();
		}
		container.add(component);
	}

	private VisualPushRegister createPushRegister(Container container, Point2D position) {
		VisualPushRegister component = new VisualPushRegister(new PushRegister());
		addComponent(component, container, position);
		return component;
	}

	private VisualPopRegister createPopRegister(Container container, Point2D position) {
		VisualPopRegister component = new VisualPopRegister(new PopRegister());
		addComponent(component, container, position);
		return component;
	}

	private VisualControlRegister createControlRegister(Container container, Point2D position,
			Marking marking, SynchronisationType syncType) {
		VisualControlRegister component = new VisualControlRegister(new ControlRegister());
		component.getReferencedControlRegister().setSynchronisationType(syncType);
		component.getReferencedControlRegister().setMarking(marking);
		addComponent(component, container, position);
		return component;
	}

	private VisualConnection createConnection(VisualComponent first, VisualComponent second) {
		MathNode firstRef = first.getReferencedComponent();
		MathNode secondRef = second.getReferencedComponent();
		MathConnection connectionRef = ((Dfs)dfs.getMathModel()).connect(firstRef, secondRef);
		VisualConnection connection = new VisualConnection(connectionRef, first, second);
		Hierarchy.getNearestContainer(first, second).add(connection);
		return connection;
	}

	private VisualControlConnection createControlConnection(VisualComponent first, VisualComponent second, boolean inversing) {
		MathNode firstRef = first.getReferencedComponent();
		MathNode secondRef = second.getReferencedComponent();
		ControlConnection connectionRef = ((Dfs)dfs.getMathModel()).controlConnect(firstRef, secondRef);
		connectionRef.setInverting(inversing);
		VisualControlConnection connection = new VisualControlConnection(connectionRef, first, second);
		connection.setBubble(inversing);
		Hierarchy.getNearestContainer(first, second).add(connection);
		return connection;
	}

	private void convertConnectionToPolyline(VisualConnection connection, double x1Offset, double y1Offset, double x2Offset, double y2Offset) {
		connection.setConnectionType(ConnectionType.POLYLINE, true);
		Polyline p = (Polyline)connection.getGraphic();
		ControlPoint cp1 = new ControlPoint();
		cp1.setPosition(new Point2D.Double(connection.getFirst().getX() + x1Offset, connection.getFirst().getY() + y1Offset));
		p.add(cp1);
		ControlPoint cp2 = new ControlPoint();
		cp2.setPosition(new Point2D.Double(connection.getSecond().getX() + x2Offset, connection.getSecond().getY() + y2Offset));
		p.add(cp2);
	}

}
