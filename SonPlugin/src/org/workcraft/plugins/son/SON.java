package org.workcraft.plugins.son;

import org.workcraft.Framework;
import org.workcraft.annotations.VisualClass;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.AbstractMathModel;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.math.PageNode;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.gui.propertyeditor.ModelProperties;
import org.workcraft.plugins.shared.CommonVisualSettings;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.plugins.son.connections.SONConnection.Semantics;
import org.workcraft.plugins.son.elements.*;
import org.workcraft.plugins.son.elements.Event;
import org.workcraft.plugins.son.exception.IncompatibleScenarioException;
import org.workcraft.plugins.son.properties.ConnectionTimePropertyDescriptor;
import org.workcraft.plugins.son.properties.DurationPropertyDescriptor;
import org.workcraft.plugins.son.properties.EndTimePropertyDescriptor;
import org.workcraft.plugins.son.properties.StartTimePropertyDescriptor;
import org.workcraft.plugins.son.util.Scenario;
import org.workcraft.plugins.son.util.ScenarioRef;
import org.workcraft.plugins.son.util.ScenarioSaveList;
import org.workcraft.util.Hierarchy;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

@VisualClass(org.workcraft.plugins.son.VisualSON.class)
public class SON extends AbstractMathModel {

    public final Condition createCondition(String name, Container container) {
        if (container == null) {
            container = getRoot();
        }
        Condition condition = new Condition();
        container.add(condition);
        if (name != null) {
            setName(condition, name);
        }
        return condition;
    }

    public SONConnection connect(Node first, Node second, Semantics semantics) throws InvalidConnectionException {
        if (getSONConnection(first, second) != null) {
            throw new InvalidConnectionException("Duplicate Connections" + getNodeReference(first) + " " + getNodeReference(second));
        }

        SONConnection con = new SONConnection((MathNode) first, (MathNode) second, semantics);
        Hierarchy.getNearestContainer(first, second).add(con);

        return con;
    }

    public Collection<Node> getComponents() {
        ArrayList<Node> result = new ArrayList<>();

        for (Node node : Hierarchy.getDescendantsOfType(getRoot(), MathNode.class)) {
            if (node instanceof PlaceNode || node instanceof Event) {
                result.add(node);
            }
        }

        //remove all nodes in collapsed blocks
        for (Block block : this.getBlocks()) {
            if (!this.getSONConnections(block).isEmpty()) {
                result.removeAll(block.getComponents());
                result.add(block);
            }
        }

        return result;
    }

    public Collection<Node> getNodes() {
        ArrayList<Node> result = new ArrayList<>();
        result.addAll(getComponents());
        for (SONConnection con : getSONConnections()) {
            if (con.getSemantics() != Semantics.BHVLINE) {
                result.add(con);
            }
        }

        return result;
    }

    public ArrayList<String> getNodeRefs(Collection<? extends Node> nodes) {
        ArrayList<String> result = new ArrayList<>();
        for (Node node : nodes) {
            result.add(getNodeReference(node));
        }

        return result;
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

    public Collection<ChannelPlace> getChannelPlaces() {
        return Hierarchy.getDescendantsOfType(getRoot(), ChannelPlace.class);
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

    public Collection<PlaceNode> getPlaceNodes() {
        ArrayList<PlaceNode> result = new ArrayList<>();
        for (Node node : getComponents()) {
            if (node instanceof PlaceNode) {
                result.add((PlaceNode) node);
            }
        }

        return result;
    }

    public Collection<Time> getTimeNodes() {
        ArrayList<Time> result = new ArrayList<>();
        for (Node node : getComponents()) {
            if (node instanceof Time) {
                result.add((Time) node);
            }
        }

        return result;
    }

    public String getComponentLabel(Node n) {
        if (n instanceof PlaceNode) {
            return ((PlaceNode) n).getLabel();
        }

        if (n instanceof TransitionNode) {
            return ((TransitionNode) n).getLabel();
        } else {
            return null;
        }
    }

    public void setForegroundColor(Node n, Color nodeColor) {
        if (n instanceof PlaceNode) {
            ((PlaceNode) n).setForegroundColor(nodeColor);
        }
        if (n instanceof TransitionNode) {
            ((TransitionNode) n).setForegroundColor(nodeColor);
        }
        if (n instanceof SONConnection) {
            ((SONConnection) n).setColor(nodeColor);
        }
        if (n instanceof ONGroup) {
            ((ONGroup) n).setForegroundColor(nodeColor);
        }
    }

    public void setFillColor(Node n, Color nodeColor) {
        if (n instanceof PlaceNode) {
            ((PlaceNode) n).setFillColor(nodeColor);
        }
        if (n instanceof TransitionNode) {
            ((TransitionNode) n).setFillColor(nodeColor);
        }
    }

    public void refreshAllColor() {
        for (Node n:  getComponents()) {
            setFillColor(n, CommonVisualSettings.getFillColor());
            setForegroundColor(n, CommonVisualSettings.getBorderColor());
            setTimeColor(n, Color.BLACK);
            setTokenColor(n, Color.BLACK);
        }
        for (ONGroup group : this.getGroups()) {
            setForegroundColor(group, SONSettings.getGroupForegroundColor());
        }

        for (SONConnection con : this.getSONConnections()) {
            setForegroundColor(con, CommonVisualSettings.getBorderColor());
            setTimeColor(con, Color.BLACK);
        }
        for (Block block : this.getBlocks()) {
            setFillColor(block, CommonVisualSettings.getFillColor());
            setForegroundColor(block, CommonVisualSettings.getBorderColor());
        }
    }

    public void refreshNodeColor() {
        for (Node n:  getComponents()) {
            setFillColor(n, CommonVisualSettings.getFillColor());
            setForegroundColor(n, CommonVisualSettings.getBorderColor());
            setTokenColor(n, Color.BLACK);
        }
        for (ONGroup group : this.getGroups()) {
            setForegroundColor(group, SONSettings.getGroupForegroundColor());
        }

        for (SONConnection con : this.getSONConnections()) {
            setForegroundColor(con, CommonVisualSettings.getBorderColor());
        }
        for (Block block : this.getBlocks()) {
            setFillColor(block, CommonVisualSettings.getFillColor());
            setForegroundColor(block, CommonVisualSettings.getBorderColor());
        }
    }

    public void setTimeColor(Node n, Color color) {
        if (n instanceof PlaceNode) {
            ((PlaceNode) n).setDurationColor(color);
        }
        if (n instanceof Condition) {
            ((Condition) n).setStartTimeColor(color);
            ((Condition) n).setEndTimeColor(color);
        }
        if (n instanceof Block) {
            ((Block) n).setDurationColor(color);
        }
        if (n instanceof SONConnection) {
            ((SONConnection) n).setTimeLabelColor(color);
        }
    }

    public void setTokenColor(Node n, Color color) {
        if (n instanceof PlaceNode) {
            ((PlaceNode) n).setTokenColor(color);
        }
    }

    public void clearMarking() {
        for (PlaceNode p : getPlaceNodes()) {
            p.setMarked(false);
        }
    }

    public void resetErrStates() {
        for (Condition con : this.getConditions()) {
            con.setErrors(0);
        }
        for (Event event : this.getEvents()) {
            event.setFaulty(false);
        }
    }

    public void resetConditionErrStates() {
        for (Condition con : this.getConditions()) {
            con.setErrors(0);
        }
    }

    public final ChannelPlace createChannelPlace() {
        return createChannelPlace(null);
    }

    public final ChannelPlace createChannelPlace(String name) {
        ChannelPlace newCP = new ChannelPlace();
        if (name != null) {
            setName(newCP, name);
        }
        getRoot().add(newCP);
        return newCP;
    }

    // Scenarios
    public final Scenario createScenario(String name, ScenarioRef scenario) {
        Scenario s = new Scenario();
        if (name != null) {
            setName(s, name);
        }
        getRoot().add(s);
        s.setScenario(scenario.toString());
        return s;
    }

    public Collection<Scenario> getScenarios() {
        return Hierarchy.getDescendantsOfType(getRoot(), Scenario.class);
    }

    public ScenarioSaveList importScenarios() {
        ScenarioSaveList result = new ScenarioSaveList();
        ArrayList<Scenario> removeList = new ArrayList<>();

        for (Scenario s : getScenarios()) {
            ScenarioRef ref = new ScenarioRef();
            try {
                ref.fromString(s.getScenario(), this);
            } catch (IncompatibleScenarioException e) {
                ref = null;
                removeList.add(s);
            }
            if (ref != null) {
                result.add(ref);
            }
        }

        if (!removeList.isEmpty()) {
            JOptionPane.showMessageDialog(Framework.getInstance().getMainWindow(),
                    "Elements in saved scenarios are incompatible with current model. "
                    + "Erroneous scenarios have been removed from the list",
                    "Incompatible scenario", JOptionPane.ERROR_MESSAGE);
        }
        for (Scenario scenario: removeList) {
            remove(scenario);
        }

        return result;
    }

    //Connection
    public Collection<SONConnection> getSONConnections() {
        return Hierarchy.getDescendantsOfType(getRoot(), SONConnection.class);
    }

    public Collection<SONConnection> getSONConnections(Node node) {
        ArrayList<SONConnection> result = new ArrayList<>();
        for (Connection con : this.getConnections(node)) {
            if (con instanceof SONConnection) {
                result.add((SONConnection) con);
            }
        }

        return result;
    }

    public SONConnection getSONConnection(Node first, Node second) {
        ArrayList<SONConnection> connection = new ArrayList<>();

        for (SONConnection con : getSONConnections(first)) {
            if (getSONConnections(second).contains(con)) {
                connection.add(con);
            }
        }
        if (connection.size() > 1) {
            throw new RuntimeException("Connection size between" + getNodeReference(first) + "and" + getNodeReference(first) + "> 1");
        }

        if (connection.size() == 0) {
            return null;
        }

        return connection.iterator().next();
    }

    public Collection<SONConnection> getInputSONConnections(Node node) {
        ArrayList<SONConnection> result = new ArrayList<>();
        for (SONConnection con : this.getSONConnections(node)) {
            if (node instanceof MathNode) {
                if (con.getSecond() == node) {
                    result.add(con);
                }
            }
        }
        return result;
    }

    public Collection<SONConnection> getOutputSONConnections(Node node) {
        Collection<SONConnection> result = new ArrayList<>();
        for (SONConnection con : this.getSONConnections(node)) {
            if (node instanceof MathNode) {
                if (con.getFirst() == node) {
                    result.add(con);
                }
            }
        }
        return result;
    }

    public Collection<Semantics> getSONConnectionTypes(Node node) {
        Collection<Semantics> result = new HashSet<>();
        for (SONConnection con : getSONConnections(node)) {
            result.add(con.getSemantics());
        }

        return result;
    }

    public Collection<Semantics> getSONConnectionTypes(Collection<Node> nodes) {
        Collection<Semantics> result = new HashSet<>();
        for (Node node : nodes) {
            for (SONConnection con : getSONConnections(node)) {
                if (nodes.contains(con.getFirst()) && nodes.contains(con.getSecond())) {
                    result.add(con.getSemantics());
                }
            }
        }
        return result;
    }

    public Semantics getSONConnectionType(Node first, Node second) {
        SONConnection con = getSONConnection(first, second);
        return con.getSemantics();
    }

    public Collection<Semantics> getInputSONConnectionTypes(Node node) {
        Collection<Semantics> result = new HashSet<>();
        for (SONConnection con : this.getSONConnections(node)) {
            if (node instanceof MathNode) {
                if (con.getSecond() == node) {
                    result.add(con.getSemantics());
                }
            }
        }
        return result;
    }

    public Collection<Semantics> getOutputSONConnectionTypes(Node node) {
        Collection<Semantics> result = new HashSet<>();
        for (SONConnection con : this.getSONConnections(node)) {
            if (node instanceof MathNode) {
                if (con.getFirst() == node) {
                    result.add(con.getSemantics());
                }
            }
        }
        return result;
    }

    public Collection<SONConnection> getInputPNConnections(Node node) {
        Collection<SONConnection> result = new ArrayList<>();

        for (SONConnection con : getInputSONConnections(node)) {
            if (con.getSemantics() == Semantics.PNLINE) {
                result.add(con);
            }
        }
        return result;
    }

    public Collection<SONConnection> getOutputPNConnections(Node node) {
        Collection<SONConnection> result = new ArrayList<>();

        for (SONConnection con : getOutputSONConnections(node)) {
            if (con.getSemantics() == Semantics.PNLINE) {
                result.add(con);
            }
        }
        return result;
    }

    public Collection<SONConnection> getInputScenarioPNConnections(Node node, ScenarioRef s) {
        Collection<SONConnection> result = new ArrayList<>();

        for (SONConnection con : getInputSONConnections(node)) {
            if (con.getSemantics() == Semantics.PNLINE) {
                if (s != null) {
                    if (s.getConnections(this).contains(con)) {
                        result.add(con);
                    }
                } else {
                    result.add(con);
                }
            }
        }
        return result;
    }

    public Collection<SONConnection> getOutputScenarioPNConnections(Node node, ScenarioRef s) {
        Collection<SONConnection> result = new ArrayList<>();

        for (SONConnection con : getOutputSONConnections(node)) {
            if (con.getSemantics() == Semantics.PNLINE) {
                if (s != null) {
                    if (s.getConnections(this).contains(con)) {
                        result.add(con);
                    }
                } else {
                    result.add(con);
                }
            }
        }
        return result;
    }

    //Group based methods
    public Collection<Block> getBlocks() {
        return Hierarchy.getDescendantsOfType(getRoot(), Block.class);
    }

    public Collection<TransitionNode> getTransitionNodes() {
        ArrayList<TransitionNode> result = new ArrayList<>();
        for (TransitionNode node :  Hierarchy.getDescendantsOfType(getRoot(), TransitionNode.class)) {
            if (node instanceof Block) {
                if (((Block) node).getIsCollapsed()) {
                    result.add(node);
                }
            }
            if (node instanceof Event) {
                result.add(node);
            }
        }
        return result;
    }

    public Collection<PageNode> getPageNodes() {
        return Hierarchy.getDescendantsOfType(getRoot(), PageNode.class);
    }

    public Collection<ONGroup> getGroups() {
        return Hierarchy.getDescendantsOfType(getRoot(), ONGroup.class);
    }

    public ONGroup getGroup(Node node) {
        for (ONGroup group : getGroups()) {
            if (group.contains(node)) {
                return group;
            }
        }
        return null;
    }

    public boolean isInSameGroup(Node first, Node second) {
        for (ONGroup group : getGroups()) {
            if (group.contains(first) && group.contains(second)) {
                return true;
            }
        }
        return false;
    }

    public String toString(Collection<? extends Node> nodes) {

        StringBuffer result = new StringBuffer("");

        boolean first = true;
        for (Node node : nodes) {
            if (!first) {
                result.append(',');
                result.append(' ' + getNodeReference(node));
            } else {
                result.append(' ');
                result.append('[');
                result.append(getNodeReference(node));
                first = false;
            }
        }
        if (!nodes.isEmpty()) {
            result.append(']');
        }
        return result.toString();
    }

    @Override
    public ModelProperties getProperties(Node node) {
        ModelProperties properties = super.getProperties(node);
        if (node instanceof SONConnection) {
            SONConnection con = (SONConnection) node;
            if (con.getSemantics() == Semantics.PNLINE || con.getSemantics() == Semantics.ASYNLINE) {
                properties.add(new ConnectionTimePropertyDescriptor((SONConnection) node));
            }
        }

        if (node instanceof Time) {
            properties.add(new StartTimePropertyDescriptor((Time) node));
            properties.add(new EndTimePropertyDescriptor((Time) node));
            properties.add(new DurationPropertyDescriptor((Time) node));
        }

        return properties;
    }
}
