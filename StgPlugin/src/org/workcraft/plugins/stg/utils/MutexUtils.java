package org.workcraft.plugins.stg.utils;

import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.dom.visual.SizeHelper;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.stg.*;
import org.workcraft.types.Pair;
import org.workcraft.utils.DialogUtils;
import org.workcraft.utils.LogUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MutexUtils {

    public static LinkedList<Pair<String, String>> getMutexGrantPairs(Stg stg) {
        LinkedList<Pair<String, String>> exceptions = new LinkedList<>();
        for (Mutex mutex: getMutexes(stg)) {
            Pair<String, String> exception = Pair.of(mutex.g1.name, mutex.g2.name);
            exceptions.add(exception);
        }
        return exceptions;
    }

    public static LinkedList<Mutex> getImplementableMutexes(Stg stg) {
        LinkedList<Mutex> result = new LinkedList<>();
        final ArrayList<StgPlace> problematicPlaces = new ArrayList<>();
        for (StgPlace place: stg.getMutexPlaces()) {
            Mutex mutex = getMutex(stg, place);
            if (mutex != null) {
                result.add(mutex);
            } else {
                problematicPlaces.add(place);
            }
        }
        if (!problematicPlaces.isEmpty()) {
            String problematicPlacesString = ReferenceHelper.getNodesAsString(stg, problematicPlaces, SizeHelper.getWrapLength());
            String msg = "The following mutex places may not be implementable by mutex:\n\n" +
                    problematicPlacesString + "\n\nProceed synthesis without these places anyways?";
            if (!DialogUtils.showConfirmError(msg, false)) {
                result = null;
            }
        }
        return result;
    }

    public static LinkedList<Mutex> getMutexes(Stg stg) {
        LinkedList<Mutex> result = new LinkedList<>();
        for (StgPlace place: stg.getMutexPlaces()) {
            Mutex mutex = getMutex(stg, place);
            if (mutex != null) {
                result.add(mutex);
            }
        }
        return result;
    }

    public static Mutex getMutex(Stg stg, StgPlace place) {
        Set<MathNode> preset = stg.getPreset(place);
        Set<MathNode> postset = stg.getPostset(place);
        if ((preset.size() != 2) || (postset.size() != 2)) {
            return null;
        }
        Iterator<MathNode> postsetIterator = postset.iterator();
        Node succ1 = postsetIterator.next();
        Node succ2 = postsetIterator.next();
        if (!(succ1 instanceof SignalTransition) || !(succ2 instanceof SignalTransition)) {
            return null;
        }
        SignalTransition tSucc1 = (SignalTransition) succ1;
        SignalTransition tSucc2 = (SignalTransition) succ2;
        if ((tSucc1.getSignalType() == Signal.Type.INPUT) || (tSucc2.getSignalType() == Signal.Type.INPUT)) {
            return null;
        }
        Set<SignalTransition> triggers1 = getTriggers(stg, tSucc1, place);
        Set<SignalTransition> triggers2 = getTriggers(stg, tSucc2, place);
        if ((triggers1.size() != 1) || (triggers2.size() != 1)) {
            return null;
        }
        SignalTransition trigger1 = triggers1.iterator().next();
        SignalTransition trigger2 = triggers2.iterator().next();

        String name = stg.getNodeReference(place);
        Signal g1 = createSignalFromTransition(stg, tSucc1);
        Signal g2 = createSignalFromTransition(stg, tSucc2);
        Signal r1 = createSignalFromTransition(stg, trigger1);
        Signal r2 = createSignalFromTransition(stg, trigger2);
        return new Mutex(name, r1, g1, r2, g2);
    }

    private static Signal createSignalFromTransition(Stg stg, SignalTransition transition) {
        return new Signal(stg.getSignalReference(transition), transition.getSignalType());
    }

    private static Set<SignalTransition> getTriggers(Stg stg, SignalTransition transition, StgPlace skipPlace) {
        HashSet<SignalTransition> result = new HashSet<>();
        for (MathNode predPlace: stg.getPreset(transition)) {
            if (!(predPlace instanceof StgPlace) || (predPlace == skipPlace)) {
                continue;
            }
            for (MathNode predTransition: stg.getPreset(predPlace)) {
                if (!(predTransition instanceof SignalTransition) || stg.getPreset(predTransition).contains(predPlace)) {
                    continue;
                }
                result.add((SignalTransition) predTransition);
            }
        }
        return result;
    }

    public static void factoroutMutexs(StgModel model, Collection<Mutex> mutexes) {
        if ((model instanceof Stg) && (mutexes != null)) {
            Stg stg = (Stg) model;
            for (Mutex mutex: mutexes) {
                LogUtils.logInfo("Factoring out " + mutex);
                factoreoutMutexRequest(stg, mutex.r1);
                factoreoutMutexGrant(stg, mutex.g1);
                factoreoutMutexRequest(stg, mutex.r2);
                factoreoutMutexGrant(stg, mutex.g2);
            }
        }
    }

    private static void factoreoutMutexRequest(Stg stg, Signal signal) {
        if (signal.type == Signal.Type.INTERNAL) {
            stg.setSignalType(signal.name, Signal.Type.OUTPUT);
        }
    }

    private static void factoreoutMutexGrant(Stg stg, Signal signal) {
        stg.setSignalType(signal.name, Signal.Type.INPUT);
    }

    public static void restoreMutexSignals(StgModel model, Collection<Mutex> mutexes) {
        if ((model instanceof Stg) && (mutexes != null)) {
            Stg stg = (Stg) model;
            for (Mutex mutex : mutexes) {
                stg.setSignalType(mutex.r1.name, mutex.r1.type);
                stg.setSignalType(mutex.g1.name, mutex.g1.type);
                stg.setSignalType(mutex.r2.name, mutex.r2.type);
                stg.setSignalType(mutex.g2.name, mutex.g2.type);
            }
        }
    }

    public static void restoreMutexPlacesByName(StgModel model, Collection<Mutex> mutexes) {
        if ((model != null) && (mutexes != null)) {
            for (Mutex mutex : mutexes) {
                Node node = model.getNodeByReference(mutex.name);
                if (node instanceof StgPlace) {
                    StgPlace place = (StgPlace) node;
                    place.setMutex(true);
                }
            }
        }
    }

    public static void restoreMutexPlacesByContext(StgModel model, Collection<Mutex> mutexes) {
        if ((model instanceof Stg) && (mutexes != null)) {
            Stg stg = (Stg) model;
            for (Mutex mutex : mutexes) {
                restoreMutexPlaceByContext(stg, mutex);
            }
        }
    }

    private static void restoreMutexPlaceByContext(Stg stg, Mutex mutex) {
        if ((stg == null) || (mutex == null)) {
            return;
        }
        for (Place place: stg.getPlaces()) {
            if (place instanceof StgPlace) {
                StgPlace stgPlace = (StgPlace) place;
                Mutex newMutex = MutexUtils.getMutex(stg, stgPlace);
                if (newMutex == null) {
                    continue;
                }
                boolean r1r1 = mutex.r1.equals(newMutex.r1);
                boolean g1g1 = mutex.g1.equals(newMutex.g1);
                boolean r2r2 = mutex.r2.equals(newMutex.r2);
                boolean g2g2 = mutex.g2.equals(newMutex.g2);
                boolean r1r2 = mutex.r1.equals(newMutex.r2);
                boolean g1g2 = mutex.g1.equals(newMutex.g2);
                boolean r2r1 = mutex.r2.equals(newMutex.r1);
                boolean g2g1 = mutex.g2.equals(newMutex.g1);
                boolean match11 = r1r1 && g1g1 && r2r2 && g2g2;
                boolean match21 = r1r2 && g1g2 && r2r1 && g2g1;
                if (match11 || match21) {
                    stgPlace.setMutex(true);
                }
            }
        }
    }

    public static void logInfoPossiblyImplementableMutex(Collection<Mutex> mutexes) {
        if ((mutexes == null) || mutexes.isEmpty()) {
            return;
        }
        String s = mutexes.stream().map(mutex -> mutex.name).collect(Collectors.joining(","));
        LogUtils.logInfo("Possibly implementable (structurally detected) mutex places: " + s);
    }

    public static Set<String> getMutexPlaceReferences(Stg stg) {
        HashSet<String> result = new HashSet<>();
        for (StgPlace place: stg.getMutexPlaces()) {
            result.add(stg.getNodeReference(place));
        }
        return result;
    }

}
