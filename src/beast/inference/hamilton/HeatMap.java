package beast.inference.hamilton;

import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.Logger;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.mcmc.MCMC;
import beast.inference.mcmc.MCMCOptions;
import beast.inference.model.CompoundLikelihood;
import beast.inference.model.DummyModel;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.model.Parameter.DefaultBounds;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.prior.PriorParsers.MultivariateDistributionLikelihood;
import beast.inference.trace.TraceCorrelation;
import beast.inference.trace.TraceFactory.TraceType;
import beast.math.MathUtils;
import beast.math.distributions.MultivariateNormalDistribution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.IntStream;

public class HeatMap {

    public static void main(String... args) throws FileNotFoundException {

//        MathUtils.setSeed(666);

        double[] mu = {0.07447646, 0.9947984};
        double[][] Sigma = {{1.24064E-04, 0.0000562561}, {0.0000562561, 0.0026316385}};

        int L = 8;
        double epsilon = 0.01108163159885985384057;
        int M = 10000;

        int a = -16, b = 16, c = -16, d = 16;
        final double[][] ESS = new double[b - a + 1][d - c + 1];
        int[][] positions = new int[(b-a+1) * (d-c+1)][2];
        int k = 0;
        for (int i = a; i <= b; ++i)
            for (int j = c; j <= d; ++j)
                positions[k++] = new int[]{i, j};

        final double base = 1.25;

        Arrays.stream(positions)
//                .parallel()
                .forEach(ij -> {
            Parameter q = new Parameter.Default("q", 2);
//            q.setParameterValue(0, 0.07447646);
//            q.setParameterValue(1, 0.9947984);
            q.setParameterValue(0, 0.0);
            q.setParameterValue(1, 0.0);
            q.addBounds(new DefaultBounds(Double.POSITIVE_INFINITY, 0, 2));
            MultivariateDistributionLikelihood mvn = new MultivariateDistributionLikelihood(new MultivariateNormalDistribution(mu, Sigma, false));
            mvn.addData(q);
            Likelihood U = new CompoundLikelihood(Arrays.asList(
                    mvn,
                    new DummyModel(q)
            ));
            OperatorSchedule os = new SimpleOperatorSchedule();
            os.addOperator(new HamiltonUpdate(U, new Parameter[]{q}, new double[]{Math.pow(base, ij[0]), Math.pow(base, ij[1])}, epsilon, L, 1.0, 1.0, CoercionMode.COERCION_OFF));
            MCMC hmc = new MCMC("hmc");
            TabDelimitedFormatter tabFormatter = null;
            try {
                tabFormatter = new TabDelimitedFormatter(new FileOutputStream(new File(ij[0] + "," + ij[1] + ".log")));
            } catch (FileNotFoundException e) {
                System.exit(-1);
            }
            MCLogger tabLogger = new MCLogger(tabFormatter, 1, false);
            tabLogger.add(U);
            ArrayLogFormatter arrayFormatter = new ArrayLogFormatter(false);
            MCLogger arrayLogger = new MCLogger(arrayFormatter, 1, false);
            arrayLogger.add(U);
            hmc.init(new MCMCOptions(M), U, os, new Logger[]{
//                    tabLogger,
                    arrayLogger});
            hmc.chain();
            ESS[ij[0] - a][ij[1] - c] = new TraceCorrelation<>(arrayFormatter.getTraces().get(1).getValues(M / 10 + 1, M), TraceType.DOUBLE, 1).getESS();
        });

        final PrintWriter pw = new PrintWriter(new File("ess.csv"));
        pw.println(String.join(",", IntStream.range(c, d + 1).mapToObj(Integer::toString).toArray(String[]::new)));
        for (int i = 0; i < ESS.length; ++i) {
            pw.println((i + a) + "," + String.join(",", Arrays.stream(ESS[i]).mapToObj(Double::toString).toArray(String[]::new)));
        }
        pw.close();
    }

}
