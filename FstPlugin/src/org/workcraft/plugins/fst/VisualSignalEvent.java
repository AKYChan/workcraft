package org.workcraft.plugins.fst;

import java.awt.Color;

import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.Stylable;
import org.workcraft.plugins.fsm.VisualEvent;
import org.workcraft.plugins.fsm.VisualState;
import org.workcraft.plugins.fst.SignalEvent.Direction;

public class VisualSignalEvent extends VisualEvent {

    public VisualSignalEvent() {
        this(null, null, null);
    }

    public VisualSignalEvent(SignalEvent mathConnection) {
        this(mathConnection, null, null);
    }

    public VisualSignalEvent(SignalEvent mathConnection, VisualState first, VisualState second) {
        super(mathConnection, first, second);
        removePropertyDeclarationByName("Label color");
    }

    @Override
    public Color getLabelColor() {
        Signal signal = getReferencedSignalEvent().getSignal();
        if (signal != null) {
            switch (signal.getType()) {
            case INPUT: return FstSettings.getInputColor();
            case OUTPUT: return FstSettings.getOutputColor();
            case INTERNAL: return FstSettings.getInternalColor();
            case DUMMY: return FstSettings.getDummyColor();
            }
        }
        return Color.BLACK;
    }

    @Override
    public String getLabel(DrawRequest r) {
        String result = super.getLabel(r);
        if (getReferencedSignalEvent().getSignal().hasDirection()) {
            if (FstSettings.getShowToggle() || (getReferencedSignalEvent().getDirection() != Direction.TOGGLE)) {
                result += getReferencedSignalEvent().getDirection();
            }
        }
        return result;
    }

    public SignalEvent getReferencedSignalEvent() {
        return (SignalEvent)getReferencedEvent();
    }


    @Override
    public void copyStyle(Stylable src) {
        super.copyStyle(src);
        if (src instanceof VisualSignalEvent) {
            VisualSignalEvent srcSignalEvent = (VisualSignalEvent)src;
            getReferencedSignalEvent().setDirection(srcSignalEvent.getReferencedSignalEvent().getDirection());
        }
    }

}
