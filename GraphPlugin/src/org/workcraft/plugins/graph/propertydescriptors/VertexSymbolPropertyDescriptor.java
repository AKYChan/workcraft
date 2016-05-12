package org.workcraft.plugins.graph.propertydescriptors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.workcraft.dom.Node;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.graph.Graph;
import org.workcraft.plugins.graph.Symbol;
import org.workcraft.plugins.graph.Vertex;

public class VertexSymbolPropertyDescriptor implements PropertyDescriptor {
    private final Graph dg;
    private final Vertex vertex;

    public VertexSymbolPropertyDescriptor(Graph dg, Vertex vertex) {
        this.dg = dg;
        this.vertex = vertex;
    }

    @Override
    public Map<Object, String> getChoice() {
        return null;
    }

    @Override
    public String getName() {
        return Vertex.PROPERTY_SYMBOL;
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

    @Override
    public Object getValue() throws InvocationTargetException {
        Symbol symbol = vertex.getSymbol();
        String symbolName = "";
        if (symbol != null) {
            symbolName = dg.getName(symbol);
        }
        return symbolName;
    }

    @Override
    public void setValue(Object value) throws InvocationTargetException {
        Symbol symbol = null;
        String symbolName = (String) value;
        if (!symbolName.isEmpty()) {
            Node node = dg.getNodeByReference(symbolName);
            if (node instanceof Symbol) {
                symbol = (Symbol) node;
            } else {
                symbol = dg.createSymbol(symbolName);
            }
        }
        vertex.setSymbol(symbol);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isCombinable() {
        return true;
    }

    @Override
    public boolean isTemplatable() {
        return true;
    }
}
