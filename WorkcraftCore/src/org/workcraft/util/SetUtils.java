package org.workcraft.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {

    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<T>();
        if ((set1 != null) && (set2 != null)) {
            for (T o : set1) {
                if (set2.contains(o)) {
                    result.add(o);
                }
            }
        }
        return result;
    }

    public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<T>();
        if (set1 != null) {
            result.addAll(set1);
        }
        if (set2 != null) {
            result.addAll(set2);
        }
        return result;
    }

    public static <T> Set<T> symmetricDifference(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<T>();
        Set<T> tmp = new HashSet<T>();
        if (set1 != null) {
            result.addAll(set1);
            tmp.addAll(set1);
        }
        if (set2 != null) {
            result.addAll(set2);
            tmp.retainAll(set2);
        }
        result.removeAll(tmp);
        return result;

    }

}
