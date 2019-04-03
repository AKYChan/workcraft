package org.workcraft.plugins.circuit.commands;

import org.workcraft.gui.tools.GraphEditorTool;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.tools.InitialisationAnalyserTool;
import org.workcraft.plugins.circuit.utils.ResetUtils;

import java.util.Collection;
import java.util.function.Function;

public class ForceInitInputPortsTagCommand extends AbstractTagCommand {

    @Override
    Function<Circuit, Collection<Contact>> getFunction() {
        return circuit -> ResetUtils.tagForceInitInputPorts(circuit);
    }

    @Override
    String getMessage() {
        return "Force init input port";
    }

    @Override
    Class<? extends GraphEditorTool> getToolClass() {
        return InitialisationAnalyserTool.class;
    }

}
