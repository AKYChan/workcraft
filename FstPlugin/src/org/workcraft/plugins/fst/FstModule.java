package org.workcraft.plugins.fst;

import org.workcraft.CompatibilityManager;
import org.workcraft.Framework;
import org.workcraft.Initialiser;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Tool;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.interop.Exporter;
import org.workcraft.interop.Importer;
import org.workcraft.plugins.fst.interop.DotGExporter;
import org.workcraft.plugins.fst.interop.DotGImporter;
import org.workcraft.plugins.fst.serialisation.DotGSerialiser;
import org.workcraft.plugins.fst.tools.FsmToFstConverterTool;
import org.workcraft.plugins.fst.tools.FstToFsmConverterTool;
import org.workcraft.plugins.fst.tools.FstToStgConverterTool;
import org.workcraft.plugins.fst.tools.PetriToFsmConverterTool;
import org.workcraft.plugins.fst.tools.StgToFstConverterTool;
import org.workcraft.serialisation.ModelSerialiser;

public class FstModule implements Module {

    private final class StgToBinaryFstConverterTool extends StgToFstConverterTool {
        @Override
        public boolean isBinary() {
            return true;
        }
    }

    @Override
    public String getDescription() {
        return "Finite State Transducer";
    }

    @Override
    public void init() {
        initPluginManager();
        initCompatibilityManager();
    }

    private void initPluginManager() {
        final Framework framework = Framework.getInstance();
        final PluginManager pm = framework.getPluginManager();

        pm.registerClass(ModelDescriptor.class, FstDescriptor.class);

        pm.registerClass(Exporter.class, DotGExporter.class);
        pm.registerClass(Importer.class, DotGImporter.class);

        pm.registerClass(ModelSerialiser.class, DotGSerialiser.class);

        pm.registerClass(Tool.class, StgToFstConverterTool.class);
        pm.registerClass(Tool.class, FstToStgConverterTool.class);
        pm.registerClass(Tool.class, PetriToFsmConverterTool.class);
        pm.registerClass(Tool.class, FsmToFstConverterTool.class);
        pm.registerClass(Tool.class, FstToFsmConverterTool.class);

        pm.registerClass(Tool.class, new Initialiser<Tool>() {
            @Override
            public Tool create() {
                return new StgToBinaryFstConverterTool();
            }
        });
    }

    private void initCompatibilityManager() {
        final Framework framework = Framework.getInstance();
        final CompatibilityManager cm = framework.getCompatibilityManager();

        cm.registerMetaReplacement(
                "<descriptor class=\"org.workcraft.plugins.fst.FstModelDescriptor\"/>",
                "<descriptor class=\"org.workcraft.plugins.fst.FstDescriptor\"/>");
    }

}
