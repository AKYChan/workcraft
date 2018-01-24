package org.workcraft.plugins.xmas;

import java.util.Collection;

import org.workcraft.annotations.CustomTools;
import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.ShortName;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.visual.AbstractVisualModel;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.exceptions.NodeCreationException;
import org.workcraft.exceptions.VisualModelInstantiationException;
import org.workcraft.plugins.xmas.components.VisualXmasComponent;
import org.workcraft.plugins.xmas.components.VisualXmasConnection;
import org.workcraft.plugins.xmas.components.VisualXmasContact;
import org.workcraft.plugins.xmas.components.XmasContact.IOType;
import org.workcraft.util.Hierarchy;

@DisplayName("xMAS Circuit")
@ShortName("xMAS")
@CustomTools (XmasToolsProvider.class)
public class VisualXmas extends AbstractVisualModel {

    private final Xmas circuit;

    @Override
    public void validateConnection(Node first, Node second) throws InvalidConnectionException {
        if (!(first instanceof VisualXmasContact) || !(second instanceof VisualXmasContact)) {
            throw new InvalidConnectionException("Connection is only allowed between ports");
        } else {
            if (((VisualXmasContact) first).getIOType() != IOType.OUTPUT) {
                throw new InvalidConnectionException("Connection is only allowed from output port.");
            }
            if (((VisualXmasContact) second).getIOType() != IOType.INPUT) {
                throw new InvalidConnectionException("Connection is only allowed to input port.");
            }
            for (Connection c: this.getConnections(first)) {
                if (c.getFirst() == first) {
                    throw new InvalidConnectionException("Only one connection is allowed from port.");
                }
            }
            for (Connection c: this.getConnections(second)) {
                if (c.getSecond() == second) {
                    throw new InvalidConnectionException("Only one connection is allowed to port.");
                }
            }
        }
    }

    public VisualXmas(Xmas model, VisualGroup root) {
        super(model, root);
        circuit = model;
    }

    public VisualXmas(Xmas model) throws VisualModelInstantiationException {
        super(model);
        circuit = model;
        try {
            createDefaultFlatStructure();
        } catch (NodeCreationException e) {
            throw new VisualModelInstantiationException(e);
        }
    }

    @Override
    public VisualConnection connect(Node first, Node second, MathConnection mConnection) throws InvalidConnectionException {
        validateConnection(first, second);
        VisualXmasConnection connection = null;
        if (first instanceof VisualComponent && second instanceof VisualComponent) {
            VisualComponent c1 = (VisualComponent) first;
            VisualComponent c2 = (VisualComponent) second;
            if (mConnection == null) {
                mConnection = circuit.connect(c1.getReferencedComponent(), c2.getReferencedComponent());
            }
            connection = new VisualXmasConnection(mConnection, c1, c2);
            Node parent = Hierarchy.getCommonParent(c1, c2);
            VisualGroup nearestAncestor = Hierarchy.getNearestAncestor(parent, VisualGroup.class);
            nearestAncestor.add(connection);
        }
        return connection;
    }

    public VisualGroup getGroup(VisualComponent vsc) {
        return Hierarchy.getNearestAncestor(vsc, VisualGroup.class);
    }

    public Collection<VisualXmasComponent> getNodes() {
        return Hierarchy.getDescendantsOfType(getRoot(), VisualXmasComponent.class);
    }
}
