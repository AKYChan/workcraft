package org.workcraft.formula.sat;

import org.junit.BeforeClass;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.encoding.BinaryNumberProvider;
import org.workcraft.plugins.builtin.settings.CommonSatSettings;
import org.workcraft.plugins.builtin.settings.CommonSatSettings.SatSolver;

public class BinarySolverTests extends SolverTests {

    @BeforeClass
    public static void setSatSolver() {
        CommonSatSettings.setSatSolver(SatSolver.CLASP);
    }

    @Override
    protected LegacySolver<BooleanFormula> createSolver() {
        return new LegacySolver<>(
                new Optimiser<>(new BinaryNumberProvider()),
                new CleverCnfGenerator());
    }

}
