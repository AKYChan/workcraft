package org.workcraft.dom.visual;

import org.workcraft.commands.AbstractLayoutCommand;
import org.workcraft.dom.Container;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.observation.ObservableState;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

public interface VisualModel extends Model, ObservableState {
    void createDefaultStructure();
    MathModel getMathModel();
    void draw(Graphics2D g, Decorator decorator);

    void setCurrentLevel(Container group);
    Container getCurrentLevel();

    String getNodeMathReference(Node node);
    String getMathName(Node node);
    void setMathName(Node node, String name);

    void validateConnection(Node first, Node second) throws InvalidConnectionException;
    VisualConnection connect(Node first, Node second) throws InvalidConnectionException;
    VisualConnection connect(Node first, Node second, MathConnection connection) throws InvalidConnectionException;

    void validateUndirectedConnection(Node first, Node second) throws InvalidConnectionException;
    VisualConnection connectUndirected(Node first, Node second) throws InvalidConnectionException;

    <T extends VisualComponent> T createVisualComponent(MathNode refNode, Class<T> type, Container container);
    <T extends VisualComponent> T createVisualComponent(MathNode refNode, Class<T> type);
    <T extends VisualComponent> T getVisualComponent(MathNode refNode, Class<T> type);
    <T extends VisualComponent> T getVisualComponentByMathReference(String ref, Class<T> type);

    <T extends VisualReplica> T createVisualReplica(VisualComponent master, Class<T> type, Container container);

    void selectAll();
    void selectNone();
    void selectInverse();
    void select(Node node);
    void select(Collection<Node> nodes);
    void addToSelection(Node node);
    void addToSelection(Collection<Node> nodes);
    void removeFromSelection(Node node);
    void removeFromSelection(Collection<Node> nodes);
    void deleteSelection();

    Collection<Node> getSelection();

    boolean isGroupable(Node node);
    VisualGroup groupSelection();
    VisualPage groupPageSelection();
    void ungroupSelection();

    Collection<Node> hitBox(Point2D p1, Point2D p2);
    AbstractLayoutCommand getBestLayouter();
    Rectangle2D getBoundingBox();

    void setGraphEditorTools(List<GraphEditorTool> tool);
    List<GraphEditorTool> getGraphEditorTools();
}
