/*
 *
 * Copyright 2008,2009 Newcastle University
 *
 * This file is part of Workcraft.
 *
 * Workcraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Workcraft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.workcraft.plugins.circuit.interop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.workcraft.Framework;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathNode;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.FormatException;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.CircuitModelDescriptor;
import org.workcraft.plugins.circuit.CircuitSettings;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.Contact.IOType;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.genlib.Function;
import org.workcraft.plugins.circuit.genlib.Gate;
import org.workcraft.plugins.circuit.genlib.GenlibUtils;
import org.workcraft.plugins.circuit.genlib.Library;
import org.workcraft.plugins.circuit.javacc.GenlibParser;
import org.workcraft.plugins.circuit.javacc.ParseException;
import org.workcraft.plugins.circuit.javacc.VerilogParser;
import org.workcraft.plugins.circuit.verilog.Instance;
import org.workcraft.plugins.circuit.verilog.Module;
import org.workcraft.plugins.circuit.verilog.Pin;
import org.workcraft.plugins.circuit.verilog.Port;
import org.workcraft.plugins.cpog.optimisation.BooleanFormula;
import org.workcraft.plugins.cpog.optimisation.booleanvisitors.BooleanUtils;
import org.workcraft.plugins.cpog.optimisation.booleanvisitors.FormulaToString;
import org.workcraft.plugins.shared.CommonDebugSettings;
import org.workcraft.workspace.ModelEntry;


public class VerilogImporter implements Importer {

	private static final String PRIMITIVE_GATE_INPUT_PREFIX = "i";
	private static final String PRIMITIVE_GATE_OUTPUT_NAME = "o";

	private class Wire {
		public FunctionContact source = null;
		public HashSet<FunctionContact> sinks = new HashSet<>();
	}

	@Override
	public boolean accept(File file) {
		return file.getName().endsWith(".v");
	}

	@Override
	public String getDescription() {
		return "Verilog (.v)";
	}

	@Override
	public ModelEntry importFrom(InputStream in) throws DeserialisationException {
		return new ModelEntry(new CircuitModelDescriptor(), importCircuit(in));
	}

	public Circuit importCircuit(InputStream in) throws DeserialisationException {
		try {
			VerilogParser parser = new VerilogParser(in);
			HashMap<String, Module> modules = getModuleMap(parser.parseCircuit());
			HashSet<Module> topModules = getTopModule(modules);
			if (topModules.size() == 0) {
				throw new RuntimeException("No top module found.");
			}
			if (topModules.size() > 1) {
				throw new RuntimeException("Too many top modules found.");
			}
			if (CommonDebugSettings.getVerboseImport()) {
				System.out.print("Info: parsed Verilog modules\n");
				for (Module module: modules.values()) {
					if (topModules.contains(module)) {
						System.out.print("// Top module\n");
					}
					printModule(module);
				}
			}
			Module topModule = topModules.iterator().next();
			Circuit circuit = createCircuit(topModule, modules);
			return circuit;
		} catch (FormatException e) {
			throw new DeserialisationException(e);
		} catch (ParseException e) {
			throw new DeserialisationException(e);
		}
	}

	private HashSet<Module> getTopModule(HashMap<String, Module> modules) {
		HashSet<Module> result = new HashSet<>(modules.values());
		for (Module module: modules.values()) {
			if (module.instances.isEmpty()) {
				result.remove(module);
			}
			for (Instance instance: module.instances) {
				if (instance.moduleName == null) continue;
				result.remove(modules.get(instance.moduleName));
			}
		}
		return result;
	}

	private void printModule(Module module) {
		System.out.print("module " + module.name + " ");
		boolean firstPort = true;
		for (Port port: module.ports) {
			if (firstPort) {
				System.out.print("(");
			} else {
				System.out.print(",");
			}
			System.out.print("\n    " + port.type + " " + port.name);
			firstPort = false;
		}
		System.out.println(");");

		for (Instance instance: module.instances) {
			System.out.print("    " + instance.moduleName);
			if (instance.name != null) {
				System.out.print(" " + instance.name);
			}
			boolean firstPin = true;
			for (Pin connection: instance.connections) {
				if (firstPin) {
					System.out.print(" (");
				} else {
					System.out.print(", ");
				}
				if (connection.name != null) {
					System.out.print("." + connection.name + "(" + connection.netName +")");
				} else {
					System.out.print(connection.netName);
				}
				firstPin = false;
			}
			System.out.println(");");
		}
		System.out.print("endmodule\n\n");
	}

	private Circuit createCircuit(Module topModule, HashMap<String, Module> modules) {
		Circuit circuit = new Circuit();
		HashMap<Instance, FunctionComponent> instanceComponentMap = new HashMap<>();
		Library library = readGenlib();
		HashMap<String, Wire> wires = createPorts(circuit, topModule);
		for (Instance verilogInstance: topModule.instances) {
			Gate gate = createPrimitiveGate(verilogInstance);
			if (gate == null) {
				gate = library.get(verilogInstance.moduleName);
			}
			FunctionComponent component = null;
			if (gate != null) {
				component = createLibraryGate(circuit, verilogInstance, wires, gate);
			} else {
				component = createBlackBox(circuit, verilogInstance, wires, modules);
			}
			if (component != null) {
				instanceComponentMap.put(verilogInstance, component);
			}
		}
		createConnections(circuit, wires);
		setInitialState(circuit, wires, topModule.highSignals);
		mergeGroups1(circuit, topModule.groups, instanceComponentMap);
		return circuit;
	}

	private void mergeGroups1(Circuit circuit, Set<List<Instance>> groups, HashMap<Instance, FunctionComponent> instanceComponentMap) {
		for (List<Instance> group: groups) {
			HashSet<FunctionComponent> components = new HashSet<>();
			FunctionComponent rootComponent = null;
			for (Instance instance: group) {
				FunctionComponent component = instanceComponentMap.get(instance);
				if (component != null) {
					components.add(component);
					rootComponent = component;
				}
			}
			components.remove(rootComponent);
			mergeComponents1(circuit, rootComponent, components);
			circuit.remove(rootComponent);
			for (FunctionComponent component: components) {
				circuit.remove(component);
			}
		}
	}

	private FunctionComponent mergeComponents1(Circuit circuit, FunctionComponent rootComponent, HashSet<FunctionComponent> components) {
		HashSet<FunctionComponent> leafComponents = new HashSet<>();
		for (FunctionComponent component: components) {
			for (MathNode node: CircuitUtils.getComponentPostset(circuit, component)) {
				if (node == rootComponent) {
					leafComponents.add(component);
				}
			}
		}
		return mergeComponents(circuit, rootComponent, leafComponents);
	}

	private FunctionComponent mergeComponents(Circuit circuit, FunctionComponent rootComponent, HashSet<FunctionComponent> leafComponents) {
		FunctionComponent component = null;
		Collection<Contact> rootOutputContacts = rootComponent.getOutputs();
		if (rootOutputContacts.size() != 1) {
			throw new RuntimeException("Cannot determin the output of component '" + circuit.getName(rootComponent) + "'.");
		}
		FunctionContact rootOutputContact = (FunctionContact)rootOutputContacts.iterator().next();
		List<Contact> rootInputContacts = new LinkedList<>(rootComponent.getInputs());

		HashMap<Contact, Contact> newToOldContactMap = new HashMap<>();
		component = new FunctionComponent();
		circuit.add(component);
		FunctionContact outputContact = new FunctionContact(IOType.OUTPUT);
		component.add(outputContact);
		circuit.setName(outputContact, PRIMITIVE_GATE_OUTPUT_NAME);
		newToOldContactMap.put(outputContact, rootOutputContact);

		List<BooleanFormula> leafSetFunctions = new LinkedList<>();
		for (Contact rootInputContact: rootInputContacts) {
			BooleanFormula leafSetFunction = null;
			for (FunctionComponent leafComponent: leafComponents) {
				Collection<Contact> leafOutputContacts = leafComponent.getOutputs();
				if (leafOutputContacts.size() != 1) {
					throw new RuntimeException("Cannot determin the output of component '" + circuit.getName(leafComponent) + "'.");
				}
				FunctionContact leafOutputContact = (FunctionContact)leafOutputContacts.iterator().next();
				List<Contact> leafInputContacts = new LinkedList<>(leafComponent.getInputs());

				Set<Node> oldContacts = circuit.getPostset(leafOutputContact);
				if (oldContacts.contains(rootInputContact)) {
					List<BooleanFormula> replacementContacts = new LinkedList<>();
					for (Contact leafInputContact: leafInputContacts) {
						FunctionContact inputContact = new FunctionContact(IOType.INPUT);
						component.add(inputContact);
						circuit.setName(inputContact, rootInputContact.getName() + leafInputContact.getName());
						replacementContacts.add(inputContact);
						newToOldContactMap.put(inputContact, leafInputContact);
					}
					leafSetFunction = BooleanUtils.dumbReplace(
							leafOutputContact.getSetFunction(), leafInputContacts, replacementContacts);
				}

			}
			if (leafSetFunction == null) {
				FunctionContact inputContact = new FunctionContact(IOType.INPUT);
				component.add(inputContact);
				circuit.setName(inputContact, rootInputContact.getName());
				newToOldContactMap.put(inputContact, rootInputContact);
				leafSetFunction = inputContact;
			}
			leafSetFunctions.add(leafSetFunction);
		}
		BooleanFormula rootSetFunction = rootOutputContact.getSetFunction();
		final BooleanFormula setFunction = BooleanUtils.dumbReplace(rootSetFunction, rootInputContacts, leafSetFunctions);
		outputContact.setSetFunction(setFunction);

		System.out.println("> : " + FormulaToString.toString(rootSetFunction));
		for (BooleanFormula leafSetFunction: leafSetFunctions) {
			System.out.println("+ : " + FormulaToString.toString(leafSetFunction));
		}
		System.out.println("= : " + FormulaToString.toString(setFunction));

		for (Connection outputConnection: circuit.getConnections(rootOutputContact)) {
			if (outputConnection.getFirst() != rootOutputContact) continue;
			Node toNode = outputConnection.getSecond();
			circuit.remove(outputConnection);
			try {
				circuit.connect(outputContact, toNode);
			} catch (InvalidConnectionException e) {
			}
		}
		for (Contact newContact: newToOldContactMap.keySet()) {
			if (newContact.isOutput()) continue;
			Contact oldContact = newToOldContactMap.get(newContact);
			if (oldContact == null) continue;
			for (Node fromNode: circuit.getPreset(oldContact)) {
				try {
					circuit.connect(fromNode, newContact);
				} catch (InvalidConnectionException e) {
				}
			}
		}
		return component;
	}


	private void mergeGroups(Circuit circuit, Set<List<Instance>> groups, HashMap<Instance, FunctionComponent> instanceComponentMap) {
		for (List<Instance> group: groups) {
			List<FunctionComponent> orderedComponents = new LinkedList<>();
			for (Instance instance: group) {
				FunctionComponent component = instanceComponentMap.get(instance);
				if (component != null) {
					orderedComponents.add(component);
				}
			}
			mergeComponents(circuit, orderedComponents);
		}
	}

	private void mergeComponents(Circuit circuit, List<FunctionComponent> components) {
//		for (FunctionComponent firstComponent: components) {
		FunctionComponent firstComponent = components.iterator().next();
		{
			//Deque<FunctionComponent> stack = new ArrayDeque<>();
			for (MathNode node: CircuitUtils.getComponentPostset(circuit, firstComponent)) {
				if (node instanceof FunctionComponent) {
					FunctionComponent secondComponent = (FunctionComponent)node;
					FunctionComponent component = mergeComponents(circuit, firstComponent, secondComponent);
					circuit.remove(secondComponent);

				}
			}
			circuit.remove(firstComponent);
		}
	}

	private FunctionComponent mergeComponents(Circuit circuit, FunctionComponent firstComponent, FunctionComponent secondComponent) {
		FunctionComponent component = null;
		Collection<Contact> firstOutputContacts = firstComponent.getOutputs();
		Collection<Contact> secondOutputContacts = secondComponent.getOutputs();
		if ((firstOutputContacts.size() == 1) && (secondOutputContacts.size() == 1)) {
			FunctionContact firstOutputContact = (FunctionContact)firstOutputContacts.iterator().next();
			List<Contact> firstInputContacts = new LinkedList<>(firstComponent.getInputs());
			FunctionContact secondOutputContact = (FunctionContact)secondOutputContacts.iterator().next();
			List<Contact> secondInputContacts = new LinkedList<>(secondComponent.getInputs());

			HashMap<Contact, Contact> newToOldContactMap = new HashMap<>();
			component = new FunctionComponent();
			circuit.add(component);
			FunctionContact outputContact = new FunctionContact(IOType.OUTPUT);
			component.add(outputContact);
			circuit.setName(outputContact, PRIMITIVE_GATE_OUTPUT_NAME);
			newToOldContactMap.put(outputContact, secondOutputContact);

			Set<Node> oldContacts = circuit.getPostset(firstOutputContact);
			List<BooleanFormula> firstSetFunctions = new LinkedList<>();
			for (Contact secondInputContact: secondInputContacts) {
				if ( !oldContacts.contains(secondInputContact) ) {
					FunctionContact inputContact = new FunctionContact(IOType.INPUT);
					component.add(inputContact);
					circuit.setName(inputContact, secondInputContact.getName());
					newToOldContactMap.put(inputContact, secondInputContact);
					firstSetFunctions.add(inputContact);
				} else {
					List<BooleanFormula> replacementContacts = new LinkedList<>();
					for (Contact firstInputContact: firstInputContacts) {
						FunctionContact inputContact = new FunctionContact(IOType.INPUT);
						component.add(inputContact);
						circuit.setName(inputContact, secondInputContact.getName() + firstInputContact.getName());
						replacementContacts.add(inputContact);
						newToOldContactMap.put(inputContact, firstInputContact);
					}
					BooleanFormula firstSetFunction = BooleanUtils.dumbReplace(
							firstOutputContact.getSetFunction(), firstInputContacts, replacementContacts);
					firstSetFunctions.add(firstSetFunction);
				}
			}
			BooleanFormula secondSetFunction = secondOutputContact.getSetFunction();
			final BooleanFormula setFunction = BooleanUtils.dumbReplace(secondSetFunction, secondInputContacts, firstSetFunctions);
			outputContact.setSetFunction(setFunction);

			System.out.println("> : " + FormulaToString.toString(secondSetFunction));
			for (BooleanFormula firstSetFunction: firstSetFunctions) {
				System.out.println("+ : " + FormulaToString.toString(firstSetFunction));
			}
			System.out.println("= : " + FormulaToString.toString(setFunction));

			for (Connection outputConnection: circuit.getConnections(secondOutputContact)) {
				if (outputConnection.getFirst() != secondOutputContact) continue;
				Node toNode = outputConnection.getSecond();
				circuit.remove(outputConnection);
				try {
					circuit.connect(outputContact, toNode);
				} catch (InvalidConnectionException e) {
				}
			}
			for (Contact newContact: newToOldContactMap.keySet()) {
				if (newContact.isOutput()) continue;
				Contact oldContact = newToOldContactMap.get(newContact);
				if (oldContact == null) continue;
				for (Node fromNode: circuit.getPreset(oldContact)) {
					try {
						circuit.connect(fromNode, newContact);
					} catch (InvalidConnectionException e) {
					}
				}
			}
		}
		return component;
	}

	private Library readGenlib() {
		Library library = new Library();
		String libraryFileName = CircuitSettings.getGateLibrary();
		if ((libraryFileName == null) || libraryFileName.isEmpty()) {
			System.out.println("Warning: gate library file is not specified.");
		} else {
			File libraryFile = new File(libraryFileName);
			final Framework framework = Framework.getInstance();
			if (framework.checkFile(libraryFile, "Gate library access error")) {
				try {
					InputStream genlibInputStream = new FileInputStream(CircuitSettings.getGateLibrary());
					library = new GenlibParser(genlibInputStream).parseGenlib();
				} catch (FileNotFoundException e) {
				} catch (ParseException e) {
					System.out.println("Warning: could not parse the gate library '" + libraryFileName + "'.");
				}
			}
		}
		return library;
	}

	private Gate createPrimitiveGate(Instance verilogInstance) {
		String operator = getPrimitiveOperator(verilogInstance.moduleName);
		if (operator == null) {
			return null;
		}
		String expression = "";
		int index = 0;
		for (Pin verilogPin: verilogInstance.connections) {
			if (index > 0) {
				String pinName = getPrimitiveGatePinName(index);
				if ( !expression.isEmpty() ) {
					expression += operator;
				}
				expression += pinName;
			}
			index++;
		}
		if (getPrimitiveUnitness(verilogInstance.moduleName)) {
			if (index > 1) {
				expression = "(" + expression + ")";
			}
			expression = "!" + expression;
		}
		Function function = new Function(PRIMITIVE_GATE_OUTPUT_NAME, expression);
		return new Gate("", function, null, true);
	}

	private String getPrimitiveOperator(String primitiveName) {
		switch (primitiveName) {
		case "buf":
		case "not":
			return "";
		case "and":
		case "nand":
			return "*";
		case "or":
		case "nor":
			return "+";
		case "xnor":
			return "^";
		default:
			return null;
		}
	}

	private boolean getPrimitiveUnitness(String primitiveName) {
		switch (primitiveName) {
		case "buf":
		case "and":
		case "or":
			return false;
		case "not":
		case "nand":
		case "nor":
		case "xnor":
			return true;
		default:
			return true;
		}
	}

	private String getPrimitiveGatePinName(int index) {
		if (index == 0) {
			return PRIMITIVE_GATE_OUTPUT_NAME;
		} else {
			return PRIMITIVE_GATE_INPUT_PREFIX + index;
		}
	}

	private FunctionComponent createLibraryGate(Circuit circuit, Instance verilogInstance,
			HashMap<String, Wire> wires, Gate gate) {
		FunctionComponent component = GenlibUtils.instantiateGate(gate, verilogInstance.name, circuit);
		int index = 0;
		for (Pin verilogPin: verilogInstance.connections) {
			Wire wire = wires.get(verilogPin.netName);
			if (wire == null) {
				wire = new Wire();
				wires.put(verilogPin.netName, wire);
			}
			String pinName = (gate.isPrimititve() ? getPrimitiveGatePinName(index++) : verilogPin.name);
			Node node = circuit.getNodeByReference(component, pinName);
			if (node instanceof FunctionContact) {
				FunctionContact contact = (FunctionContact)node;
				if (contact.isInput()) {
					wire.sinks.add(contact);
				} else {
					wire.source = contact;
				}
			}
		}
		return component;
	}

	private FunctionComponent createBlackBox(Circuit circuit, Instance verilogInstance,
			HashMap<String, Wire> wires, HashMap<String, Module> modules) {
		final FunctionComponent component = new FunctionComponent();
		component.setModule(verilogInstance.moduleName);
		circuit.add(component);
		try {
			circuit.setName(component, verilogInstance.name);
		} catch (ArgumentException e) {
			System.out.println("Warning: cannot set name '" + verilogInstance.name +"' for component '" + circuit.getName(component) + "'.");
		}
		Module module = modules.get(verilogInstance.moduleName);
		HashMap<String, Port> instancePorts = getModulePortMap(module);
		for (Pin verilogPin: verilogInstance.connections) {
			Port verilogPort = instancePorts.get(verilogPin.name);
			Wire wire = wires.get(verilogPin.netName);
			if (wire == null) {
				wire = new Wire();
				wires.put(verilogPin.netName, wire);
			}
			FunctionContact contact = new FunctionContact();
			if ((verilogPort != null) && (verilogPort.isInput())) {
				contact.setIOType(IOType.INPUT);
				wire.sinks.add(contact);
			} else {
				contact.setIOType(IOType.OUTPUT);
				wire.source = contact;
			}
			component.add(contact);
			if (verilogPin.name != null) {
				circuit.setName(contact, verilogPin.name);
			}
		}
		return component;
	}

	private HashMap<String, Wire> createPorts(Circuit circuit, Module module) {
		HashMap<String, Wire> wires = new HashMap<>();
		for (Port verilogPort: module.ports) {
			FunctionContact contact = new FunctionContact();
			Wire wire = new Wire();
			if (verilogPort.isInput()) {
				contact.setIOType(IOType.INPUT);
				wire.source = contact;
			} else if (verilogPort.isOutput()) {
				contact.setIOType(IOType.OUTPUT);
				wire.sinks.add(contact);
			}
			wires.put(verilogPort.name, wire);
			circuit.setName(contact, verilogPort.name);
			circuit.add(contact);
		}
		return wires;
	}

	private void createConnections(Circuit circuit, Map<String, Wire> wires) {
		for (Wire wire: wires.values()) {
			if (wire.source == null) continue;
			for (FunctionContact sink: wire.sinks) {
				try {
					circuit.connect(wire.source, sink);
				} catch (InvalidConnectionException e) {
				}
			}
		}
	}

	private void setInitialState(Circuit circuit, Map<String, Wire> wires, Set<String> highSignals) {
		for (String signalName: wires.keySet()) {
			Wire wire = wires.get(signalName);
			if ((highSignals != null) && (wire.source != null)) {
				wire.source.setInitToOne(highSignals.contains(signalName));
			}
		}
	}

	private HashMap<String, Module> getModuleMap(List<Module> modules) throws ParseException {
		HashMap<String, Module> result = new HashMap<>();
		for (Module module: modules) {
			if ((module == null) || (module.name == null)) continue;
			result.put(module.name, module);
		}
		return result;
	}

	private HashMap<String, Port> getModulePortMap(Module module) {
		HashMap<String, Port> result = new HashMap<>();
		if (module != null) {
			for (Port port: module.ports) {
				result.put(port.name, port);
			}
		}
		return result;
	}

}
