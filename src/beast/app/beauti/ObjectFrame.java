/*
 * ObjectFrame.java
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

import beast.xml.AndRule;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.OrRule;
import beast.xml.Reference;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
* @author Arman Bilge
*/
class ObjectFrame<T> extends JFrame {

    protected final Beauti beauti;
    protected final XMLObjectParser<T> parser;
    protected final XMLObject xo;
    protected final Set<XMLAction> actions;

    @FunctionalInterface
    protected interface XMLAction {
        void act(XMLObject xo);
    }

    public ObjectFrame(final Beauti beauti, final XMLObjectParser<T> parser) {
        super(parser.getParserName());
        this.beauti = beauti;
        this.parser = parser;
        this.xo = new XMLObject(beauti.getDocument(), parser.getParserName());
        this.actions = new HashSet<>();

        final Container pane = getContentPane();
        setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

        final Panel descriptionPanel = new Panel(new FlowLayout());
        descriptionPanel.add(new JLabel(parser.getParserDescription()));
        pane.add(descriptionPanel);

        final JTextField idField = new JTextField(16);
        final JLabel idLabel = new JLabel("id: ", JLabel.LEADING);
        idLabel.setLabelFor(idField);
        actions.add(xo -> xo.setAttribute("id", idField.getText()));
        final Panel idPanel = new Panel(new FlowLayout());
        idPanel.add(idLabel);
        idPanel.add(idField);
        pane.add(idPanel);

        final XMLSyntaxRule[] rules = parser.getSyntaxRules();
        for (final XMLSyntaxRule rule : rules)
            pane.add(createPanelForRule(rule, actions));
        pack();
        setVisible(true);
    }

    protected Panel createPanelForRule(final XMLSyntaxRule rule, final Set<XMLAction> actions) {
        final Panel panel = new Panel();
        if (rule instanceof AndRule) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final Set<XMLAction> andActions = new HashSet<>();
            for (final XMLSyntaxRule r : ((AndRule) rule).getRules())
                panel.add(createPanelForRule(r, andActions));
            actions.add(xo -> andActions.forEach(a -> a.act(xo)));
        } else if (rule instanceof OrRule) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final List<JCheckBox> checkBoxes = new ArrayList();
            final Set<XMLAction> orActions = new LinkedHashSet<>();
            for (final XMLSyntaxRule r : ((OrRule) rule).getRules()) {
                final Panel p = new Panel(new FlowLayout());
                final Panel rp = createPanelForRule(r, orActions);
                rp.setEnabled(false);
                final JCheckBox cb = new JCheckBox();
                cb.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        rp.setEnabled(cb.isSelected());
                    }
                });
                p.add(cb);
                checkBoxes.add(cb);
                p.add(rp);
                panel.add(p);
            }
            actions.add(xo -> {
                final Iterator<JCheckBox> cbit = checkBoxes.iterator();
                final Iterator<XMLAction> oait = orActions.iterator();
                while (cbit.hasNext() && oait.hasNext())
                    if (cbit.next().isSelected()) oait.next().act(xo);
            });
        } else if (rule instanceof XORRule) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final ButtonGroup buttonGroup = new ButtonGroup();
            final List<JRadioButton> radioButtons = new ArrayList();
            final Set<XMLAction> xorActions = new LinkedHashSet<>();
            for (final XMLSyntaxRule r : ((XORRule) rule).getRules()) {
                final Panel p = new Panel(new FlowLayout());
                final Panel rp = createPanelForRule(r, xorActions);
                rp.setEnabled(false);
                final JRadioButton rb = new JRadioButton();
                rb.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        rp.setEnabled(rb.isSelected());
                    }
                });
                p.add(rb);
                radioButtons.add(rb);
                buttonGroup.add(rb);
                p.add(rp);
                panel.add(p);
            }
            actions.add(xo -> {
                final Iterator<JRadioButton> cbit = radioButtons.iterator();
                final Iterator<XMLAction> oait = xorActions.iterator();
                while (cbit.hasNext() && oait.hasNext())
                    if (cbit.next().isSelected()) {
                        oait.next().act(xo);
                        break;
                    }
            });
        } else if (rule instanceof AttributeRule) {
            final AttributeRule ar = (AttributeRule) rule;
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final Panel p = new Panel(new FlowLayout());
            final JTextField tf = new JTextField(16);
            final JCheckBox cb;
            if (ar.getOptional()) {
                tf.setEnabled(false);
                cb = new JCheckBox();
                cb.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tf.setEnabled(cb.isSelected());
                    }
                });
                p.add(cb);
            } else {
                cb = null;
            }
            final JLabel l = new JLabel(ar.getName() + " (" + ar.getTypeName() + "):");
            l.setLabelFor(tf);
            p.add(l);
            p.add(tf);
            actions.add(xo -> {
                if (cb != null || cb.isSelected())
                    xo.setAttribute(ar.getName(), tf.getText());
            });
            panel.add(p);
            panel.add(new JLabel(ar.getDescription()));
        } else if (rule instanceof ElementRule) {
            final ElementRule er = (ElementRule) rule;
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            if (er.getName() != null) {
                panel.add(new JLabel(er.getName()));
                final Set<XMLAction> erActions = new HashSet<>();
                for (final XMLSyntaxRule r : er.getRules())
                    panel.add(createPanelForRule(r, erActions));
                actions.add(xo -> erActions.forEach(a -> a.act(xo)));
            } else {
                panel.add(new JLabel(er.getElementClass().getSimpleName() + " (min: " + er.getMin() + (er.getMax() != Integer.MAX_VALUE ? ", max: " + er.getMax() : "") + ")"));
                final DefaultListModel lm = new DefaultListModel<>();
                final JList<Object> l = new JList<>(lm);
                panel.add(l);
                final Panel bp = new Panel(new FlowLayout());
                final JButton ab = new JButton("+");
                ab.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final JDialog d = new JDialog(ObjectFrame.this, er.getElementClass().getSimpleName());
                        final ButtonGroup bg = new ButtonGroup();
                        final JPanel rp = new JPanel(new FlowLayout());
                        rp.setVisible(true);
                        final JComboBox<Reference> rc = new JComboBox<>(new DefaultComboBoxModel<>());
                        for (final Reference r : beauti.getReferences(er.getElementClass()))
                            rc.addItem(r);
                        rc.setEnabled(false);
                        final JRadioButton rb = new JRadioButton();
                        bg.add(rb);
                        rb.addActionListener(new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                rc.setEnabled(rb.isSelected());
                            }
                        });
                        rp.add(rb);
                        rp.add(rc);
                        d.getContentPane().add(rp);
                        final JPanel cp = new JPanel(new FlowLayout());
                        final JComboBox<XMLObjectParser<?>> cc = new JComboBox<>(new DefaultComboBoxModel<>());
                        for (final XMLObjectParser p : beauti.getParsers(er.getElementClass()))
                            cc.addItem(p);
                        cc.setEnabled(false);
                        final JRadioButton cb = new JRadioButton();
                        bg.add(cb);
                        cb.addActionListener(new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                cc.setEnabled(cb.isSelected());
                            }
                        });
                        cp.add(cb);
                        cp.add(cc);
                        d.getContentPane().add(cp);
                        d.pack();
                        d.setVisible(true);
                    }
                });
                bp.add(ab);
                final JButton rb = new JButton("-");
                rb.setEnabled(false);
                rb.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (final int i : l.getSelectedIndices())
                            lm.remove(i);
                    }
                });
                bp.add(rb);
                panel.add(bp);
                l.addVetoableChangeListener(new VetoableChangeListener() {
                    @Override
                    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
                        ab.setEnabled(lm.size() >= Integer.MAX_VALUE);
                        rb.setEnabled(lm.size() < 0);
                    }
                });
                actions.add(xo -> {
                    for (int i = 0; i < lm.getSize(); ++i)
                        xo.addChild(lm.getElementAt(i));
                });
            }
            panel.add(new JLabel(er.getDescription()));
        } else {
            panel.setLayout(new FlowLayout());
            panel.add(new JLabel("Unknown XMLSyntaxRule!"));
        }
        return panel;
    }

    public XMLObject getXMLObject() {
        return xo;
    }

}
