/*
*
*
* Copyright 2008,2009 Newcastle University
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.circuit.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.workcraft.NodeTransformer;
import org.workcraft.TransformationTool;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualFunctionComponent;
import org.workcraft.plugins.circuit.VisualFunctionContact;
import org.workcraft.plugins.cpog.optimisation.BooleanFormula;
import org.workcraft.plugins.cpog.optimisation.booleanvisitors.BooleanUtils;
import org.workcraft.plugins.cpog.optimisation.expressions.BooleanOperations;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.LogUtils;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

public class BubbleToggleTool extends TransformationTool implements NodeTransformer {

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
        return WorkspaceUtils.canHas(we, VisualCircuit.class);
    }

    @Override
    public boolean isApplicableTo(Node node) {
        return (node instanceof VisualFunctionComponent)
                || ((node instanceof VisualFunctionContact)
                && (node.getParent() instanceof VisualFunctionComponent));
    }

    @Override
    public boolean isEnabled(WorkspaceEntry we, Node node) {
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
        return Position.BOTTOM;
    }

    @Override
    public void run(WorkspaceEntry we) {
        final VisualModel visualModel = we.getModelEntry().getVisualModel();
        if (visualModel != null) {
            Collection<VisualFunctionContact> contacts = Hierarchy.getDescendantsOfType(visualModel.getRoot(), VisualFunctionContact.class);
            Collection<Node> selection = new LinkedList<>(visualModel.getSelection());
            for (Node node: new LinkedList<>(selection)) {
                if (node instanceof VisualFunctionComponent) {
                    VisualFunctionComponent component = (VisualFunctionComponent) node;
                    selection.addAll(component.getVisualOutputs());
                }
            }
            contacts.retainAll(selection);
            if (!contacts.isEmpty()) {
                we.saveMemento();
                for (VisualFunctionContact contact: contacts) {
                    transform(visualModel, contact);
                }
            }
        }
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
                    contact.setSetFunction(BooleanOperations.not(setFunction));
                } else {
                    contact.setSetFunction(resetFunction);
                    contact.setResetFunction(setFunction);
                }
            } else {
                for (FunctionContact dependantContact: getDependantContacts(contact)) {
                    BooleanFormula setFunction = dependantContact.getSetFunction();
                    BooleanFormula notContact = BooleanOperations.not(contact);
                    if (setFunction != null) {
                        dependantContact.setSetFunction(BooleanUtils.prettifyReplace(setFunction, contact, notContact));
                    }
                    BooleanFormula resetFunction = dependantContact.getResetFunction();
                    if (resetFunction != null) {
                        dependantContact.setSetFunction(BooleanUtils.prettifyReplace(resetFunction, contact, notContact));
                    }
                }
            }
            Node parent = node.getParent();
            if (parent instanceof VisualFunctionComponent) {
                VisualFunctionComponent component = (VisualFunctionComponent) parent;
                String label = component.getLabel();
                if (!label.isEmpty()) {
                    String ref = circuit.getNodeReference(node);
                    LogUtils.logWarningLine("Label '" + label + "' is removed from component `" + ref + "`.");
                    component.setLabel("");
                }
                component.invalidateRenderingResult();
            }
        }
    }

}
