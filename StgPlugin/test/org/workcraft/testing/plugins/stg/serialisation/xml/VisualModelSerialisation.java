package org.workcraft.testing.plugins.stg.serialisation.xml;

import org.junit.Test;
import org.workcraft.PluginProvider;
import org.workcraft.plugins.layout.RandomLayoutCommand;
import org.workcraft.plugins.serialisation.XMLModelDeserialiser;
import org.workcraft.plugins.serialisation.XMLModelSerialiser;
import org.workcraft.plugins.stg.Stg;
import org.workcraft.plugins.stg.StgDescriptor;
import org.workcraft.plugins.stg.VisualStg;
import org.workcraft.serialisation.DeserialisationResult;
import org.workcraft.serialisation.ReferenceProducer;
import org.workcraft.testing.plugins.stg.serialisation.SerialisationTestingUtils;
import org.workcraft.util.DataAccumulator;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class VisualModelSerialisation {

    @Test
    public void simpleSaveLoad() throws Exception {

        Stg stg = XMLSerialisationTestingUtils.createTestSTG1();
        VisualStg visualstg = new VisualStg(stg);

        RandomLayoutCommand layout = new RandomLayoutCommand();
        WorkspaceEntry we = new WorkspaceEntry(null);

        we.setModelEntry(new ModelEntry(new StgDescriptor(), visualstg));

        layout.run(we);

        // serialise
        PluginProvider mockPluginManager = XMLSerialisationTestingUtils.createMockPluginManager();

        XMLModelSerialiser serialiser = new XMLModelSerialiser(mockPluginManager);

        DataAccumulator mathData = new DataAccumulator();
        ReferenceProducer mathModelReferences = serialiser.serialise(stg, mathData, null);

        DataAccumulator visualData = new DataAccumulator();
        serialiser.serialise(visualstg, visualData, mathModelReferences);

        System.out.println(new String(mathData.getData()));
        System.out.println("---------------");
        System.out.println(new String(visualData.getData()));

        // deserialise
        XMLModelDeserialiser deserialiser = new XMLModelDeserialiser(mockPluginManager);

        DeserialisationResult mathResult = deserialiser.deserialise(mathData.getInputStream(), null, null);
        DeserialisationResult visualResult = deserialiser.deserialise(visualData.getInputStream(), mathResult.references, mathResult.model);

        SerialisationTestingUtils.compareNodes(visualstg.getRoot(), visualResult.model.getRoot());
    }
}
