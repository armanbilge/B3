/*
 * ParserPane.java
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
import javafx.scene.layout.GridPane;

/**
 * @author Arman Bilge
 */
public abstract class ParserPane<P extends XMLObjectParser<?>> extends GridPane {

    private final XMLObject xo;

    protected ParserPane(final XMLObject xo) {
        this.xo = xo;
    }

    protected abstract Class<P> getParserType();

}
