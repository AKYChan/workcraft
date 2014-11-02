package org.workcraft.gui.graph.tools;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.workspace.WorkspaceEntry;

abstract public class AbstractContractorTool implements Tool {
	private final Framework framework;

	public AbstractContractorTool(Framework framework) {
		this.framework = framework;
	}

	@Override
	public String getDisplayName() {
		return "Contract selected nodes";
	}

	@Override
	public String getSection() {
		return "Transformations";
	}

	@Override
	public void run(WorkspaceEntry we) {
		final VisualModel model = we.getModelEntry().getVisualModel();
		if (model.getSelection().size() > 0) {
			we.saveMemento();
			contractSelection(model);
		}
	}

	private void contractSelection(VisualModel model) {
		for (Node cur: model.getSelection()) {
			if (cur instanceof VisualComponent) {
				for (Node pred: model.getPreset(cur)) {
					for (Node succ: model.getPostset(cur)) {
						try {
							model.connect(pred, succ);
						} catch (InvalidConnectionException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		model.deleteSelection();
	}

}
