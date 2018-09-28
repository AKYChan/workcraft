package org.workcraft.plugins.circuit.commands;

import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.utils.ResetUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

import java.util.HashSet;

public class TagForceInitSelfLoopsCommand extends CircuitAbstractInitialisationCommand {

    @Override
    public String getDisplayName() {
        return "Tag force init for all self-loops";
    }

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public Void execute(WorkspaceEntry we) {
        we.captureMemento();
        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);
        HashSet<? extends Contact> changedContacts = ResetUtils.setForceInitSelfLoops(circuit, true);
        if (changedContacts.isEmpty()) {
            we.cancelMemento();
        } else {
            we.saveMemento();
        }
        return null;
    }

}
