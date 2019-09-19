package org.workcraft.plugins.cpog.sat;

import org.junit.*;
import org.workcraft.plugins.cpog.CpogSettings;
import org.workcraft.plugins.cpog.encoding.Encoding;
import org.workcraft.utils.DesktopApi;

public abstract class SolverTests {

    private static final String[] smallScenarios = {"110", "101", "011"};
    private static final String[] xorScenarios = {"000", "011", "101", "110"};

    protected abstract LegacySolver<?> createSolver();

    protected LegacySolver<?> createSolver(int[] levels) {
        return null;
    }

    @BeforeClass
    public static void skipOnWindows() {
        Assume.assumeFalse(DesktopApi.getOs().isWindows());
    }

    @BeforeClass
    public static void setSatSolver() {
        CpogSettings.setSatSolver(CpogSettings.SatSolver.MINISAT);
    }

    @Test
    public void testSmall10() {
        Encoding solution = createSolver().solve(smallScenarios, 1, 0);
        Assert.assertNull(solution);
    }

    @Ignore @Test
    public void testSmall20() {
        Encoding solution = createSolver().solve(smallScenarios, 2, 0);
        Assert.assertNull(solution);
    }

    @Test
    public void testSmall21() {
        Encoding solution = createSolver().solve(smallScenarios, 2, 1);
        Assert.assertNotNull(solution);
    }

    @Test
    public void testSmall22() {
        Encoding solution = createSolver().solve(smallScenarios, 2, 2);
        Assert.assertNotNull(solution);
    }

    @Test
    public void testSmall23() {
        Encoding solution = createSolver().solve(smallScenarios, 2, 3);
        Assert.assertNotNull(solution);
    }

    @Test
    public void testSmall30() {
        Encoding solution = createSolver().solve(smallScenarios, 3, 0);
        Assert.assertNotNull(solution);
    }

    @Test
    public void testXor() {
        Encoding solution = createSolver().solve(xorScenarios, 2, 1);
        Assert.assertNull(solution);
    }

}
