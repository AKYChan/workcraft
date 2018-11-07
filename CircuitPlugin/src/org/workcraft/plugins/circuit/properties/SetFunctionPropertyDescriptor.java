package org.workcraft.plugins.circuit.properties;

import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.jj.ParseException;
import org.workcraft.formula.utils.StringGenerator;
import org.workcraft.gui.properties.PropertyDescriptor;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualFunctionContact;
import org.workcraft.plugins.circuit.utils.CircuitUtils;

import java.util.Map;

public class SetFunctionPropertyDescriptor implements PropertyDescriptor {

    private final VisualCircuit circuit;
    private final VisualFunctionContact contact;

    public SetFunctionPropertyDescriptor(VisualCircuit circuit, VisualFunctionContact contact) {
        this.circuit = circuit;
        this.contact = contact;
    }

    @Override
    public String getName() {
        return FunctionContact.PROPERTY_SET_FUNCTION;
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String) {
            try {
                BooleanFormula formula = CircuitUtils.parseContactFunction(circuit, contact, (String) value);
                contact.setSetFunction(formula);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Object getValue() {
        return StringGenerator.toString(contact.getSetFunction());
    }

    @Override
    public Map<Object, String> getChoice() {
        return null;
    }

    @Override
    public boolean isVisible() {
        return contact.isDriver();
    }

    @Override
    public boolean isCombinable() {
        return true;
    }

    @Override
    public boolean isTemplatable() {
        return false;
    }

}
