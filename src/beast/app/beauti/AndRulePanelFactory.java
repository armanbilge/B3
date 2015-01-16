package beast.app.beauti;

import beast.xml.AndRule;
import beast.xml.XMLObject;
import beast.xml.XMLSyntaxRule;

import javax.swing.BoxLayout;

/**
 * @author Arman Bilge
 */
final class AndRulePanelFactory extends RulePanelFactory<AndRule> {

    @Override
    protected Class<AndRule> getRuleType() {
        return AndRule.class;
    }

    @Override
    protected RulePanel createPanel(final XMLObject xo, final AndRule rule) {
        return new AndRulePanel(xo, rule);
    }

    private static final class AndRulePanel extends RulePanel<AndRule> {
        protected AndRulePanel(final XMLObject xo, final AndRule rule) {
            super(xo, rule);
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            for (final XMLSyntaxRule r : rule.getRules())
                add(createRulePanel(xo, r));
        }
    }
}
