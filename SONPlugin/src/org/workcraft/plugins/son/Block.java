package org.workcraft.plugins.son;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import org.workcraft.dom.Node;
import org.workcraft.dom.math.PageNode;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.shared.CommonVisualSettings;
import org.workcraft.plugins.son.components.Condition;
import org.workcraft.plugins.son.components.Event;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.util.Hierarchy;

public class Block extends PageNode{
	private String label="";
	private Color color = CommonVisualSettings.getBorderColor();

	public Collection<Node> getComponents(){
		ArrayList<Node> result = new ArrayList<Node>();
		result.addAll(getConditions());
		result.addAll(getEvents());
		return result;
	}

	public Collection<Condition> getConditions(){
		return Hierarchy.getDescendantsOfType(this, Condition.class);
	}

	public Collection<Event> getEvents(){
		return Hierarchy.getDescendantsOfType(this, Event.class);
	}

	public Collection<PageNode> getPageNodes(){
		return Hierarchy.getDescendantsOfType(this, PageNode.class);
	}

	public Collection<Block> getBlock(){
		return Hierarchy.getDescendantsOfType(this, Block.class);
	}

	public Collection<SONConnection> getSONConnections(){
		return Hierarchy.getDescendantsOfType(this, SONConnection.class);
	}

	public void setForegroundColor(Color color){
		this.color = color;
		sendNotification(new PropertyChangedEvent(this, "foregroundColor"));
	}

	public Color getForegroundColor(){
		return color;
	}

	public void setLabel(String label){
		this.label = label;
		sendNotification(new PropertyChangedEvent(this, "label"));
	}

	public String getLabel(){
		return label;
	}

}
