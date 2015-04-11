/*
 * RulePaneFactory.java
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
import javafx.scene.control.Label;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Arman Bilge
 */
public abstract class RulePaneFactory<R extends XMLSyntaxRule> {

    protected abstract Class<R> getRuleType();

    protected abstract RulePane<? super R> createPane(XMLObject xo, R rule);

    private static final Map<Class<? extends XMLSyntaxRule>,RulePaneFactory> factories = new HashMap<>();

    public static <X extends XMLSyntaxRule> RulePane<? super X> createRulePane(final XMLObject xo, final X rule) {
        final RulePaneFactory factory = factories.get(rule.getClass());
        if (factory != null)
            return factory.createPane(xo, rule);
        else
            return new UnsupportedRulePane(xo, rule);
    }

    public static void registerFactory(final RulePaneFactory<?> factory) {
        factories.put(factory.getRuleType(), factory);
    }

    private static final class UnsupportedRulePane extends RulePane<XMLSyntaxRule> {
        private UnsupportedRulePane(final XMLObject xo, final XMLSyntaxRule rule) {
            super(xo, rule);
            add(new Label("Rule of type " + rule.getClass() + " is currently unsupported!"), 0, 0);
        }
    }

    static {
        registerFactory(new AndRulePaneFactory());
        registerFactory(new OrRulePaneFactory());
        registerFactory(new XORRulePaneFactory());
        registerFactory(new AttributeRulePaneFactory());
        // TODO: Register all built-in factories here
    }

}
