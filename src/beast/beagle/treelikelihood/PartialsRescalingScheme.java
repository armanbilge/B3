/*
 * PartialsRescalingScheme.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
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

package beast.beagle.treelikelihood;

/**
 * @author Marc Suchard
 * @author Andrew Rambaut
 */
public enum PartialsRescalingScheme {

    DEFAULT("default"), // what ever our current favourite default is
    NONE("none"),       // no scaling
    DYNAMIC("dynamic"), // rescale when needed and reuse scaling factors
    ALWAYS("always"),   // rescale every node, every site, every time - slow but safe
    DELAYED("delayed"), // postpone until first underflow then switch to 'always'
    AUTO("auto");       // BEAGLE automatic scaling - currently playing it safe with 'always'
//    KICK_ASS("kickAss"),// should be good, probably still to be discovered

    PartialsRescalingScheme(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    private final String text;

    public static PartialsRescalingScheme parseFromString(String text) {
        for (PartialsRescalingScheme scheme : PartialsRescalingScheme.values()) {
            if (scheme.getText().compareToIgnoreCase(text) == 0)
                return scheme;
        }
        return DEFAULT;
    }

    @Override
    public String toString() {
        return text;
    }
}
