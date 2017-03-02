package org.workcraft.plugins.stg.serialisation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import org.workcraft.Info;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.PageNode;
import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.exceptions.FormatException;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.plugins.stg.StgPlace;
import org.workcraft.serialisation.Format;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.ReferenceProducer;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.LogUtils;

public class DotGSerialiser implements ModelSerialiser {

    class ReferenceResolver implements ReferenceProducer {
        HashMap<Object, String> refMap = new HashMap<>();

        @Override
        public String getReference(Object obj) {
            return refMap.get(obj);
        }
    }

    private void writeSignalsHeader(PrintWriter out, Collection<String> signalNames, String header) {
        if (!signalNames.isEmpty()) {
            LinkedList<String> sortedNames = new LinkedList<>(signalNames);
            Collections.sort(sortedNames);
            out.write(header);
            for (String name : sortedNames) {
                out.write(" ");
                out.write(name);
            }
            out.write("\n");
        }
    }

    private Iterable<Node> sortNodes(Collection<? extends Node> nodes, final Model model) {
        ArrayList<Node> list = new ArrayList<>(nodes);
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return model.getNodeReference(o1).compareTo(model.getNodeReference(o2));
            }
        });
        return list;
    }

    private void writeGraphEntry(PrintWriter out, Model model, Node node) {
        if (node instanceof StgPlace) {
            StgPlace stgPlace = (StgPlace) node;
            if (stgPlace.isImplicit()) {
                return;
            }
        }
        Set<Node> postset = model.getPostset(node);
        if (!postset.isEmpty()) {
            String nodeRef = model.getNodeReference(node);
            out.write(nodeRef);
            for (Node succNode : sortNodes(postset, model)) {
                String succNodeRef = model.getNodeReference(succNode);
                if (succNode instanceof StgPlace) {
                    StgPlace succPlace = (StgPlace) succNode;
                    if (succPlace.isImplicit()) {
                        Collection<Node> succPostset = model.getPostset(succNode);
                        if (succPostset.size() > 1) {
                            throw new FormatException("Implicit place cannot have more than one node in postset");
                        }
                        Node succTransition = succPostset.iterator().next();
                        String succTransitionRef = model.getNodeReference(succTransition);
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
    }

    @Override
    public ReferenceProducer serialise(Model model, OutputStream out, ReferenceProducer refs) {
        PrintWriter writer = new PrintWriter(out);
        writer.write(Info.getGeneratedByText("# STG file ", "\n"));
        writer.write(".model " + getClearTitle(model) + "\n");
        ReferenceResolver resolver = new ReferenceResolver();
        if (model instanceof StgModel) {
            writeSTG((StgModel) model, writer);
        } else if (model instanceof PetriNetModel) {
            writePN((PetriNetModel) model, writer);
        } else {
            throw new ArgumentException("Model class not supported: " + model.getClass().getName());
        }
        writer.write(".end\n");
        writer.close();
        return resolver;
    }

    public String getClearTitle(Model model) {
        String title = model.getTitle();
        // Non-empty model name must be present in .model line of .g file.
        // Otherwise Petrify will use the full file name (possibly with bad characters) as a Verilog module name.
        if ((title == null) || title.isEmpty()) {
            title = "Untitled";
        }
        // If the title start with a number then prepend it with an underscore.
        if (Character.isDigit(title.charAt(0))) {
            title = "_" + title;
        }
        // Petrify does not allow spaces and special symbols in the model name, so replace them with underscores.
        String result = title.replaceAll("[^A-Za-z0-9_]", "_");
        if (!result.equals(model.getTitle())) {
            LogUtils.logWarningLine("Model title was exported as '" + result + "'.");
        }
        return result;
    }

    private void writeSTG(StgModel stg, PrintWriter out) {
        writeSignalsHeader(out, stg.getSignalReferences(Type.INTERNAL), ".internal");
        writeSignalsHeader(out, stg.getSignalReferences(Type.INPUT), ".inputs");
        writeSignalsHeader(out, stg.getSignalReferences(Type.OUTPUT), ".outputs");
        Set<String> pageRefs = getPageReferences(stg);
        if (!pageRefs.isEmpty()) {
            out.write("# Pages added as dummies: " + ReferenceHelper.getReferencesAsString((Collection) pageRefs) + "\n");
        }
        Set<String> dummyRefs = stg.getDummyReferences();
        dummyRefs.addAll(pageRefs);
        writeSignalsHeader(out, dummyRefs, ".dummy");

        out.write(".graph\n");
        for (Node n : sortNodes(stg.getSignalTransitions(), stg)) {
            writeGraphEntry(out, stg, n);
        }
        for (Node n : sortNodes(stg.getDummyTransitions(), stg)) {
            writeGraphEntry(out, stg, n);
        }
        for (Node n : sortNodes(stg.getPlaces(), stg)) {
            writeGraphEntry(out, stg, n);
        }
        writeMarking(stg, stg.getPlaces(), out);
    }

    private Set<String> getPageReferences(StgModel stg) {
        Set<String> result = new HashSet<>();
        for (PageNode page: Hierarchy.getDescendantsOfType(stg.getRoot(), PageNode.class)) {
            result.add(stg.getNodeReference(page));
        }
        return result;
    }

    private void writeMarking(Model model, Collection<Place> places, PrintWriter out) {
        ArrayList<String> markingEntries = new ArrayList<>();
        for (Place p: places) {
            final int tokens = p.getTokens();
            final String reference;
            if (p instanceof StgPlace) {
                if (((StgPlace) p).isImplicit()) {
                    Node predNode = model.getPreset(p).iterator().next();
                    String predRef = model.getNodeReference(predNode);
                    Node succNode = model.getPostset(p).iterator().next();
                    String succRef = model.getNodeReference(succNode);
                    reference = "<" + predRef + "," + succRef + ">";
                } else {
                    reference = model.getNodeReference(p);
                }
            } else {
                reference = model.getNodeReference(p);
            }
            if (tokens == 1) {
                markingEntries.add(reference);
            } else if (tokens > 1) {
                markingEntries.add(reference + "=" + tokens);
            }
        }
        Collections.sort(markingEntries);
        out.write(".marking {");
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
            if (p instanceof StgPlace) {
                StgPlace stgPlace = (StgPlace) p;
                if (stgPlace.getCapacity() != 1) {
                    String placeRef = model.getNodeReference(p);
                    capacity.append(" " + placeRef + "=" + stgPlace.getCapacity());
                }
            }
        }
        if (capacity.length() > 0) {
            out.write(".capacity" + capacity + "\n");
        }
    }

    private void writePN(PetriNetModel net, PrintWriter out) {
        LinkedList<String> transitions = new LinkedList<>();
        for (Transition t : net.getTransitions()) {
            String transitionRef = net.getNodeReference(t);
            transitions.add(transitionRef);
        }
        writeSignalsHeader(out, transitions, ".dummy");
        out.write(".graph\n");
        for (Transition t : net.getTransitions()) {
            writeGraphEntry(out, net, t);
        }
        for (Place p : net.getPlaces()) {
            writeGraphEntry(out, net, p);
        }
        writeMarking(net, net.getPlaces(), out);
    }

    @Override
    public boolean isApplicableTo(Model model) {
        return (model instanceof StgModel) || (model instanceof PetriNetModel);
    }

    @Override
    public String getDescription() {
        return "Workcraft STG serialiser";
    }

    @Override
    public String getExtension() {
        return ".g";
    }

    @Override
    public UUID getFormatUUID() {
        return Format.STG;
    }

}
