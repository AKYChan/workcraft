package org.workcraft.formula.sat;

import org.workcraft.formula.BooleanVariable;

import java.util.Map;

public class CnfTask {

    private final String body;
    private final Map<String, BooleanVariable> vars;

    public CnfTask(String body, Map<String, BooleanVariable> vars) {
        this.body = body;
        this.vars = vars;
    }

    public String getBody() {
        return body;
    }
    public Map<String, BooleanVariable> getVars() {
        return vars;
    }

}
