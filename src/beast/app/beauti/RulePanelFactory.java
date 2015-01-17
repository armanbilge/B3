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

import javax.swing.JLabel;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Arman Bilge
 */
public abstract class RulePanelFactory<R extends XMLSyntaxRule> {

    protected abstract Class<R> getRuleType();

    protected abstract RulePanel<? super R> createPanel(XMLObject xo, R rule);

    private static final Map<Class<? extends XMLSyntaxRule>,RulePanelFactory> factories = new HashMap<>();

    public static <X extends XMLSyntaxRule> RulePanel<? super X> createRulePanel(final XMLObject xo, final X rule) {
        final RulePanelFactory factory = factories.get(rule.getClass());
        if (factory != null)
            return factory.createPanel(xo, rule);
        else
            return new UnsupportedRulePanel(xo, rule);
    }

    public static void registerFactory(final RulePanelFactory<?> factory) {
        factories.put(factory.getRuleType(), factory);
    }

    private static final class UnsupportedRulePanel extends RulePanel<XMLSyntaxRule> {
        private UnsupportedRulePanel(final XMLObject xo, final XMLSyntaxRule rule) {
            super(xo, rule);
            setLayout(new FlowLayout());
            add(new JLabel("Rule of type " + rule.getClass() + " is currently unsupported!"));
        }
    }

    static {
        registerFactory(new AndRulePanelFactory());
        // TODO: Register all built-in factories here
    }

}
