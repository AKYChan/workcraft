package org.workcraft.plugins.wtg;

import org.workcraft.annotations.CustomTools;
import org.workcraft.annotations.DisplayName;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.dtd.Signal;
import org.workcraft.plugins.dtd.VisualDtd;
import org.workcraft.plugins.dtd.VisualSignal;
import org.workcraft.plugins.dtd.VisualTransition;

@DisplayName("Waveform Transition Graph")
@CustomTools(WtgToolsProvider.class)
public class VisualWtg extends VisualDtd {

    public VisualWtg(Wtg model) {
        this(model, null);
    }

    public VisualWtg(Wtg model, VisualGroup root) {
        super(model, root);
    }

    @Override
    public void validateConnection(Node first, Node second) throws InvalidConnectionException {
        if (first == second) {
            throw new InvalidConnectionException("Self-loops are not allowed.");
        }

        if (getConnection(first, second) != null) {
            throw new InvalidConnectionException("Connection already exists.");
        }

        if ((first instanceof VisualWaveform) && (second instanceof VisualWaveform)) {
            throw new InvalidConnectionException("Cannot directly connect waveforms.");
        }

        if ((first instanceof VisualState) && (second instanceof VisualState)) {
            throw new InvalidConnectionException("Cannot directly connect states.");
        }

        if ((first instanceof VisualState) && (second instanceof VisualWaveform)) {
            if (!getPreset(second).isEmpty()) {
                throw new InvalidConnectionException("Waveform cannot have more than one preceding state.");
            }
            return;
        }

        if ((first instanceof VisualWaveform) && (second instanceof VisualState)) {
            if (!getPostset(first).isEmpty()) {
                throw new InvalidConnectionException("Waveform cannot have more than one succeeding state.");
            }
            return;
        }

        if ((first instanceof VisualTransition) && (second instanceof VisualTransition)) {
            Node firstWaveform = first.getParent();
            Node secondWaveform = second.getParent();
            if (firstWaveform != secondWaveform) {
                throw new InvalidConnectionException("Cannot connect events from different waveforms.");
            }
            VisualTransition firstTransition = (VisualTransition) first;
            VisualTransition secondTransition = (VisualTransition) second;
            if (firstTransition.getX() > secondTransition.getX()) {
                throw new InvalidConnectionException("Invalid order of events.");
            }
            if ((firstTransition.getSignal() == secondTransition.getSignal())
                    && (firstTransition.getDirection() == secondTransition.getDirection())) {
                throw new InvalidConnectionException("Cannot order transitions of the same signal and direction.");
            }
            return;
        }

        if ((first instanceof VisualSignal) && (second instanceof VisualTransition)) {
            Signal firstSignal = ((VisualSignal) first).getReferencedSignal();
            Signal secondSignal = ((VisualTransition) second).getSignal();
            if (firstSignal != secondSignal) {
                throw new InvalidConnectionException("Cannot relate transition with a different signal.");
            }
            return;
        }
        throw new InvalidConnectionException("Invalid connection.");
    }

}
