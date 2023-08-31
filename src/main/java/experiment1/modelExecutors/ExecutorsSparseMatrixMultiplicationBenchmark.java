package experiment1.modelExecutors;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ExecutorsSparseMatrixMultiplicationBenchmark {
    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();
    List<List<SparseMatrixEntry>> matrixRows;
    double[][] denseMatrix;
    private  ExecutorService executorService;
    @Param({"5000","6000","7000","8000","9000","10000"})
    private int intialSize;
    @Param({"1", "2", "4", "8", "16", "32"})
    private static int numCores;

    @Setup(Level.Trial)
    public void setupExecutors() {
        executorService = Executors.newFixedThreadPool(numCores);
    }

    @Setup
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
    }
    @Setup(Level.Trial)
    public void setup() {
        // Initialize the matrixRows and denseMatrix
        Random random = new Random(12345);
        matrixRows = new ArrayList<>();
        for (int i = 0; i < intialSize; i++) {
            List<SparseMatrixEntry> rowEntries = new ArrayList<>();
            for (int j = 0; j < intialSize; j++) {
                if (random.nextDouble() < 0.01) {  // 1% of cells are non-zero
                    rowEntries.add(new SparseMatrixEntry(i, j, random.nextDouble()));
                }
            }
            matrixRows.add(rowEntries);
        }

        denseMatrix = new double[intialSize][intialSize];
        for (int i = 0; i < denseMatrix.length; i++) {
            for (int j = 0; j < denseMatrix[i].length; j++) {
                denseMatrix[i][j] = random.nextDouble();
            }
        }
    }

    static class MultiplyTask implements Callable<double[]> {
        List<SparseMatrixEntry> rowEntries;
        double[][] denseMatrix;

        MultiplyTask(List<SparseMatrixEntry> rowEntries, double[][] denseMatrix) {
            this.rowEntries = rowEntries;
            this.denseMatrix = denseMatrix;
        }

        @Override
        public double[] call() {
            double[] result = new double[denseMatrix[0].length];
            for (SparseMatrixEntry entry : rowEntries) {
                for (int j = 0; j < denseMatrix[0].length; j++) {
                    result[j] += entry.value * denseMatrix[entry.col][j];
                }
            }
            return result;
        }
    }

    @Benchmark
    public double[][] multiply() throws InterruptedException, ExecutionException {
        int numRows = matrixRows.size();
        double[][] result = new double[numRows][denseMatrix[0].length];

        List<Future<double[]>> futures = new ArrayList<>();
        for (List<SparseMatrixEntry> rowEntries : matrixRows) {
            futures.add(executorService.submit(new MultiplyTask(rowEntries, denseMatrix)));
        }

        for (int i = 0; i < numRows; i++) {
            result[i] = futures.get(i).get();
        }

        return result;
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
