package beast.inference.hamilton;

import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.MCLogger;
import beast.inference.mcmc.MCMC;
import beast.inference.trace.Trace;
import beast.xml.IntegerAttribute;
import beast.xml.ObjectElement;
import beast.xml.Parseable;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.Arrays;
import java.util.List;

/**
 * @author Arman Bilge
 */
public class MassMatrix {

    final ArrayLogFormatter f;
    final int start, stop;

    @Parseable
    public MassMatrix(@ObjectElement(name = "analysis") MCMC mcmc, @IntegerAttribute(name="logger") int l, @IntegerAttribute(name="start") int start, @IntegerAttribute(name="stop") int stop) {
        f = new ArrayLogFormatter(false);
        ((MCLogger) mcmc.getLoggers()[l]).addFormatter(f);
        this.start = start;
        this.stop = stop;
        mcmc.run();
    }

    public double[] getMassMatrix() {

        double[][] covar = new double[stop - start][];
        List<Trace> traces = f.getTraces().subList(start, stop);
        for (int i = 0; i < traces.size(); ++i) {
            int j = 0;
            covar[i] = new double[traces.get(i).getValuesSize() - traces.get(i).getValuesSize() / 10];
            for (Object d : traces.get(i).getValues(traces.get(i).getValuesSize() / 10, traces.get(i).getValuesSize()))
                covar[i][j++] = (Double) d;
        }
        Covariance C = new Covariance(new BlockRealMatrix(covar).transpose());
        final RealMatrix M = C.getCovarianceMatrix();
        final RealMatrix Minv = MatrixUtils.inverse(M);
        return Arrays.stream(Minv.getData()).flatMapToDouble(Arrays::stream).map(d->Math.round(d * 1000000)/1000000).toArray();

    }

}
