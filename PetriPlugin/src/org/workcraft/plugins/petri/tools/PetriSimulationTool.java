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

package org.workcraft.plugins.petri.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.ExceptionDialog;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.SimulationTool;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.petri.VisualPlace;
import org.workcraft.plugins.petri.VisualReplicaPlace;

public class PetriSimulationTool extends SimulationTool {

    public PetriSimulationTool() {
        this(false);
    }

    public PetriSimulationTool(boolean enableTraceGraph) {
        super(enableTraceGraph);
    }

    public PetriNetModel getUnderlyingPetri() {
        return (PetriNetModel) getUnderlyingModel().getMathModel();
    }

    @Override
    public boolean isConnectionExcited(VisualConnection connection) {
        VisualNode first = connection.getFirst();
        Place place = null;
        if (first instanceof VisualPlace) {
            place = ((VisualPlace) first).getReferencedPlace();
        } else if (first instanceof VisualReplicaPlace) {
            place = ((VisualReplicaPlace) first).getReferencedPlace();
        }
        return (place != null) && (place.getTokens() > 0);
    }

    @Override
    public HashMap<Node, Integer> readModelState() {
        HashMap<Node, Integer>  result = new HashMap<>();
        for (Place place: getUnderlyingPetri().getPlaces()) {
            result.put(place, place.getTokens());
        }
        return result;
    }

    @Override
    public void writeModelState(Map<Node, Integer> state) {
        HashSet<Place> places = new HashSet<>(getUnderlyingPetri().getPlaces());
        for (Node node: state.keySet()) {
            if (node instanceof Place) {
                Place place = (Place) node;
                if (places.contains(place)) {
                    place.setTokens(state.get(place));
                } else {
                    ExceptionDialog.show(null, new RuntimeException("Place " + place.toString() + " is not in the model"));
                }
            }
        }
    }

    @Override
    public void applySavedState(final GraphEditor editor) {
        if ((savedState == null) || savedState.isEmpty()) {
            return;
        }
        MathModel model = editor.getModel().getMathModel();
        if (model instanceof PetriNetModel) {
            PetriNetModel petri = (PetriNetModel) model;
            editor.getWorkspaceEntry().saveMemento();
            for (Place place: petri.getPlaces()) {
                String ref = petri.getNodeReference(place);
                Node underlyingNode = getUnderlyingPetri().getNodeByReference(ref);
                if ((underlyingNode instanceof Place) && savedState.containsKey(underlyingNode)) {
                    Integer tokens = savedState.get(underlyingNode);
                    place.setTokens(tokens);
                }
            }
        }
    }

    @Override
    public boolean isEnabledNode(Node node) {
        boolean result = false;
        if (node instanceof Transition) {
            Transition transition = (Transition) node;
            result = getUnderlyingPetri().isEnabled(transition);
        }
        return result;
    }

    @Override
    public ArrayList<Node> getEnabledNodes() {
        ArrayList<Node> result = new ArrayList<>();
        for (Transition transition: getUnderlyingPetri().getTransitions()) {
            if (isEnabledNode(transition)) {
                result.add(transition);
            }
        }
        return result;
    }

    @Override
    public boolean fire(String ref) {
        boolean result = false;
        Transition transition = null;
        if (ref != null) {
            final Node node = getUnderlyingPetri().getNodeByReference(ref);
            if (node instanceof Transition) {
                transition = (Transition) node;
            }
        }
        if (isEnabledNode(transition)) {
            getUnderlyingPetri().fire(transition);
            result = true;
        }
        return result;
    }

    @Override
    public boolean unfire(String ref) {
        boolean result = false;
        Transition transition = null;
        if (ref != null) {
            final Node node = getUnderlyingPetri().getNodeByReference(ref);
            if (node instanceof Transition) {
                transition = (Transition) node;
            }
        }
        if (transition != null) {
            if (getUnderlyingPetri().isUnfireEnabled(transition)) {
                getUnderlyingPetri().unFire(transition);
                result = true;
            }
        }
        return result;
    }

}
