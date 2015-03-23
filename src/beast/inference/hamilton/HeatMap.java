package beast.inference.hamilton;

import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.Logger;
import beast.inference.loggers.MCLogger;
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
import beast.math.distributions.MultivariateNormalDistribution;

import java.util.Arrays;
import java.util.stream.IntStream;

public class HeatMap {

    public static void main(String... args) {

        double[] mu = {0.07447646, 0.9947984};
        double[][] Sigma = {{1.24064E-04, 0.0000562561}, {0.0000562561, 0.0026316385}};

        int L = 8;
        double epsilon = 0.01108163159885985384057;
        int M = 10000;

        int a = -32, b = 32, c = -32, d = 32;
        final double[][] ESS = new double[b - a + 1][d - c + 1];
        int[][] positions = new int[(b-a+1) * (d-c+1)][2];
        int k = 0;
        for (int i = a; i <= b; ++i)
            for (int j = c; j <= d; ++j)
                positions[k++] = new int[]{i, j};
        Arrays.stream(positions).parallel().forEach(ij -> {
            Parameter q = new Parameter.Default("q", 2);
            q.setParameterValue(0,1);
            q.setParameterValue(1,1);
            q.addBounds(new DefaultBounds(Double.POSITIVE_INFINITY, 0, 2));
            MultivariateDistributionLikelihood mvn = new MultivariateDistributionLikelihood(new MultivariateNormalDistribution(mu, Sigma, false));
            mvn.addData(q);
            Likelihood U = new CompoundLikelihood(Arrays.asList(
                    mvn,
                    new DummyModel(q)
            ));
            OperatorSchedule os = new SimpleOperatorSchedule();
            os.addOperator(new HamiltonUpdate(U, new Parameter[]{q}, new double[]{Math.pow(1.125,ij[0]), Math.pow(2, ij[1])}, epsilon, L, 1.0, 1.0, CoercionMode.COERCION_OFF));
            MCMC hmc = new MCMC("hmc");
            ArrayLogFormatter formatter = new ArrayLogFormatter(false);
            MCLogger logger = new MCLogger(formatter, 1, false);
            logger.add(U);
            hmc.init(new MCMCOptions(M), U, os, new Logger[]{logger});
            hmc.chain();
            ESS[ij[0] - a][ij[1] - c] = new TraceCorrelation<>(formatter.getTraces().get(1).getValues(M / 10 + 1, M), TraceType.DOUBLE, 1).getESS();
        });

        System.out.println(String.join(",", IntStream.range(c, d+1).mapToObj(Integer::toString).toArray(String[]::new)));
        for (int i = 0; i < ESS.length; ++i) {
            System.out.println((i + a) + "," + String.join(",", Arrays.stream(ESS[i]).mapToObj(Double::toString).toArray(String[]::new)));
        }
    }

}
