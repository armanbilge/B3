/*
 * Citable.java
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

package beast.util;

import java.util.List;

/**
 * Interface for associating a list of citations with an object
 *
 * @author Marc A. Suchard
 */

public interface Citable {

    /**
     * @return a list of citations associated with this object
     */
    List<Citation> getCitations();

    public class Utils {

        public static String getCitationString(Citable citable, String prepend, String postpend) {
            List<Citation> citations = citable.getCitations();
            if (citations == null || citations.size() == 0) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            for (Citation citation : citations) {
                builder.append(prepend);
                builder.append(citation.toString());
                builder.append(postpend);
            }
            return builder.toString();
        }

        public static String getCitationString(Citable citable) {
            return getCitationString(citable, DEFAULT_PREPEND, DEFAULT_POSTPEND);
        }

        public static final String DEFAULT_PREPEND = "\t\t";
        public static final String DEFAULT_POSTPEND = "\n";
    }
}
