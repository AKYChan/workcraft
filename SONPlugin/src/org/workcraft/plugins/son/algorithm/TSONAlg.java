package org.workcraft.plugins.son.algorithm;

import java.util.Collection;
import java.util.HashSet;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.Block;
import org.workcraft.plugins.son.SONModel;
import org.workcraft.plugins.son.elements.Event;

public class TSONAlg extends RelationAlgorithm{

	private SONModel net;

	public TSONAlg(SONModel net) {
		super(net);
		this.net = net;
	}

	/**
	 * get all inputs of a given block without concerning connection types
	 */
	public Collection<Node> getBlockInputs(Block block){
		Collection<Node> result = new HashSet<Node>();
		Collection<Node> components = block.getComponents();

		if(!block.getIsCollapsed())
			for(Node node:components){
				for(Node pre : net.getPreset(node)){
					if(!components.contains(pre))
						result.add(pre);
				}
			}
		else
			result.addAll(net.getPreset(block));
		return result;
	}

	/**
	 * get all outputs of a given block without concerning connection types
	 */
	public Collection<Node> getBlockOutputs(Block block){
		Collection<Node> result = new HashSet<Node>();
		Collection<Node> components = block.getComponents();

		if(!block.getIsCollapsed())
			for(Node node: components){
				for(Node post : net.getPostset(node)){
					if(!components.contains(post))
						result.add(post);
				}
			}
		else
			result.addAll(net.getPostset(block));
		return result;
	}

	/**
	 * get petri net-based inputs of a given block
	 */
	public Collection<Node> getBlockPNInputs(Block block){
		Collection<Node> result = new HashSet<Node>();
		Collection<Node> components = block.getComponents();

		if(!block.getIsCollapsed())
			for(Node node:components){
				for(Node pre : net.getPreset(node)){
					if(!components.contains(pre) && net.getSONConnectionType(node, pre) == "POLYLINE")
						result.add(pre);
				}
			}
		else
			for(Node pre : net.getPreset(block))
				if(net.getSONConnectionType(block, pre) == "POLYLINE")
					result.add(pre);

		return result;
	}

	/**
	 * get petri net-based outputs of a given block
	 */
	public Collection<Node> getBlockPNOutputs(Block block){
		Collection<Node> result = new HashSet<Node>();
		Collection<Node> components = block.getComponents();

		if(!block.getIsCollapsed())
			for(Node node:components){
				for(Node post : net.getPostset(node)){
					if(!components.contains(post) && net.getSONConnectionType(node, post) == "POLYLINE")
						result.add(post);
				}
			}
		else
			for(Node post : net.getPostset(block))
				if(net.getSONConnectionType(block, post) == "POLYLINE")
					result.add(post);

		return result;
	}

	/**
	 * get cson-based inputs of a given block
	 */
	public Collection<Node> getBlockASynInputs(Block block){
		Collection<Node> result = new HashSet<Node>();
		Collection<Node> components = block.getComponents();

		for(Node node:components){
			if(node instanceof Event || node instanceof Block)
				for(Node pre : this.getPreASynEvents(node)){
					if(!components.contains(pre) && pre!=null)
						result.add(pre);
				}
		}
		return result;
	}

	/**
	 * get cson-based outputs of a given block
	 */
	public Collection<Node> getBlockASynOutputs(Block block){
		Collection<Node> result = new HashSet<Node>();
		Collection<Node> components = block.getComponents();

		for(Node node:components){
			if(node instanceof Event || node instanceof Block)
				for(Node post : this.getPostASynEvents(node)){
					if(!components.contains(post) && post!=null)
						result.add(post);
				}
		}
		return result;
	}

	/**
	 * check if a given input causally precede all outputs
	 */
	public boolean isCausallyPrecede (Node input, Collection<Node> outputs){
		for(Node post : net.getPostset(input)){
			if(outputs.contains(post))
				outputs.remove(post);
			else
				isCausallyPrecede(post, outputs);
		}
		if(outputs.isEmpty())
			return true;
		else
			return false;
	}

}
