package org.workcraft.plugins.circuit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.gui.DesktopApi;
import org.workcraft.plugins.circuit.interop.VerilogFormat;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class CircuitImportExportTests {

    private static final String[] TEST_CIRCUIT_WORKS = {
        "org/workcraft/plugins/circuit/buffer.circuit.work",
        "org/workcraft/plugins/circuit/celement.circuit.work",
        "org/workcraft/plugins/circuit/buffer-tm.circuit.work",
        "org/workcraft/plugins/circuit/celement-tm.circuit.work",
        "org/workcraft/plugins/circuit/vme-tm.circuit.work",
    };

    @BeforeClass
    public static void initPlugins() {
        final Framework framework = Framework.getInstance();
        framework.initPlugins();
        switch (DesktopApi.getOs()) {
        case LINUX:
            CircuitSettings.setGateLibrary("../dist-template/linux/libraries/workcraft.lib");
            break;
        case MACOS:
            CircuitSettings.setGateLibrary("../dist-template/osx/Contents/Resources/libraries/workcraft.lib");
            break;
        case WINDOWS:
            CircuitSettings.setGateLibrary("..\\dist-template\\windows\\libraries\\workcraft.lib");
            break;
        default:
        }
    }

    @Test
    public void testCircuitImport() throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        for (String testCircuitWork: TEST_CIRCUIT_WORKS) {
            URL wUrl = classLoader.getResource(testCircuitWork);

            WorkspaceEntry wWe = framework.loadWork(wUrl.getFile());
            Set<String> wInputs = new HashSet<>();
            Set<String> wOutputs = new HashSet<>();
            Set<String> wGates = new HashSet<>();
            countCircuitNodes(wWe, wInputs, wOutputs, wGates);

            Set<String> vInputs = new HashSet<>();
            Set<String> vOutputs = new HashSet<>();
            Set<String> vGates = new HashSet<>();
            WorkspaceEntry vWe = null;
            try {
                File vFile = File.createTempFile("workcraft-", ".v");
                vFile.deleteOnExit();
                framework.exportModel(wWe.getModelEntry(), vFile, VerilogFormat.getInstance());
                vWe = framework.loadWork(vFile);
                countCircuitNodes(vWe, vInputs, vOutputs, vGates);
            } catch (IOException | SerialisationException e) {
            }

            Assert.assertEquals(wInputs, vInputs);
            Assert.assertEquals(wOutputs, vOutputs);
            Assert.assertEquals(wGates, vGates);

            framework.closeWork(wWe);
            framework.closeWork(vWe);
        }
    }

    private void countCircuitNodes(WorkspaceEntry we, Set<String> inputs, Set<String> outputs, Set<String> gates) {
        Circuit circuit = WorkspaceUtils.getAs(we, Circuit.class);
        for (Contact port: circuit.getPorts()) {
            if (port.isInput()) {
                inputs.add(port.getName());
            }
            if (port.isOutput()) {
                outputs.add(port.getName());
            }
        }
        for (FunctionComponent component: circuit.getFunctionComponents()) {
            gates.add(component.getModule() + " " + component.getName());
        }
    }

}
