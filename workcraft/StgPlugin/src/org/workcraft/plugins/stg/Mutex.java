package org.workcraft.plugins.stg;

public class Mutex {

    public enum Protocol {
        STRICT("Strict (forbid two grants)"),
        RELAXED("Relaxed (allow two grants on reset)");

        private final String name;

        Protocol(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final String name;
    public final Signal r1;
    public final Signal g1;
    public final Signal r2;
    public final Signal g2;

    public Mutex(String name, Signal r1, Signal g1, Signal r2, Signal g2) {
        this.name = name;
        this.r1 = r1;
        this.g1 = g1;
        this.r2 = r2;
        this.g2 = g2;
    }

    @Override
    public String toString() {
        return "MUTEX " + name + " (.r1(" + r1.name + "), .g1(" + g1.name + "), .r2(" + r2.name + "), .g2(" + g2.name + "))";
    }

}
