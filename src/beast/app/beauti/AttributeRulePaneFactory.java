/*
 * AttributeRulePaneFactory.java
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

import beast.xml.AttributeRule;
import beast.xml.XMLObject;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * @author Arman Bilge
 */
final class AttributeRulePaneFactory extends RulePaneFactory<AttributeRule> {

    @Override
    protected Class<AttributeRule> getRuleType() {
        return AttributeRule.class;
    }

    @Override
    protected RulePane createPane(final XMLObject xo, final AttributeRule rule) {
        return new AttributeRulePane(xo, rule);
    }

    private static final class AttributeRulePane extends RulePane<AttributeRule> {
        protected AttributeRulePane(final XMLObject xo, final AttributeRule rule) {

            super(xo, rule);
            int col = 0;

            final CheckBox cb = new CheckBox();
            add(cb, col++, 0);
            cb.setVisible(rule.getOptional());

            final Label name = new Label(rule.getName() + ": ");
            add(name, col++, 0);
            name.setTooltip(new Tooltip(rule.getDescription()));

            final TextField value = new TextField();
            add(value, col++, 0);
            cb.selectedProperty().addListener((ov, oldValue, newValue) -> {
                value.setDisable(!newValue);
            });
            value.setDisable(rule.getOptional());

        }
    }
}
