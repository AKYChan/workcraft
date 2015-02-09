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

package org.workcraft.plugins.circuit;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.workcraft.Framework;
import org.workcraft.annotations.CustomTools;
import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.ShortName;
import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.AbstractVisualModel;
import org.workcraft.dom.visual.ConnectionHelper;
import org.workcraft.dom.visual.TransformHelper;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.dom.visual.connections.ControlPoint;
import org.workcraft.dom.visual.connections.Polyline;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.exceptions.NodeCreationException;
import org.workcraft.exceptions.VisualModelInstantiationException;
import org.workcraft.gui.graph.GraphEditorPanel;
import org.workcraft.gui.propertyeditor.ModelProperties;
import org.workcraft.plugins.circuit.Contact.IOType;
import org.workcraft.plugins.circuit.VisualContact.Direction;
import org.workcraft.serialisation.xml.NoAutoSerialisation;
import org.workcraft.util.Func;
import org.workcraft.util.Hierarchy;
import org.workcraft.workspace.WorkspaceEntry;

@DisplayName("Digital Circuit")
@ShortName("circuit")
@CustomTools(CircuitToolsProvider.class)
public class VisualCircuit extends AbstractVisualModel {

	private Circuit circuit;

	@Override
	public void validateConnection(Node first, Node second) throws InvalidConnectionException {
		if (first==second) {
			throw new InvalidConnectionException ("Connections are only valid between different objects.");
		}

		if (second instanceof VisualConnection) {
			throw new InvalidConnectionException ("Merging connections is not allowed.");
		}

		if (second instanceof VisualComponent) {
			for (Connection c: this.getConnections(second)) {
				if (c.getSecond() == second)
					throw new InvalidConnectionException ("Only one connection is allowed as a driver.");
			}
		}

		if (first instanceof VisualContact) {
			Node fromParent = ((VisualComponent)first).getParent();
			Contact.IOType toType = ((Contact)((VisualComponent)first).getReferencedComponent()).getIOType();

			if ((fromParent instanceof VisualCircuitComponent) && (toType == Contact.IOType.INPUT))
				throw new InvalidConnectionException ("Inputs of components cannot be drivers.");

			if (!(fromParent instanceof VisualCircuitComponent) && (toType == Contact.IOType.OUTPUT))
				throw new InvalidConnectionException ("Outputs from the environment cannot be drivers.");
		}

		if (second instanceof VisualContact) {
			Node toParent = ((VisualComponent)second).getParent();
			Contact.IOType toType = ((Contact)((VisualComponent)second).getReferencedComponent()).getIOType();

			if ((toParent instanceof VisualCircuitComponent) && (toType == Contact.IOType.OUTPUT))
				throw new InvalidConnectionException ("Outputs of the components cannot be driven.");

			if (!(toParent instanceof VisualCircuitComponent) && (toType == Contact.IOType.INPUT))
				throw new InvalidConnectionException ("Inputs from the environment cannot be driven.");
		}
	}

	public VisualCircuit(Circuit model, VisualGroup root) {
		super(model, root);
		circuit = model;
	}

	public VisualCircuit(Circuit model) throws VisualModelInstantiationException {
		super(model);
		circuit = model;
		try {
			createDefaultFlatStructure();
		} catch (NodeCreationException e) {
			throw new VisualModelInstantiationException(e);
		}
	}

	@Override
	public VisualConnection connect(Node first, Node second) throws InvalidConnectionException {
		validateConnection(first, second);
		if (first instanceof VisualConnection) {
			VisualConnection connection = (VisualConnection)first;
			List<Point2D> locations = new LinkedList<Point2D>();
			int splitIndex = -1;
 			if (connection.getGraphic() instanceof Polyline) {
 				AffineTransform localToRootTransform = TransformHelper.getTransformToRoot(connection);
				Polyline polyline = (Polyline)connection.getGraphic();
				for (ControlPoint cp:  polyline.getControlPoints()) {
					Point2D location = localToRootTransform.transform(cp.getPosition(), null);
					locations.add(location);
				}
				splitIndex = polyline.getNearestSegment(connection.getSplitPoint(), null);
			}

			Container vContainer = (Container)connection.getParent();
			Container mParent = (Container)(connection.getReferencedConnection().getParent());
			Joint mJoint = new Joint();
			mParent.add(mJoint);
			VisualJoint vJoint = new VisualJoint(mJoint);
			vContainer.add(vJoint);
			vJoint.setPosition(connection.getSplitPoint());
			remove(connection);

			VisualConnection vc1 = connect(connection.getFirst(), vJoint);
			VisualConnection vc2 = connect(vJoint, connection.getSecond());
			if (!locations.isEmpty()) {
				ConnectionHelper.addControlPoints(vc1, locations.subList(0, splitIndex));
				ConnectionHelper.addControlPoints(vc2, locations.subList(splitIndex, locations.size()));
			}
			first = vJoint;
		}

		VisualCircuitConnection vConnection = null;
		if ((first instanceof VisualComponent) && (second instanceof VisualComponent)) {
			VisualComponent vComponent1 = (VisualComponent)first;
			MathNode mComponent1 = vComponent1.getReferencedComponent();

			VisualComponent vComponent2 = (VisualComponent)second;
			MathNode mComponent2 = vComponent2.getReferencedComponent();

			Node vParent = Hierarchy.getCommonParent(vComponent1, vComponent2);
			Container vContainer = (Container)Hierarchy.getNearestAncestor(vParent, new Func<Node, Boolean>() {
				@Override
				public Boolean eval(Node node) {
					return ((node instanceof VisualGroup) || (node instanceof VisualPage));
				}
			});
			Container mContainer = NamespaceHelper.getMathContainer(this, vContainer);

			MathConnection mConnection = (MathConnection)circuit.connect(mComponent1, mComponent2);
			vConnection = new VisualCircuitConnection(mConnection, vComponent1, vComponent2);
			vContainer.add(vConnection);

			Container mParent = (Container)(mConnection.getParent());
			LinkedList<Node> mConnections = new LinkedList<Node>();
			mConnections.add(mConnection);
			mParent.reparent(mConnections, mContainer);
		}
		return vConnection;
	}

	public Collection<VisualFunctionContact> getVisualFunctionContacts() {
		return Hierarchy.getChildrenOfType(getRoot(), VisualFunctionContact.class);
	}

	public VisualFunctionContact getOrCreateContact(Container container, String name, IOType ioType) {
		// here "parent" is a container of a visual model
		if (name != null) {
			for (Node n: container.getChildren()) {
				if (n instanceof VisualFunctionContact) {
					VisualFunctionContact contact = (VisualFunctionContact)n;
					String contactName = getMathModel().getName(contact.getReferencedContact());
					if (name.equals(contactName)) {
						return contact;
					}
				} // TODO: if found something else with that name, return null or exception?
			}
		}

		Direction direction = Direction.WEST;
		if (ioType == null) {
			ioType = IOType.OUTPUT;
		}
		if (ioType == IOType.OUTPUT) {
			direction = Direction.EAST;
		}

		VisualFunctionContact vc = new VisualFunctionContact(new FunctionContact(ioType));
		vc.setDirection(direction);

		if (container instanceof VisualFunctionComponent) {
			VisualFunctionComponent component = (VisualFunctionComponent)container;
			component.addContact(this, vc);
		} else {
			Container mathContainer = NamespaceHelper.getMathContainer(this, getRoot());
			mathContainer.add(vc.getReferencedComponent());
			add(vc);
		}
		if (name != null) {
			circuit.setName(vc.getReferencedComponent(), name);
		}
		return vc;
	}

	public Collection<Environment> getEnvironments() {
		return Hierarchy.getChildrenOfType(getRoot(), Environment.class);
	}

	private WorkspaceEntry getWorkspaceEntry() {
		Framework framework = Framework.getInstance();
		GraphEditorPanel editor = framework.getMainWindow().getCurrentEditor();
		WorkspaceEntry we = editor.getWorkspaceEntry();
		return we;
	}

	@NoAutoSerialisation
	public File getEnvironmentFile() {
		File result = null;
		for (Environment env: getEnvironments()) {
			result = env.getFile();
			File base = env.getBase();
			if (base != null) {
				URI relativeUri = base.toURI().relativize(result.toURI());
				if (!relativeUri.equals(result.toURI())) {
					base = getWorkspaceEntry().getFile().getParentFile();
					result = new File(base, relativeUri.getPath());
				}
			}
			break;
		}
		return result;
	}

	@NoAutoSerialisation
	public void setEnvironmentFile(File value) {
		boolean envChanged = false;
		getWorkspaceEntry().captureMemento();

		for (Environment env: getEnvironments()) {
			remove(env);
			envChanged = true;
		}

		if (value != null) {
			Environment env = new Environment();
			env.setFile(value);
			File base = getWorkspaceEntry().getFile().getParentFile();
			env.setBase(base);
			add(env);
			envChanged = true;
		}

		if (envChanged) {
			getWorkspaceEntry().setChanged(true);
			getWorkspaceEntry().saveMemento();
		}
	}

	@Override
	public ModelProperties getProperties(Node node) {
		ModelProperties properties = super.getProperties(node);
		if (node == null) {
			properties.add(new EnvironmentFilePropertyDescriptor(this));
		} else if (node instanceof VisualFunctionContact) {
			VisualFunctionContact contact = (VisualFunctionContact)node;
			VisualContactFormulaProperties props = new VisualContactFormulaProperties(this);
			properties.add(props.getSetProperty(contact));
			properties.add(props.getResetProperty(contact));
		}
		return properties;
	}

	public String getMathName(VisualComponent component) {
		return getMathModel().getName(component.getReferencedComponent());
	}

}
