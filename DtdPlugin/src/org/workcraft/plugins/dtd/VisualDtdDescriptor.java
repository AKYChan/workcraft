package org.workcraft.plugins.dtd;

import org.workcraft.util.ValidationUtils;
import org.workcraft.dom.VisualModelDescriptor;
import org.workcraft.dom.math.MathModel;
import org.workcraft.exceptions.VisualModelInstantiationException;

public class VisualDtdDescriptor implements VisualModelDescriptor {

    @Override
    public VisualDtd create(MathModel mathModel) throws VisualModelInstantiationException {
        ValidationUtils.validateMathModelType(mathModel, Dtd.class, VisualDtd.class.getSimpleName());
        return new VisualDtd((Dtd) mathModel);
    }

}
