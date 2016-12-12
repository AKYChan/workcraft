package org.workcraft.plugins.stg.tools;

import java.util.Collection;
import java.util.HashSet;

import org.workcraft.NodeTransformer;
import org.workcraft.TransformationTool;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.VisualSignalTransition;
import org.workcraft.plugins.stg.VisualStg;
import org.workcraft.workspace.ModelEntry;

public class MirrorSignalTool extends TransformationTool implements NodeTransformer {

    @Override
    public String getDisplayName() {
        return "Mirror signals (selected or all)";
    }

    @Override
    public String getPopupName() {
        return "Mirror signal";
    }

    @Override
    public boolean isApplicableTo(ModelEntry me) {
        return me.getMathModel() instanceof Stg;
    }

    @Override
    public boolean isApplicableTo(Node node) {
        if (node instanceof VisualSignalTransition) {
            VisualSignalTransition signalTransition = (VisualSignalTransition) node;
            Type signalType = signalTransition.getSignalType();
            return (signalType == Type.INPUT) || (signalType == Type.OUTPUT);
        }
        return false;
    }

    @Override
    public boolean isEnabled(ModelEntry me, Node node) {
        return true;
    }

    @Override
    public Position getPosition() {
        return null;
    }

    @Override
    public Collection<Node> collect(Model model) {
        Collection<Node> signalTransitions = new HashSet<>();
        if (model instanceof VisualStg) {
            VisualStg stg = (VisualStg) model;
            signalTransitions.addAll(stg.getVisualSignalTransitions());
            Collection<Node> selection = stg.getSelection();
            if (!selection.isEmpty()) {
                signalTransitions.retainAll(selection);
            }
        }
        return signalTransitions;
    }

    @Override
    public void transform(Model model, Collection<Node> nodes) {
        if (model instanceof VisualStg) {
            VisualStg visualStg = (VisualStg) model;
            HashSet<String> processedSignals = new HashSet<>();
            Stg stg = (Stg) visualStg.getMathModel();
            for (Node node: nodes) {
                if (node instanceof VisualSignalTransition) {
                    VisualSignalTransition visualTransition = (VisualSignalTransition) node;
                    SignalTransition transition = visualTransition.getReferencedTransition();
                    String signalRef = stg.getSignalReference(transition);
                    if (!processedSignals.contains(signalRef)) {
                        transform(visualStg, visualTransition);
                        processedSignals.add(signalRef);
                    }
                }
            }
        }
    }

    @Override
    public void transform(Model model, Node node) {
        if ((model instanceof VisualStg) && (node instanceof VisualSignalTransition)) {
            SignalTransition signalTransition = ((VisualSignalTransition) node).getReferencedTransition();
            Type signalType = signalTransition.getSignalType();
            if (signalType == Type.INPUT) {
                signalTransition.setSignalType(Type.OUTPUT);
            } else if (signalType == Type.OUTPUT) {
                signalTransition.setSignalType(Type.INPUT);
            }
        }
    }

}
