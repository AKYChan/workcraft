package org.workcraft.plugins.stg;

import java.util.Collection;
import java.util.Set;

import org.workcraft.dom.Node;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.stg.SignalTransition.Type;

public interface StgModel extends PetriNetModel {
    Collection<DummyTransition> getDummyTransitions();
    Collection<SignalTransition> getSignalTransitions();
    Collection<SignalTransition> getSignalTransitions(Type type);
    Collection<StgPlace> getMutexPlaces();

    Set<String> getDummyReferences();
    Set<String> getSignalReferences();
    Set<String> getSignalReferences(Type type);
    int getInstanceNumber(Node st);
    void setInstanceNumber(Node st, int number);

}
