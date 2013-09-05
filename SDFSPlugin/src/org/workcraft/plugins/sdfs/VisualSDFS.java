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

package org.workcraft.plugins.sdfs;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.workcraft.annotations.CustomTools;
import org.workcraft.annotations.DisplayName;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.AbstractVisualModel;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.exceptions.NodeCreationException;
import org.workcraft.exceptions.VisualModelInstantiationException;
import org.workcraft.util.Hierarchy;

@DisplayName("Static Data Flow Structures")
@CustomTools( SDFSToolsProvider.class )
public class VisualSDFS extends AbstractVisualModel {

	public VisualSDFS(SDFS model) throws VisualModelInstantiationException {
		this(model, null);
	}

	public VisualSDFS(SDFS model, VisualGroup root) {
		super(model, root);
		if (root == null) {
			try {
				createDefaultFlatStructure();
			} catch (NodeCreationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void validateConnection(Node first, Node second)	throws InvalidConnectionException {
		if (first == null || second == null) {
			throw new InvalidConnectionException ("Invalid connection");
		}
		// Types of connections
		if ((first instanceof VisualLogic)
		&& !(second instanceof VisualLogic || second instanceof VisualRegister || second instanceof VisualPopRegister)) {
			throw new InvalidConnectionException ("Invalid connection from spreadtoken logic");
		}
		if ((first instanceof VisualRegister)
	 	&& !(second instanceof VisualLogic || second instanceof VisualRegister || second instanceof VisualCounterflowRegister || second instanceof VisualPushRegister  || second instanceof VisualPopRegister)) {
			throw new InvalidConnectionException ("Invalid connection from spreadtoken register");
		}
		if ((first instanceof VisualCounterflowLogic)
		&& !(second instanceof VisualCounterflowLogic || second instanceof VisualCounterflowRegister)) {
			throw new InvalidConnectionException ("Invalid connection from counterflow logic");
		}
		if ((first instanceof VisualCounterflowRegister)
		&& !(second instanceof VisualCounterflowLogic || second instanceof VisualCounterflowRegister || second instanceof VisualRegister)) {
			throw new InvalidConnectionException ("Invalid connection from counterflow register");
		}
		if ((first instanceof VisualControlRegister)
		&& !(second instanceof VisualControlRegister || second instanceof VisualPushRegister || second instanceof VisualPopRegister)) {
			throw new InvalidConnectionException ("Invalid connection from control register");
		}
		if ((first instanceof VisualPushRegister)
		&& !(second instanceof VisualLogic || second instanceof VisualRegister || second instanceof VisualPopRegister)) {
			throw new InvalidConnectionException ("Invalid connection from push register");
		}
		if ((first instanceof VisualPopRegister)
		&& !(second instanceof VisualRegister)) {
			throw new InvalidConnectionException ("Invalid connection from pop register");
		}
		// Number of connections
		if ((first instanceof VisualRegister) 	&& (second instanceof VisualPushRegister)
		&& !(getRPostset((VisualComponent)first, VisualPushRegister.class).size() == 0)) {
			throw new InvalidConnectionException ("Single push register can be connected to a spreadtoken register only");
		}
		if ((first instanceof VisualPopRegister) 	&& (second instanceof VisualRegister)
		&& !(getRPostset((VisualComponent)first, VisualRegister.class).size() == 0)) {
			throw new InvalidConnectionException ("Single spreadtoken register can be connected to a pop register only");
		}
	}

	@Override
	public void connect(Node first, Node second) throws InvalidConnectionException {
		validateConnection(first, second);
		VisualComponent c1 = (VisualComponent) first;
		VisualComponent c2 = (VisualComponent) second;
		MathNode ref1 = c1.getReferencedComponent();
		MathNode ref2 = c2.getReferencedComponent();
		MathConnection con = ((SDFS)getMathModel()).connect(ref1, ref2);
		VisualConnection ret = new VisualConnection(con, c1, c2);
		Hierarchy.getNearestContainer(c1, c2).add(ret);
	}

	public String getName(VisualComponent component) {
		return ((SDFS)getMathModel()).getName(component.getReferencedComponent());
	}

	public <R> Set<R> getPreset(Node node, Class<R> type) {
		Set<R> result = new HashSet<R>();
		for (Node pred: getPreset(node)) {
			try {
				result.add(type.cast(pred));
			} catch (ClassCastException e) {
			}
		}
		return result;
	}

	public <R> Set<R> getPostset(Node node, Class<R> type) {
		Set<R> result = new HashSet<R>();
		for (Node pred: getPostset(node)) {
			try {
				result.add(type.cast(pred));
			} catch (ClassCastException e) {
			}
		}
		return result;
	}

	public <R> Set<R> getRPreset(Node node, Class<R> rType) {
		Set<R> result = new HashSet<R>();
		Set<Node> visited = new HashSet<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(node);
		while (!queue.isEmpty()) {
			Node cur = queue.remove();
			if (visited.contains(cur)) continue;
			visited.add(cur);
			for (Node pred: getPreset(cur)) {
				if ( !(pred instanceof VisualComponent) ) continue;
				try {
					result.add(rType.cast(pred));
				} catch (ClassCastException e) {
					if ((pred instanceof VisualLogic) || (pred instanceof VisualCounterflowLogic)) {
						queue.add(pred);
					}
				}
			}
		}
		return result;
	}

	public <R> Set<R> getRPostset(Node node, Class<R> rType) {
		Set<R> result = new HashSet<R>();
		Set<Node> visited = new HashSet<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(node);
		while (!queue.isEmpty()) {
			Node cur = queue.remove();
			if (visited.contains(cur)) continue;
			visited.add(cur);
			for (Node succ: getPostset(cur)) {
				if ( !(succ instanceof VisualComponent) ) continue;
				try {
					result.add(rType.cast(succ));
				} catch (ClassCastException e) {
					if ((succ instanceof VisualLogic) || (succ instanceof VisualCounterflowLogic)) {
						queue.add(succ);
					}
				}
			}
		}
		return result;
	}

}