package org.workcraft.plugins.policy.properties;

import org.workcraft.gui.properties.PropertyDescriptor;
import org.workcraft.plugins.policy.VisualBundle;
import org.workcraft.plugins.policy.VisualPolicyNet;

import java.awt.*;
import java.util.Map;

public final class BundleColorPropertyDescriptor implements PropertyDescriptor {
    private final VisualPolicyNet model;
    private final VisualBundle bundle;

    public BundleColorPropertyDescriptor(VisualPolicyNet model, VisualBundle bundle) {
        this.model = model;
        this.bundle = bundle;
    }

    @Override
    public Map<Object, String> getChoice() {
        return null;
    }

    @Override
    public String getName() {
        return model.getPetriNet().getName(bundle.getReferencedBundle()) + " color";
    }

    @Override
    public Class<?> getType() {
        return Color.class;
    }

    @Override
    public Object getValue() {
        return bundle.getColor();
    }

    @Override
    public void setValue(Object value) {
        bundle.setColor((Color) value);
    }

}