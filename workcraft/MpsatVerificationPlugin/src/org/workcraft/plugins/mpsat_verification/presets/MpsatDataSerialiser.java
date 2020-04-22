package org.workcraft.plugins.mpsat_verification.presets;

import org.w3c.dom.Element;
import org.workcraft.plugins.mpsat_verification.MpsatVerificationSettings;
import org.workcraft.plugins.mpsat_verification.presets.VerificationParameters.SolutionMode;
import org.workcraft.presets.DataSerialiser;
import org.workcraft.utils.XmlUtils;

public class MpsatDataSerialiser implements DataSerialiser<VerificationParameters> {

    private static final String SETTINGS_ELEMENT = "settings";
    private static final String SETTINGS_DESCRIPTION_ATTRIBUTE = "description";
    private static final String SETTINGS_MODE_ATTRIBUTE = "mode";
    private static final String SETTINGS_VERBOSITY_ATTRIBUTE = "verbosity";
    private static final String SETTINGS_SOLUTION_LIMIT_ATTRIBUTE = "solutionNumberLimit";
    private static final String SETTINGS_SOLUTION_MODE_ATTRIBUTE = "solutionMode";
    private static final String SETTINGS_REACH_ELEMENT = "reach";
    private static final String SETTINGS_INVERSE_PREDICATE_ATTRIBUTE = "inversePredicate";

    @Override
    public VerificationParameters fromXML(Element parent) {
        Element element = XmlUtils.getChildElement(SETTINGS_ELEMENT, parent);

        String description = element.getAttribute(SETTINGS_DESCRIPTION_ATTRIBUTE);

        VerificationMode mode = XmlUtils.readEnumAttribute(element, SETTINGS_MODE_ATTRIBUTE,
                VerificationMode.class, VerificationMode.STG_REACHABILITY);

        int verbosity = XmlUtils.readIntAttribute(element, SETTINGS_VERBOSITY_ATTRIBUTE, 0);

        int solutionNumberLimit = XmlUtils.readIntAttribute(element, SETTINGS_SOLUTION_LIMIT_ATTRIBUTE,
                MpsatVerificationSettings.getSolutionCount());

        SolutionMode solutionMode = XmlUtils.readEnumAttribute(element, SETTINGS_SOLUTION_MODE_ATTRIBUTE,
                SolutionMode.class, MpsatVerificationSettings.getSolutionMode());

        Element reachElement = XmlUtils.getChildElement(SETTINGS_REACH_ELEMENT, element);
        String reach = reachElement == null ? "" : reachElement.getTextContent();

        boolean inversePredicate = XmlUtils.readBooleanAttribute(element, SETTINGS_INVERSE_PREDICATE_ATTRIBUTE, true);

        return new VerificationParameters(description, mode, verbosity, solutionMode, solutionNumberLimit, reach, inversePredicate);
    }

    @Override
    public void toXML(VerificationParameters verificationParameters, Element parent) {
        Element element = parent.getOwnerDocument().createElement(SETTINGS_ELEMENT);
        element.setAttribute(SETTINGS_DESCRIPTION_ATTRIBUTE, verificationParameters.getDescription());
        element.setAttribute(SETTINGS_MODE_ATTRIBUTE, verificationParameters.getMode().name());
        element.setAttribute(SETTINGS_VERBOSITY_ATTRIBUTE, Integer.toString(verificationParameters.getVerbosity()));
        element.setAttribute(SETTINGS_SOLUTION_LIMIT_ATTRIBUTE, Integer.toString(verificationParameters.getSolutionNumberLimit()));
        element.setAttribute(SETTINGS_SOLUTION_MODE_ATTRIBUTE, verificationParameters.getSolutionMode().name());

        Element reach = parent.getOwnerDocument().createElement(SETTINGS_REACH_ELEMENT);
        reach.setTextContent(verificationParameters.getExpression());
        element.appendChild(reach);

        element.setAttribute(SETTINGS_INVERSE_PREDICATE_ATTRIBUTE, Boolean.toString(verificationParameters.getInversePredicate()));

        parent.appendChild(element);
    }

}
