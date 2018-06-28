package org.workcraft.plugins.atacs.commands;

public class AtacsGeneralisedCelementSynthesisCommand extends AtacsAbstractSynthesisCommand {

    @Override
    public String[] getSynthesisParameter() {
        return new String[] {"-og"};
    }

    @Override
    public String getDisplayName() {
        return "Generalised C-element [ATACS]";
    }

    @Override
    public Position getPosition() {
        return Position.MIDDLE;
    }

    @Override
    public boolean boxSequentialComponents() {
        return true;
    }

    @Override
    public boolean boxCombinationalComponents() {
        return true;
    }

    @Override
    public boolean sequentialAssign() {
        return true;
    }

}
