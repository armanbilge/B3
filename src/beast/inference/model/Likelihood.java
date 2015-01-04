/*
 * Likelihood.java
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

package beast.inference.model;

import beast.inference.loggers.Loggable;
import beast.math.Differentiable;
import beast.math.MachineAccuracy;
import beast.util.Identifiable;

import java.util.HashSet;
import java.util.Set;

/**
 * classes that calculate likelihoods should implement this interface.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: Likelihood.java,v 1.16 2005/05/24 20:26:00 rambaut Exp $
 */

public interface Likelihood extends Loggable, Identifiable, Differentiable {

	/**
	 * Get the model.
	 * @return the model.
	 */
	Model getModel();

	/**
	 * Get the log likelihood.
	 * @return the log likelihood.
	 */
	double getLogLikelihood();

	default double evaluate() {
		return getLogLikelihood();
	}

	/**
	 * Forces a complete recalculation of the likelihood next time getLikelihood is called
	 */
	void makeDirty();

    /**
     * @return  A detailed name of likelihood for debugging.
     */
    String prettyName();

    /**
     * @return is the likelihood used in the MCMC?
     */
    boolean isUsed();

    void setUsed();

    /**
     * @return true if this likelihood should be evaluated early (if for example it may return a zero likelihood
     * and could terminate the evaluation early or is a optional conditioning for another likelihood.
     */
    boolean evaluateEarly();

    /**
	 * A simple abstract base class for likelihood functions
	 */

	public abstract class Abstract implements Likelihood, ModelListener {

		public Abstract(Model model) {

			this.model = model;
			if (model != null) model.addModelListener(this);
   		}

        public void modelChangedEvent(Model model, Object object, int index) {
            makeDirty();
        }

        // by default restore is the same as changed
        public void modelRestored(Model model) {
            makeDirty();
        }

        // **************************************************************
	    // Likelihood IMPLEMENTATION
	    // **************************************************************

		/**
		 * Get the model.
		 * @return the model.
		 */
		public Model getModel() { return model; }

		public final double getLogLikelihood() {
			if (!getLikelihoodKnown()) {
				logLikelihood = calculateLogLikelihood();
				likelihoodKnown = true;
			}
			return logLikelihood;
		}

		public void makeDirty() {
			likelihoodKnown = false;
		}

		/**
		 * Called to decide if the likelihood must be calculated. Can be overridden
		 * (for example, to always return false).
         * @return  true if no need to recompute likelihood
         */
		protected boolean getLikelihoodKnown() {
			return likelihoodKnown;
		}

		protected abstract double calculateLogLikelihood();

		public String toString() {
            // don't call any "recalculating" stuff like getLogLikelihood() in toString -
            // this interferes with the debugger.

            //return getClass().getName() + "(" + getLogLikelihood() + ")";
            return getClass().getName() + "(" + (getLikelihoodKnown() ? logLikelihood : "??") + ")";
		}

        static public String getPrettyName(Likelihood l) {
            final Model m = l.getModel();
            String s = l.getClass().getName();
            String[] parts = s.split("\\.");
            s = parts[parts.length - 1];
            if( m != null ) {
                final String modelName = m.getModelName();
                final String i = m.getId();
                s = s + "(" + modelName;
                if( i != null && !i.equals(modelName) ) {
                    s = s + '[' + i + ']';
                }
                s = s + ")";
            }
            return s;
        }

      public String prettyName() {
          return getPrettyName(this);
      }

        public boolean isUsed() {
            return used;
        }

        public void setUsed() {
            this.used = true;
        }

        public boolean evaluateEarly() {
            return false;
        }

        // **************************************************************
	    // Loggable IMPLEMENTATION
	    // **************************************************************

		/**
		 * @return the log columns.
		 */
		public beast.inference.loggers.LogColumn[] getColumns() {
			return new beast.inference.loggers.LogColumn[] {
				new LikelihoodColumn(getId())
			};
		}

		private class LikelihoodColumn extends beast.inference.loggers.NumberColumn {
			public LikelihoodColumn(String label) { super(label); }
			public double getDoubleValue() { return getLogLikelihood(); }
		}

		// **************************************************************
	    // Identifiable IMPLEMENTATION
	    // **************************************************************

		private String id = null;

		public void setId(String id) { this.id = id; }

		public String getId() { return id; }

		private final Model model;
		private double logLikelihood;
		private boolean likelihoodKnown = false;

        private boolean used = false;
	}


    // set to store all created likelihoods
    final static Set<Likelihood> FULL_LIKELIHOOD_SET = new HashSet<Likelihood>();

}
