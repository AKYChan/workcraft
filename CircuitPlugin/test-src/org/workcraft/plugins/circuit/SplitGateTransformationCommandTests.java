package org.workcraft.plugins.circuit;

import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.plugins.circuit.commands.SplitGateTransformationCommand;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class SplitGateTransformationCommandTests {

    @BeforeClass
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
    }

    @Test
    public void testVmeSplitGateTransformationCommand() throws DeserialisationException {
        testSplitGateTransformationCommand("org/workcraft/plugins/circuit/vme-tm.circuit.work", 15, 18);
    }

    private void testSplitGateTransformationCommand(String work, int expMappedGateCount, int expUnmappedGateCount)
            throws DeserialisationException {

        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(work);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        VisualCircuit circuit = WorkspaceUtils.getAs(we, VisualCircuit.class);

        circuit.selectAll();

        SplitGateTransformationCommand command = new SplitGateTransformationCommand();
        command.execute(we);

        int dstMappedGateCount = 0;
        int dstUnmappedGateCount = 0;
        for (VisualFunctionComponent component: circuit.getVisualFunctionComponents()) {
            if (component.isMapped()) {
                dstMappedGateCount++;
            } else {
                dstUnmappedGateCount++;
            }
        }

        Assert.assertEquals(expMappedGateCount, dstMappedGateCount);
        Assert.assertEquals(expUnmappedGateCount, dstUnmappedGateCount);

        framework.closeWork(we);
    }

}
