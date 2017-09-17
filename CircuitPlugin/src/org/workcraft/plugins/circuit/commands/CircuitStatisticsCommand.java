package org.workcraft.plugins.circuit.commands;

import java.util.Collection;

import org.workcraft.gui.graph.commands.AbstractStatisticsCommand;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.CircuitSettings;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.genlib.Gate;
import org.workcraft.plugins.circuit.genlib.GenlibUtils;
import org.workcraft.plugins.circuit.genlib.Library;
import org.workcraft.util.MultiSet;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CircuitStatisticsCommand extends AbstractStatisticsCommand {

    @Override
    public String getDisplayName() {
        return "Circuit analysis";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Circuit.class);
    }

    @Override
    public String getStatistics(WorkspaceEntry we) {
        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);

        Collection<FunctionComponent> components = circuit.getFunctionComponents();
        Collection<Contact> ports = circuit.getPorts();

        int isolatedComponentCount = 0;
        int isolatedPinCount = 0;
        MultiSet<Integer> fanin = new MultiSet<>();
        MultiSet<Integer> fanout = new MultiSet<>();
        for (FunctionComponent component: components) {
            boolean isIsolatedComponent = true;
            Collection<Contact> inputs = component.getInputs();
            Collection<Contact> outputs = component.getOutputs();
            for (Contact input: inputs) {
                Contact driver = CircuitUtils.findDriver(circuit, input, false);
                if (driver == null) {
                    isolatedComponentCount++;
                } else {
                    isIsolatedComponent = false;
                }
            }
            fanin.add(inputs.size());
            for (Contact output: outputs) {
                Collection<Contact> driven = CircuitUtils.findDriven(circuit, output, false);
                if (driven.isEmpty()) {
                    isolatedPinCount++;
                } else {
                    isIsolatedComponent = false;
                }
                fanout.add(driven.size());
            }
            if (isIsolatedComponent) {
                isolatedComponentCount++;
            }
        }

        int inPortCount = 0;
        int outPortCount = 0;
        int isolatedPortCount = 0;
        for (Contact port: ports) {
            if (port.isOutput()) {
                outPortCount++;
            } else {
                inPortCount++;
                Collection<Contact> driven = CircuitUtils.findDriven(circuit, port, false);
                fanout.add(driven.size());
            }
            if (circuit.getPreset(port).isEmpty() && circuit.getPostset(port).isEmpty()) {
                isolatedPortCount++;
            }
        }

        String libraryFileName = CircuitSettings.getGateLibrary();
        Library library = GenlibUtils.readLibrary(libraryFileName);
        double gateArea = 0.0;
        int mappedCount = 0;
        MultiSet<String> namedComponents = new MultiSet<>();
        for (FunctionComponent component: components) {
            if (component.isMapped()) {
                mappedCount++;
                String moduleName = component.getModule();
                Gate gate = library.get(moduleName);
                if (gate != null) {
                    gateArea += gate.size;
                } else {
                    namedComponents.add(moduleName);
                }
            }
        }

        return "Circuit analysis:"
                + "\n  Component count (mapped / unmapped) -  " + components.size()
                + " (" + mappedCount + " / " + (components.size() - mappedCount) + ")"
                + (mappedCount == 0 ? "" : "\n  Area of mapped components -  " + gateArea + getNamedComponentArea(namedComponents))
                + "\n  Port count (input / output) -  " + ports.size() + " (" + inPortCount + " / " + outPortCount + ")"
                + "\n  Fanin distribution (0 / 1 / 2 ...) -  " + getDistribution(fanin)
                + "\n  Fanout distribution (0 / 1 / 2 ...) -  " + getDistribution(fanout)
                + "\n  Disconnected components / ports / pins -  " + isolatedComponentCount + " / " + isolatedPortCount
                + " / " + isolatedPinCount;
    }

    private String getDistribution(MultiSet<Integer> multiset) {
        String result = "";
        int max = 0;
        for (Integer i: multiset.toSet()) {
            if (i > max) {
                max = i;
            }
        }
        for (int i = 0; i <= max; ++i) {
            if (!result.isEmpty()) {
                result += " / ";
            }
            result += multiset.count(i);
        }
        return result;
    }

    private String getNamedComponentArea(MultiSet<String> multiset) {
        String result = "";
        for (String s: multiset.toSet()) {
            int count = multiset.count(s);
            result += " + " + (count > 1 ? count + "*" : "") + s;
        }
        return result;
    }

}
