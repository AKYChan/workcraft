package org.workcraft.plugins.graph.tools;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.ContainerDecoration;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.plugins.graph.VisualGraph;
import org.workcraft.plugins.graph.VisualVertex;
import org.workcraft.plugins.petri.PetriNet;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.petri.VisualPetriNet;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.petri.tools.PetriSimulationTool;
import org.workcraft.plugins.shared.CommonSimulationSettings;
import org.workcraft.util.Func;

public class GraphSimulationTool extends PetriSimulationTool {

    private GraphToPetriConverter generator;

    @Override
    public void activated(final GraphEditor editor) {
        super.activated(editor);
        setStatePaneVisibility(false);
    }

    @Override
    public String getTraceLabelByReference(String ref) {
        String label = null;
        if (ref != null) {
            label = generator.getSymbol(ref);
            if (label == "") {
                label = Character.toString(VisualVertex.EPSILON_SYMBOL);
            }
        }
        if (label == null) {
            label = super.getTraceLabelByReference(ref);
        }
        return label;
    }

    @Override
    public void generateUnderlyingModel(VisualModel model) {
        final VisualGraph graph = (VisualGraph) model;
        final VisualPetriNet petri = new VisualPetriNet(new PetriNet());
        generator = new GraphToPetriConverter(graph, petri);
        setUnderlyingModel(generator.getDstModel());
    }

    @Override
    public void applySavedState(final GraphEditor editor) {
        // Not applicable to this model
    }

    @Override
    public void mousePressed(GraphEditorMouseEvent e) {
        Point2D posRoot = e.getPosition();
        Node node = HitMan.hitDeepest(posRoot, e.getModel().getRoot(),
                new Func<Node, Boolean>() {
                    @Override
                    public Boolean eval(Node node) {
                        return getExcitedTransitionOfNode(node) != null;
                    }
                });

        Transition transition = getExcitedTransitionOfNode(node);
        if (transition != null) {
            executeTransition(e.getEditor(), transition);
        }
    }

    @Override
    public Decorator getDecorator(final GraphEditor editor) {
        return new Decorator() {
            @Override
            public Decoration getDecoration(Node node) {
                if (generator == null) return null;
                if (node instanceof VisualVertex) {
                    return getVertexDecoration((VisualVertex) node);
                } else if (node instanceof VisualConnection) {
                    return getConnectionDecorator((VisualConnection) node);
                } else if (node instanceof VisualPage || node instanceof VisualGroup) {
                    return getContainerDecoration((Container) node);
                }

                return null;
            }
        };
    }

    private Decoration getVertexDecoration(VisualVertex vertex) {
        Node transition = getTraceCurrentNode();
        final boolean isHighlighted = generator.isRelated(vertex, transition);
        final boolean isExcited = isVertexExcited(vertex);
        return new Decoration() {
            @Override
            public Color getColorisation() {
                if (isHighlighted) return CommonSimulationSettings.getEnabledBackgroundColor();
                if (isExcited) return CommonSimulationSettings.getEnabledForegroundColor();
                return null;
            }

            @Override
            public Color getBackground() {
                if (isHighlighted) return CommonSimulationSettings.getEnabledForegroundColor();
                if (isExcited) return CommonSimulationSettings.getEnabledBackgroundColor();
                return null;
            }
        };
    }

    protected Decoration getConnectionDecorator(VisualConnection node) {
        if (!isConnectionExcited((VisualConnection) node)) return null;
        return new Decoration() {
            @Override
            public Color getColorisation() {
                return CommonSimulationSettings.getEnabledForegroundColor();
            }

            @Override
            public Color getBackground() {
                return CommonSimulationSettings.getEnabledBackgroundColor();
            }
        };
    }

    @Override
    public Decoration getContainerDecoration(Container container) {
        final boolean isExcited = isContainerExcited(container);
        return new ContainerDecoration() {
            @Override
            public Color getColorisation() {
                return null;
            }
            @Override
            public Color getBackground() {
                return null;
            }
            @Override
            public boolean isContainerExcited() {
                return isExcited;
            }
        };
    }

    @Override
    public boolean isConnectionExcited(VisualConnection connection) {
        VisualPlace place = generator.getRelatedPlace(connection);
        return (place == null) ? false : place.getReferencedPlace().getTokens() != 0;
    }

    private boolean isVertexExcited(VisualVertex vertex) {
        VisualTransition transition = generator.getRelatedTransition(vertex);
        return (transition == null) ? false : isEnabledNode(transition.getReferencedTransition());
    }

    private Transition getExcitedTransitionOfNode(Node node) {
        if ((node != null) && (node instanceof VisualVertex)) {
            VisualTransition vTransition = generator.getRelatedTransition((VisualVertex) node);
            if (vTransition != null) {
                Transition transition = vTransition.getReferencedTransition();
                if (isEnabledNode(transition)) {
                    return transition;
                }
            }
        }
        return null;
    }

}
