package org.workcraft.plugins.petrify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.plugins.petrify.commands.CscConflictResolutionCommand;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.utils.MutexUtils;
import org.workcraft.utils.BackendUtils;
import org.workcraft.utils.PackageUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class CscConflictResolutionCommandTests {

    @BeforeAll
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
        PetrifySettings.setCommand(BackendUtils.getTemplateToolPath("PetrifyTools", "petrify"));
    }

    @Test
    public void vmeCscConflictResolution() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testCscConflictResolutionCommand(workName, new String[] {"csc0"});
    }

    @Test
    public void arbitrationCscConflictResolution() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "arbitration-3-hierarchy.stg.work");
        testCscConflictResolutionCommand(workName, new String[] {});
    }

    @Test
    public void togglePageCsc0ConflictResolution() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "toggle-page_csc0.stg.work");
        testCscConflictResolutionCommand(workName, new String[] {"csc0"});
    }

    private void testCscConflictResolutionCommand(String workName, String[] cscSignals)
            throws DeserialisationException {

        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);
        WorkspaceEntry srcWe = framework.loadWork(url.getFile());

        Stg srcStg = WorkspaceUtils.getAs(srcWe, Stg.class);
        Set<String> srcInputs = srcStg.getSignalReferences(Signal.Type.INPUT);
        Set<String> srcInternals = srcStg.getSignalReferences(Signal.Type.INTERNAL);
        Set<String> srcOutputs = srcStg.getSignalReferences(Signal.Type.OUTPUT);
        Set<String> srcMutexes = MutexUtils.getMutexPlaceReferences(srcStg);

        CscConflictResolutionCommand command = new CscConflictResolutionCommand();
        WorkspaceEntry dstWe = command.execute(srcWe);

        Stg dstStg = WorkspaceUtils.getAs(dstWe, Stg.class);
        Set<String> dstInputs = dstStg.getSignalReferences(Signal.Type.INPUT);
        Set<String> dstInternals = dstStg.getSignalReferences(Signal.Type.INTERNAL);
        Set<String> dstOutputs = dstStg.getSignalReferences(Signal.Type.OUTPUT);
        Set<String> dstMutexes = MutexUtils.getMutexPlaceReferences(dstStg);

        Set<String> expInternals = new HashSet<>();
        expInternals.addAll(srcInternals);
        if (cscSignals != null) {
            for (String cscSignal: cscSignals) {
                expInternals.add(cscSignal);
            }
        }

        Assertions.assertEquals(srcInputs, dstInputs);
        Assertions.assertEquals(expInternals, dstInternals);
        Assertions.assertEquals(srcOutputs, dstOutputs);
        Assertions.assertEquals(srcMutexes.size(), dstMutexes.size());
    }

}
