package org.workcraft.plugins.fsm.converters;

import java.util.Map;

import org.workcraft.dom.math.MathNode;
import org.workcraft.gui.graph.converters.DefaultModelConverter;
import org.workcraft.plugins.fsm.State;
import org.workcraft.plugins.fsm.VisualFsm;
import org.workcraft.plugins.graph.Vertex;
import org.workcraft.plugins.graph.VisualGraph;

public class GraphToFsmConverter extends DefaultModelConverter<VisualGraph, VisualFsm> {

    public GraphToFsmConverter(VisualGraph srcModel, VisualFsm dstModel) {
        super(srcModel, dstModel);
    }

    @Override
    public Map<Class<? extends MathNode>, Class<? extends MathNode>> getComponentClassMap() {
        Map<Class<? extends MathNode>, Class<? extends MathNode>> result = super.getComponentClassMap();
        result.put(Vertex.class, State.class);
        return result;
    }

}
