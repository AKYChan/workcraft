package org.workcraft.plugins.stg;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.plugins.builtin.commands.AnonymiseTransformationCommand;
import org.workcraft.utils.PackageUtils;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.utils.WorkspaceUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class AnonymiseCommandTests {

    @BeforeClass
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
    }

    @Test
    public void testVmeStgAnonymiseCommands() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testStgAnonymiseCommands(workName, new String[]{"p0", "p1", "p2", "p3"}, new String[]{"in0", "in1", "in2", "out0", "out1", "out2"});
    }

    private void testStgAnonymiseCommands(String workName, String[] placeRefs, String[] signalRefs)
            throws DeserialisationException {

        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);
        WorkspaceEntry we = framework.loadWork(url.getFile());

        Stg stg = WorkspaceUtils.getAs(we, Stg.class);

        AnonymiseTransformationCommand command = new AnonymiseTransformationCommand();
        command.execute(we);

        HashSet<String> expectedPlacesRefs = new HashSet<>(Arrays.asList(placeRefs));
        HashSet<String> actualPlaceRefs = new HashSet<>();
        for (StgPlace place : stg.getPlaces()) {
            if (!place.isImplicit()) {
                actualPlaceRefs.add(stg.getNodeReference(place));
            }
        }
        Assert.assertEquals(expectedPlacesRefs, actualPlaceRefs);

        HashSet<String> expectedSignalRefs = new HashSet<>(Arrays.asList(signalRefs));
        Collection<String> actualSignalRefs = stg.getSignalReferences();
        Assert.assertEquals(expectedSignalRefs, actualSignalRefs);

        framework.closeWork(we);
    }

}
