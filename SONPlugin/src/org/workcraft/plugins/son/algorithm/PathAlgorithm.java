package org.workcraft.plugins.son.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.SON;

public class PathAlgorithm{

	private SON net;
	private static Collection<Path> pathResult =new ArrayList<Path>();

	public PathAlgorithm(SON net) {
		this.net = net;
	}

    private void bfs(Collection<Node> nodes , LinkedList<Node> visited, Node v) {
        LinkedList<Node> post = getPostset(visited.getLast(), nodes);
        // examine post nodes
        for (Node node : post) {
            if (visited.contains(node)) {
                continue;
            }
            if (node.equals(v)) {
                visited.add(node);
                Path path = new Path();
                path.addAll(visited);
                pathResult.add(path);
                visited.removeLast();
                break;
            }
        }
        // in breadth-first, recursion needs to come after visiting post nodes
        for (Node node : post) {
            if (visited.contains(node) || node.equals(v)) {
                continue;
            }
            visited.addLast(node);
            bfs(nodes, visited, v);
            visited.removeLast();
        }
    }

    public Collection<Path> getPaths (Node s, Node v, Collection<Node> nodes){
    	pathResult.clear();
    	LinkedList<Node> visited = new LinkedList<Node>();
    	visited.add(s);
    	bfs(nodes, visited, v);
    	return pathResult;
    }

    private LinkedList<Node> getPostset(Node n, Collection<Node> nodes){
    	LinkedList<Node> list = new LinkedList<Node>();
    	for(Node post : net.getPostset(n))
    		if(nodes.contains(post))
    			list.add(post);
    	return list;
    }

	public static Collection<Node> dfs (Collection<Node> s, Collection<Node> v, SON net){
		Collection<Node> result = new HashSet<Node>();
		RelationAlgorithm relation = new RelationAlgorithm(net);
        Stack<Node> stack = new Stack<Node>();

		for(Node s1 : s){
			Collection<Node> visit = new ArrayList<Node>();
			stack.push(s1);
			visit.add(s1);

            while(!stack.empty()){
        		s1 = stack.peek();

            	if(v.contains(s1)){
            		result.add(s1);
            	}

            	Node post = null;
    			for (Node n: relation.getPostPNSet(s1)){
    				if(result.contains(n)){
    					result.add(s1);
    				}
    				if(!visit.contains(n)){
    					post = n;
    					break;
    				}
    			}

    			if(post != null){
    				visit.add(post);
    				stack.push(post);
    			}else{
    				stack.pop();
    			}
            }
		}
		return result;
	}
}

