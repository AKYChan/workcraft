package org.workcraft.plugins.cpog.encoding.twohot;

import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.Literal;
import org.workcraft.formula.cnf.Cnf;
import org.workcraft.formula.cnf.CnfClause;
import org.workcraft.plugins.cpog.encoding.CnfSorter;

import java.util.ArrayList;
import java.util.List;

import static org.workcraft.plugins.cpog.encoding.CnfOperations.not;
import static org.workcraft.plugins.cpog.encoding.CnfOperations.or;

public class TwoHotRangeProvider {

    private final Cnf constraints = new Cnf();

    public Cnf getConstraints() {
        return constraints;
    }

    public TwoHotRange generate(String name, int range) {
        if (range < 2) {
            throw new RuntimeException("can't select 2 hot out of " + range);
        }

        List<Literal> literals = createLiterals(name + "_sel", range);
        List<Literal> sort1 = createLiterals(name + "_sorta_", range);
        List<Literal> thermo = createLiterals(name + "_t_", range);
        List<Literal> sort2 = createLiterals(name + "_sortb_", range);

        constraints.add(CnfSorter.sortRound(sort1, thermo, literals));
        constraints.add(CnfSorter.sortRound(sort2, sort1));

        for (int i = 0; i < range - 2; i++) {
            constraints.addClauses(or(not(sort2.get(i))));
        }

        for (int i = 0; i < range - 2; i += 2) {
            constraints.addClauses(or(not(literals.get(i)), not(literals.get(i + 1))));
        }

        constraints.addClauses(or(sort2.get(range - 1)));
        constraints.addClauses(or(sort2.get(range - 2)));

        return new TwoHotRange(literals, thermo);
    }

    private List<Literal> createLiterals(String name, int range) {
        List<Literal> literals = new ArrayList<>();

        for (int i = 0; i < range; i++) {
            literals.add(new Literal(name + i));
        }
        return literals;
    }

    public static List<CnfClause> selectAnd(Literal literal, Literal[] vars, TwoHotRange code) {
        if (code.size() != vars.length) {
            throw new RuntimeException("Lengths do not match: code=" + code.size() + ", vars=" + vars.length);
        }

        List<Literal> preResult = new ArrayList<>();
        for (int i = 0; i < vars.length; i++) {
            preResult.add(new Literal(literal.getVariable().getLabel() + (literal.getNegation() ? "i" : "") + "_sv" + i));
        }

        List<CnfClause> result = new ArrayList<>();
        for (int i = 0; i < vars.length; i++) {
            Literal res = preResult.get(i);
            Literal sel = code.get(i);
            Literal var = vars[i];
            result.add(or(not(res), not(sel), var));
            result.add(or(res, sel));
            result.add(or(res, not(var)));
            result.add(or(not(literal), res));
        }

        CnfClause resTrue = new CnfClause();
        resTrue.add(literal);
        for (int i = 0; i < vars.length; i++) {
            resTrue.add(not(preResult.get(i)));
        }
        result.add(resTrue);
        return result;
    }

    public static BooleanFormula selectAnd(BooleanFormula[] vars, TwoHotRange number) {
        throw new RuntimeException("incorrect");
    }

}
