package org.workcraft.plugins.circuit.commands;

import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.utils.ResetUtils;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

import java.util.ArrayList;
import java.util.HashSet;

public class ForceInitSequentialGatesCommand extends CircuitAbstractInitialisationCommand {

    @Override
    public String getDisplayName() {
        return "Force init all sequential gates";
    }

    @Override
    public Position getPosition() {
        return Position.TOP_MIDDLE;
    }

    @Override
    public Void execute(WorkspaceEntry we) {
        we.captureMemento();
        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);
        HashSet<? extends Contact> changedContacts = ResetUtils.setForceInitSequentialGates(circuit, true);
        if (changedContacts.isEmpty()) {
            we.cancelMemento();
        } else {
            we.saveMemento();
            ArrayList<String> refs = ReferenceHelper.getReferenceList(circuit, changedContacts);
            LogUtils.logInfo(LogUtils.getTextWithRefs("Force init output pin", refs));
        }
        return super.execute(we);
    }

}
