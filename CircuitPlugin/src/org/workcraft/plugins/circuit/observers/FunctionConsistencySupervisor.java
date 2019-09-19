package org.workcraft.plugins.circuit.observers;

import org.workcraft.dom.Node;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.Zero;
import org.workcraft.formula.utils.BooleanUtils;
import org.workcraft.observation.HierarchyEvent;
import org.workcraft.observation.HierarchySupervisor;
import org.workcraft.observation.NodesDeletingEvent;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.utils.Hierarchy;

import java.util.ArrayList;

public class FunctionConsistencySupervisor extends HierarchySupervisor {

    @Override
    public void handleEvent(HierarchyEvent e) {
        if (e instanceof NodesDeletingEvent) {
            for (Node node : e.getAffectedNodes()) {
                if (node instanceof Contact) {
                    // Update all set/reset functions when a contact is removed
                    final Contact contact = (Contact) node;
                    handleContactRemoval(contact);
                }
            }
        }
    }

    private void handleContactRemoval(final Contact contact) {
        final ArrayList<FunctionContact> functionContacts = new ArrayList<>(
                Hierarchy.getChildrenOfType(getRoot(), FunctionContact.class));

        for (final FunctionContact functionContact : functionContacts) {
            final BooleanFormula setFunction = BooleanUtils.replaceClever(
                    functionContact.getSetFunction(), contact, Zero.getInstance());

            functionContact.setSetFunction(setFunction);

            final BooleanFormula resetFunction = BooleanUtils.replaceClever(
                    functionContact.getResetFunction(), contact, Zero.getInstance());

            functionContact.setResetFunction(resetFunction);
        }
    }

}
