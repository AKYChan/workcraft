package org.workcraft.plugins.circuit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Config;
import org.workcraft.Framework;

public class CircuitSettingsTest {

    @BeforeClass
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
        framework.resetConfig();
    }

    @Test
    public void circuitSettingsTest() {
        final Framework framework = Framework.getInstance();
        String prefix = "CircuitSettings";

        Assert.assertEquals(Config.toString(CircuitSettings.getShowContacts()),
                framework.getConfigVar(prefix + ".showContacts", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getShowZeroDelayNames()),
                framework.getConfigVar(prefix + ".showZeroDelayNames", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getActiveWireColor()),
                framework.getConfigVar(prefix + ".activeWireColor", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getInactiveWireColor()),
                framework.getConfigVar(prefix + ".inactiveWireColor", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getConflictInitGateColor()),
                framework.getConfigVar(prefix + ".conflictInitGateColor", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getForcedInitGateColor()),
                framework.getConfigVar(prefix + ".forcedInitGateColor", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getPropagatedInitGateColor()),
                framework.getConfigVar(prefix + ".propagatedInitGateColor", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getBorderWidth()),
                framework.getConfigVar(prefix + ".borderWidth", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getWireWidth()),
                framework.getConfigVar(prefix + ".wireWidth", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getSimplifyStg()),
                framework.getConfigVar(prefix + ".simplifyStg", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getGateLibrary()),
                framework.getConfigVar(prefix + ".gateLibrary", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getSubstitutionLibrary()),
                framework.getConfigVar(prefix + ".substitutionLibrary", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getMutexData()),
                framework.getConfigVar(prefix + ".mutexData", false));

        Assert.assertEquals(Config.toString(CircuitSettings.getBusSuffix()),
                framework.getConfigVar(prefix + ".busSuffix", false));
    }

}
