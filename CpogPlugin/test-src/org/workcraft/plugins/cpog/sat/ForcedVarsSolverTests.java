package org.workcraft.plugins.cpog.sat;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.utils.StringGenerator;
import org.workcraft.plugins.cpog.CpogSettings;
import org.workcraft.plugins.cpog.encoding.Encoding;
import org.workcraft.plugins.cpog.encoding.onehot.OneHotNumberProvider;

@Ignore // This only works with MINISAT solver which is not supported in Travis OSX
public class ForcedVarsSolverTests {

    private static final boolean DEBUG = false;
    private static final String[] smallTest1 = {"a"};
    private static final String[] smallTest2 = {"a", "b"};
    private static final String[] smallTest3 = {"a", "1"};
    private static final String[] smallTest4 = {"0", "a"};
    private static final String[] smallTest5 = {"0", "1", "a"};
    private static final String[] smallTest6 = {"0", "1", "a", "A"};

    private LegacySolver<BooleanFormula> createSolver() {
        return new LegacySolver<>(
                new Optimiser<>(new OneHotNumberProvider()), new CleverCnfGenerator());
    }

    private void testSolve(String[] scenarios, int free, int derived, boolean solutionExists) {
        Encoding solution = createSolver().solve(scenarios, free, derived);
        printSolution(solution);
        if (solutionExists) {
            Assert.assertNotNull(solution);
        } else {
            Assert.assertNull(solution);
        }
    }

    private void printSolution(Encoding solution) {
        if (!DEBUG) return;
        if (solution == null) {
            System.out.println("No solution.");
        } else {
            boolean[][] encoding = solution.getCode();
            for (int i = 0; i < encoding.length; i++) {
                for (int j = 0; j < encoding[i].length; j++) {
                    System.out.print(encoding[i][j] ? 1 : 0);
                }
                System.out.println();
            }

            System.out.println("Functions:");
            BooleanFormula[] functions = solution.getFormulas();
            for (int i = 0; i < functions.length; i++) {
                System.out.println(StringGenerator.toString(functions[i]));
            }
        }
    }

    @BeforeClass
    public static void setSatSolver() {
        CpogSettings.setSatSolver(CpogSettings.SatSolver.MINISAT);
    }

    @Test
    public void solveSmall1() {
        testSolve(smallTest1, 0, 0, true);
    }

    @Test
    public void solveSmall200Unsolvable() {
        testSolve(smallTest2, 0, 0, false);
    }

    @Test
    public void solveSmall212Unsolvable() {
        testSolve(smallTest2, 1, 2, false);
    }

    @Test
    public void solveSmall213Solvable() {
        testSolve(smallTest2, 1, 3, true);
    }

    @Test
    public void solveSmall311Solvable() {
        testSolve(smallTest3, 1, 1, true);
    }

    @Test
    public void solveSmall310Unsolvable() {
        testSolve(smallTest3, 1, 0, false);
    }

    @Test
    public void solveSmall410Unsolvable() {
        testSolve(smallTest4, 1, 0, false);
    }

    @Test
    public void solveSmall411Solvable() {
        testSolve(smallTest4, 1, 1, true);
    }

    @Test
    public void solveSmall514Unsolvable() {
        testSolve(smallTest5, 1, 4, false);
    }

    @Test
    public void solveSmall521Unsolvable() {
        testSolve(smallTest5, 2, 1, false);
    }

    @Test
    public void solveSmall522Solvable() {
        testSolve(smallTest5, 2, 2, true);
    }

    @Test
    public void solveSmall622Unsolvable() {
        testSolve(smallTest6, 2, 2, false);
    }

    @Test
    public void solveSmall623Solvable() {
        testSolve(smallTest6, 2, 3, true);
    }

}
