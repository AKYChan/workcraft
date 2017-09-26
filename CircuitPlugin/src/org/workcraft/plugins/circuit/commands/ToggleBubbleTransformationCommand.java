package org.workcraft.plugins.circuit.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.workcraft.NodeTransformer;
import org.workcraft.commands.AbstractTransformationCommand;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.BooleanOperations;
import org.workcraft.formula.utils.BooleanUtils;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualFunctionComponent;
import org.workcraft.plugins.circuit.VisualFunctionContact;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class ToggleBubbleTransformationCommand extends AbstractTransformationCommand implements NodeTransformer {

    @Override
    public String getDisplayName() {
        return "Toggle inversion of selected contacts and outputs of selected components";
    }

    @Override
    public String getPopupName() {
        return "Toggle inversion";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, VisualCircuit.class);
    }

    @Override
    public boolean isApplicableTo(Node node) {
        return (node instanceof VisualFunctionComponent)
                || ((node instanceof VisualFunctionContact)
                && (node.getParent() instanceof VisualFunctionComponent));
    }

    @Override
    public boolean isEnabled(ModelEntry me, Node node) {
        boolean result = false;
        if (node instanceof VisualFunctionComponent) {
            VisualFunctionComponent component = (VisualFunctionComponent) node;
            if (!component.getVisualOutputs().isEmpty()) {
                result = true;
            }
        } else if (node instanceof VisualFunctionContact) {
            FunctionContact contact = ((VisualFunctionContact) node).getReferencedFunctionContact();
            for (FunctionContact dependantContact: getDependantContacts(contact)) {
                if ((dependantContact.getSetFunction() != null) || (dependantContact.getResetFunction() != null)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private HashSet<FunctionContact> getDependantContacts(FunctionContact contact) {
        HashSet<FunctionContact> result = new HashSet<>();
        Node parent = contact.getParent();
        if (parent instanceof FunctionComponent) {
            if (contact.isOutput()) {
                result.add(contact);
            } else {
                FunctionComponent component = (FunctionComponent) parent;
                for (Contact outputContact: component.getOutputs()) {
                    if (outputContact instanceof FunctionContact) {
                        result.add((FunctionContact) outputContact);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public Collection<Node> collect(Model model) {
        Collection<Node> contacts = new HashSet<>();
        if (model instanceof VisualCircuit) {
            VisualCircuit circuit = (VisualCircuit) model;
            contacts.addAll(Hierarchy.getDescendantsOfType(circuit.getRoot(), VisualFunctionContact.class));
            Collection<Node> selection = new LinkedList<>(circuit.getSelection());
            for (Node node: new LinkedList<>(selection)) {
                if (node instanceof VisualFunctionComponent) {
                    VisualFunctionComponent component = (VisualFunctionComponent) node;
                    selection.addAll(component.getVisualOutputs());
                }
            }
            contacts.retainAll(selection);
        }
        return contacts;
    }

    @Override
    public void transform(Model model, Node node) {
        if ((model instanceof VisualCircuit) && (node instanceof VisualFunctionContact)) {
            Circuit circuit = (Circuit) ((VisualCircuit) model).getMathModel();
            FunctionContact contact = ((VisualFunctionContact) node).getReferencedFunctionContact();
            if (contact.isOutput()) {
                BooleanFormula setFunction = contact.getSetFunction();
                BooleanFormula resetFunction = contact.getResetFunction();
                if (resetFunction == null) {
                    contact.setSetFunctionQuiet(BooleanOperations.not(setFunction));
                } else {
                    contact.setSetFunctionQuiet(resetFunction);
                    contact.setResetFunctionQuiet(setFunction);
                }
            } else {
                for (FunctionContact dependantContact: getDependantContacts(contact)) {
                    BooleanFormula setFunction = dependantContact.getSetFunction();
                    BooleanFormula notContact = BooleanOperations.not(contact);
                    if (setFunction != null) {
                        dependantContact.setSetFunctionQuiet(BooleanUtils.dumbReplace(setFunction, contact, notContact));
                    }
                    BooleanFormula resetFunction = dependantContact.getResetFunction();
                    if (resetFunction != null) {
                        dependantContact.setResetFunctionQuiet(BooleanUtils.dumbReplace(resetFunction, contact, notContact));
                    }
                }
            }
            Node parent = node.getParent();
            if (parent instanceof VisualFunctionComponent) {
                VisualFunctionComponent component = (VisualFunctionComponent) parent;
                String label = component.getLabel();
                if (!label.isEmpty()) {
                    String ref = circuit.getNodeReference(node);
                    LogUtils.logWarning("Label '" + label + "' is removed from component '" + ref + "'.");
                    component.setLabel("");
                }
                component.invalidateRenderingResult();
            }
        }
    }

}
