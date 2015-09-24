package org.workcraft.plugins.xmas.stg;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.Movable;
import org.workcraft.dom.visual.Positioning;
import org.workcraft.dom.visual.TransformHelper;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualReplicaPlace;
import org.workcraft.plugins.stg.STG;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.VisualSTG;
import org.workcraft.plugins.stg.VisualSignalTransition;
import org.workcraft.plugins.xmas.VisualXmas;
import org.workcraft.plugins.xmas.XmasUtils;
import org.workcraft.plugins.xmas.components.VisualForkComponent;
import org.workcraft.plugins.xmas.components.VisualFunctionComponent;
import org.workcraft.plugins.xmas.components.VisualJoinComponent;
import org.workcraft.plugins.xmas.components.VisualMergeComponent;
import org.workcraft.plugins.xmas.components.VisualQueueComponent;
import org.workcraft.plugins.xmas.components.VisualSinkComponent;
import org.workcraft.plugins.xmas.components.VisualSourceComponent;
import org.workcraft.plugins.xmas.components.VisualSwitchComponent;
import org.workcraft.plugins.xmas.components.VisualXmasComponent;
import org.workcraft.plugins.xmas.components.VisualXmasContact;
import org.workcraft.util.Hierarchy;

public class StgGenerator {

	private enum XmasStgType { IORACLE, TORACLE, IRDY, IDN, TRDY, TDN }

	private static final String _INITIATOR  = "I";
	private static final String _TARGET     = "T";
	private static final String _RDY	    = "Rdy";
	private static final String _DN	   	    = "Dn";
	private static final String _ORACLE		= "_oracle";
	private static final String _MEM 		= "_mem";
	private static final String _HEAD		= "_hd";
	private static final String _TAIL		= "_tl";
	private static final String _PORT_I		= "_i";
	private static final String _PORT_O		= "_o";
	private static final String _PORT_A		= "_a";
	private static final String _PORT_B		= "_b";

	private static final String _O_IRDY	= _PORT_O + _INITIATOR + _RDY;
	private static final String _O_IDN  = _PORT_O + _INITIATOR + _DN;
	private static final String _A_IRDY	= _PORT_A + _INITIATOR + _RDY;
	private static final String _A_IDN  = _PORT_A + _INITIATOR + _DN;
	private static final String _B_IRDY	= _PORT_B + _INITIATOR + _RDY;
	private static final String _B_IDN  = _PORT_B + _INITIATOR + _DN;

	private static final String _I_TRDY	= _PORT_I + _TARGET + _RDY;
	private static final String _I_TDN  = _PORT_I + _TARGET + _DN;
	private static final String _A_TRDY	= _PORT_A + _TARGET + _RDY;
	private static final String _A_TDN  = _PORT_A + _TARGET + _DN;
	private static final String _B_TRDY	= _PORT_B + _TARGET + _RDY;
	private static final String _B_TDN  = _PORT_B + _TARGET + _DN;


	private static final String name0	 	= "_0";
	private static final String name1 		= "_1";
	private static final double xScaling = 10;
	private static final double yScaling = 10;
	private static final double QUEUE_SLOT_SPACING = 20.0;

	private SignalStg clockStg = null;
	private Set<SignalStg> clockControlSignals = null;
	private Map<VisualXmasContact, ContactStg> contactMap = new HashMap<>();
	private Map<VisualSourceComponent, SourceStg> sourceMap = new HashMap<>();
	private Map<VisualSinkComponent, SinkStg> sinkMap = new HashMap<>();
	private Map<VisualFunctionComponent, FunctionStg> functionMap = new HashMap<>();
	private Map<VisualForkComponent, ForkStg> forkMap = new HashMap<>();
	private Map<VisualJoinComponent, JoinStg> joinMap = new HashMap<>();
	private Map<VisualSwitchComponent, SwitchStg> switchMap = new HashMap<>();
	private Map<VisualMergeComponent, MergeStg> mergeMap = new HashMap<>();
	private Map<VisualQueueComponent, QueueStg> queueMap = new HashMap<>();
	private final VisualXmas xmas;
	private final VisualSTG stg;

	public StgGenerator(VisualXmas xmas) {
		this.xmas = xmas;
		this.stg = new VisualSTG(new STG());
		convert();
	}

	private void convert() {
		try {
			{
				clockStg = generateClockStg();
				clockControlSignals = new HashSet<>();
			}
			for(VisualSourceComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSourceComponent.class)) {
				SourceStg sourceStg = generateSourceStg(component);
				sourceMap.put(component, sourceStg);
			}
			for(VisualSinkComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSinkComponent.class)) {
				SinkStg sinkStg = generateSinkStg(component);
				sinkMap.put(component, sinkStg);
			}
			for(VisualFunctionComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualFunctionComponent.class)) {
				FunctionStg functionStg = generateFunctionStg(component);
				functionMap.put(component, functionStg);
			}
			for(VisualForkComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualForkComponent.class)) {
				ForkStg forkStg = generateForkStg(component);
				forkMap.put(component, forkStg);
			}
			for(VisualJoinComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualJoinComponent.class)) {
				JoinStg joinStg = generateJoinStg(component);
				joinMap.put(component, joinStg);
			}
			for(VisualSwitchComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSwitchComponent.class)) {
				SwitchStg switchStg = generateSwitchStg(component);
				switchMap.put(component, switchStg);
			}
			for(VisualMergeComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualMergeComponent.class)) {
				MergeStg mergeStg = generateMergeStg(component);
				mergeMap.put(component, mergeStg);
			}
			for(VisualQueueComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualQueueComponent.class)) {
				QueueStg queueStg = generateQueueStg(component);
				queueMap.put(component, queueStg);
			}

			connectClockStg();
			for(VisualSourceComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSourceComponent.class)) {
				connectSourceStg(component);
			}
			for(VisualSinkComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSinkComponent.class)) {
				connectSinkStg(component);
			}
			for(VisualFunctionComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualFunctionComponent.class)) {
				connectFunctionStg(component);
			}
			for(VisualForkComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualForkComponent.class)) {
				connectForkStg(component);
			}
			for(VisualJoinComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualJoinComponent.class)) {
				connectJoinStg(component);
			}
			for(VisualSwitchComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSwitchComponent.class)) {
				connectSwitchStg(component);
			}
			for(VisualMergeComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualMergeComponent.class)) {
				connectMergeStg(component);
			}
			for(VisualQueueComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualQueueComponent.class)) {
				connectQueueStg(component);
			}
		} catch (InvalidConnectionException e) {
			throw new RuntimeException(e);
		}
		stg.selectNone();
	}

	public VisualSTG getStg() {
		return stg;
	}

	static void setPosition(Movable node, double x, double y) {
		TransformHelper.applyTransform(node, AffineTransform.getTranslateInstance(x, y));
	}

	private void createConsumingArc(VisualPlace p, VisualSignalTransition t) throws InvalidConnectionException {
		if (p != null && t != null) {
			stg.connect(p, t);
		}
	}

	private void createProducingArc(VisualSignalTransition t, VisualPlace p) throws InvalidConnectionException {
		if (p != null && t != null) {
			stg.connect(t, p);
		}
	}

	private void createReadArc(VisualPlace p, VisualSignalTransition t) throws InvalidConnectionException {
		if (p != null && t != null) {
			stg.connectUndirected(p, t);
		}
	}

	private void createReadArcs(VisualPlace p, Collection<VisualSignalTransition> ts) throws InvalidConnectionException {
		if (ts != null) {
			for (VisualSignalTransition t: ts) {
				stg.connectUndirected(p, t);
			}
		}
	}

	private void createReadArcsBetweenSignals(SignalStg from, SignalStg to) throws InvalidConnectionException {
		if ((from != null) && (to != null)) {
			createReadArcs(from.one, to.riseList);
			createReadArcs(from.zero, to.fallList);
		}
	}

	private void createReplicaReadArc(VisualPlace p, VisualSignalTransition t) throws InvalidConnectionException {
		double dx = ((p.getRootSpaceX() > t.getRootSpaceX()) ? 6.0 : -6.0);
		createReplicaReadArc(p, t, dx, 0.0);
	}

	private void createReplicaReadArc(VisualPlace p, VisualSignalTransition t, double xOffset, double yOffset) throws InvalidConnectionException {
		Point2D replicaPosition = new Point2D.Double(t.getRootSpaceX() + xOffset, t.getRootSpaceY() + yOffset);
		createReplicaReadArcs(p, Arrays.asList(t), replicaPosition);
	}

	private void createReplicaReadArcs(VisualPlace p, List<VisualSignalTransition> ts, double xOffset, double yOffset) throws InvalidConnectionException {
		if ((p != null) && (ts != null)) {
			VisualReplicaPlace replicaPlace = null;
			for (VisualSignalTransition t: ts) {
				if (replicaPlace == null) {
					Container container = Hierarchy.getNearestContainer(new HashSet<Node>(ts));
					replicaPlace = stg.createVisualReplica(p, container, VisualReplicaPlace.class);
					Point2D pos = new Point2D.Double(t.getRootSpaceX() + xOffset, t.getRootSpaceY() + yOffset);
					replicaPlace.setRootSpacePosition(pos);
				}
				stg.connectUndirected(replicaPlace, t);
			}
		}
	}

	private void createReplicaReadArcs(VisualPlace p, Collection<VisualSignalTransition> ts, Point2D replicaPosition) throws InvalidConnectionException {
		if ((p != null) && (ts != null)) {
			Container container = Hierarchy.getNearestContainer(new HashSet<Node>(ts));
			VisualReplicaPlace replicaPlace = stg.createVisualReplica(p, container, VisualReplicaPlace.class);
			if (replicaPosition != null) {
				replicaPlace.setRootSpacePosition(replicaPosition);
			}
			for (VisualSignalTransition t: ts) {
				stg.connectUndirected(replicaPlace, t);
			}
		}
	}

	private void createReplicaReadArcBetweenSignals(SignalStg from, SignalStg to) throws InvalidConnectionException {
		double xT = to.fallList.get(0).getRootSpaceX();
		double xP = to.one.getRootSpaceX();
		double yZero = to.zero.getRootSpaceY();
		double yOne = to.one.getRootSpaceY();
		double x = ((xT > xP) ? xT + 6.0 : xT - 6.0);
		createReplicaReadArcs(from.zero, to.fallList, new Point2D.Double(x, yZero));
		createReplicaReadArcs(from.one, to.riseList, new Point2D.Double(x, yOne));
	}

	private void createReplicaReadArcBetweenDoneSignals(SignalStg from, SignalStg to, double yOffset) throws InvalidConnectionException {
		double xT = to.fallList.get(0).getRootSpaceX();
		double xP = to.one.getRootSpaceX();
		double yZero = to.zero.getRootSpaceY();
		double yOne = to.one.getRootSpaceY();
		double xFall = ((xT > xP) ? xT + 6.0 : xT - 6.0);
		double yFall = yZero;
		double xRise = ((xT < xP) ? xT + 6.0 : xT - 6.0);
		double yRise = ((yOne > yZero) ? yOne + 1.0 : yOne - 1.0);
		createReplicaReadArcs(from.zero, to.fallList, new Point2D.Double(xFall, yFall + yOffset));
		createReplicaReadArcs(from.one, to.riseList, new Point2D.Double(xRise, yRise + yOffset));
	}


	private void createReplicaReadArcsFromDoneToClock(SignalStg dn) throws InvalidConnectionException {
		clockControlSignals.add(dn);
		int cnt = clockControlSignals.size();
		double dx = 4.0;
		int dy = (cnt % 2 == 0 ? 1 : -1) * cnt / 2;
		for (VisualSignalTransition t: clockStg.fallList) {
			createReplicaReadArc(dn.zero, t, dx, dy);
		}
		for (VisualSignalTransition t: clockStg.riseList) {
			createReplicaReadArc(dn.one, t, -dx, dy);
		}
	}

	private void createReplicaReadArcsFromClockToDone(SignalStg dn) throws InvalidConnectionException {
		double xt = 0.0;
		for (VisualSignalTransition t: dn.getAllTransitions()) {
			xt += t.getRootSpaceX();
		}
		xt /= dn.getAllTransitions().size();
		double xp = 0.0;
		for (VisualPlace p: dn.getAllPlaces()) {
			xp += p.getRootSpaceX();
		}
		xp /=  dn.getAllPlaces().size();
		Point2D centerPos = getSignalCenterPosition(dn);
		Point2D clk1Pos = centerPos;
		if (dn.fallList.size() > 1) {
			clk1Pos = new Point2D.Double(centerPos.getX() + (xt - xp), centerPos.getY());
		}
		Point2D clk0Pos = new Point2D.Double(centerPos.getX() + (xt - xp) / 2.0, centerPos.getY());
		createReplicaReadArcs(clockStg.one, dn.fallList, clk1Pos);
		createReplicaReadArcs(clockStg.zero, dn.riseList, clk0Pos);
	}

	private void createReplicaReadArcFromSignalToOracle(SignalStg signal, SignalStg oracle) throws InvalidConnectionException {
		double x = oracle.fallList.get(0).getRootSpaceX();
		double yFall = oracle.fallList.get(0).getRootSpaceY();
		double yRise = oracle.riseList.get(0).getRootSpaceY();
		double y = ((yFall > yRise) ? yFall + 2.0 : yFall - 2.0);
		createReplicaReadArcs(signal.one, oracle.fallList, new Point2D.Double(x, y));
	}

	private void createReplicaReadArcsFromClockToCombinational(SignalStg rdy) throws InvalidConnectionException {
		double xt = 0.0;
		for (VisualSignalTransition t: rdy.getAllTransitions()) {
			xt += t.getRootSpaceX();
		}
		xt /= rdy.getAllTransitions().size();
		double xp = 0.0;
		double yp = 0.0;
		for (VisualPlace p: rdy.getAllPlaces()) {
			xp += p.getRootSpaceX();
			yp += p.getRootSpaceY();
		}
		xp /=  rdy.getAllPlaces().size();
		yp /=  rdy.getAllPlaces().size();
		Point2D clk0Pos = new Point2D.Double(xt + (xt - xp) / 2.0, yp);
		createReplicaReadArcs(clockStg.zero, rdy.getAllTransitions(), clk0Pos);
	}

	private void createReplicaReadArcsFromClockToSequential(SignalStg rdy) throws InvalidConnectionException {
		createReplicaReadArcs(clockStg.one, rdy.getAllTransitions(), getSignalCenterPosition(rdy));
	}

	private Point2D getSignalCenterPosition(SignalStg signal) {
		double x = 0.0;
		for (VisualSignalTransition t: signal.getAllTransitions()) {
			x += t.getRootSpaceX();
		}
		x /= signal.getAllTransitions().size();

		double y = 0.0;
		for (VisualPlace p: signal.getAllPlaces()) {
			y += p.getRootSpaceY();
		}
		y /= signal.getAllPlaces().size();

		return new Point2D.Double(x, y);
	}

	private SignalStg generateBasicSignalStg(String signalName, double x, double y, SignalTransition.Type type) throws InvalidConnectionException {
		VisualPlace zero = stg.createPlace(signalName + name0, null);
		zero.getReferencedPlace().setTokens(1);
		zero.setNamePositioning(Positioning.BOTTOM);
		zero.setLabelPositioning(Positioning.TOP);
		setPosition(zero, x + 0.0, y + 2.0);

		VisualPlace one = stg.createPlace(signalName + name1, null);
		one.getReferencedPlace().setTokens(0);
		one.setNamePositioning(Positioning.TOP);
		one.setLabelPositioning(Positioning.BOTTOM);
		setPosition(one, x + 0.0, y - 2.0);

		VisualSignalTransition fall = stg.createSignalTransition(signalName, type, SignalTransition.Direction.MINUS, null);
		createConsumingArc(one, fall);
		createProducingArc(fall, zero);
		setPosition(fall, x + 4.0, y + 0.0);

		VisualSignalTransition rise = stg.createSignalTransition(signalName, type, SignalTransition.Direction.PLUS, null);
		createConsumingArc(zero, rise);
		createProducingArc(rise, one);
		setPosition(rise, x - 4.0, y - 0.0);

		return new SignalStg(zero, one, fall, rise);
	}

	private SignalStg generateSignalStg(XmasStgType xmasSignalType, String signalName, double x, double y) throws InvalidConnectionException {
		return generateSignalStg(xmasSignalType, signalName, new Point2D.Double(x, y), 1, 1);
	}

	private SignalStg generateSignalStg(XmasStgType xmasSignalType, String signalName, double x, double y, int fallCount, int riseCount) throws InvalidConnectionException {
		return generateSignalStg(xmasSignalType, signalName, new Point2D.Double(x, y), fallCount, riseCount);
	}

	private SignalStg generateSignalStg(XmasStgType xmasSignalType, String signalName, Point2D pos, int fallCount, int riseCount) throws InvalidConnectionException {
		double x = pos.getX();
		double y = pos.getY();
		int xSign = getXSign(xmasSignalType);
		int ySign = getYSign(xmasSignalType);
		SignalTransition.Type type = getSignalType(xmasSignalType);

		VisualPlace zero = stg.createPlace(signalName + name0, null);
		zero.getReferencedPlace().setTokens(1);
		zero.setNamePositioning((ySign < 0) ? Positioning.BOTTOM : Positioning.TOP);
		zero.setLabelPositioning((ySign < 0) ? Positioning.TOP : Positioning.BOTTOM);
		setPosition(zero, x + xSign * 4.0, y + ySign * 2.0);

		VisualPlace one = stg.createPlace(signalName + name1, null);
		one.getReferencedPlace().setTokens(0);
		one.setNamePositioning((ySign < 0) ? Positioning.TOP : Positioning.BOTTOM);
		one.setLabelPositioning((ySign < 0) ? Positioning.BOTTOM : Positioning.TOP);
		setPosition(one, x + xSign * 4.0, y - ySign * 2.0);

		ArrayList<VisualSignalTransition> fallList = new ArrayList<>(fallCount);
		for (int i = fallCount-1; i >= 0; --i) {
			VisualSignalTransition fall = stg.createSignalTransition(signalName, type, SignalTransition.Direction.MINUS, null);
			createConsumingArc(one, fall);
			createProducingArc(fall, zero);
			setPosition(fall, x + 0.0, y + ySign * (2.0 + i));
			fallList.add(fall);
		}

		ArrayList<VisualSignalTransition> riseList = new ArrayList<>(riseCount);
		for (int i = riseCount-1; i >= 0; --i) {
			VisualSignalTransition rise = stg.createSignalTransition(signalName, type, SignalTransition.Direction.PLUS, null);
			createConsumingArc(zero, rise);
			createProducingArc(rise, one);
			setPosition(rise, x + 0.0, y - ySign * (2.0 + i));
			riseList.add(rise);
		}

		return new SignalStg(zero, one, fallList, riseList);
	}

	private int getXSign(XmasStgType type) {
		if ((type == XmasStgType.TORACLE) || (type == XmasStgType.TRDY) || (type == XmasStgType.TDN)) {
			return -1;
		}
		return +1;
	}

	private int getYSign(XmasStgType type) {
		if ((type == XmasStgType.TORACLE) || (type == XmasStgType.TRDY) || (type == XmasStgType.IDN)) {
			return -1;
		}
		return +1;
	}

	private SignalTransition.Type getSignalType(XmasStgType type) {
		if ((type == XmasStgType.IORACLE) || (type == XmasStgType.TORACLE)) {
			return Type.INPUT;
		}
		if ((type == XmasStgType.IDN) || (type == XmasStgType.TDN)) {
			return Type.OUTPUT;
		}
		return Type.INTERNAL;
	}

	private void setSignalInitialState(SignalStg signalStg, boolean initToOne) {
		if (initToOne) {
			signalStg.one.getReferencedPlace().setTokens(1);
			signalStg.zero.getReferencedPlace().setTokens(0);
		} else {
			signalStg.one.getReferencedPlace().setTokens(0);
			signalStg.zero.getReferencedPlace().setTokens(1);
		}
	}


	public ContactStg getContactStg(VisualXmasContact contact) {
		return contactMap.get(contact);
	}

	private Point2D getComponentPosition(VisualXmasComponent component) {
		AffineTransform transform = TransformHelper.getTransformToRoot(component);
		double x =	xScaling * (transform.getTranslateX() + component.getX());
		double y =	yScaling * (transform.getTranslateY() + component.getY());
		return new Point2D.Double(x, y);
	}

	private void groupComponentStg(NodeStg nodeStg) {
//		stg.select(nodeStg.getAllNodes());
//		stg.groupSelection();
	}


	private SignalStg generateClockStg() throws InvalidConnectionException {
		String name = "clk";
		SignalStg clockStg = generateBasicSignalStg(name, 60.0, 25.0, Type.INPUT);
		setSignalInitialState(clockStg, true);
		groupComponentStg(clockStg);
		return clockStg;
	}

	private void connectClockStg()  throws InvalidConnectionException {
		for(VisualSourceComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSourceComponent.class)) {
			SourceStg sourceStg = getSourceStg(component);
			if (sourceStg != null) {
				createReplicaReadArcsFromDoneToClock(sourceStg.o.dn);
			}
		}
		for(VisualSinkComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSinkComponent.class)) {
			SinkStg sinkStg = getSinkStg(component);
			if (sinkStg != null) {
				createReplicaReadArcsFromDoneToClock(sinkStg.i.dn);
			}
		}
		for(VisualFunctionComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualFunctionComponent.class)) {
			FunctionStg funcStg = getFunctionStg(component);
			if (funcStg != null) {
				createReplicaReadArcsFromDoneToClock(funcStg.i.dn);
				createReplicaReadArcsFromDoneToClock(funcStg.o.dn);
			}
		}
		for(VisualForkComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualForkComponent.class)) {
			ForkStg forkStg = getForkStg(component);
			if (forkStg != null) {
				createReplicaReadArcsFromDoneToClock(forkStg.i.dn);
				createReplicaReadArcsFromDoneToClock(forkStg.a.dn);
				createReplicaReadArcsFromDoneToClock(forkStg.b.dn);
			}
		}
		for(VisualJoinComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualJoinComponent.class)) {
			JoinStg joinStg = getJoinStg(component);
			if (joinStg != null) {
				createReplicaReadArcsFromDoneToClock(joinStg.a.dn);
				createReplicaReadArcsFromDoneToClock(joinStg.b.dn);
				createReplicaReadArcsFromDoneToClock(joinStg.o.dn);
			}
		}
		for(VisualSwitchComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualSwitchComponent.class)) {
			SwitchStg switchStg = getSwitchStg(component);
			if (switchStg != null) {
				createReplicaReadArcsFromDoneToClock(switchStg.i.dn);
				createReplicaReadArcsFromDoneToClock(switchStg.a.dn);
				createReplicaReadArcsFromDoneToClock(switchStg.b.dn);
			}
		}
		for(VisualMergeComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualMergeComponent.class)) {
			MergeStg mergeStg = getMergeStg(component);
			if (mergeStg != null) {
				createReplicaReadArcsFromDoneToClock(mergeStg.a.dn);
				createReplicaReadArcsFromDoneToClock(mergeStg.b.dn);
				createReplicaReadArcsFromDoneToClock(mergeStg.o.dn);
			}
		}
		for(VisualQueueComponent component : Hierarchy.getDescendantsOfType(xmas.getRoot(), VisualQueueComponent.class)) {
			QueueStg queueStg = getQueueStg(component);
			if (queueStg != null) {
				createReplicaReadArcsFromDoneToClock(queueStg.i.dn);
				createReplicaReadArcsFromDoneToClock(queueStg.o.dn);
			}
		}
	}

	public SignalStg getClockStg() {
		return clockStg;
	}


	private SourceStg generateSourceStg(VisualSourceComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		SignalStg oracle = generateSignalStg(XmasStgType.IORACLE, name + _ORACLE, pos.getX() - 10.0, pos.getY());
		ContactStg o = null;
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isOutput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _O_IRDY, pos.getX() + 0.0, pos.getY());
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _O_IDN, pos.getX() + 0.0, pos.getY() - 8.0, 1, 2);
				o = new ContactStg(rdy, dn);
				contactMap.put(contact, o);
			}
		}
		if (o != null) {
			if (oracle != null) {
				createReadArc(oracle.zero, o.rdy.fallList.get(0));
				createReadArc(oracle.one, o.rdy.riseList.get(0));
				createReadArc(oracle.zero, o.dn.riseList.get(0));
				createReadArc(oracle.one, o.dn.riseList.get(1));
			}
			createReadArc(o.rdy.zero, o.dn.riseList.get(0));
			createReadArc(o.rdy.one, o.dn.riseList.get(1));
		}
		SourceStg sourceStg = new SourceStg(o, oracle);
		groupComponentStg(sourceStg);
		return sourceStg;
	}

	private void connectSourceStg(VisualSourceComponent component) throws InvalidConnectionException {
		SourceStg sourceStg = getSourceStg(component);
		if (sourceStg != null) {
			VisualXmasContact oContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isOutput()) {
					oContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (oContact != null) {
				ContactStg o = getContactStg(oContact);
				if (o != null) {
					createReplicaReadArcFromSignalToOracle(o.rdy, sourceStg.oracle);
				}
				if (clockStg != null) {
					createReplicaReadArcsFromClockToSequential(sourceStg.oracle);
					createReplicaReadArcsFromClockToCombinational(sourceStg.o.rdy);
					createReplicaReadArcsFromClockToDone(sourceStg.o.dn);
				}
			}
		}
	}

	public SourceStg getSourceStg(VisualSourceComponent component) {
		return sourceMap.get(component);
	}


	private SinkStg generateSinkStg(VisualSinkComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		SignalStg oracle = generateSignalStg(XmasStgType.TORACLE, name + _ORACLE, pos.getX() + 10.0, pos.getY());
		ContactStg i = null;
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isInput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _I_TRDY, pos.getX() - 0.0, pos.getY());
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _I_TDN, pos.getX() - 0.0, pos.getY() + 8.0, 1, 2);
				i = new ContactStg(rdy, dn);
				contactMap.put(contact, i);
			}
		}
		if (i != null) {
			if (oracle != null) {
				createReadArc(oracle.zero, i.rdy.fallList.get(0));
				createReadArc(oracle.one, i.rdy.riseList.get(0));
				createReadArc(oracle.zero, i.dn.riseList.get(0));
				createReadArc(oracle.one, i.dn.riseList.get(1));
			}
			createReadArc(i.rdy.zero, i.dn.riseList.get(0));
			createReadArc(i.rdy.one, i.dn.riseList.get(1));
		}
		SinkStg sinkStg = new SinkStg(i, oracle);
		groupComponentStg(sinkStg);
		return sinkStg;
	}

	private void connectSinkStg(VisualSinkComponent component) throws InvalidConnectionException {
		SinkStg sinkStg = getSinkStg(component);
		if (sinkStg != null) {
			VisualXmasContact iContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isInput()) {
					iContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (iContact != null) {
				ContactStg i = getContactStg(iContact);
				if (i != null) {
					createReplicaReadArcFromSignalToOracle(i.rdy, sinkStg.oracle);
				}
				if (clockStg != null) {
					createReplicaReadArcsFromClockToSequential(sinkStg.oracle);
					createReplicaReadArcsFromClockToCombinational(sinkStg.i.rdy);
					createReplicaReadArcsFromClockToDone(sinkStg.i.dn);
				}
			}
		}
	}

	public SinkStg getSinkStg(VisualSinkComponent component) {
		return sinkMap.get(component);
	}


	private FunctionStg generateFunctionStg(VisualFunctionComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		ContactStg i = null;
		ContactStg o = null;
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isInput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _I_TRDY, pos.getX(), pos.getY() + 4.0);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _I_TDN, pos.getX(), pos.getY() + 12.0, 1, 2);
				i = new ContactStg(rdy, dn);
				contactMap.put(contact, i);
			} else {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _O_IRDY, pos.getX(), pos.getY() - 4.0);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _O_IDN, pos.getX(), pos.getY() - 12.0, 1, 2);
				o = new ContactStg(rdy, dn);
				contactMap.put(contact, o);
			}
		}
		if (i != null) {
			createReadArc(i.rdy.zero, i.dn.riseList.get(0));
			createReadArc(i.rdy.one, i.dn.riseList.get(1));
		}
		if (o != null) {
			createReadArc(o.rdy.zero, o.dn.riseList.get(0));
			createReadArc(o.rdy.one, o.dn.riseList.get(1));
		}
		FunctionStg functionStg = new FunctionStg(i, o);
		groupComponentStg(functionStg);
		return functionStg;
	}

	private void connectFunctionStg(VisualFunctionComponent component) throws InvalidConnectionException {
		FunctionStg functionStg = getFunctionStg(component);
		if (functionStg != null) {
			VisualXmasContact iContact = null;
			VisualXmasContact oContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isInput()) {
					iContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else {
					oContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (iContact != null) {
				ContactStg i = getContactStg(iContact);
				if (i != null) {
					createReplicaReadArcBetweenSignals(i.rdy, functionStg.o.rdy);
					createReplicaReadArcBetweenDoneSignals(i.dn, functionStg.o.dn, 0.0);
					createReplicaReadArc(i.rdy.zero, functionStg.o.dn.riseList.get(0));
					createReplicaReadArc(i.rdy.one, functionStg.o.dn.riseList.get(1));
				}
			}
			if (oContact != null) {
				ContactStg o = getContactStg(oContact);
				if (o != null) {
					createReplicaReadArcBetweenSignals(o.rdy, functionStg.i.rdy);
					createReplicaReadArcBetweenDoneSignals(o.dn, functionStg.i.dn, 0.0);
					createReplicaReadArc(o.rdy.zero, functionStg.i.dn.riseList.get(0));
					createReplicaReadArc(o.rdy.one, functionStg.i.dn.riseList.get(1));
				}
			}
			if (clockStg != null) {
				createReplicaReadArcsFromClockToDone(functionStg.i.dn);
				createReplicaReadArcsFromClockToCombinational(functionStg.i.rdy);
				createReplicaReadArcsFromClockToDone(functionStg.o.dn);
				createReplicaReadArcsFromClockToCombinational(functionStg.o.rdy);
			}
		}

	}

	public FunctionStg getFunctionStg(VisualFunctionComponent component) {
		return functionMap.get(component);
	}


	private ForkStg generateForkStg(VisualForkComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		ContactStg i = null;
		ContactStg a = null;
		ContactStg b = null;
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isInput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _I_TRDY, pos.getX(), pos.getY() - 4.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _I_TDN, pos.getX(), pos.getY() + 4.0, 1, 3);
				i = new ContactStg(rdy, dn);
				contactMap.put(contact, i);
			} else if (a == null) {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _A_IRDY, pos.getX(), pos.getY() - 12.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _A_IDN, pos.getX(), pos.getY() - 20.0, 1, 3);
				a = new ContactStg(rdy, dn);
				contactMap.put(contact, a);
			} else {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _B_IRDY, pos.getX(), pos.getY() + 20.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _B_IDN, pos.getX(), pos.getY() + 12.0, 1, 3);
				b = new ContactStg(rdy, dn);
				contactMap.put(contact, b);
			}
		}
		if (i != null) {
			createReadArc(i.rdy.zero, i.dn.riseList.get(0));
			createReadArc(i.rdy.zero, i.dn.riseList.get(1));
			createReadArc(i.rdy.one, i.dn.riseList.get(2));
		}
		if (a != null) {
			createReadArc(a.rdy.zero, a.dn.riseList.get(0));
			createReadArc(a.rdy.zero, a.dn.riseList.get(1));
			createReadArc(a.rdy.one, a.dn.riseList.get(2));
		}
		if (b != null) {
			createReadArc(b.rdy.zero, b.dn.riseList.get(0));
			createReadArc(b.rdy.zero, b.dn.riseList.get(1));
			createReadArc(b.rdy.one, b.dn.riseList.get(2));
		}
		ForkStg forkStg = new ForkStg(i, a, b);
		groupComponentStg(forkStg);
		return forkStg;
	}

	private void connectForkStg(VisualForkComponent component) throws InvalidConnectionException {
		ForkStg forkStg = getForkStg(component);
		if (forkStg != null) {
			VisualXmasContact iContact = null;
			VisualXmasContact aContact = null;
			VisualXmasContact bContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isInput()) {
					iContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else if (aContact == null) {
					aContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else {
					bContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (iContact != null) {
				ContactStg i = getContactStg(iContact);
				if (i != null) {
					createReplicaReadArc(i.rdy.zero, forkStg.a.rdy.fallList.get(1), -6.0, 0.0);
					createReplicaReadArc(i.rdy.zero, forkStg.b.rdy.fallList.get(1), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, forkStg.a.rdy.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, forkStg.b.rdy.riseList.get(0), -6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(i.dn, forkStg.a.dn, 0.0);
					createReplicaReadArcBetweenDoneSignals(i.dn, forkStg.b.dn, 0.0);
					createReplicaReadArc(i.rdy.zero, forkStg.a.dn.riseList.get(1), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, forkStg.a.dn.riseList.get(2), -6.0, -1.0);
					createReplicaReadArc(i.rdy.zero, forkStg.b.dn.riseList.get(1), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, forkStg.b.dn.riseList.get(2), -6.0, -1.0);
				}
			}
			if (aContact != null) {
				ContactStg a = getContactStg(aContact);
				if (a != null) {
					createReplicaReadArc(a.rdy.zero, forkStg.i.rdy.fallList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.zero, forkStg.b.rdy.fallList.get(0), -6.0, 0.0);
					createReplicaReadArc(a.rdy.one, forkStg.i.rdy.riseList.get(0), +6.0, -1.0);
					createReplicaReadArc(a.rdy.one, forkStg.b.rdy.riseList.get(0), -6.0, +1.0);
					createReplicaReadArcBetweenDoneSignals(a.dn, forkStg.i.dn, -1.0);
					createReplicaReadArc(a.rdy.zero, forkStg.b.dn.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(a.rdy.one, forkStg.b.dn.riseList.get(2), -6.0, 0.0);
					createReplicaReadArc(a.rdy.zero, forkStg.i.dn.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.one, forkStg.i.dn.riseList.get(2), +6.0, 0.0);
				}
			}
			if (bContact != null) {
				ContactStg b = getContactStg(bContact);
				if (b != null) {
					createReplicaReadArc(b.rdy.zero, forkStg.i.rdy.fallList.get(1), +6.0, 0.0);
					createReplicaReadArc(b.rdy.zero, forkStg.a.rdy.fallList.get(0), -6.0, 0.0);
					createReplicaReadArc(b.rdy.one, forkStg.i.rdy.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(b.rdy.one, forkStg.a.rdy.riseList.get(0), -6.0, +1.0);
					createReplicaReadArcBetweenDoneSignals(b.dn, forkStg.i.dn, 0.0);
					createReplicaReadArc(b.rdy.zero, forkStg.a.dn.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(b.rdy.one, forkStg.a.dn.riseList.get(2), -6.0, 0.0);
					createReplicaReadArc(b.rdy.zero, forkStg.i.dn.riseList.get(1), +6.0, 0.0);
					createReplicaReadArc(b.rdy.one, forkStg.i.dn.riseList.get(2), +6.0, +1.0);
				}
			}
			if (clockStg != null) {
				createReplicaReadArcsFromClockToDone(forkStg.i.dn);
				createReplicaReadArcsFromClockToCombinational(forkStg.i.rdy);
				createReplicaReadArcsFromClockToDone(forkStg.a.dn);
				createReplicaReadArcsFromClockToCombinational(forkStg.a.rdy);
				createReplicaReadArcsFromClockToDone(forkStg.b.dn);
				createReplicaReadArcsFromClockToCombinational(forkStg.b.rdy);
			}
		}
	}

	public ForkStg getForkStg(VisualForkComponent component) {
		return forkMap.get(component);
	}


	private JoinStg generateJoinStg(VisualJoinComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		ContactStg a = null;
		ContactStg b = null;
		ContactStg o = null;
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isOutput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _O_IRDY, pos.getX(), pos.getY() + 4.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _O_IDN, pos.getX(), pos.getY() - 4.0, 1, 3);
				o = new ContactStg(rdy, dn);
				contactMap.put(contact, o);
			} else if (a == null) {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _A_TRDY, pos.getX(), pos.getY() - 20.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _A_TDN, pos.getX(), pos.getY() - 12.0, 1, 3);
				a = new ContactStg(rdy, dn);
				contactMap.put(contact, a);
			} else {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _B_TRDY, pos.getX(), pos.getY() + 12.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _B_TDN, pos.getX(), pos.getY() + 20.0, 1, 3);
				b = new ContactStg(rdy, dn);
				contactMap.put(contact, b);
			}
		}
		if (a != null) {
			createReadArc(a.rdy.zero, a.dn.riseList.get(0));
			createReadArc(a.rdy.zero, a.dn.riseList.get(1));
			createReadArc(a.rdy.one, a.dn.riseList.get(2));
		}
		if (b != null) {
			createReadArc(b.rdy.zero, b.dn.riseList.get(0));
			createReadArc(b.rdy.zero, b.dn.riseList.get(1));
			createReadArc(b.rdy.one, b.dn.riseList.get(2));
		}
		if (o != null) {
			createReadArc(o.rdy.zero, o.dn.riseList.get(0));
			createReadArc(o.rdy.zero, o.dn.riseList.get(1));
			createReadArc(o.rdy.one, o.dn.riseList.get(2));
		}
		JoinStg joinStg = new JoinStg(a, b, o);
		groupComponentStg(joinStg);
		return joinStg;
	}

	private void connectJoinStg(VisualJoinComponent component) throws InvalidConnectionException {
		JoinStg joinStg = getJoinStg(component);
		if (joinStg != null) {
			VisualXmasContact aContact = null;
			VisualXmasContact bContact = null;
			VisualXmasContact oContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isOutput()) {
					oContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else if (aContact == null) {
					aContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else {
					bContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (aContact != null) {
				ContactStg a = getContactStg(aContact);
				if (a != null) {
					createReplicaReadArc(a.rdy.zero, joinStg.b.rdy.fallList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.zero, joinStg.o.rdy.fallList.get(1), -6.0, 0.0);
					createReplicaReadArc(a.rdy.one, joinStg.b.rdy.riseList.get(0), +6.0, -1.0);
					createReplicaReadArc(a.rdy.one, joinStg.o.rdy.riseList.get(0), -6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(a.dn, joinStg.o.dn, 0.0);
					createReplicaReadArc(a.rdy.zero, joinStg.o.dn.riseList.get(1), -6.0, 0.0);
					createReplicaReadArc(a.rdy.one, joinStg.o.dn.riseList.get(2), -6.0, -1.0);
					createReplicaReadArc(a.rdy.zero, joinStg.b.dn.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.one, joinStg.b.dn.riseList.get(2), +6.0, 0.0);
				}
			}
			if (bContact != null) {
				ContactStg b = getContactStg(bContact);
				if (b != null) {
					createReplicaReadArc(b.rdy.zero, joinStg.a.rdy.fallList.get(1), +6.0, 0.0);
					createReplicaReadArc(b.rdy.zero, joinStg.o.rdy.fallList.get(0), -6.0, 0.0);
					createReplicaReadArc(b.rdy.one, joinStg.a.rdy.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(b.rdy.one, joinStg.o.rdy.riseList.get(0), -6.0, +1.0);
					createReplicaReadArcBetweenDoneSignals(b.dn, joinStg.o.dn, +1.0);
					createReplicaReadArc(b.rdy.zero, joinStg.o.dn.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(b.rdy.one, joinStg.o.dn.riseList.get(2), -6.0, 0.0);
					createReplicaReadArc(b.rdy.zero, joinStg.a.dn.riseList.get(1), +6.0, 0.0);
					createReplicaReadArc(b.rdy.one, joinStg.a.dn.riseList.get(2), +6.0, +1.0);
				}
			}
			if (oContact != null) {
				ContactStg o = getContactStg(oContact);
				if (o != null) {
					createReplicaReadArc(o.rdy.zero, joinStg.a.rdy.fallList.get(0), +6.0, 0.0);
					createReplicaReadArc(o.rdy.zero, joinStg.b.rdy.fallList.get(1), +6.0, 0.0);
					createReplicaReadArc(o.rdy.one, joinStg.a.rdy.riseList.get(0), +6.0, -1.0);
					createReplicaReadArc(o.rdy.one, joinStg.b.rdy.riseList.get(0), +6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(o.dn, joinStg.a.dn, 0.0);
					createReplicaReadArcBetweenDoneSignals(o.dn, joinStg.b.dn, 0.0);
					createReplicaReadArc(o.rdy.zero, joinStg.a.dn.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(o.rdy.one, joinStg.a.dn.riseList.get(2), +6.0, 0.0);
					createReplicaReadArc(o.rdy.zero, joinStg.b.dn.riseList.get(1), +6.0, 0.0);
					createReplicaReadArc(o.rdy.one, joinStg.b.dn.riseList.get(2), +6.0, +1.0);
				}
			}
		}
	}

	public JoinStg getJoinStg(VisualJoinComponent component) {
		return joinMap.get(component);
	}


	private SwitchStg generateSwitchStg(VisualSwitchComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		ContactStg i = null;
		ContactStg a = null;
		ContactStg b = null;
		SignalStg oracle = generateSignalStg(XmasStgType.IORACLE, name + _ORACLE, pos.getX() + 12.0, pos.getY());
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isInput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _I_TRDY, pos.getX(), pos.getY() - 4.0, 1, 2);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _I_TDN, pos.getX(), pos.getY() + 4.0, 1, 3);
				i = new ContactStg(rdy, dn);
				contactMap.put(contact, i);
			} else if (a == null) {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _A_IRDY, pos.getX(), pos.getY() - 12.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _A_IDN, pos.getX(), pos.getY() - 20.0, 1, 3);
				a = new ContactStg(rdy, dn);
				contactMap.put(contact, a);
			} else {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _B_IRDY, pos.getX(), pos.getY() + 20.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _B_IDN, pos.getX(), pos.getY() + 12.0, 1, 3);
				b = new ContactStg(rdy, dn);
				contactMap.put(contact, b);
			}
		}
		if (i != null) {
			createReadArc(i.rdy.zero, i.dn.riseList.get(0));
			createReadArc(i.rdy.zero, i.dn.riseList.get(1));
			createReadArc(i.rdy.one, i.dn.riseList.get(2));
		}
		if (a != null) {
			createReadArc(a.rdy.zero, a.dn.riseList.get(0));
			createReadArc(a.rdy.zero, a.dn.riseList.get(1));
			createReadArc(a.rdy.one, a.dn.riseList.get(2));
		}
		if (b != null) {
			createReadArc(b.rdy.zero, b.dn.riseList.get(0));
			createReadArc(b.rdy.zero, b.dn.riseList.get(1));
			createReadArc(b.rdy.one, b.dn.riseList.get(2));
		}
		if ((oracle != null) && (a != null) && (b != null)) {
			createReadArcs(a.rdy.zero, oracle.getAllTransitions());
			createReadArcs(b.rdy.zero, oracle.getAllTransitions());
			createReplicaReadArc(oracle.zero, a.rdy.fallList.get(1), -6.0, 0.0);
			createReplicaReadArc(oracle.zero, a.dn.riseList.get(1), -6.0, 0.0);
			createReplicaReadArc(oracle.zero, b.rdy.riseList.get(0), -6.0, +1.0);
			createReplicaReadArc(oracle.zero, b.dn.riseList.get(2), -6.0, -1.0);
			createReplicaReadArc(oracle.one, a.rdy.riseList.get(0), -6.0, +1.0);
			createReplicaReadArc(oracle.one, a.dn.riseList.get(2), -6.0, -1.0);
			createReplicaReadArc(oracle.one, b.rdy.fallList.get(1), -6.0, 0.0);
			createReplicaReadArc(oracle.one, b.dn.riseList.get(1), -6.0, 0.0);
		}
		SwitchStg switchStg = new SwitchStg(i, a, b, oracle);
		groupComponentStg(switchStg);
		return switchStg;
	}

	private void connectSwitchStg(VisualSwitchComponent component) throws InvalidConnectionException {
		SwitchStg switchStg = getSwitchStg(component);
		if (switchStg != null) {
			VisualXmasContact iContact = null;
			VisualXmasContact aContact = null;
			VisualXmasContact bContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isInput()) {
					iContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else if (aContact == null) {
					aContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else {
					bContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (iContact != null) {
				ContactStg i = getContactStg(iContact);
				if (i != null) {
					createReplicaReadArc(i.rdy.zero, switchStg.a.rdy.fallList.get(0), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, switchStg.a.rdy.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(i.rdy.zero, switchStg.b.rdy.fallList.get(0), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, switchStg.b.rdy.riseList.get(0), -6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(i.dn, switchStg.a.dn, 0.0);
					createReplicaReadArcBetweenDoneSignals(i.dn, switchStg.b.dn, 0.0);
					createReplicaReadArc(i.rdy.zero, switchStg.a.dn.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, switchStg.a.dn.riseList.get(2), -6.0,  0.0);
					createReplicaReadArc(i.rdy.zero, switchStg.b.dn.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(i.rdy.one, switchStg.b.dn.riseList.get(2), -6.0, 0.0);
				}
			}
			if (aContact != null) {
				ContactStg a = getContactStg(aContact);
				if (a != null) {
					createReplicaReadArc(a.rdy.zero, switchStg.i.rdy.fallList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.one, switchStg.i.rdy.riseList.get(1), +6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(a.dn, switchStg.i.dn, -1.0);
					createReplicaReadArc(a.rdy.zero, switchStg.i.dn.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.one, switchStg.i.dn.riseList.get(1), +6.0, +1.0);
				}
			}
			if (bContact != null) {
				ContactStg b = getContactStg(bContact);
				if (b != null) {
					createReplicaReadArc(b.rdy.zero, switchStg.i.rdy.fallList.get(0), +6.0, +1.0);
					createReplicaReadArc(b.rdy.one, switchStg.i.rdy.riseList.get(0), +6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(b.dn, switchStg.i.dn, 0.0);
					createReplicaReadArc(b.rdy.zero, switchStg.i.dn.riseList.get(0), +6.0, +1.0);
					createReplicaReadArc(b.rdy.one, switchStg.i.dn.riseList.get(2), +6.0, +1.0);
				}
			}
			if (clockStg != null) {
				createReplicaReadArcsFromClockToDone(switchStg.i.dn);
				createReplicaReadArcsFromClockToCombinational(switchStg.i.rdy);
				createReplicaReadArcsFromClockToDone(switchStg.a.dn);
				createReplicaReadArcsFromClockToCombinational(switchStg.a.rdy);
				createReplicaReadArcsFromClockToDone(switchStg.b.dn);
				createReplicaReadArcsFromClockToCombinational(switchStg.b.rdy);
				createReplicaReadArcsFromClockToSequential(switchStg.oracle);
			}
		}
	}

	public SwitchStg getSwitchStg(VisualSwitchComponent component) {
		return switchMap.get(component);
	}


	private MergeStg generateMergeStg(VisualMergeComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		ContactStg a = null;
		ContactStg b = null;
		ContactStg o = null;
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isOutput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _O_IRDY, pos.getX(), pos.getY() + 4.0, 1, 2);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _O_IDN, pos.getX(), pos.getY() - 4.0, 1, 3);
				o = new ContactStg(rdy, dn);
				contactMap.put(contact, o);
			} else if (a == null) {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _A_TRDY, pos.getX(), pos.getY() - 20.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _A_TDN, pos.getX(), pos.getY() - 12.0, 1, 3);
				a = new ContactStg(rdy, dn);
				contactMap.put(contact, a);
			} else {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _B_TRDY, pos.getX(), pos.getY() + 12.0, 2, 1);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _B_TDN, pos.getX(), pos.getY() + 20.0, 1, 3);
				b = new ContactStg(rdy, dn);
				contactMap.put(contact, b);
			}
		}
		if (a != null) {
			createReadArc(a.rdy.zero, a.dn.riseList.get(0));
			createReadArc(a.rdy.zero, a.dn.riseList.get(1));
			createReadArc(a.rdy.one, a.dn.riseList.get(2));
		}
		if (b != null) {
			createReadArc(b.rdy.zero, b.dn.riseList.get(0));
			createReadArc(b.rdy.zero, b.dn.riseList.get(1));
			createReadArc(b.rdy.one, b.dn.riseList.get(2));
		}
		if (o != null) {
			createReadArc(o.rdy.zero, o.dn.riseList.get(0));
			createReadArc(o.rdy.zero, o.dn.riseList.get(1));
			createReadArc(o.rdy.one, o.dn.riseList.get(2));
		}
		MergeStg mergeStg = new MergeStg(a, b, o);
		groupComponentStg(mergeStg);
		return mergeStg;
	}

	private void connectMergeStg(VisualMergeComponent component) throws InvalidConnectionException {
		MergeStg mergeStg = getMergeStg(component);
		if (mergeStg != null) {
			VisualXmasContact aContact = null;
			VisualXmasContact bContact = null;
			VisualXmasContact oContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isOutput()) {
					oContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else if (aContact == null) {
					aContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else {
					bContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (aContact != null) {
				ContactStg a = getContactStg(aContact);
				if (a != null) {
					createReplicaReadArc(a.rdy.zero, mergeStg.a.rdy.fallList.get(1), +6.0, 0.0);
					createReplicaReadArc(a.rdy.zero, mergeStg.o.rdy.fallList.get(0), -6.0, -1.0);
					createReplicaReadArc(a.rdy.one, mergeStg.a.rdy.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(a.rdy.one, mergeStg.o.rdy.riseList.get(0), -6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(a.dn, mergeStg.a.dn, -1.0);
					createReplicaReadArcBetweenDoneSignals(a.dn, mergeStg.o.dn, 0.0);
					createReplicaReadArc(a.rdy.zero, mergeStg.a.dn.riseList.get(1), +6.0, 0.0);
					createReplicaReadArc(a.rdy.zero, mergeStg.o.dn.riseList.get(0), -6.0, -1.0);
					createReplicaReadArc(a.rdy.one, mergeStg.a.dn.riseList.get(2), +6.0, +1.0);
					createReplicaReadArc(a.rdy.one, mergeStg.o.dn.riseList.get(2), -6.0, -1.0);
				}
			}
			if (bContact != null) {
				ContactStg b = getContactStg(bContact);
				if (b != null) {
					createReplicaReadArc(b.rdy.zero, mergeStg.b.rdy.fallList.get(1), +6.0, 0.0);
					createReplicaReadArc(b.rdy.zero, mergeStg.o.rdy.fallList.get(0), -6.0, 0.0);
					createReplicaReadArc(b.rdy.one, mergeStg.b.rdy.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(b.rdy.one, mergeStg.o.rdy.riseList.get(1), -6.0, 0.0);
					createReplicaReadArcBetweenDoneSignals(b.dn, mergeStg.b.dn, -1.0);
					createReplicaReadArcBetweenDoneSignals(b.dn, mergeStg.o.dn, +1.0);
					createReplicaReadArc(b.rdy.zero, mergeStg.b.dn.riseList.get(1), +6.0, 0.0);
					createReplicaReadArc(b.rdy.zero, mergeStg.o.dn.riseList.get(0), -6.0, 0.0);
					createReplicaReadArc(b.rdy.one, mergeStg.b.dn.riseList.get(2), +6.0, +1.0);
					createReplicaReadArc(b.rdy.one, mergeStg.o.dn.riseList.get(1), -6.0, -1.0);
				}
			}
			if (oContact != null) {
				ContactStg o = getContactStg(oContact);
				if (o != null) {
					createReplicaReadArc(o.rdy.zero, mergeStg.a.rdy.fallList.get(0), +6.0, 0.0);
					createReplicaReadArc(o.rdy.zero, mergeStg.b.rdy.fallList.get(0), +6.0, 0.0);
					createReplicaReadArc(o.rdy.one, mergeStg.a.rdy.riseList.get(0), +6.0, -1.0);
					createReplicaReadArc(o.rdy.one, mergeStg.b.rdy.riseList.get(0), +6.0, -1.0);
					createReplicaReadArcBetweenDoneSignals(o.dn, mergeStg.a.dn, 0.0);
					createReplicaReadArcBetweenDoneSignals(o.dn, mergeStg.b.dn, 0.0);
					createReplicaReadArc(o.rdy.zero, mergeStg.a.dn.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(o.rdy.zero, mergeStg.b.dn.riseList.get(0), +6.0, 0.0);
					createReplicaReadArc(o.rdy.one, mergeStg.a.dn.riseList.get(2), +6.0, 0.0);
					createReplicaReadArc(o.rdy.one, mergeStg.b.dn.riseList.get(2), +6.0, 0.0);
				}
			}
		}
	}

	public MergeStg getMergeStg(VisualMergeComponent component) {
		return mergeMap.get(component);
	}


	private QueueStg generateQueueStg(VisualQueueComponent component) throws InvalidConnectionException {
		String name = xmas.getMathName(component);
		Point2D pos = getComponentPosition(component);
		int capacity = component.getReferencedQueueComponent().getCapacity();
		ContactStg i = null;
		ContactStg o = null;
		ArrayList<SlotStg> slotList = new ArrayList<>(capacity);
		double xContact = 0.5 * QUEUE_SLOT_SPACING * (capacity + 1);
		for (VisualXmasContact contact: component.getContacts()) {
			if (contact.isOutput()) {
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _O_IRDY, pos.getX() + xContact, pos.getY(), 1, capacity);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _O_IDN, pos.getX() + xContact, pos.getY() - 12.0, 1, capacity + 1);
				o = new ContactStg(rdy, dn);
				contactMap.put(contact, o);
			} else {
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _I_TRDY, pos.getX() - xContact, pos.getY(), 1, capacity);
				setSignalInitialState(rdy, true);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _I_TDN, pos.getX() - xContact, pos.getY() + 12.0, 1, capacity + 1);
				i = new ContactStg(rdy, dn);
				contactMap.put(contact, i);
			}
		}
		for (int idx = 0; idx < capacity; idx++) {
			double xSlot = QUEUE_SLOT_SPACING * (idx - 0.5 * (capacity - 1));
			char suffix = (char)idx;
			suffix += 'A';
			SignalStg mem = generateBasicSignalStg(name + _MEM + suffix, pos.getX() + xSlot, pos.getY(), SignalTransition.Type.INPUT);
			ContactStg hd = null;
			{
				SignalStg rdy = generateSignalStg(XmasStgType.IRDY, name + _HEAD + suffix +_RDY, pos.getX() + xSlot, pos.getY() - 8.0);
				SignalStg dn = generateSignalStg(XmasStgType.IDN, name + _HEAD + suffix + _DN, pos.getX() + xSlot, pos.getY() - 16.0, 4, 3);
				hd = new ContactStg(rdy, dn);
			}
			ContactStg tl = null;
			{
				SignalStg rdy = generateSignalStg(XmasStgType.TRDY, name + _TAIL + suffix +_RDY, pos.getX() + xSlot, pos.getY() + 8.0);
				SignalStg dn = generateSignalStg(XmasStgType.TDN, name + _TAIL + suffix + _DN, pos.getX() + xSlot, pos.getY() + 16.0, 4, 3);
				setSignalInitialState(rdy, (idx == 0));
				tl = new ContactStg(rdy, dn);
			}
			SlotStg slot = new SlotStg(mem, hd, tl);
			slotList.add(slot);
			// Connections of head within slot
			createReplicaReadArc(slot.hd.rdy.one, slot.mem.fallList.get(0), 0.0, -2.0);
			createReplicaReadArc(slot.mem.zero, slot.hd.rdy.fallList.get(0), -6.0, 0.0);
			createReplicaReadArc(slot.mem.one, slot.hd.rdy.riseList.get(0), -6.0, 0.0);
			createReplicaReadArc(slot.mem.zero, slot.hd.dn.fallList.get(1), +6.0, -1.0);
			createReplicaReadArc(slot.mem.zero, slot.hd.dn.riseList.get(0), -6.0, 0.0);
			createReplicaReadArcs(slot.mem.one, Arrays.asList(slot.hd.dn.riseList.get(2), slot.hd.dn.riseList.get(1)), -6.0, 0.0);
			createReplicaReadArc(slot.mem.one, slot.hd.dn.fallList.get(0), -6.0, 0.0);
			createReplicaReadArc(slot.mem.one, slot.hd.dn.fallList.get(2), -6.0, 0.0);
			createReadArc(slot.hd.rdy.zero, slot.hd.dn.riseList.get(0));
			createReadArc(slot.hd.rdy.zero, slot.hd.dn.fallList.get(3));
			createReadArc(slot.hd.rdy.one, slot.hd.dn.riseList.get(2));
			createReadArc(slot.hd.rdy.one, slot.hd.dn.fallList.get(2));
			createReadArc(slot.hd.rdy.one, slot.hd.dn.fallList.get(1));
			createReadArc(slot.hd.rdy.one, slot.hd.dn.fallList.get(0));
			// Connection of tail within slot
			createReplicaReadArc(slot.tl.rdy.one, slot.mem.riseList.get(0), 0.0, +2.0);
			createReplicaReadArc(slot.mem.zero, slot.tl.rdy.riseList.get(0), +6.0, 0.0);
			createReplicaReadArc(slot.mem.one, slot.tl.rdy.fallList.get(0), +6.0, 0.0);
			createReplicaReadArc(slot.mem.one, slot.tl.dn.fallList.get(1), -6.0, +1.0);
			createReplicaReadArc(slot.mem.one, slot.tl.dn.riseList.get(0), +6.0, 0.0);
			createReplicaReadArcs(slot.mem.zero, Arrays.asList(slot.tl.dn.riseList.get(2), slot.tl.dn.riseList.get(1)), +6.0, 0.0);
			createReplicaReadArc(slot.mem.zero, slot.tl.dn.fallList.get(0), +6.0, 0.0);
			createReplicaReadArc(slot.mem.zero, slot.tl.dn.fallList.get(2), +6.0, 0.0);
			createReadArc(slot.tl.rdy.zero, slot.tl.dn.riseList.get(0));
			createReadArc(slot.tl.rdy.zero, slot.tl.dn.fallList.get(3));
			createReadArc(slot.tl.rdy.one, slot.tl.dn.riseList.get(2));
			createReadArc(slot.tl.rdy.one, slot.tl.dn.fallList.get(2));
			createReadArc(slot.tl.rdy.one, slot.tl.dn.fallList.get(1));
			createReadArc(slot.tl.rdy.one, slot.tl.dn.fallList.get(0));
		}
		for (int idx = 0; idx < capacity; idx++) {
			SlotStg slot = slotList.get(idx);
			// Connections with input port
			createReplicaReadArc(i.rdy.one, slot.mem.riseList.get(0), -6.0, -1.0);
			createReplicaReadArc(i.rdy.one, slot.tl.dn.fallList.get(1), -6.0, 0.0);
			createReplicaReadArc(i.rdy.zero, slot.tl.dn.fallList.get(2), +6.0, -1.0);
			createReplicaReadArcs(slot.mem.one, Arrays.asList(i.rdy.fallList.get(0), i.dn.riseList.get(0)), +6.0, -idx);
			createReplicaReadArcs(slot.mem.zero, Arrays.asList(i.rdy.riseList.get(idx), i.dn.riseList.get(capacity - idx)), +6.0, 0.0);
			createReplicaReadArcs(slot.hd.dn.zero, i.dn.fallList, -6.0, +1.0 + idx);
			createReplicaReadArcs(slot.hd.dn.one, i.dn.riseList, -6.0, +1.0 - idx);
			createReplicaReadArcs(slot.tl.dn.zero, i.dn.fallList, +6.0, +1.0 + idx);
			createReplicaReadArcs(slot.tl.dn.one, i.dn.riseList, +6.0, +1.0 - idx);
			// Connections with output port
			createReplicaReadArc(o.rdy.one, slot.mem.fallList.get(0), +6.0, +1.0);
			createReplicaReadArc(o.rdy.one, slot.hd.dn.fallList.get(1), +6.0, 0.0);
			createReplicaReadArc(o.rdy.zero, slot.hd.dn.fallList.get(2), -6.0, 1.0);
			createReplicaReadArcs(slot.mem.zero, Arrays.asList(o.rdy.fallList.get(0), o.dn.riseList.get(0)), -6.0, +idx);
			createReplicaReadArcs(slot.mem.one, Arrays.asList(o.rdy.riseList.get(idx), o.dn.riseList.get(capacity - idx)), -6.0, 0.0);
			createReplicaReadArcs(slot.hd.dn.zero, o.dn.fallList, -6.0, -1.0 - idx);
			createReplicaReadArcs(slot.hd.dn.one, o.dn.riseList, -6.0, -1.0 + idx);
			createReplicaReadArcs(slot.tl.dn.zero, o.dn.fallList, +6.0, -1.0 - idx);
			createReplicaReadArcs(slot.tl.dn.one, o.dn.riseList, +6.0, -1.0 + idx);
			// Connections with the next slot
			SlotStg nextSlot = slotList.get((idx + 1) % capacity);
			createReplicaReadArc(slot.mem.zero, nextSlot.hd.rdy.riseList.get(0), -6.0, +1.0);
			createReplicaReadArc(slot.mem.zero, nextSlot.hd.dn.riseList.get(2), -6.0, -1.0);
			createReplicaReadArc(slot.mem.one, nextSlot.hd.dn.riseList.get(1), -6.0, 0.0);
			createReplicaReadArc(slot.mem.one, nextSlot.tl.rdy.riseList.get(0), +6.0, -1.0);
			createReplicaReadArc(slot.mem.zero, nextSlot.tl.dn.riseList.get(1), +6.0, 0.0);
			createReplicaReadArc(slot.mem.one, nextSlot.tl.dn.riseList.get(2), +6.0, +1.0);
		}
		if (i != null) {
			createReadArc(i.rdy.zero, i.dn.riseList.get(0));
			createReadArc(i.rdy.one, i.dn.riseList.get(1));
			createReadArc(i.rdy.one, i.dn.riseList.get(2));
		}
		if (o != null) {
			createReadArc(o.rdy.zero, o.dn.riseList.get(0));
			createReadArc(o.rdy.one, o.dn.riseList.get(1));
			createReadArc(o.rdy.one, o.dn.riseList.get(2));
		}

		QueueStg queueStg = new QueueStg(i, o, slotList);
		groupComponentStg(queueStg);
		return queueStg;
	}

	private void connectQueueStg(VisualQueueComponent component) throws InvalidConnectionException {
		QueueStg queueStg = getQueueStg(component);
		if (queueStg != null) {
			VisualXmasContact iContact = null;
			VisualXmasContact oContact = null;
			for (VisualXmasContact contact: component.getContacts()) {
				if (contact.isOutput()) {
					oContact =  XmasUtils.getConnectedContact(xmas, contact);
				} else {
					iContact =  XmasUtils.getConnectedContact(xmas, contact);
				}
			}
			if (iContact != null) {
				ContactStg i = getContactStg(iContact);
				if (i != null) {
					for (SlotStg slot: queueStg.slotList) {
						createReplicaReadArc(i.rdy.one, slot.mem.riseList.get(0), -6.0, -2.0);
					}
				}
				createReplicaReadArcs(i.dn.zero, queueStg.o.dn.fallList, -6.0, 0.0);
				createReplicaReadArcs(i.dn.one, queueStg.o.dn.riseList, -6.0, -2.0);
				for (SlotStg slot: queueStg.slotList) {
					createReplicaReadArc(i.rdy.zero, slot.tl.dn.fallList.get(0), +6.0, -1.0);
					createReplicaReadArc(i.rdy.one, slot.tl.dn.fallList.get(1), -6.0, -1.0);
				}
			}
			if (oContact != null) {
				ContactStg o = getContactStg(oContact);
				if (o != null) {
					for (SlotStg slot: queueStg.slotList) {
						createReplicaReadArc(o.rdy.one, slot.mem.fallList.get(0), +6.0, +2.0);
					}
				}
				createReplicaReadArcs(o.dn.zero, queueStg.i.dn.fallList, +6.0, 0.0);
				createReplicaReadArcs(o.dn.one, queueStg.i.dn.riseList, +6.0, +2.0);
				for (SlotStg slot: queueStg.slotList) {
					createReplicaReadArc(o.rdy.zero, slot.hd.dn.fallList.get(0), -6.0, +1.0);
					createReplicaReadArc(o.rdy.one, slot.hd.dn.fallList.get(1), +6.0, +1.0);
				}
			}
			if (clockStg != null) {
				createReplicaReadArcsFromClockToDone(queueStg.i.dn);
				createReplicaReadArcsFromClockToCombinational(queueStg.i.rdy);
				createReplicaReadArcsFromClockToDone(queueStg.o.dn);
				createReplicaReadArcsFromClockToCombinational(queueStg.o.rdy);
				for (SlotStg slot: queueStg.slotList) {
					createReplicaReadArcsFromClockToSequential(slot.mem);
					createReplicaReadArcsFromClockToCombinational(slot.hd.rdy);
					createReplicaReadArcsFromClockToDone(slot.hd.dn);
					createReplicaReadArcsFromClockToCombinational(slot.tl.rdy);
					createReplicaReadArcsFromClockToDone(slot.tl.dn);
				}
			}
		}
	}

	public QueueStg getQueueStg(VisualQueueComponent component) {
		return queueMap.get(component);
	}


	public boolean isRelated(Node highLevelNode, Node node) {
		NodeStg nodeStg = null;
		if (highLevelNode instanceof VisualSourceComponent) {
			nodeStg = getSourceStg((VisualSourceComponent)highLevelNode);
		} else if (highLevelNode instanceof VisualSinkComponent) {
			nodeStg = getSinkStg((VisualSinkComponent)highLevelNode);
		} else if (highLevelNode instanceof VisualFunctionComponent) {
			nodeStg = getFunctionStg((VisualFunctionComponent)highLevelNode);
		}
		return ((nodeStg != null) && nodeStg.contains(node));
	}

}
