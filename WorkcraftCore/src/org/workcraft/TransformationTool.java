package org.workcraft;

import java.util.Collection;
import java.util.HashSet;

import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.util.WorkspaceUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public abstract class TransformationTool extends PromotedTool implements MenuOrdering {

    @Override
    public String getSection() {
        return "!   Transformations";  // 3 spaces - positions 2nd
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public void run(WorkspaceEntry we) {
        VisualModel visualModel = WorkspaceUtils.getAs(we.getModelEntry(), VisualModel.class);
        Collection<Node> nodes = collect(visualModel);
        if (!nodes.isEmpty()) {
            we.saveMemento();
            transform(visualModel, nodes);
            visualModel.selectNone();
        }
    }

    @Override
    public ModelEntry apply(ModelEntry me) {
        VisualModel visualModel = WorkspaceUtils.getAs(me, VisualModel.class);
        Collection<Node> nodes = collect(visualModel);
        transform(visualModel, nodes);
        return me;
    }

    public Collection<Node> collect(Model model) {
        Collection<Node> result = new HashSet<>();
        if (model instanceof VisualModel) {
            VisualModel visualModel = (VisualModel) model;
            result.addAll(visualModel.getSelection());
        }
        return result;
    }

    public void transform(Model model, Collection<Node> nodes) {
        for (Node node: nodes) {
            transform(model, node);
        }
    }

    public abstract void transform(Model model, Node node);

}
