/*
 * SequenceList.java
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

package beast.evolution.sequence;

import beast.evolution.util.TaxonList;

/**
 * Interface for a list of sequences.
 *
 * @version $Id: SequenceList.java,v 1.10 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Andrew Rambaut
 */
public interface SequenceList extends TaxonList {

	/**
	 * @return a count of the number of sequences in the list.
	 */
	public int getSequenceCount();

	/**
	 * @return the ith sequence in the list.
	 */
	public Sequence getSequence(int i);

	/**
	 * Sets an named attribute for a given sequence.
	 * @param index the index of the sequence whose attribute is being set.
	 * @param name the name of the attribute.
	 * @param value the new value of the attribute.
	 */
	public void setSequenceAttribute(int index, String name, Object value);

	/**
	 * @return an object representing the named attributed for the given sequence.
	 * @param index the index of the sequence whose attribute is being fetched.
	 * @param name the name of the attribute of interest.
	 */
	public Object getSequenceAttribute(int index, String name);

}

