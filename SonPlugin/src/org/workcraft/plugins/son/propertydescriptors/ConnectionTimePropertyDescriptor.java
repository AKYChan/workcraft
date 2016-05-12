package org.workcraft.plugins.son.propertydescriptors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.son.connections.SONConnection;
import org.workcraft.plugins.son.util.Interval;

public class ConnectionTimePropertyDescriptor implements PropertyDescriptor {
    private final SONConnection con;
    public static final String PROPERTY_CONNECTION_TIME = "Time interval";

    public ConnectionTimePropertyDescriptor(SONConnection con) {
        this.con = con;
    }

    @Override
    public String getName() {
        return PROPERTY_CONNECTION_TIME;
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isCombinable() {
        return false;
    }

    @Override
    public boolean isTemplatable() {
        return false;
    }

    @Override
    public Object getValue() throws InvocationTargetException {
        Interval value = con.getTime();
        return value.toString();
    }

    @Override
    public void setValue(Object value) throws InvocationTargetException {
        String input = (String) value;
        Interval result = new Interval(Interval.getMin(input), Interval.getMax(input));
        con.setTime(result);
    }

    @Override
    public Map<? extends Object, String> getChoice() {
        return null;
    }

}
