package org.workcraft.plugins.cpog;

import org.workcraft.annotations.DisplayName;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.PageNode;
import org.workcraft.dom.visual.*;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.exceptions.NodeCreationException;
import org.workcraft.formula.jj.BooleanFormulaParser;
import org.workcraft.formula.jj.ParseException;
import org.workcraft.formula.utils.StringGenerator;
import org.workcraft.gui.graph.generators.DefaultNodeGenerator;
import org.workcraft.gui.graph.tools.CommentGeneratorTool;
import org.workcraft.gui.graph.tools.ConnectionTool;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.gui.graph.tools.NodeGeneratorTool;
import org.workcraft.gui.propertyeditor.ModelProperties;
import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.cpog.tools.CpogSelectionTool;
import org.workcraft.util.Hierarchy;

import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@DisplayName("Conditional Partial Order Graph")
public class VisualCpog extends AbstractVisualModel {

    private final class BooleanFormulaPropertyDescriptor implements PropertyDescriptor {
        private final Node node;

        private BooleanFormulaPropertyDescriptor(Node node) {
            this.node = node;
        }

        @Override
        public Map<Object, String> getChoice() {
            return null;
        }

        @Override
        public String getName() {
            if (node instanceof VisualRhoClause) return "Function";
            return "Condition";
        }

        @Override
        public Class<?> getType() {
            return String.class;
        }

        @Override
        public Object getValue() {
            if (node instanceof VisualRhoClause) return StringGenerator.toString(((VisualRhoClause) node).getFormula());
            if (node instanceof VisualVertex) return StringGenerator.toString(((VisualVertex) node).getCondition());
            return StringGenerator.toString(((VisualArc) node).getCondition());
        }

        @Override
        public void setValue(Object value) throws InvocationTargetException {
            try {
                if (node instanceof VisualRhoClause) {
                    ((VisualRhoClause) node).setFormula(BooleanFormulaParser.parse((String) value, mathModel.getVariables()));
                } else if (node instanceof VisualArc) {
                    ((VisualArc) node).setCondition(BooleanFormulaParser.parse((String) value, mathModel.getVariables()));
                } else if (node instanceof VisualVertex) {
                    ((VisualVertex) node).setCondition(BooleanFormulaParser.parse((String) value, mathModel.getVariables()));
                }
            } catch (ParseException e) {
                throw new InvocationTargetException(e);
            }
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public boolean isCombinable() {
            return true;
        }

        @Override
        public boolean isTemplatable() {
            return true;
        }
    }

    private Cpog mathModel;

    public VisualCpog(Cpog model) {
        this(model, null);
    }

    public VisualCpog(Cpog model, VisualGroup root) {
        super(model, root);
        this.mathModel = model;
        setGraphEditorTools();
        if (root == null) {
            try {
                createDefaultFlatStructure();
            } catch (NodeCreationException e) {
                throw new RuntimeException(e);
            }
        }
        new ConsistencyEnforcer(this).attach(getRoot());
    }

    private void setGraphEditorTools() {
        List<GraphEditorTool> tools = new ArrayList<>();
        tools.add(new CpogSelectionTool());
        tools.add(new CommentGeneratorTool());
        tools.add(new ConnectionTool(false, true, true));
        tools.add(new NodeGeneratorTool(new DefaultNodeGenerator(Vertex.class)));
        tools.add(new NodeGeneratorTool(new DefaultNodeGenerator(Variable.class)));
        tools.add(new NodeGeneratorTool(new DefaultNodeGenerator(RhoClause.class)));
        setGraphEditorTools(tools);
    }

    @Override
    public void validateConnection(Node first, Node second) throws InvalidConnectionException {
        if (first instanceof VisualVariable && !getPreset(first).isEmpty()) {
            throw new InvalidConnectionException("Variables do not support multiple connections.");
        }
        if (second instanceof VisualVariable && !getPreset(second).isEmpty()) {
            throw new InvalidConnectionException("Variables do not support multiple connections.");
        }

        if ((first instanceof VisualVertex) && (second instanceof VisualVertex)) return;
        if ((first instanceof VisualVertex) && (second instanceof VisualVariable)) return;
        if ((first instanceof VisualVariable) && (second instanceof VisualVertex)) return;

        throw new InvalidConnectionException("Invalid connection.");
    }

    @Override
    public VisualConnection connect(Node first, Node second, MathConnection mConnection) throws InvalidConnectionException {
        validateConnection(first, second);
        VisualConnection ret = null;
        if ((first instanceof VisualVertex) && (second instanceof VisualVertex)) {
            VisualVertex v = (VisualVertex) first;
            VisualVertex u = (VisualVertex) second;
            ret = connect(v, u);
        } else {
            VisualVertex v;
            VisualVariable u;
            if (first instanceof VisualVertex) {
                v = (VisualVertex) first;
                u = (VisualVariable) second;
            } else {
                v = (VisualVertex) second;
                u = (VisualVariable) first;
            }
            if (mConnection == null) {
                mConnection = mathModel.connect(v.getMathVertex(), u.getMathVariable());
            }
            ret = new VisualDynamicVariableConnection((DynamicVariableConnection) mConnection, v, u);
            Hierarchy.getNearestContainer(v, u).add(ret);
        }
        return ret;
    }

    public VisualArc connect(VisualVertex v, VisualVertex u) {
        Arc con = mathModel.connect(v.getMathVertex(), u.getMathVertex());
        VisualArc arc = new VisualArc(con, v, u);
        Hierarchy.getNearestContainer(v, u).add(arc);
        return arc;
    }

    @Override
    public boolean isGroupable(Node node) {
        return (node instanceof VisualVertex) || (node instanceof VisualVariable);
    }

    @Override
    public VisualGroup groupSelection() {
        return groupSelection(null);
    }

    public VisualScenario groupSelection(String graphName) {
        VisualScenario scenario = null;
        Collection<Node> nodes = SelectionHelper.getGroupableCurrentLevelSelection(this);
        if (nodes.size() >= 1) {
            scenario = new VisualScenario();
            if (graphName != null) {
                scenario.setLabel(graphName);
            }
            getCurrentLevel().add(scenario);
            getCurrentLevel().reparent(nodes, scenario);
            Point2D centre = TransformHelper.getSnappedCentre(nodes);
            VisualModelTransformer.translateNodes(nodes, -centre.getX(), -centre.getY());
            scenario.setPosition(centre);
            select(scenario);
        }
        return scenario;
    }

    // TODO: Add safe versions of these methods; see getVertices(Container root).
    @Deprecated
    public Collection<VisualScenario> getGroups() {
        return Hierarchy.getChildrenOfType(getRoot(), VisualScenario.class);
    }

    @Deprecated
    public Collection<VisualVariable> getVariables() {
        return Hierarchy.getChildrenOfType(getRoot(), VisualVariable.class);
    }

    @Deprecated
    public Collection<VisualVertex> getVertices() {
        return Hierarchy.getChildrenOfType(getRoot(), VisualVertex.class);
    }

    public Collection<VisualVertex> getVertices(Container root) {
        return Hierarchy.getChildrenOfType(root, VisualVertex.class);
    }

    public Collection<VisualVariable> getVariables(Container root) {
        return Hierarchy.getChildrenOfType(root, VisualVariable.class);
    }

    public Collection<VisualArc> getArcs(Container root) {
        return Hierarchy.getChildrenOfType(root, VisualArc.class);
    }

    public VisualVertex createVisualVertex(Container container) {
        Vertex mathVertex = new Vertex();
        mathModel.add(mathVertex);

        VisualVertex vertex = new VisualVertex(mathVertex);
        container.add(vertex);
        return vertex;
    }

    public VisualVariable createVisualVariable() {
        Variable mathVariable = new Variable();
        mathModel.add(mathVariable);

        VisualVariable variable = new VisualVariable(mathVariable);

        getRoot().add(variable);

        return variable;
    }

    public VisualScenario createVisualScenario() {
        VisualScenario scenario = new VisualScenario();
        getRoot().add(scenario);
        return scenario;
    }

    @Override
    public ModelProperties getProperties(Node node) {
        ModelProperties properties = super.getProperties(node);
        if (node != null) {
            if (node instanceof VisualRhoClause ||
                    node instanceof VisualVertex ||
                    node instanceof VisualArc) {
                properties.add(new BooleanFormulaPropertyDescriptor(node));
            }
        }
        return properties;
    }

    public void removeWithoutNotify(Node node) {
        if (node.getParent() instanceof VisualPage) {
            ((VisualPage) node.getParent()).removeWithoutNotify(node);
        } else if (node.getParent() instanceof VisualGroup) {
            ((VisualGroup) node.getParent()).removeWithoutNotify(node);
        }
    }

    public VisualScenarioPage groupScenarioPageSelection(String graphName) {
        VisualScenarioPage scenario = null;
        PageNode pageNode = new PageNode();
        Collection<Node> nodes = SelectionHelper.getGroupableCurrentLevelSelection(this);
        if (nodes.size() >= 1) {
            scenario = new VisualScenarioPage(pageNode);
            if (graphName != null) {
                scenario.setLabel(graphName);
            }
            getCurrentLevel().add(scenario);
            getCurrentLevel().reparent(nodes, scenario);
            Point2D centre = TransformHelper.getSnappedCentre(nodes);
            VisualModelTransformer.translateNodes(nodes, -centre.getX(), -centre.getY());
            scenario.setPosition(centre);
            select(scenario);
        }
        return scenario;

    }

}
