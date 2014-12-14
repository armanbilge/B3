/*
 * AbstractNucleotideModel.java
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

package beast.evomodel.substmodel;

import beast.evolution.datatype.Nucleotides;

/**
 * base class for nucleotide rate matrices
 *
 * @version $Id: AbstractNucleotideModel.java,v 1.3 2005/05/24 20:25:58 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Korbinian Strimmer
 */
abstract public class AbstractNucleotideModel extends AbstractSubstitutionModel  {

	double freqA, freqC, freqG, freqT,
            // A+G
            freqR,
            // C+T
            freqY;

	// Constructor
	public AbstractNucleotideModel(String name, FrequencyModel freqModel)
	{
		super(name, Nucleotides.INSTANCE, freqModel);
		
	}
	
	protected void frequenciesChanged() {
		// Nothing to precalculate
	}
	
	protected void ratesChanged() {
		// Nothing to precalculate
	}
	
	protected void calculateFreqRY() {
		freqA = freqModel.getFrequency(0);
		freqC = freqModel.getFrequency(1);
		freqG = freqModel.getFrequency(2);
		freqT = freqModel.getFrequency(3);
		freqR = freqA + freqG;
		freqY = freqC + freqT;
	}
	
	// *****************************************************************
	// Interface Model
	// *****************************************************************
	
	
	protected void storeState() { } // nothing to do
	
	/**
	 * Restore the additional stored state
	 */
	protected void restoreState() {
		updateMatrix = true;
	}
	
	protected void acceptState() { } // nothing to do
}