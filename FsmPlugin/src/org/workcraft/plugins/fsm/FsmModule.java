package org.workcraft.plugins.fsm;

import org.workcraft.Framework;
import org.workcraft.Initialiser;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Tool;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.graph.tools.AbstractContractorTool;
import org.workcraft.plugins.fsm.serialisation.EventDeserialiser;
import org.workcraft.plugins.fsm.serialisation.EventSerialiser;
import org.workcraft.plugins.fsm.tools.DeadlockCheckerTool;
import org.workcraft.plugins.fsm.tools.DeterminismCheckerTool;
import org.workcraft.plugins.fsm.tools.DgToFsmConverterTool;
import org.workcraft.plugins.fsm.tools.FsmToDgConverterTool;
import org.workcraft.plugins.fsm.tools.FsmToPnConverterTool;
import org.workcraft.plugins.fsm.tools.ReachabilityCheckerTool;
import org.workcraft.plugins.fsm.tools.ReversibilityCheckerTool;
import org.workcraft.serialisation.xml.XMLDeserialiser;
import org.workcraft.serialisation.xml.XMLSerialiser;
import org.workcraft.workspace.WorkspaceEntry;

public class FsmModule  implements Module {

	@Override
	public void init() {
		final Framework framework = Framework.getInstance();
		final PluginManager pm = framework.getPluginManager();

		pm.registerClass(ModelDescriptor.class, FsmModelDescriptor.class);

		pm.registerClass(XMLSerialiser.class, EventSerialiser.class);
		pm.registerClass(XMLDeserialiser.class, EventDeserialiser.class);

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new FsmToDgConverterTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new DgToFsmConverterTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new FsmToPnConverterTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new DeadlockCheckerTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new DeterminismCheckerTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new ReachabilityCheckerTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new ReversibilityCheckerTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new AbstractContractorTool() {
					@Override
					public boolean isApplicableTo(WorkspaceEntry we) {
						return we.getModelEntry().getMathModel() instanceof Fsm;
					}
				};
			}
		});
	}

	@Override
	public String getDescription() {
		return "Finite State Machine";
	}

}
