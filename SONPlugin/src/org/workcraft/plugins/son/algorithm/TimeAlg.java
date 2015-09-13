package org.workcraft.plugins.son.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.Before;
import org.workcraft.plugins.son.Interval;
import org.workcraft.plugins.son.Phase;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.plugins.son.connections.SONConnection.Semantics;
import org.workcraft.plugins.son.elements.ChannelPlace;
import org.workcraft.plugins.son.elements.Condition;
import org.workcraft.plugins.son.elements.Event;
import org.workcraft.plugins.son.elements.Time;
import org.workcraft.plugins.son.elements.TransitionNode;
import org.workcraft.plugins.son.exception.InvalidStructureException;

public class TimeAlg extends RelationAlgorithm{

	private SON net;

	public TimeAlg(SON net) {
		super(net);
		this.net = net;
	}

	private ArrayList<String> nodeConsistency (Node node, Interval start, Interval end, Interval dur){
		ArrayList<String> result = new ArrayList<String>();
		int tsl = start.getMin();
		int tsu = start.getMax();
		int tfl = end.getMin();
		int tfu = end.getMax();
		int dl = dur.getMin();
		int du = dur.getMax();

		//Equation 3
		if(!(tsl <= tfl))
			result.add("Node inconsistency: minStart"
					+resultHelper(node)+" > minEnd"+resultHelper(node)+".");
		if(!(tsu <= tfu))
			result.add("Node inconsistency: maxStart"
					+resultHelper(node)+" > maxEnd"+resultHelper(node)+".");
		//Equation 6
		int lowBound = tsl + dl;
		int upBound = tsu + du;
		Interval i = new Interval(lowBound, upBound);
		if(!i.isOverlapping(end))
			result.add("Node inconsistency: start"
					+resultHelper(node)+" + duration"+resultHelper(node)
					+"is not intersected with end"+resultHelper(node)+".");

		//Equation 7
		int lowBound2 = tfl - du;
		int upBound2 = tfu -dl;
		Interval i2 = new Interval(lowBound2, upBound2);
		if(!i2.isOverlapping(start))
			result.add("Node inconsistency: end"
					+resultHelper(node)+" - duration"+resultHelper(node)
					+"is not intersected with start"+resultHelper(node)+".");

		//Equation 8
		int lowBound3 = Math.max(0, tfl - tsu);
		int upBound3 = tfu -tsl;
		Interval i3 = new Interval(lowBound3, upBound3);
		if(!i3.isOverlapping(dur))
			result.add("Node inconsistency: end"
					+resultHelper(node)+" - start"+resultHelper(node)
					+"is not intersected with duration"+resultHelper(node)+".");
		return result;
	}

	private ArrayList<String> concurConsistency (TransitionNode t){
		ArrayList<String> result = new ArrayList<String>();

		Collection<SONConnection> inputConnections =  getInputPNConnections(t);;
		Collection<SONConnection> outputConnections =  getOutputPNConnections(t);;

		if(inputConnections.size() > 1){
			SONConnection con = inputConnections.iterator().next();
			Interval time = con.getTime();
			for(SONConnection con1 : inputConnections){
				Interval time1 = con1.getTime();
				if(!time.equals(time1)){
					result.add("Concurrently inconsistency: start"
							+resultHelper(t)+" != start'"+resultHelper(t)+".");
				}
			}
		}

		if(outputConnections.size() > 1){
			SONConnection con = outputConnections.iterator().next();
			Interval time = con.getTime();
			for(SONConnection con1 : outputConnections){
				Interval time1 = con1.getTime();
				if(!time.equals(time1)){
					result.add("Concurrently inconsistency: end"
							+resultHelper(t)+" != end'"+resultHelper(t)+".");
				}
			}
		}
		return result;
	}

	private ArrayList<String> alterConsistency(Condition c){
		ArrayList<String> result = new ArrayList<String>();

		Collection<SONConnection> inputConnections = getInputPNConnections(c);
		Collection<SONConnection> outputConnections = getOutputPNConnections(c);

		if(inputConnections.size() > 1|| outputConnections.size() > 1){
			boolean isConsisent = false;
			for(SONConnection con : inputConnections){
				for(SONConnection con2 : outputConnections){
					if(nodeConsistency(c, con.getTime(), con2.getTime(), c.getDuration()).isEmpty()){
						c.setStartTime(con.getTime());
						c.setEndTime(con2.getTime());
						isConsisent = true;
						break;
					}
				}
			}
			if(!isConsisent){
				result.add("Alternatively inconsistency: cannot find node consistent scenario" +
						"for node "+resultHelper(c)+".");
			}
		}

		return result;
	}

	private ArrayList<String> asynConsistency(ChannelPlace cp, Collection<ChannelPlace> sync) throws InvalidStructureException{
		ArrayList<String> result = new ArrayList<String>();
		//check all transitionNodes first
		Interval start = null;
		TransitionNode input = null;
		if(net.getInputSONConnections(cp).size() == 1){
			SONConnection con = net.getInputSONConnections(cp).iterator().next();
			start = con.getTime();
			input = (TransitionNode)con.getFirst();
		}

		Interval end = null;
		TransitionNode output = null;
		if(net.getOutputSONConnections(cp).size() == 1){
			SONConnection con = net.getOutputSONConnections(cp).iterator().next();
			end = con.getTime();
			output = (TransitionNode)con.getSecond();
		}

		if(start==null || end==null ||input==null || output == null){
			throw new InvalidStructureException("Empty channel place input/output: "+ net.getNodeReference(cp));
		}

		cp.setStartTime(start);
		cp.setEndTime(end);

		if(sync.contains(cp)){
			//Equation 17
			if(!start.equals(input.getEndTime())){
				result.add("Sync inconsistency: start"+resultHelper(cp)
						+"!= end"+ resultHelper(input)+".");
			}
			//Equation 17
			else if(!end.equals(output.getStartTime())){
				result.add("Sync inconsistency: end"+resultHelper(cp)
						+"!= start"+ resultHelper(output)+".");
			}
			//Equation 18
			else if(!(input.getStartTime().equals(output.getStartTime())))
				result.add("Sync inconsistency: start"+resultHelper(input)
						+" != start"+resultHelper(output));
			else if(!(input.getDuration().equals(output.getDuration()))){
				result.add("Sync inconsistency: duration"+resultHelper(input)
						+" != duration"+resultHelper(output));
			}
			else if(!(input.getEndTime().equals(output.getEndTime()))){
				result.add("Sync inconsistency: end"+resultHelper(input)
						+" != end"+resultHelper(output));
			}
			//Equation 19
			if(!nodeConsistency(cp, start, end, cp.getDuration()).isEmpty())
				result.add("Sync inconsistency: "+resultHelper(cp)
						+"is not node consistency");

		}else{
			if(!nodeConsistency(cp, start, end, cp.getDuration()).isEmpty())
				result.add("Async inconsistency: "+resultHelper(cp)
						+"is not node consistency");
		}
		return result;
	}

	public ArrayList<String> behaviouralConsistency(TransitionNode t, Map<Condition, Collection<Phase>> phases){
		ArrayList<String> result = new ArrayList<String>();
		BSONAlg bsonAlg = new BSONAlg(net);
		Before before = bsonAlg.before(t, phases);
		for(TransitionNode[] v : before){
        	TransitionNode v0 = v[0];
        	TransitionNode v1 = v[1];
			//Equation 17
			if(!concurConsistency(v0).isEmpty()){
				result.add("Behavioural inconsistency: "+resultHelper(v0)
						+"is not concurrently consistency.");
				continue;
			}
			//Equation 17
			else if(!concurConsistency(v1).isEmpty()){
				result.add("Behavioural inconsistency: "+resultHelper(v0)
						+"is not concurrently consistency.");
				continue;
			}

			int gsl = v0.getStartTime().getMin();
			int gsu = v0.getStartTime().getMax();
			int hsl = v1.getStartTime().getMin();
			int hsu = v1.getStartTime().getMax();

			//Equation 20
			if(!(gsl <= hsl)){
				result.add("Behavioural inconsistency: minStart"+resultHelper(v0)
						+" > "+"minStart"+resultHelper(v1)+".");
			}
			else if(!(gsu <= hsu)){
				result.add("Behavioural inconsistency: maxStart"+resultHelper(v0)
						+" > "+"maxStart"+resultHelper(v1)+".");
			}
		}
		return result;
	}

	public ArrayList<String> behaviouralConsistency2(Condition initialLow){
		ArrayList<String> result = new ArrayList<String>();
		for(SONConnection con : net.getSONConnections()){
			if(con.getSemantics() == Semantics.BHVLINE && con.getFirst() == initialLow){
				Condition c = (Condition)con.getSecond();
				if(initialLow.getStartTime()!=c.getStartTime())
					result.add("Behavioural inconsistency: start"+resultHelper(initialLow)
							+" != "+"start"+resultHelper(c)+".");
			}
		}
		return result;
	}

	public ArrayList<String> behaviouralConsistency3(Condition finalLow){
		ArrayList<String> result = new ArrayList<String>();
		for(SONConnection con : net.getSONConnections()){
			if(con.getSemantics() == Semantics.BHVLINE && con.getFirst() == finalLow){
				Condition c = (Condition)con.getSecond();
				if(finalLow.getStartTime()!=c.getStartTime())
					result.add("Behavioural inconsistency: end"+resultHelper(finalLow)
							+" != "+"end"+resultHelper(c)+".");
			}
		}
		return result;
	}

	private Collection<SONConnection> getInputPNConnections(Node node){
		Collection<SONConnection> result = new ArrayList<SONConnection>();

		for(SONConnection con : net.getInputSONConnections(node)){
			if(con.getSemantics() == Semantics.PNLINE)
				result.add(con);
		}
		return result;
	}

	private Collection<SONConnection> getOutputPNConnections(Node node){
		Collection<SONConnection> result = new ArrayList<SONConnection>();

		for(SONConnection con : net.getOutputSONConnections(node)){
			if(con.getSemantics() == Semantics.PNLINE)
				result.add(con);
		}
		return result;
	}

	public ArrayList<String> specifiedValueChecker(Node node, boolean isSync) throws InvalidStructureException{
		ArrayList<String> result = new ArrayList<String>();

		if ((node instanceof Time) && !((Time)node).getDuration().isSpecified() && !isSync){
			result.add("Fail to run time consistency checking, duration value is required.");
		}

		if(node instanceof TransitionNode){
			for(SONConnection con : getInputPNConnections(node)){
				if(!con.getTime().isSpecified()){
					result.add("Fail to run time consistency checking, node has unspecified start time value.");
					break;
				}
			}

			for(SONConnection con : getOutputPNConnections(node)){
				if(!con.getTime().isSpecified()){
					result.add("Fail to run time consistency checking, node has unspecified end time value.");
					break;
				}
			}
		}else if (node instanceof Condition){
			Condition c = (Condition)node;

			boolean hasSpecifiedInput = false;
			//initial state
			if(getInputPNConnections(c).isEmpty()){
				if(c.getStartTime().isSpecified())
					hasSpecifiedInput = true;
			}else{
				for(SONConnection con : getInputPNConnections(c)){
					if(!con.getTime().isSpecified()){
						hasSpecifiedInput = true;
					}
				}
			}
			boolean hasSpecifiedOutput = false;
			//final state
			if(getOutputPNConnections(c).isEmpty()){
				if(c.getEndTime().isSpecified())
					hasSpecifiedOutput = true;
			}else{
				for(SONConnection con : getOutputPNConnections(c)){
					if(!con.getTime().isSpecified()){
						hasSpecifiedOutput = true;
					}
				}
			}
			if(!hasSpecifiedInput){
				result.add("Fail to run time consistency checking, at least one specified start time is required.");
			}
			if(!hasSpecifiedOutput){
				result.add("Fail to run time consistency checking, at least one specified end time is required.");
			}
		//check all transitionNodes first!
		}else if (node instanceof ChannelPlace){
			ChannelPlace cp = (ChannelPlace)node;
			Interval start = null;
			TransitionNode input = null;
			if(net.getInputSONConnections(cp).size() == 1){
				SONConnection con = net.getInputSONConnections(cp).iterator().next();
				start = con.getTime();
				input = (TransitionNode)con.getFirst();
			}

			Interval end = null;
			TransitionNode output = null;
			if(net.getOutputSONConnections(cp).size() == 1){
				SONConnection con = net.getOutputSONConnections(cp).iterator().next();
				end = con.getTime();
				output = (TransitionNode)con.getSecond();
			}

			if(start == null || end== null || input==null || output == null){
				throw new InvalidStructureException("Empty channel place input/output: "+ net.getNodeReference(cp));
			}

			if(isSync){
				if(!input.getStartTime().isSpecified() || !input.getDuration().isSpecified() || !input.getEndTime().isSpecified()){
					result.add("Fail to run time consistency checking, input node has unspecified time value.");
				}
				if(!output.getStartTime().isSpecified() || !output.getDuration().isSpecified() || !output.getEndTime().isSpecified()){
					result.add("Fail to run time consistency checking, output node has unspecified time value.");
				}
			}else{
				if(!start.isSpecified() || !end.isSpecified() || !cp.getDuration().isSpecified())
					result.add("Fail to run time consistency checking, (asynchronous) channel place has unspecified time value.");
			}
		}
		return result;
	}

	private String resultHelper(Node node){
		return "("+net.getNodeReference(node)+")";
	}

	public ArrayList<String> TimeConsistecy(Node node, Collection<ChannelPlace> sync) throws InvalidStructureException{
		ArrayList<String> result = new ArrayList<String>();

		//check for unspecified value.
		if(!(node instanceof ChannelPlace)){
			result.addAll(specifiedValueChecker(node, false));
			if(!result.isEmpty())
				return result;
		}

		//ON time consistency checking.
		if(node instanceof TransitionNode){
			TransitionNode t = (TransitionNode)node;
			ArrayList<String> concurResult = concurConsistency(t);
			ArrayList<String> nodeResult = null;

			if(concurResult.isEmpty()){
				if(net.getInputSONConnections(t).size() > 0){
					SONConnection con = net.getInputSONConnections(t).iterator().next();
					t.setStartTime(con.getTime());
				}

				if(net.getOutputSONConnections(t).size() > 0){
					SONConnection con = net.getOutputSONConnections(t).iterator().next();
					t.setEndTime(con.getTime());
				}
				if(t.getStartTime()!=null && t.getEndTime()!=null){
					nodeResult = nodeConsistency(t, t.getStartTime(), t.getEndTime(), t.getDuration());
					result.addAll(nodeResult);
				}else
					throw new InvalidStructureException("Empty event input/output: "+ net.getNodeReference(t));
			}else{
				result.addAll(concurResult);
			}
		//ON time consistency checking
		}else if(node instanceof Condition){
			result.addAll(alterConsistency((Condition)node));
		//CSON time consistency checking
		}else if(node instanceof ChannelPlace){
			ChannelPlace cp = (ChannelPlace)node;

			if(sync.contains(cp)){
				result.addAll(specifiedValueChecker(cp, true));
			}else{
				result.addAll(specifiedValueChecker(cp, false));
			}

			if(!result.isEmpty())
				return result;
			result.addAll(asynConsistency(cp, sync));
		}
		return result;
	}
}
