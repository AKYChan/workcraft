package org.workcraft.plugins.fst.tools;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.dom.visual.VisualTransformableNode;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.ContainerDecoration;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.plugins.fsm.State;
import org.workcraft.plugins.fsm.VisualEvent;
import org.workcraft.plugins.fsm.VisualState;
import org.workcraft.plugins.fst.Fst;
import org.workcraft.plugins.fst.VisualFst;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualTransition;
import org.workcraft.plugins.shared.CommonSimulationSettings;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.VisualStg;
import org.workcraft.plugins.stg.tools.StgSimulationTool;
import org.workcraft.util.Func;

public class FstSimulationTool extends StgSimulationTool {
    private FstToStgConverter generator;

    @Override
    public void activated(final GraphEditor editor) {
        super.activated(editor);
        setStatePaneVisibility(true);
    }

    @Override
    public String getTraceLabelByReference(String ref) {
        String label = null;
        if (ref != null) {
            label = generator.getEventLabel(ref);
            if (label == "") label = Character.toString(VisualEvent.EPSILON_SYMBOL);
        }
        if (label == null) {
            label = super.getTraceLabelByReference(ref);
        }
        return label;
    }

    @Override
    public void generateUnderlyingModel(VisualModel model) {
        final VisualFst fst = (VisualFst) model;
        final VisualStg stg = new VisualStg(new Stg());
        generator = new FstToStgConverter(fst, stg);
        setUnderlyingModel(generator.getDstModel());
    }

    @Override
    public void applySavedState(final GraphEditor editor) {
        if ((savedState == null) || savedState.isEmpty()) {
            return;
        }
        MathModel model = editor.getModel().getMathModel();
        if (model instanceof Fst) {
            editor.getWorkspaceEntry().saveMemento();
            Fst fst = (Fst) model;
            for (State state: fst.getStates()) {
                String ref = fst.getNodeReference(state);
                Node underlyingNode = getUnderlyingStg().getNodeByReference(ref);
                if ((underlyingNode instanceof Place) && savedState.containsKey(underlyingNode)) {
                    boolean isInitial = savedState.get(underlyingNode) > 0;
                    state.setInitial(isInitial);
                }
            }
        }
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

        Transition transition = null;
        if (node instanceof VisualTransformableNode) {
        }

        if (transition == null) {
            transition = getExcitedTransitionOfNode(node);
        }

        if (transition != null) {
            executeTransition(e.getEditor(), transition);
        }
    }

    @Override
    public boolean isContainerExcited(Container container) {
        if (excitedContainers.containsKey(container)) return excitedContainers.get(container);
        boolean ret = false;
        for (Node node: container.getChildren()) {
            if (node instanceof VisualEvent) {
                ret = ret || (getExcitedTransitionOfNode(node) != null);
            }
            if (node instanceof Container) {
                ret = ret || isContainerExcited((Container) node);
            }
            if (ret) break;
        }
        excitedContainers.put(container, ret);
        return ret;
    }

    @Override
    public Decorator getDecorator(final GraphEditor editor) {
        return new Decorator() {
            @Override
            public Decoration getDecoration(Node node) {
                Node transition = getTraceCurrentNode();
                final boolean isExcited = getExcitedTransitionOfNode(node) != null;
                final boolean isHighlighted = generator.isRelated(node, transition);

                if (node instanceof VisualEvent) {
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

                if (node instanceof VisualState) {
                    final VisualPlace p = generator.getRelatedPlace((VisualState) node);
                    return new Decoration() {
                        @Override
                        public Color getColorisation() {
                            return null;
                        }
                        @Override
                        public Color getBackground() {
                            if (p.getReferencedPlace().getTokens() > 0) return CommonSimulationSettings.getEnabledForegroundColor();
                            return null;
                        }
                    };
                }

                if (node instanceof VisualPage || node instanceof VisualGroup) {
                    if (node.getParent() == null) return null; // do not work with the root node
                    final boolean ret = isContainerExcited((Container) node);
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
                            return ret;
                        }
                    };

                }

                return null;
            }
        };
    }

    private Transition getExcitedTransitionOfNode(Node node) {
        if ((node != null) && (node instanceof VisualEvent)) {
            VisualTransition vTransition = generator.getRelatedTransition((VisualEvent) node);
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
