package org.workcraft.plugins.stg.converters;

import java.util.Map;

import org.workcraft.dom.Container;
import org.workcraft.dom.hierarchy.NamespaceProvider;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.references.UniqueReferenceManager;
import org.workcraft.dom.references.NameManager;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.VisualReplica;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.gui.graph.converters.DefaultModelConverter;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.petri.VisualPetriNet;
import org.workcraft.plugins.petri.VisualReadArc;
import org.workcraft.plugins.petri.VisualReplicaPlace;
import org.workcraft.plugins.stg.DummyTransition;
import org.workcraft.plugins.stg.LabelParser;
import org.workcraft.plugins.stg.StgPlace;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.VisualImplicitPlaceArc;
import org.workcraft.plugins.stg.VisualStg;

public class StgToPetriConverter extends DefaultModelConverter<VisualStg, VisualPetriNet> {

    public StgToPetriConverter(VisualStg srcModel, VisualPetriNet dstModel) {
        super(srcModel, dstModel);
    }

    @Override
    public Map<Class<? extends MathNode>, Class<? extends MathNode>> getComponentClassMap() {
        Map<Class<? extends MathNode>, Class<? extends MathNode>> result = super.getComponentClassMap();
        result.put(StgPlace.class, Place.class);
        result.put(DummyTransition.class, Transition.class);
        result.put(SignalTransition.class, Transition.class);
        return result;
    }

    @Override
    public Map<Class<? extends VisualReplica>, Class<? extends VisualReplica>> getReplicaClassMap() {
        Map<Class<? extends VisualReplica>, Class<? extends VisualReplica>> result = super.getReplicaClassMap();
        result.put(VisualReplicaPlace.class, VisualReplicaPlace.class);
        return result;
    }

    @Override
    public String convertNodeName(String srcName, Container container) {
        String dstCandidate = LabelParser.getTransitionName(srcName);
        dstCandidate = dstCandidate.replace("+", "_PLUS").replace("-", "_MINUS").replace("~", "_TOGGLE");

        UniqueReferenceManager refManager
                = (UniqueReferenceManager) getDstModel().getPetriNet().getReferenceManager();

        NamespaceProvider namespaceProvider = refManager.getNamespaceProvider(container);
        NameManager nameManagerer = refManager.getNameManager(namespaceProvider);
        return nameManagerer.getDerivedName(null, dstCandidate);
    }

    @Override
    public void preprocessing() {
        VisualStg stg = getSrcModel();
        for (VisualImplicitPlaceArc connection: stg.getVisualImplicitPlaceArcs()) {
            stg.makeExplicit(connection);
        }
    }

    @Override
    public VisualConnection convertConnection(VisualConnection srcConnection) {
        VisualConnection dstConnection = null;
        if (srcConnection instanceof VisualReadArc) {
            VisualNode srcFirst = srcConnection.getFirst();
            VisualNode srcSecond = srcConnection.getSecond();
            VisualNode dstFirst = getSrcToDstNode(srcFirst);
            VisualNode dstSecond = getSrcToDstNode(srcSecond);
            if ((dstFirst != null) && (dstSecond != null)) {
                try {
                    dstConnection = getDstModel().connectUndirected(dstFirst, dstSecond);
                    dstConnection.copyStyle(srcConnection);
                    dstConnection.copyShape(srcConnection);
                } catch (InvalidConnectionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            dstConnection = super.convertConnection(srcConnection);
        }
        return dstConnection;
    }

}
