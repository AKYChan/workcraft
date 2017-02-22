package org.workcraft.plugins.dtd;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.VisualClass;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.graph.Vertex;

@DisplayName("Transition")
@VisualClass(org.workcraft.plugins.dtd.VisualTransition.class)
public class Transition extends Vertex {

    public static final String PROPERTY_DIRECTION = "Direction";

    public enum Direction {
        RISE("+", "rise"),
        FALL("-", "fall"),
        DESTABILISE("*", "destabilise"),
        STABILISE("?", "stabilise");

        private final String symbol;
        private final String description;

        Direction(String name, String description) {
            this.symbol = name;
            this.description = description;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Direction fromSymbol(String symbol) {
            for (Direction item : Direction.values()) {
                if ((symbol != null) && (symbol.equals(item.symbol))) {
                    return item;
                }
            }
            throw new ArgumentException("Unexpected string: " + symbol);
        }

        @Override
        public String toString() {
            return symbol + " (" + description + ")";
        }

        public Direction reverse() {
            switch (this) {
            case RISE: return FALL;
            case FALL: return RISE;
            case DESTABILISE: return STABILISE;
            case STABILISE: return DESTABILISE;
            }
            return null;
        }
    }

    private Direction direction = Direction.RISE;

    public Transition() {
    }

    public Transition(Signal signal) {
        super(signal);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_DIRECTION));
        }
    }

    public Signal getSignal() {
        return (Signal) getSymbol();
    }

}
