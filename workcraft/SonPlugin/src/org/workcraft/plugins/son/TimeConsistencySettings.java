package org.workcraft.plugins.son;

import java.util.ArrayList;

import org.workcraft.dom.Node;
import org.workcraft.plugins.son.gui.TimeConsistencyDialog.Granularity;
import org.workcraft.plugins.son.util.Interval;
import org.workcraft.plugins.son.util.ScenarioRef;

public class TimeConsistencySettings {

    private final boolean inconsistencyHighlight;
    private final boolean unspecifyHighlight;
    private final boolean causalConsistency;
    private final boolean causalHighlight;
    private final ArrayList<ONGroup> selectedGroups;
    private final ScenarioRef seletedScenario;
    private final ArrayList<Node> seletedNodes;
    private final int tabIndex;
    private final Granularity granularity;
    private final Interval defaultDuration;

    public TimeConsistencySettings(boolean inconsistencyHighlight,
            boolean unspecifyHighlight, ArrayList<ONGroup> selectedGroups,
            ScenarioRef seletedScenario, ArrayList<Node> seletedNodes, int tabIndex,
            Granularity granularity, boolean causalConsistency,
            Interval defaultDuration, boolean causalHighlight) {
        this.inconsistencyHighlight = inconsistencyHighlight;
        this.unspecifyHighlight = unspecifyHighlight;
        this.selectedGroups = selectedGroups;
        this.seletedScenario = seletedScenario;
        this.seletedNodes = seletedNodes;
        this.tabIndex = tabIndex;
        this.granularity = granularity;
        this.causalConsistency = causalConsistency;
        this.defaultDuration = defaultDuration;
        this.causalHighlight = causalHighlight;
    }

    public boolean getInconsistencyHighlight() {
        return inconsistencyHighlight;
    }

    public boolean getUnspecifyHighlight() {
        return unspecifyHighlight;
    }

    public ArrayList<ONGroup> getSelectedGroups() {
        return selectedGroups;
    }

    public ScenarioRef getSeletedScenario() {
        return seletedScenario;
    }

    public ArrayList<Node> getSeletedNodes() {
        return seletedNodes;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public boolean isCausalConsistency() {
        if ((getTabIndex() == 0) && causalConsistency) {
            return true;
        } else {
            return false;
        }
    }

    public Interval getDefaultDuration() {
        return defaultDuration;
    }

    public boolean isCausalHighlight() {
        return causalHighlight;
    }
}
