/*
 * TaxonList.java
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

package beast.evolution.util;

import beast.util.Identifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface for a list of taxa.
 *
 * @version $Id: TaxonList.java,v 1.16 2006/09/05 13:29:34 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public interface TaxonList extends Identifiable, Iterable<Taxon> {

	/**
	 * @return a count of the number of taxa in the list.
	 */
	public int getTaxonCount();

	/**
	 * @return the ith taxon.
	 */
	public Taxon getTaxon(int taxonIndex);

	/**
	 * @return the ID of the ith taxon.
	 */
	public String getTaxonId(int taxonIndex);

	/**
	 * returns the index of the taxon with the given id.
	 */
	int getTaxonIndex(String id);

	/**
	 * returns the index of the given taxon.
	 */
	int getTaxonIndex(Taxon taxon);

    /**
     * returns the taxa as a Java list
     * @return
     */
    List<Taxon> asList();

	/**
	 * @return an object representing the named attributed for the given taxon.
	 * @param taxonIndex the index of the taxon whose attribute is being fetched.
	 * @param name the name of the attribute of interest.
	 */
	public Object getTaxonAttribute(int taxonIndex, String name);

	default boolean hasAttribute(int index, String name) {
		return getTaxonAttribute(index, name) != null;
	}

	default Set<String> getTaxonListIdSet() {
		Set<String> taxaSet = new HashSet<String>();
		for (int i =0; i < getTaxonCount(); i++) {
			taxaSet.add(getTaxonId(i));
		}
		return taxaSet;
	}

	default int findDuplicateTaxon() {
		Set<String> taxaSet = new HashSet<String>();
		for (int i = 0; i < getTaxonCount(); i++) {
			Taxon taxon = getTaxon(i);
			if (taxaSet.contains(taxon.getId())) {
				return i;
			}
			taxaSet.add(taxon.getId());
		}
		return -1;
	}

	default boolean equals(TaxonList taxa2) {
		if (getTaxonCount() != taxa2.getTaxonCount()) {
			return false;
		}
		for (int i =0; i < getTaxonCount(); i++) {
			if (taxa2.getTaxonIndex(getTaxon(i)) == -1) {
				return false;
			}
		}
		return true;
	}

	@Deprecated
	class Utils {

		@Deprecated
		public static boolean hasAttribute(TaxonList taxa, int index, String name) {
			return taxa.hasAttribute(index, name);
		}

		@Deprecated
		public static Set<String> getTaxonListIdSet(TaxonList taxa) {
			return taxa.getTaxonListIdSet();
		}

		@Deprecated
        public static int findDuplicateTaxon(TaxonList taxonList) {
            return taxonList.findDuplicateTaxon();
        }

		@Deprecated
        public static boolean areTaxaIdentical(TaxonList taxa1, TaxonList taxa2) {
            return taxa1.equals(taxa2);
        }


	}

	class MissingTaxonException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1864895946392309485L;

		public MissingTaxonException(String message) { super(message); }
	}
}
