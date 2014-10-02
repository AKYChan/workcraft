package org.workcraft.plugins.son.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.connections.SONConnection.Semantics;

public class CSONPathAlg extends ONPathAlg{

	private SON net;

	public CSONPathAlg(SON net) {
		super(net);
		this.net = net;
	}

	@Override
	public List<Node[]> createAdj(Collection<Node> nodes){

		List<Node[]> result = new ArrayList<Node[]>();

		for (Node n: nodes){
			for (Node next: net.getPostset(n))
				if(nodes.contains(next)){
					Node[] adjoin = new Node[2];
					adjoin[0] = n;
					adjoin[1] = next;
					if(!result.contains(adjoin))
						result.add(adjoin);

					if(net.getSONConnectionType(n, next) == Semantics.SYNCLINE){
						Node[] reAdjoin = new Node[2];
						reAdjoin[0] = next;
						reAdjoin[1] = n;
						if(!result.contains(reAdjoin))
							result.add(reAdjoin);
				}
			}
		}
		return result;
	}

	@Override
	public Collection<Path> cycleTask (Collection<Node> nodes){
		Collection<Path> result = new ArrayList<Path>();
		for(Node start : relationAlg.getInitial(nodes))
			for(Node end : relationAlg.getFinal(nodes))
				result.addAll(PathAlgorithm.getCycles(start, end, createAdj(nodes)));

		 return cyclePathFilter(result);
	}

	private Collection<Path> cyclePathFilter(Collection<Path> paths){
		List<Path> delList = new ArrayList<Path>();
		for (Path path : paths){
			if(!net.getSONConnectionTypes(path).contains(Semantics.PNLINE))
				delList.add(path);
			if(!net.getSONConnectionTypes(path).contains(Semantics.SYNCLINE) && !net.getSONConnectionTypes(path).contains(Semantics.ASYNLINE))
				delList.add(path);
		}
		paths.removeAll(delList);

		return paths;
	}

}
