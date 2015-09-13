package org.workcraft.plugins.son.tools;


import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.workcraft.Tool;
import org.workcraft.dom.Node;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.gui.graph.tools.AbstractTool;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.Phase;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.VisualSON;
import org.workcraft.plugins.son.algorithm.BSONAlg;
import org.workcraft.plugins.son.algorithm.CSONCycleAlg;
import org.workcraft.plugins.son.algorithm.Path;
import org.workcraft.plugins.son.algorithm.PathAlgorithm;
import org.workcraft.plugins.son.algorithm.RelationAlgorithm;
import org.workcraft.plugins.son.algorithm.TimeAlg;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.plugins.son.connections.VisualSONConnection;
import org.workcraft.plugins.son.elements.Block;
import org.workcraft.plugins.son.elements.ChannelPlace;
import org.workcraft.plugins.son.elements.Condition;
import org.workcraft.plugins.son.elements.Event;
import org.workcraft.plugins.son.elements.TransitionNode;
import org.workcraft.plugins.son.exception.InconsistencyTimeException;
import org.workcraft.plugins.son.exception.InvalidStructureException;
import org.workcraft.util.GUI;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class TestTool extends AbstractTool implements Tool{

	private String message = "";

	public boolean isApplicableTo(WorkspaceEntry we) {
		return WorkspaceUtils.canHas(we, SON.class);

	}

	public String getSection(){
		return "test";
	}

	public String getDisplayName(){
		return "Test";
	}

	GraphEditor editor1;

	public void run(WorkspaceEntry we){
		System.out.println("================================================================================");
		SON net=(SON)we.getModelEntry().getMathModel();
		VisualSON vnet = (VisualSON)we.getModelEntry().getVisualModel();
		timeTest(net);
		//getScenario(net);

	//	dfsTest(net);
		//outputBefore(net);
		//phaseTest(net);
		//csonCycleTest(net);
		//abtreactConditionTest(net);
		//GUI.drawEditorMessage(editor, g, Color.red, "sfasdfadsfa");
		//syncCycleTest(net);
		//blockMathLevelTest(net, vnet);
		//mathLevelTest(net, vnet);
		//connectionTypeTest(net, vnet);
		//this.convertBlockTest(net, vnet);
		//relation(net, vnet);
		//conditionOutputTest(vnet);
	}

	private void timeTest(SON net){
		TimeAlg timeAlg = new TimeAlg(net);

		for(Node node : net.getComponents()){
			System.out.println(net.getNodeReference(node));
			try {
				for(String str : timeAlg.TimeConsistecy(node, getSyncCPs(net))){
					System.out.println(str);
				}
			} catch (InvalidStructureException e) {
				System.out.println("Structure error");
			}
		}
	}

	protected Collection<ChannelPlace> getSyncCPs(SON net){
		Collection<ChannelPlace> result = new HashSet<ChannelPlace>();
		HashSet<Node> nodes = new HashSet<Node>();
		nodes.addAll(net.getTransitionNodes());
		nodes.addAll(net.getChannelPlaces());
		CSONCycleAlg cycleAlg = new CSONCycleAlg(net);

		for(Path path : cycleAlg.syncCycleTask(nodes)){
			for(Node node : path){
				if(node instanceof ChannelPlace)
					result.add((ChannelPlace)node);
			}
		}
		return result;
	}

	private void getScenario(SON net){
		ScenarioGeneratorTool s = new ScenarioGeneratorTool();
		System.out.println(s.getStepExecution().toString());
		System.out.println(s.getStepExecution().size());
	}

	private void dfsTest(SON net){
		PathAlgorithm alg = new PathAlgorithm(net);
		RelationAlgorithm alg2 = new RelationAlgorithm(net);
		Collection<Path> result = alg.dfs3(alg2.getInitial(net.getGroups().iterator().next().getComponents()).iterator().next(),
				alg2.getFinal(net.getGroups().iterator().next().getComponents()).iterator().next(),
				net.getGroups().iterator().next().getComponents());

		for(Path path : result){
			System.out.println(path.toString(net));
		}
	}

	private void outputBefore(SON net){

		BSONAlg bsonAlg = new BSONAlg(net);
		System.out.println("\nOutput before(e):");
		Collection<TransitionNode[]> before = new ArrayList<TransitionNode[]>();

		Collection<ONGroup> groups = bsonAlg.getUpperGroups(net.getGroups());
		Collection<TransitionNode> set = new HashSet<TransitionNode>();
		for(ONGroup group : groups){
			set.addAll(group.getTransitionNodes());
		}


		for(TransitionNode e : set){
			//before =  bsonAlg.before(e);
			if(!before.isEmpty()){
				Collection<String> subResult = new ArrayList<String>();
				System.out.println("before("+ net.getComponentLabel(e)+"): ");
				for(TransitionNode[] t : before)
					subResult.add("("+net.getComponentLabel(t[0]) + " " + net.getComponentLabel(t[1])+ ")");
				System.out.println(subResult);
			}
		}

	}

	@Override
	public void drawInScreenSpace(final GraphEditor editor, Graphics2D g) {
		System.out.println("editor1111111");
		int a =0;
		if(a == 0)
			GUI.drawEditorMessage(editor, g, Color.BLACK, "afdasfasd");
	}

	private void relation(SON net, VisualSON vnet){
		for(Node node : net.getComponents()){
			System.out.println("node name: "+net.getName(node) + "  node pre size:" + net.getPreset(node).size()
					+ "  node post size:" + net.getPostset(node).size());
		}
	}

	private void phaseTest(SON net){
		BSONAlg alg = new BSONAlg(net);

		System.out.println("phase test");
		for(ONGroup group : alg.getUpperGroups(net.getGroups())){
			System.out.println("group = " + net.getNodeReference(group));
			for(Condition c : group.getConditions()){
				System.out.println("condition = " + net.getNodeReference(c));
				for(Phase phase : alg.getPhases(c)){
					System.out.println("phase = " + phase.toString(net));
				}
				System.out.println();
			}
		}

	}

	private void syncCycleTest(SON net){
		CSONCycleAlg csonPath = new CSONCycleAlg(net);
		HashSet<Node> nodes = new HashSet<Node>();
		nodes.addAll(net.getChannelPlaces());
		nodes.addAll(net.getTransitionNodes());

		for(Path path : csonPath.syncEventCycleTask(nodes)){
			System.out.println(path.toString(net));
		}
	}

	private void csonCycleTest(SON net){
		CSONCycleAlg csonPath = new CSONCycleAlg(net);


		for(Path path : csonPath.cycleTask(net.getComponents())){
			System.out.println(path.toString(net));
		}
	}

	private void exceptionTest() throws InvalidConnectionException{
		boolean a = true;
		if(a){
			message = "adfa";
			throw new InvalidConnectionException(message);
		}
	}

	private void abtreactConditionTest(SON net){
		BSONAlg alg = new BSONAlg(net);
		for(Node node : net.getComponents()){
			for(Condition c : alg.getUpperConditions(node)){
				System.out.println("abstract condition of   " + net.getNodeReference(node) + "  is  "  + net.getNodeReference(c));
			}
		}
		System.out.println("********************");
	}

/*	private void convertBlockTest(SONModel net, VisualSON vnet){
		for(Node node : net.getSONConnections()){
			System.out.println("before "+net.getName(node)+ " parent "+ node.getParent().toString() + " type = " + ((SONConnection)node).getType());
	}
			vnet.connectToBlocks();
			System.out.println("node size =" + net.getComponents().size());
			for(Node node : net.getSONConnections()){
					System.out.println("after "+net.getName(node)+ " parent "+ node.getParent().toString() + " type = " + ((SONConnection)node).getType());
			}
	}
	*/

	private void blockMathLevelTest(SON net, VisualSON vnet){
		for(Block block : net.getBlocks()){
			System.out.println("block name :" + net.getName(block));
			System.out.println("connection size : " + block.getSONConnections().size());
		}

/*		for(VisualBlock block : vnet.getVisualBlocks()){
			System.out.println("visual block name :" + vnet.getName(block));
			System.out.
			println("visual connection size : " + block.getVisualSONConnections().size());
		}*/

	}

	private void mathLevelTest(SON net, VisualSON vnet){
		for(ONGroup group: net.getGroups()){
			System.out.println(group.toString());
			System.out.println("Page size = " + group.getPageNodes().size());
			System.out.println("block size = " + group.getBlocks().size());
			System.out.println("Condition size = " + group.getConditions().size());
			System.out.println("Event size = " + group.getEvents().size());
			System.out.println("Connection size = " + group.getSONConnections().size());
			System.out.println();
		}

/*		for(PageNode page : net.getPageNodes()){
			System.out.println("page parent  "+ page.getParent().toString());
		}
		*/
/*		for(VisualONGroup vgroup: vnet.getVisualONGroups()){
			System.out.println(vgroup.toString());
			System.out.println("Visual Page size = " + vgroup.getVisualPages().size());
			System.out.println("Visual Condition size = " + vgroup.getVisualConditions().size());
			System.out.println("Visual Connection size = " + vgroup.getVisualSONConnections().size());
			System.out.println("Visual block size = " + vgroup.getVisualBlocks().size());

		}*/

/*		for(VisualPage page : vnet.getVisualPages()){
			System.out.println();
			System.out.println("visual page parent  "+ page.getParent().toString());
		}*/
	}

	private void connectionTypeTest(SON net, VisualSON vnet){
		for(SONConnection con : net.getSONConnections()){
			System.out.println("con type "+ con.getSemantics());
			System.out.println("con fisrt "+ con.getFirst());
			System.out.println("con fisrt "+ con.getSecond());
		}
		for(VisualSONConnection con : vnet.getVisualSONConnections()){
			System.out.println("con type "+ con.getSemantics());
			System.out.println("con fisrt "+ con.getFirst());
			System.out.println("con fisrt "+ con.getSecond());
		}
	}


	@Override
	public Decorator getDecorator(GraphEditor editor) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}
}

