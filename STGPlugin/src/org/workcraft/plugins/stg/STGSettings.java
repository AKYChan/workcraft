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

package org.workcraft.plugins.stg;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.workcraft.Config;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.gui.propertyeditor.SettingsPage;

public class STGSettings implements SettingsPage {
	private static final LinkedList<PropertyDescriptor> properties = new LinkedList<PropertyDescriptor>();
	private static final String prefix = "StgSettings";

	private static final String keyInputColor = prefix + ".inputColor";
	private static final String keyOutputColor = prefix + ".outputColor";
	private static final String keyInternalColor = prefix + ".internalColor";
	private static final String keyDummyColor = prefix + ".dummyColor";

	private static final Color defaultInputColor = Color.RED.darker();
	private static final Color defaultOutputColor = Color.BLUE.darker();
	private static final Color defaultInternalColor = Color.GREEN.darker();
	private static final Color defaultDummyColor = Color.BLACK.darker();

	private static Color inputColor = defaultInputColor;
	private static Color outputColor = defaultOutputColor;
	private static Color internalColor = defaultInternalColor;
	private static Color dummyColor = defaultDummyColor;

	public STGSettings() {
		properties.add(new PropertyDeclaration<STGSettings, Color>(
				this, "Input transition color", Color.class) {
			protected void setter(STGSettings object, Color value) {
				STGSettings.setInputColor(value);
			}
			protected Color getter(STGSettings object) {
				return STGSettings.getInputColor();
			}
		});

		properties.add(new PropertyDeclaration<STGSettings, Color>(
				this, "Output transition color", Color.class) {
			protected void setter(STGSettings object, Color value) {
				STGSettings.setOutputColor(value);
			}
			protected Color getter(STGSettings object) {
				return STGSettings.getOutputColor();
			}
		});

		properties.add(new PropertyDeclaration<STGSettings, Color>(
				this, "Internal transition color", Color.class) {
			protected void setter(STGSettings object, Color value) {
				STGSettings.setInternalColor(value);
			}
			protected Color getter(STGSettings object) {
				return STGSettings.getInternalColor();
			}
		});

		properties.add(new PropertyDeclaration<STGSettings, Color>(
				this, "Dummy transition color", Color.class) {
			protected void setter(STGSettings object, Color value) {
				STGSettings.setDummyColor(value);
			}
			protected Color getter(STGSettings object) {
				return STGSettings.getDummyColor();
			}
		});
	}

	@Override
	public List<PropertyDescriptor> getDescriptors() {
		return properties;
	}

	@Override
	public void load(Config config) {
		setInputColor(config.getColor(keyInputColor, defaultInputColor));
		setOutputColor(config.getColor(keyOutputColor, defaultOutputColor));
		setInternalColor(config.getColor(keyInternalColor, defaultInternalColor));
		setDummyColor(config.getColor(keyDummyColor, defaultDummyColor));
	}

	@Override
	public void save(Config config) {
		config.setColor(keyInputColor, getInputColor());
		config.setColor(keyOutputColor, getOutputColor());
		config.setColor(keyInternalColor, getInternalColor());
		config.setColor(keyDummyColor, getDummyColor());
	}

	@Override
	public String getSection() {
		return "Models";
	}

	@Override
	public String getName() {
		return "Signal Transition Graph";
	}

	public static void setInputColor(Color value) {
		inputColor = value;
	}

	public static Color getInputColor() {
		return inputColor;
	}

	public static void setOutputColor(Color value) {
		outputColor = value;
	}

	public static Color getOutputColor() {
		return outputColor;
	}

	public static void setInternalColor(Color value) {
		internalColor = value;
	}

	public static Color getInternalColor() {
		return internalColor;
	}

	public static void setDummyColor(Color value) {
		dummyColor = value;
	}

	public static Color getDummyColor() {
		return dummyColor;
	}

}
