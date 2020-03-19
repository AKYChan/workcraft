package org.workcraft.plugins.cpog.formula;

import org.workcraft.formula.BooleanFormula;

public interface ReducedBooleanWorker {
    BooleanFormula and(BooleanFormula x, BooleanFormula y);
    BooleanFormula not(BooleanFormula x);
    BooleanFormula iff(BooleanFormula x, BooleanFormula y);
}
