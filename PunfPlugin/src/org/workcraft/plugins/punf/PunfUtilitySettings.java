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

package org.workcraft.plugins.punf;
import java.util.LinkedList;
import java.util.List;

import org.workcraft.Config;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.gui.propertyeditor.Settings;

public class PunfUtilitySettings implements Settings {
	public static final String BUNDLED_DIRECTORY = "tools/punf/";

	private static final LinkedList<PropertyDescriptor> properties = new LinkedList<PropertyDescriptor>();
	private static final String prefix = "Tools.punf";

	private static final String keyCommand = prefix + ".command";
	private static final String keyExtraArgs = prefix + ".args";
	private static final String keyUseBundledVersion = prefix + ".useBundledVersion";
	private static final String keyUsePnmlUnfolding = prefix + ".usePnmlUnfolding";

	private static final String defaultCommand = "punf";
	private static final String defaultExtraArgs = "-r";
	private static Boolean defaultUseBundledVersion = true;
	private static final Boolean defaultUsePnmlUnfolding = true;

	private static String command = defaultCommand;
	private static String extraArgs = defaultExtraArgs;
	private static Boolean useBundledVersion = defaultUseBundledVersion;
	private static Boolean usePnmlUnfolding = defaultUsePnmlUnfolding;

	public PunfUtilitySettings() {
		properties.add(new PropertyDeclaration<PunfUtilitySettings, String>(
				this, "Punf command", String.class, true, false, false) {
			protected void setter(PunfUtilitySettings object, String value) {
				setCommand(value);
			}
			protected String getter(PunfUtilitySettings object) {
				return getCommand();
			}
		});

		properties.add(new PropertyDeclaration<PunfUtilitySettings, String>(
				this, "Additional parameters", String.class, true, false, false) {
			protected void setter(PunfUtilitySettings object, String value) {
				setExtraArgs(value);
			}
			protected String getter(PunfUtilitySettings object) {
				return getExtraArgs();
			}
		});

		properties.add(new PropertyDeclaration<PunfUtilitySettings, Boolean>(
				this, "Use bundled version (in " + BUNDLED_DIRECTORY + ")", Boolean.class, true, false, false) {
			protected void setter(PunfUtilitySettings object, Boolean value) {
				setUseBundledVersion(value);
			}
			protected Boolean getter(PunfUtilitySettings object) {
				return getUseBundledVersion();
			}
		});

		properties.add(new PropertyDeclaration<PunfUtilitySettings, Boolean>(
				this, "Use PNML-based unfolding (where possible)", Boolean.class, true, false, false) {
			protected void setter(PunfUtilitySettings object, Boolean value) {
				setUsePnmlUnfolding(value);
			}
			protected Boolean getter(PunfUtilitySettings object) {
				return getUsePnmlUnfolding();
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
		setExtraArgs(config.getString(keyExtraArgs, defaultExtraArgs));
		setUseBundledVersion(config.getBoolean(keyUseBundledVersion, defaultUseBundledVersion));
		setUsePnmlUnfolding(config.getBoolean(keyUsePnmlUnfolding, defaultUsePnmlUnfolding));
	}

	@Override
	public void save(Config config) {
		config.set(keyCommand, getCommand());
		config.set(keyExtraArgs, getExtraArgs());
		config.setBoolean(keyUseBundledVersion, getUseBundledVersion());
		config.setBoolean(keyUsePnmlUnfolding, getUsePnmlUnfolding());
	}

	@Override
	public String getSection() {
		return "External tools";
	}

	@Override
	public String getName() {
		return "Punf";
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

	public static Boolean getUseBundledVersion() {
		return useBundledVersion;
	}

	public static void setUseBundledVersion(Boolean value) {
		useBundledVersion = value;
	}

	public static Boolean getUsePnmlUnfolding() {
		return usePnmlUnfolding;
	}

	public static void setUsePnmlUnfolding(Boolean value) {
		usePnmlUnfolding = value;
	}

	public static String getUnfoldingExtension(boolean tryPnml) {
		return (tryPnml && getUsePnmlUnfolding() ? ".pnml" : ".mci");
	}

	public static String getCommandSuffix(boolean tryPnml) {
		return (tryPnml && getUsePnmlUnfolding() ? "_pnml" : "");
	}

}