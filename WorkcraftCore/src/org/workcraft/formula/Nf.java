package org.workcraft.formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Nf<C> implements BooleanFormula {

    private List<C> clauses = new ArrayList<>();

    public Nf() {
    }

    public Nf(C... clauses) {
        this(Arrays.asList(clauses));
    }

    public Nf(List<C> clauses) {
        this.clauses = new ArrayList<>(clauses);
    }

    public void setClauses(List<C> clauses) {
        this.clauses = new ArrayList<>(clauses);
    }

    public List<C> getClauses() {
        return clauses;
    }

    private void addClauses(List<C> list) {
        clauses.addAll(list);
    }

    public void addClauses(C... arr) {
        addClauses(Arrays.asList(arr));
    }

    public void add(Nf<C> nf) {
        addClauses(nf.getClauses());
    }

    public void addAll(List<? extends Nf<C>> nfs) {
        for (Nf<C> nf : nfs) {
            add(nf);
        }
    }

    public abstract <T> T accept(BooleanVisitor<T> visitor);

}
