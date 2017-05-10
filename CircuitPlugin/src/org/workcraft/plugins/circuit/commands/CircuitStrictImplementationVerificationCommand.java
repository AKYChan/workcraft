package org.workcraft.plugins.circuit.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.graph.commands.AbstractVerificationCommand;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.tasks.CheckStrictImplementationTask;
import org.workcraft.plugins.mpsat.MpsatChainResultHandler;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgUtils;
import org.workcraft.tasks.TaskManager;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CircuitStrictImplementationVerificationCommand extends AbstractVerificationCommand {

    private static final String TITLE = "Circuit vetification";

    public String getDisplayName() {
        return "Strict implementation [MPSat]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Circuit.class);
    }

    @Override
    public Position getPosition() {
        return Position.MIDDLE;
    }

    @Override
    public void run(WorkspaceEntry we) {
        VisualCircuit visualCircuit = WorkspaceUtils.getAs(we, VisualCircuit.class);
        File envFile = visualCircuit.getEnvironmentFile();
        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);
        if (check(circuit, envFile)) {
            final CheckStrictImplementationTask task = new CheckStrictImplementationTask(we);
            String description = "MPSat tool chain";
            String title = we.getTitle();
            if (!title.isEmpty()) {
                description += "(" + title + ")";
            }
            final Framework framework = Framework.getInstance();
            final TaskManager taskManager = framework.getTaskManager();
            final MpsatChainResultHandler monitor = new MpsatChainResultHandler(task);
            taskManager.queue(task, description, monitor);
        }
    }

    private boolean check(Circuit circuit, File envFile) {
        final Framework framework = Framework.getInstance();
        final MainWindow mainWindow = framework.getMainWindow();
        // Check that circuit is not empty
        if (circuit.getFunctionComponents().isEmpty()) {
            JOptionPane.showMessageDialog(mainWindow, "The circuit must have components.",
                    TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Check that environment STG exists
        Stg envStg = StgUtils.loadStg(envFile);
        if (envStg == null) {
            String message = "Strict implementation cannot be checked without an environment STG.";
            if (envFile != null) {
                message += "\n\nCannot read STG model from the file:\n" + envFile.getAbsolutePath();
            }
            JOptionPane.showMessageDialog(mainWindow, message, TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Make sure that input signals of the circuit are also inputs in the environment STG
        ArrayList<String> circuitInputSignals = ReferenceHelper.getReferenceList(circuit, (Collection) circuit.getInputPorts());
        ArrayList<String> circuitOutputSignals = ReferenceHelper.getReferenceList(circuit, (Collection) circuit.getOutputPorts());
        StgUtils.restoreInterfaceSignals(envStg, circuitInputSignals, circuitOutputSignals);

        // Check that the set of circuit input signals is a subset of STG input signals.
        Set<String> stgInputSignals = envStg.getSignalNames(Type.INPUT, null);
        for (Contact port: circuit.getInputPorts()) {
            circuitInputSignals.add(port.getName());
        }
        if (!stgInputSignals.containsAll(circuitInputSignals)) {
            Set<String> missingInputSignals = new HashSet<>(circuitInputSignals);
            missingInputSignals.removeAll(stgInputSignals);
            String message = "Strict implementation cannot be checked for a circuit whose\n"
                    + "input signals are not specified in its environment STG.";
            message += "\n\nThe following input signals are missing in the environemnt STG:\n"
                    + ReferenceHelper.getReferencesAsString(missingInputSignals, 50);
            JOptionPane.showMessageDialog(mainWindow, message, TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Check that the set of local signals is the same for the circuit and STG.
        Set<String> stgLocalSignals = new HashSet<>();
        stgLocalSignals.addAll(envStg.getSignalNames(Type.INTERNAL, null));
        stgLocalSignals.addAll(envStg.getSignalNames(Type.OUTPUT, null));
        Set<String> circuitLocalSignals = new HashSet<>();
        for (FunctionComponent component: circuit.getFunctionComponents()) {
            Collection<Contact> componentOutputs = component.getOutputs();
            for (Contact contact: componentOutputs) {
                String signalName = CircuitUtils.getSignalName(circuit, contact);
                circuitLocalSignals.add(signalName);
            }
        }
        if (!stgLocalSignals.equals(circuitLocalSignals)) {
            String message = "Strict implementation cannot be checked for a circuit whose\n"
                    + "non-input signals are different from those of its environment STG.";
            Set<String> missingCircuitSignals = new HashSet<>(circuitLocalSignals);
            missingCircuitSignals.removeAll(stgLocalSignals);
            if (!missingCircuitSignals.isEmpty()) {
                message += "\n\nNon-input signals missing in the circuit:\n"
                        + ReferenceHelper.getReferencesAsString(missingCircuitSignals, 50);
            }
            Set<String> missingStgSignals = new HashSet<>(stgLocalSignals);
            missingStgSignals.removeAll(circuitLocalSignals);
            if (!missingStgSignals.isEmpty()) {
                message += "\n\nNon-input signals missing in the environment STG:\n"
                        + ReferenceHelper.getReferencesAsString(missingStgSignals, 50);
            }
            JOptionPane.showMessageDialog(mainWindow, message, TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}
