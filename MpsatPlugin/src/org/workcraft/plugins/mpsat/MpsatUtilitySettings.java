/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.mpsat;
import java.util.LinkedList;
import java.util.List;

import org.workcraft.Config;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.gui.propertyeditor.Settings;
import org.workcraft.plugins.mpsat.MpsatSettings.SolutionMode;

public class MpsatUtilitySettings implements Settings {
	private static final LinkedList<PropertyDescriptor> properties = new LinkedList<PropertyDescriptor>();
	private static final String prefix = "Tools.mpsat";

	private static final String keyCommand = prefix + ".command";
	private static final String keySolutionMode = prefix + ".solutionMode";
	private static final String keyExtraArgs = prefix + ".args";
	private static final String keyUsePnmlUnfolding = prefix + ".usePnmlUnfolding";
	private static final String keyDebugReach = prefix + ".debugReach";

	private static final String defaultCommand = "mpsat";
	private static final SolutionMode defaultSolutionMode = SolutionMode.MINIMUM_COST;
	private static final String defaultExtraArgs = "";
	private static final Boolean defaultUsePnmlUnfolding = true;
	private static final Boolean defaultDebugReach = false;

	private static String command = defaultCommand;
	private static SolutionMode solutionMode = defaultSolutionMode;
	private static String extraArgs = defaultExtraArgs;
	private static Boolean usePnmlUnfolding = defaultUsePnmlUnfolding;
	private static Boolean debugReach = defaultDebugReach;

	public MpsatUtilitySettings() {
		properties.add(new PropertyDeclaration<MpsatUtilitySettings, String>(
				this, "MPSat command", String.class, true, false, false) {
			protected void setter(MpsatUtilitySettings object, String value) {
				setCommand(value);
			}
			protected String getter(MpsatUtilitySettings object) {
				return getCommand();
			}
		});

		properties.add(new PropertyDeclaration<MpsatUtilitySettings, SolutionMode>(
				this, "Check mode", SolutionMode.class, true, false, false) {
			protected void setter(MpsatUtilitySettings object, SolutionMode value) {
				setSolutionMode(value);
			}
			protected SolutionMode getter(MpsatUtilitySettings object) {
				return getSolutionMode();
			}
		});

		properties.add(new PropertyDeclaration<MpsatUtilitySettings, String>(
				this, "Additional arguments", String.class, true, false, false) {
			protected void setter(MpsatUtilitySettings object, String value) {
				setExtraArgs(value);
			}
			protected String getter(MpsatUtilitySettings object) {
				return getExtraArgs();
			}
		});

		properties.add(new PropertyDeclaration<MpsatUtilitySettings, Boolean>(
				this, "Use PNML-based unfolding (where possible)", Boolean.class, true, false, false) {
			protected void setter(MpsatUtilitySettings object, Boolean value) {
				setUsePnmlUnfolding(value);
			}
			protected Boolean getter(MpsatUtilitySettings object) {
				return getUsePnmlUnfolding();
			}
		});

		properties.add(new PropertyDeclaration<MpsatUtilitySettings, Boolean>(
				this, "Print out Reach expressions (debug)", Boolean.class, true, false, false) {
			protected void setter(MpsatUtilitySettings object, Boolean value) {
				setDebugReach(value);
			}
			protected Boolean getter(MpsatUtilitySettings object) {
				return getDebugReach();
			}
		});
	}

	@Override
	public List<PropertyDescriptor> getDescriptors() {
		return properties;
	}

	@Override
	public void load(Config config) {
		setCommand(config.getString(keyCommand, defaultCommand));
		setSolutionMode(config.getEnum(keySolutionMode, SolutionMode.class, defaultSolutionMode));
		setExtraArgs(config.getString(keyExtraArgs, defaultExtraArgs));
		setUsePnmlUnfolding(config.getBoolean(keyUsePnmlUnfolding, defaultUsePnmlUnfolding));
		setDebugReach(config.getBoolean(keyDebugReach, defaultDebugReach));
	}

	@Override
	public void save(Config config) {
		config.set(keyCommand, getCommand());
		config.setEnum(keySolutionMode, SolutionMode.class, getSolutionMode());
		config.set(keyExtraArgs, getExtraArgs());
		config.setBoolean(keyUsePnmlUnfolding, getUsePnmlUnfolding());
		config.setBoolean(keyDebugReach, getDebugReach());
	}

	@Override
	public String getSection() {
		return "External tools";
	}

	@Override
	public String getName() {
		return "MPSat";
	}

	public static String getCommand() {
		return command;
	}

	public static void setCommand(String value) {
		command = value;
	}

	public static String getExtraArgs() {
		return extraArgs;
	}

	public static void setExtraArgs(String value) {
		extraArgs = value;
	}

	public static void setSolutionMode(SolutionMode value) {
		solutionMode = value;
	}

	public static SolutionMode getSolutionMode() {
		return solutionMode;
	}

	public static int getSolutionCount() {
		return (solutionMode == SolutionMode.ALL) ? 10 : 1;
	}

	public static Boolean getUsePnmlUnfolding() {
		return usePnmlUnfolding;
	}

	public static void setUsePnmlUnfolding(Boolean value) {
		usePnmlUnfolding = value;
	}

	public static Boolean getDebugReach() {
		return debugReach;
	}

	public static void setDebugReach(Boolean value) {
		debugReach = value;
	}

	public static String getUnfoldingExtension(boolean tryPnml) {
		return (tryPnml && getUsePnmlUnfolding() ? ".pnml" : ".mci");
	}

	public static String getCommandSuffix(boolean tryPnml) {
		return (tryPnml && getUsePnmlUnfolding() ? "_pnml" : "");
	}

}
