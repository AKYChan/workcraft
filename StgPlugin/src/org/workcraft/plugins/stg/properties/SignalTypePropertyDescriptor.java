package org.workcraft.plugins.stg.properties;

import org.workcraft.dom.Container;
import org.workcraft.gui.properties.PropertyDescriptor;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.Stg;

import java.util.LinkedHashMap;
import java.util.Map;

public class SignalTypePropertyDescriptor implements PropertyDescriptor {
    private final Stg stg;
    private final String signal;
    private final Container container;

    public SignalTypePropertyDescriptor(Stg stg, String signal, Container container) {
        this.stg = stg;
        this.signal = signal;
        this.container = container;
    }

    @Override
    public Object getValue() {
        return stg.getSignalType(signal, container);
    }

    @Override
    public void setValue(Object value) {
        stg.setSignalType(signal, (Signal.Type) value, container);
    }

    @Override
    public Map<Object, String> getChoice() {
        LinkedHashMap<Object, String> result = new LinkedHashMap<>();
        for (Signal.Type type: Signal.Type.values()) {
            result.put(type, type.toString());
        }
        return result;
    }

    @Override
    public String getName() {
        return signal + " type";
    }

    @Override
    public Class<?> getType() {
        return int.class;
    }

}
