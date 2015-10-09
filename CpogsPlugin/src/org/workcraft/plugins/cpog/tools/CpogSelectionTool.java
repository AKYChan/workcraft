package org.workcraft.plugins.cpog.tools;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.PageNode;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.gui.graph.tools.SelectionTool;
import org.workcraft.observation.HierarchyEvent;
import org.workcraft.observation.HierarchySupervisor;
import org.workcraft.observation.NodesAddedEvent;
import org.workcraft.observation.NodesDeletingEvent;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.observation.StateEvent;
import org.workcraft.observation.StateSupervisor;
import org.workcraft.plugins.cpog.CPOGHangingConnectionRemover;
import org.workcraft.plugins.cpog.Variable;
import org.workcraft.plugins.cpog.VisualArc;
import org.workcraft.plugins.cpog.VisualCPOG;
import org.workcraft.plugins.cpog.VisualScenarioPage;
import org.workcraft.plugins.cpog.VisualVariable;
import org.workcraft.plugins.cpog.VisualVertex;
import org.workcraft.plugins.cpog.expressions.CpogConnector;
import org.workcraft.plugins.cpog.expressions.CpogFormula;
import org.workcraft.plugins.cpog.expressions.CpogFormulaToString;
import org.workcraft.plugins.cpog.expressions.GraphFunc;
import org.workcraft.plugins.cpog.expressions.javacc.CpogExpressionParser;
import org.workcraft.plugins.cpog.expressions.javacc.ParseException;
import org.workcraft.plugins.cpog.expressions.javacc.TokenMgrError;
import org.workcraft.plugins.cpog.optimisation.BooleanFormula;
import org.workcraft.plugins.cpog.optimisation.booleanvisitors.FormulaToString;
import org.workcraft.workspace.WorkspaceEntry;



public class CpogSelectionTool extends SelectionTool {

	final int margin = 4;
	final double minRadius = 2.0;
	final double expandRadius = 2.0;
	double maxX = 0, maxY = 0;
	int xpos = 0;
	boolean transitivesActive = true;

	private JTextArea expressionText;
	HashMap<String, CpogFormula> graphMap = new HashMap<String, CpogFormula>();
	final HashMap<String, Variable> variableMap = new HashMap<>();
    private HashMap<String, GraphReference> referenceMap = new HashMap<>();
	private Checkbox insertTransitives;
    private final HashMap<String, Point2D> prevPoints = new HashMap<>();
	private double highestY = 0; //Sets first graph at y co-ordinate of 0

	private CpogParsingTool parsingTool = new CpogParsingTool(variableMap, xpos, referenceMap);

	private ArrayList<VisualPage> refPages = new ArrayList<>();

	private GraphEditor editor;

	public CpogSelectionTool() {
		super();
	}

	public CpogSelectionTool(boolean enablePages) {
		super(enablePages);
	}

	@Override
	public void createInterfacePanel(final GraphEditor editor) {
		this.editor = editor;
		super.createInterfacePanel(editor);
		expressionText = new JTextArea();
		expressionText.setLineWrap(false);
		expressionText.setEditable(true);
		expressionText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane expressionScroll = new JScrollPane(expressionText);

		JPanel buttonPanel = new JPanel();

		JButton btnInsert = new JButton("Insert");
		btnInsert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int prevLineEnd = 0;
				ArrayList<String> expressions = new ArrayList<String>();
				try {
					for (int i = 0; i < expressionText.getLineCount(); i++) {
						String exp = expressionText.getText().substring(prevLineEnd, expressionText.getLineEndOffset(i));

						exp = exp.replace("\n", "");
						exp = exp.replace("\t", " ");

						if (exp.compareTo("") != 0) {
							expressions.add(exp);
						}

						prevLineEnd = expressionText.getLineEndOffset(i);
					}
					String exp = "";
					for (String s : expressions) {
						if (!s.contains("=")) {
							exp = exp + " " + s;
						} else {
							if (exp.compareTo("") != 0) {
								insertExpression(exp, false, false, true, false);
								exp = "";
							}
							exp = s;
						}
					}
					if (exp.compareTo("") != 0) {
						insertExpression(exp, false, false, true, false);
					}
				} catch (BadLocationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});
		buttonPanel.add(btnInsert);



		final JButton btnTextInsert = new JButton("Text File");
		btnTextInsert.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                File textFile;
                Scanner fileIn = null;
                String equation;

                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "Text Files", "txt");
                chooser.setFileFilter(filter);
                chooser.showOpenDialog(btnTextInsert);
                textFile = chooser.getSelectedFile();
                try {
                    fileIn = new Scanner(textFile);
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    JOptionPane.showMessageDialog(null, e1.getMessage(),
                            "File not found error", JOptionPane.ERROR_MESSAGE);
                }
                while (fileIn.hasNextLine()) {
                    equation = fileIn.nextLine();
                    insertExpression(equation, true, false, true, false);
                }
            }

        });
		buttonPanel.add(btnTextInsert);

		final JButton btnGetGraphExpression = new JButton("Get expression");
		btnGetGraphExpression.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				VisualCPOG visualCpog = (VisualCPOG) editor.getWorkspaceEntry().getModelEntry().getVisualModel();
				expressionText.setText(parsingTool.getExpressionFromGraph(visualCpog));
			}

		});
		buttonPanel.add(btnGetGraphExpression);

		insertTransitives = new Checkbox("Insert Transitives", false);
		controlPanel.add(insertTransitives);

		interfacePanel.add(expressionScroll, BorderLayout.CENTER);
		interfacePanel.add(buttonPanel, BorderLayout.SOUTH);
	}

	public HashMap<String, VisualVertex> insertExpression(String text,
			final boolean createDuplicates, boolean getVertList, boolean zoomFit, boolean blockTransitiveRemoval) {
        WorkspaceEntry we = editor.getWorkspaceEntry();
        final VisualCPOG visualCpog = (VisualCPOG) we.getModelEntry().getVisualModel();
        we.captureMemento();

        visualCpog.setCurrentLevel(visualCpog.getRoot());

        final LinkedHashMap<String, VisualVertex> vertexMap = new LinkedHashMap<String, VisualVertex>();
        final HashSet<ArcCondition> arcConditionList = new HashSet<ArcCondition>();
        text = text.replace("\n", "");
        text = parsingTool.replaceReferences(text);

        CpogFormula f = null;
        GraphFunc<String, CpogFormula> PGF = null;
        final HashMap<String, VisualVertex> localVertices = new HashMap<String, VisualVertex>();
        try {
            f = CpogExpressionParser.parse(text,
                    PGF = new GraphFunc<String, CpogFormula>() {

                        String name;
                        boolean ref;

                        @Override
                        public CpogFormula eval(String label) {
                            VisualVertex vertex = null;

                            if (vertexMap.containsKey(label)) {
                                vertex = vertexMap.get(label);
                                localVertices.put(label, vertex);
                                return vertex;
                            }

                            vertex = null;

                            // TODO: Optimise!

                            if (!createDuplicates)
                                for (VisualVertex v : visualCpog.getVertices(visualCpog.getCurrentLevel()))
                                    if (v.getLabel().equals(label)) {
                                        vertex = v;
                                        localVertices.put(label, vertex);
                                        break;
                                    }

                            if (vertex == null) {
                                vertex = visualCpog.createVisualVertex(visualCpog.getCurrentLevel());
                                vertex.setLabel(label);
                                vertexMap.put(label, vertex);
                                localVertices.put(label, vertex);
                            }
                            return vertex;
                        }

                        @Override
                        public GraphFunc<String, CpogFormula> removeGraphName(String name) {
                            if (vertexMap.containsKey(name)) {
                                vertexMap.remove(name);
                            }
                            return this;
                        }

                        @Override
                        public CpogFormula eval(String label, String boolExpression) throws ParseException {

                            VisualVertex vertex = null;
                            BooleanFormula bf;

                            if (vertexMap.containsKey(label)) {
                                vertex = vertexMap.get(label);
                                if (boolExpression != "") {
                                    if (FormulaToString.toString(vertex.getCondition()) == "") {
                                        try {
                                            vertex.setCondition(parsingTool.parseBool(boolExpression, visualCpog));
                                        } catch (ParseException e) {
                                            throw new ParseException("Boolean error in: " + boolExpression);
                                        }
                                    } else {
                                        try {
                                            vertex.setCondition(parsingTool.parseBool(FormulaToString.toString(vertex.getCondition()) + "|" + boolExpression, visualCpog));
                                        } catch (ParseException e) {
                                            throw new ParseException("Boolean error in: " + boolExpression);
                                        }
                                    }
                                }
                                return vertex;
                            }

                            // TODO: Optimise!

                            if (!createDuplicates)
                                for (VisualVertex v : visualCpog.getVertices(visualCpog.getCurrentLevel()))
                                    if (v.getLabel().equals(label)) {
                                        vertex = v;
                                        localVertices.put(label, vertex);
                                        break;
                                    }

                            if (vertex == null) {
                                vertex = visualCpog.createVisualVertex(visualCpog.getCurrentLevel());
                                vertex.setLabel(label);
                                vertexMap.put(label, vertex);
                                localVertices.put(label, vertex);
                            }

                            if (boolExpression != "") {
                                if (FormulaToString.toString(vertex.getCondition()) == "") {
                                    try {
                                        bf = parsingTool.parseBool(boolExpression, visualCpog);
                                        vertex.setCondition(bf);
                                    } catch (ParseException e) {
                                        throw new ParseException("Boolean error in: " + boolExpression);
                                    }
                                } else {
                                    try {
                                        bf = parsingTool.parseBool(boolExpression, visualCpog);
                                        vertex.setCondition(bf);
                                    } catch (ParseException e) {
                                        throw new ParseException("Boolean error in: " + boolExpression);
                                    }
                                }
                            }
                            return vertex;
                        }

                        @Override
                        public String getGraphName() {
                            return name;
                        }

                        @Override
                        public void setGraphName(String graphName) {
                            this.name = graphName;
                            if ((name.contains("{")) && (name.contains("}"))) {
                                ref = true;
                            }
                        }

                        @Override
                        public void setSequenceCondition(CpogFormula formula, String boolForm) {
                            ArcCondition a = new ArcCondition(formula, boolForm);
                            arcConditionList.add(a);
                        }

                        @Override
                        public boolean getRef() {
                            // TODO Auto-generated method stub
                            return ref;
                        }

                    });
        } catch (ParseException e) {
            we.cancelMemento();
            JOptionPane.showMessageDialog(null, e.getMessage(), "Parse error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (TokenMgrError e) {
            we.cancelMemento();
            JOptionPane.showMessageDialog(null, e.getMessage(),
                    "Lexical error", JOptionPane.ERROR_MESSAGE);
            return null;
        }


        if (getVertList) {
            for (VisualVertex v :vertexMap.values()) {
                visualCpog.removeWithoutNotify(v);
            }
            return localVertices;
        } else {

        visualCpog.selectNone();
        int n = vertexMap.size();
        int i = 0;

        for (VisualVertex v : vertexMap.values()) {
            visualCpog.addToSelection(v);
        }
            CpogConnector cc = new CpogConnector(visualCpog);
            f.accept(cc);
            VisualPage inserted = null;
        if (!PGF.getRef()) {//If this graph is not for reference

            graphMap.put(PGF.getGraphName(), f);

            parsingTool.setArcConditions(arcConditionList, visualCpog, vertexMap);

            LinkedHashSet<Node> roots = getRootNodes(visualCpog, vertexMap.values());//new LinkedHashSet<Node>();

            if (!(insertTransitives.getState())) {
            	boolean[][] c = parsingTool.convertToArrayForm(vertexMap.values(), visualCpog);
            	parsingTool.computeTransitiveClosure(c);
            	if (!parsingTool.hasSelfLoops(c)) {
            		boolean [][] t = parsingTool.findTransitives(c);
            		parsingTool.convertFromArrayForm(t, vertexMap.values(), visualCpog);
            	}
            }

            ArrayList<Node> prevSelection = new ArrayList<>();
            for (Node n1 : vertexMap.values()) {
                prevSelection.add(n1);
            }


            ArrayList<String> usedReferences = parsingTool.getUsedReferences();

            addUsedReferences(visualCpog, editor, usedReferences, localVertices, prevSelection);

            if (roots.isEmpty()) {
                noRootLayout(vertexMap, n, i);
            } else {
                bfsLayout(visualCpog, roots);
            }

            editor.requestFocus();
            Point2D.Double coordinate = parsingTool.getLowestVertex(visualCpog);


            if (PGF.getGraphName() != null) {
                inserted = insertAsPage(visualCpog, PGF, coordinate, editor);
            } else {
                insertLoose(visualCpog, coordinate);
            }

        }
            if (inserted != null) {
                String normalForm = getNormalForm(arcConditionList, localVertices);
                String graphName = PGF.getGraphName();
                graphName = graphName.replace("{", "");
                graphName = graphName.replace("}", "");
                LinkedHashSet<Node> roots = getRootNodes(visualCpog, localVertices.values());
                bfsLayout(visualCpog, roots);
                if (referenceMap.containsKey(graphName)) {
                	referenceMap.remove(graphName);
                }
                GraphReference g = new GraphReference(graphName, normalForm, (HashMap<String, VisualVertex>) localVertices.clone());
                g.addRefPage(inserted);
                referenceMap.put(graphName, g);
                if (PGF.getRef()) {
                    visualCpog.remove(visualCpog.getSelection());
                }
            }

        Collection<Node> prevSelection = visualCpog.getSelection();

        we.saveMemento();

        visualCpog.selectNone();

        editor.requestFocus();

        editor.forceRedraw();
        //Doesn't allow zoomFit when creating a new CPOG model
        //Such as when extracting concurrency
        if (zoomFit) {
        editor.getMainWindow().zoomFit();
        }

        visualCpog.select(prevSelection);



        return null;
    }
	}

    public String getNormalForm(HashSet<ArcCondition> arcConditionList, HashMap<String, VisualVertex> localVertices) {
        String normalForm = "";
        Collection<VisualVertex> verts = localVertices.values();
        Iterator<VisualVertex> it = verts.iterator();
        VisualVertex v;
        while (it.hasNext()) {
            v = it.next();
            if (FormulaToString.toString(v.getCondition()).compareTo("1") == 0) {
                normalForm = normalForm + v.getLabel();
            } else {
                normalForm = normalForm + "[" + FormulaToString.toString(v.getCondition()) + "]" + v.getLabel();
            }
            if (it.hasNext()) {
                normalForm = normalForm + " + ";
            }
        }

        Iterator<ArcCondition> it1 = arcConditionList.iterator();
        ArcCondition ac;
        if (!arcConditionList.isEmpty()) {
            normalForm = normalForm + " + ";
        }
        while (it1.hasNext()) {
            ac = it1.next();
            if (ac.getBoolForm().compareTo("") == 0) {
                normalForm = normalForm + " " + CpogFormulaToString.toString(ac.getFormula());
            } else {
                normalForm = normalForm + "[" + ac.getBoolForm() + "](" + CpogFormulaToString.toString(ac.getFormula()) + ")";
            }
            if (it1.hasNext()) {
                normalForm = normalForm + " + ";
            }
        }
        return normalForm;
    }

    public void insertLoose(VisualCPOG visualCpog, Double coordinate) {
        for (Node node : visualCpog.getSelection())
        {
            if (node instanceof VisualVertex)
            {
                VisualVertex v = (VisualVertex) node;
                v.setPosition(new Double(v.getX(), v.getY() + coordinate.getY()));
            }
        }
    }

    public VisualPage insertAsPage(VisualCPOG visualCpog, GraphFunc<String, CpogFormula> PGF, Double coordinate, GraphEditor editor) {
        HashSet<VisualScenarioPage> pageList = new HashSet<>();
        for (Node n0 : visualCpog.getSelection()) {
            if (n0 instanceof VisualScenarioPage) {
                pageList.add((VisualScenarioPage) n0);
            }
        }

        PageNode pageNode = new PageNode();
        visualCpog.getMathModel().add(pageNode);
        VisualScenarioPage page = new VisualScenarioPage(pageNode);
        visualCpog.getCurrentLevel().add(page);
        includeArcsInPage(visualCpog);

        Container container = visualCpog.getCurrentLevel();
        HashSet<Node> nodes = new HashSet<>();
        for (Node n : visualCpog.getSelection()) {
            if (!(n.getParent().equals(container))) {
                visualCpog.reparent(page, visualCpog, container, nodes);
                container = (Container) n.getParent();
                nodes.clear();
            }
            nodes.add(n);
        }

        visualCpog.reparent(page, visualCpog, container, nodes);
        for (VisualComponent c : page.getComponents()) {
            if (c instanceof VisualPage) {
                if (c.getComponents().isEmpty()) {
                    visualCpog.remove(c);
                }
            }
        }
        visualCpog.select(page);

        page.setLabel(PGF.getGraphName());

        coordinate.setLocation(coordinate.getX(), coordinate.getY() + (page.getBoundingBox().getHeight() / 2) + 1);
        page.setPosition(coordinate);

        attatchRefEventHandler(visualCpog, page, editor);

        return page;

    }

    public void bfsLayout(VisualCPOG visualCpog, LinkedHashSet<Node> roots) {
        Iterator<Node> root = roots.iterator();
        ConcurrentLinkedQueue<Node> q = new ConcurrentLinkedQueue<Node>();
        double originalX = 0, originalY = 0;
        while(root.hasNext()) {
            q.add(root.next());
            parsingTool.bfsLayout(q, visualCpog, originalX, originalY);
            originalY += 2.5;
        }
    }

    public void noRootLayout(LinkedHashMap<String, VisualVertex> vertexMap, int n, int i) {
        double y = maxY + 2.5;
        for (VisualVertex v : vertexMap.values()) {
            double radius = Math.max(minRadius, expandRadius * n / Math.PI
                    / 2.0);
            Double pos = new Double(maxX + radius
                    * Math.cos(2.0 * Math.PI * i / n), y + radius * Math.sin(2.0 * Math.PI * i / n));
            v.setPosition(pos);
            if (pos.y > highestY) {
                highestY = pos.y;
            }
            i++;
        }
    }

    @Override
	public void mouseClicked(GraphEditorMouseEvent e) {
		boolean processed = false;

		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
			VisualModel model = e.getEditor().getModel();
			VisualNode node = (VisualNode) HitMan.hitTestForSelection(
					e.getPosition(), model);
			if (node != null) {
				if (node instanceof VisualVariable) {
					VisualVariable var = (VisualVariable) node;
					var.toggle();
					processed = true;
				}
			}
		}

		if (!processed) {
			super.mouseClicked(e);

		}
	}

    @Override
    public void mouseReleased(GraphEditorMouseEvent e) {
        super.mouseReleased(e);

    WorkspaceEntry we = e.getEditor().getWorkspaceEntry();
    final VisualCPOG visualCpog = (VisualCPOG) we.getModelEntry().getVisualModel();

        for (Node n : visualCpog.getSelection()) {
            if (n instanceof VisualVertex) {
                ((VisualVertex) n).sendNotification(new PropertyChangedEvent(n, "position"));
            }
        }
    }


    public void startDrag(GraphEditorMouseEvent e){
        super.startDrag(e);
        WorkspaceEntry we = e.getEditor().getWorkspaceEntry();
        final VisualCPOG visualCpog = (VisualCPOG) we.getModelEntry().getVisualModel();

        prevPoints.clear();
        for (Node n : visualCpog.getSelection()) {
            if (n instanceof VisualVertex) {
                VisualVertex v = (VisualVertex) n;
                prevPoints.put(v.getLabel(), new Point2D.Double(v.getPosition().getX(), v.getPosition().getY()));
            }
        }
    }


    public LinkedHashSet<Node> getRootNodes(VisualCPOG visualCpog, Collection<VisualVertex> vertexMap) {
        LinkedHashSet<Node> roots = new LinkedHashSet<Node>();
        Set<Connection> arcs;
        Iterator<Connection> it;
        Connection connection;
        boolean second = false;
        for (Node node : vertexMap) {
            arcs = visualCpog.getConnections(node);
            it = arcs.iterator();
            //The following covers root nodes, and nodes with no connections
            while (it.hasNext()) {
                connection = it.next();
                if (!connection.getFirst().equals(node)) {
                    second = true;
                    break;
                }
            }
            if (!second) {
                roots.add(node);
            }
            second = false;
        }
        return roots;
    }

    public void addUsedReferences(VisualCPOG visualCpog, GraphEditor editor, ArrayList<String> usedReferences, HashMap<String, VisualVertex> localVertices, ArrayList<Node> prevSelection) {
        for (String k : usedReferences) {
            visualCpog.selectNone();
            ArrayList<VisualVertex> pageVerts = new ArrayList<VisualVertex>();
            if (referenceMap.containsKey(k)) {
                GraphReference g = referenceMap.get(k);
                HashMap<String, VisualVertex> vMap = g.getVertMap();
                for(String k1 : vMap.keySet()) {
                    localVertices.get(k1).setPosition(new Point2D.Double(vMap.get(k1).getX(),vMap.get(k1).getY()));
                    pageVerts.add(localVertices.get(k1));
                    if (visualCpog.getVertices(visualCpog.getCurrentLevel()).contains(localVertices.get(k1))) {
                        visualCpog.add(localVertices.get(k1));
                    }
                    visualCpog.addToSelection(localVertices.get(k1));
                }
                prevSelection.removeAll(pageVerts);
                includeArcsInPage(visualCpog);
                selectionPageGroup(editor);
                if (visualCpog.getSelection().size() == 1)
                {
                    for (Node n1 : visualCpog.getSelection()) {
                        if (n1 instanceof VisualPage) {
                            VisualPage vp = (VisualPage) n1;
                            vp.setLabel(k);
                            vp.setIsCollapsed(false);
                            vp.setParent(visualCpog.getCurrentLevel());
                            prevSelection.add(vp);
                            referenceMap.get(k).addRefPage(vp);
                            refPages.add(vp);
                        }
                    }
                }
            }
            visualCpog.addToSelection(prevSelection);
        }
    }

    public void attatchRefEventHandler(final VisualCPOG visualCpog, final Container page, final GraphEditor editor) {
        new HierarchySupervisor() {

        	ArrayList<Node> toBeRemoved = new ArrayList<>();
        	String refKey = "";
            @Override
            public void handleEvent(HierarchyEvent e) {
                ArrayList<VisualPage> relaventPages = new ArrayList<>();
                if (e instanceof NodesDeletingEvent) {
                    for (Node node : e.getAffectedNodes()) {
                        if (node instanceof VisualVertex) {
                            final VisualVertex vert = (VisualVertex) node;
                            if (!(vert.getParent() instanceof VisualScenarioPage)) {
                                if (vert.getParent() instanceof VisualPage) {
                                    VisualPage page = (VisualPage) vert.getParent();
                                    refKey = page.getLabel();
                                    relaventPages.addAll(referenceMap.get(page.getLabel()).getRefPages());
                                    relaventPages.remove(page);
                                    for (VisualPage p : relaventPages) {
                                        for (Node n : p.getChildren()) {
                                            if (n instanceof VisualVertex) {
                                                VisualVertex v = (VisualVertex) n;
                                                if (v.getLabel().compareTo(vert.getLabel()) == 0) {
                                                    if (!(e.getAffectedNodes().contains(v))) {
                                                        toBeRemoved.add(v);
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            CPOGHangingConnectionRemover arcRemover = new CPOGHangingConnectionRemover(visualCpog);
                            for (Node n : toBeRemoved) {
                                arcRemover.handleEvent(new NodesDeletingEvent(n.getParent(), n));
                                visualCpog.removeWithoutNotify(n);
                            }

                        } else if (node instanceof VisualArc) {
                            VisualArc a = (VisualArc) node;
                            if ((a.getFirst().getParent().equals(a.getSecond().getParent()))) {
                                if ((a.getFirst().getParent() instanceof VisualPage) || (a.getFirst().getParent() instanceof VisualScenarioPage)) {
                                    VisualPage vp = (VisualPage) a.getFirst().getParent();
                                    String first = ((VisualVertex)a.getFirst()).getLabel();
                                    String second = ((VisualVertex)a.getSecond()).getLabel();
                                    refKey = vp.getLabel();
                                    relaventPages.addAll(referenceMap.get(refKey).getRefPages());
                                    relaventPages.remove(vp);
                                    for (VisualPage p : relaventPages) {
                                        for (Node n : p.getChildren()) {
                                            if (n instanceof VisualVertex) {
                                                VisualVertex f = (VisualVertex) n;
                                                if (f.getLabel().equals(first)) {
                                                    for (Node n1 : p.getChildren())
                                                    {
                                                        if (n1 instanceof VisualVertex) {
                                                            VisualVertex s = (VisualVertex) n1;
                                                            if (s.getLabel().equals(second)){
                                                                Connection c = visualCpog.getConnection(f, s);
                                                                if (!(e.getAffectedNodes().contains(c))) {
                                                                    toBeRemoved.add(c);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            for (Node n : toBeRemoved) {
                                visualCpog.removeFromSelection(n);
                                visualCpog.removeWithoutNotify(n);
                                while (n.getParent() != null) {
                                	try {
										this.wait(5);
									} catch (InterruptedException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
                                }
                            }
                        }

                    }
                } else if (e instanceof NodesAddedEvent) {
                    for (Node node : e.getAffectedNodes()) {
                        if (node instanceof VisualArc) {
                        }
                    }
                }

                toBeRemoved.clear();
                //updateReferenceNormalForm(relaventPages, visualCpog, editor);
            }

        }.attach(page);

        final class StateSupervisorExtension extends StateSupervisor {
            @Override
            public void handleEvent(StateEvent e) {
                if (e instanceof PropertyChangedEvent) {
                    PropertyChangedEvent pce = (PropertyChangedEvent) e;
                    if (((PropertyChangedEvent) e).getPropertyName().compareTo("position") == 0)
                    {
                        if ((pce.getSender() instanceof VisualVertex) && !(pce.getSender().getParent() instanceof VisualScenarioPage)) {
                            VisualVertex v = (VisualVertex) pce.getSender();
                            double xDiff = 0;
                            double yDiff = 0;
                            if (prevPoints.get(v.getLabel()) != null) {
                                xDiff = v.getPosition().getX() - prevPoints.get(v.getLabel()).getX();
                                yDiff = v.getPosition().getY() - prevPoints.get(v.getLabel()).getY();
                                prevPoints.remove(v.getLabel());
                            }

                            if (v.getParent() instanceof VisualPage) {
                                VisualPage page = (VisualPage) v.getParent();

                                String refKey = page.getLabel();

                                GraphReference g = referenceMap.get(page.getLabel());
                                if (g != null) {
                                    g.updateVertexPosition(v.getLabel(), xDiff, yDiff);

                                    ArrayList<VisualPage> refPages = getRefPages(visualCpog, refKey, v);
                                    refPages.remove(page);

                                    for (VisualPage p : refPages) {
                                        for (Node n : p.getChildren()) {
                                            if ((n instanceof VisualVertex) && (((VisualVertex) n).getLabel().compareTo(v.getLabel()) == 0)) {
                                                VisualVertex vert = (VisualVertex) n;
                                                vert.setPosition(new Point2D.Double(vert.getPosition().getX() + xDiff, vert.getPosition().getY() + yDiff));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        new StateSupervisorExtension().attach(page);
    }


    public ArrayList<VisualPage> getRefPages(VisualCPOG visualCpog, String refKey, VisualVertex v) {
        ArrayList<VisualPage> result = new ArrayList<VisualPage>();


        for (VisualPage p : refPages) {
        	if (p.getLabel().compareTo(refKey) == 0) {
        		result.add(p);

        	}
        }
        return result;
    }

    public void updateReferenceNormalForm(ArrayList<VisualPage> relaventPages, VisualCPOG visualCpog, GraphEditor editor) {
        if (relaventPages.size() > 0) {
            Container previousLevel = visualCpog.getCurrentLevel();
            Collection<Node> selection = visualCpog.getSelection();

            VisualPage page = relaventPages.get(0);
            visualCpog.setCurrentLevel((Container) page.getParent());
            visualCpog.select(page);

            String newExpression = parsingTool.getExpressionFromGraph(visualCpog);
            newExpression = newExpression.replace("\n", "");
            while (newExpression.startsWith(" ")) {
                newExpression = newExpression.substring(1);
            }
            while (newExpression.endsWith(" ")) {
                newExpression = newExpression.substring(0, newExpression.length() - 1);
            }

            GraphReference g = referenceMap.get(page.getLabel());

            int eqLocation;
            eqLocation = newExpression.indexOf('=');
            g.updateNormalForm(newExpression.substring(eqLocation + 1));

            HashMap<String, VisualVertex> vertMap = (HashMap<String, VisualVertex>) insertExpression(newExpression, true, true, true, false).clone();
            for (VisualVertex v : vertMap.values()) {
                Point2D.Double newPosition = new Point2D.Double(g.getVertMap().get(v.getLabel()).getX(), g.getVertMap().get(v.getLabel()).getY());
                v.setPosition(newPosition);
            }

            g.updateVertMap(vertMap);

            visualCpog.setCurrentLevel(previousLevel);
            visualCpog.select(selection);
        }
    }

    public void includeArcsInPage(VisualCPOG visualCpog) {
        //VisualCPOG visualCpog = (VisualCPOG) editor.getWorkspaceEntry().getModelEntry().getVisualModel();
        HashSet<Node> arcs = new HashSet<>();
        for (Node n : visualCpog.getSelection()) {
            for (Connection c : visualCpog.getConnections(n)) {
                arcs.add(c);
            }
        }
        visualCpog.addToSelection(arcs);
    }

    public void insertEventLog(VisualCPOG visualCpog, int i, String[] events, double yPos) {

    	final LinkedHashMap<String, VisualVertex> vertexMap = new LinkedHashMap<String, VisualVertex>();
    	VisualVertex vertex1 , vertex2;

    	for (int c = 0; c < events.length - 1; c++) {
    		String first = "";
    		String second = "";

    		first = events[c];
    		second = events[c+1];

    		if (vertexMap.containsKey(first)) {
    			vertex1 = vertexMap.get(first);
    		} else {
    			vertex1 = visualCpog.createVisualVertex(visualCpog.getCurrentLevel());
                vertex1.setLabel(first);
                vertexMap.put(first, vertex1);
    		}


    		if (vertexMap.containsKey(second)) {
    			int d = 1;
    			while (vertexMap.containsKey(second + "_" + d)) {
    				d++;
    			}
    			vertex2 = visualCpog.createVisualVertex(visualCpog.getCurrentLevel());
                vertex2.setLabel(second + "_" + d);
                vertexMap.put(second + "_" + d, vertex2);

                events[c+1] = second + "_" + d;
    		} else {
    			vertex2 = visualCpog.createVisualVertex(visualCpog.getCurrentLevel());
                vertex2.setLabel(second);
                vertexMap.put(second, vertex2);
    		}

    		visualCpog.connect(vertex1, vertex2);

    	}

    	double xPos = vertexMap.values().size();
    	xPos = xPos*2.5;
    	xPos = 0 - xPos/2;


    	PageNode pageNode = new PageNode();
        visualCpog.getMathModel().add(pageNode);
        VisualScenarioPage page = new VisualScenarioPage(pageNode);
        visualCpog.getCurrentLevel().add(page);
        page.setLabel("t" + i);

        Container container = visualCpog.getCurrentLevel();
        HashSet<Node> nodes = new HashSet<>();


    	visualCpog.selectNone();
    	for (VisualVertex v : vertexMap.values()) {
    		v.setPosition(new Point2D.Double(xPos, yPos));
    		xPos = xPos + 2.5;
    		nodes.add(v);
    	}

    	visualCpog.reparent(page, visualCpog, container, nodes);
    	includeArcsInPage(visualCpog);


    }



}
