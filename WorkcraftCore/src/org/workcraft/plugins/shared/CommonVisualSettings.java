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

package org.workcraft.plugins.shared;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.workcraft.Config;
import org.workcraft.dom.visual.Positioning;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.gui.propertyeditor.SettingsPage;

public class CommonVisualSettings implements SettingsPage {
	private static final LinkedList<PropertyDescriptor> properties = new LinkedList<PropertyDescriptor>();
	private static final String prefix = "CommonVisualSettings";

	private static final String keyBaseSize  = prefix + ".baseSize";
	private static final String keyStrokeWidth  = prefix + ".strokeWidth";
	private static final String keyBorderColor = prefix + ".borderColor";
	private static final String keyFillColor  = prefix + ".fillColor";
	private static final String keyLabelVisibility  = prefix + ".labelVisibility";
	private static final String keyLabelPositioning  = prefix + ".labelPositioning";
	private static final String keyLabelColor  = prefix + ".labelColor";
	private static final String keyNameVisibility  = prefix + ".nameVisibility";
	private static final String keyNamePositioning  = prefix + ".namePositioning";
	private static final String keyNameColor  = prefix + ".nameColor";
	private static final String keyRedrawInterval  = prefix + ".redrawInterval";

	private static final double defaultBaseSize = 1.0;
	private static final double defaultStrokeWidth = 0.1;
	private static final Color defaultBorderColor = Color.BLACK;
	private static final Color defaultFillColor = Color.WHITE;
	private static final boolean defaultLabelVisibility = true;
	private static final Positioning defaultLabelPositioning = Positioning.TOP;
	private static final Color defaultLabelColor = Color.BLACK;
	private static final boolean defaultNameVisibility = false;
	private static final Positioning defaultNamePositioning = Positioning.BOTTOM;
	private static final Color defaultNameColor = Color.BLUE.darker();
	private static final Integer defaultRedrawInterval = 20;

	private static double baseSize = defaultBaseSize;
	private static double strokeWidth = defaultStrokeWidth;
	private static Color borderColor = defaultBorderColor;
	private static Color fillColor = defaultFillColor;
	private static boolean labelVisibility = defaultLabelVisibility;
	private static Positioning labelPositioning = defaultLabelPositioning;
	private static Color labelColor = defaultLabelColor;
	private static boolean nameVisibility = defaultNameVisibility;
	private static Positioning namePositioning = defaultNamePositioning;
	private static Color nameColor = defaultNameColor;
	private static Integer redrawInterval = defaultRedrawInterval;

	public CommonVisualSettings() {
		properties.add(new PropertyDeclaration<CommonVisualSettings, Double>(
				this, "Base size (cm)", Double.class) {
			protected void setter(CommonVisualSettings object, Double value) {
				CommonVisualSettings.setBaseSize(value);
			}
			protected Double getter(CommonVisualSettings object) {
				return CommonVisualSettings.getBaseSize();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Double>(
				this, "Stroke width (cm)", Double.class) {
			protected void setter(CommonVisualSettings object, Double value) {
				CommonVisualSettings.setStrokeWidth(value);
			}
			protected Double getter(CommonVisualSettings object) {
				return CommonVisualSettings.getStrokeWidth();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Color>(
				this, "Border color", Color.class) {
			protected void setter(CommonVisualSettings object, Color value) {
				CommonVisualSettings.setBorderColor(value);
			}
			protected Color getter(CommonVisualSettings object) {
				return CommonVisualSettings.getBorderColor();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Color>(
				this, "Fill color", Color.class) {
			protected void setter(CommonVisualSettings object, Color value) {
				CommonVisualSettings.setFillColor(value);
			}
			protected Color getter(CommonVisualSettings object) {
				return CommonVisualSettings.getFillColor();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Boolean>(
				this, "Show component labels", Boolean.class) {
			protected void setter(CommonVisualSettings object, Boolean value) {
				CommonVisualSettings.setLabelVisibility(value);
			}
			protected Boolean getter(CommonVisualSettings object) {
				return CommonVisualSettings.getLabelVisibility();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Positioning>(
				this, "Label positioning", Positioning.class, Positioning.getChoice()) {
			protected void setter(CommonVisualSettings object, Positioning value) {
				CommonVisualSettings.setLabelPositioning(value);
			}
			protected Positioning getter(CommonVisualSettings object) {
				return CommonVisualSettings.getLabelPositioning();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Color>(
				this, "Label color", Color.class) {
			protected void setter(CommonVisualSettings object, Color value) {
				CommonVisualSettings.setLabelColor(value);
			}
			protected Color getter(CommonVisualSettings object) {
				return CommonVisualSettings.getLabelColor();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Boolean>(
				this, "Show component names", Boolean.class) {
			protected void setter(CommonVisualSettings object, Boolean value) {
				CommonVisualSettings.setNameVisibility(value);
			}
			protected Boolean getter(CommonVisualSettings object) {
				return CommonVisualSettings.getNameVisibility();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Positioning>(
				this, "Name positioning", Positioning.class, Positioning.getChoice()) {
			protected void setter(CommonVisualSettings object, Positioning value) {
				CommonVisualSettings.setNamePositioning(value);
			}
			protected Positioning getter(CommonVisualSettings object) {
				return CommonVisualSettings.getNamePositioning();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Color>(
				this, "Name color", Color.class) {
			protected void setter(CommonVisualSettings object, Color value) {
				CommonVisualSettings.setNameColor(value);
			}
			protected Color getter(CommonVisualSettings object) {
				return CommonVisualSettings.getNameColor();
			}
		});

		properties.add(new PropertyDeclaration<CommonVisualSettings, Integer>(
				this, "Minimal redraw interval (ms)", Integer.class) {
			protected void setter(CommonVisualSettings object, Integer value) {
				CommonVisualSettings.setRedrawInterval(value);
			}
			protected Integer getter(CommonVisualSettings object) {
				return CommonVisualSettings.getRedrawInterval();
			}
		});
	}

	@Override
	public List<PropertyDescriptor> getDescriptors() {
		return properties;
	}

	@Override
	public void load(Config config) {
		setBaseSize(config.getDouble(keyBaseSize, defaultBaseSize));
		setStrokeWidth(config.getDouble(keyStrokeWidth, defaultStrokeWidth));
		setBorderColor(config.getColor(keyBorderColor, defaultBorderColor));
		setFillColor(config.getColor(keyFillColor, defaultFillColor));
		setLabelVisibility(config.getBoolean(keyLabelVisibility, defaultLabelVisibility));
		setLabelPositioning(config.getTextPositioning(keyLabelPositioning, defaultLabelPositioning));
		setLabelColor(config.getColor(keyLabelColor, defaultLabelColor));
		setNameVisibility(config.getBoolean(keyNameVisibility, defaultNameVisibility));
		setNamePositioning(config.getTextPositioning(keyNamePositioning, defaultNamePositioning));
		setNameColor(config.getColor(keyNameColor, defaultNameColor));
		setRedrawInterval(config.getInt(keyRedrawInterval, defaultRedrawInterval));
	}

	@Override
	public void save(Config config) {
		config.setDouble(keyBaseSize, getBaseSize());
		config.setDouble(keyStrokeWidth, getStrokeWidth());
		config.setColor(keyBorderColor, getBorderColor());
		config.setColor(keyFillColor, getFillColor());
		config.setBoolean(keyLabelVisibility, getLabelVisibility());
		config.setColor(keyLabelColor, getLabelColor());
		config.setTextPositioning(keyLabelPositioning, getLabelPositioning());
		config.setBoolean(keyNameVisibility, getNameVisibility());
		config.setColor(keyNameColor, getNameColor());
		config.setTextPositioning(keyNamePositioning, getNamePositioning());
		config.setInt(keyRedrawInterval, getRedrawInterval());
	}

	@Override
	public String getSection() {
		return "Common";
	}

	@Override
	public String getName() {
		return "Visual";
	}

	public static double getBaseSize() {
		return baseSize;
	}

	public static void setBaseSize(double value) {
		baseSize = value;
	}

	public static double getStrokeWidth() {
		return strokeWidth;
	}

	public static void setStrokeWidth(double value) {
		strokeWidth = value;
	}

	public static Color getBorderColor() {
		return borderColor;
	}

	public static void setBorderColor(Color value) {
		borderColor = value;
	}

	public static Color getFillColor() {
		return fillColor;
	}

	public static void setFillColor(Color value) {
		fillColor = value;
	}

	public static Boolean getLabelVisibility() {
		return labelVisibility;
	}

	public static void setLabelVisibility(Boolean value) {
		labelVisibility = value;
	}

	public static Positioning getLabelPositioning() {
		return labelPositioning;
	}

	public static void setLabelPositioning(Positioning value) {
		labelPositioning = value;
	}

	public static Color getLabelColor() {
		return labelColor;
	}

	public static void setLabelColor(Color value) {
		labelColor = value;
	}

	public static Boolean getNameVisibility() {
		return nameVisibility;
	}

	public static void setNameVisibility(Boolean value) {
		nameVisibility = value;
	}

	public static Positioning getNamePositioning() {
		return namePositioning;
	}

	public static void setNamePositioning(Positioning value) {
		namePositioning = value;
	}

	public static Color getNameColor() {
		return nameColor;
	}

	public static void setNameColor(Color value) {
		nameColor = value;
	}

	public static void setRedrawInterval(Integer value) {
		redrawInterval = value;
	}

	public static Integer getRedrawInterval() {
		return redrawInterval;
	}

}
