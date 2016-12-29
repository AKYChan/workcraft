package org.workcraft.plugins.petri.dom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.workcraft.dom.Connection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.petri.PetriNet;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;

public class DOMTests {

    @Test
    public void test1() throws InvalidConnectionException {
        PetriNet petri = new PetriNet();

        Place p1 = new Place();
        Place p2 = new Place();
        Transition t1 = new Transition();

        petri.add(p1);
        petri.add(p2);
        petri.add(t1);
        Connection con1 = petri.connect(p1, t1);
        Connection con2 = petri.connect(t1, p2);

        assertSame(p1, petri.getNodeByReference(petri.getNodeReference(p1)));
        assertSame(p2, petri.getNodeByReference(petri.getNodeReference(p2)));

        assertTrue(petri.getPreset(p2).contains(t1));
        assertTrue(petri.getPostset(p1).contains(t1));

        assertTrue(petri.getConnections(p1).contains(con1));

        petri.remove(p1);

        assertTrue(petri.getConnections(t1).contains(con2));
        assertFalse(petri.getConnections(t1).contains(con1));

        boolean thrown = true;
        try {
            petri.getNodeReference(null);
            thrown = false;
        } catch (Throwable th) { }

        assertTrue(thrown);
    }

}
