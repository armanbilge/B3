/*
 * ParserFrame.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.app.beauti;

import beast.xml.XMLObject;
import beast.xml.XMLSyntaxRule;

import javax.swing.JPanel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arman Bilge
 */
public abstract class RulePanel<R extends XMLSyntaxRule> extends JPanel {

    private final XMLObject xo;
    private final R rule;
    private final List<RulePanel<?>> panels = new ArrayList<>();

    protected RulePanel(final XMLObject xo, final R rule) {
        this.xo = xo;
        this.rule = rule;
    }

    public XMLObject getXMLObject() {
        return xo;
    }

    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        for (final Component comp : getComponents())
            comp.setEnabled(enabled);
    }

    protected RulePanel<?> getPanel(final int i) {
        return panels.get(i);
    }

    protected int getPanelCount() {
        return panels.size();
    }

    public Component add(final Component comp) {
        if (comp instanceof RulePanel<?>)
            panels.add((RulePanel<?>) comp);
        return super.add(comp);
    }

}
