/*
 * MaskableSpeciationModel.java
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

package beast.evomodel.speciation;

/**
 * Created by IntelliJ IDEA.
 * User: adru001
 * Date: Nov 2, 2010
 * Time: 10:56:03 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MaskableSpeciationModel extends SpeciationModel {

    public MaskableSpeciationModel(String name, Type units) {
        super(name, units);
    }

    // a model specific implementation that allows this speciation model
    // to be partially masked by another -- useful in model averaging applications
    public abstract void mask(SpeciationModel mask);

    public abstract void unmask();
}
