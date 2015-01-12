/*
 * LogisticGrowth.java
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
 * This class models logistic growth.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: LogisticGrowth.java,v 1.15 2005/05/24 20:25:56 rambaut Exp $
 */
public class LogisticGrowth extends ExponentialGrowth {

    /**
     * Construct demographic model with default settings
     */
    public LogisticGrowth(Type units) {

        super(units);
    }

    public void setShape(double value) {
        c = value;
    }

    public double getShape() {
        return c;
    }

    public void setRespectingShape(boolean b) {
        respectShape = b;
    }

    public boolean respectingShape() {
        return respectShape;
    }

    double lowLimit = 0; // 1e-6;

    /**
     * An alternative parameterization of this model. This
     * function sets the time at which there is a 0.5 proportion
     * of N0.
     *
     * The general form for any k where tk is the time at which Nt = N0/k:
     *		c = (k - 1.0) / (exp(r * tk) - k);
     */
    public void setTime50(double time50) {
        c = 1.0 / (Math.exp(getGrowthRate() * time50) - 2.0);
    }

    public double getTime50() {
        return Math.log(1/c + 2) / getGrowthRate();
    }

    public void setRespectTime50(boolean b) {
        respectTime50 = b;
    }

    public boolean respectingTime50() {
        return respectTime50;
    }

    protected double getTime50ChainRule(boolean respectGrowthRate) {

        final double t50 = getTime50();
        final double r = getGrowthRate();
        final double ert50 = Math.exp(r * t50);
        final double ert50m2 = ert50 - 2;
        final double ert50m2Squared = ert50m2 * ert50m2;

        if (!respectGrowthRate)
            return - r * ert50 / ert50m2Squared;
        else
            return -t50 * ert50 / ert50m2Squared;

    }

    public void setShapeFromTimeAtAlpha(double time, double alpha) {

        // New parameterization of logistic shape to be the time at which the
        // population reached some proportion alpha:
        double ert = Math.exp(-getGrowthRate() * time);
        c = ((1.0 - alpha) * ert) / (ert - alpha);
    }
    // Implementation of abstract methods

    /**
     * Gets the value of the demographic function N(t) at time t.
     *
     * @param t the time
     * @return the value of the demographic function N(t) at time t.
     */
    public double getDemographic(double t) {

        double nZero = getN0();
        double r = getGrowthRate();
        double c = getShape();

//		return nZero * (1 + c) / (1 + (c * Math.exp(r*t)));
//		AER rearranging this to use exp(-rt) may help
// 		with some overflow situations...

        double expOfMRT = Math.exp(-r * t);
        return lowLimit + (nZero * (1 + c) * expOfMRT) / (c + expOfMRT);
    }

    public double getLogDemographic(double t) {
        final double d = getDemographic(t);
        if( d == 0.0 && lowLimit == 0.0 ) {
            double nZero = getN0();
            double r = getGrowthRate();
            double c = getShape();
            int sign = c > 0 ? 1 : -1;

            final double v1 = Math.log(c * sign) + r * t;
            double ld = Math.log(nZero);
            if( v1 < 600 ) {
                double v = sign * Math.exp(v1);

                if( c > -1 ) {
                    ld += Math.log1p(c) - Math.log1p(v);
                } else {
                    ld += Math.log((1+c)/(1+v));
                }
            } else {
                 ld += Math.log1p(c) - sign * v1;
            }
            return ld;
        }
//        if(  ! (Math.abs(Math.log(d) - ld) < 1e-12) ) {
//           return Math.log(d);
//        }
        return Math.log(d);
    }
    /**
     * Returns value of demographic intensity function at time t
     * (= integral 1/N(x) dx from 0 to t).
     */
    public double getIntensity(double t) {

        double nZero = getN0();
        double r = getGrowthRate();
        double c = getShape();

        double ert = Math.exp(r * t);
        if( lowLimit == 0 ) {
       // double emrt = Math.exp(-r * t);
          return (c * (ert - 1)/r + t)  / ((1+c) * nZero);
        }
        double z = lowLimit;
        return (r*t*z + (1 + c)*nZero*Math.log(nZero + c*nZero + z + c*ert*z))/(r*z*(nZero + c*nZero + z));
    }

    public double getDifferentiatedIntensity(double t) {

        double nZero = getN0();
        double r = getGrowthRate();
        double c = getShape();
        double rt = r * t;
        double ert = Math.exp(rt);
        double cp1 = c + 1;

        if (respectingN0()) {

            if( lowLimit == 0 ) {
                // double emrt = Math.exp(-r * t);
                return - (c * (ert - 1) + rt) / ((c + 1) * nZero * nZero * r);
            }
            double z = lowLimit;
            double cnZeropzpnZero = c * nZero + z + nZero;
            double czert = c * z * ert;
            double cnZeropzpnZeropczert = cnZeropzpnZero + czert;
            return cp1 * cp1 * nZero / (r * z * cnZeropzpnZero * cnZeropzpnZeropczert)
                    - cp1 * (rt - Math.log(cnZeropzpnZeropczert)) / (r * cnZeropzpnZero * cnZeropzpnZero);

        } else if (respectingGrowthRate()) {

            double deriv = 0.0;

            if (lowLimit == 0) {
                deriv += c * (ert * (rt - 1) + 1) / (cp1 * nZero * r * r);
                if (respectingTime50())
                    deriv += -(rt - ert + 1) / (cp1 * cp1 * r * nZero) * getTime50ChainRule(true);
            } else {

                double z = lowLimit;
                double cnZeropzpnZero = c * nZero + z + nZero;
                double czert = c * z * ert;
                double cnZeropzpnZeropczert = cnZeropzpnZero + czert;
                double logcnZeropzpnZeropczert = Math.log(cnZeropzpnZeropczert);
                double rzcnZeropzpnZero = r * z * cnZeropzpnZero;

                deriv += cp1 * nZero * (rt - logcnZeropzpnZeropczert) / (r * rzcnZeropzpnZero)
                        - cp1 * nZero * t / (r * z * cnZeropzpnZeropczert);

                if (respectingTime50()) {
                    deriv += ((cp1 * nZero * (nZero + z*ert) / cnZeropzpnZeropczert + nZero * logcnZeropzpnZeropczert) / rzcnZeropzpnZero
                            - nZero * (cp1 * nZero * logcnZeropzpnZeropczert + rt * z) / (rzcnZeropzpnZero * cnZeropzpnZero)) * getTime50ChainRule(true);
                }

            }

            if (respectingDoublingTime()) deriv *= getDoublingTimeChainRule();
            return deriv;

        } else if (respectingShape()) {

            double deriv = 1.0;
            if (respectingTime50()) deriv *= getTime50ChainRule(false);

            if( lowLimit == 0 ) {
                // double emrt = Math.exp(-r * t);
                deriv *= - (rt - ert + 1) / (cp1 * cp1 * r * nZero);
            } else {
                double z = lowLimit;
                double cnZeropzpnZero = c * nZero + z + nZero;
                double czert = c * z * ert;
                double cnZeropzpnZeropczert = cnZeropzpnZero + czert;
                double logcnZeropzpnZeropczert = Math.log(cnZeropzpnZeropczert);
                double rzcnZeropzpnZero = r * z * cnZeropzpnZero;
                deriv *= (cp1 * nZero * (nZero + z*ert) / cnZeropzpnZeropczert + nZero * logcnZeropzpnZeropczert) / rzcnZeropzpnZero
                        - nZero * (cp1 * nZero * logcnZeropzpnZeropczert + rt * z) / (rzcnZeropzpnZero * cnZeropzpnZero);
            }

            return deriv;

        } else {
            return 0;
        }

    }

    /**
     * Returns value of demographic intensity function at time t
     * (= integral 1/N(x) dx from 0 to t).
     */
    public double getInverseIntensity(double x) {

        throw new RuntimeException("Not implemented!");
    }

    public double getIntegral(double start, double finish) {
        if( lowLimit > 0 ) {
            double v1 = getNumericalIntegral(start, finish);
            final double v2 = getIntensity(finish) - getIntensity(start);
            return v2;
        }
        double intervalLength = finish - start;

        double nZero = getN0();
        double r = getGrowthRate();
        double c = getShape();
        double expOfMinusRT = Math.exp(-r * start);
        double expOfMinusRG = Math.exp(-r * intervalLength);

        double term1 = nZero * (1.0 + c);
        if (term1 == 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        double term2 = c * (1.0 - expOfMinusRG);

        double term3 = (term1 * expOfMinusRT) * r * expOfMinusRG;
        double term2over3;
        if (term3 == 0.0) {
            double l1 = expOfMinusRG < 1e-8 ?  -r * intervalLength : Math.log1p(expOfMinusRG);
            final int sign = c > 0 ? 1 : -1;
            term2over3 = (sign/term1) * Math.exp(l1 + r * start + Math.log(c*sign) - Math.log(r));

           // throw new RuntimeException("Infinite integral!");
        } else {
//            if (term3 != 0.0 && term2 == 0.0) {
//                term2over3 = 0.0;
//            } else if (term3 == 0.0 && term2 == 0.0) {
//                throw new RuntimeException("term3 and term2 are both zeros. N0=" + getN0() + " growthRate=" + getGrowthRate() + "c=" + c);
//            } else {
                term2over3 = term2 / term3;
//            }
        }

        final double term5 = intervalLength / term1;

//        double v0 = 1/term1 * (finish + c * (Math.exp(r*finish) - 1) /r);
//        double v1 = 1/term1 * (start + c * (Math.exp(r*start) - 1) /r);
//        double v =  1/term1 * ((finish + c * (Math.exp(r*finish) - 1) /r)  - (start + c * (Math.exp(r*start) - 1) /r));
//        double v2 = 1/term1 * ((finish-start) + (c * (Math.exp(r*finish) - 1) /r)  - (c * (Math.exp(r*start) - 1) /r));
//        double v3 = 1/term1 * ((finish-start) + (c/r)* ( Math.exp(r*finish) - 1)   - (c/r) * (Math.exp(r*start) - 1) );
//        double v4 =  1/term1 * ((finish-start) + (c/r) * (Math.exp(r*finish) - Math.exp(r*start) ) );
       // double v = ( (c * (Math.exp(r*finish) - Math.exp(r*start)) / r) + (start - finish)) / term1;

        return term5 + term2over3;
    }

    public double getDifferentiatedIntegral(double start, double finish) {
        if( lowLimit > 0 ) {
            double v1 = getNumericalIntegral(start, finish);
            final double v2 = getDifferentiatedIntensity(finish) - getDifferentiatedIntensity(start);
            return v2;
        }

        double intervalLength = finish - start;

        double nZero = getN0();
        double r = getGrowthRate();
        double c = getShape();
        double cp1 = c + 1;
        double expOfMinusRT = Math.exp(-r * start);
        double expOfMinusRG = Math.exp(-r * intervalLength);

        double term1 = nZero * cp1;
        if (term1 == 0.0) {
            return 0.0;
        }

        boolean term3Is0 = term1 * expOfMinusRT * r * expOfMinusRG == 0;

        if (respectingN0()) {

            if (!term3Is0) {
                return (c * (Math.exp(r*start) - Math.exp(r * (intervalLength + start))) - intervalLength * r)
                        / (cp1 * nZero * nZero * r);
            } else if (expOfMinusRG < 1e-8) {
                double rintervalLength = r * intervalLength;
                return - cp1 * Math.exp(-rintervalLength) * (c * Math.exp(r * start) + rintervalLength * Math.exp(rintervalLength))
                        / (nZero * nZero * r);
            } else {
                double rintervalLength = r * intervalLength;
                return - cp1 * (c * (Math.exp(-rintervalLength) + 1) * Math.exp(r * start) + rintervalLength)
                        / (nZero * nZero * r);
            }

        } else if (respectingGrowthRate()) {

            double deriv = 0.0;

            if (!term3Is0) {

                double rintervalLength = r * intervalLength;
                double erintervalLength = Math.exp(rintervalLength);
                deriv += c * Math.exp(r*start) * ((erintervalLength - 1) * (r * start - 1) + rintervalLength * erintervalLength)
                        / (cp1 * nZero * r * r);

                if (respectingTime50())
                    deriv += Math.exp(r * (intervalLength + start) - r * intervalLength - Math.exp(r * start))
                            / (cp1 * cp1 * nZero * r) * getTime50ChainRule(true);

            } else if (expOfMinusRG < 1e-8) {

                double rintervalLength = r * intervalLength;
                double rstart = r * start;
                deriv += - c * cp1 * Math.exp(rstart - rintervalLength) * (rintervalLength - rstart + 1) / (nZero * r * r);
                if (respectingTime50())
                    deriv += Math.exp(-rintervalLength) * ((2*c + 1) * Math.exp(r * start) + rintervalLength * Math.exp(rintervalLength))
                            / (nZero * r) * getTime50ChainRule(true);

            } else {

                double rintervalLength = r * intervalLength;
                double rstart = r * start;
                double term = Math.exp(rintervalLength) * (rstart - 1) - rintervalLength * rstart - 1;
                deriv += c * Math.exp(r * (start - rintervalLength)) * (c + 1) * term / (nZero * r * r);
                if (respectingTime50())
                    deriv += Math.exp(-rintervalLength) * ((2*c + 1) * (Math.exp(r * (intervalLength + start)) + Math.exp(r * start)) + rintervalLength * Math.exp(rintervalLength))
                            / (nZero * r) * getTime50ChainRule(true);

            }

            if (respectingDoublingTime()) deriv *= getDoublingTimeChainRule();
            return deriv;

        } else if (respectingShape()) {

            double deriv = 1.0;
            if (respectingTime50()) deriv *= getTime50ChainRule(false);

            if (!term3Is0) {
                deriv *= Math.exp(r * (intervalLength + start) - r * intervalLength - Math.exp(r * start))
                        / (cp1 * cp1 * nZero * r);
            } else if (expOfMinusRG < 1e-8) {
                double rintervalLength = r * intervalLength;
                deriv *= Math.exp(-rintervalLength) * ((2*c + 1) * Math.exp(r * start) + rintervalLength * Math.exp(rintervalLength))
                        / (nZero * r);
            } else {
                double rintervalLength = r * intervalLength;
                deriv *= Math.exp(-rintervalLength) * ((2*c + 1) * (Math.exp(r * (intervalLength + start)) + Math.exp(r * start)) + rintervalLength * Math.exp(rintervalLength))
                        / (nZero * r);
            }

            return deriv;

        } else {
            return 0;
        }
        
    }

    public int getNumArguments() {
        return 3;
    }

    public String getArgumentName(int n) {
        switch (n) {
            case 0:
                return "N0";
            case 1:
                return "r";
            case 2:
                return "c";
        }
        throw new IllegalArgumentException("Argument " + n + " does not exist");
    }

    public double getArgument(int n) {
        switch (n) {
            case 0:
                return getN0();
            case 1:
                return getGrowthRate();
            case 2:
                return getShape();
        }
        throw new IllegalArgumentException("Argument " + n + " does not exist");
    }

    public void setArgument(int n, double value) {
        switch (n) {
            case 0:
                setN0(value);
                break;
            case 1:
                setGrowthRate(value);
                break;
            case 2:
                setShape(value);
                break;
            default:
                throw new IllegalArgumentException("Argument " + n + " does not exist");

        }
    }

    public double getLowerBound(int n) {
        return 0.0;
    }

    public double getUpperBound(int n) {
        return Double.POSITIVE_INFINITY;
    }

    public DemographicFunction getCopy() {
        LogisticGrowth df = new LogisticGrowth(getUnits());
        df.setN0(getN0());
        df.setGrowthRate(getGrowthRate());
        df.c = c;

        return df;
    }

    //
    // private stuff
    //

    private double c;
    private boolean respectShape;
    private boolean respectTime50;
}
