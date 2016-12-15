package org.workcraft.plugins.cpog.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.Command;
import org.workcraft.plugins.cpog.VisualCpog;
import org.workcraft.plugins.cpog.tasks.PGMinerResultHandler;
import org.workcraft.plugins.cpog.tasks.PGMinerTask;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class PGMinerSelectedGraphsExtractionTool implements Command {

    public String getSection() {
        return "! Process Mining";
    }

    public String getDisplayName() {
        return "Extract concurrency of selected graphs";
    }

    @Override
    public boolean isApplicableTo(ModelEntry me) {
        return me.getVisualModel() instanceof VisualCpog;
    }

    public File getInputFile(WorkspaceEntry we) {
        try {
            VisualCpog visualCpog = (VisualCpog) we.getModelEntry().getVisualModel();
            String allGraphs = CpogParsingTool.getExpressionFromGraph(visualCpog);
            ArrayList<String> tempGraphs = new ArrayList<>();
            ArrayList<String> graphs = new ArrayList<>();

            if (allGraphs == "") throw new ArrayIndexOutOfBoundsException();

            int i = allGraphs.indexOf(" + ");
            while (i > -1) {
                allGraphs = allGraphs.substring(0, i) + "\n" + allGraphs.substring(i + 2);
                i = allGraphs.indexOf(" + ");
            }
            allGraphs = allGraphs + "\n";
            allGraphs = allGraphs.replaceAll(" -> ", " ");

            String[] graphList = allGraphs.split("\n");

            allGraphs = "";

            for (String g : graphList) tempGraphs.add(g);

            for (String graph : tempGraphs) {
                int index = graph.indexOf("= ");
                if (index >= 0) {
                    graph = graph.substring(index + 2);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Error: A graph which is not a scenario has been selected.\n"
                            + "Please remove this from the selection, or group this as a page to continue",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                graph = graph.trim();
                graphs.add(graph);
            }

            File inputFile = File.createTempFile("input", ".tr");
            PrintStream expressions = new PrintStream(inputFile);

            for (String graph: graphs) {
                expressions.println(graph);
            }

            expressions.close();

            return inputFile;

        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e2) {

            JOptionPane.showMessageDialog(null,
                    "Error: No scenarios have been selected",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            throw e2;
        }

        return null;
    }

    @Override
    public ModelEntry run(ModelEntry me) {
        return null; // !!!
    }

    @Override
    public WorkspaceEntry run(WorkspaceEntry we) {
        try {
            PGMinerTask task = new PGMinerTask(getInputFile(we), false);
            final Framework framework = Framework.getInstance();
            PGMinerResultHandler result = new PGMinerResultHandler((VisualCpog) we.getModelEntry().getVisualModel(), we, true);
            framework.getTaskManager().queue(task, "PGMiner", result);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        return we;
    }

}
