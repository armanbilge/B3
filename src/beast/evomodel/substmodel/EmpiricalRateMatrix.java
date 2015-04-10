/*
 * EmpiricalRateMatrix.java
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

import beast.evolution.datatype.AminoAcids;
import beast.evolution.datatype.DataType;

/**
 * An interface for empirical rate matrices.
 *
 * @version $Id: EmpiricalRateMatrix.java,v 1.3 2005/05/24 20:25:58 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public interface EmpiricalRateMatrix {

	String getName();
	DataType getDataType();
	
	double[] getEmpiricalRates();
	double[] getEmpiricalFrequencies();
			
	public abstract class Abstract implements EmpiricalRateMatrix {
	
		public Abstract(String name, DataType dataType) {
			this.name = name;
			this.dataType = dataType;
		}
		
		public final String getName() { return name; }
		
		public final DataType getDataType() { return dataType; }
		
		public final double[] getEmpiricalRates() { return rates; }
		public final double[] getEmpiricalFrequencies() { return frequencies; }
			
		protected double[] rates = null;
		protected double[] frequencies = null;
		
		private String name;
		protected DataType dataType;
	}

    public abstract class AbstractAminoAcid extends Abstract {
	
		public AbstractAminoAcid(String name) {
			super(name, AminoAcids.INSTANCE);
			
			int n = dataType.getStateCount();
			rates = new double[(n * (n - 1)) / 2];
			frequencies = new double[n];
		}
		
		public final void setEmpiricalRates(double[][]matrix, String aminoAcidOrder) {
			int k = 0;
			
			for (int i = 0; i < dataType.getStateCount(); i++) {
			
				int u = aminoAcidOrder.indexOf(dataType.getChar(i));
				
				for (int j = i + 1; j < dataType.getStateCount(); j++) {
				
					int v = aminoAcidOrder.indexOf(dataType.getChar(j));
					
					if (u < v) {
						rates[k] = matrix[u][v];
					} else {
						rates[k] = matrix[v][u];
					}
					
					k++;
				}
			}
		}
		
		public final void setEmpiricalFrequencies(double[]freqs, String aminoAcidOrder) {

            double sum = 0.0;
            for (int i = 0; i < dataType.getStateCount(); i++) {
				int u = aminoAcidOrder.indexOf(dataType.getChar(i));
				frequencies[i] = freqs[u];
                sum += frequencies[i];
            }

            // normalize - we should probably detect large discrepancies but the empirical
            // matrices have numerical rounding that cause small discrepancies.
            for (int i = 0; i < dataType.getStateCount(); i++) {
				frequencies[i] /= sum;
            }
        }
	}
}