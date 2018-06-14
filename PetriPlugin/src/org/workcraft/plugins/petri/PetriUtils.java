package org.workcraft.plugins.petri;

import java.util.HashMap;
import java.util.HashSet;

import org.workcraft.dom.Node;
import org.workcraft.gui.graph.tools.Trace;
import org.workcraft.util.LogUtils;

public class PetriUtils {

    public static HashMap<Place, Integer> getMarking(PetriNetModel net) {
        HashMap<Place, Integer> marking = new HashMap<>();
        for (Place place: net.getPlaces()) {
            marking.put(place, place.getTokens());
        }
        return marking;
    }

    public static void setMarking(PetriNetModel net, HashMap<Place, Integer> marking) {
        for (Place place: net.getPlaces()) {
            Integer count = marking.get(place);
            if (count != null) {
                place.setTokens(count);
            }
        }
    }

    public static boolean fireTrace(PetriNetModel net, Trace trace) {
        for (String ref: trace) {
            Node node = net.getNodeByReference(ref);
            if (node instanceof Transition) {
                Transition transition = (Transition) node;
                if (net.isEnabled(transition)) {
                    net.fire(transition);
                } else {
                    LogUtils.logError("Trace transition '" + ref + "' is not enabled.");
                    return false;
                }
            } else {
                LogUtils.logError("Trace transition '" + ref + "' cannot be found.");
                return false;
            }
        }
        return true;
    }

    public static HashSet<Transition> getEnabledTransitions(PetriNetModel net) {
        HashSet<Transition> result = new HashSet<>();
        for (Transition transition: net.getTransitions()) {
            if (net.isEnabled(transition)) {
                result.add(transition);
            }
        }
        return result;
    }

}
