package org.workcraft.plugins.mpsat;

public enum SynthesisMode {
    COMPLEX_GATE_IMPLEMENTATION("-E -!", "Complex-gate synthesis", false),
    GENERALISED_CELEMENT_IMPLEMENTATION("-G -!", "Generalised C-element synthesis", false),
    STANDARD_CELEMENT_IMPLEMENTATION("-S -!", "Standard C-element synthesis", false),
    TECH_MAPPING("-T -f -p2 -cl -!", "Technology mapping", true);

    private final String argument;
    private final String description;
    private final boolean needLib;

    SynthesisMode(String argument, String description, boolean needLib) {
        this.argument = argument;
        this.description = description;
        this.needLib = needLib;
    }

    public static SynthesisMode getModeByArgument(String arg) {
        SynthesisMode result = null;
        for (SynthesisMode mode: SynthesisMode.values()) {
            String modeArg = mode.getArgument();
            if ((modeArg != null) && modeArg.equals(arg)) {
                result = mode;
                break;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return description;
    }

    public String getArgument() {
        return argument;
    }

    public boolean needLib() {
        return needLib;
    }

}
