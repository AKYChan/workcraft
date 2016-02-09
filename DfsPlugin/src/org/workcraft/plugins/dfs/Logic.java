/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.dfs;

import org.workcraft.annotations.VisualClass;
import org.workcraft.observation.PropertyChangedEvent;

@VisualClass(org.workcraft.plugins.dfs.VisualLogic.class)
public class Logic extends MathDelayNode {
    public static final String PROPERTY_EARLY_EVALUATION = "Early evaluation";
    public static final String PROPERTY_COMPUTED = "Computed";

    private boolean computed = false;
    private boolean earlyEvaluation = false;

    public boolean isComputed() {
        return computed;
    }

    public void setComputed(boolean value) {
        this.computed = value;
        sendNotification(new PropertyChangedEvent(this, PROPERTY_COMPUTED));
    }

    public boolean isEarlyEvaluation() {
        return earlyEvaluation;
    }

    public void setEarlyEvaluation(boolean value) {
        this.earlyEvaluation = value;
        sendNotification(new PropertyChangedEvent(this, PROPERTY_EARLY_EVALUATION));
    }

}
