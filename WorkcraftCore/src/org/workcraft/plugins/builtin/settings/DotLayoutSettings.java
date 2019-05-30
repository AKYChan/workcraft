package org.workcraft.plugins.builtin.settings;

import org.workcraft.Config;
import org.workcraft.gui.properties.PropertyDeclaration;
import org.workcraft.gui.properties.PropertyDescriptor;
import org.workcraft.utils.DesktopApi;

import java.util.LinkedList;
import java.util.List;

public class DotLayoutSettings extends AbstractLayoutSettings {

    public enum Rankdir {
        LR("left-to-right", "LR"),
        TB("top-to-bottom", "TB"),
        RL("right-to-left", "RL"),
        BT("bottom-to-top", "BT");

        public final String name;
        public final String value;

        Rankdir(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final LinkedList<PropertyDescriptor> properties = new LinkedList<>();
    private static final String prefix = "DotLayoutSettings";

    private static final String keyCommand = prefix + ".command";
    private static final String keyRankdir = prefix + ".rankdir";
    private static final String keyNodesep = prefix + ".sepNodesep";
    private static final String keyRanksep = prefix + ".sepRanksep";
    private static final String keyImportConnectionsShape = prefix + ".importConnectionsShape";

    private static final String defaultCommand = DesktopApi.getOs().isWindows() ? "tools\\GraphvizMinimal\\dot.exe" : "dot";
    private static final Rankdir defaultRankdir = Rankdir.LR;
    private static final double defaultNodesep = 1.0;
    private static final double defaultRanksep = 1.0;
    private static final boolean defaultImportConnectionsShape = true;

    private static String dotCommand = defaultCommand;
    private static Rankdir rankdir = defaultRankdir;
    private static double nodesep = defaultNodesep;
    private static double ranksep = defaultRanksep;
    private static boolean importConnectionsShape = defaultImportConnectionsShape;

    public DotLayoutSettings() {
        properties.add(new PropertyDeclaration<DotLayoutSettings, String>(
                this, "Dot command", String.class) {
            @Override
            public void setter(DotLayoutSettings object, String value) {
                setCommand(value);
            }
            @Override
            public String getter(DotLayoutSettings object) {
                return getCommand();
            }
        });

        properties.add(new PropertyDeclaration<DotLayoutSettings, Rankdir>(
                this, "Direction of layout", Rankdir.class) {
            @Override
            public void setter(DotLayoutSettings object, Rankdir value) {
                setRankdir(value);
            }
            @Override
            public Rankdir getter(DotLayoutSettings object) {
                return getRankdir();
            }
        });

        properties.add(new PropertyDeclaration<DotLayoutSettings, Double>(
                this, "Node separation", Double.class) {
            @Override
            public void setter(DotLayoutSettings object, Double value) {
                setNodesep(value);
            }
            @Override
            public Double getter(DotLayoutSettings object) {
                return getNodesep();
            }
        });

        properties.add(new PropertyDeclaration<DotLayoutSettings, Double>(
                this, "Rank separation", Double.class) {
            @Override
            public void setter(DotLayoutSettings object, Double value) {
                setRanksep(value);
            }
            @Override
            public Double getter(DotLayoutSettings object) {
                return getRanksep();
            }
        });

        properties.add(new PropertyDeclaration<DotLayoutSettings, Boolean>(
                this, "Import connections shape", Boolean.class) {
            @Override
            public void setter(DotLayoutSettings object, Boolean value) {
                setImportConnectionsShape(value);
            }
            @Override
            public Boolean getter(DotLayoutSettings object) {
                return getImportConnectionsShape();
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
        setRankdir(config.getEnum(keyRankdir, Rankdir.class, defaultRankdir));
        setNodesep(config.getDouble(keyNodesep, defaultNodesep));
        setRanksep(config.getDouble(keyRanksep, defaultRanksep));
        setImportConnectionsShape(config.getBoolean(keyImportConnectionsShape, defaultImportConnectionsShape));
    }

    @Override
    public void save(Config config) {
        config.set(keyCommand, getCommand());
        config.setEnum(keyRankdir, getRankdir());
        config.setDouble(keyNodesep, getNodesep());
        config.setDouble(keyRanksep, getRanksep());
        config.setBoolean(keyImportConnectionsShape, getImportConnectionsShape());
    }

    @Override
    public String getName() {
        return "Dot";
    }

    public static String getCommand() {
        return dotCommand;
    }

    public static void setCommand(String value) {
        dotCommand = value;
    }

    public static Rankdir getRankdir() {
        return rankdir;
    }

    public static void setRankdir(Rankdir value) {
        rankdir = value;
    }

    public static double getNodesep() {
        return nodesep;
    }

    public static void setNodesep(double value) {
        if (value < 0.0) {
            value = 0.0;
        }
        if (value > 10.0) {
            value = 10.0;
        }
        nodesep = value;
    }

    public static double getRanksep() {
        return ranksep;
    }

    public static void setRanksep(double value) {
        if (value < 0.0) {
            value = 0.0;
        }
        if (value > 10.0) {
            value = 10.0;
        }
        ranksep = value;
    }

    public static boolean getImportConnectionsShape() {
        return importConnectionsShape;
    }

    public static void setImportConnectionsShape(boolean value) {
        importConnectionsShape = value;
    }

}
