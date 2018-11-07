package org.workcraft.plugins.policy;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.ShortName;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.*;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.graph.tools.CommentGeneratorTool;
import org.workcraft.gui.graph.tools.ConnectionTool;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.gui.propertyeditor.ModelProperties;
import org.workcraft.plugins.petri.VisualPetriNet;
import org.workcraft.plugins.petri.tools.PetriPlaceGeneratorTool;
import org.workcraft.plugins.policy.observers.SpanningTreeInvalidator;
import org.workcraft.plugins.policy.properties.BundleColorPropertyDescriptor;
import org.workcraft.plugins.policy.properties.BundleNamePropertyDescriptor;
import org.workcraft.plugins.policy.properties.BundlesOfTransitionPropertyDescriptor;
import org.workcraft.plugins.policy.properties.TransitionsOfBundlePropertyDescriptor;
import org.workcraft.plugins.policy.tools.PolicyBundledTransitionGeneratorTool;
import org.workcraft.plugins.policy.tools.PolicySelectionTool;
import org.workcraft.plugins.policy.tools.PolicySimulationTool;
import org.workcraft.util.ColorGenerator;
import org.workcraft.util.ColorUtils;
import org.workcraft.util.Hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@DisplayName ("Policy Net")
@ShortName("policy")
public class VisualPolicyNet extends VisualPetriNet {

    private final ColorGenerator bundleColorGenerator = new ColorGenerator(ColorUtils.getHsbPalette(
            new float[]{0.05f, 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f},
            new float[]{0.50f}, new float[]{0.9f, 0.7f, 0.5f}));

    public VisualPolicyNet(PolicyNet model) {
        this(model, null);
    }

    public VisualPolicyNet(PolicyNet model, VisualGroup root) {
        super(model, root == null ? new VisualLocality((Locality) model.getRoot()) : root);
        setGraphEditorTools();
        new SpanningTreeInvalidator(this).attach(getRoot());
    }

    private void setGraphEditorTools() {
        List<GraphEditorTool> tools = new ArrayList<>();
        tools.add(new PolicySelectionTool());
        tools.add(new CommentGeneratorTool());
        tools.add(new ConnectionTool());
        tools.add(new PetriPlaceGeneratorTool());
        tools.add(new PolicyBundledTransitionGeneratorTool());
        tools.add(new PolicySimulationTool());
        setGraphEditorTools(tools);
    }

    public PolicyNet getPolicyNet() {
        return (PolicyNet) getMathModel();
    }

    @Override
    public VisualGroup groupSelection() {
        ArrayList<VisualNode> selected = new ArrayList<>();
        ArrayList<MathNode> refSelected = new ArrayList<>();
        for (VisualNode node : SelectionHelper.getOrderedCurrentLevelSelection(this)) {
            if (node instanceof VisualTransformableNode) {
                selected.add(node);
                if (node instanceof VisualComponent) {
                    refSelected.add(((VisualComponent) node).getReferencedComponent());
                } else if (node instanceof VisualLocality) {
                    refSelected.add(((VisualLocality) node).getLocality());
                }
            }
        }
        VisualLocality newLocality = null;
        if (selected.size() > 0) {
            VisualLocality curLocality = (VisualLocality) getCurrentLevel();
            newLocality = new VisualLocality(getPolicyNet().createLocality(refSelected, curLocality.getLocality()));
            curLocality.add(newLocality);
            curLocality.reparent(selected, newLocality);

            ArrayList<VisualNode> connectionsToLocality = new ArrayList<>();
            for (VisualConnection connection : Hierarchy.getChildrenOfType(curLocality, VisualConnection.class)) {
                if (Hierarchy.isDescendant(connection.getFirst(), newLocality) && Hierarchy.isDescendant(connection.getSecond(), newLocality)) {
                    connectionsToLocality.add(connection);
                }
            }
            curLocality.reparent(connectionsToLocality, newLocality);
            select(newLocality);
        }
        return newLocality;
    }

    @Override
    public void ungroupSelection() {
        int count = 0;
        for (VisualNode node : SelectionHelper.getOrderedCurrentLevelSelection(this)) {
            if (node instanceof VisualLocality) {
                count++;
            }
        }
        if (count == 1) {
            ArrayList<VisualNode> toSelect = new ArrayList<>();
            Collection<MathNode> mathNodes = new ArrayList<>();
            for (VisualNode node : SelectionHelper.getOrderedCurrentLevelSelection(this)) {
                if (node instanceof VisualLocality) {
                    VisualLocality locality = (VisualLocality) node;
                    for (VisualNode subNode : locality.unGroup()) {
                        toSelect.add(subNode);
                    }
                    for (Node child : locality.getLocality().getChildren()) {
                        mathNodes.add((MathNode) child);
                    }
                    locality.getLocality().reparent(mathNodes, ((VisualLocality) getCurrentLevel()).getLocality());
                    getMathModel().remove(locality.getLocality());
                    getCurrentLevel().remove(locality);
                } else {
                    toSelect.add(node);
                }
            }
            select(toSelect);
        }
    }

    public Collection<VisualBundledTransition> getVisualBundledTransitions() {
        return Hierarchy.getDescendantsOfType(getRoot(), VisualBundledTransition.class);
    }

    public Collection<VisualBundle> getVisualBundles() {
        return Hierarchy.getDescendantsOfType(getRoot(), VisualBundle.class);
    }

    public Collection<VisualLocality> getVisualLocalities() {
        return Hierarchy.getDescendantsOfType(getRoot(), VisualLocality.class);
    }

    public VisualBundle createVisualBundle() {
        Bundle bundle = getPolicyNet().createBundle();
        VisualBundle visualBundle = new VisualBundle(bundle);
        getRoot().add(visualBundle);
        visualBundle.setColor(bundleColorGenerator.updateColor());
        return visualBundle;
    }

    public VisualBundle createVisualBundle(String name) {
        VisualBundle b = createVisualBundle();
        getPolicyNet().setName(b.getReferencedBundle(), name);
        return b;
    }

    public void bundleTransitions(Collection<VisualBundledTransition> transitions) {
        if (transitions != null && !transitions.isEmpty()) {
            VisualBundle bundle = createVisualBundle();
            for (VisualBundledTransition t: transitions) {
                bundle.getReferencedBundle().add(t.getReferencedTransition());
            }
        }
    }

    public void unbundleTransitions(Collection<VisualBundledTransition> transitions) {
        for (VisualBundledTransition t: transitions) {
            getPolicyNet().unbundleTransition(t.getReferencedTransition());
        }
        for (VisualBundle b: getVisualBundles()) {
            if (b.getReferencedBundle().isEmpty()) {
                getRoot().remove(b);
            }
        }
    }

    public String getBundlesOfTransitionAsString(VisualBundledTransition t) {
        String result = "";
        for (VisualBundle b: getBundlesOfTransition(t)) {
            if (result != "") {
                result += ", ";
            }
            result += getPolicyNet().getName(b.getReferencedBundle());
        }
        return result;
    }

    public void setBundlesOfTransitionAsString(VisualBundledTransition t, String s) {
        for (Bundle b: getPolicyNet().getBundles()) {
            b.remove(t.getReferencedTransition());
        }
        for (String ref : s.split("\\s*,\\s*")) {
            Node node = getPolicyNet().getNodeByReference(ref);
            if (node == null) {
                node = createVisualBundle(ref).getReferencedBundle();
            }
            if (node instanceof Bundle) {
                ((Bundle) node).add(t.getReferencedTransition());
            }
        }
        for (VisualBundle b: getVisualBundles()) {
            if (b.getReferencedBundle().isEmpty()) {
                getRoot().remove(b);
            }
        }
    }

    public String getTransitionsOfBundleAsString(VisualBundle b) {
        String result = "";
        for (VisualBundledTransition t: getTransitionsOfBundle(b)) {
            if (result != "") {
                result += ", ";
            }
            result += getPolicyNet().getName(t.getReferencedTransition());
        }
        return result;
    }

    public void setTransitionsOfBundleAsString(VisualBundle vb, String s) {
        Bundle b = vb.getReferencedBundle();
        for (BundledTransition t: new ArrayList<BundledTransition>(b.getTransitions())) {
            b.remove(t);
        }
        for (String ref : s.split("\\s*,\\s*")) {
            Node node = getPetriNet().getNodeByReference(ref);
            if (node instanceof BundledTransition) {
                b.add((BundledTransition) node);
            }
        }
    }

    public Collection<VisualBundle> getBundlesOfTransition(VisualBundledTransition t) {
        Collection<VisualBundle> result = new HashSet<>();
        if (t != null) {
            for (VisualBundle b: getVisualBundles()) {
                if (b.getReferencedBundle().contains(t.getReferencedTransition())) {
                    result.add(b);
                }
            }
        }
        return result;
    }

    public Collection<VisualBundledTransition> getTransitionsOfBundle(VisualBundle b) {
        Collection<VisualBundledTransition> result = new HashSet<>();
        for (VisualBundledTransition t: getVisualBundledTransitions()) {
            if (b.getReferencedBundle().contains(t.getReferencedTransition())) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public ModelProperties getProperties(VisualNode node) {
        ModelProperties properties = super.getProperties(node);
        if (node == null) {
            for (VisualBundle bundle: getVisualBundles()) {
                properties.add(new BundleNamePropertyDescriptor(this, bundle));
                properties.add(new BundleColorPropertyDescriptor(this, bundle));
                properties.add(new TransitionsOfBundlePropertyDescriptor(this, bundle));
            }
        } else if (node instanceof VisualBundledTransition) {
            VisualBundledTransition transition = (VisualBundledTransition) node;
            properties.add(new BundlesOfTransitionPropertyDescriptor(this, transition));
        }
        return properties;
    }

}
