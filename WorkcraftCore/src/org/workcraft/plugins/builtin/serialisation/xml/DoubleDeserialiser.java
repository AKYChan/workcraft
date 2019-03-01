package org.workcraft.plugins.builtin.serialisation.xml;

import org.w3c.dom.Element;
import org.workcraft.serialisation.BasicXMLDeserialiser;

public class DoubleDeserialiser implements BasicXMLDeserialiser<Double> {

    @Override
    public String getClassName() {
        return double.class.getName();
    }

    @Override
    public Double deserialise(Element element) {
        return DoubleSerialiser.doubleFromString(element.getAttribute("value"));
    }

}
