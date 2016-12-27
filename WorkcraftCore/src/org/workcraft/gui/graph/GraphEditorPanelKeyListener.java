package org.workcraft.gui.graph;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.workcraft.gui.DesktopApi;
import org.workcraft.gui.events.GraphEditorKeyEvent;
import org.workcraft.gui.graph.tools.GraphEditorKeyListener;

class GraphEditorPanelKeyListener implements KeyListener {
    GraphEditorPanel editor;
    GraphEditorKeyListener forwardListener;

    GraphEditorPanelKeyListener(GraphEditorPanel editor, GraphEditorKeyListener forwardListener) {
        this.editor = editor;
        this.forwardListener = forwardListener;
    }

    public void keyPressed(KeyEvent e) {
        if (DesktopApi.isMenuKeyDown(e)) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                editor.panLeft();
                break;
            case KeyEvent.VK_UP:
                editor.panUp();
                break;
            case KeyEvent.VK_RIGHT:
                editor.panRight();
                break;
            case KeyEvent.VK_DOWN:
                editor.panDown();
                break;
            }
        } else {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_EQUALS:
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_ADD:
                editor.zoomIn();
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
            case KeyEvent.VK_SUBTRACT:
                editor.zoomOut();
                break;
            case KeyEvent.VK_MULTIPLY:
                editor.zoomFit();
                break;
            case KeyEvent.VK_DIVIDE:
                editor.panCenter();
                break;
            }
        }
        GraphEditorKeyEvent geke = new GraphEditorKeyEvent(editor, e);
        forwardListener.keyPressed(geke);

    }

    public void keyReleased(KeyEvent e) {
        GraphEditorKeyEvent geke = new GraphEditorKeyEvent(editor, e);
        forwardListener.keyReleased(geke);

    }

    public void keyTyped(KeyEvent e) {
        GraphEditorKeyEvent geke = new GraphEditorKeyEvent(editor, e);
        forwardListener.keyTyped(geke);
    }
}
