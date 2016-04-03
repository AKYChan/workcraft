package org.workcraft.plugins.circuit;

import java.util.Collection;

import org.workcraft.dom.Node;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.observation.StateEvent;
import org.workcraft.observation.StateSupervisor;
import org.workcraft.plugins.circuit.Contact.SignalLevel;

public class InitStateConsistencySupervisor extends StateSupervisor  {

    private final Circuit circuit;

    public InitStateConsistencySupervisor(Circuit circuit) {
        this.circuit = circuit;
    }

    @Override
    public void handleEvent(StateEvent e) {
        if (e instanceof PropertyChangedEvent) {
            PropertyChangedEvent pce = (PropertyChangedEvent) e;
            Object sender = e.getSender();
            String propertyName = pce.getPropertyName();
            if ((sender instanceof Contact) && propertyName.equals(Contact.PROPERTY_SIGNAL_LEVEL)) {
                Contact contact = (Contact) sender;
                handleInitStateChange(contact);
            }
        }
    }

    private void handleInitStateChange(Contact contact) {
        boolean initToOne = contact.getSignalLevel() == SignalLevel.HIGH;
        Node parent = contact.getParent();
        boolean isZeroDelay = false;
        boolean invertDriver = false;
        boolean inverDriven = false;
        if (parent instanceof FunctionComponent) {
            FunctionComponent component = (FunctionComponent) parent;
            isZeroDelay = component.getIsZeroDelay();
            if (isZeroDelay && component.isInverter()) {
                invertDriver = contact.isOutput();
                inverDriven = contact.isInput();
            }
        }
        Contact driverContact = CircuitUtils.findDriver(circuit, contact, isZeroDelay);
        if (driverContact != null) {
            driverContact.setSignalLevel(initToOne != invertDriver);
        }
        Collection<Contact> drivenContacts = CircuitUtils.findDriven(circuit, contact, isZeroDelay);
        for (Contact drivenContact: drivenContacts) {
            drivenContact.setSignalLevel(initToOne != inverDriven);
        }
    }

}
