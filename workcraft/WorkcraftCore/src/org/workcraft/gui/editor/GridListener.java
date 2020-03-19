package org.workcraft.gui.editor;

/**
 * The <code>GridListener</code> interface defines methods for classes that wish
 * to be notified of the changes in grid parameters.
 */
public interface GridListener {
    /**
     * The grid parameters (such as number of visible lines, major and minor line intervals, etc.) have changed.
     * @param sender
     * The grid that sent the event.
     */
    void gridChanged(Grid sender);
}
