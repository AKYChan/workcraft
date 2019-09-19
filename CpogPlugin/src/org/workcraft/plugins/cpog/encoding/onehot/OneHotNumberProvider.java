package org.workcraft.plugins.cpog.encoding.onehot;

import static org.workcraft.formula.BooleanOperations.and;
import static org.workcraft.formula.BooleanOperations.imply;
import static org.workcraft.formula.BooleanOperations.not;
import static org.workcraft.formula.BooleanOperations.or;

import java.util.ArrayList;
import java.util.List;

import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.BooleanVariable;
import org.workcraft.formula.FreeVariable;
import org.workcraft.formula.One;
import org.workcraft.plugins.cpog.encoding.NumberProvider;

public class OneHotNumberProvider implements NumberProvider<OneHotIntBooleanFormula> {

    private final List<BooleanFormula> rho = new ArrayList<>();

    @Override
    public OneHotIntBooleanFormula generate(String varPrefix, int range) {
        List<BooleanVariable> vars = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            vars.add(new FreeVariable(varPrefix + "sel" + i));
        }

        for (int i = 0; i < range; i++) {
            for (int j = i + 1; j < range; j++) {
                rho.add(or(not(vars.get(i)), not(vars.get(j))));
            }
        }
        rho.add(or(vars));
        return new OneHotIntBooleanFormula(vars);
    }

    @Override
    public BooleanFormula select(BooleanFormula[] booleanFormulas, OneHotIntBooleanFormula number) {
        if (number.getRange() != booleanFormulas.length) {
            throw new RuntimeException("Lengths do not match");
        }
        List<BooleanFormula> result = new ArrayList<>();
        for (int i = 0; i < booleanFormulas.length; i++) {
            result.add(imply(number.get(i), booleanFormulas[i]));
        }
        return and(result);
    }

    @Override
    public BooleanFormula getConstraints() {
        return and(rho);
    }

    @Override
    public BooleanFormula less(OneHotIntBooleanFormula a, OneHotIntBooleanFormula b) {
        return One.getInstance();
    }
}
