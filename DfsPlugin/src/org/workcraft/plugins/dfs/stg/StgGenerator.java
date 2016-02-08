package org.workcraft.plugins.dfs.stg;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.Positioning;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.dfs.ControlRegister.SynchronisationType;
import org.workcraft.plugins.dfs.DfsSettings;
import org.workcraft.plugins.dfs.DfsSettings.Palette;
import org.workcraft.plugins.dfs.VisualBinaryRegister;
import org.workcraft.plugins.dfs.VisualControlConnection;
import org.workcraft.plugins.dfs.VisualControlRegister;
import org.workcraft.plugins.dfs.VisualCounterflowLogic;
import org.workcraft.plugins.dfs.VisualCounterflowRegister;
import org.workcraft.plugins.dfs.VisualDfs;
import org.workcraft.plugins.dfs.VisualLogic;
import org.workcraft.plugins.dfs.VisualPopRegister;
import org.workcraft.plugins.dfs.VisualPushRegister;
import org.workcraft.plugins.dfs.VisualRegister;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.VisualSignalTransition;
import org.workcraft.plugins.stg.generator.NodeStg;
import org.workcraft.util.ColorUtils;
import org.workcraft.util.ColorGenerator;
import org.workcraft.util.Hierarchy;

public class StgGenerator extends org.workcraft.plugins.stg.generator.StgGenerator {
    public static final String nameC         = "C_";
    public static final String nameFwC         = "fwC_";
    public static final String nameBwC         = "bwC_";
    public static final String nameM         = "M_";
    public static final String nameOrM         = "orM_";
    public static final String nameAndM        = "andM_";
    public static final String nameTrueM     = "trueM_";
    public static final String nameFalseM    = "falseM_";
    public static final String name0             = "_0";
    public static final String name1         = "_1";
    public static final String labelC         = "C(";
    public static final String labelFwC        = "fwC(";
    public static final String labelBwC        = "bwC(";
    public static final String labelM         = "M(";
    public static final String labelOrM        = "orM(";
    public static final String labelAndM     = "andM(";
    public static final String labelTrueM    = "trueM(";
    public static final String labelFalseM    = "falseM(";
    public static final String label0         = ")=0";
    public static final String label1         = ")=1";

    private Map<VisualLogic, LogicStg> logicMap;
    private Map<VisualRegister, RegisterStg> registerMap;
    private Map<VisualCounterflowLogic, CounterflowLogicStg> counterflowLogicMap;
    private Map<VisualCounterflowRegister, CounterflowRegisterStg> counterflowRegisterMap;
    private Map<VisualControlRegister, BinaryRegisterStg> controlRegisterMap;
    private Map<VisualPushRegister, BinaryRegisterStg> pushRegisterMap;
    private Map<VisualPopRegister, BinaryRegisterStg> popRegisterMap;

    public StgGenerator(VisualDfs dfs) {
        super(dfs);
    }

    private VisualDfs getDfsModel() {
        return (VisualDfs)getSrcModel();
    }

    @Override
    public Point2D getScale() {
        return new Point2D.Double(6.0, 6.0);
    }

    @Override
    public void convert() {
        try {
            for(VisualLogic l : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualLogic.class)) {
                LogicStg lstg = generateLogicStg(l);
                groupComponentStg(lstg);
                putLogicStg(l, lstg);
            }
            for(VisualRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualRegister.class)) {
                RegisterStg rstg = generateRegisterSTG(r);
                groupComponentStg(rstg);
                putRegisterStg(r, rstg);
            }

            for(VisualCounterflowLogic l : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualCounterflowLogic.class)) {
                CounterflowLogicStg lstg = generateCounterflowLogicStg(l);
                groupComponentStg(lstg);
                putCounterflowLogicStg(l, lstg);
            }
            for(VisualCounterflowRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualCounterflowRegister.class)) {
                CounterflowRegisterStg rstg = generateCounterflowRegisterSTG(r);
                groupComponentStg(rstg);
                putCounterflowRegisterStg(r, rstg);
            }

            for(VisualControlRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualControlRegister.class)) {
                BinaryRegisterStg rstg = generateControlRegisterStg(r);
                groupComponentStg(rstg);
                putControlRegisterStg(r, rstg);
            }
            for(VisualPushRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualPushRegister.class)) {
                BinaryRegisterStg rstg = generatePushRegisterStg(r);
                groupComponentStg(rstg);
                putPushRegisterStg(r, rstg);
            }
            for(VisualPopRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualPopRegister.class)) {
                BinaryRegisterStg rstg = generatePopRegisterStg(r);
                groupComponentStg(rstg);
                putPopRegisterStg(r, rstg);
            }

            for(VisualLogic l : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualLogic.class)) {
                connectLogicStg(l);
            }
            for(VisualRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualRegister.class)) {
                connectRegisterStg(r);
            }

            for(VisualCounterflowLogic l : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualCounterflowLogic.class)) {
                connectCounterflowLogicStg(l);
            }
            for(VisualCounterflowRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualCounterflowRegister.class)) {
                connectCounterflowRegisterStg(r);
            }

            for(VisualControlRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualControlRegister.class)) {
                connectControlRegisterStg(r);
            }
            for(VisualPushRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualPushRegister.class)) {
                connectPushRegisterStg(r);
            }
            for(VisualPopRegister r : Hierarchy.getDescendantsOfType(getDfsModel().getRoot(), VisualPopRegister.class)) {
                connectPopRegisterStg(r);
            }
        } catch (InvalidConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    private Color[] tokenColors;;

    private ColorGenerator createColorGenerator(boolean required) {
        ColorGenerator result = null;
        if (required) {
            if (tokenColors == null) {
                Palette palette = DfsSettings.getTokenPalette();
                if (palette == Palette.GENERATED) {
                    tokenColors = ColorUtils.getHsbPalette(
                            new float[]{0.05f, 0.45f, 0.85f, 0.25f, 0.65f, 0.15f, 0.55f, 0.95f, 0.35f, 0.75f},
                            new float[]{0.50f},  new float[]{0.7f, 0.5f, 0.3f});
                } else {
                    tokenColors = palette.getColors();
                }
            }
            result = new ColorGenerator(tokenColors);
        }
        return result;
    }


    private LogicStg generateLogicStg(VisualLogic l) throws InvalidConnectionException {
        String name = getDfsModel().getName(l);
        Point2D pos = getComponentPosition(l);
        double x = pos.getX();
        double y = pos.getY();
        Collection<Node> nodes = new LinkedList<Node>();
        SignalTransition.Type type = SignalTransition.Type.INTERNAL;
        ColorGenerator tokenColorGenerator = createColorGenerator(getDfsModel().getPreset(l).size() == 0);


        Container curContainer = null;

        VisualPlace C0 = getStgModel().createPlace(nameC + name + name0, curContainer);
        C0.setLabel(labelC + name + label0);
        C0.setLabelPositioning(Positioning.BOTTOM);
        if (!l.getReferencedLogic().isComputed()) {
            C0.getReferencedPlace().setTokens(1);
        }
        C0.setForegroundColor(l.getForegroundColor());
        C0.setFillColor(l.getFillColor());
        setPosition(C0, x + 2.0, y + 1.0);
        nodes.add(C0);

        VisualPlace C1 = getStgModel().createPlace(nameC + name + name1, curContainer);
        C1.setLabel(labelC + name + label1);
        C1.setLabelPositioning(Positioning.TOP);
        if (l.getReferencedLogic().isComputed()) {
            C1.getReferencedPlace().setTokens(1);
        }
        C1.setForegroundColor(l.getForegroundColor());
        C1.setFillColor(l.getFillColor());
        setPosition(C1, x + 2.0, y - 1.0);
        nodes.add(C1);

        Set<Node> preset = new HashSet<Node>();
        preset.addAll(getDfsModel().getPreset(l, VisualLogic.class));
        preset.addAll(getDfsModel().getPreset(l, VisualRegister.class));
        preset.addAll(getDfsModel().getPreset(l, VisualControlRegister.class));
        preset.addAll(getDfsModel().getPreset(l, VisualPushRegister.class));
        preset.addAll(getDfsModel().getPreset(l, VisualPopRegister.class));
        if (preset.size() == 0) {
            preset.add(l);
        }
        Map<Node, VisualSignalTransition> CRs = new HashMap<Node, VisualSignalTransition>();
        Map<Node, VisualSignalTransition> CFs = new HashMap<Node, VisualSignalTransition>();
        VisualSignalTransition CR = null;
        VisualSignalTransition CF = null;
        double dy = 0.0;
        for (Node n: preset) {
            if (CR == null || l.getReferencedLogic().isEarlyEvaluation()) {
                CR = getStgModel().createSignalTransition(nameC + name, type, SignalTransition.Direction.PLUS, curContainer);
                CR.setTokenColorGenerator(tokenColorGenerator);
                createConsumingArc(C0, CR, false);
                createProducingArc(CR, C1, true);
                setPosition(CR, x - 2.0, y + 1.0 + dy);
                nodes.add(CR);
            }
            CRs.put(n, CR);
            if (CF == null) {
                CF = getStgModel().createSignalTransition(nameC + name, type, SignalTransition.Direction.MINUS, curContainer);
                createConsumingArc(C1, CF, false);
                createProducingArc(CF, C0, false);
                setPosition(CF, x - 2.0, y - 1.0 - dy);
                nodes.add(CF);
            }
            CFs.put(n, CF);
            dy += 1.0;
        }

        return new LogicStg(C0, C1, CRs, CFs);
    }

    private void connectLogicStg(VisualLogic l) throws InvalidConnectionException {
        LogicStg lstg = getLogicStg(l);
        for (VisualLogic n: getDfsModel().getPreset(l, VisualLogic.class)) {
            LogicStg nstg = getLogicStg(n);
            createReadArc(nstg.C1, lstg.CRs.get(n), true);
            createReadArc(nstg.C0, lstg.CFs.get(n), false);
        }
        for (VisualRegister n: getDfsModel().getPreset(l, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, lstg.CRs.get(n), true);
            createReadArc(nstg.M0, lstg.CFs.get(n), false);
        }
        for (VisualControlRegister n: getDfsModel().getPreset(l, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            createReadArc(nstg.M1, lstg.CRs.get(n), true);
            createReadArc(nstg.M0, lstg.CFs.get(n), false);
        }
        for (VisualPushRegister n: getDfsModel().getPreset(l, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArc(nstg.tM1, lstg.CRs.get(n), true);
            createReadArc(nstg.tM0, lstg.CFs.get(n), false);
        }
        for (VisualPopRegister n: getDfsModel().getPreset(l, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArc(nstg.M1, lstg.CRs.get(n), true);
            createReadArc(nstg.M0, lstg.CFs.get(n), false);
        }
    }

    public LogicStg getLogicStg(VisualLogic logic) {
        return ((logicMap == null) ? null : logicMap.get(logic));
    }

    public void putLogicStg(VisualLogic logic, LogicStg stg) {
        if (logicMap == null) {
            logicMap = new HashMap<>();
        }
        logicMap.put(logic, stg);
    }


    private RegisterStg generateRegisterSTG(VisualRegister r) throws InvalidConnectionException {
        String name = getDfsModel().getName(r);
        Point2D pos = getComponentPosition(r);
        double x = pos.getX();
        double y = pos.getY();
        Collection<Node> nodes = new LinkedList<Node>();
        SignalTransition.Type type = SignalTransition.Type.INTERNAL;
        if (getDfsModel().getPreset(r).size() == 0) {
            type = SignalTransition.Type.INPUT;
        } else if (getDfsModel().getPostset(r).size() == 0) {
            type = SignalTransition.Type.OUTPUT;
        }
        ColorGenerator tokenColorGenerator = createColorGenerator(getDfsModel().getPreset(r).size() == 0);
        Container curContainer = null;

        VisualPlace M0 = getStgModel().createPlace(nameM + name + name0, curContainer);
        M0.setLabel(labelM + name + label0);
        M0.setLabelPositioning(Positioning.BOTTOM);
        if (!r.getReferencedRegister().isMarked()) {
            M0.getReferencedPlace().setTokens(1);
        }
        M0.setForegroundColor(r.getForegroundColor());
        M0.setFillColor(r.getFillColor());
        setPosition(M0, x + 2.0, y + 1.0);
        nodes.add(M0);

        VisualPlace M1 = getStgModel().createPlace(nameM + name + name1, curContainer);
        M1.setLabel(labelM + name + label1);
        M1.setLabelPositioning(Positioning.TOP);
        if (r.getReferencedRegister().isMarked()) {
            M1.getReferencedPlace().setTokens(1);
        }
        M1.setTokenColor(r.getTokenColor());
        setPosition(M1, x + 2.0, y - 1.0);
        nodes.add(M1);

        VisualSignalTransition MR = getStgModel().createSignalTransition(nameM + name, type, SignalTransition.Direction.PLUS, curContainer);
        MR.setTokenColorGenerator(tokenColorGenerator);
        createConsumingArc(M0, MR, false);
        createProducingArc(MR, M1, true);
        setPosition(MR, x - 2.0, y + 1.0);
        nodes.add(MR);

        VisualSignalTransition MF = getStgModel().createSignalTransition(nameM + name, type, SignalTransition.Direction.MINUS, curContainer);
        createConsumingArc(M1, MF, false);
        createProducingArc(MF, M0, false);
        setPosition(MF, x - 2.0, y - 1.0);
        nodes.add(MF);

        return new RegisterStg(M0, M1, MR, MF);
    }

    private void connectRegisterStg(VisualRegister r) throws InvalidConnectionException {
        RegisterStg rstg = getRegisterStg(r);
        // preset
        for (VisualLogic n: getDfsModel().getPreset(r, VisualLogic.class)) {
            LogicStg nstg = getLogicStg(n);
            createReadArc(nstg.C1, rstg.MR, true);
            createReadArc(nstg.C0, rstg.MF, false);
        }
        // R-preset
        for (VisualRegister n: getDfsModel().getRPreset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.MR, true);
            createReadArc(nstg.M0, rstg.MF, false);
        }
        for (VisualCounterflowRegister n: getDfsModel().getRPreset(r, VisualCounterflowRegister.class)) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.orM1, rstg.MR, true);
            createReadArc(nstg.orM0, rstg.MF, false);
            createReadArc(nstg.andM1, rstg.MF, false);
            createReadArc(nstg.andM0, rstg.MR, false);
        }
        for (VisualControlRegister n: getDfsModel().getRPreset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            createReadArc(nstg.M1, rstg.MR, true);
            createReadArc(nstg.M0, rstg.MF, false);
        }
        for (VisualPushRegister n: getDfsModel().getRPreset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArc(nstg.tM1, rstg.MR, true);
            createReadArc(nstg.tM0, rstg.MF, false);
        }
        for (VisualPopRegister n: getDfsModel().getRPreset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArc(nstg.M1, rstg.MR, true);
            createReadArc(nstg.M0, rstg.MF, false);
        }
        // R-postset
        for (VisualRegister n: getDfsModel().getRPostset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.MF, false);
            createReadArc(nstg.M0, rstg.MR, false);
        }
        for (VisualCounterflowRegister n: getDfsModel().getRPostset(r, VisualCounterflowRegister.class)) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.andM1, rstg.MF, false);
            createReadArc(nstg.andM0, rstg.MR, false);
        }
        for (VisualControlRegister n: getDfsModel().getRPostset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            createReadArc(nstg.M1, rstg.MF, false);
            createReadArc(nstg.M0, rstg.MR, false);
        }
        for (VisualPushRegister n: getDfsModel().getRPostset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArc(nstg.M1, rstg.MF, false);
            createReadArc(nstg.M0, rstg.MR, false);
        }
        for (VisualPopRegister n: getDfsModel().getRPostset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArc(nstg.tM1, rstg.MF, false);
            createReadArc(nstg.tM0, rstg.MR, false);
        }
    }

    public RegisterStg getRegisterStg(VisualRegister register) {
        return ((registerMap == null) ? null : registerMap.get(register));
    }

    public void putRegisterStg(VisualRegister register, RegisterStg stg) {
        if (registerMap == null) {
            registerMap = new HashMap<>();
        }
        registerMap.put(register, stg);
    }


    private CounterflowLogicStg generateCounterflowLogicStg(VisualCounterflowLogic l) throws InvalidConnectionException {
        String name = getDfsModel().getName(l);
        Point2D pos = getComponentPosition(l);
        double x = pos.getX();
        double y = pos.getY();
        Collection<Node> nodes = new LinkedList<Node>();
        SignalTransition.Type type = SignalTransition.Type.INTERNAL;
        ColorGenerator presetTokenColorGenerator = createColorGenerator(getDfsModel().getPreset(l).size() == 0);
        ColorGenerator postsetTokenColorGenerator = createColorGenerator(getDfsModel().getPostset(l).size() == 0);

        Container curContainer = null;

        VisualPlace fwC0 = getStgModel().createPlace(nameFwC + name + name0, curContainer);
        fwC0.setLabel(labelFwC + name + label0);
        fwC0.setLabelPositioning(Positioning.BOTTOM);
        if (!l.getReferencedCounterflowLogic().isForwardComputed()) {
            fwC0.getReferencedPlace().setTokens(1);
        }
        fwC0.setForegroundColor(l.getForegroundColor());
        fwC0.setFillColor(l.getFillColor());
        setPosition(fwC0, x + 2.0, y - 2.0);
        nodes.add(fwC0);

        VisualPlace fwC1 = getStgModel().createPlace(nameFwC + name + name1, curContainer);
        fwC1.setLabel(labelFwC + name + label1);
        fwC1.setLabelPositioning(Positioning.TOP);
        if (l.getReferencedCounterflowLogic().isForwardComputed()) {
            fwC1.getReferencedPlace().setTokens(1);
        }
        fwC1.setForegroundColor(l.getForegroundColor());
        fwC1.setFillColor(l.getFillColor());
        setPosition(fwC1, x + 2.0, y - 4.0);
        nodes.add(fwC1);

        Set<Node> preset = new HashSet<Node>();
        preset.addAll(getDfsModel().getPreset(l, VisualCounterflowLogic.class));
        preset.addAll(getDfsModel().getPreset(l, VisualCounterflowRegister.class));
        if (preset.size() == 0) {
            preset.add(l);
        }
        Map<Node, VisualSignalTransition> fwCRs = new HashMap<Node, VisualSignalTransition>();
        Map<Node, VisualSignalTransition> fwCFs = new HashMap<Node, VisualSignalTransition>();
        {
            VisualSignalTransition fwCR = null;
            VisualSignalTransition fwCF = null;
            double dy = 0.0;
            for (Node n: preset) {
                if (fwCR == null || l.getReferencedCounterflowLogic().isForwardEarlyEvaluation()) {
                    fwCR = getStgModel().createSignalTransition(nameFwC + name, type, SignalTransition.Direction.PLUS, curContainer);
                    fwCR.setTokenColorGenerator(presetTokenColorGenerator);
                    createConsumingArc(fwC0, fwCR, false);
                    createProducingArc(fwCR, fwC1, true);
                    setPosition(fwCR, x - 2.0, y - 2.0 + dy);
                    nodes.add(fwCR);
                }
                fwCRs.put(n, fwCR);
                if (fwCF == null) {
                    fwCF = getStgModel().createSignalTransition(nameFwC + name, type, SignalTransition.Direction.MINUS, curContainer);
                    createConsumingArc(fwC1, fwCF, false);
                    createProducingArc(fwCF, fwC0, false);
                    setPosition(fwCF, x - 2.0, y - 4.0 - dy);
                    nodes.add(fwCF);
                }
                fwCFs.put(n, fwCF);
                dy += 1.0;
            }
        }

        VisualPlace bwC0 = getStgModel().createPlace(nameBwC + name + name0, curContainer);
        bwC0.setLabel(labelBwC + name + label0);
        bwC0.setLabelPositioning(Positioning.BOTTOM);
        if (!l.getReferencedCounterflowLogic().isBackwardComputed()) {
            bwC0.getReferencedPlace().setTokens(1);
        }
        bwC0.setForegroundColor(l.getForegroundColor());
        bwC0.setFillColor(l.getFillColor());
        setPosition(bwC0, x + 2.0, y + 4.0);
        nodes.add(bwC0);

        VisualPlace bwC1 = getStgModel().createPlace(nameBwC + name + name1, curContainer);
        bwC1.setLabel(labelBwC + name + label1);
        bwC1.setLabelPositioning(Positioning.TOP);
        if (l.getReferencedCounterflowLogic().isBackwardComputed()) {
            bwC1.getReferencedPlace().setTokens(1);
        }
        bwC1.setForegroundColor(l.getForegroundColor());
        bwC1.setFillColor(l.getFillColor());
        setPosition(bwC1, x + 2.0, y + 2.0);
        nodes.add(bwC1);

        Set<Node> postset = new HashSet<Node>();
        postset.addAll(getDfsModel().getPostset(l, VisualCounterflowLogic.class));
        postset.addAll(getDfsModel().getPostset(l, VisualCounterflowRegister.class));
        if (postset.size() == 0) {
            postset.add(l);
        }
        Map<Node, VisualSignalTransition> bwCRs = new HashMap<Node, VisualSignalTransition>();
        Map<Node, VisualSignalTransition> bwCFs = new HashMap<Node, VisualSignalTransition>();
        {
            VisualSignalTransition bwCR = null;
            VisualSignalTransition bwCF = null;
            double dy = 0.0;
            for (Node n: postset) {
                if (bwCR == null || l.getReferencedCounterflowLogic().isBackwardEarlyEvaluation()) {
                    bwCR = getStgModel().createSignalTransition(nameBwC + name, type, SignalTransition.Direction.PLUS, curContainer);
                    bwCR.setTokenColorGenerator(postsetTokenColorGenerator);
                    createConsumingArc(bwC0, bwCR, false);
                    createProducingArc(bwCR, bwC1, false);
                    setPosition(bwCR, x - 2.0, y + 4.0 + dy);
                    nodes.add(bwCR);
                }
                bwCRs.put(n, bwCR);
                if (bwCF == null) {
                    bwCF = getStgModel().createSignalTransition(nameBwC + name, type, SignalTransition.Direction.MINUS, curContainer);
                    createConsumingArc(bwC1, bwCF, false);
                    createProducingArc(bwCF, bwC0, false);
                    setPosition(bwCF, x - 2.0, y + 2.0 - dy);
                    nodes.add(bwCF);
                }
                bwCFs.put(n, bwCF);
                dy += 1.0;
            }
        }

        return new CounterflowLogicStg(fwC0, fwC1, fwCRs, fwCFs, bwC0, bwC1, bwCRs, bwCFs);
    }

    private void connectCounterflowLogicStg(VisualCounterflowLogic l) throws InvalidConnectionException {
        CounterflowLogicStg lstg = getCounterflowLogicStg(l);
        // preset
        for (VisualCounterflowLogic n: getDfsModel().getPreset(l, VisualCounterflowLogic.class)) {
            CounterflowLogicStg nstg = getCounterflowLogicStg(n);
            createReadArc(nstg.fwC1, lstg.fwCRs.get(n), true);
            createReadArc(nstg.fwC0, lstg.fwCFs.get(n), false);
        }
        for (VisualCounterflowRegister n: getDfsModel().getPreset(l, VisualCounterflowRegister.class)) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.orM1, lstg.fwCRs.get(n), true);
            createReadArc(nstg.orM0, lstg.fwCFs.get(n), false);
        }
        // postset
        for (VisualCounterflowLogic n: getDfsModel().getPostset(l, VisualCounterflowLogic.class)) {
            CounterflowLogicStg nstg = getCounterflowLogicStg(n);
            createReadArc(nstg.bwC1, lstg.bwCRs.get(n), false);
            createReadArc(nstg.bwC0, lstg.bwCFs.get(n), false);
        }
        for (VisualCounterflowRegister n: getDfsModel().getPostset(l, VisualCounterflowRegister.class)) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.orM1, lstg.bwCRs.get(n), false);
            createReadArc(nstg.orM0, lstg.bwCFs.get(n), false);
        }
    }

    public CounterflowLogicStg getCounterflowLogicStg(VisualCounterflowLogic logic) {
        return ((counterflowLogicMap == null) ? null : counterflowLogicMap.get(logic));
    }

    public void putCounterflowLogicStg(VisualCounterflowLogic logic, CounterflowLogicStg stg) {
        if (counterflowLogicMap == null) {
            counterflowLogicMap = new HashMap<>();
        }
        counterflowLogicMap.put(logic, stg);
    }


    private CounterflowRegisterStg generateCounterflowRegisterSTG(VisualCounterflowRegister r) throws InvalidConnectionException {
        String name = getDfsModel().getName(r);
        Point2D pos = getComponentPosition(r);
        double x = pos.getX();
        double y = pos.getY();
        Collection<Node> nodes = new LinkedList<Node>();
        SignalTransition.Type type = SignalTransition.Type.INTERNAL;
        if (getDfsModel().getPreset(r).size() == 0 || getDfsModel().getPostset(r).size() == 0) {
            type = SignalTransition.Type.INPUT;
        }
        ColorGenerator presetTokenColorGenerator = createColorGenerator(getDfsModel().getPreset(r).size() == 0);
        ColorGenerator postsetTokenColorGenerator = createColorGenerator(getDfsModel().getPostset(r).size() == 0);

        Container curContainer = null;

        VisualPlace orM0 = getStgModel().createPlace(nameOrM + name + name0, curContainer);
        orM0.setLabel(labelOrM + name + label0);
        orM0.setLabelPositioning(Positioning.BOTTOM);
        if (!r.getReferencedCounterflowRegister().isOrMarked()) {
            orM0.getReferencedPlace().setTokens(1);
        }
        orM0.setForegroundColor(r.getForegroundColor());
        orM0.setFillColor(r.getFillColor());
        setPosition(orM0, x + 2.0, y - 2.0);
        nodes.add(orM0);

        VisualPlace orM1 = getStgModel().createPlace(nameOrM + name + name1, curContainer);
        orM1.setLabel(labelOrM + name + label1);
        orM1.setLabelPositioning(Positioning.TOP);
        if (r.getReferencedCounterflowRegister().isOrMarked()) {
            orM1.getReferencedPlace().setTokens(    1);
        }
        orM1.setForegroundColor(r.getForegroundColor());
        orM1.setFillColor(r.getFillColor());
        setPosition(orM1, x + 2.0, y - 4.0);
        nodes.add(orM1);

        VisualSignalTransition orMRfw = getStgModel().createSignalTransition(nameOrM + name, type, SignalTransition.Direction.PLUS, curContainer);
        orMRfw.setTokenColorGenerator(presetTokenColorGenerator);
        createConsumingArc(orM0, orMRfw, false);
        createProducingArc(orMRfw, orM1, true);
        setPosition(orMRfw, x - 2.0, y - 2.5);
        nodes.add(orMRfw);

        VisualSignalTransition orMRbw = getStgModel().createSignalTransition(nameOrM + name, type, SignalTransition.Direction.PLUS, curContainer);
        orMRbw.setTokenColorGenerator(postsetTokenColorGenerator);
        createConsumingArc(orM0, orMRbw, false);
        createProducingArc(orMRbw, orM1, true);
        setPosition(orMRbw, x - 2.0, y - 1.5);
        nodes.add(orMRbw);

        VisualSignalTransition orMFfw = getStgModel().createSignalTransition(nameOrM + name, type, SignalTransition.Direction.MINUS, curContainer);
        createConsumingArc(orM1, orMFfw, false);
        createProducingArc(orMFfw, orM0, false);
        setPosition(orMFfw, x - 2.0, y - 4.5);
        nodes.add(orMFfw);

        VisualSignalTransition orMFbw = getStgModel().createSignalTransition(nameOrM + name, type, SignalTransition.Direction.MINUS, curContainer);
        createConsumingArc(orM1, orMFbw, false);
        createProducingArc(orMFbw, orM0, false);
        setPosition(orMFbw, x - 2.0, y - 3.5);
        nodes.add(orMFbw);

        VisualPlace andM0 = getStgModel().createPlace(nameAndM + name + name0, curContainer);
        andM0.setLabel(labelAndM + name + label0);
        andM0.setLabelPositioning(Positioning.BOTTOM);
        if (!r.getReferencedCounterflowRegister().isAndMarked()) {
            andM0.getReferencedPlace().setTokens(1);
        }
        andM0.setForegroundColor(r.getForegroundColor());
        andM0.setFillColor(r.getFillColor());
        setPosition(andM0, x + 2.0, y + 4.0);
        nodes.add(andM0);

        VisualPlace andM1 = getStgModel().createPlace(nameAndM + name + name1, curContainer);
        andM1.setLabel(labelAndM + name + label1);
        andM1.setLabelPositioning(Positioning.TOP);
        if (r.getReferencedCounterflowRegister().isAndMarked()) {
            andM1.getReferencedPlace().setTokens(1);
        }
        andM1.setForegroundColor(r.getForegroundColor());
        andM1.setFillColor(r.getFillColor());
        setPosition(andM1, x + 2.0, y + 2.0);
        nodes.add(andM1);

        VisualSignalTransition andMR = getStgModel().createSignalTransition(nameAndM + name, type, SignalTransition.Direction.PLUS, curContainer);
        createConsumingArc(andM0, andMR, false);
        createProducingArc(andMR, andM1, false);
        setPosition(andMR, x - 2.0, y + 4.0);
        nodes.add(andMR);

        VisualSignalTransition andMF = getStgModel().createSignalTransition(nameAndM + name, type, SignalTransition.Direction.MINUS, curContainer);
        createConsumingArc(andM1, andMF, false);
        createProducingArc(andMF, andM0, false);
        setPosition(andMF, x - 2.0, y + 2.0);
        nodes.add(andMF);

        return new CounterflowRegisterStg(orM0, orM1, orMRfw, orMRbw, orMFfw, orMFbw, andM0, andM1, andMR, andMF);
    }

    private void connectCounterflowRegisterStg(VisualCounterflowRegister r) throws InvalidConnectionException {
        CounterflowRegisterStg rstg = getCounterflowRegisterStg(r);

        for (VisualRegister n: getDfsModel().getPreset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.orMRfw, true);
            createReadArc(nstg.M1, rstg.andMR, false);
            createReadArc(nstg.M0, rstg.orMFfw, false);
            createReadArc(nstg.M0, rstg.andMF, false);
        }
        for (VisualRegister n: getDfsModel().getPostset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.orMRbw, true);
            createReadArc(nstg.M1, rstg.andMR, false);
            createReadArc(nstg.M0, rstg.orMFbw, false);
            createReadArc(nstg.M0, rstg.andMF, false);
        }

        for (VisualCounterflowLogic n: getDfsModel().getPreset(r, VisualCounterflowLogic.class)) {
            CounterflowLogicStg nstg = getCounterflowLogicStg(n);
            createReadArc(nstg.fwC1, rstg.orMRfw, true);
            createReadArc(nstg.fwC0, rstg.orMFfw, false);
            createReadArc(nstg.fwC1, rstg.andMR, false);
            createReadArc(nstg.fwC0, rstg.andMF, false);
        }
        for (VisualCounterflowLogic n: getDfsModel().getPostset(r, VisualCounterflowLogic.class)) {
            CounterflowLogicStg nstg = getCounterflowLogicStg(n);
            createReadArc(nstg.bwC1, rstg.orMRbw, true);
            createReadArc(nstg.bwC0, rstg.orMFbw, false);
            createReadArc(nstg.bwC1, rstg.andMR, false);
            createReadArc(nstg.bwC0, rstg.andMF, false);
        }

        for (VisualCounterflowRegister n: getDfsModel().getPreset(r, VisualCounterflowRegister.class)) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.orM1, rstg.orMRfw, true);
            createReadArc(nstg.orM0, rstg.orMFfw, false);
        }
        for (VisualCounterflowRegister n: getDfsModel().getPostset(r, VisualCounterflowRegister.class)) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.orM1, rstg.orMRbw, true);
            createReadArc(nstg.orM0, rstg.orMFbw, false);
        }

        Set<VisualCounterflowRegister> rSet = new HashSet<VisualCounterflowRegister>();
        rSet.add(r);
        rSet.addAll(getDfsModel().getRPreset(r, VisualCounterflowRegister.class));
        rSet.addAll(getDfsModel().getRPostset(r, VisualCounterflowRegister.class));
        for (VisualCounterflowRegister n: rSet) {
            CounterflowRegisterStg nstg = getCounterflowRegisterStg(n);
            createReadArc(nstg.orM1, rstg.andMR, true);
            createReadArc(nstg.orM0, rstg.andMF, false);
            createReadArc(nstg.andM1, rstg.orMFfw, false);
            createReadArc(nstg.andM1, rstg.orMFbw, false);
            createReadArc(nstg.andM0, rstg.orMRfw, false);
            createReadArc(nstg.andM0, rstg.orMRbw, false);
        }
    }

    public CounterflowRegisterStg getCounterflowRegisterStg(VisualCounterflowRegister register) {
        return ((counterflowRegisterMap == null) ? null: counterflowRegisterMap.get(register));
    }

    public void putCounterflowRegisterStg(VisualCounterflowRegister register, CounterflowRegisterStg stg) {
        if (counterflowRegisterMap == null) {
            counterflowRegisterMap = new HashMap<>();
        }
        counterflowRegisterMap.put(register, stg);
    }


    private BinaryRegisterStg generateBinaryRegisterSTG(VisualBinaryRegister r,
            boolean andSync, boolean orSync) throws InvalidConnectionException {
        Collection<Node> nodes = new LinkedList<Node>();
        String name = getDfsModel().getName(r);
        Point2D pos = getComponentPosition(r);
        double x = pos.getX();
        double y = pos.getY();
        SignalTransition.Type type = SignalTransition.Type.INTERNAL;
        if (getDfsModel().getPreset(r, VisualControlRegister.class).size() == 0) {
            type = SignalTransition.Type.INPUT;
        } else if (getDfsModel().getPostset(r).size() == 0) {
            type = SignalTransition.Type.OUTPUT;
        }
        ColorGenerator tokenColorGenerator = createColorGenerator(getDfsModel().getPreset(r).size() == 0);
        Container curContainer = null;

        VisualPlace M0 = getStgModel().createPlace(nameM + name + name0, curContainer);
        M0.setLabel(labelM + name + label0);
        M0.setLabelPositioning(Positioning.BOTTOM);
        if (!r.getReferencedBinaryRegister().isTrueMarked() && !r.getReferencedBinaryRegister().isFalseMarked()) {
            M0.getReferencedPlace().setTokens(1);
        }
        M0.setForegroundColor(r.getForegroundColor());
        M0.setFillColor(r.getFillColor());
        setPosition(M0, x - 4.0, y + 1.0);
        nodes.add(M0);

        VisualPlace M1 = getStgModel().createPlace(nameM + name + name1, curContainer);
        M1.setLabel(labelM + name + label1);
        M1.setLabelPositioning(Positioning.TOP);
        if (r.getReferencedBinaryRegister().isTrueMarked() || r.getReferencedBinaryRegister().isFalseMarked()) {
            M1.getReferencedPlace().setTokens(1);
        }
        M1.setForegroundColor(r.getForegroundColor());
        M1.setFillColor(r.getFillColor());
        setPosition(M1, x - 4.0, y - 1.0);
        nodes.add(M1);

        VisualPlace tM0 = getStgModel().createPlace(nameTrueM + name + name0, curContainer);
        tM0.setLabel(labelTrueM + name + label0);
        tM0.setLabelPositioning(Positioning.BOTTOM);
        if (!r.getReferencedBinaryRegister().isTrueMarked()) {
            tM0.getReferencedPlace().setTokens(1);
        }
        tM0.setForegroundColor(r.getForegroundColor());
        tM0.setFillColor(r.getFillColor());
        setPosition(tM0, x + 4.0, y - 2.0);
        nodes.add(tM0);

        VisualPlace tM1 = getStgModel().createPlace(nameTrueM + name + name1, curContainer);
        tM1.setLabel(labelTrueM + name + label1);
        tM1.setLabelPositioning(Positioning.TOP);
        if (r.getReferencedBinaryRegister().isTrueMarked()) {
            tM1.getReferencedPlace().setTokens(1);
        }
        tM1.setForegroundColor(r.getForegroundColor());
        tM1.setFillColor(r.getFillColor());
        setPosition(tM1, x + 4.0, y - 4.0);
        nodes.add(tM1);

        Set<Node> preset = new HashSet<Node>();
        preset.addAll(getDfsModel().getRPreset(r, VisualControlRegister.class));
        if (preset.size() == 0) {
            preset.add(r);
        }

        Map<Node, VisualSignalTransition> tMRs = new HashMap<Node, VisualSignalTransition>();
        VisualSignalTransition tMR = null;
        double dy = 0.0;
        for (Node n: preset) {
            if (tMR == null || orSync) {
                tMR = getStgModel().createSignalTransition(nameTrueM + name, type, SignalTransition.Direction.PLUS, curContainer);
                tMR.setTokenColorGenerator(tokenColorGenerator);
                createConsumingArc(tM0, tMR, false);
                createProducingArc(tMR, tM1, true);
                createConsumingArc(M0, tMR, false);
                createProducingArc(tMR, M1, true);
                setPosition(tMR, x, y - 2.0 + dy);
                nodes.add(tMR);
            }
            tMRs.put(n, tMR);
            dy += 1.0;
        }
        VisualSignalTransition tMF = getStgModel().createSignalTransition(nameTrueM + name, type, SignalTransition.Direction.MINUS, curContainer);
        createConsumingArc(tM1, tMF, false);
        createProducingArc(tMF, tM0, false);
        createConsumingArc(M1, tMF, false);
        createProducingArc(tMF, M0, false);
        setPosition(tMF, x, y - 4.0 - dy);
        nodes.add(tMF);


        VisualPlace fM0 = getStgModel().createPlace(nameFalseM + name + name0, curContainer);
        fM0.setLabel(labelFalseM + name + label0);
        fM0.setLabelPositioning(Positioning.BOTTOM);
        if (!r.getReferencedBinaryRegister().isFalseMarked()) {
            fM0.getReferencedPlace().setTokens(1);
        }
        fM0.setForegroundColor(r.getForegroundColor());
        fM0.setFillColor(r.getFillColor());
        setPosition(fM0, x + 4.0, y + 4.0);
        nodes.add(fM0);

        VisualPlace fM1 = getStgModel().createPlace(nameFalseM + name + name1, curContainer);
        fM1.setLabel(labelFalseM + name + label1);
        fM1.setLabelPositioning(Positioning.TOP);
        if (r.getReferencedBinaryRegister().isFalseMarked()) {
            fM1.getReferencedPlace().setTokens(1);
        }
        fM1.setForegroundColor(r.getForegroundColor());
        fM1.setFillColor(r.getFillColor());
        setPosition(fM1, x + 4.0, y + 2.0);
        nodes.add(fM1);

        Map<Node, VisualSignalTransition> fMRs = new HashMap<Node, VisualSignalTransition>();
        VisualSignalTransition fMR = null;
        dy = 0.0;
        for (Node n: preset) {
            if (fMR == null || andSync) {
                fMR = getStgModel().createSignalTransition(nameFalseM + name, type, SignalTransition.Direction.PLUS, curContainer);
                fMR.setTokenColorGenerator(tokenColorGenerator);
                createConsumingArc(fM0, fMR, false);
                createProducingArc(fMR, fM1, true);
                createConsumingArc(M0, fMR, false);
                createProducingArc(fMR, M1, true);
                setPosition(fMR, x, y + 4.0 + dy);
                nodes.add(fMR);
            }
            fMRs.put(n, fMR);
            dy += 1.0;
        }
        VisualSignalTransition fMF = getStgModel().createSignalTransition(nameFalseM + name, type, SignalTransition.Direction.MINUS, curContainer);
        createConsumingArc(fM1, fMF, false);
        createProducingArc(fMF, fM0, false);
        createConsumingArc(M1, fMF, false);
        createProducingArc(fMF, M0, false);
        setPosition(fMF, x, y + 2.0 - dy);
        nodes.add(fMF);

        // mutual exclusion
        createReadArcs(tM0, fMRs.values(), false);
        createReadArcs(fM0, tMRs.values(), false);

        return new BinaryRegisterStg(M0, M1, tM0, tM1, tMRs, tMF, fM0, fM1, fMRs, fMF);
    }

    private BinaryRegisterStg generateControlRegisterStg(VisualControlRegister r) throws InvalidConnectionException {
        boolean andSync = (r.getReferencedControlRegister().getSynchronisationType() == SynchronisationType.AND);
        boolean orSync = (r.getReferencedControlRegister().getSynchronisationType() == SynchronisationType.OR);
        return generateBinaryRegisterSTG(r, andSync, orSync);
    }

    private void connectControlRegisterStg(VisualControlRegister r) throws InvalidConnectionException {
        BinaryRegisterStg rstg = getControlRegisterStg(r);
        // preset
        for (VisualLogic n: getDfsModel().getPreset(r, VisualLogic.class)) {
            LogicStg nstg = getLogicStg(n);
            createReadArcs(nstg.C1, rstg.tMRs.values(), true);
            createReadArcs(nstg.C1, rstg.fMRs.values(), true);
            createReadArc(nstg.C0, rstg.tMF, false);
            createReadArc(nstg.C0, rstg.fMF, false);
        }
        // R-preset
        for (VisualRegister n: getDfsModel().getRPreset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArcs(nstg.M1, rstg.tMRs.values(), true);
            createReadArcs(nstg.M1, rstg.fMRs.values(), true);
            createReadArc(nstg.M0, rstg.tMF, false);
            createReadArc(nstg.M0, rstg.fMF, false);
        }
        Collection<VisualControlRegister> crPreset = getDfsModel().getRPreset(r, VisualControlRegister.class);
        for (VisualControlRegister n: crPreset) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            Connection connection = getDfsModel().getConnection(n, r);
            if (connection instanceof VisualControlConnection && ((VisualControlConnection)connection).getReferencedControlConnection().isInverting()) {
                createReadArc(nstg.tM1, rstg.fMRs.get(n), true);
                createReadArc(nstg.fM1, rstg.tMRs.get(n), true);
            } else {
                createReadArc(nstg.tM1, rstg.tMRs.get(n), true);
                createReadArc(nstg.fM1, rstg.fMRs.get(n), true);
            }
            if (r.getReferencedControlRegister().getSynchronisationType() != SynchronisationType.PLAIN) {
                for (VisualControlRegister m: crPreset) {
                    if (m == n) continue;
                    BinaryRegisterStg mstg = getControlRegisterStg(m);
                    if (r.getReferencedControlRegister().getSynchronisationType() == SynchronisationType.OR) {
                        createReadArc(mstg.M1, rstg.tMRs.get(n), true);
                    }
                    if (r.getReferencedControlRegister().getSynchronisationType() == SynchronisationType.AND) {
                        createReadArc(mstg.M1, rstg.fMRs.get(n), true);
                    }
                }
            }
            createReadArc(nstg.M0, rstg.tMF, false);
            createReadArc(nstg.M0, rstg.fMF, false);
        }
        for (VisualPushRegister n: getDfsModel().getRPreset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArcs(nstg.tM1, rstg.tMRs.values(), true);
            createReadArcs(nstg.tM1, rstg.fMRs.values(), true);
            createReadArc(nstg.tM0, rstg.tMF, false);
            createReadArc(nstg.tM0, rstg.fMF, false);
        }
        for (VisualPopRegister n: getDfsModel().getRPreset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArcs(nstg.M1, rstg.tMRs.values(), true);
            createReadArcs(nstg.M1, rstg.fMRs.values(), true);
            createReadArc(nstg.M0, rstg.tMF, false);
            createReadArc(nstg.M0, rstg.fMF, false);
        }
        // R-postset
        for (VisualRegister n: getDfsModel().getRPostset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArc(nstg.M1, rstg.fMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
        for (VisualControlRegister n: getDfsModel().getRPostset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArc(nstg.M1, rstg.fMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
        for (VisualPushRegister n: getDfsModel().getRPostset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            Connection connection = getDfsModel().getConnection(r, n);
            if (connection instanceof VisualControlConnection && ((VisualControlConnection)connection).getReferencedControlConnection().isInverting()) {
                createReadArc(nstg.tM1, rstg.fMF, false);
                createReadArc(nstg.fM1, rstg.tMF, false);
            } else {
                createReadArc(nstg.tM1, rstg.tMF, false);
                createReadArc(nstg.fM1, rstg.fMF, false);
            }
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
        for (VisualPopRegister n: getDfsModel().getRPostset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            Connection connection = getDfsModel().getConnection(r, n);
            if (connection instanceof VisualControlConnection && ((VisualControlConnection)connection).getReferencedControlConnection().isInverting()) {
                createReadArc(nstg.tM1, rstg.fMF, false);
                createReadArc(nstg.fM1, rstg.tMF, false);
            } else {
                createReadArc(nstg.tM1, rstg.tMF, false);
                createReadArc(nstg.fM1, rstg.fMF, false);
            }
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
    }

    public BinaryRegisterStg getControlRegisterStg(VisualControlRegister register) {
        return ((controlRegisterMap == null) ? null : controlRegisterMap.get(register));
    }

    public void putControlRegisterStg(VisualControlRegister register, BinaryRegisterStg stg) {
        if (controlRegisterMap == null) {
            controlRegisterMap = new HashMap<>();
        }
        controlRegisterMap.put(register, stg);
    }


    private BinaryRegisterStg generatePushRegisterStg(VisualPushRegister r) throws InvalidConnectionException {
        return generateBinaryRegisterSTG(r, false, false);
    }

    private void connectPushRegisterStg(VisualPushRegister r) throws InvalidConnectionException {
        BinaryRegisterStg rstg = getPushRegisterStg(r);
        // preset
        for (VisualLogic n: getDfsModel().getPreset(r, VisualLogic.class)) {
            LogicStg nstg = getLogicStg(n);
            createReadArcs(nstg.C1, rstg.tMRs.values(), true);
            createReadArcs(nstg.C1, rstg.fMRs.values(), true);
            createReadArc(nstg.C0, rstg.tMF, false);
            createReadArc(nstg.C0, rstg.fMF, false);
        }
        // R-preset
        for (VisualRegister n: getDfsModel().getRPreset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArcs(nstg.M1, rstg.tMRs.values(), true);
            createReadArcs(nstg.M1, rstg.fMRs.values(), true);
            createReadArc(nstg.M0, rstg.tMF, false);
            createReadArc(nstg.M0, rstg.fMF, false);
        }
        for (VisualControlRegister n: getDfsModel().getRPreset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            Connection connection = getDfsModel().getConnection(n, r);
            if (connection instanceof VisualControlConnection && ((VisualControlConnection)connection).getReferencedControlConnection().isInverting()) {
                createReadArc(nstg.tM1, rstg.fMRs.get(n), true);
                createReadArc(nstg.fM1, rstg.tMRs.get(n), true);
                createReadArc(nstg.tM0, rstg.fMF, false);
                createReadArc(nstg.fM0, rstg.tMF, false);
            } else {
                createReadArc(nstg.tM1, rstg.tMRs.get(n), true);
                createReadArc(nstg.fM1, rstg.fMRs.get(n), true);
                createReadArc(nstg.tM0, rstg.tMF, false);
                createReadArc(nstg.fM0, rstg.fMF, false);
            }
        }
        for (VisualPushRegister n: getDfsModel().getRPreset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArcs(nstg.tM1, rstg.tMRs.values(), true);
            createReadArcs(nstg.tM1, rstg.fMRs.values(), true);
            createReadArc(nstg.tM0, rstg.tMF, false);
            createReadArc(nstg.tM0, rstg.fMF, false);
        }
        for (VisualPopRegister n: getDfsModel().getRPreset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArcs(nstg.M1, rstg.tMRs.values(), true);
            createReadArcs(nstg.M1, rstg.fMRs.values(), true);
            createReadArc(nstg.M0, rstg.tMF, false);
            createReadArc(nstg.M0, rstg.fMF, false);
        }
        // R-postset
        for (VisualRegister n: getDfsModel().getRPostset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false); // register M1 in R-postset is read only by tMF
            createReadArcs(nstg.M0, rstg.tMRs.values(), false); // register M0 in R-postset is read only by tMR
        }
        for (VisualControlRegister n: getDfsModel().getRPostset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
        }
        for (VisualPushRegister n: getDfsModel().getRPostset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
        }
        for (VisualPopRegister n: getDfsModel().getRPostset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArc(nstg.tM1, rstg.tMF, false); // pop tM1 in R-postset is read only by tMF
            createReadArcs(nstg.tM0, rstg.tMRs.values(), false); // pop tM0 in R-postset is read only by tMR
        }
    }

    public BinaryRegisterStg getPushRegisterStg(VisualPushRegister register) {
        return ((pushRegisterMap == null) ? null : pushRegisterMap.get(register));
    }

    public void putPushRegisterStg(VisualPushRegister register, BinaryRegisterStg stg) {
        if (pushRegisterMap == null) {
            pushRegisterMap = new HashMap<>();
        }
        pushRegisterMap.put(register, stg);
    }


    private BinaryRegisterStg generatePopRegisterStg(VisualPopRegister r) throws InvalidConnectionException {
        return generateBinaryRegisterSTG(r, false, false);
    }

    private void connectPopRegisterStg(VisualPopRegister r) throws InvalidConnectionException {
        BinaryRegisterStg rstg = getPopRegisterStg(r);
        // preset
        for (VisualLogic n: getDfsModel().getPreset(r, VisualLogic.class)) {
            LogicStg nstg = getLogicStg(n);
            createReadArcs(nstg.C1, rstg.tMRs.values(), true);
            createReadArc(nstg.C0, rstg.tMF, false);
        }
        // R-preset
        for (VisualRegister n: getDfsModel().getRPreset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArcs(nstg.M1, rstg.tMRs.values(), true);
            createReadArc(nstg.M0, rstg.tMF, false);
        }
        for (VisualControlRegister n: getDfsModel().getRPreset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            Connection connection = getDfsModel().getConnection(n, r);
            if (connection instanceof VisualControlConnection && ((VisualControlConnection)connection).getReferencedControlConnection().isInverting()) {
                createReadArc(nstg.tM1, rstg.fMRs.get(n), true);
                createReadArc(nstg.fM1, rstg.tMRs.get(n), true);
                createReadArc(nstg.tM0, rstg.fMF, false);
                createReadArc(nstg.fM0, rstg.tMF, false);
            } else {
                createReadArc(nstg.tM1, rstg.tMRs.get(n), true);
                createReadArc(nstg.fM1, rstg.fMRs.get(n), true);
                createReadArc(nstg.tM0, rstg.tMF, false);
                createReadArc(nstg.fM0, rstg.fMF, false);
            }
        }
        for (VisualPushRegister n: getDfsModel().getRPreset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArcs(nstg.tM1, rstg.tMRs.values(), true);
            createReadArc(nstg.tM0, rstg.tMF, false);
        }
        for (VisualPopRegister n: getDfsModel().getRPreset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArcs(nstg.M1, rstg.tMRs.values(), true);
            createReadArc(nstg.M0, rstg.tMF, false);
        }
        // R-postset
        for (VisualRegister n: getDfsModel().getRPostset(r, VisualRegister.class)) {
            RegisterStg nstg = getRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArc(nstg.M1, rstg.fMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
        for (VisualControlRegister n: getDfsModel().getRPostset(r, VisualControlRegister.class)) {
            BinaryRegisterStg nstg = getControlRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArc(nstg.M1, rstg.fMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
        for (VisualPushRegister n: getDfsModel().getRPostset(r, VisualPushRegister.class)) {
            BinaryRegisterStg nstg = getPushRegisterStg(n);
            createReadArc(nstg.M1, rstg.tMF, false);
            createReadArc(nstg.M1, rstg.fMF, false);
            createReadArcs(nstg.M0, rstg.tMRs.values(), false);
            createReadArcs(nstg.M0, rstg.fMRs.values(), false);
        }
        for (VisualPopRegister n: getDfsModel().getRPostset(r, VisualPopRegister.class)) {
            BinaryRegisterStg nstg = getPopRegisterStg(n);
            createReadArc(nstg.tM1, rstg.tMF, false);
            createReadArc(nstg.tM1, rstg.fMF, false);
            createReadArcs(nstg.tM0, rstg.tMRs.values(), false);
            createReadArcs(nstg.tM0, rstg.fMRs.values(), false);
        }
    }

    public BinaryRegisterStg getPopRegisterStg(VisualPopRegister register) {
        return ((popRegisterMap == null) ? null : popRegisterMap.get(register));
    }

    public void putPopRegisterStg(VisualPopRegister register, BinaryRegisterStg stg) {
        if (popRegisterMap == null) {
            popRegisterMap = new HashMap<>();
        }
        popRegisterMap.put(register, stg);
    }


    public boolean isRelated(Node highLevelNode, Node node) {
        NodeStg nodeStg = null;
        if (highLevelNode instanceof VisualLogic) {
            nodeStg = getLogicStg((VisualLogic)highLevelNode);
        } else if (highLevelNode instanceof VisualRegister) {
            nodeStg = getRegisterStg((VisualRegister)highLevelNode);
        } else if (highLevelNode instanceof VisualCounterflowLogic) {
            nodeStg = getCounterflowLogicStg((VisualCounterflowLogic)highLevelNode);
        } else if (highLevelNode instanceof VisualCounterflowRegister) {
            nodeStg = getCounterflowRegisterStg((VisualCounterflowRegister)highLevelNode);
        } else if (highLevelNode instanceof VisualControlRegister) {
            nodeStg = getControlRegisterStg((VisualControlRegister)highLevelNode);
        } else if (highLevelNode instanceof VisualPushRegister) {
            nodeStg = getPushRegisterStg((VisualPushRegister)highLevelNode);
        } else if (highLevelNode instanceof VisualPopRegister) {
            nodeStg = getPopRegisterStg((VisualPopRegister)highLevelNode);
        }
        return ((nodeStg != null) && nodeStg.contains(node));
    }

}
