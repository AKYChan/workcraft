package org.workcraft.plugins.stg;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Framework;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.plugins.stg.commands.*;
import org.workcraft.util.PackageUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class StgTransformationCommandTests {

    @BeforeClass
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
    }

    @Test
    public void testCelementMirrorSignalTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "celement.stg.work");
        testMirrorSignalTransformationCommand(workName);
    }

    @Test
    public void testBuckMirrorSignalTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "buck.stg.work");
        testMirrorSignalTransformationCommand(workName);
    }

    @Test
    public void testVmeMirrorSignalTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testMirrorSignalTransformationCommand(workName);
    }

    private void testMirrorSignalTransformationCommand(String workName) throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        Stg stg = WorkspaceUtils.getAs(we, Stg.class);
        Set<String> srcInputs = stg.getSignalReferences(Signal.Type.INPUT);
        Set<String> srcOutputs = stg.getSignalReferences(Signal.Type.OUTPUT);
        Set<String> srcInternals = stg.getSignalReferences(Signal.Type.INTERNAL);

        MirrorSignalTransformationCommand command = new MirrorSignalTransformationCommand();
        command.execute(we);
        Set<String> dstInputs = stg.getSignalReferences(Signal.Type.INPUT);
        Set<String> dstOutputs = stg.getSignalReferences(Signal.Type.OUTPUT);
        Set<String> dstInternals = stg.getSignalReferences(Signal.Type.INTERNAL);

        framework.closeWork(we);
        Assert.assertEquals(srcInputs, dstOutputs);
        Assert.assertEquals(srcOutputs, dstInputs);
        Assert.assertEquals(srcInternals, dstInternals);
    }

    @Test
    public void testCelementMirrorTransitionTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "celement.stg.work");
        testMirrorTransitionTransformationCommand(workName);
    }

    @Test
    public void testBuckMirrorTransitionTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "buck.stg.work");
        testMirrorTransitionTransformationCommand(workName);
    }

    @Test
    public void testVmeMirrorTransitionTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testMirrorTransitionTransformationCommand(workName);
    }

    private void testMirrorTransitionTransformationCommand(String workName) throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        Stg stg = WorkspaceUtils.getAs(we, Stg.class);
        int srcMinusCount = 0;
        int srcPlusCount = 0;
        int srcToggleCount = 0;
        for (SignalTransition srcTransition : stg.getSignalTransitions()) {
            switch (srcTransition.getDirection()) {
            case MINUS:
                srcMinusCount++;
                break;
            case PLUS:
                srcPlusCount++;
                break;
            case TOGGLE:
                srcToggleCount++;
                break;
            }
        }

        MirrorTransitionTransformationCommand command = new MirrorTransitionTransformationCommand();
        command.execute(we);
        int dstMinusCount = 0;
        int dstPlusCount = 0;
        int dstToggleCount = 0;
        for (SignalTransition dstTransition : stg.getSignalTransitions()) {
            switch (dstTransition.getDirection()) {
            case MINUS:
                dstMinusCount++;
                break;
            case PLUS:
                dstPlusCount++;
                break;
            case TOGGLE:
                dstToggleCount++;
                break;
            }
        }

        framework.closeWork(we);
        Assert.assertEquals(srcMinusCount, dstPlusCount);
        Assert.assertEquals(srcPlusCount, dstMinusCount);
        Assert.assertEquals(srcToggleCount, dstToggleCount);
    }

    @Test
    public void testCelementPlaceTransformationCommands() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "celement.stg.work");
        testPlaceTransformationCommands(workName);
    }

    @Test
    public void testBuckPlaceTransformationCommands() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "buck.stg.work");
        testPlaceTransformationCommands(workName);
    }

    @Test
    public void testVmePlaceTransformationCommands() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testPlaceTransformationCommands(workName);
    }

    private void testPlaceTransformationCommands(String workName) throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        VisualStg stg = WorkspaceUtils.getAs(we, VisualStg.class);
        int srcPlaces = stg.getVisualPlaces().size();
        int srcImplicitPlaceArcs = stg.getVisualImplicitPlaceArcs().size();
        int srcSignalTransitions = stg.getVisualSignalTransitions().size();
        int srcDummyTransitions = stg.getVisualDummyTransitions().size();

        ExplicitPlaceTransformationCommand command1 = new ExplicitPlaceTransformationCommand();
        command1.execute(we);
        int expPlaces = stg.getVisualPlaces().size();
        int expImplicitPlaceArcs = stg.getVisualImplicitPlaceArcs().size();
        int expSignalTransitions = stg.getVisualSignalTransitions().size();
        int expDummyTransitions = stg.getVisualDummyTransitions().size();

        Assert.assertEquals(srcPlaces + srcImplicitPlaceArcs, expPlaces + expImplicitPlaceArcs);
        Assert.assertEquals(srcSignalTransitions, expSignalTransitions);
        Assert.assertEquals(srcDummyTransitions, expDummyTransitions);

        ImplicitPlaceTransformationCommand command2 = new ImplicitPlaceTransformationCommand();
        command2.execute(we);
        int impPlaces = stg.getVisualPlaces().size();
        int impImplicitPlaceArcs = stg.getVisualImplicitPlaceArcs().size();
        int impSignalTransitions = stg.getVisualSignalTransitions().size();
        int impDummyTransitions = stg.getVisualDummyTransitions().size();

        framework.closeWork(we);
        Assert.assertEquals(srcPlaces + srcImplicitPlaceArcs, impPlaces + impImplicitPlaceArcs);
        Assert.assertEquals(srcSignalTransitions, impSignalTransitions);
        Assert.assertEquals(srcDummyTransitions, impDummyTransitions);
    }

    @Test
    public void testHandshakes2ExpandHandshakeTransformationCommand() throws IOException, DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "handshakes-2.stg.work");
        testExpandHandshakeTransformationCommand(workName);
    }

    @Test
    public void testHandshakes3ExpandHandshakeTransformationCommand() throws IOException, DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "handshakes-3.stg.work");
        testExpandHandshakeTransformationCommand(workName);
    }

    private void testExpandHandshakeTransformationCommand(String workName) throws IOException, DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        VisualStg stg = WorkspaceUtils.getAs(we, VisualStg.class);
        int srcPlaces = stg.getVisualPlaces().size();
        int srcImplicitPlaceArcs = stg.getVisualImplicitPlaceArcs().size();
        int srcSignalTransitions = stg.getVisualSignalTransitions().size();
        int srcDummyTransitions = stg.getVisualDummyTransitions().size();
        int srcConnections = stg.getVisualConnections().size();

        stg.selectAll();
        ExpandHandshakeReqAckTransformationCommand command = new ExpandHandshakeReqAckTransformationCommand();
        command.execute(we);
        int dstPlaces = stg.getVisualPlaces().size();
        int dstImplicitPlaceArcs = stg.getVisualImplicitPlaceArcs().size();
        int dstSignalTransitions = stg.getVisualSignalTransitions().size();
        int dstDummyTransitions = stg.getVisualDummyTransitions().size();
        int dstConnections = stg.getVisualConnections().size();

        framework.closeWork(we);
        Assert.assertEquals(srcPlaces, dstPlaces);
        Assert.assertEquals(srcSignalTransitions * 2, dstSignalTransitions);
        Assert.assertEquals(srcDummyTransitions, dstDummyTransitions);
        Assert.assertEquals(srcImplicitPlaceArcs + srcSignalTransitions, dstImplicitPlaceArcs);
        Assert.assertEquals(srcConnections + srcSignalTransitions, dstConnections);
    }

    @Test
    public void testVmeSelectAllSignalTransitionsTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testSelectAllSignalTransitionsTransformationCommand(workName, new String[]{"dsr+", "dtack+/1"}, 5);
    }

    private void testSelectAllSignalTransitionsTransformationCommand(String workName, String[] refs, int expCount) throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        VisualStg stg = WorkspaceUtils.getAs(we, VisualStg.class);
        selectVisualComponentsByMathRefs(stg, refs);

        SelectAllSignalTransitionsTransformationCommand command = new SelectAllSignalTransitionsTransformationCommand();
        command.execute(we);
        int count = stg.getSelection().size();

        framework.closeWork(we);
        Assert.assertEquals(expCount, count);
    }

    @Test
    public void testVmeSignalToDummyTransitionTransformationCommand() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testSignalToDummyTransitionTransformationCommand(workName, new String[]{"dsw+", "dtack+/1"});
    }

    private void testSignalToDummyTransitionTransformationCommand(String workName, String[] refs) throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);

        WorkspaceEntry we = framework.loadWork(url.getFile());
        VisualStg stg = WorkspaceUtils.getAs(we, VisualStg.class);

        int srcSignalTransitionCount = stg.getVisualSignalTransitions().size();
        int srcDummyTransitionCount = stg.getVisualDummyTransitions().size();

        selectVisualComponentsByMathRefs(stg, refs);

        int selectionCount = stg.getSelection().size();

        SignalToDummyTransitionTransformationCommand command = new SignalToDummyTransitionTransformationCommand();
        command.execute(we);

        int dstSignalTransitionCount = stg.getVisualSignalTransitions().size();
        int dstDummyTransitionCount = stg.getVisualDummyTransitions().size();

        framework.closeWork(we);
        Assert.assertEquals(srcDummyTransitionCount + selectionCount, dstDummyTransitionCount);
        Assert.assertEquals(srcSignalTransitionCount - selectionCount, dstSignalTransitionCount);
    }

    private void selectVisualComponentsByMathRefs(VisualStg stg, String[] refs) {
        stg.selectNone();
        for (String ref: refs) {
            VisualComponent t = stg.getVisualComponentByMathReference(ref, VisualComponent.class);
            if (t != null) {
                stg.addToSelection(t);
            }
        }
    }

}
