package org.workcraft.plugins.petri;

import org.workcraft.CompatibilityManager;
import org.workcraft.Framework;
import org.workcraft.Initialiser;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Tool;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.plugins.petri.tools.TransitionContractorTool;

public class PetriNetModule implements Module {

	@Override
	public String getDescription() {
		return "Petri Net";
	}

	@Override
	public void init() {
		initPluginManager();
		initCompatibilityManager();
	}

	private void initPluginManager() {
		final Framework framework = Framework.getInstance();
		final PluginManager pm = framework.getPluginManager();
		pm.registerClass(ModelDescriptor.class, PetriNetDescriptor.class);

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new TransitionContractorTool();
			}
		});
	}

	private void initCompatibilityManager() {
		final Framework framework = Framework.getInstance();
		final CompatibilityManager cm = framework.getCompatibilityManager();

		cm.registerMetaReplacement(
				"<descriptor class=\"org.workcraft.plugins.petri.PetriNetModelDescriptor\"/>",
				"<descriptor class=\"org.workcraft.plugins.petri.PetriNetDescriptor\"/>");
	}

}
