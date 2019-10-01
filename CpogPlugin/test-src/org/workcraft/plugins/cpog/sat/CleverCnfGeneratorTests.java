package org.workcraft.plugins.cpog.sat;

import org.junit.Assert;
import org.junit.Test;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.BooleanVariable;
import org.workcraft.formula.FreeVariable;
import org.workcraft.formula.cnf.Cnf;
import org.workcraft.formula.ClauseUtils;
import org.workcraft.formula.workers.BooleanWorker;
import org.workcraft.formula.workers.MemoryConservingBooleanWorker;
import org.workcraft.formula.workers.PrettifyBooleanWorker;
import org.workcraft.utils.SetUtils;

import java.util.Set;

public class CleverCnfGeneratorTests {

    private static final BooleanWorker WORKER = new PrettifyBooleanWorker(new MemoryConservingBooleanWorker());

    @Test
    public void testSimpleCnfPrinter() {
        CleverCnfGenerator generator = new CleverCnfGenerator();
        BooleanVariable a = new FreeVariable("a");
        BooleanVariable b = new FreeVariable("b");
        // f = a * b + b * a + a * b
        BooleanFormula f = WORKER.or(WORKER.or(WORKER.and(a, b), WORKER.and(b, a)), WORKER.and(a, b));
        Cnf cnf = generator.generate(f);
        Set<Set<String>> expected = SetUtils.convertArraysToSets(new String[][]{{"a"}, {"b"}});
        Assert.assertEquals(expected, ClauseUtils.getLiteralSets(cnf));

    }

}
