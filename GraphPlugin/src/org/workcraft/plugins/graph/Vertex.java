package org.workcraft.plugins.graph;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.IdentifierPrefix;
import org.workcraft.annotations.VisualClass;
import org.workcraft.dom.math.MathNode;
import org.workcraft.observation.PropertyChangedEvent;

@DisplayName("Vertex")
@IdentifierPrefix("v")
@VisualClass(VisualVertex.class)
public class Vertex extends MathNode {

    public static final String PROPERTY_SYMBOL = "Symbol";

    private Symbol symbol;

    public Vertex() {
    }

    public Vertex(Symbol symbol) {
        super();
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol value) {
        if (symbol != value) {
            symbol = value;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_SYMBOL));
        }
    }

}
