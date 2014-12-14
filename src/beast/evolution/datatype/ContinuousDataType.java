/*
 * ContinuousDataType.java
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

package beast.evolution.datatype;

/**
 * @author Andrew Rambaut
 *
 * Continuous data type. This is a place holder to allow mixing of continuous with
 * discrete traits. None of the methods will return anything useful.
 */
public class ContinuousDataType extends DataType {

    public static final String DESCRIPTION = "continuous";
    public static final ContinuousDataType INSTANCE = new ContinuousDataType();

    /**
     * Constructor
     */
    public ContinuousDataType(){
        stateCount = 0;
        ambiguousStateCount = 0;
    }

    @Override
    public char[] getValidChars() {
        return null;
    }

    /**
     * @return the description of the data type
     */
    public String getDescription() {
		return DESCRIPTION;
	}

    public Type getType(){
        return Type.CONTINUOUS;
    }

}
