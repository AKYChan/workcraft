package org.workcraft.plugins.mpsat;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.utils.DesktopApi;
import org.workcraft.plugins.mpsat.commands.MpsatPlaceRedundancyVerificationCommand;
import org.workcraft.plugins.pcomp.PcompSettings;
import org.workcraft.plugins.punf.PunfSettings;
import org.workcraft.utils.PackageUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

public class MpsatPlaceRedundancyVerificationCommandTest {

    @BeforeClass
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
        switch (DesktopApi.getOs()) {
        case LINUX:
            PcompSettings.setCommand("dist-template/linux/tools/UnfoldingTools/pcomp");
            PunfSettings.setCommand("dist-template/linux/tools/UnfoldingTools/punf");
            MpsatVerificationSettings.setCommand("dist-template/linux/tools/UnfoldingTools/mpsat");
            break;
        case MACOS:
            PcompSettings.setCommand("dist-template/osx/Contents/Resources/tools/UnfoldingTools/pcomp");
            PunfSettings.setCommand("dist-template/osx/Contents/Resources/tools/UnfoldingTools/punf");
            MpsatVerificationSettings.setCommand("dist-template/osx/Contents/Resources/tools/UnfoldingTools/mpsat");
            break;
        case WINDOWS:
            PcompSettings.setCommand("dist-template\\windows\\tools\\UnfoldingTools\\pcomp.exe");
            PunfSettings.setCommand("dist-template\\windows\\tools\\UnfoldingTools\\punf.exe");
            MpsatVerificationSettings.setCommand("dist-template\\windows\\tools\\UnfoldingTools\\mpsat.exe");
            break;
        default:
        }
    }

    @Test
    public void testPhilosophersPlaceRedundancyVerification() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "philosophers-deadlock.pn.work");
        testPlaceRedundancyVerificationCommands(workName, new String[]{"north.p4", "south.p5"}, true);
        testPlaceRedundancyVerificationCommands(workName, new String[]{"fork1_free", "north.p4"}, false);
    }

    @Test
    public void testVmePlaceRedundancyVerification() throws DeserialisationException {
        String workName = PackageUtils.getPackagePath(getClass(), "vme.stg.work");
        testPlaceRedundancyVerificationCommands(workName, new String[]{"<d+,dtack+>"}, false);
        testPlaceRedundancyVerificationCommands(workName, new String[]{"<d+/100,dtack+/100>"}, null);
    }

    private void testPlaceRedundancyVerificationCommands(String workName, String[] refs, Boolean redundant)
            throws DeserialisationException {

        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);
        WorkspaceEntry we = framework.loadWork(url.getFile());

        MpsatPlaceRedundancyVerificationCommand command = new MpsatPlaceRedundancyVerificationCommand() {
            @Override
            protected HashSet<String> getSelectedPlaces(WorkspaceEntry we) {
                return new HashSet<>(Arrays.asList(refs));
            }
        };

        Assert.assertEquals(redundant, command.execute(we));
    }

}
