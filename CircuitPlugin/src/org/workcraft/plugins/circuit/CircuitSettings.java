package org.workcraft.plugins.circuit;

import org.workcraft.Config;
import org.workcraft.gui.properties.PropertyDeclaration;
import org.workcraft.gui.properties.PropertyDescriptor;
import org.workcraft.plugins.builtin.settings.AbstractModelSettings;
import org.workcraft.plugins.circuit.utils.Gate2;
import org.workcraft.plugins.circuit.utils.Gate3;
import org.workcraft.plugins.stg.Mutex;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.StgSettings;
import org.workcraft.types.Pair;
import org.workcraft.utils.DesktopApi;
import org.workcraft.utils.DialogUtils;
import org.workcraft.utils.ExecutableUtils;

import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CircuitSettings extends AbstractModelSettings {

    private static final Pattern MUTEX_DATA_PATTERN = Pattern.compile(
            "(\\w+)\\(\\((\\w+),(\\w+)\\),\\((\\w+),(\\w+)\\)\\)");

    private static final int MUTEX_NAME_GROUP = 1;
    private static final int MUTEX_R1_GROUP = 2;
    private static final int MUTEX_G1_GROUP = 3;
    private static final int MUTEX_R2_GROUP = 4;
    private static final int MUTEX_G2_GROUP = 5;

    private static final Pattern GATE2_DATA_PATTERN = Pattern.compile("(\\w+)\\((\\w+),(\\w+)\\)");
    private static final Pattern GATE3_DATA_PATTERN = Pattern.compile("(\\w+)\\((\\w+),(\\w+),(\\w+)\\)");
    private static final int GATE_NAME_GROUP = 1;
    private static final int GATE_PIN1_GROUP = 2;
    private static final int GATE_PIN2_GROUP = 3;
    private static final int GATE_PIN3_GROUP = 4;

    private static final LinkedList<PropertyDescriptor> properties = new LinkedList<>();
    private static final String prefix = "CircuitSettings";

    private static final String keyShowContacts = prefix + ".showContacts";
    private static final String keyContactFontSize = prefix + ".contactFontSize";
    private static final String keyFunctionFontSize = prefix + ".functionFontSize";
    private static final String keyShowZeroDelayNames = prefix + ".showZeroDelayNames";
    private static final String keyBorderWidth = prefix + ".borderWidth";
    private static final String keyWireWidth = prefix + ".wireWidth";
    private static final String keyActiveWireColor = prefix + ".activeWireColor";
    private static final String keyInactiveWireColor = prefix + ".inactiveWireColor";
    private static final String keySimplifyStg = prefix + ".simplifyStg";
    private static final String keyGateLibrary = prefix + ".gateLibrary";
    private static final String keyExportSubstitutionLibrary = prefix + ".exportSubstitutionLibrary";
    private static final String keyInvertExportSubstitutionRules = prefix + ".invertExportSubstitutionRules";
    private static final String keyImportSubstitutionLibrary = prefix + ".importSubstitutionLibrary";
    private static final String keyInvertImportSubstitutionRules = prefix + ".invertImportSubstitutionRules";
    private static final String keyBufData = prefix + ".bufData";
    private static final String keyAndData = prefix + ".andData";
    private static final String keyOrData = prefix + ".orData";
    private static final String keyNandData = prefix + ".nandData";
    private static final String keyNorData = prefix + ".norData";
    private static final String keyNandbData = prefix + ".nandbData";
    private static final String keyNorbData = prefix + ".norbData";
    private static final String keyMutexData = prefix + ".mutexData";
    private static final String keyBusSuffix = prefix + ".busSuffix";
    private static final String keyResetPort = prefix + ".resetPort";
    private static final String keySetPin = prefix + ".setPin";
    private static final String keyClearPin = prefix + ".clearPin";
    private static final String keyTbufData = prefix + ".tbufData";
    private static final String keyTinvData = prefix + ".tinvData";
    private static final String keyScanSuffix = prefix + ".scanSuffix";
    private static final String keyScaninPortPin = prefix + ".scaninPortPin";
    private static final String keyScanoutPortPin = prefix + ".scanoutPortPin";
    private static final String keyScanckPortPin = prefix + ".scanckPortPin";
    private static final String keyScanenPortPin = prefix + ".scanenPortPin";
    private static final String keyScantmPortPin = prefix + ".scantmPortPin";
    private static final String keyVerilogAssignDelay = prefix + ".verilogAssignDelay";

    private static final boolean defaultShowContacts = false;
    private static final double defaultContactFontSize = 0.4f;
    private static final double defaultFunctionFontSize = 0.5f;
    private static final boolean defaultShowZeroDelayNames = false;
    private static final Double defaultBorderWidth = 0.06;
    private static final Double defaultWireWidth = 0.04;
    private static final Color defaultActiveWireColor = new Color(1.0f, 0.0f, 0.0f);
    private static final Color defaultInactiveWireColor = new Color(0.0f, 0.0f, 1.0f);
    private static final boolean defaultSimplifyStg = true;
    private static final String defaultGateLibrary = DesktopApi.getOs().isWindows() ? "libraries\\workcraft.lib" : "libraries/workcraft.lib";
    private static final String defaultExportSubstitutionLibrary = "";
    private static final boolean defaultInvertExportSubstitutionRules = false;
    private static final String defaultImportSubstitutionLibrary = "";
    private static final boolean defaultInvertImportSubstitutionRules = true;
    private static final String defaultBufData = "BUF (I, O)";
    private static final String defaultAndData = "AND2 (A, B, O)";
    private static final String defaultOrData = "OR2 (A, B, O)";
    private static final String defaultNandData = "NAND2 (A, B, ON)";
    private static final String defaultNorData = "NOR2 (A, B, ON)";
    private static final String defaultNandbData = "NAND2B (AN, B, ON)";
    private static final String defaultNorbData = "NOR2B (AN, B, ON)";
    private static final String defaultMutexData = "MUTEX ((r1, g1), (r2, g2))";
    private static final String defaultBusSuffix = "__$";
    private static final String defaultResetPort = "reset";
    private static final String defaultSetPin = "S";
    private static final String defaultClearPin = "R";
    private static final String defaultTbufData = "TBUF (I, O)";
    private static final String defaultTinvData = "TINV (I, ON)";
    private static final String defaultScanSuffix = "_scan";
    private static final String defaultScaninPortPin = "scanin / SI";
    private static final String defaultScanoutPortPin = "scanout / SO";
    private static final String defaultScanckPortPin = "scanck / CK";
    private static final String defaultScanenPortPin = "scanen / SE";
    private static final String defaultScantmPortPin = "scantm / TM";
    private static final boolean defaultVerilogAssignDelay = false;

    private static boolean showContacts = defaultShowContacts;
    private static double contactFontSize = defaultContactFontSize;
    private static double functionFontSize = defaultFunctionFontSize;
    private static boolean showZeroDelayNames = defaultShowZeroDelayNames;
    private static Double borderWidth = defaultBorderWidth;
    private static Double wireWidth = defaultWireWidth;
    private static Color activeWireColor = defaultActiveWireColor;
    private static Color inactiveWireColor = defaultInactiveWireColor;
    private static boolean simplifyStg = defaultSimplifyStg;
    private static String gateLibrary = defaultGateLibrary;
    private static String exportSubstitutionLibrary = defaultExportSubstitutionLibrary;
    private static boolean invertExportSubstitutionRules = defaultInvertExportSubstitutionRules;
    private static String importSubstitutionLibrary = defaultImportSubstitutionLibrary;
    private static boolean invertImportSubstitutionRules = defaultInvertImportSubstitutionRules;
    private static String bufData = defaultBufData;
    private static String andData = defaultAndData;
    private static String orData = defaultOrData;
    private static String nandData = defaultNandData;
    private static String norData = defaultNorData;
    private static String nandbData = defaultNandbData;
    private static String norbData = defaultNorbData;
    private static String mutexData = defaultMutexData;
    private static String busSuffix = defaultBusSuffix;
    private static String resetPort = defaultResetPort;
    private static String setPin = defaultSetPin;
    private static String clearPin = defaultClearPin;
    private static String tbufData = defaultTbufData;
    private static String tinvData = defaultTinvData;
    private static String scanSuffix = defaultScanSuffix;
    private static String scaninPortPin = defaultScaninPortPin;
    private static String scanoutPortPin = defaultScanoutPortPin;
    private static String scanckPortPin = defaultScanckPortPin;
    private static String scanenPortPin = defaultScanenPortPin;
    private static String scantmPortPin = defaultScantmPortPin;
    private static boolean verilogAssignDelay = defaultVerilogAssignDelay;

    static {
        properties.add(new PropertyDeclaration<>(Boolean.class,
                "Show contacts",
                CircuitSettings::setShowContacts,
                CircuitSettings::getShowContacts));

        properties.add(new PropertyDeclaration<>(Double.class,
                "Contact font size (cm)",
                CircuitSettings::setContactFontSize,
                CircuitSettings::getContactFontSize));

        properties.add(new PropertyDeclaration<>(Double.class,
                "Function font size (cm)",
                CircuitSettings::setFunctionFontSize,
                CircuitSettings::getFunctionFontSize));

        properties.add(new PropertyDeclaration<>(Boolean.class,
                "Show names of zero-dealy components",
                CircuitSettings::setShowZeroDelayNames,
                CircuitSettings::getShowZeroDelayNames));

        properties.add(new PropertyDeclaration<>(Double.class,
                "Border width",
                CircuitSettings::setBorderWidth,
                CircuitSettings::getBorderWidth));

        properties.add(new PropertyDeclaration<>(Double.class,
                "Wire width",
                CircuitSettings::setWireWidth,
                CircuitSettings::getWireWidth));

        properties.add(new PropertyDeclaration<>(Color.class,
                "Active wire",
                CircuitSettings::setActiveWireColor,
                CircuitSettings::getActiveWireColor));

        properties.add(new PropertyDeclaration<>(Color.class,
                "Inactive wire",
                CircuitSettings::setInactiveWireColor,
                CircuitSettings::getInactiveWireColor));

        properties.add(new PropertyDeclaration<>(Boolean.class,
                "Simplify generated circuit STG",
                CircuitSettings::setSimplifyStg,
                CircuitSettings::getSimplifyStg));

        properties.add(new PropertyDeclaration<>(File.class,
                "Gate library for technology mapping",
                (value) -> setGateLibrary(ExecutableUtils.getBaseRelativePath(value)),
                () -> ExecutableUtils.getBaseRelativeFile(getGateLibrary())));

        properties.add(new PropertyDeclaration<>(File.class,
                "Substitution rules for export",
                (value) -> setExportSubstitutionLibrary(ExecutableUtils.getBaseRelativePath(value)),
                () -> ExecutableUtils.getBaseRelativeFile(getExportSubstitutionLibrary())));

        properties.add(new PropertyDeclaration<>(Boolean.class,
                "Invert substitution rules for export",
                CircuitSettings::setInvertExportSubstitutionRules,
                CircuitSettings::getInvertExportSubstitutionRules));

        properties.add(new PropertyDeclaration<>(File.class,
                "Substitution rules for import",
                (value) -> setImportSubstitutionLibrary(ExecutableUtils.getBaseRelativePath(value)),
                () -> ExecutableUtils.getBaseRelativeFile(getImportSubstitutionLibrary())));

        properties.add(new PropertyDeclaration<>(Boolean.class,
                "Invert substitution rules for import",
                CircuitSettings::setInvertImportSubstitutionRules,
                CircuitSettings::getInvertImportSubstitutionRules));

        properties.add(new PropertyDeclaration<>(String.class,
                "BUF name and input-output pins",
                (value) -> setGate2Data(value, CircuitSettings::setBufData, "BUF", defaultBufData),
                CircuitSettings::getBufData));

        properties.add(new PropertyDeclaration<>(String.class,
                "AND2 name and input-output pins",
                (value) -> setGate3Data(value, CircuitSettings::setAndData, "AND2", defaultAndData),
                CircuitSettings::getAndData));

        properties.add(new PropertyDeclaration<>(String.class,
                "OR2 name and input-output pins",
                (value) -> setGate3Data(value, CircuitSettings::setOrData, "OR2", defaultOrData),
                CircuitSettings::getOrData));

        properties.add(new PropertyDeclaration<>(String.class,
                "NAND2 name and input-output pins",
                (value) -> setGate3Data(value, CircuitSettings::setNandData, "NAND2", defaultNandData),
                CircuitSettings::getNandData));

        properties.add(new PropertyDeclaration<>(String.class,
                "NOR2 name and input-output pins",
                (value) -> setGate3Data(value, CircuitSettings::setNorData, "NOR2", defaultNorData),
                CircuitSettings::getNorData));

        properties.add(new PropertyDeclaration<>(String.class,
                "NAND2B name and input-output pins",
                (value) -> setGate3Data(value, CircuitSettings::setNandbData, "NAND2B", defaultNandbData),
                CircuitSettings::getNandbData));

        properties.add(new PropertyDeclaration<>(String.class,
                "NOR2B name and input-output pins",
                (value) -> setGate3Data(value, CircuitSettings::setNorbData, "NOR2B", defaultNorbData),
                CircuitSettings::getNorbData));

        properties.add(new PropertyDeclaration<>(String.class,
                "Mutex name and request-grant pairs",
                (value) -> {
                    if (parseMutexData(value) != null) {
                        setMutexData(value);
                    } else {
                        errorDescriptionFormat("Mutex", defaultMutexData);
                    }
                },
                CircuitSettings::getMutexData));

        properties.add(new PropertyDeclaration<>(Mutex.Protocol.class,
                "Mutex protocol",
                StgSettings::setMutexProtocol,
                StgSettings::getMutexProtocol));

        properties.add(new PropertyDeclaration<>(String.class,
                "Bus split suffix ($ is replaced by index)",
                CircuitSettings::setBusSuffix,
                CircuitSettings::getBusSuffix));

        properties.add(new PropertyDeclaration<>(String.class,
                "Reset port name",
                CircuitSettings::setResetPort,
                CircuitSettings::getResetPort));

        properties.add(new PropertyDeclaration<>(String.class,
                "Initialisation SET pin name",
                CircuitSettings::setSetPin,
                CircuitSettings::getSetPin));

        properties.add(new PropertyDeclaration<>(String.class,
                "Initialisation CLEAR pin name",
                CircuitSettings::setClearPin,
                CircuitSettings::getClearPin));

        properties.add(new PropertyDeclaration<>(String.class,
                "Testable buffer name and input-output pins",
                (value) -> setGate2Data(value, CircuitSettings::setTbufData, "Testable buffer", defaultTbufData),
                CircuitSettings::getTbufData));

        properties.add(new PropertyDeclaration<>(String.class,
                "Testable inverter name and input-output pins",
                (value) -> setGate2Data(value, CircuitSettings::setTinvData, "Testable inverter", defaultTinvData),
                CircuitSettings::getTinvData));

        properties.add(new PropertyDeclaration<>(String.class,
                "Scan module suffix",
                CircuitSettings::setScanSuffix,
                CircuitSettings::getScanSuffix));

        properties.add(new PropertyDeclaration<>(String.class,
                "Scan input port / pin names",
                (value) -> setPortPinPair(value, CircuitSettings::setScaninPortPin, "Scan input"),
                                CircuitSettings::getScaninPortPin));

        properties.add(new PropertyDeclaration<>(String.class,
                "Scan output port / pin (for multi-output component) names",
                (value) -> setPortPinPair(value, CircuitSettings::setScanoutPortPin, "Scan output"),
                CircuitSettings::getScanoutPortPin));

        properties.add(new PropertyDeclaration<>(String.class,
                "Scan clock port / pin names",
                (value) -> setPortPinPair(value, CircuitSettings::setScanckPortPin, "Scan clock"),
                CircuitSettings::getScanckPortPin));

        properties.add(new PropertyDeclaration<>(String.class,
                "Scan enable port / pin name",
                (value) -> setPortPinPair(value, CircuitSettings::setScanenPortPin, "Scan enable"),
                CircuitSettings::getScanenPortPin));

        properties.add(new PropertyDeclaration<>(String.class,
                "Scan test mode port / pin names",
                (value) -> setPortPinPair(value, CircuitSettings::setScantmPortPin, "Scan test mode"),
                CircuitSettings::getScantmPortPin));

        properties.add(new PropertyDeclaration<>(Boolean.class,
                "Delay assign statements in Verilog export",
                CircuitSettings::setVerilogAssignDelay,
                CircuitSettings::getVerilogAssignDelay));
    }

    private static void errorDescriptionFormat(String prefix, String suffix) {
        DialogUtils.showError(prefix + " description format is incorrect. It should be as follows:\n" + suffix);
    }

    private static void errorPortPinFormat(String prefix) {
        DialogUtils.showError(prefix + " port and pin should be a pair of valid names separated by '/'.");
    }

    @Override
    public Collection<PropertyDescriptor> getDescriptors() {
        return properties;
    }

    @Override
    public String getName() {
        return "Digital Circuit";
    }

    @Override
    public void load(Config config) {
        setShowContacts(config.getBoolean(keyShowContacts, defaultShowContacts));
        setContactFontSize(config.getDouble(keyContactFontSize, defaultContactFontSize));
        setFunctionFontSize(config.getDouble(keyFunctionFontSize, defaultFunctionFontSize));
        setShowZeroDelayNames(config.getBoolean(keyShowZeroDelayNames, defaultShowZeroDelayNames));
        setBorderWidth(config.getDouble(keyBorderWidth, defaultBorderWidth));
        setWireWidth(config.getDouble(keyWireWidth, defaultWireWidth));
        setActiveWireColor(config.getColor(keyActiveWireColor, defaultActiveWireColor));
        setInactiveWireColor(config.getColor(keyInactiveWireColor, defaultInactiveWireColor));
        setSimplifyStg(config.getBoolean(keySimplifyStg, defaultSimplifyStg));
        setGateLibrary(config.getString(keyGateLibrary, defaultGateLibrary));
        setExportSubstitutionLibrary(config.getString(keyExportSubstitutionLibrary, defaultExportSubstitutionLibrary));
        setInvertExportSubstitutionRules(config.getBoolean(keyInvertExportSubstitutionRules, defaultInvertExportSubstitutionRules));
        setImportSubstitutionLibrary(config.getString(keyImportSubstitutionLibrary, defaultImportSubstitutionLibrary));
        setInvertImportSubstitutionRules(config.getBoolean(keyInvertImportSubstitutionRules, defaultInvertImportSubstitutionRules));
        setBufData(config.getString(keyBufData, defaultBufData));
        setAndData(config.getString(keyAndData, defaultAndData));
        setOrData(config.getString(keyOrData, defaultOrData));
        setNandbData(config.getString(keyNandData, defaultNandData));
        setNorbData(config.getString(keyNorData, defaultNorData));
        setNandbData(config.getString(keyNandbData, defaultNandbData));
        setNorbData(config.getString(keyNorbData, defaultNorbData));
        setMutexData(config.getString(keyMutexData, defaultMutexData));
        setBusSuffix(config.getString(keyBusSuffix, defaultBusSuffix));
        setResetPort(config.getString(keyResetPort, defaultResetPort));
        setSetPin(config.getString(keySetPin, defaultSetPin));
        setClearPin(config.getString(keyClearPin, defaultClearPin));
        setTbufData(config.getString(keyTbufData, defaultTbufData));
        setTinvData(config.getString(keyTinvData, defaultTinvData));
        setScanSuffix(config.getString(keyScanSuffix, defaultScanSuffix));
        setScaninPortPin(config.getString(keyScaninPortPin, defaultScaninPortPin));
        setScanoutPortPin(config.getString(keyScanoutPortPin, defaultScanoutPortPin));
        setScanckPortPin(config.getString(keyScanckPortPin, defaultScanckPortPin));
        setScanenPortPin(config.getString(keyScanenPortPin, defaultScanenPortPin));
        setScantmPortPin(config.getString(keyScantmPortPin, defaultScantmPortPin));
        setVerilogAssignDelay(config.getBoolean(keyVerilogAssignDelay, defaultVerilogAssignDelay));
    }

    @Override
    public void save(Config config) {
        config.setBoolean(keyShowContacts, getShowContacts());
        config.setDouble(keyContactFontSize, getContactFontSize());
        config.setDouble(keyFunctionFontSize, getFunctionFontSize());
        config.setBoolean(keyShowZeroDelayNames, getShowZeroDelayNames());
        config.setDouble(keyBorderWidth, getBorderWidth());
        config.setDouble(keyWireWidth, getWireWidth());
        config.setColor(keyActiveWireColor, getActiveWireColor());
        config.setColor(keyInactiveWireColor, getInactiveWireColor());
        config.setBoolean(keySimplifyStg, getSimplifyStg());
        config.set(keyGateLibrary, getGateLibrary());
        config.set(keyExportSubstitutionLibrary, getExportSubstitutionLibrary());
        config.setBoolean(keyInvertExportSubstitutionRules, getInvertExportSubstitutionRules());
        config.set(keyImportSubstitutionLibrary, getImportSubstitutionLibrary());
        config.setBoolean(keyInvertImportSubstitutionRules, getInvertImportSubstitutionRules());
        config.set(keyBufData, getBufData());
        config.set(keyAndData, getAndData());
        config.set(keyOrData, getOrData());
        config.set(keyNandData, getNandData());
        config.set(keyNorData, getNorData());
        config.set(keyNandbData, getNandbData());
        config.set(keyNorbData, getNorbData());
        config.set(keyMutexData, getMutexData());
        config.set(keyBusSuffix, getBusSuffix());
        config.set(keyResetPort, getResetPort());
        config.set(keySetPin, getSetPin());
        config.set(keyClearPin, getClearPin());
        config.set(keyTbufData, getTbufData());
        config.set(keyTinvData, getTinvData());
        config.set(keyScanSuffix, getScanSuffix());
        config.set(keyScaninPortPin, getScaninPortPin());
        config.set(keyScanoutPortPin, getScanoutPortPin());
        config.set(keyScanckPortPin, getScanckPortPin());
        config.set(keyScanenPortPin, getScanenPortPin());
        config.set(keyScantmPortPin, getScantmPortPin());
        config.setBoolean(keyVerilogAssignDelay, getVerilogAssignDelay());
    }

    public static boolean getShowContacts() {
        return showContacts;
    }

    public static void setShowContacts(boolean value) {
        showContacts = value;
    }

    public static double getContactFontSize() {
        return contactFontSize;
    }

    public static void setContactFontSize(double value) {
        contactFontSize = value;
    }

    public static double getFunctionFontSize() {
        return functionFontSize;
    }

    public static void setFunctionFontSize(double value) {
        functionFontSize = value;
    }

    public static boolean getShowZeroDelayNames() {
        return showZeroDelayNames;
    }

    public static void setShowZeroDelayNames(boolean value) {
        showZeroDelayNames = value;
    }

    public static double getBorderWidth() {
        return borderWidth;
    }

    public static void setBorderWidth(double value) {
        borderWidth = value;
    }

    public static double getWireWidth() {
        return wireWidth;
    }

    public static void setWireWidth(double value) {
        wireWidth = value;
    }

    public static Color getActiveWireColor() {
        return activeWireColor;
    }

    public static void setActiveWireColor(Color value) {
        activeWireColor = value;
    }

    public static Color getInactiveWireColor() {
        return inactiveWireColor;
    }

    public static void setInactiveWireColor(Color value) {
        inactiveWireColor = value;
    }

    public static boolean getSimplifyStg() {
        return simplifyStg;
    }

    public static void setSimplifyStg(boolean value) {
        simplifyStg = value;
    }

    public static String getGateLibrary() {
        return gateLibrary;
    }

    public static void setGateLibrary(String value) {
        gateLibrary = value;
    }

    public static String getExportSubstitutionLibrary() {
        return exportSubstitutionLibrary;
    }

    public static void setExportSubstitutionLibrary(String value) {
        exportSubstitutionLibrary = value;
    }

    public static boolean getInvertExportSubstitutionRules() {
        return invertExportSubstitutionRules;
    }

    public static void setInvertExportSubstitutionRules(boolean value) {
        invertExportSubstitutionRules = value;
    }

    public static String getImportSubstitutionLibrary() {
        return importSubstitutionLibrary;
    }

    public static void setImportSubstitutionLibrary(String value) {
        importSubstitutionLibrary = value;
    }

    public static boolean getInvertImportSubstitutionRules() {
        return invertImportSubstitutionRules;
    }

    public static void setInvertImportSubstitutionRules(boolean value) {
        invertImportSubstitutionRules = value;
    }

    public static String getBufData() {
        return bufData;
    }

    public static void setBufData(String value) {
        bufData = value;
    }

    public static Gate2 parseBufData() {
        return parseGate2Data(getBufData());
    }

    public static String getAndData() {
        return andData;
    }

    public static void setAndData(String value) {
        andData = value;
    }

    public static Gate3 parseAndData() {
        return parseGate3Data(getAndData());
    }

    public static String getOrData() {
        return orData;
    }

    public static void setOrData(String value) {
        orData = value;
    }

    public static Gate3 parseOrData() {
        return parseGate3Data(getOrData());
    }

    public static String getNandData() {
        return nandData;
    }

    public static void setNandData(String value) {
        nandData = value;
    }

    public static Gate3 parseNandData() {
        return parseGate3Data(getNandData());
    }

    public static String getNorData() {
        return norData;
    }

    public static void setNorData(String value) {
        norData = value;
    }

    public static Gate3 parseNorData() {
        return parseGate3Data(getNorData());
    }

    public static String getNandbData() {
        return nandbData;
    }

    public static void setNandbData(String value) {
        nandbData = value;
    }

    public static Gate3 parseNandbData() {
        return parseGate3Data(getNandbData());
    }

    public static String getNorbData() {
        return norbData;
    }

    public static void setNorbData(String value) {
        norbData = value;
    }

    public static Gate3 parseNorbData() {
        return parseGate3Data(getNorbData());
    }

    public static String getMutexData() {
        return mutexData;
    }

    public static void setMutexData(String value) {
        mutexData = value;
    }

    public static Mutex parseMutexData() {
        return parseMutexData(getMutexData());
    }

    public static String getBusSuffix() {
        return busSuffix;
    }

    public static void setBusSuffix(String value) {
        busSuffix = value;
    }

    public static String getResetPort() {
        return resetPort;
    }

    public static void setResetPort(String value) {
        resetPort = value;
    }

    public static String getSetPin() {
        return setPin;
    }

    public static void setSetPin(String value) {
        setPin = value;
    }

    public static String getClearPin() {
        return clearPin;
    }

    public static void setClearPin(String value) {
        clearPin = value;
    }

    public static String getTbufData() {
        return tbufData;
    }

    public static void setTbufData(String value) {
        tbufData = value;
    }

    public static Gate2 parseTbufData() {
        return parseGate2Data(getTbufData());
    }

    public static String getTinvData() {
        return tinvData;
    }

    public static void setTinvData(String value) {
        tinvData = value;
    }

    public static Gate2 parseTinvData() {
        return parseGate2Data(getTinvData());
    }

    public static String getScanSuffix() {
        return scanSuffix;
    }

    public static void setScanSuffix(String value) {
        scanSuffix = value;
    }

    public static String getScaninPortPin() {
        return scaninPortPin;
    }

    public static void setScaninPortPin(String value) {
        scaninPortPin = value;
    }

    public static Pair<String, String> parseScaninPortPin() {
        return parsePortPinPair(getScaninPortPin());
    }

    public static String getScanoutPortPin() {
        return scanoutPortPin;
    }

    public static void setScanoutPortPin(String value) {
        scanoutPortPin = value;
    }

    public static Pair<String, String> parseScanoutPortPin() {
        return parsePortPinPair(getScanoutPortPin());
    }

    public static String getScanckPortPin() {
        return scanckPortPin;
    }

    public static void setScanckPortPin(String value) {
        scanckPortPin = value;
    }

    public static Pair<String, String> parseScanckPortPin() {
        return parsePortPinPair(getScanckPortPin());
    }

    public static String getScanenPortPin() {
        return scanenPortPin;
    }

    public static void setScanenPortPin(String value) {
        scanenPortPin = value;
    }

    public static Pair<String, String> parseScanenPortPin() {
        return parsePortPinPair(getScanenPortPin());
    }

    public static String getScantmPortPin() {
        return scantmPortPin;
    }

    public static void setScantmPortPin(String value) {
        scantmPortPin = value;
    }

    public static Pair<String, String> parseScantmPortPin() {
        return parsePortPinPair(getScantmPortPin());
    }

    public static boolean getVerilogAssignDelay() {
        return verilogAssignDelay;
    }

    public static void setVerilogAssignDelay(boolean value) {
        verilogAssignDelay = value;
    }

    private static void setGate2Data(String value, Consumer<String> setter, String msg, String defaultValue) {
        if (parseGate2Data(value) != null) {
            setter.accept(value);
        } else {
            errorDescriptionFormat(msg, defaultValue);
        }
    }

    private static Gate2 parseGate2Data(String value) {
        Gate2 result = null;
        Matcher matcher = GATE2_DATA_PATTERN.matcher(value.replaceAll("\\s", ""));
        if (matcher.find()) {
            String name = matcher.group(GATE_NAME_GROUP);
            String in = matcher.group(GATE_PIN1_GROUP);
            String out = matcher.group(GATE_PIN2_GROUP);
            result = new Gate2(name, in, out);
        }
        return result;
    }

    private static void setGate3Data(String value, Consumer<String> setter, String msg, String defaultValue) {
        if (parseGate3Data(value) != null) {
            setter.accept(value);
        } else {
            errorDescriptionFormat(msg, defaultValue);
        }
    }

    private static Gate3 parseGate3Data(String value) {
        Gate3 result = null;
        Matcher matcher = GATE3_DATA_PATTERN.matcher(value.replaceAll("\\s", ""));
        if (matcher.find()) {
            String name = matcher.group(GATE_NAME_GROUP);
            String in1 = matcher.group(GATE_PIN1_GROUP);
            String in2 = matcher.group(GATE_PIN2_GROUP);
            String out = matcher.group(GATE_PIN3_GROUP);
            result = new Gate3(name, in1, in2, out);
        }
        return result;
    }

    private static Mutex parseMutexData(String str) {
        Mutex result = null;
        Matcher matcher = MUTEX_DATA_PATTERN.matcher(str.replaceAll("\\s", ""));
        if (matcher.find()) {
            String name = matcher.group(MUTEX_NAME_GROUP);
            Signal r1 = new Signal(matcher.group(MUTEX_R1_GROUP), Signal.Type.INPUT);
            Signal g1 = new Signal(matcher.group(MUTEX_G1_GROUP), Signal.Type.OUTPUT);
            Signal r2 = new Signal(matcher.group(MUTEX_R2_GROUP), Signal.Type.INPUT);
            Signal g2 = new Signal(matcher.group(MUTEX_G2_GROUP), Signal.Type.OUTPUT);
            result = new Mutex(name, r1, g1, r2, g2);
        }
        return result;
    }

    private static void setPortPinPair(String value, Consumer<String> setter, String msg) {
        if (parsePortPinPair(value) != null) {
            setter.accept(value);
        } else {
            errorPortPinFormat(msg);
        }
    }

    public static Pair<String, String> parsePortPinPair(String value) {
        String portName = null;
        String pinName = null;
        if ((value != null) && !value.isEmpty()) {
            String[] split = value.replaceAll("\\s", "").split("/");
            if (split.length > 0) {
                portName = split[0];
            }
            if (split.length > 1) {
                pinName = split[1];
            } else {
                pinName = portName;
            }
        }
        return Pair.of(portName, pinName);
    }
}
