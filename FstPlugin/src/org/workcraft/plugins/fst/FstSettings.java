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

package org.workcraft.plugins.fst;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.workcraft.Config;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.gui.propertyeditor.Settings;

public class FstSettings implements Settings {
    private static final LinkedList<PropertyDescriptor> properties = new LinkedList<PropertyDescriptor>();
    private static final String prefix = "FstSettings";

    private static final String keyInputColor = prefix + ".inputColor";
    private static final String keyOutputColor = prefix + ".outputColor";
    private static final String keyInternalColor = prefix + ".internalColor";
    private static final String keyDummyColor = prefix + ".dummyColor";
    private static final String keyShowToggle = prefix + ".showToggle";

    private static final Color defaultInputColor = Color.RED.darker();
    private static final Color defaultOutputColor = Color.BLUE.darker();
    private static final Color defaultInternalColor = Color.GREEN.darker();
    private static final Color defaultDummyColor = Color.BLACK.darker();
    private static final boolean defaultShowToggle = false;

    private static Color inputColor = defaultInputColor;
    private static Color outputColor = defaultOutputColor;
    private static Color internalColor = defaultInternalColor;
    private static Color dummyColor = defaultDummyColor;
    private static boolean showToggle = defaultShowToggle;

    public FstSettings() {
        properties.add(new PropertyDeclaration<FstSettings, Color>(
                this, "Input transition color", Color.class, true, false, false) {
            protected void setter(FstSettings object, Color value) {
                setInputColor(value);
            }
            protected Color getter(FstSettings object) {
                return getInputColor();
            }
        });

        properties.add(new PropertyDeclaration<FstSettings, Color>(
                this, "Output transition color", Color.class, true, false, false) {
            protected void setter(FstSettings object, Color value) {
                setOutputColor(value);
            }
            protected Color getter(FstSettings object) {
                return getOutputColor();
            }
        });

        properties.add(new PropertyDeclaration<FstSettings, Color>(
                this, "Internal transition color", Color.class, true, false, false) {
            protected void setter(FstSettings object, Color value) {
                setInternalColor(value);
            }
            protected Color getter(FstSettings object) {
                return getInternalColor();
            }
        });

        properties.add(new PropertyDeclaration<FstSettings, Color>(
                this, "Dummy transition color", Color.class, true, false, false) {
            protected void setter(FstSettings object, Color value) {
                setDummyColor(value);
            }
            protected Color getter(FstSettings object) {
                return getDummyColor();
            }
        });

        properties.add(new PropertyDeclaration<FstSettings, Boolean>(
                this, "Show signal toggle (~)", Boolean.class, true, false, false) {
            protected void setter(FstSettings object, Boolean value) {
                setShowToggle(value);
            }
            protected Boolean getter(FstSettings object) {
                return getShowToggle();
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
        setShowToggle(config.getBoolean(keyShowToggle, defaultShowToggle));
    }

    @Override
    public void save(Config config) {
        config.setColor(keyInputColor, getInputColor());
        config.setColor(keyOutputColor, getOutputColor());
        config.setColor(keyInternalColor, getInternalColor());
        config.setColor(keyDummyColor, getDummyColor());
        config.setBoolean(keyShowToggle, getShowToggle());
    }

    @Override
    public String getSection() {
        return "Models";
    }

    @Override
    public String getName() {
        return "Finite State Trasducer";
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

    public static Boolean getShowToggle() {
        return showToggle;
    }

    public static void setShowToggle(Boolean value) {
        showToggle = value;
    }

}
