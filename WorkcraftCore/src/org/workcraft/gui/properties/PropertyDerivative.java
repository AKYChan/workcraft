package org.workcraft.gui.properties;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class PropertyDerivative implements PropertyDescriptor {

    final PropertyDescriptor descriptor;

    public PropertyDerivative(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public Class<?> getType() {
        return descriptor.getType();
    }

    @Override
    public Object getValue() throws InvocationTargetException {
        return descriptor.getValue();
    }

    @Override
    public void setValue(Object value) throws InvocationTargetException {
        descriptor.setValue(value);
    }

    @Override
    public Map<? extends Object, String> getChoice() {
        return descriptor.getChoice();
    }

    @Override
    public boolean isEditable() {
        return descriptor.isEditable();
    }

    @Override
    public boolean isVisible() {
        return descriptor.isVisible();
    }

    @Override
    public boolean isCombinable() {
        return descriptor.isCombinable();
    }

    @Override
    public boolean isTemplatable() {
        return descriptor.isTemplatable();
    }

}
