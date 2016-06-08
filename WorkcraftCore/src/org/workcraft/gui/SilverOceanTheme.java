package org.workcraft.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import org.workcraft.dom.visual.SizeHelper;

public class SilverOceanTheme extends OceanTheme {

    private final class CheckBoxIcon implements Icon {
        @Override
        public int getIconWidth() {
            return SizeHelper.getCheckBoxIconSize();
        }

        @Override
        public int getIconHeight() {
            return SizeHelper.getCheckBoxIconSize();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton abstractButton = (AbstractButton) c;
            ButtonModel buttonModel = abstractButton.getModel();
            int w = getIconWidth();
            int h = getIconHeight();
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;
                RenderingHints rh = new RenderingHints(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHints(rh);
                float s = SizeHelper.getMinimalStrockWidth();
                g2.setStroke(new BasicStroke(1.0f));
            }
            g.setColor(Color.WHITE);
            g.fillRect(x, y, w, h);
            if (buttonModel.isEnabled()) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.GRAY);
            }
            g.drawRect(x, y, w, h);
            if (buttonModel.isSelected()) {
                g.drawLine(x + 1, y + 1, x + w - 1, y + h - 1);
                g.drawLine(x + w - 1, y + 1, x + 1, y + h - 1);
            }
        }
    }

    private final class RadioButtonIcon implements Icon {
        public int getIconWidth() {
            return SizeHelper.getRadioBurronIconSize();
        }

        public int getIconHeight() {
            return SizeHelper.getRadioBurronIconSize();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton abstractButton = (AbstractButton) c;
            ButtonModel buttonModel = abstractButton.getModel();
            int w = getIconWidth();
            int h = getIconHeight();
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;
                RenderingHints rh = new RenderingHints(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHints(rh);
                float s = SizeHelper.getMinimalStrockWidth();
                g2.setStroke(new BasicStroke(s));
            }
            g.setColor(Color.WHITE);
            g.fillOval(x, y, w - 1, h - 1);
            if (buttonModel.isEnabled()) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.GRAY);
            }
            g.drawOval(x, y, w - 1, h - 1);
            if (buttonModel.isSelected()) {
                int dx = (int) Math.round(0.25 * w);
                int dy = (int) Math.round(0.25 * h);
                g.fillOval(x + dx, y + dy, w - 2 * dx, h - 2 * dy);
            }
        }
    }

    public static void enable() {
        MetalLookAndFeel.setCurrentTheme(new SilverOceanTheme());
    }

    @Override
    public FontUIResource getControlTextFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.PLAIN, SizeHelper.getBaseFontSize());
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return getControlTextFont();
    }

    @Override
    public FontUIResource getSubTextFont() {
        return getControlTextFont();
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return getControlTextFont();
    }

    @Override
    public FontUIResource getUserTextFont() {
        return getControlTextFont();
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return new FontUIResource(getControlTextFont().deriveFont(Font.BOLD));
    }

    @Override
    protected ColorUIResource getSecondary1() {
        return new ColorUIResource(0x999999);
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return  new ColorUIResource(0xcccccc);
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return  new ColorUIResource(0xeeeeee);
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return new ColorUIResource(0x999999);
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return new ColorUIResource(0xbbccdd);
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return new ColorUIResource(0xbbccdd);
    }

    @Override
    public void addCustomEntriesToTable(UIDefaults table) {
        super.addCustomEntriesToTable(table);
        List<Serializable> buttonGradient = Arrays.asList(1.0, 0.0, getSecondary3(), getSecondary2(), getSecondary2());

        Object[] uiDefaults = {
                "Button.gradient", buttonGradient,
                "CheckBox.gradient", buttonGradient,
                "CheckBoxMenuItem.gradient", buttonGradient,
                "InternalFrame.activeTitleGradient", buttonGradient,
                "RadioButton.gradient", buttonGradient,
                "RadioButtonMenuItem.gradient", buttonGradient,
                "ScrollBar.gradient", buttonGradient,
                "Slider.focusGradient", buttonGradient,
                "Slider.gradient", buttonGradient,
                "ToggleButton.gradient", buttonGradient,

                "TabbedPane.selected", getPrimary2(),
                "TabbedPane.contentAreaColor", getPrimary2(),

                "CheckBox.icon", new CheckBoxIcon(),
                "CheckBoxMenuItem.checkIcon", new CheckBoxIcon(),
                "RadioButton.icon", new RadioButtonIcon(),

                "ScrollBar.width", SizeHelper.getScrollbarWidth(),
        };
        table.putDefaults(uiDefaults);
    }

}
