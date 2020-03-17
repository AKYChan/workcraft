package org.workcraft.plugins.stg.serialisation;

import org.workcraft.Info;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.math.PageNode;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.exceptions.FormatException;
import org.workcraft.plugins.petri.PetriModel;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.stg.*;
import org.workcraft.plugins.stg.utils.StgUtils;
import org.workcraft.utils.ExportUtils;
import org.workcraft.utils.Hierarchy;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

public class SerialiserUtils {

    private static final String KEYWORD_MODEL = ".model";
    private static final String KEYWORD_NAME = ".name";
    private static final String KEYWORD_INPUTS = ".inputs";
    private static final String KEYWORD_OUTPUTS = ".outputs";
    private static final String KEYWORD_INTERNAL = ".internal";
    private static final String KEYWORD_DUMMY = ".dummy";
    private static final String KEYWORD_GRAPH = ".graph";
    private static final String KEYWORD_MARKING = ".marking";
    private static final String KEYWORD_CAPACITY = ".capacity";
    private static final String KEYWORD_END = ".end";

    public enum Style { STG, LPN };

    public static void writeModel(Model model, OutputStream out, Style style, boolean needsInitialState) {
        if (!(model instanceof PetriModel)) {
            throw new ArgumentException("Model class not supported: " + model.getClass().getName());
        }
        PetriModel petri = (PetriModel) model;

        PrintWriter writer = new PrintWriter(out);
        writeIntro(writer, petri, style);
        if (petri instanceof StgModel) {
            StgModel stg = (StgModel) petri;
            writeSignalDeclarations(writer, stg);
            if (needsInitialState) {
                writeInitialState(writer, stg, style);
            }
            boolean needsInstanceNumbers = (style == Style.LPN) && hasInstanceNumbers(petri);
            writeStg(writer, stg, needsInstanceNumbers);
        } else {
            writePetri(writer, petri);
        }
        writer.write(KEYWORD_END + "\n");
        writer.close();
    }

    private static void writeIntro(PrintWriter writer, PetriModel petri, Style style) {
        String prefix = (style == Style.LPN) ? "# LPN file " : "# STG file ";
        writer.write(Info.getGeneratedByText(prefix, "\n"));

        String keyword = (style == Style.LPN) ? KEYWORD_NAME : KEYWORD_MODEL;
        String title = ExportUtils.asIdentifier(petri.getTitle());
        writer.write(keyword + " " + title + "\n");
    }

    private static void writeInitialState(PrintWriter writer, StgModel stg, Style style) {
        HashMap<String, Boolean> initialState = StgUtils.getInitialState(stg, 1000);
        if (!initialState.isEmpty()) {
            switch (style) {
            case STG:
                writeInitialStateStg(writer, stg, initialState);
                break;
            case LPN:
                writeInitialStateLpn(writer, stg, initialState);
                break;
            }
        }
    }

    private static void writeInitialStateLpn(PrintWriter writer, StgModel stg, HashMap<String, Boolean> initialState) {
        writer.write("#@.init_state [");
        for (final Signal.Type type : Signal.Type.values()) {
            for (String signal : getSortedSignals(stg, type)) {
                Boolean signalState = initialState.get(signal);
                if ((signalState == null) || !signalState) {
                    writer.write("0");
                } else {
                    writer.write("1");
                }
            }
        }
        writer.write("]\n");
    }

    private static void writeInitialStateStg(PrintWriter writer, StgModel stg, HashMap<String, Boolean> initialState) {
        writer.write(".initial state");
        for (final Signal.Type type : Signal.Type.values()) {
            for (String signal : getSortedSignals(stg, type)) {
                Boolean signalState = initialState.get(signal);
                writer.write(" ");
                if ((signalState == null) || !signalState) {
                    writer.write("!");
                }
                writer.write(signal);
            }
        }
        writer.write("\n");
    }

    private static boolean hasInstanceNumbers(PetriModel petriModel) {
        if (petriModel instanceof StgModel) {
            StgModel stg = (StgModel) petriModel;
            for (SignalTransition st: stg.getSignalTransitions()) {
                if (stg.getInstanceNumber(st) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getReference(Model model, Node node, boolean needInstanceNumbers) {
        String result = model.getNodeReference(node);
        if (needInstanceNumbers && (model instanceof StgModel) && (node instanceof NamedTransition)) {
            NamedTransition nt = (NamedTransition) node;
            StgModel stg = (StgModel) model;
            if (stg.getInstanceNumber(nt) == 0) {
                result += "/0";
            }
        }
        return result;
    }

    private static void writeSignalDeclaration(PrintWriter out, List<String> signals, String declarationKeyword) {
        if (!signals.isEmpty()) {
            out.write(declarationKeyword);
            for (String signal: signals) {
                out.write(" ");
                out.write(signal);
            }
            out.write("\n");
        }
    }

    private static Iterable<Node> sortNodes(Collection<? extends Node> nodes, final Model model) {
        ArrayList<Node> list = new ArrayList<>(nodes);
        Collections.sort(list, Comparator.comparing(model::getNodeReference));
        return list;
    }

    private static void writeGraphEntry(PrintWriter out, Model model, Node node, boolean needInstanceNumbers) {
        if ((node instanceof StgPlace) && ((StgPlace) node).isImplicit()) {
            return;
        }
        String nodeRef = getReference(model, node, needInstanceNumbers);
        out.write(nodeRef);
        Set<Node> postset = model.getPostset(node);
        for (Node succNode : sortNodes(postset, model)) {
            String succNodeRef = getReference(model, succNode, needInstanceNumbers);
            if (succNode instanceof StgPlace) {
                StgPlace succPlace = (StgPlace) succNode;
                if (succPlace.isImplicit()) {
                    Collection<Node> succPostset = model.getPostset(succNode);
                    if (succPostset.size() > 1) {
                        throw new FormatException("Implicit place cannot have more than one node in postset");
                    }
                    Node succTransition = succPostset.iterator().next();
                    String succTransitionRef = getReference(model, succTransition, needInstanceNumbers);
                    out.write(" " + succTransitionRef);
                } else {
                    out.write(" " + succNodeRef);
                }
            } else {
                out.write(" " + succNodeRef);
            }
        }
        out.write("\n");
    }


    private static void writeSignalDeclarations(PrintWriter out, StgModel stg) {
        writeSignalDeclaration(out, getSortedSignals(stg, Signal.Type.INPUT), KEYWORD_INPUTS);
        writeSignalDeclaration(out, getSortedSignals(stg, Signal.Type.OUTPUT), KEYWORD_OUTPUTS);
        writeSignalDeclaration(out, getSortedSignals(stg, Signal.Type.INTERNAL), KEYWORD_INTERNAL);
        Set<String> pageRefs = getPageReferences(stg);
        if (!pageRefs.isEmpty()) {
            out.write("# Pages added as dummies: " + String.join(", ", pageRefs) + "\n");
        }
        List<String> dummyRefs = new ArrayList(stg.getDummyReferences());
        dummyRefs.addAll(pageRefs);
        writeSignalDeclaration(out, dummyRefs, KEYWORD_DUMMY);
    }

    private static void writeStg(PrintWriter out, StgModel stg, boolean needInstanceNumbers) {
        out.write(KEYWORD_GRAPH + "\n");
        for (Node n : sortNodes(stg.getSignalTransitions(), stg)) {
            writeGraphEntry(out, stg, n, needInstanceNumbers);
        }
        for (Node n : sortNodes(stg.getDummyTransitions(), stg)) {
            writeGraphEntry(out, stg, n, needInstanceNumbers);
        }
        for (Node n : sortNodes(stg.getPlaces(), stg)) {
            writeGraphEntry(out, stg, n, needInstanceNumbers);
        }
        writeMarking(out, stg, stg.getPlaces(), needInstanceNumbers);
    }

    private static List<String> getSortedSignals(StgModel stg, Signal.Type type) {
        List<String> result = new ArrayList<>(stg.getSignalReferences(type));
        Collections.sort(result);
        return result;
    }

    private static Set<String> getPageReferences(StgModel stg) {
        Set<String> result = new HashSet<>();
        for (PageNode page: Hierarchy.getDescendantsOfType(stg.getRoot(), PageNode.class)) {
            result.add(stg.getNodeReference(page));
        }
        return result;
    }

    private static void writeMarking(PrintWriter out, MathModel model, Collection<? extends Place> places, boolean needInstanceNumbers) {
        ArrayList<String> markingEntries = new ArrayList<>();
        for (Place p: places) {
            final int tokens = p.getTokens();
            final String reference;
            if (p instanceof StgPlace) {
                if (((StgPlace) p).isImplicit()) {
                    MathNode predNode = model.getPreset(p).iterator().next();
                    String predRef = getReference(model, predNode, needInstanceNumbers);
                    MathNode succNode = model.getPostset(p).iterator().next();
                    String succRef = getReference(model, succNode, needInstanceNumbers);
                    reference = "<" + predRef + "," + succRef + ">";
                } else {
                    reference = getReference(model, p, needInstanceNumbers);
                }
            } else {
                reference = getReference(model, p, needInstanceNumbers);
            }
            if (tokens == 1) {
                markingEntries.add(reference);
            } else if (tokens > 1) {
                markingEntries.add(reference + "=" + tokens);
            }
        }
        Collections.sort(markingEntries);
        out.write(KEYWORD_MARKING + " {");
        boolean first = true;
        for (String m : markingEntries) {
            if (!first) {
                out.write(" ");
            } else {
                first = false;
            }
            out.write(m);
        }
        out.write("}\n");
        StringBuilder capacity = new StringBuilder();
        for (Place p : places) {
            if (p.getCapacity() != 1) {
                String placeRef = getReference(model, p, needInstanceNumbers);
                capacity.append(" " + placeRef + "=" + p.getCapacity());
            }
        }
        if (capacity.length() > 0) {
            out.write(KEYWORD_CAPACITY + capacity + "\n");
        }
    }

    private static void writePetri(PrintWriter out, PetriModel petriModel) {
        LinkedList<String> transitions = new LinkedList<>();
        for (Transition t : petriModel.getTransitions()) {
            String transitionRef = petriModel.getNodeReference(t);
            transitions.add(transitionRef);
        }
        writeSignalDeclaration(out, transitions, KEYWORD_DUMMY);
        out.write(KEYWORD_GRAPH + "\n");
        for (Transition t : petriModel.getTransitions()) {
            writeGraphEntry(out, petriModel, t, false);
        }
        for (Place p : petriModel.getPlaces()) {
            writeGraphEntry(out, petriModel, p, false);
        }
        writeMarking(out, petriModel, petriModel.getPlaces(), false);
    }

}
