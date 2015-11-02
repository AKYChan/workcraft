package org.workcraft.plugins.dfs;

import org.workcraft.CompatibilityManager;
import org.workcraft.Framework;
import org.workcraft.Initialiser;
import org.workcraft.Module;
import org.workcraft.PluginManager;
import org.workcraft.Tool;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.graph.tools.AbstractContractorTool;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.plugins.dfs.tools.CheckDataflowDeadlockTool;
import org.workcraft.plugins.dfs.tools.CheckDataflowHazardTool;
import org.workcraft.plugins.dfs.tools.CheckDataflowTool;
import org.workcraft.plugins.dfs.tools.ComponentMergerTool;
import org.workcraft.plugins.dfs.tools.StgGeneratorTool;
import org.workcraft.plugins.dfs.tools.WaggingGeneratorTool;
import org.workcraft.workspace.WorkspaceEntry;

public class DfsModule implements Module {

	@Override
	public String getDescription() {
		return "Dataflow Structure";
	}

	@Override
	public void init() {
		initPluginManager();
		initCompatibilityManager();
	}

	private void initPluginManager() {
		final Framework framework = Framework.getInstance();
		final PluginManager pm = framework.getPluginManager();

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new StgGeneratorTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new WaggingGeneratorTool() {
					@Override
					public String getDisplayName() {
						return "2-way wagging";
					}
					@Override
					public int getWayCount() {
						return 2;
					}
				};
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new WaggingGeneratorTool() {
					@Override
					public String getDisplayName() {
						return "3-way wagging";
					}
					@Override
					public int getWayCount() {
						return 3;
					}
				};
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new WaggingGeneratorTool() {
					@Override
					public String getDisplayName() {
						return "4-way wagging";
					}
					@Override
					public int getWayCount() {
						return 4;
					}
				};
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new WaggingGeneratorTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new AbstractContractorTool() {
					@Override
					public boolean isApplicableTo(WorkspaceEntry we) {
						return we.getModelEntry().getMathModel() instanceof Dfs;
					}
				};
			}
		});


		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new CheckDataflowDeadlockTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new CheckDataflowHazardTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new CheckDataflowTool();
			}
		});

		pm.registerClass(Tool.class, new Initialiser<Tool>() {
			@Override
			public Tool create() {
				return new ComponentMergerTool();
			}
		});

		pm.registerClass(ModelDescriptor.class, DfsDescriptor.class);
		pm.registerClass(Settings.class, DfsSettings.class);
	}

	private void initCompatibilityManager() {
		final Framework framework = Framework.getInstance();
		final CompatibilityManager cm = framework.getCompatibilityManager();

		cm.registerMetaReplacement(
				"<descriptor class=\"org.workcraft.plugins.dfs.DfsModelDescriptor\"/>",
				"<descriptor class=\"org.workcraft.plugins.dfs.DfsDescriptor\"/>");
	}
}
