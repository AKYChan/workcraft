package org.workcraft.plugins.circuit;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.workcraft.Framework;
import org.workcraft.Info;
import org.workcraft.exceptions.DeserialisationException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.plugins.circuit.interop.VerilogFormat;
import org.workcraft.plugins.interop.EpsFormat;
import org.workcraft.plugins.interop.PdfFormat;
import org.workcraft.plugins.interop.PngFormat;
import org.workcraft.plugins.interop.PsFormat;
import org.workcraft.plugins.interop.SvgFormat;
import org.workcraft.util.FileUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class CircuitExportTests {

    @BeforeClass
    public static void init() {
        final Framework framework = Framework.getInstance();
        framework.init();
    }

    @Test
    public void testVmeCircuitExport()
            throws DeserialisationException, IOException, SerialisationException {
        String vHeader = Info.getGeneratedByText("// Verilog netlist ", "\n") +
                "module vme (d, lds, dtack, dsr, dsw, ldtack);\n" +
                "    input dsr, dsw, ldtack;\n" +
                "    output d, lds, dtack;";
        String svgHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.0//EN'\n" +
                "          'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'>";
        String pngHeader =  (char) 0xFFFD + "PNG";
        String pdfHeader = "%PDF-1.4";
        String epsHeader = "%!PS-Adobe-3.0 EPSF-3.0";
        String psHeader = "%!PS-Adobe-3.0";

        testCircuitExport("org/workcraft/plugins/circuit/vme-tm.circuit.work",
                vHeader, svgHeader, pngHeader, pdfHeader, epsHeader, psHeader);
    }

    private void testCircuitExport(String workName, String vHeader,
            String svgHeader, String pngHeader, String pdfHeader, String epsHeader, String psHeader)
                    throws DeserialisationException, IOException, SerialisationException {

        final Framework framework = Framework.getInstance();
        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource(workName);
        WorkspaceEntry we = framework.loadWork(url.getFile());
        ModelEntry me = we.getModelEntry();
        File directory = FileUtils.createTempDirectory("workcraft-" + workName);

        File vFile = new File(directory, "export.v");
        framework.exportModel(me, vFile, VerilogFormat.getInstance());
        Assert.assertEquals(vHeader, FileUtils.readHeader(vFile, vHeader.length()));

        File svgFile = new File(directory, "export.svg");
        framework.exportModel(me, svgFile, SvgFormat.getInstance());
        Assert.assertEquals(svgHeader, FileUtils.readHeader(svgFile, svgHeader.length()));

        File pngFile = new File(directory, "export.png");
        framework.exportModel(me, pngFile, PngFormat.getInstance());
        Assert.assertEquals(pngHeader, FileUtils.readHeader(pngFile, pngHeader.length()));

        File pdfFile = new File(directory, "export.pdf");
        framework.exportModel(me, pdfFile, PdfFormat.getInstance());
        Assert.assertEquals(pdfHeader, FileUtils.readHeader(pdfFile, pdfHeader.length()));

        File epsFile = new File(directory, "export.eps");
        framework.exportModel(me, epsFile, EpsFormat.getInstance());
        Assert.assertEquals(epsHeader, FileUtils.readHeader(epsFile, epsHeader.length()));

        File psFile = new File(directory, "export.ps");
        framework.exportModel(me, psFile, PsFormat.getInstance());
        Assert.assertEquals(psHeader, FileUtils.readHeader(psFile, psHeader.length()));

        framework.closeWork(we);
        FileUtils.deleteOnExitRecursively(directory);
    }

}
