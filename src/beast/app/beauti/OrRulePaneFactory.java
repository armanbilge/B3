/*
 * OrRulePaneFactory.java
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

import beast.xml.OrRule;
import beast.xml.XMLObject;
import beast.xml.XMLSyntaxRule;
import javafx.scene.control.CheckBox;

/**
 * @author Arman Bilge
 */
final class OrRulePaneFactory extends RulePaneFactory<OrRule> {

    @Override
    protected Class<OrRule> getRuleType() {
        return OrRule.class;
    }

    @Override
    protected RulePane createPane(final XMLObject xo, final OrRule rule) {
        return new OrRulePane(xo, rule);
    }

    private static final class OrRulePane extends RulePane<OrRule> {
        protected OrRulePane(final XMLObject xo, final OrRule rule) {
            super(xo, rule);
            int row = 0;
            for (final XMLSyntaxRule r : rule.getRules()) {
                final CheckBox cb = new CheckBox();
                final RulePane<?> rp = createRulePane(xo, r);
                cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
                    rp.setDisable(!newValue);
                });
                add(cb, 0, row);
                add(rp, 1, row);
                ++row;
            }
        }
    }
}
