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
            add(new JLabel("Rule of type " + rule.getClass() + " is current unsupported!"));
        }
    }

    static {
        // TODO: Register all built-in factories here
    }

}
