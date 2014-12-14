/*
 * TwoStates.java
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
 * implements DataType for two-state data
 *
 * @version $Id: TwoStates.java,v 1.9 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class TwoStates extends DataType {

    /**
	 * Name of data type. For XML and human reading of data type.
	 */
	public static final String DESCRIPTION = "binary";
	public static final TwoStates INSTANCE = new TwoStates();

    public static final int ZERO_STATE = 0;
	public static final int ONE_STATE = 1;

	public static final int UNKNOWN_STATE = 2;
	public static final int GAP_STATE = 3;
	
	/** 
	 * A table to translate state numbers (0-3) into character codes
	 */
	public static final char[] TWOSTATE_CHARS = 
		{ '0','1', UNKNOWN_CHARACTER,GAP_CHARACTER};

	/** 
	 * A table to map state numbers (0-3) to their ambiguities
	 */
	public static final String[] TWOSTATE_AMBIGUITIES = {
	//	 0    1	   ?     -
		"0", "1", "01", "01"
	};

	/**
	 * Private constructor - DEFAULT_INSTANCE provides the only instance
	 */
	private TwoStates() {
		stateCount = 2;
		ambiguousStateCount = 4;
	}

    @Override
    public char[] getValidChars() {
        return TWOSTATE_CHARS;
    }

    // Get state corresponding to character c
	public int getState(char c)
	{
		switch (c)
		{
 			case '0': 
				return 0;
			case '1': 
				return 1;
			case UNKNOWN_CHARACTER:
				return 2;
			case GAP_CHARACTER:
				return 3;
			default:
				return 2;
		}
	}

	/**
	 * Get character corresponding to a given state
	 *
	 * @param state state
	 *
	 * return corresponding character
	 */
	public char getChar(int state) {
		return TWOSTATE_CHARS[state];
	}

	/**
	 * returns an array containing the non-ambiguous states
	 * that this state represents.
	 */
	public int[] getStates(int state) {

		String stateString = TWOSTATE_AMBIGUITIES[state];
		int[] states = new int[stateString.length()];
		for (int i = 0; i < stateString.length(); i++) {
			states[i] = getState(stateString.charAt(i));
		}

		return states;
	}
	
	/**
	 * returns an array containing the non-ambiguous states that this state represents.
	 */
	public boolean[] getStateSet(int state) {
	
		boolean[] stateSet = new boolean[stateCount];
		if (state < 2) {
			stateSet[1-state] = false;
			stateSet[state] = true;
		} else {
			stateSet[0] = true;
			stateSet[1] = true;
		}
		
		return stateSet;
	}

	/**
	 * description of data type
	 *
	 * @return string describing the data type
	 */
	public String getDescription() {
		return DESCRIPTION;
	}

	/**
	 * type of data type
	 *
	 * @return integer code for the data type
	 */
	public Type getType() {
		return Type.TWO_STATES;
	}

}
