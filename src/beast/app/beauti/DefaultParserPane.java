/*
 * DefaultParserPane.java
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
import beast.xml.XMLObjectParser;
import beast.xml.XMLSyntaxRule;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * @author Arman Bilge
 */
final class DefaultParserPane<P extends XMLObjectParser<?>> extends ParserPane<P> {

    private final Class<P> parserType;

    protected DefaultParserPane(final P parser, final XMLObject xo) {
        super(xo);
        parserType = (Class<P>) parser.getClass();

        final GridPane idPane = new GridPane();
        idPane.add(new Label("id: "), 0, 0);
        final TextField idField = new TextField(parser.getParserName() + "_##");
        idPane.add(idField, 1, 0);
        add(idPane, 0, 0);
        int row = 1;
        for (final XMLSyntaxRule rule : parser.getSyntaxRules())
            add(RulePaneFactory.createRulePane(xo, rule), 0, row++);
    }

    @Override
    protected Class<P> getParserType() {
        return parserType;
    }
}
