package org.workcraft.plugins.cpog.tasks;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.VisualTransformableNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.One;
import org.workcraft.formula.Zero;
import org.workcraft.formula.encoding.Encoding;
import org.workcraft.formula.encoding.onehot.OneHotIntBooleanFormula;
import org.workcraft.formula.encoding.onehot.OneHotNumberProvider;
import org.workcraft.formula.jj.BooleanFormulaParser;
import org.workcraft.formula.jj.ParseException;
import org.workcraft.formula.sat.CleverCnfGenerator;
import org.workcraft.formula.sat.DefaultSolver;
import org.workcraft.formula.sat.Optimiser;
import org.workcraft.formula.utils.FormulaToString;
import org.workcraft.plugins.cpog.EncoderSettings;
import org.workcraft.plugins.cpog.EncoderSettings.GenerationMode;
import org.workcraft.plugins.cpog.Variable;
import org.workcraft.plugins.cpog.VariableState;
import org.workcraft.plugins.cpog.VisualArc;
import org.workcraft.plugins.cpog.VisualCpog;
import org.workcraft.plugins.cpog.VisualScenario;
import org.workcraft.plugins.cpog.VisualScenarioPage;
import org.workcraft.plugins.cpog.VisualVertex;
import org.workcraft.util.Func;
import org.workcraft.util.Geometry;
import org.workcraft.workspace.WorkspaceEntry;

public class ScencoExecutionSupport {

    // UTILITY FUNCTIONS
    // FUNCTION TO CONVERT BINARY TO INT
    protected String binaryToInt(String string) {
        int value = 0, wg = 1;
        if (string != null) {
            for (int i = string.length() - 1; i >= 0; i--) {
                if (string.charAt(i) == '1') {
                    value += wg;
                }
                wg *= 2;
            }

            return String.valueOf(value);
        }
        return "0";
    }

    // BUILD CONSTRAINT FOR EACH ELEMENTS LOOPING ON THE SCENARIOS
    protected String generateConstraint(char[][][] constraints, int numScenarios, int event1, int event2) {
        StringBuilder s = new StringBuilder();
        for (int k = 0; k < numScenarios; k++) s.append(constraints[k][event1][event2]);
        return s.toString();
    }

    // FUNCTION FOR SEEKING ALL TRIVIAL CONSTRAINTS
    protected char trivialEncoding(char[][][] constraints, int numScenarios, int event1, int event2) {
        char trivial = '-';

        for (int k = 0; k < numScenarios; k++) {
            if (constraints[k][event1][event2] == '0') {
                if (trivial == '1') return '?';
                trivial = '0';
            }

            if (constraints[k][event1][event2] == '1') {
                if (trivial == '0') return '?';
                trivial = '1';
            }
        }

        return trivial;
    }

    // FUNCTION FOR PARSING FILE CONTAINING BEST SOLUTION FOUND. IT PRINTS
    // THE MICROCONTROLLER SYNTHESISED WITH ABC TOOL
    protected void printController(int m, String resultDirectoryPath, String[] optEnc) {
        System.out.println();
        String fileName = resultDirectoryPath;
        for (int i = 0; i < m; i++) {
            fileName = fileName.concat(binaryToInt(optEnc[i]) + "_");
        }
        fileName = fileName.concat(".prg");
        File f = new File(fileName);
        if (f.exists() && !f.isDirectory()) {
            System.out.println("Boolean controller:");
            try {
                FileInputStream fstream = new FileInputStream(fileName);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader bre = new BufferedReader(new InputStreamReader(in));
                String strLine;
                bre.readLine();
                bre.readLine();
                while ((strLine = bre.readLine()) != null) {
                    System.out.println(strLine);
                }
                in.close();
            } catch (Exception e) { //Catch exception if any
                System.err.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    // RESET ALL THE PARAMETERS TO CALL SCENCO TOOL
    protected void resetVars(String verbose, String genMode, String numSol, String customFlag, String customPath, String effort, String espressoFlag, String abcFlag, String gateLibFlag, String cpogSize, String disableFunction, String oldSynt) {
        verbose = "";
        genMode = "";
        numSol = "";
        customFlag = "";
        customPath = "";
        effort = "";
        espressoFlag = "";
        abcFlag = "";
        gateLibFlag = "";
        cpogSize = "";
        disableFunction = "";
        oldSynt = "";
    }

    protected int scanScenarios(int m, ArrayList<VisualTransformableNode> scenarios,
            HashMap<String, Integer> events, ArrayList<Point2D> positions,
            ArrayList<Integer> count) {
        int n = 0;
        // Scan every scenario
        for (int k = 0; k < m; k++) {

            // Scan every elements of each scenario
            for (VisualComponent component : scenarios.get(k).getComponents()) {
                if (component instanceof VisualVertex) {
                    // If element is a vertex
                    VisualVertex vertex = (VisualVertex) component;

                    if (!events.containsKey(vertex.getLabel())) { // Check if a condition is present on vertex
                        events.put(vertex.getLabel(), n);
                        count.add(1);
                        Point2D p = vertex.getCenter();
                        p.setLocation(p.getX() - scenarios.get(k).getBoundingBox().getMinX(), p.getY() - scenarios.get(k).getBoundingBox().getMinY());
                        positions.add(p);
                        n++;
                    } else {
                        int id = events.get(vertex.getLabel());
                        count.set(id, count.get(id) + 1);
                        Point2D p = vertex.getCenter();
                        p.setLocation(p.getX() - scenarios.get(k).getBoundingBox().getMinX(), p.getY() - scenarios.get(k).getBoundingBox().getMinY());
                        positions.set(id, Geometry.add(positions.get(id), p));
                    }
                }
            }
        }
        return n;
    }

    protected ArrayList<String> constructConstraints(char[][][] constraints, int[][] graph, int m, int n,
            ArrayList<VisualTransformableNode> scenarios,
            HashMap<String, Integer> events, ArrayList<Point2D> positions,
            ArrayList<Integer> count) {

        ArrayList<String> args = new ArrayList<>();

        for (int k = 0; k < m; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    constraints[k][i][j] = '0';
                }
            }

            for (VisualComponent component : scenarios.get(k).getComponents()) {
                if (component instanceof VisualVertex) {
                    VisualVertex vertex = (VisualVertex) component;
                    int id = events.get(vertex.getLabel());
                    constraints[k][id][id] = '1';
                }
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    graph[i][j] = 0;
                }
            }

            for (VisualConnection c : scenarios.get(k).getConnections()) {
                if (c instanceof VisualArc) {
                    VisualArc arc = (VisualArc) c;
                    VisualNode c1 = arc.getFirst(), c2 = arc.getSecond();
                    if (c1 instanceof VisualVertex && c2 instanceof VisualVertex) {
                        int id1 = events.get(((VisualVertex) c1).getLabel());
                        int id2 = events.get(((VisualVertex) c2).getLabel());
                        graph[id1][id2] = 1;
                    }
                }
            }

            // compute transitive closure

            for (int t = 0; t < n; t++) {
                for (int i = 0; i < n; i++) {
                    if (graph[i][t] > 0) {
                        for (int j = 0; j < n; j++) {
                            if (graph[t][j] > 0) graph[i][j] = 1;
                        }
                    }
                }
            }

            // detect transitive arcs

            for (int t = 0; t < n; t++) {
                for (int i = 0; i < n; i++) {
                    if (graph[i][t] > 0) {
                        for (int j = 0; j < n; j++) {
                            if (graph[t][j] > 0) graph[i][j] = 2;
                        }
                    }
                }
            }

            // report cyclic scenario

            for (int i = 0; i < n; i++) {
                if (graph[i][i] > 0) {
                    args.add("ERROR");
                    args.add("Scenario '" + scenarios.get(k).getLabel() + "' is cyclic.");
                    args.add("Invalid scenario");
                    return args;
                }
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        char ch = '0';

                        if (graph[i][j] > 0) ch = '1';
                        if (graph[i][j] > 1) ch = '-';
                        if (constraints[k][i][i] == '0' || constraints[k][j][j] == '0') ch = '-';

                        constraints[k][i][j] = ch;
                    }
                }
            }
        }

        args.add("OK");
        return args;
    }

    // FUNCTION FOR PREPARING FILES NEEDED TO SCENCO TOOL TO WORK PROPERLY.
    // IT FILLS IN FILE CONTAINING ALL THE SCENARIOS AND THE CUSTOM ENCODING
    // FILE, IF USER WANTS TO USE A CUSTOM SOLUTION.
    protected int writeCpogIntoFile(int m, ArrayList<VisualTransformableNode> scenarios,
            File scenarioFile, File encodingFile, EncoderSettings settings) {
        try {

            PrintStream output = new PrintStream(scenarioFile);

            for (int k = 0; k < m; k++) {
                Map<String, Integer> nodes = new HashMap<>();

                output.println(".scenario CPOG_" + k);

                // Print GO-DONE signals
                if (!settings.isCpogSize()) {
                    for (VisualComponent component : scenarios.get(k).getComponents()) {
                        if (component instanceof VisualVertex) {
                            VisualVertex vertex = (VisualVertex) component;
                            output.println(settings.GO_SIGNAL + " " + vertex.getLabel());
                            output.println(vertex.getLabel() + " " + settings.DONE_SIGNAL);
                        }
                    }
                }

                // Print arcs
                for (VisualConnection c : scenarios.get(k).getConnections()) {
                    if (c instanceof VisualArc) {
                        VisualArc arc = (VisualArc) c;
                        VisualNode c1 = arc.getFirst(), c2 = arc.getSecond();
                        if (c1 instanceof VisualVertex && c2 instanceof VisualVertex) {
                            nodes.put(((VisualVertex) c1).getLabel(), 0);
                            nodes.put(((VisualVertex) c2).getLabel(), 0);
                            output.println(((VisualVertex) c1).getLabel() + " " + ((VisualVertex) c2).getLabel());
                        }
                    }
                }

                // Print conditions on vertices
                for (VisualComponent component : scenarios.get(k).getComponents()) {
                    if (component instanceof VisualVertex) {
                        VisualVertex vertex = (VisualVertex) component;
                        BooleanFormula condition = vertex.getCondition();
                        if (condition != One.instance() && condition != Zero.instance()) {

                            // Format output by substituting ' with !
                            String cond = FormulaToString.toString(condition).replace("'", "!");
                            String result = "";
                            String tmp = "";
                            for (int i = 0; i < cond.length(); i++) {
                                if (cond.charAt(i) != '(' && cond.charAt(i) != ')' && cond.charAt(i) != '+' &&
                                        cond.charAt(i) != '*' && cond.charAt(i) != '!' && cond.charAt(i) != ' ') {
                                    tmp = "";
                                    while (i < cond.length() && cond.charAt(i) != '(' && cond.charAt(i) != ')' && cond.charAt(i) != '+' &&
                                            cond.charAt(i) != '*' && cond.charAt(i) != '!' && cond.charAt(i) != ' ') {
                                        tmp += cond.charAt(i);
                                        i++;
                                    }
                                    //System.out.println("TMP: " + tmp);
                                    for (int j = tmp.length() - 1; j >= 0; j--) {
                                        //System.out.println(j + ") " + tmp.charAt(j));
                                        result += tmp.charAt(j);
                                    }
                                    if (i < cond.length()) {
                                        result += cond.charAt(i);
                                    }
                                } else {
                                    result += cond.charAt(i);
                                }
                            }

                            String end = "";
                            for (int i = 0; i < result.length(); i++) {
                                if (result.charAt(i) == '(') {
                                    end += ')';
                                } else if (result.charAt(i) == ')') {
                                    end += '(';
                                } else {
                                    end += result.charAt(i);
                                }
                            }

                            // Print conditions on each vertices
                            output.print(":");
                            for (int i = end.length() - 1; i >= 0; i--) {
                                output.print(end.charAt(i));
                            }
                            output.println(" " + vertex.getLabel());
                        }

                        //VisualVertex vertex = (VisualVertex) component;
                        if (!nodes.containsKey(vertex.getLabel())) {
                            output.println(vertex.getLabel());
                        }
                    }

                }
                output.println(".end");
                if (k != m - 1) {
                    output.println();
                }
            }
            output.close();

            // WRITING CUSTOM ENCODING FILE
            if (settings.getGenMode() != GenerationMode.SCENCO) {

                if (settings.isCustomEncMode()) {
                    PrintStream output1 = new PrintStream(encodingFile);

                    String[] enc = settings.getCustomEnc();
                    for (int k = 0; k < m; k++) {
                        if (enc[k].contains("2") || enc[k].contains("3") || enc[k].contains("4") ||
                                enc[k].contains("5") || enc[k].contains("6") || enc[k].contains("7") ||
                                enc[k].contains("8") || enc[k].contains("9")) {
                            JOptionPane.showMessageDialog(Framework.getInstance().getMainWindow(),
                                    "Op-code " + enc[k] + " not allowed.",
                                    "Custom encoding error", JOptionPane.ERROR_MESSAGE);
                            output1.close();
                            return -1;

                        }
                        String empty = "";
                        for (int i = 0; i < settings.getBits(); i++) empty += 'X';
                        if (enc[k].isEmpty() || enc[k].equals(empty)) {
                            output1.println("/");
                        } else {
                            output1.println(enc[k]);
                        }
                    }
                    output1.println(settings.getBits());
                    output1.close();
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }

        return 0;
    }

    // THIS FUNCTION CALLS SCENCO TOOL BY PASSING ALL THE NEEDED ARGUMENTS BY COMMAND LINE.
    // IN ADDITION, IT PARSES THE OUTPUT OF THE TOOL INSTAINTIANING ALL THE VARIABLES NEEDED
    // TO WORKCRAFT TO BUILD THE COMPOSITIONAL GRAPH
    protected int callingScenco(Process process, EncoderSettings settings, ArrayList<String> parameters,
            Double currArea, WorkspaceEntry we, int it, boolean continuous, String[] optEnc,
            String[] optFormulaeVertices, String[] truthTableVertices, String[] optVertices,
            String[] optSources, String[] optDests, String[] optFormulaeArcs, String[] truthTableArcs,
            String[] arcNames) throws IOException {
        int a = 0;
        int v = 0;
        //Debug Printing: launching executable
        /*System.out.println("CALLING SCENCO: " + scencoCommand + " " + scenarioFile.getAbsolutePath() + " " +
          "-m" + " " + effort + " " + genMode + " " + numSol + " " + customFlag + " " + customPath + " " +
          verbose + " " + cpogSize + " " + disableFunction + " " + oldSynt + " " +
          espressoFlag + " " + espressoCommand + " " + abcFlag + " " + abcFolder + " " + gateLibFlag + " " +
          gatesLibrary + " " + modBitFlag + " " + modBit); */

        process = new ProcessBuilder(parameters).start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            if (settings.isVerboseMode()) {
                System.out.println(line);
            }

            // Read Optimal Encoding
            if (line.contains("MIN: ")) {
                StringTokenizer st2 = new StringTokenizer(line, " ");
                int j = 0;
                st2.nextElement();
                while (st2.hasMoreElements()) {
                    optEnc[j++] = (String) st2.nextElement();
                }
            }

            // Read Optimal Formulae
            if (line.contains(".start_formulae")) {
                line = br.readLine();
                while (line.contains(".end_formulae") == false) {
                    if (settings.isVerboseMode()) {
                        System.out.println(line);
                    }
                    StringTokenizer st2 = new StringTokenizer(line, ",");
                    String el = (String) st2.nextElement();
                    if (el.equals("V")) { //formula of a vertex
                        optVertices[v] = (String) st2.nextElement();
                        truthTableVertices[v] = (String) st2.nextElement();
                        optFormulaeVertices[v++] = (String) st2.nextElement();
                    } else {
                        optSources[a] = (String) st2.nextElement();
                        optDests[a] = (String) st2.nextElement();
                        arcNames[a] = optSources[a] + "->" + optDests[a];
                        truthTableArcs[a] = (String) st2.nextElement();
                        optFormulaeArcs[a++] = (String) st2.nextElement();
                    }
                    line = br.readLine();
                }

            }

            // Read statistics
            if (line.contains(".statistics")) {
                line = br.readLine();
                while (line.contains(".end_statistics") == false) {
                    System.out.println(line);
                    line = br.readLine();
                }
            }

            // Read errors
            if (line.contains(".error")) {
                line = br.readLine();
                while (line.contains(".end_error") == false) {
                    JOptionPane.showMessageDialog(Framework.getInstance().getMainWindow(),
                            line, "scenco error", JOptionPane.ERROR_MESSAGE);
                    line = br.readLine();
                }
                return -1;

            }
        }

        process.destroy();
        is.close();
        isr.close();
        br.close();
        return 0;
    }

    // FUNCTION TO INSTANTIATE THE MAP FOR CONNECTING EACH VISUAL ELEMENT IN WORKCRAFT
    // INTO THE CORRESPONDING FORMULA. THE KEY IS REPRESENTED BY THE NAME OF THE ELEMENT,
    // FOR THE ARCS, THE NAME CORRESPOND TO NAME OF SOURCE -> NAME OF DEST
    protected void connectFormulaeToVisualVertex(int v, int a, Variable[] vars, HashMap<String,
            BooleanFormula> formulaeName, String[] optFormulaeVertices, String[] optVertices,
            String[] optFormulaeArcs, String[] arcNames) throws ParseException {
        final Variable[] variables = vars;
        for (int i = 0; i < v; i++) {
            if (optFormulaeVertices[i].contains("x")) {
                BooleanFormula formulaOpt = null;
                formulaOpt = BooleanFormulaParser.parse(optFormulaeVertices[i], new Func<String, BooleanFormula>() {

                    @Override
                    public BooleanFormula eval(String arg) {
                        arg = arg.substring("x_".length());
                        int id = Integer.parseInt(arg);
                        return variables[id];
                    }
                });

                formulaeName.put(optVertices[i], formulaOpt);

            }
        }
        for (int i = 0; i < a; i++) {
            if (optFormulaeArcs[i].contains("x")) {
                BooleanFormula formulaOpt = null;
                formulaOpt = BooleanFormulaParser.parse(optFormulaeArcs[i], new Func<String, BooleanFormula>() {
                    @Override
                    public BooleanFormula eval(String arg) {
                        arg = arg.substring("x_".length());
                        int id = Integer.parseInt(arg);
                        return variables[id];
                    }
                });

                formulaeName.put(arcNames[i], formulaOpt);
            }
        }
    }

    // Instantiating encoding into graphs
    protected void instantiateEncoding(int m, int freeVariables,
            ArrayList<VisualTransformableNode> scenarios, Variable[] vars,
            boolean[][] encoding, int pr, HashMap<String, Integer> events,
            VisualVertex[] vertices, VisualCpog cpog, VisualScenario resultCpog,
            ArrayList<Point2D> positions, ArrayList<Integer> count,
            HashMap<String, BooleanFormula> formulaeName) {
        for (int k = 0; k < m; k++) {
            for (int i = 0; i < freeVariables; i++) {
                if (scenarios.get(k) instanceof VisualScenario) {
                    VisualScenario scenario = (VisualScenario) scenarios.get(k);
                    scenario.getEncoding().setState(vars[i], VariableState.fromBoolean(encoding[k][i]));
                } else if (scenarios.get(k) instanceof VisualScenarioPage) {
                    VisualScenarioPage scenario = (VisualScenarioPage) scenarios.get(k);
                    scenario.getEncoding().setState(vars[i], VariableState.fromBoolean(encoding[k][i]));
                }
                //scenario.getEncoding().setState(vars[i], VariableState.fromBoolean(encoding[k][i]));
            }
            for (int i = freeVariables; i < freeVariables + pr; i++) {
                if (scenarios.get(k) instanceof  VisualScenario) {
                    VisualScenario scenario = (VisualScenario) scenarios.get(k);
                    scenario.getEncoding().setState(vars[i], VariableState.fromBoolean(encoding[k][i]));
                } else if (scenarios.get(k) instanceof VisualScenarioPage) {
                    VisualScenarioPage scenario = (VisualScenarioPage) scenarios.get(k);
                    scenario.getEncoding().setState(vars[i], VariableState.fromBoolean(encoding[k][i]));
                }
                //scenarios.get(k).getEncoding().setState(vars[i], VariableState.fromBoolean(encoding[k][i]));
            }
        }

        for (String eventName : events.keySet()) {
            int id = events.get(eventName);
            vertices[id] = cpog.createVisualVertex(resultCpog);
            vertices[id].setLabel(eventName);
            vertices[id].setPosition(Geometry.multiply(positions.get(id), 1.0 / count.get(id)));
            if (formulaeName.containsKey(eventName)) {
                vertices[id].setCondition(formulaeName.get(eventName));
            } else {
                vertices[id].setCondition(One.instance());
            }
        }

    }

    // Build up the Cpog into Workcraft window
    protected void buildCpog(int n, int m, char[][][] constraints,
            VisualCpog cpog, VisualVertex[] vertices, HashMap<String,
            BooleanFormula> formulaeName) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                BooleanFormula condition;

                char trivial = trivialEncoding(constraints, m, i, j);
                if (trivial != '?') {
                    if (trivial == '1') {
                        condition = One.instance();
                    } else {
                        continue;
                    }
                }
                if (i != j) {
                    VisualArc arc = cpog.connect(vertices[i], vertices[j]);
                    String arcName = vertices[i].getLabel() + "->" + vertices[j].getLabel();

                    if (formulaeName.containsKey(arcName)) {
                        condition = formulaeName.get(arcName);
                    } else {
                        condition = One.instance();
                    }

                    arc.setCondition(condition);
                }
            }
        }
    }

    // group similar constraints
    protected void groupConstraints(int n, int m, char[][][] constraints, HashMap<String, Integer> task) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (trivialEncoding(constraints, m, i, j) == '?') {
                    String constraint = generateConstraint(constraints, m, i, j);
                    if (!task.containsKey(constraint)) {
                        task.put(constraint, task.size());
                    }
                }
            }
        }
    }

    protected Encoding satBasedRun(int predicates, Variable[] vars, String[] instance, int derivedVariables) {
        Optimiser<OneHotIntBooleanFormula> oneHot = new Optimiser<>(new OneHotNumberProvider());
        DefaultSolver<BooleanFormula> solverCnf = new DefaultSolver<>(oneHot, new CleverCnfGenerator());
        Encoding solution = null;

        try {
            if (predicates > 0) {
                System.out.println("INFORMATION: SAT-Based encoding cannot handle graphs with internal CPOGs.");
                return solution;
            }

            solution = solverCnf.solve(instance, vars, derivedVariables);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Framework.getInstance().getMainWindow(),
                    e.getMessage(), "Encoding result", JOptionPane.ERROR_MESSAGE);
            System.out.println("INFORMATION: Scenco cannot solve the CPOG.");
        }

        return solution;
    }

}
