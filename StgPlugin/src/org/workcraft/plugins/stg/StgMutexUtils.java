package org.workcraft.plugins.stg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.workcraft.dom.Node;
import org.workcraft.plugins.stg.SignalTransition.Type;

public class StgMutexUtils {

    public static class MutexData {
        public String me;
        public String r1;
        public String g1;
        public String r2;
        public String g2;

        @Override
        public String toString() {
            return me + " (.r1(" + r1 + "), .g1(" + g1 + "), .r2(" + r2 + "), .g2(" + g2 + "))";
        }
    }

    public static boolean fillMutexContext(Stg stg, StgPlace place, MutexData me) {
        if (!place.isMutex()) {
            return false;
        }
        me.me = stg.getNodeReference(place);
        Set<Node> preset = stg.getPreset(place);
        Set<Node> postset = stg.getPostset(place);
        if ((preset.size() != 2) || (postset.size() != 2)) {
            return false;
        }
        Iterator<Node> postsetIterator = postset.iterator();
        Node succ1 = postsetIterator.next();
        Node succ2 = postsetIterator.next();
        if ((succ1 instanceof SignalTransition) && (succ2 instanceof SignalTransition)) {
            SignalTransition tSucc1 = (SignalTransition) succ1;
            SignalTransition tSucc2 = (SignalTransition) succ2;
            if ((tSucc1.getSignalType() == Type.INPUT) || (tSucc2.getSignalType() == Type.INPUT)) {
                return false;
            }
            me.g1 = tSucc1.getSignalName();
            me.g2 = tSucc2.getSignalName();
            Set<SignalTransition> triggers1 = getTriggers(stg, tSucc1, place);
            Set<SignalTransition> triggers2 = getTriggers(stg, tSucc2, place);
            if ((triggers1.size() != 1) || (triggers2.size() != 1)) {
                return false;
            }
            SignalTransition trigger1 = triggers1.iterator().next();
            SignalTransition trigger2 = triggers2.iterator().next();
            me.r1 = trigger1.getSignalName();
            me.r2 = trigger2.getSignalName();
        }
        return true;
    }

    private static Set<SignalTransition> getTriggers(Stg stg, SignalTransition transition, StgPlace skipPlace) {
        HashSet<SignalTransition> result = new HashSet<>();
        for (Node predPlace: stg.getPreset(transition)) {
            if ((predPlace instanceof StgPlace) && (predPlace != skipPlace)) {
                for (Node predTransition: stg.getPreset(predPlace)) {
                    if (predTransition instanceof SignalTransition) {
                        result.add((SignalTransition) predTransition);
                    }
                }
            }
        }
        return result;
    }

}
