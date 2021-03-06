package org.workcraft.plugins.atacs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.workcraft.Framework;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.plugins.atacs.commands.ComplexGateSynthesisCommand;
import org.workcraft.utils.BackendUtils;
import org.workcraft.utils.PackageUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.net.URL;

public class MissingToolTests {

    @BeforeAll
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
    }

    @Test
    public void testMissingAtacsVerification() throws DeserialisationException {
        AtacsSettings.setCommand(BackendUtils.getTemplateToolPath("ATACS", "atacs-missing"));
        testMissingTool();
    }

    private void testMissingTool() throws DeserialisationException {
        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        String workName = PackageUtils.getPackagePath(getClass(), "buffer.stg.work");
        URL url = classLoader.getResource(workName);
        WorkspaceEntry we = framework.loadWork(url.getFile());
        ComplexGateSynthesisCommand command = new ComplexGateSynthesisCommand();
        Assertions.assertNull(command.execute(we));
    }

}
