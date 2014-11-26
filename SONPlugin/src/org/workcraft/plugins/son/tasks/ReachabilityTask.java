package org.workcraft.plugins.son.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.dom.Node;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.ToolboxPanel;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.plugins.son.Phase;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.algorithm.BSONAlg;
import org.workcraft.plugins.son.algorithm.CSONCycleAlg;
import org.workcraft.plugins.son.algorithm.CycleAlgorithm;
import org.workcraft.plugins.son.algorithm.ONCycleAlg;
import org.workcraft.plugins.son.algorithm.Path;
import org.workcraft.plugins.son.algorithm.RelationAlgorithm;
import org.workcraft.plugins.son.connections.SONConnection.Semantics;
import org.workcraft.plugins.son.elements.ChannelPlace;
import org.workcraft.plugins.son.elements.Condition;
import org.workcraft.plugins.son.elements.PlaceNode;
import org.workcraft.plugins.son.elements.TransitionNode;
import org.workcraft.plugins.son.tools.SONSimulationTool;
import org.workcraft.tasks.ProgressMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.Task;
import org.workcraft.tasks.Result.Outcome;
import org.workcraft.workspace.WorkspaceEntry;

public class ReachabilityTask implements Task<VerificationResult>{

	private SON net;
	private final WorkspaceEntry we;
	private final Framework framework;
	private BSONAlg bsonAlg;

	private Collection<PlaceNode> marking;
	private Map<Condition, Phase> phases;
	private Collection<TransitionNode> causalPredecessors;


	public ReachabilityTask(SON net, WorkspaceEntry we, Framework framework){
		this.net = net;
		this.we = we;
		this.framework = framework;

		bsonAlg = new BSONAlg(net);
		marking = new ArrayList<PlaceNode>();
		causalPredecessors = new HashSet<TransitionNode>();

		for(PlaceNode node : net.getPlaceNodes()){
			if(node.isMarked())
				marking.add(node);
		}

		phases = bsonAlg.getPhases();
	}

	@Override
	public Result<? extends VerificationResult> run(
			ProgressMonitor<? super VerificationResult> monitor) {

		if(marking.isEmpty()){
			JOptionPane.showMessageDialog(null,
					"Double click on condition/channel place or use property editor"
					+ " to mark some nodes and check the reachability.",
					"Marking required", JOptionPane.INFORMATION_MESSAGE);
			return new Result<VerificationResult>(Outcome.FINISHED);
		}

		//cycle detection
		ONCycleAlg cycleAlg = new ONCycleAlg(net);
		if(!cycleAlg.cycleTask(net.getComponents()).isEmpty()){
			we.cancelMemento();
			JOptionPane.showMessageDialog(null,
					"Fail to run reachability anaylsis tool, " +
					"error may due to incorrect structure", "Invalid structure", JOptionPane.WARNING_MESSAGE);
			return new Result<VerificationResult>(Outcome.FINISHED);
		}

		if(reachabilityTask()){
			int result = JOptionPane.showConfirmDialog(null,
					"The selected marking is REACHABLE from initial states. \n" +
					"Select OK to analysis the trace leading to the marking in the simulation tool.",
					"Reachability task result", JOptionPane.OK_CANCEL_OPTION);
			if(result == 0){
				Map<PlaceNode, Boolean> finalStates = simulation();
				for(PlaceNode node : marking){
					if(finalStates.get(node) == false)
						throw new RuntimeException("Reachability algorithm error, doesn't reach selected marking" + net.getNodeReference(node));
				}
				return new Result<VerificationResult>(Outcome.FINISHED);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,
					"The selected marking is UNREACHABLE from initial states",
					"Reachability task result", JOptionPane.INFORMATION_MESSAGE);
		}
		we.cancelMemento();
		return new Result<VerificationResult>(Outcome.FINISHED);
	}

	private Map<PlaceNode, Boolean> simulation(){
		Map<PlaceNode, Boolean> result;
		final MainWindow mainWindow = framework.getMainWindow();
		GraphEditorPanel currentEditor = mainWindow.getCurrentEditor();
		if(currentEditor == null || currentEditor.getWorkspaceEntry() != we) {
			final List<GraphEditorPanel> editors = mainWindow.getEditors(we);
			if(editors.size()>0) {
				currentEditor = editors.get(0);
				mainWindow.requestFocus(currentEditor);
			} else {
				currentEditor = mainWindow.createEditorWindow(we);
			}
		}
		final ToolboxPanel toolbox = currentEditor.getToolBox();
		final SONSimulationTool tool = toolbox.getToolInstance(SONSimulationTool.class);
		toolbox.selectTool(tool);
		result = tool.ReachabilitySimulator(tool.getGraphEditor(), causalPredecessors);
		tool.mergeTrace(tool.getGraphEditor());

		return result;
	}


	private boolean reachabilityTask(){
		Collection<Path> sync = getSyncCycles();
		Collection<Node> syncCycles = new HashSet<Node>();
		for(Path path : sync){
			syncCycles.addAll(path);
		}
		//if marking contains a synchronous channel place, it's unreachable.
		for(PlaceNode node : marking){
			if(node instanceof ChannelPlace)
				if(syncCycles.contains(node)) {
					return false;
				}
		}

		causalPredecessors = new HashSet<TransitionNode>();

		//get CausalPredecessors for each marking
		for(PlaceNode node : marking){
			causalPredecessors.addAll(getCausalPredecessors(node, sync));
		}

		Collection<Node> consume = new HashSet<Node>();

		//get all place nodes which are the input (consumed) of causal predecessors
		for(TransitionNode t : causalPredecessors){
			for(Node pre : net.getPreset(t)){
				consume.add(pre);
			}
		}
//		//test
//		System.out.println();
//		System.out.println("marking = " + net.getNodeReference(node));
//		for(Node n : causalPredecessors){
//			System.out.print(" " + net.getNodeReference(n));
//			net.setForegroundColor(n, Color.RED);
//		}
//		System.out.println();
//		System.out.println();
//		System.out.println("consume:");
//		for(Node n : consume){
//			System.out.print(" " + net.getNodeReference(n));
//		}
//		System.out.println();

		// marking is reachable if
		//1. none of the marked conditions is consumed by causalPredecessors.
		//2. all of corresponding abstract conditions are not consumed by causalPredecessors
		for(PlaceNode node : marking){
			if(consume.contains(node))
				return false;
			Collection<Condition> c = bsonAlg.getAbstractConditions(node);
			if(consume.containsAll(c))
				return false;
		}

		return true;
	}

	private Collection<TransitionNode> getCausalPredecessors (Node node, Collection<Path> sync){
		Collection<TransitionNode> result = new HashSet<TransitionNode>();

		RelationAlgorithm relationAlg = new RelationAlgorithm(net);

		Path path = isInSync(node, sync);

		if(node instanceof TransitionNode){
			if(path.isEmpty()){
				for(Node pre : net.getPreset(node)){
					for(TransitionNode t : CausalRelations(pre)){
						result.add(t);
						result.addAll(getCausalPredecessors(t, sync));
					}
				}
			}else{
				for(Node n : path){
					if(n instanceof TransitionNode)
						result.add((TransitionNode)n);
				}
				for(Node pre : relationAlg.getPreset(path)){
					for(TransitionNode t : CausalRelations(pre)){
						result.add(t);
						result.addAll(getCausalPredecessors(t, sync));
					}
				}
			}
		}
		else{
			for(TransitionNode t : CausalRelations(node)){
				result.add(t);
				result.addAll(getCausalPredecessors(t, sync));
			}
		}
		return result;
	}

	private List<TransitionNode> CausalRelations(Node pre){
		List<Node> causalSet = new ArrayList<Node>();
		RelationAlgorithm relationAlg = new RelationAlgorithm(net);

		//if condition is not max/min phase, add its pre-event to the set.
		if((pre instanceof Condition) && !(net.getSONConnectionTypes(pre).contains(Semantics.BHVLINE))){
			causalSet.addAll(relationAlg.getPrePNSet(pre));
		}
		//else if pre is channel place, add its pre-event to the set
		else if(pre instanceof ChannelPlace){
			causalSet.addAll(net.getPreset(pre));
		}
		//else if marking contains pre and pre is not initial state, add its PN pre-event to the set
		else if(marking.contains(pre) && !relationAlg.isInitial(pre)){
			causalSet.addAll(relationAlg.getPrePNSet(pre));
		}
		//else if 'pre' is a bhv condition,
		else if((pre instanceof Condition)
				&& (net.getOutputSONConnectionTypes(pre).contains(Semantics.BHVLINE))
				&& (!net.getInputSONConnectionTypes(pre).contains(Semantics.BHVLINE))){
			//get corresponding abstract conditions
			Collection<Condition> absConditions = bsonAlg.getAbstractConditions(pre);
			//for each phase of abstract conditions,
			for(Condition c : absConditions){
				Phase phase = phases.get(c);
				Collection<Condition> max = bsonAlg.getMaximalPhase(phase);
				Collection<Condition> min = bsonAlg.getMinimalPhase(phase);
				//if pre is in min, add abstract pre-event to the abstract set.
				if (min.contains(pre)){
					causalSet.addAll(relationAlg.getPrePNSet(c));
				}
				//if pre is maximal but not minimal phase, add behavioural pre-event to the behavioural set.
				else if(!min.contains(pre) && max.contains(pre))
					causalSet.addAll(relationAlg.getPrePNSet(pre));
			}
		}
		//if 'pre' is an abstract condition, get minimal and maximal phase,
		//if min == max, add abstract pre-event to the set
		//else add pre-behavioural event of which min!=max
		else if(bsonAlg.isAbstractCondition(pre)){
			Phase phase = phases.get(pre);
			Collection<Condition> min = bsonAlg.getMinimalPhase(phase);
			Collection<Condition> max = bsonAlg.getMaximalPhase(phase);
			if(min.containsAll(max) && max.containsAll(min)){
				causalSet.addAll(relationAlg.getPrePNSet(pre));
			}
			else{
				for(Condition cMax : max){
					if(!min.contains(cMax))
						causalSet.addAll(relationAlg.getPrePNSet(cMax));
				}
			}
		}

		List<TransitionNode> result = new ArrayList<TransitionNode>();
		for(Node node : causalSet){
			if(node instanceof TransitionNode)
				result.add((TransitionNode)node);
		}

		return result;
	}


	private Path isInSync(Node node, Collection<Path> sync){
		Path result = new Path();
		for(Path path : sync)
			if(path.contains(node)){
				result = path;
			}
		return result;
	}

	private Collection<Path> getSyncCycles(){
		List<Path> result = new ArrayList<Path>();
		HashSet<Node> nodes = new HashSet<Node>();
		nodes.addAll(net.getTransitionNodes());
		nodes.addAll(net.getChannelPlaces());
		CycleAlgorithm cycleAlg = new CycleAlgorithm();
		CSONCycleAlg alg = new CSONCycleAlg(net);

		List<Node> list = new ArrayList<Node>();
		list.addAll(nodes);

		for(List<Integer> cycleIndex : cycleAlg.getCycles(alg.createGraph(list))){
			if(cycleIndex.size() > 1){
				Path cycle = new Path();
				for(Integer index : cycleIndex){
					cycle.add(list.get(index));
				}
				result.add(cycle);
			}
		}
		return result;
	}
}
