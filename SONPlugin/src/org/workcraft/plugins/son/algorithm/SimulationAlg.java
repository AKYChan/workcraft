package org.workcraft.plugins.son.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.SONModel;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.plugins.son.elements.ChannelPlace;
import org.workcraft.plugins.son.elements.Condition;
import org.workcraft.plugins.son.elements.EventNode;

public class SimulationAlg extends RelationAlgorithm {

	private SONModel net;
	private BSONAlg bsonAlg;

	private Collection<EventNode> syncEventSet = new HashSet<EventNode>();
	private Collection<Node> checkedEvents = new HashSet<Node>();

	private Collection<Node> history;
	private List<ArrayList<Node>> syncCycles;
	private Collection<ArrayList<Node>> cycleResult;

	private Collection<Node> minimalExeEvents = new HashSet<Node>();
	private Collection<Node> minimalReverseExeEvents = new HashSet<Node>();
	private Collection<Node> postEventSet = new HashSet<Node>();
	private Collection<Node> preEventSet = new HashSet<Node>();

	private Collection<ONGroup> abstractGroups;
	private Collection<ONGroup> bhvGroups;

	public SimulationAlg(SONModel net){
		super(net);
		this.net = net;
		history = new ArrayList<Node>();
		syncCycles= new ArrayList<ArrayList<Node>>();
		cycleResult = new HashSet<ArrayList<Node>>();
		bsonAlg = new BSONAlg(net);

		abstractGroups = bsonAlg.getAbstractGroups(net.getGroups());
		bhvGroups = bsonAlg.getBhvGroups(net.getGroups());
	}


	private void getMinimalExeEventSet (EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){

		HashSet<Node> syncEvents = new HashSet<Node>();

		for(ArrayList<Node> cycle : sync){
			if(cycle.contains(e))
				syncEvents.addAll(cycle);
		}

		if(!syncEvents.isEmpty()){
			for(Node n : syncEvents){
				if(enabledEvents.contains(n) && !minimalExeEvents.contains(n)){
					minimalExeEvents.add(n);

					for(EventNode pre : this.getPreAsynEvents((EventNode)n)){
						if(!syncEvents.contains(pre) && enabledEvents.contains(pre))
							getMinimalExeEventSet((EventNode)n, sync, enabledEvents);
					}

				}
				if(!enabledEvents.contains(n))
					throw new RuntimeException("algorithm error: has unenabled event in sync cycle  "+net.getName(n));
			}
		}

		if(!this.getPreAsynEvents(e).isEmpty() && enabledEvents.contains(e)){
			if(!minimalExeEvents.contains(e))
				minimalExeEvents.add(e);

			for(EventNode n : this.getPreAsynEvents(e))
				if(!minimalExeEvents.contains(n) && enabledEvents.contains(n)){
					minimalExeEvents.add(n);
					getMinimalExeEventSet((EventNode)n, sync, enabledEvents);
				}
		}
		else
			minimalExeEvents.add(e);
	}

	/**
	 * return minimal execution set of a given node.
	 * This may contain other nodes which have synchronous with the target node.
	 */
	public List<EventNode> getMinimalExeResult (EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){
		List<EventNode> result = new ArrayList<EventNode>();

		getMinimalExeEventSet(e, sync, enabledEvents);

		for(Node n : this.minimalExeEvents)
			if(n instanceof EventNode)
				result.add((EventNode)n);;

		return result;
	}

	private void getSynEventSet(EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){

		Collection<Node> syncEvents = new HashSet<Node>();

		for(ArrayList<Node> cycle : sync){
			if(cycle.contains(e))
				syncEvents.addAll(cycle);
		}

		if(!syncEvents.isEmpty()){
			for(Node n : syncEvents){
				if(enabledEvents.contains(n) && !postEventSet.contains(n)){
					postEventSet.add(n);

					for(EventNode post : this.getPostAsynEvents((EventNode)n)){
						if(!syncEvents.contains(post) && enabledEvents.contains(post))
							getSynEventSet((EventNode)n, sync, enabledEvents);
					}

				}
				if(!enabledEvents.contains(n))
					throw new RuntimeException("algorithm error: has unenable event in sync cycle");
			}
		}

		if(!this.getPostAsynEvents(e).isEmpty() && enabledEvents.contains(e)){
			if(!postEventSet.contains(e))
				postEventSet.add(e);

			for(EventNode n : this.getPostAsynEvents(e))
				if(!postEventSet.contains(n) && enabledEvents.contains(n)){
					postEventSet.add(n);
					getSynEventSet((EventNode)n, sync, enabledEvents);
				}
		}
		else
			postEventSet.add(e);
	}

	public List<EventNode> getPostExeResult (EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){
		List<EventNode> result = new ArrayList<EventNode>();
		getSynEventSet(e, sync, enabledEvents);

		for(Node n : this.postEventSet)
			if(n instanceof EventNode)
				result.add((EventNode)n);

		return result;
	}

	/**
	 * clear all set.
	 */
	public void clearAll(){
			syncEventSet.clear();
			checkedEvents.clear();

			history.clear();
			syncCycles.clear();
			cycleResult.clear();

			minimalExeEvents.clear();
			minimalReverseExeEvents.clear();
			postEventSet.clear();
			preEventSet.clear();
	}

	/**
	 * create a adjacency matrix.
	 */
	public List<Node[]> createAdj(Collection<Node> nodes){

		List<Node[]> result = new ArrayList<Node[]>();

		for (Node n: nodes){
			for (Node next: net.getPostset(n)){
				if(next instanceof ChannelPlace && net.getSONConnectionType(next, n) == "ASYNLINE"){
					Node[] adjoin = new Node[2];
					for(Node n2 : net.getPostset(next))
						if(n2 instanceof EventNode){
							adjoin[0] = n;
							adjoin[1] = n2;
							result.add(adjoin);
						}
				}

				if(next instanceof ChannelPlace && net.getSONConnectionType(next, n) =="SYNCLINE"){
					Node[] adjoin = new Node[2];
					Node[] reAdjoin = new Node[2];
					for(Node n2 : net.getPostset(next))
						if(n2 instanceof EventNode){
							adjoin[0] = n;
							adjoin[1] = n2;
							reAdjoin[0] = n2;
							reAdjoin[1] = n;
							result.add(adjoin);
							result.add(reAdjoin);
						}
				}

				if(next instanceof EventNode || next instanceof Condition){
					Node[] adjoin = new Node[2];
					adjoin[0] = n;
					adjoin[1] = next;
					result.add(adjoin);
					}
				}
		}
		return result;
	}

	/**
	 * get all paths between two given nodes. They may contain cyclic path.
	 */
	public void getAllPath(Node start, Node end, List<Node[]> adj){

		history.add(start);

		for (int i=0; i< adj.size(); i++){
			if (((Node)adj.get(i)[0]).equals(start)){
				if(((Node)adj.get(i)[1]).equals(end)){
					continue;
				}
				if(!history.contains((Node)adj.get(i)[1])){
					getAllPath((Node)adj.get(i)[1], end, adj);
				}
				else {
					ArrayList<Node> cycle=new ArrayList<Node>();

						cycle.addAll(history);
						int n=cycle.indexOf(((Node)adj.get(i)[1]));
						for (int m = 0; m < n; m++ ){
							cycle.remove(0);
						}
						cycleResult.add(cycle);
				}
			}
		}
		history.remove(start);
	}

	/**
	 * get synchronous cycle for a set of node.
	 */
	public Collection<ArrayList<Node>> getSyncCycles(Collection<Node> nodes){

		List<ArrayList<Node>> subResult = new ArrayList<ArrayList<Node>>();
		Collection<ArrayList<Node>> result = new ArrayList<ArrayList<Node>>();

		this.clearAll();

		for(Node start : getInitial(nodes))
			for(Node end : getFinal(nodes))
				getAllPath(start, end, createAdj(nodes));

		if(!cycleResult.isEmpty()){
			for(ArrayList<Node> cycle : cycleResult){
				boolean hasCondition = false;
				for(Node n : cycle)
					if(n instanceof Condition)
						hasCondition = true;
				if(!hasCondition)
					subResult.add(cycle);
			}

			getLongestCycle(subResult);

			for(ArrayList<Node> list : getLongestCycle(subResult)){
				HashSet<Node> filter = new HashSet<Node>();
				filter.addAll(list);
				ArrayList<Node> cycle = new ArrayList<Node>();
				cycle.addAll(filter);
				result.add(cycle);
			}
		}
		return result;
	}

	private List<ArrayList<Node>> getLongestCycle(List<ArrayList<Node>> cycles){

		if(syncCycles.isEmpty())
			syncCycles.add(cycles.get(0));

			boolean hasMerged = false;
			int i = syncCycles.size()-1;
			Collection<Node> merge = new HashSet<Node>();
			ArrayList<Node> cycle = new ArrayList<Node>();


		for(int j=0; j < cycles.size(); j++){
				if(!syncCycles.get(i).containsAll(cycles.get(j)) && this.hasCommonElements(syncCycles.get(i), cycles.get(j))){
					hasMerged = true;
					merge.addAll(syncCycles.get(i));
					merge.addAll(cycles.get(j));
					syncCycles.remove(i);
					cycle.addAll(merge);
					syncCycles.add(cycle);
					}
			}

			if(hasMerged)
				getLongestCycle(cycles);
			else{
				for(int m=0; m<cycles.size(); m++){
					boolean b = true;
					for(int n=0; n<syncCycles.size();n++){
						if(syncCycles.get(n).containsAll(cycles.get(m)))
							b = false;
					}
					if(b){
						syncCycles.add(cycles.get(m));
						getLongestCycle(cycles);
					}
				}
			}
		return syncCycles;
	}

	private boolean hasCommonElements(Collection<Node> cycle1, Collection<Node> cycle2){
		for(Node n : cycle1)
			if(cycle2.contains(n))
				return true;
		for(Node n : cycle2)
			if(cycle1.contains(n))
				return true;
		return false;
	}


	private boolean isPNEnabled (EventNode e) {
		// gather number of connections for each pre-place
		for (Node n : net.getPreset(e)){
			if(n instanceof Condition)
				if (!((Condition)n).isMarked())
					return false;
			}
		if(net.getPreset(e).isEmpty())
			return false;

		return true;
	}

	private boolean isSyncEnabled(EventNode e, Collection<ArrayList<Node>> sync, Map<Condition, Collection<Condition>> phases){
		HashSet<Node> syncEvents = new HashSet<Node>();

		for(ArrayList<Node> cycle : sync){
			if(cycle.contains(e))
				syncEvents.addAll(cycle);
		}

		if(syncEvents.contains(e)){
			checkedEvents.addAll(syncEvents);
			for(Node n : syncEvents)
				if(n instanceof EventNode){
					if(!this.isPNEnabled((EventNode)n) || !this.isBhvEnabled((EventNode)n, phases))
							return false;
					for(EventNode pre : this.getPreAsynEvents((EventNode)n)){
						if(!syncEvents.contains(pre)){
							if(!this.isAsynEnabled((EventNode)n, sync, phases) || !this.isBhvEnabled((EventNode)n, phases))
								return false;
						}
					}
				}
			}

		return true;
	}

	private boolean isAsynEnabled(EventNode e, Collection<ArrayList<Node>> sync, Map<Condition, Collection<Condition>> phases){

		for (Node n : net.getPreset(e)){
			if(n instanceof ChannelPlace)
				if (((ChannelPlace)n).hasToken() == false)
					for(Node node : net.getPreset(n)){
						if(node instanceof EventNode && !checkedEvents.contains(node)){
							if(!this.isPNEnabled((EventNode)node) ||!this.isSyncEnabled((EventNode)node, sync, phases)
									||!this.isAsynEnabled((EventNode)node, sync, phases) ||!this.isBhvEnabled((EventNode)node, phases))
								return false;
						}
				}
		}
		return true;
	}

	private boolean isBhvEnabled(EventNode e, Map<Condition, Collection<Condition>> phases){
		for(ONGroup group : abstractGroups){
			if(group.getComponents().contains(e)){
				for(Node pre : getPrePNSet(e))
					if(pre instanceof Condition){
						Collection<Condition> phase = phases.get((Condition)pre);
						for(Condition max : bsonAlg.getMaximalPhase(phase))
							if(!max.isMarked())
								return false;
				}
			return true;
			}
		}

		for(ONGroup group : bhvGroups){
			if(group.getComponents().contains(e)){
				for(Condition c : phases.keySet()){
					if(c.isMarked())
						if((!phases.get(c).containsAll(getPrePNSet(e)) && phases.get(c).containsAll(getPostPNSet(e)))||
								(!phases.get(c).containsAll(getPostPNSet(e)) && phases.get(c).containsAll(getPrePNSet(e))))
							return false;
					if(!c.isMarked())
						if(phases.get(c).containsAll(getPostPNSet(e)) && phases.get(c).containsAll(getPrePNSet(e)))
							return false;
					}
				}
			}
		return true;
	}

	final public boolean isEnabled (EventNode e, Collection<ArrayList<Node>> sync, Map<Condition, Collection<Condition>> phases){
		checkedEvents.clear();
		if(isPNEnabled(e) && isSyncEnabled(e, sync, phases) && this.isAsynEnabled(e, sync, phases) && isBhvEnabled(e, phases)){
			return true;
		}
		return false;
	}

	public void fire(Collection<EventNode> events){
		for(EventNode e : events){
			for (SONConnection c : net.getSONConnections(e)) {
				if (c.getType() == "POLYLINE" && e==c.getFirst()) {
					Condition to = (Condition)c.getSecond();
					if(to.isMarked())
						throw new RuntimeException("Token setting error: the number of token in "+net.getName(to) + " > 1");
					to.setMarked(true);
				}
				if (c.getType() == "POLYLINE" && e==c.getSecond()) {
					Condition from = (Condition)c.getFirst();
					from.setMarked(false);

				}
				if (c.getType() == "ASYNLINE" && e==c.getFirst()){
						ChannelPlace to = (ChannelPlace)c.getSecond();
						if(events.containsAll(net.getPostset(to)) && events.containsAll(net.getPreset(to)))
							to.setToken(((ChannelPlace)to).hasToken());
						else{
							if(to.hasToken())
								throw new RuntimeException("Token setting error: the number of token in "+net.getName(to) + " > 1");
							to.setToken(true);
						}
				}
				if (c.getType() == "ASYNLINE" && e==c.getSecond()){
						ChannelPlace from = (ChannelPlace)c.getFirst();
						if(events.containsAll(net.getPostset(from)) && events.containsAll(net.getPreset(from)))
							from.setToken(((ChannelPlace)from).hasToken());
						else
							from.setToken(!((ChannelPlace)from).hasToken());
				}
			}

		for(ONGroup group : abstractGroups){
			if(group.getEvents().contains(e)){
				Collection<Condition> preMax = new HashSet<Condition>();
				Collection<Condition> postMin = new HashSet<Condition>();
				for(Node pre : getPrePNSet(e))
					preMax.addAll( bsonAlg.getMaximalPhase(bsonAlg.getPhase((Condition)pre)));
				for(Node post : getPostPNSet(e))
					postMin.addAll(bsonAlg.getMinimalPhase(bsonAlg.getPhase((Condition)post)));

				if(!preMax.containsAll(postMin)){
					boolean isFinal=true;
					for(Condition fin : preMax)
							if(!isFinal(fin))
								isFinal=false;
					if(isFinal){
						for(Condition fin : preMax){
							//structure such that condition fin has more than one high-level states
							int tokens = 0;
							for(Node post : net.getPostset(fin)){
								if(post instanceof Condition && net.getSONConnectionType(post, fin) == "BHVLINE")
									if(((Condition)post).isMarked())
										tokens++;
							}
							//if preMax has token and there is no high-level states has token, then token -> false;
							if(fin.isMarked() && tokens == 0)
								fin.setMarked(false);
						}
					}
					boolean isIni = true;
					for(Condition init : postMin)
							if(!isInitial(init))
								isIni=false;
					if(isIni)
						for(Condition ini : postMin){
							//structure such that condition ini has more than one high-level states
							int tokens = 0;
							int size = 0;
							for(Node post : net.getPostset(ini)){
								if(post instanceof Condition && net.getSONConnectionType(post, ini) == "BHVLINE"){
									size++;
									if(((Condition)post).isMarked())
										tokens++;
									}
							}

							if(!ini.isMarked() && tokens == size)
								ini.setMarked(true);
							//	JOptionPane.showMessageDialog(null, "Token setting error: token in "+net.getName(ini) + " is true", "Error", JOptionPane.WARNING_MESSAGE);
						}
					}
				}
			}
		}
	}


	//reverse simulation

	final public boolean isUnfireEnabled (EventNode e, Collection<ArrayList<Node>> sync, Map<Condition, Collection<Condition>> phases) {
		checkedEvents.clear();
		if(isPNUnEnabled(e) && isSyncUnEnabled(e, sync, phases) && this.isAsynUnEnabled(e, sync, phases) && isBhvUnEnabled(e, phases))
			return true;
		return false;
	}


	private boolean isPNUnEnabled (EventNode e) {
		for (Node n : net.getPostset(e)){
			if(n instanceof Condition)
				if (!((Condition)n).isMarked())
					return false;
			}
		if(net.getPostset(e).isEmpty())
			return false;
		return true;
	}

	private boolean isSyncUnEnabled(EventNode e, Collection<ArrayList<Node>> sync, Map<Condition, Collection<Condition>> phases){
		HashSet<Node> syncEvents = new HashSet<Node>();

		for(ArrayList<Node> cycle : sync){
			if(cycle.contains(e))
				syncEvents.addAll(cycle);
		}

		if(syncEvents.contains(e)){
			checkedEvents.addAll(syncEvents);
			for(Node n : syncEvents)
				if(n instanceof EventNode){
					if(!this.isPNUnEnabled((EventNode)n) || !this.isBhvUnEnabled((EventNode)n, phases))
							return false;
					for(Node post : this.getPostAsynEvents((EventNode)n)){
						if(post instanceof EventNode && !syncEvents.contains(post)){
							if(!this.isAsynUnEnabled((EventNode)n, sync, phases)||!this.isBhvUnEnabled((EventNode)n, phases))
								return false;
						}
					}
				}
			}
		return true;
	}

	private boolean isAsynUnEnabled(EventNode e, Collection<ArrayList<Node>> sync, Map<Condition, Collection<Condition>> phases){

		for (Node n : net.getPostset(e)){
			if(n instanceof ChannelPlace)
				if (((ChannelPlace)n).hasToken() == false)
					for(Node node : net.getPostset(n)){
						if(node instanceof EventNode && !checkedEvents.contains(node)){
							if(!this.isPNUnEnabled((EventNode)node) ||!this.isSyncUnEnabled((EventNode)node, sync, phases)
									||!this.isAsynUnEnabled((EventNode)node, sync, phases) ||!this.isBhvUnEnabled((EventNode)node, phases))
								return false;
						}
				}
		}
		return true;
	}

	private boolean isBhvUnEnabled(EventNode e, Map<Condition, Collection<Condition>> phases){
		for(ONGroup group : abstractGroups){
			if(group.getComponents().contains(e)){
				for(Node pre : getPostPNSet(e))
					if(pre instanceof Condition){
						Collection<Condition> phase = bsonAlg.getPhase((Condition)pre);
						for(Condition min : bsonAlg.getMinimalPhase(phase))
							if(!min.isMarked())
								return false;
				}
			return true;
			}
		}

		for(ONGroup group : bhvGroups){
			if(group.getComponents().contains(e)){
				for(Condition c : phases.keySet()){
					if(c.isMarked())
						if((!phases.get(c).containsAll(getPrePNSet(e)) && phases.get(c).containsAll(getPostPNSet(e)))||
								(!phases.get(c).containsAll(getPostPNSet(e)) && phases.get(c).containsAll(getPrePNSet(e))))
							return false;
					if(!c.isMarked())
						if(phases.get(c).containsAll(getPostPNSet(e)) && phases.get(c).containsAll(getPrePNSet(e)))
							return false;
					}
				}
			}
		return true;
	}

	public void unFire(Collection<EventNode> events){
		for(EventNode e : events){
			for (SONConnection c : net.getSONConnections(e)) {
				if (c.getType() == "POLYLINE" && e==c.getSecond()) {
					Condition to = (Condition)c.getFirst();
					to.setMarked(true);
				}
				if (c.getType() == "POLYLINE" && e==c.getFirst()) {
					Condition from = (Condition)c.getSecond();
					from.setMarked(false);
				}
				if (c.getType() == "ASYNLINE" && e==c.getSecond()){
						ChannelPlace to = (ChannelPlace)c.getFirst();
						if(events.containsAll(net.getPreset(to)) && events.containsAll(net.getPostset(to)))
							to.setToken(((ChannelPlace)to).hasToken());
						else
							to.setToken(!((ChannelPlace)to).hasToken());
				}
				if (c.getType() == "ASYNLINE" && e==c.getFirst()){
						ChannelPlace from = (ChannelPlace)c.getSecond();
						if(events.containsAll(net.getPreset(from)) && events.containsAll(net.getPostset(from)))
							from.setToken(((ChannelPlace)from).hasToken());
						else
							from.setToken(!((ChannelPlace)from).hasToken());
				}
			}

			for(ONGroup group : abstractGroups){
				if(group.getEvents().contains(e)){
					Collection<Condition> preMax = new HashSet<Condition>();
					Collection<Condition> postMin = new HashSet<Condition>();
					for(Node pre : getPrePNSet(e))
						preMax.addAll( bsonAlg.getMaximalPhase(bsonAlg.getPhase((Condition)pre)));
					for(Node post : getPostPNSet(e))
						postMin.addAll(bsonAlg.getMinimalPhase(bsonAlg.getPhase((Condition)post)));

					if(!preMax.containsAll(postMin)){
						boolean isInitial=true;
						for(Condition ini : postMin)
								if(!isInitial(ini))
									isInitial=false;
						if(isInitial){
								for(Condition ini : postMin){
									//structure such that condition fin has more than one high-level states
									int tokens = 0;
									for(Node post : net.getPostset(ini)){
										if(post instanceof Condition && net.getSONConnectionType(post, ini) == "BHVLINE")
											if(((Condition)post).isMarked())
												tokens++;
									}
									//if preMax has token and there is no high-level states has token, then token -> false;
									if(ini.isMarked() && tokens == 0)
										ini.setMarked(false);
								}
							}

						boolean isFinal = true;
						for(Condition fin : preMax)
								if(!isFinal(fin))
									isFinal=false;
						if(isFinal)
							for(Condition fin : preMax){
								//structure such that condition ini has more than one high-level states
								int tokens = 0;
								int size = 0;
								for(Node post : net.getPostset(fin)){
									if(post instanceof Condition && net.getSONConnectionType(post, fin)== "BHVLINE"){
										size++;
										if(((Condition)post).isMarked())
											tokens++;
										}
								}

								if(!fin.isMarked() && tokens == size)
									fin.setMarked(true);
								//	JOptionPane.showMessageDialog(null, "Token setting error: token in "+net.getName(ini) + " is true", "Error", JOptionPane.WARNING_MESSAGE);
							}
					}
				}
			}
		}
	}

	private void getMinimalReverseExeEventSet (EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){

		HashSet<Node> syncEvents = new HashSet<Node>();

		for(ArrayList<Node> cycle : sync){
			if(cycle.contains(e))
				syncEvents.addAll(cycle);
		}

		if(!syncEvents.isEmpty()){
			for(Node n : syncEvents){
				if(enabledEvents.contains(n) && !minimalReverseExeEvents.contains(n)){
					minimalReverseExeEvents.add(n);

					for(EventNode pre : this.getPostAsynEvents((EventNode)n)){
						if(!syncEvents.contains(pre) && enabledEvents.contains(pre))
							getMinimalReverseExeEventSet((EventNode)n, sync, enabledEvents);
					}

				}
				if(!enabledEvents.contains(n))
					throw new RuntimeException("algorithm error: has unenabled event in sync cycle  "+net.getName(n));
			}
		}

		if(!this.getPostAsynEvents(e).isEmpty() && enabledEvents.contains(e)){
			if(!minimalReverseExeEvents.contains(e))
				minimalReverseExeEvents.add(e);

			for(EventNode n : this.getPostAsynEvents(e))
				if(!minimalReverseExeEvents.contains(n) && enabledEvents.contains(n)){
					minimalReverseExeEvents.add(n);
					getMinimalReverseExeEventSet((EventNode)n, sync, enabledEvents);
				}
		}
		else
			minimalReverseExeEvents.add(e);
	}

	public List<EventNode> getMinimalReverseExeResult (EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){
		List<EventNode> result = new ArrayList<EventNode>();

		getMinimalReverseExeEventSet(e, sync, enabledEvents);

		for(Node n : this.minimalReverseExeEvents)
			if(n instanceof EventNode)
				result.add((EventNode)n);;

		return result;
	}

	private void getPreEventsSet(EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){

		HashSet<Node> syncEvents = new HashSet<Node>();

		for(ArrayList<Node> cycle : sync){
			if(cycle.contains(e))
				syncEvents.addAll(cycle);
		}

		if(!syncEvents.isEmpty()){
			for(Node n : syncEvents){
				if(enabledEvents.contains(n) && !preEventSet.contains(n)){
					preEventSet.add(n);

					for(EventNode pre : this.getPreAsynEvents((EventNode)n)){
						if(!syncEvents.contains(pre) && enabledEvents.contains(pre))
							getPreEventsSet((EventNode)n, sync, enabledEvents);
					}

				}
				if(!enabledEvents.contains(n))
					throw new RuntimeException("algorithm error: has unenabled event in sync cycle");
			}
		}

		if(!this.getPreAsynEvents(e).isEmpty() && enabledEvents.contains(e)){
			if(!preEventSet.contains(e))
				preEventSet.add(e);

			for(EventNode n : this.getPreAsynEvents(e))
				if(!preEventSet.contains(n) && enabledEvents.contains(n)){
					preEventSet.add(n);
					getPreEventsSet((EventNode)n, sync, enabledEvents);
				}
		}
		else
			preEventSet.add(e);
	}

	public List<EventNode> getPreExeResult (EventNode e, Collection<ArrayList<Node>> sync, Collection<EventNode> enabledEvents){
		List<EventNode> result = new ArrayList<EventNode>();
		getPreEventsSet(e, sync, enabledEvents);

		for(Node n : this.preEventSet)
			if(n instanceof EventNode)
				result.add((EventNode)n);

		return result;
	}

	//others

	public Collection<ONGroup> getAbstractGroups(){
		return this.abstractGroups;
	}
}
