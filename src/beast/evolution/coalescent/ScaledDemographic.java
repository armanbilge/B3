/*
 * ScaledDemographic.java
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

package beast.evolution.coalescent;

/**
 * Scale a demographic by a fixed factor.
 *
 * Minimal implementation, only value and intensity implemented.
 *
 * @author Joseph Heled
 *         Date: 19/11/2007
 */
public class ScaledDemographic extends DemographicFunction.Abstract {
    private final double scale;
    private final DemographicFunction demo;

    public ScaledDemographic(DemographicFunction demo, double scale) {
        super(demo.getUnits());
        this.scale = scale;
        this.demo = demo;
    }

    public double getDemographic(double t) {
        return demo.getDemographic(t) * scale;
    }

    public double getIntensity(double t) {
        return demo.getIntensity(t) / scale;
    }

    public double getIntegral(double start, double finish) {
        return (demo.getIntensity(finish) - demo.getIntensity(start)) / scale;
    }

    public double getInverseIntensity(double x) {
        throw new RuntimeException("unimplemented");
    }

    public int getNumArguments() {
        throw new RuntimeException("unimplemented");
    }

    public String getArgumentName(int n) {
        throw new RuntimeException("unimplemented");
    }

    public double getArgument(int n) {
        throw new RuntimeException("unimplemented");
    }

    public void setArgument(int n, double value) {
        throw new RuntimeException("unimplemented");
    }

    public double getLowerBound(int n) {
        throw new RuntimeException("unimplemented");
    }

    public double getUpperBound(int n) {
        throw new RuntimeException("unimplemented");
    }

    public DemographicFunction getCopy() {
        throw new RuntimeException("unimplemented");
    }
}
