package org.workcraft.plugins.circuit;

import org.workcraft.dom.Container;
import org.workcraft.dom.math.AbstractMathModel;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathNode;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.circuit.observers.FunctionConsistencySupervisor;
import org.workcraft.plugins.circuit.observers.IOTypeConsistencySupervisor;
import org.workcraft.plugins.circuit.observers.ZeroDelayConsistencySupervisor;
import org.workcraft.plugins.circuit.references.CircuitReferenceManager;
import org.workcraft.serialisation.References;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.MultiSet;

import java.util.Collection;

public class Circuit extends AbstractMathModel {

    public Circuit() {
        this(null, null);
    }

    public Circuit(Container root, References refs) {
        super(root, new CircuitReferenceManager(refs));
        new FunctionConsistencySupervisor(this).attach(getRoot());
        new ZeroDelayConsistencySupervisor(this).attach(getRoot());
        new IOTypeConsistencySupervisor(this).attach(getRoot());
    }

    @Override
    public MathConnection connect(MathNode first, MathNode second) throws InvalidConnectionException {
        validateConnection(first, second);
        MathConnection connection = new MathConnection(first, second);
        Container container = Hierarchy.getNearestContainer(first, second);
        if (container instanceof CircuitComponent) {
            container = (Container) container.getParent();
        }
        container.add(connection);
        return connection;
    }

    public Collection<FunctionContact> getFunctionContacts() {
        return Hierarchy.getDescendantsOfType(getRoot(), FunctionContact.class);
    }

    public Collection<FunctionComponent> getFunctionComponents() {
        return Hierarchy.getDescendantsOfType(getRoot(), FunctionComponent.class);
    }

    public Collection<Joint> getJoints() {
        return Hierarchy.getDescendantsOfType(getRoot(), Joint.class);
    }

    public Collection<Contact> getPorts() {
        return Hierarchy.getDescendantsOfType(getRoot(), Contact.class, contact -> contact.isPort());
    }

    public Collection<Contact> getInputPorts() {
        return Hierarchy.getDescendantsOfType(getRoot(), Contact.class, contact -> contact.isPort() && contact.isInput());
    }

    public Collection<Contact> getOutputPorts() {
        return Hierarchy.getDescendantsOfType(getRoot(), Contact.class, contact -> contact.isPort() && contact.isOutput());
    }

    public Collection<Contact> getDrivers() {
        return Hierarchy.getDescendantsOfType(getRoot(), Contact.class, contact -> contact.isDriver());
    }

    @Override
    public MultiSet<String> getStatistics() {
        MultiSet<String> result = new MultiSet<>();
        result.add("Component", getFunctionComponents().size());
        result.add("Port", getPorts().size());
        return result;
    }

}
