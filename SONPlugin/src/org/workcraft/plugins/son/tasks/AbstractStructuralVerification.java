package org.workcraft.plugins.son.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.workcraft.dom.Node;
import org.workcraft.plugins.son.ONGroup;
import org.workcraft.plugins.son.Phase;
import org.workcraft.plugins.son.SON;
import org.workcraft.plugins.son.algorithm.BSONAlg;
import org.workcraft.plugins.son.algorithm.BSONCycleAlg;
import org.workcraft.plugins.son.algorithm.CSONCycleAlg;
import org.workcraft.plugins.son.algorithm.ONCycleAlg;
import org.workcraft.plugins.son.algorithm.Path;
import org.workcraft.plugins.son.algorithm.RelationAlgorithm;
import org.workcraft.plugins.son.algorithm.TSONAlg;
import org.workcraft.plugins.son.elements.Condition;

abstract class AbstractStructuralVerification implements StructuralVerification{

	private SON net;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RelationAlgorithm relationAlg;
	private CSONCycleAlg csonCycleAlg;
	private BSONAlg bsonAlg;
	private BSONCycleAlg bsonCycleAlg;
	private ONCycleAlg onCycleAlg;
	private TSONAlg tsonAlg;
	private Map<Condition, Collection<Phase>> allPhases;

	public AbstractStructuralVerification(SON net){
		this.net = net;

		relationAlg = new RelationAlgorithm(net);
		csonCycleAlg = new CSONCycleAlg(net);
		bsonAlg = new BSONAlg(net);
		allPhases = bsonAlg.getAllPhases();
		bsonCycleAlg = new BSONCycleAlg(net, allPhases);
		onCycleAlg = new ONCycleAlg(net);
		tsonAlg = new TSONAlg(net);
	}

	public abstract void task(Collection<ONGroup> groups);

	public Collection<String> getRelationErrorsSetRefs(Collection<Node> set){
		Collection<String> result = new ArrayList<String>();
		for(Node node : set)
			result.add(net.getNodeReference(node));
		return result;
	}

	public Collection<String> getGroupErrorsSetRefs(Collection<ONGroup> set){
		Collection<String> result = new ArrayList<String>();
		for(ONGroup node : set)
			result.add(net.getNodeReference(node));
		return result;
	}

	public Collection<ArrayList<String>> getCycleErrorsSetRefs(Collection<Path> set){
		Collection<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		for(Path path : set){
			ArrayList<String> sPath = new ArrayList<String>();
			for(Node node : path){
				sPath.add(net.getNodeReference(node));
				result.add(sPath);
			}
		}
		return result;
	}

	public Map<Condition, Collection<Phase>> getAllPhases(){
		return allPhases;
	}

	public void infoMsg(String msg){
		logger.info(msg);
	}

	public void infoMsg(String msg, Node node){
		logger.info(msg + " [" + net.getNodeReference(node) + "]");
	}

	public void errMsg(String msg){
		logger.info(msg);
	}

	public void errMsg(String msg, Node node){
		logger.info(msg + " [" + net.getNodeReference(node) + "]");
	}

	public RelationAlgorithm getRelationAlg(){
		return this.relationAlg;
	}

	public BSONAlg getBSONAlg(){
		return this.bsonAlg;
	}

	public BSONCycleAlg getBSONCycleAlg(){
		return bsonCycleAlg;
	}

	public CSONCycleAlg getCSONCycleAlg(){
		return csonCycleAlg;
	}

	public ONCycleAlg getONCycleAlg(){
		return onCycleAlg;
	}

	public TSONAlg getTSONAlg(){
		return tsonAlg;
	}
}
