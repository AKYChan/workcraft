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

package org.workcraft.dom.visual.connections;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.workcraft.dom.Node;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.TransformHelper;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.DummyMouseListener;

public class DefaultAnchorGenerator extends DummyMouseListener {
	@Override
	public void mouseClicked(GraphEditorMouseEvent e) {
		if (e.getClickCount() == 2) {
			VisualModel model = e.getModel();
			Node node = HitMan.hitTestForSelection(e.getPosition(), model);
			if (node instanceof VisualConnection) {
				VisualConnection connection = (VisualConnection)node;
				if (connection.getGraphic() instanceof Polyline) {
					e.getEditor().getWorkspaceEntry().saveMemento();
					model.selectNone();
					Polyline polyline = (Polyline)connection.getGraphic();
					AffineTransform t = TransformHelper.getTransform(model.getRoot(), connection);
					Point2D pt = t.transform(e.getPosition(), null);
					polyline.createControlPoint(pt);
					model.select(connection);
				}
			}
		}
	}

}