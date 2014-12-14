/*
 * MutableTaxonListListener.java
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

/**
 * Interface for a listener to a mutable list of taxa.
 *
 * @version $Id: MutableTaxonListListener.java,v 1.6 2005/05/24 20:25:57 rambaut Exp $
 *
 * @author Alexei Drummond
 */
public interface MutableTaxonListListener {

	/** Called when a taxon has been added to the taxonList */
	void taxonAdded(TaxonList taxonList, Taxon taxon);
	
	/** Called when a taxon has been successfully removed from the taxonList */
	void taxonRemoved(TaxonList taxonList, Taxon taxon);
	
	/** Called when one or more taxon has been edited */
	void taxaChanged(TaxonList taxonList);
}
