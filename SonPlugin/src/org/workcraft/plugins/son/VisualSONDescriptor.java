package org.workcraft.plugins.son;

import org.workcraft.util.ValidationUtils;
import org.workcraft.dom.VisualModelDescriptor;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.VisualModelInstantiationException;

public class VisualSONDescriptor implements VisualModelDescriptor {

    public VisualModel create(MathModel mathModel) throws VisualModelInstantiationException {
        ValidationUtils.validateMathModelType(mathModel, SON.class, VisualSON.class.getSimpleName());
        return new VisualSON((SON) mathModel);
    }

}
