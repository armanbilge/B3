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
import beast.xml.ContentRule;
import beast.xml.ElementRule;
import beast.xml.OrRule;
import beast.xml.Reference;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
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
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
* @author Arman Bilge
*/
class ObjectFrame<T> extends JFrame implements DocumentListener {

    protected final Beauti beauti;
    protected final XMLObjectParser<T> parser;
    protected final XMLObject xo;
    protected final Set<XMLAction> actions;

    @Override
    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        actions.forEach(XMLAction::validate);
    }

    protected interface XMLAction {
        boolean read();
        void write();
        void validate();
    }

    public ObjectFrame(final XMLObject xo, final Beauti beauti, final XMLObjectParser<T> parser) {
        super(parser.getParserName());
        this.xo = xo;
        this.beauti = beauti;
        this.parser = parser;
        if (xo.getName() != parser.getParserName())
            throw new IllegalArgumentException();
        actions = new HashSet<>();

        final Container pane = getContentPane();
        setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

        final Panel descriptionPanel = new Panel(new FlowLayout());
        descriptionPanel.add(new JLabel(parser.getParserDescription()));
        pane.add(descriptionPanel);

        final JTextField idField = new JTextField(16);
        idField.setText(parser.getParserName() + "_" + Integer.toString(beauti.getInstanceCount(parser)));
        idField.getDocument().addDocumentListener(this);
        final JLabel idLabel = new JLabel("id: ", JLabel.LEADING);
        idLabel.setLabelFor(idField);
        final JLabel idWarning = createWarningIcon();
        actions.add(new XMLAction() {
            @Override
            public boolean read() {
                try {
                    idField.setText(xo.getStringAttribute("id"));
                } catch (XMLParseException e) {
                    e.printStackTrace();
                }
                return xo.hasAttribute("id");
            }
            @Override
            public void write() {
                xo.setAttribute("id", idField.getText());
            }
            @Override
            public void validate() {
                final String s = idField.getText();
                if (s.isEmpty() || s.contains("\""))
                    idWarning.setVisible(true);
                else
                    idWarning.setVisible(false);
            }
        });
        final Panel idPanel = new Panel(new FlowLayout());
        idPanel.add(idLabel);
        idPanel.add(idField);
        idPanel.add(idWarning);
        pane.add(idPanel);

        final XMLSyntaxRule[] rules = parser.getSyntaxRules();
        for (final XMLSyntaxRule rule : rules)
            pane.add(createPanelForRule(xo, rule, actions));
        changedUpdate(null);

        final JPanel buttonPanel = new JPanel(new FlowLayout());
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        final JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getXMLObject();
                beauti.store(idField.getText(), xo);
                setVisible(false);
            }
        });
        buttonPanel.add(cancelButton);
        buttonPanel.add(doneButton);
        add(buttonPanel);

        pack();
        setVisible(true);
    }

    protected Panel createPanelForRule(final XMLObject xo, final XMLSyntaxRule rule, final Set<XMLAction> actions) {
        final Panel panel = new Panel() {
            @Override
            public void setEnabled(boolean b) {
                super.setEnabled(b);
                Arrays.stream(getComponents()).forEach(c -> c.setEnabled(b));
            }
        };
        if (rule instanceof AndRule) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final Set<XMLAction> andActions = new HashSet<>();
            for (final XMLSyntaxRule r : ((AndRule) rule).getRules())
                panel.add(createPanelForRule(xo, r, andActions));
            actions.add(new XMLAction() {
                @Override
                public boolean read() {
                    andActions.forEach(XMLAction::read);
                    return true;
                }
                @Override
                public void write() {
                    andActions.forEach(XMLAction::write);
                }
                @Override
                public void validate() {
                    andActions.forEach(XMLAction::validate);
                }
            });
        } else if (rule instanceof OrRule) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final List<JCheckBox> checkBoxes = new ArrayList();
            final Set<XMLAction> orActions = new LinkedHashSet<>();
            for (final XMLSyntaxRule r : ((OrRule) rule).getRules()) {
                final Panel p = new Panel(new FlowLayout());
                final Panel rp = createPanelForRule(xo, r, orActions);
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
            actions.add(new XMLAction() {
                @Override
                public boolean read() {
                    final Iterator<JCheckBox> cbit = checkBoxes.iterator();
                    final Iterator<XMLAction> oait = orActions.iterator();
                    while (cbit.hasNext() && oait.hasNext())
                        cbit.next().setSelected(oait.next().read());
                    return true;
                }
                @Override
                public void write() {
                    final Iterator<JCheckBox> cbit = checkBoxes.iterator();
                    final Iterator<XMLAction> oait = orActions.iterator();
                    while (cbit.hasNext() && oait.hasNext())
                        if (cbit.next().isSelected()) oait.next().write();
                }
                @Override
                public void validate() {
                    final Iterator<JCheckBox> cbit = checkBoxes.iterator();
                    final Iterator<XMLAction> oait = orActions.iterator();
                    while (cbit.hasNext() && oait.hasNext())
                        if (cbit.next().isSelected()) oait.next().validate();
                }
            });
        } else if (rule instanceof XORRule) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            final ButtonGroup buttonGroup = new ButtonGroup();
            final List<JRadioButton> radioButtons = new ArrayList();
            final Set<XMLAction> xorActions = new LinkedHashSet<>();
            final Set<Action> radioActions = new HashSet<>();
            final Action radioAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    radioActions.forEach(a -> a.actionPerformed(e));
                }
            };
            for (final XMLSyntaxRule r : ((XORRule) rule).getRules()) {
                final Panel p = new Panel(new FlowLayout());
                final Panel rp = createPanelForRule(xo, r, xorActions);
                rp.setEnabled(false);
                final JRadioButton rb = new JRadioButton();
                radioActions.add(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        rp.setEnabled(rb.isSelected());
                    }
                });
                rb.addActionListener(radioAction);
                p.add(rb);
                radioButtons.add(rb);
                buttonGroup.add(rb);
                p.add(rp);
                panel.add(p);
            }
            actions.add(new XMLAction() {
                @Override
                public boolean read() {
                    final Iterator<JRadioButton> cbit = radioButtons.iterator();
                    final Iterator<XMLAction> oait = xorActions.iterator();
                    while (cbit.hasNext() && oait.hasNext())
                        if (oait.next().read()) {
                            cbit.next().setSelected(true);
                            break;
                        };
                    return true;
                }
                @Override
                public void write() {
                    final Iterator<JRadioButton> cbit = radioButtons.iterator();
                    final Iterator<XMLAction> oait = xorActions.iterator();
                    while (cbit.hasNext() && oait.hasNext())
                        if (cbit.next().isSelected()) {
                            oait.next().write();
                            break;
                        }
                }
                @Override
                public void validate() {
                    final Iterator<JRadioButton> cbit = radioButtons.iterator();
                    final Iterator<XMLAction> oait = xorActions.iterator();
                    while (cbit.hasNext() && oait.hasNext())
                        if (cbit.next().isSelected()) {
                            oait.next().validate();
                            break;
                        }
                }
            });
        } else if (rule instanceof AttributeRule) {
            final AttributeRule ar = (AttributeRule) rule;
            panel.setLayout(new FlowLayout());
            final JTextField tf = new JTextField(16);
            tf.getDocument().addDocumentListener(this);
            final JCheckBox cb;
            if (ar.getOptional()) {
                tf.setEnabled(false);
                cb = new JCheckBox();
                cb.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tf.setEnabled(cb.isSelected());
                        changedUpdate(null);
                    }
                });
                panel.add(cb);
            } else {
                cb = null;
            }
            final JLabel l = new JLabel(ar.getName() + " (" + ar.getTypeName() + "):");
            l.setLabelFor(tf);
            final JLabel w = createWarningIcon();
            panel.add(l);
            panel.add(tf);
            panel.add(w);
            actions.add(new XMLAction() {
                @Override
                public boolean read() {
                    final boolean b = xo.hasAttribute(ar.getName());
                    if (cb != null) cb.setSelected(b);
                    if (b) try {
                        tf.setText(xo.getAttribute(ar.getName()).toString());
                    } catch (XMLParseException e) {
                        e.printStackTrace();
                    }
                    return b;
                }
                @Override
                public void write() {
                    if (cb == null || cb.isSelected())
                        xo.setAttribute(ar.getName(), tf.getText());
                }
                @Override
                public void validate() {
                    write();
                    if (cb == null || cb.isSelected())
                        w.setVisible(!ar.isSatisfied(xo));
                    else
                        w.setVisible(false);
                }
            });
            tf.setToolTipText(ar.getDescription());
        } else if (rule instanceof ElementRule) {
            final ElementRule er = (ElementRule) rule;
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            if (er.getName() != null) {
                final JPanel rp = new JPanel();
                rp.setLayout(new BoxLayout(rp, BoxLayout.PAGE_AXIS));
                final JPanel p = new JPanel(new FlowLayout());
                final JCheckBox cb;
                if (er.getMin() == 0) {
                    rp.setEnabled(false);
                    cb = new JCheckBox();
                    cb.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            rp.setEnabled(cb.isSelected());
                        }
                    });
                    p.add(cb);
                } else {
                    cb = null;
                }
                p.add(new JLabel(er.getName()));
                panel.add(p);
                panel.add(rp);
                final Set<XMLAction> erActions = new HashSet<>();
                if (!xo.hasChildNamed(er.getName()))
                    xo.addChild(new XMLObject(beauti.getDocument(), er.getName()));
                for (final XMLSyntaxRule r : er.getRules())
                    rp.add(createPanelForRule(xo.getChild(er.getName()), r, erActions));
                actions.add(new XMLAction() {
                    @Override
                    public boolean read() {
                        final boolean b = xo.hasChildNamed(er.getName());
                        if (cb != null) cb.setSelected(b);
                        if (b) erActions.forEach(XMLAction::read);
                        return b;
                    }
                    @Override
                    public void write() {
                        if (cb == null || cb.isSelected())
                            erActions.forEach(XMLAction::write);

                    }
                    @Override
                    public void validate() {
                        if (cb == null || cb.isSelected())
                            erActions.forEach(XMLAction::validate);
                    }
                });
            } else {
                final int min = er.getMin();
                final int max = er.getMax();
                final String count;
                if (min == max)
                    count = "count: " + min;
                else
                    count = "min: " + min + (max != Integer.MAX_VALUE ? ", max: " + max : "");
                panel.add(new JLabel(er.getElementClass().getSimpleName() + " (" + count + ")"));
                final DefaultListModel lm = new DefaultListModel<>();
                final JList<Object> l = new JList<>(lm);
                panel.add(l);
                final Panel bp = new Panel(new FlowLayout());
                final JButton ab = new JButton("+");
                ab.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final JDialog d = new JDialog(ObjectFrame.this, er.getElementClass().getSimpleName());
                        d.setLayout(new BoxLayout(d.getContentPane(), BoxLayout.PAGE_AXIS));
                        final ButtonGroup bg = new ButtonGroup();
                        final JPanel rp = new JPanel(new FlowLayout());
                        final JComboBox<Reference> rc = new JComboBox<>(new DefaultComboBoxModel<>());
                        for (final Reference r : beauti.getReferences(er.getElementClass()))
                            rc.addItem(r);
                        rc.setEnabled(false);
                        final JRadioButton rb = new JRadioButton();
                        bg.add(rb);
                        rp.add(rb);
                        rp.add(rc);
                        final JPanel cp = new JPanel(new FlowLayout());
                        final JComboBox<XMLObjectParser> cc = new JComboBox<>(new DefaultComboBoxModel<>());
                        cc.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList l, Object o, int i, boolean s, boolean f) {
                                final Component c = super.getListCellRendererComponent(l, o, i, s, f);
                                if (o != null) {
                                    final XMLObjectParser p = (XMLObjectParser) o;
                                    setText(p.getParserName());
                                    setToolTipText(p.getParserDescription());
                                }
                                return c;
                            }
                        });
                        for (final XMLObjectParser p : beauti.getParsers(er.getElementClass()))
                            cc.addItem(p);
                        cc.setEnabled(false);
                        final JRadioButton cb = new JRadioButton();
                        bg.add(cb);
                        cp.add(cb);
                        cp.add(cc);
                        final JButton pb = new JButton("[No action selected]");
                        Action a = new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                rc.setEnabled(rb.isSelected());
                                cc.setEnabled(cb.isSelected());
                                if (rb.isSelected())
                                    pb.setText("Reference");
                                else
                                    pb.setText("Create");
                            }
                        };
                        rb.addActionListener(a);
                        cb.addActionListener(a);
                        d.add(new JLabel("Select idref:"));
                        d.add(rp);
                        d.add(new JLabel("Create new object using parser:"));
                        d.add(cp);
                        final JPanel bp = new JPanel();
                        final JButton xb = new JButton("Cancel");
                        pb.addActionListener(new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (rb.isSelected()) {
                                    lm.addElement(rc.getSelectedItem());
                                }
                                else {
                                    final XMLObjectParser p = (XMLObjectParser) cc.getSelectedItem();
                                    final XMLObject xo = new XMLObject(beauti.getDocument(), p.getParserName());
                                    new ObjectFrame(xo, beauti, p);
                                }
                                d.setVisible(false);
                            }
                        });
                        xb.addActionListener(new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                d.setVisible(false);
                            }
                        });
                        bp.add(xb);
                        bp.add(pb);
                        d.add(bp);
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
                l.addVetoableChangeListener(evt -> {
                    ab.setEnabled(lm.size() >= Integer.MAX_VALUE);
                    rb.setEnabled(lm.size() < 0);
                });
                actions.add(new XMLAction() {
                    @Override
                    public boolean read() {
                        return false;
                    }
                    @Override
                    public void write() {
                        for (int i = 0; i < lm.getSize(); ++i)
                            xo.addChild(lm.getElementAt(i));
                    }
                    @Override
                    public void validate() {

                    }
                });
//                actions.add(() -> {
//                    for (int i = 0; i < lm.getSize(); ++i)
//                        xo.addChild(lm.getElementAt(i));
//                });
            }
            panel.add(new JLabel(er.getDescription()));
        } else if (rule instanceof ContentRule) {
            panel.setLayout(new FlowLayout());
            panel.add(new JLabel("Content rule is not supported yet!"));
        } else {
            panel.setLayout(new FlowLayout());
            panel.add(new JLabel("Unknown/unsupported XMLSyntaxRule!"));
        }
        return panel;
    }

    private static JLabel createWarningIcon() {
        return new JLabel(UIManager.getIcon("OptionPane.errorIcon"));
    }

    public XMLObject getXMLObject() {
        xo.clear();
        actions.forEach(XMLAction::write);
        return xo;
    }



}
