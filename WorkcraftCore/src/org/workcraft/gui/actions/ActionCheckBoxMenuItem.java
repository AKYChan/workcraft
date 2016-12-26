package org.workcraft.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JCheckBoxMenuItem;

@SuppressWarnings("serial")
public class ActionCheckBoxMenuItem extends JCheckBoxMenuItem implements Actor {
    class ActionForwarder implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ActionCheckBoxMenuItem.this.fireActionPerformed();
        }
    }

    private final LinkedList<ScriptedActionListener> listeners = new LinkedList<>();
    private Action scriptedAction = null;

    public ActionCheckBoxMenuItem(Action action) {
        this(action, action.getText());
    }

    public ActionCheckBoxMenuItem(Action action, String text) {
        super(text);
        scriptedAction = action;
        scriptedAction.addActor(this);
        setEnabled(scriptedAction.isEnabled());

        addActionListener(new ActionForwarder());
    }

    private void fireActionPerformed() {
        if (scriptedAction != null) {
            for (ScriptedActionListener l : listeners) {
                l.actionPerformed(scriptedAction);
            }
        }
    }

    public void addScriptedActionListener(ScriptedActionListener listener) {
        listeners.add(listener);
    }

    public void removeScriptedActionListener(ScriptedActionListener listener) {
        listeners.remove(listener);
    }

    public void actionEnableStateChanged(boolean actionEnableState) {
        this.setEnabled(actionEnableState);
    }
}
