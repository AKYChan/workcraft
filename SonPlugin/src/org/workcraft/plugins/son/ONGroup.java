package org.workcraft.plugins.son;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import org.workcraft.annotations.IdentifierPrefix;
import org.workcraft.annotations.VisualClass;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.math.PageNode;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.plugins.son.elements.Block;
import org.workcraft.plugins.son.elements.Condition;
import org.workcraft.plugins.son.elements.Event;
import org.workcraft.plugins.son.elements.TransitionNode;
import org.workcraft.util.Hierarchy;

@IdentifierPrefix("g")
@VisualClass (org.workcraft.plugins.son.VisualONGroup.class)
public class ONGroup extends PageNode {

    private String label = "";
    private Color color = SONSettings.getGroupForegroundColor();

    public Collection<Node> getComponents() {
        ArrayList<Node> result = new ArrayList<>();

        for (Node node : Hierarchy.getDescendantsOfType(this, MathNode.class)) {
            if (node instanceof Condition || node instanceof Event) {
                result.add(node);
            }
        }

        //remove the nodes in isolate blocks
        for (Block block : this.getBlocks()) {
            boolean isCollapsed = false;
            for (SONConnection con : getSONConnections()) {
                if (con.getFirst() == block || con.getSecond() == block) {
                    isCollapsed = true;
                    break;
                }
            }
            if (isCollapsed) {
                result.removeAll(block.getComponents());
                result.add(block);
            }
        }
        return result;
    }

    public boolean contains(Node node) {
        if (this.getComponents().contains(node)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean containsAll(Collection<Node> nodes) {
        for (Node node: nodes) {
            if (!this.contains(node)) {
                return false;
            }
        }
        return true;
    }

    public Collection<Condition> getConditions() {
        ArrayList<Condition> result = new ArrayList<>();
        for (Node node : getComponents()) {
            if (node instanceof Condition) {
                result.add((Condition) node);
            }
        }

        return result;
    }

    public Collection<Event> getEvents() {
        ArrayList<Event> result = new ArrayList<>();
        for (Node node : getComponents()) {
            if (node instanceof Event) {
                result.add((Event) node);
            }
        }

        return result;
    }

    public Collection<TransitionNode> getTransitionNodes() {
        ArrayList<TransitionNode> result = new ArrayList<>();
        for (Node node : getComponents()) {
            if (node instanceof Event) {
                result.add((Event) node);
            }
            if (node instanceof Block) {
                if (((Block) node).getIsCollapsed()) {
                    result.add((Block) node);
                }
            }
        }

        return result;
    }

    public Collection<PageNode> getPageNodes() {
        return Hierarchy.getDescendantsOfType(this, PageNode.class);
    }

    public Collection<Block> getBlocks() {
        return Hierarchy.getDescendantsOfType(this, Block.class);
    }

    public Collection<Block> getCollapsedBlocks() {
        Collection<Block> result = new ArrayList<>();
        for (Block block : getBlocks()) {
            if (block.getIsCollapsed()) {
                result.add(block);
            }
        }
        return result;
    }

    public Collection<Block> getUncollapsedBlocks() {
        Collection<Block> result = new ArrayList<>();
        for (Block block : getBlocks()) {
            if (!block.getIsCollapsed()) {
                result.add(block);
            }
        }
        return result;
    }

    public Collection<SONConnection> getSONConnections() {
        return Hierarchy.getDescendantsOfType(this, SONConnection.class);
    }

    public void setForegroundColor(Color value) {
        if (!color.equals(value)) {
            color = value;
            sendNotification(new PropertyChangedEvent(this, "foregroundColor"));
        }
    }

    public Color getForegroundColor() {
        return color;
    }

    public void setLabel(String value) {
        if (value == null) value = "";
        if (!label.equals(label)) {
            label = value;
            sendNotification(new PropertyChangedEvent(this, "label"));
        }
    }

    public String getLabel() {
        return label;
    }
}
