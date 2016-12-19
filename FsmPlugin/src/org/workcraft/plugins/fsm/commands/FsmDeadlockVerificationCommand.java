package org.workcraft.plugins.fsm.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JOptionPane;

import org.workcraft.Framework;
import org.workcraft.dom.references.ReferenceHelper;
import org.workcraft.dom.visual.SelectionHelper;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.graph.commands.AbstractVerificationCommand;
import org.workcraft.gui.graph.tools.SelectionTool;
import org.workcraft.plugins.fsm.Event;
import org.workcraft.plugins.fsm.Fsm;
import org.workcraft.plugins.fsm.State;
import org.workcraft.plugins.fsm.VisualFsm;
import org.workcraft.workspace.WorkspaceEntry;
import org.workcraft.workspace.WorkspaceUtils;

public class FsmDeadlockVerificationCommand extends AbstractVerificationCommand {

    private static final String TITLE = "Verification result";

    @Override
    public String getDisplayName() {
        return "Deadlock";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Fsm.class);
    }

    @Override
    public void run(WorkspaceEntry we) {
        final Framework framework = Framework.getInstance();
        final MainWindow mainWindow = framework.getMainWindow();
        final Fsm fsm = WorkspaceUtils.getAs(we, Fsm.class);
        HashSet<State> deadlockStates = checkDeadlock(fsm);
        if (deadlockStates.isEmpty()) {
            JOptionPane.showMessageDialog(mainWindow, "The model is deadlock-free.",
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
        } else {
            HashSet<State> finalDeadlockStates = new HashSet<>();
            for (State state: deadlockStates) {
                if (state.isFinal()) {
                    finalDeadlockStates.add(state);
                }
            }
            deadlockStates.removeAll(finalDeadlockStates);
            String message = "The model has a deadlock.";
            if (!deadlockStates.isEmpty()) {
                String stateStr = ReferenceHelper.getNodesAsString(fsm, (Collection) deadlockStates);
                message += "\n\nNon-final deadlock states: \n" + stateStr;
            }
            if (!finalDeadlockStates.isEmpty()) {
                String stateStr = ReferenceHelper.getNodesAsString(fsm, (Collection) finalDeadlockStates);
                message += "\n\nFinal deadlock states: \n" + stateStr;
            }
            message += "\n\nSelect deadlock states?\n";
            if (JOptionPane.showConfirmDialog(mainWindow, message,
                    TITLE, JOptionPane.WARNING_MESSAGE + JOptionPane.YES_NO_OPTION) == 0) {

                VisualFsm visualFsm = (VisualFsm) we.getModelEntry().getVisualModel();
                mainWindow.getToolbox(we).selectToolInstance(SelectionTool.class);
                SelectionHelper.selectByReferencedComponents(visualFsm, (HashSet) deadlockStates);
            }
        }
    }

    private HashSet<State> checkDeadlock(final Fsm fsm) {
        HashSet<State> deadlockStates = new HashSet<>();
        HashMap<State, HashSet<Event>> stateEvents = FsmUtils.calcStateOutgoingEventsMap(fsm);

        HashSet<State> visited = new HashSet<>();
        Queue<State> queue = new LinkedList<>();

        State initialState = fsm.getInitialState();
        if (initialState != null) {
            queue.add(initialState);
        }

        while (!queue.isEmpty()) {
            State curState = queue.remove();
            if (visited.contains(curState)) continue;
            visited.add(curState);
            for (Event curEvent: stateEvents.get(curState)) {
                State nextState = (State) curEvent.getSecond();
                if (nextState != null) {
                    queue.add(nextState);
                }
            }
            if (stateEvents.get(curState).isEmpty()) {
                deadlockStates.add(curState);
            }
        }
        return deadlockStates;
    }

}
