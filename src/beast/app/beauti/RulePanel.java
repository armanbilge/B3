package beast.app.beauti;

import beast.xml.XMLObject;
import beast.xml.XMLSyntaxRule;

import javax.swing.JPanel;

/**
 * @author Arman Bilge
 */
public abstract class RulePanel<R extends XMLSyntaxRule> extends JPanel {

    final XMLObject xo;
    final R rule;

    protected RulePanel(final XMLObject xo, final R rule) {
        this.xo = xo;
        this.rule = rule;
    }

}
