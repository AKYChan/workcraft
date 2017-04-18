package org.workcraft.plugins.interop;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;
import org.workcraft.dom.Model;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.interop.Exporter;
import org.workcraft.serialisation.Format;
import org.workcraft.util.XmlUtil;

public class SVGExporter implements Exporter {

    @Override
    public void export(Model model, OutputStream out) throws IOException, SerialisationException {
        if (model == null) {
            throw new SerialisationException("Not a visual model");
        }
        try {
            Document doc = XmlUtil.createDocument();
            SVGGraphics2D g2d = new SVGGraphics2D(doc);
            g2d.scale(50, 50);
            Rectangle2D bounds = ((VisualGroup) model.getRoot()).getBoundingBoxInLocalSpace();
            g2d.translate(-bounds.getMinX(), -bounds.getMinY());
            g2d.setSVGCanvasSize(new Dimension((int) (bounds.getWidth() * 50), (int) (bounds.getHeight() * 50)));
            ((VisualModel) model).draw(g2d, Decorator.Empty.INSTANCE);
            g2d.stream(new OutputStreamWriter(out));
        } catch (ParserConfigurationException e) {
            throw new SerialisationException(e);
        }
    }

    @Override
    public String getDescription() {
        return ".svg (Batik SVG generator)";
    }

    @Override
    public String getExtenstion() {
        return ".svg";
    }

    @Override
    public int getCompatibility(Model model) {
        if (model instanceof VisualModel) {
            return Exporter.GENERAL_COMPATIBILITY;
        } else {
            return Exporter.NOT_COMPATIBLE;
        }
    }

    @Override
    public UUID getTargetFormat() {
        return Format.SVG;
    }

}
