package org.workcraft.plugins.circuit.interop;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.workcraft.Framework;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Node;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.hierarchy.NamespaceProvider;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.references.HierarchicalUniqueNameReferenceManager;
import org.workcraft.dom.references.NameManager;
import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.FormatException;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.utils.BooleanUtils;
import org.workcraft.formula.utils.FormulaToString;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.CircuitDescriptor;
import org.workcraft.plugins.circuit.CircuitSettings;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.Contact.IOType;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.expression.Expression;
import org.workcraft.plugins.circuit.expression.ExpressionUtils;
import org.workcraft.plugins.circuit.expression.Literal;
import org.workcraft.plugins.circuit.genlib.Function;
import org.workcraft.plugins.circuit.genlib.Gate;
import org.workcraft.plugins.circuit.genlib.GenlibUtils;
import org.workcraft.plugins.circuit.genlib.Library;
import org.workcraft.plugins.circuit.jj.expression.ExpressionParser;
import org.workcraft.plugins.circuit.jj.genlib.GenlibParser;
import org.workcraft.plugins.circuit.jj.verilog.VerilogParser;
import org.workcraft.plugins.circuit.verilog.Assign;
import org.workcraft.plugins.circuit.verilog.Instance;
import org.workcraft.plugins.circuit.verilog.Module;
import org.workcraft.plugins.circuit.verilog.Pin;
import org.workcraft.plugins.circuit.verilog.Port;
import org.workcraft.plugins.shared.CommonDebugSettings;
import org.workcraft.plugins.stg.MutexData;
import org.workcraft.util.LogUtils;
import org.workcraft.workspace.ModelEntry;

public class VerilogImporter implements Importer {

    private static final String MUTEX_MODULE_NAME = "MUTEX";
    private static final String MUTEX_R1_NAME = "r1";
    private static final String MUTEX_G1_NAME = "g1";
    private static final String MUTEX_R2_NAME = "r2";
    private static final String MUTEX_G2_NAME = "g2";

    private class AssignGate {
        public final String outputName;
        public final String setFunction;
        public final String resetFunction;
        public final HashMap<String, String> connections; // (portName -> wireName)

        AssignGate(String outputName, String setFunction, String resetFunction, HashMap<String, String> connections) {
            this.outputName = outputName;
            this.setFunction = setFunction;
            this.resetFunction = resetFunction;
            this.connections = connections;
        }
    }

    private static final String MSG_MANY_TOP_MODULES = "More than one top module is found.";
    private static final String MSG_NO_TOP_MODULE = "No top module found.";

    private static final String PRIMITIVE_GATE_INPUT_PREFIX = "i";
    private static final String PRIMITIVE_GATE_OUTPUT_NAME = "o";

    private final boolean sequentialAssign;

    private class Wire {
        public FunctionContact source = null;
        public HashSet<FunctionContact> sinks = new HashSet<>();
        public HashSet<FunctionContact> undefined = new HashSet<>();
    }

    public VerilogImporter() {
        this(false);
    }

    public VerilogImporter(boolean sequentialAssign) {
        this.sequentialAssign = sequentialAssign;
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
        return new ModelEntry(new CircuitDescriptor(), importCircuit(in));
    }

    public Circuit importCircuit(InputStream in) throws DeserialisationException {
        return importCircuit(in, new LinkedList<MutexData>());
    }

    public Circuit importCircuit(InputStream in, Collection<MutexData> mutexData) throws DeserialisationException {
        try {
            VerilogParser parser = new VerilogParser(in);
            if (CommonDebugSettings.getParserTracing()) {
                parser.enable_tracing();
            } else {
                parser.disable_tracing();
            }
            HashMap<String, Module> modules = getModuleMap(parser.parseCircuit());
            HashSet<Module> topModules = getTopModule(modules);
            if (topModules.size() == 0) {
                throw new DeserialisationException(MSG_NO_TOP_MODULE);
            }
            if (topModules.size() > 1) {
                throw new DeserialisationException(MSG_MANY_TOP_MODULES);
            }
            if (CommonDebugSettings.getVerboseImport()) {
                LogUtils.logInfoLine("Parsed Verilog modules");
                for (Module module: modules.values()) {
                    if (topModules.contains(module)) {
                        System.out.print("// Top module\n");
                    }
                    printModule(module);
                }
            }
            Module topModule = topModules.iterator().next();
            return createCircuit(topModule, modules, mutexData);
        } catch (FormatException | org.workcraft.plugins.circuit.jj.verilog.ParseException e) {
            throw new DeserialisationException(e);
        }
    }

    private HashSet<Module> getTopModule(HashMap<String, Module> modules) {
        HashSet<Module> result = new HashSet<>(modules.values());
        if (modules.size() > 1) {
            for (Module module: modules.values()) {
                if (module.isEmpty()) {
                    result.remove(module);
                }
                for (Instance instance: module.instances) {
                    if (instance.moduleName == null) continue;
                    result.remove(modules.get(instance.moduleName));
                }
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

        for (Assign assign: module.assigns) {
            System.out.println("    assign " + assign.name + " = " + assign.formula + ";");
        }

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
                    System.out.print("." + connection.name + "(" + connection.netName + ")");
                } else {
                    System.out.print(connection.netName);
                }
                firstPin = false;
            }
            System.out.println(");");
        }
        System.out.print("endmodule\n\n");
    }

    private Circuit createCircuit(Module topModule, HashMap<String, Module> modules, Collection<MutexData> mutexData) {
        Circuit circuit = new Circuit();
        circuit.setTitle(topModule.name);
        HashMap<Instance, FunctionComponent> instanceComponentMap = new HashMap<>();
        HashMap<String, Wire> wires = createPorts(circuit, topModule);
        for (Assign assign: topModule.assigns) {
            createAssignGate(circuit, assign, wires);
        }
        Library library = null;
        for (Instance verilogInstance: topModule.instances) {
            Gate gate = createPrimitiveGate(verilogInstance);
            if (gate == null) {
                if (library == null) {
                    library = readGenlib();
                }
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
        for (MutexData me: mutexData) {
            createMutex(circuit, me, wires, modules);
        }
        createConnections(circuit, wires);
        setZeroDelayAttribute(instanceComponentMap);
        setInitialState(circuit, wires, topModule.signalStates);
        mergeGroups(circuit, topModule.groups, instanceComponentMap);
        return circuit;
    }

    private FunctionComponent createAssignGate(Circuit circuit, Assign assign, HashMap<String, Wire> wires) {
        final FunctionComponent component = new FunctionComponent();
        circuit.add(component);
        setAssignComponentName(circuit, component, assign.name);

        AssignGate assignGate = null;
        if (sequentialAssign && isSequentialAssign(assign)) {
            assignGate = createSequentialAssignGate(assign);
        } else {
            assignGate = createCombinationalAssignGate(assign);
        }
        FunctionContact outContact = null;
        for (Map.Entry<String, String> connection: assignGate.connections.entrySet()) {
            Wire wire = getOrCreateWire(connection.getValue(), wires);
            FunctionContact contact = new FunctionContact();
            if (connection.getKey().equals(assignGate.outputName)) {
                contact.setIOType(IOType.OUTPUT);
                outContact = contact;
                wire.source = contact;
            } else {
                contact.setIOType(IOType.INPUT);
                wire.sinks.add(contact);
            }
            component.add(contact);
            if (connection.getKey() != null) {
                circuit.setName(contact, connection.getKey());
            }
        }

        if (outContact != null) {
            try {
                BooleanFormula setFormula = CircuitUtils.parseContactFuncton(circuit, component, assignGate.setFunction);
                outContact.setSetFunctionQuiet(setFormula);
                BooleanFormula resetFormula = CircuitUtils.parseContactFuncton(circuit, component, assignGate.resetFunction);
                outContact.setResetFunctionQuiet(resetFormula);
            } catch (org.workcraft.formula.jj.ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return component;
    }

    private void setAssignComponentName(Circuit circuit, FunctionComponent component, String name) {
        HierarchicalUniqueNameReferenceManager refManager
                = (HierarchicalUniqueNameReferenceManager) circuit.getReferenceManager();

        NamespaceProvider namespaceProvider = refManager.getNamespaceProvider(circuit.getRoot());
        NameManager nameManagerer = refManager.getNameManager(namespaceProvider);
        String candidateName = NamespaceHelper.flattenReference(name);
        String componentName = nameManagerer.getDerivedName(component, candidateName);
        try {
            circuit.setName(component, componentName);
        } catch (ArgumentException e) {
            String oldComponentName = circuit.getName(component);
            LogUtils.logWarningLine("Cannot set name '" + componentName + "' for component '" + oldComponentName + "'.");
        }
    }

    private boolean isSequentialAssign(Assign assign) {
        Expression expression = convertStringToExpression(assign.formula);
        LinkedList<Literal> literals = new LinkedList<>(expression.getLiterals());
        for (Literal literal: literals) {
            if (assign.name.equals(literal.name)) {
                return true;
            }
        }
        return false;
    }

    private AssignGate createCombinationalAssignGate(Assign assign) {
        Expression expression = convertStringToExpression(assign.formula);
        int index = 0;
        HashMap<String, String> connections = new HashMap<>(); // (port -> net)
        String outputName = getPrimitiveGatePinName(0);
        connections.put(outputName, assign.name);
        LinkedList<Literal> literals = new LinkedList<>(expression.getLiterals());
        Collections.reverse(literals);
        for (Literal literal: literals) {
            String netName = literal.name;
            String name = getPrimitiveGatePinName(++index);
            literal.name = name;
            connections.put(name, netName);
        }
        return new AssignGate(outputName, expression.toString(), null, connections);
    }

    private AssignGate createSequentialAssignGate(Assign assign) {
        Expression expression = convertStringToExpression(assign.formula);
        String function = expression.toString();

        String setFunction = ExpressionUtils.extactSetExpression(function, assign.name);
        Expression setExpression = convertStringToExpression(setFunction);

        String resetFunction = ExpressionUtils.extactResetExpression(function, assign.name);
        Expression resetExpression = convertStringToExpression(resetFunction);
        if (CommonDebugSettings.getVerboseImport()) {
            LogUtils.logInfoLine("Extracting SET and RESET from assign " + assign.name + " = " + assign.formula);
            LogUtils.logInfoLine("  Function: " + function);
            LogUtils.logInfoLine("  Set function: " + setFunction);
            LogUtils.logInfoLine("  Reset function: " + resetFunction);
        }
        HashMap<String, String> connections = new HashMap<>();
        String outputName = getPrimitiveGatePinName(0);
        connections.put(outputName, assign.name);

        LinkedList<Literal> literals = new LinkedList<>();
        literals.addAll(setExpression.getLiterals());
        literals.addAll(resetExpression.getLiterals());
        Collections.reverse(literals);
        int index = 0;
        HashMap<String, String> netToPortMap = new HashMap<>();
        for (Literal literal: literals) {
            String netName = literal.name;
            String name = netToPortMap.get(netName);
            if (name == null) {
                name = getPrimitiveGatePinName(++index);
                netToPortMap.put(netName, name);
            }
            literal.name = name;
            connections.put(name, netName);
        }
        return new AssignGate(outputName, setExpression.toString(), resetExpression.toString(), connections);
    }

    private Expression convertStringToExpression(String formula) {
        InputStream expressionStream = new ByteArrayInputStream(formula.getBytes());
        ExpressionParser expressionParser = new ExpressionParser(expressionStream);
        if (CommonDebugSettings.getParserTracing()) {
            expressionParser.enable_tracing();
        } else {
            expressionParser.disable_tracing();
        }
        Expression expression = null;
        try {
            expression = expressionParser.parseExpression();
        } catch (org.workcraft.plugins.circuit.jj.expression.ParseException e1) {
            LogUtils.logWarningLine("Could not parse assign expression '" + formula + "'.");
        }
        return expression;
    }

    private Library readGenlib() {
        Library library = new Library();
        String libraryFileName = CircuitSettings.getGateLibrary();
        if ((libraryFileName == null) || libraryFileName.isEmpty()) {
            LogUtils.logWarningLine("Gate library file is not specified.");
        } else {
            File libraryFile = new File(libraryFileName);
            final Framework framework = Framework.getInstance();
            if (framework.checkFileMessageLog(libraryFile, "Gate library access error")) {
                try {
                    InputStream genlibInputStream = new FileInputStream(libraryFileName);
                    GenlibParser genlibParser = new GenlibParser(genlibInputStream);
                    if (CommonDebugSettings.getParserTracing()) {
                        genlibParser.enable_tracing();
                    } else {
                        genlibParser.disable_tracing();
                    }
                    library = genlibParser.parseGenlib();
                    LogUtils.logInfoLine("Mapping the imported Verilog into the gate library '" + libraryFileName + "'.");
                } catch (FileNotFoundException e) {
                } catch (org.workcraft.plugins.circuit.jj.genlib.ParseException e) {
                    LogUtils.logWarningLine("Could not parse the gate library '" + libraryFileName + "'.");
                }
            }
        }
        return library;
    }

    private HashMap<String, Wire> createPorts(Circuit circuit, Module module) {
        HashMap<String, Wire> wires = new HashMap<>();
        for (Port verilogPort: module.ports) {
            LinkedList<String> portPath = NamespaceHelper.splitReference(verilogPort.name);
            if (portPath.size() == 1) {
                // Primary port
                FunctionContact contact = new FunctionContact();
                Wire wire = getOrCreateWire(verilogPort.name, wires);
                if (verilogPort.isInput()) {
                    contact.setIOType(IOType.INPUT);
                    wire.source = contact;
                } else if (verilogPort.isOutput()) {
                    contact.setIOType(IOType.OUTPUT);
                    wire.sinks.add(contact);
                }
                circuit.setName(contact, verilogPort.name);
                circuit.add(contact);
            } else if (portPath.size() == 2) {
                // Environment component pin
                String componentName = portPath.get(0);
                String contactName = portPath.get(1);
                FunctionComponent component = null;
                Node parent = circuit.getNodeByReference(componentName);
                if (parent instanceof FunctionComponent) {
                    component = (FunctionComponent) parent;
                } else {
                    component = new FunctionComponent();
                    circuit.add(component);
                    circuit.setName(component, componentName);
                    component.setIsEnvironment(true);
                }
                FunctionContact contact = new FunctionContact();
                Wire wire = getOrCreateWire(verilogPort.name, wires);
                if (verilogPort.isInput()) {
                    contact.setIOType(IOType.OUTPUT);
                    wire.source = contact;
                } else if (verilogPort.isOutput()) {
                    contact.setIOType(IOType.INPUT);
                    wire.sinks.add(contact);
                }
                component.add(contact);
                circuit.setName(contact, contactName);
            } else {
                // Neither primary port nor environment component pin
                throw new RuntimeException("Port '" + verilogPort.name + "' cannot be imported.");
            }
        }
        return wires;
    }

    private Gate createPrimitiveGate(Instance verilogInstance) {
        String operator = getPrimitiveOperator(verilogInstance.moduleName);
        if (operator == null) {
            return null;
        }
        String expression = "";
        int index;
        for (index = 0; index < verilogInstance.connections.size(); index++) {
            if (index > 0) {
                String pinName = getPrimitiveGatePinName(index);
                if (!expression.isEmpty()) {
                    expression += operator;
                }
                expression += pinName;
            }
        }
        if (isInvertingPrimitive(verilogInstance.moduleName)) {
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

    private boolean isInvertingPrimitive(String primitiveName) {
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
            Wire wire = getOrCreateWire(verilogPin.netName, wires);
            String pinName = gate.isPrimitive() ? getPrimitiveGatePinName(index++) : verilogPin.name;
            Node node = circuit.getNodeByReference(component, pinName);
            if (node instanceof FunctionContact) {
                FunctionContact contact = (FunctionContact) node;
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
        component.setIsEnvironment(true);
        circuit.add(component);
        try {
            circuit.setName(component, verilogInstance.name);
        } catch (ArgumentException e) {
            String componentRef = circuit.getNodeReference(component);
            LogUtils.logWarningLine("Cannot set name '" + verilogInstance.name + "' for component '" + componentRef + "'.");
        }
        Module module = modules.get(verilogInstance.moduleName);
        HashMap<String, Port> instancePorts = getModulePortMap(module);
        for (Pin verilogPin: verilogInstance.connections) {
            Port verilogPort = instancePorts.get(verilogPin.name);
            Wire wire = getOrCreateWire(verilogPin.netName, wires);
            FunctionContact contact = new FunctionContact();
            if (verilogPort == null) {
                wire.undefined.add(contact);
            } else {
                if (verilogPort.isInput()) {
                    contact.setIOType(IOType.INPUT);
                    wire.sinks.add(contact);
                } else {
                    contact.setIOType(IOType.OUTPUT);
                    wire.source = contact;
                }
            }
            component.add(contact);
            if (verilogPin.name != null) {
                circuit.setName(contact, verilogPin.name);
            }
        }
        return component;
    }

    private FunctionComponent createMutex(Circuit circuit, MutexData me,
            HashMap<String, Wire> wires, HashMap<String, Module> modules) {
        final FunctionComponent component = new FunctionComponent();
        component.setModule(MUTEX_MODULE_NAME);
        circuit.add(component);
        try {
            circuit.setName(component, me.ref);
        } catch (ArgumentException e) {
            String componentRef = circuit.getNodeReference(component);
            LogUtils.logWarningLine("Cannot set name '" + me.ref + "' for component '" + componentRef + "'.");
        }
        addMutexPin(circuit, component, MUTEX_R1_NAME, IOType.INPUT, me.r1, wires);
        FunctionContact g1Contact = addMutexPin(circuit, component, MUTEX_G1_NAME, IOType.OUTPUT, me.g1, wires);
        addMutexPin(circuit, component, MUTEX_R2_NAME, IOType.INPUT, me.r2, wires);
        FunctionContact g2Contact = addMutexPin(circuit, component, MUTEX_G2_NAME, IOType.OUTPUT, me.g2, wires);
        try {
            setMutexFunctions(circuit, component, g1Contact, MUTEX_R1_NAME, MUTEX_G2_NAME);
            setMutexFunctions(circuit, component, g2Contact, MUTEX_R2_NAME, MUTEX_G1_NAME);
        } catch (org.workcraft.formula.jj.ParseException e) {
            throw new RuntimeException(e);
        }
        setMutexGrant(circuit, me.g1, wires);
        setMutexGrant(circuit, me.g2, wires);
        return component;
    }

    private void setMutexFunctions(Circuit circuit, final FunctionComponent component, FunctionContact grantContact,
            String reqPinName, String otherGrantPinName) throws org.workcraft.formula.jj.ParseException {
        String setString = reqPinName + " * " + otherGrantPinName + "'";
        BooleanFormula setFormula = CircuitUtils.parseContactFuncton(circuit, component, setString);
        grantContact.setSetFunctionQuiet(setFormula);
        String resetString = reqPinName + "'";
        BooleanFormula resetFormula = CircuitUtils.parseContactFuncton(circuit, component, resetString);
        grantContact.setResetFunctionQuiet(resetFormula);
    }

    private void setMutexGrant(Circuit circuit, String wireName, HashMap<String, Wire> wires) {
        Node g1Node = circuit.getNodeByReference(wireName);
        if (g1Node instanceof FunctionContact) {
            FunctionContact g1Port = (FunctionContact) g1Node;
            g1Port.setIOType(IOType.OUTPUT);
            Wire g1Wire = getOrCreateWire(wireName, wires);
            g1Wire.sinks.add(g1Port);
        }
    }

    private FunctionContact addMutexPin(Circuit circuit, FunctionComponent component, String pinName, IOType type,
            String wireName, HashMap<String, Wire> wires) {
        FunctionContact contact = new FunctionContact();
        contact.setIOType(type);
        component.add(contact);
        circuit.setName(contact, pinName);
        Wire wire = getOrCreateWire(wireName, wires);
        if (type == IOType.INPUT) {
            wire.sinks.add(contact);
        } else {
            wire.source = contact;
        }
        return contact;
    }

    private Wire getOrCreateWire(String name, HashMap<String, Wire> wires) {
        Wire wire = wires.get(name);
        if (wire == null) {
            wire = new Wire();
            wires.put(name, wire);
        }
        return wire;
    }

    private void createConnections(Circuit circuit, Map<String, Wire> wires) {
        boolean finalised = false;
        while (!finalised) {
            finalised = true;
            for (Wire wire: wires.values()) {
                finalised &= finaliseWire(circuit, wire);
            }
        }
        for (Wire wire: wires.values()) {
            createConnection(circuit, wire);
        }
    }

    private boolean finaliseWire(Circuit circuit, Wire wire) {
        boolean result = true;
        if (wire.source == null) {
            if (wire.undefined.size() == 1) {
                wire.source = wire.undefined.iterator().next();
                if (wire.source.isPort()) {
                    wire.source.setIOType(IOType.INPUT);
                } else {
                    wire.source.setIOType(IOType.OUTPUT);
                }
                String contactRef = circuit.getNodeReference(wire.source);
                LogUtils.logInfoLine("Source contact detected: " + contactRef);
                wire.undefined.clear();
                result = false;
            }
        } else if (!wire.undefined.isEmpty()) {
            wire.sinks.addAll(wire.undefined);
            for (FunctionContact contact: wire.undefined) {
                if (contact.isPort()) {
                    contact.setIOType(IOType.OUTPUT);
                } else {
                    contact.setIOType(IOType.INPUT);
                }
            }
            String contactRefs = ReferenceHelper.getNodesAsString(circuit, (Collection) wire.undefined);
            LogUtils.logInfoLine("Sink contacts detected: " + contactRefs);
            wire.undefined.clear();
            result = false;
        }
        return result;
    }

    private void createConnection(Circuit circuit, Wire wire) {
        Contact sourceContact = wire.source;
        if (sourceContact == null) {
            HashSet<FunctionContact> contacts = new HashSet<>();
            contacts.addAll(wire.sinks);
            contacts.addAll(wire.undefined);
            if (!contacts.isEmpty()) {
                String contactRefs = ReferenceHelper.getNodesAsString(circuit, (Collection) wire.undefined);
                LogUtils.logErrorLine("Wire without a source is connected to the following contacts: " + contactRefs);
            }
        } else {
            String sourceRef = circuit.getNodeReference(sourceContact);
            if (!wire.undefined.isEmpty()) {
                String contactRefs = ReferenceHelper.getNodesAsString(circuit, (Collection) wire.undefined);
                LogUtils.logErrorLine("Wire from contact '" + sourceRef + "' has undefined sinks: " + contactRefs);
            }
            if (sourceContact.isPort() && sourceContact.isOutput()) {
                sourceContact.setIOType(IOType.INPUT);
                LogUtils.logWarningLine("Source contact '" + sourceRef + "' is changed to input port.");
            }
            if (!sourceContact.isPort() && sourceContact.isInput()) {
                sourceContact.setIOType(IOType.OUTPUT);
                LogUtils.logWarningLine("Source contact '" + sourceRef + "' is changed to output pin.");
            }
            for (FunctionContact sinkContact: wire.sinks) {
                if (sinkContact.isPort() && sinkContact.isInput()) {
                    sinkContact.setIOType(IOType.OUTPUT);
                    LogUtils.logWarningLine("Sink contact '" + circuit.getNodeReference(sinkContact) + "' is changed to output port.");
                }
                if (!sinkContact.isPort() && sinkContact.isOutput()) {
                    sinkContact.setIOType(IOType.INPUT);
                    LogUtils.logWarningLine("Sink contact '" + circuit.getNodeReference(sinkContact) + "' is changed to input pin.");
                }
                try {
                    circuit.connect(sourceContact, sinkContact);
                } catch (InvalidConnectionException e) {
                }
            }
        }
    }

    private void setZeroDelayAttribute(HashMap<Instance, FunctionComponent> instanceComponentMap) {
        for (Instance verilogInstance: instanceComponentMap.keySet()) {
            FunctionComponent component = instanceComponentMap.get(verilogInstance);
            if ((component != null) && (verilogInstance.zeroDelay)) {
                try {
                    component.setIsZeroDelay(true);
                } catch (ArgumentException e) {
                    LogUtils.logWarningLine("Component '" + verilogInstance.name + "': " + e.getMessage());
                }
            }
        }
    }

    private void setInitialState(Circuit circuit, Map<String, Wire> wires, Map<String, Boolean> signalStates) {
        // Set all signals first to 1 and then to 0, to make sure a switch and initiates switching of the neighbours.
        for (String signalName: wires.keySet()) {
            Wire wire = wires.get(signalName);
            if (wire.source != null) {
                wire.source.setInitToOne(true);
            }
        }
        for (String signalName: wires.keySet()) {
            Wire wire = wires.get(signalName);
            if (wire.source != null) {
                wire.source.setInitToOne(false);
            }
        }
        // Set all signals specified as high to 1.
        if (signalStates != null) {
            for (String signalName: wires.keySet()) {
                Wire wire = wires.get(signalName);
                if ((wire.source != null) && signalStates.containsKey(signalName)) {
                    boolean signalState = signalStates.get(signalName);
                    wire.source.setInitToOne(signalState);
                }
            }
        }
    }

    private HashMap<String, Module> getModuleMap(List<Module> modules) {
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

    private void mergeGroups(Circuit circuit, Set<List<Instance>> groups, HashMap<Instance, FunctionComponent> instanceComponentMap) {
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
            FunctionComponent complexComponent = mergeComponents(circuit, rootComponent, components);
            for (FunctionComponent component: components) {
                if (component == complexComponent) continue;
                circuit.remove(component);
            }
            // Prefix all the pins with underscore so there is no name clash on the subsequent round of renaming
            for (Contact contact: complexComponent.getContacts()) {
                circuit.setName(contact, "_" + contact.getName());
            }
            // Compact all the pins names
            int index = 0;
            for (Contact contact: complexComponent.getContacts()) {
                if (contact.isOutput()) {
                    circuit.setName(contact, getPrimitiveGatePinName(0));
                } else {
                    circuit.setName(contact, getPrimitiveGatePinName(++index));
                }
            }
        }
    }

    private FunctionComponent mergeComponents(Circuit circuit, FunctionComponent rootComponent, HashSet<FunctionComponent> components) {
        boolean done = false;
        do {
            HashSet<FunctionComponent> leafComponents = new HashSet<>();
            for (FunctionComponent component: components) {
                if (component == rootComponent) continue;
                for (MathNode node: CircuitUtils.getComponentPostset(circuit, component)) {
                    if (node != rootComponent) continue;
                    leafComponents.add(component);
                }
            }
            if (leafComponents.isEmpty()) {
                done = true;
            } else {
                FunctionComponent newComponent = mergeLeafComponents(circuit, rootComponent, leafComponents);
                components.remove(rootComponent);
                circuit.remove(rootComponent);
                rootComponent = newComponent;
            }
        } while (!done);
        return rootComponent;
    }

    private FunctionComponent mergeLeafComponents(Circuit circuit, FunctionComponent rootComponent, HashSet<FunctionComponent> leafComponents) {
        FunctionComponent component = null;
        FunctionContact rootOutputContact = getOutputContact(circuit, rootComponent);
        List<Contact> rootInputContacts = new LinkedList<>(rootComponent.getInputs());

        HashMap<Contact, Contact> newToOldContactMap = new HashMap<>();
        component = new FunctionComponent();
        circuit.add(component);
        FunctionContact outputContact = new FunctionContact(IOType.OUTPUT);
        outputContact.setInitToOne(rootOutputContact.getInitToOne());
        component.add(outputContact);
        circuit.setName(outputContact, PRIMITIVE_GATE_OUTPUT_NAME);
        newToOldContactMap.put(outputContact, rootOutputContact);

        List<BooleanFormula> leafSetFunctions = new LinkedList<>();
        for (Contact rootInputContact: rootInputContacts) {
            BooleanFormula leafSetFunction = null;
            for (FunctionComponent leafComponent: leafComponents) {
                FunctionContact leafOutputContact = getOutputContact(circuit, leafComponent);
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
        BooleanFormula setFunction = printFunctionSubstitution(rootSetFunction, rootInputContacts, leafSetFunctions);
        outputContact.setSetFunctionQuiet(setFunction);

        connectMergedComponent(circuit, component, rootComponent, newToOldContactMap);
        return component;
    }

    private BooleanFormula printFunctionSubstitution(BooleanFormula function, List<Contact> inputContacts, List<BooleanFormula> inputFunctions) {
        final BooleanFormula setFunction = BooleanUtils.dumbReplace(function, inputContacts, inputFunctions);
        if (CommonDebugSettings.getVerboseImport()) {
            LogUtils.logInfoLine("Expression substitution");
            LogUtils.logInfoLine("  Original: " + FormulaToString.toString(function));
            Iterator<Contact> contactIterator = inputContacts.iterator();
            Iterator<BooleanFormula> formulaIterator = inputFunctions.iterator();
            while (contactIterator.hasNext() && formulaIterator.hasNext()) {
                Contact contact = contactIterator.next();
                BooleanFormula formula = formulaIterator.next();
                LogUtils.logInfoLine("  Replacement: " + contact.getName() + " = " + FormulaToString.toString(formula));
            }
            LogUtils.logInfoLine("  Result: " + FormulaToString.toString(setFunction));
        }
        return setFunction;
    }

    private FunctionContact getOutputContact(Circuit circuit, FunctionComponent component) {
        Collection<Contact> outputContacts = component.getOutputs();
        if (outputContacts.size() != 1) {
            throw new RuntimeException("Cannot determin the output of component '" + circuit.getName(component) + "'.");
        }
        return (FunctionContact) outputContacts.iterator().next();
    }

    private void connectMergedComponent(Circuit circuit, FunctionComponent newComponent,
            FunctionComponent oldComponent, HashMap<Contact, Contact> newToOldContactMap) {

        FunctionContact oldOutputContact = getOutputContact(circuit, oldComponent);
        FunctionContact newOutputContact = getOutputContact(circuit, newComponent);
        for (Connection oldConnection: new HashSet<>(circuit.getConnections(oldOutputContact))) {
            if (oldConnection.getFirst() != oldOutputContact) continue;
            Node toNode = oldConnection.getSecond();
            circuit.remove(oldConnection);
            try {
                boolean hasNewContact = false;
                for (Contact newContact: newToOldContactMap.keySet()) {
                    Contact oldContact = newToOldContactMap.get(newContact);
                    if (toNode == oldContact) {
                        circuit.connect(newOutputContact, newContact);
                        hasNewContact = true;
                    }
                }
                if (!hasNewContact) {
                    circuit.connect(newOutputContact, toNode);
                }
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

}
