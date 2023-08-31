package experiment1.modelForkJoin;

import org.openjdk.jmh.annotations.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ForkJoinSparseMatrixMultiplicationBenchmark {
    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();
    List<List<SparseMatrixEntry>> matrixRows;
    double[][] denseMatrix;
    private ForkJoinPool POOL;
    @Param({"5000","6000","7000","8000","9000","10000"})
    private int intialSize;
    @Param({"1", "2", "4", "8", "16", "32"})
    private static int numCores;

    @Setup(Level.Trial)
    public void setupPool() {
        POOL = new ForkJoinPool(numCores);
    }

    @Setup
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
    }



    @Setup(Level.Trial)
    public void setup() {
        // Initialize the matrixRows and denseMatrix here
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

    static class MultiplyTask extends RecursiveTask<double[]> {
        List<SparseMatrixEntry> rowEntries;
        double[][] denseMatrix;

        MultiplyTask(List<SparseMatrixEntry> rowEntries, double[][] denseMatrix) {
            this.rowEntries = rowEntries;
            this.denseMatrix = denseMatrix;
        }

        @Override
        protected double[] compute() {
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
    public double[][] multiply() {
        int numRows = matrixRows.size();
        double[][] result = new double[numRows][denseMatrix[0].length];

        List<MultiplyTask> tasks = new ArrayList<>();
        for (List<SparseMatrixEntry> rowEntries : matrixRows) {
            MultiplyTask task = new MultiplyTask(rowEntries, denseMatrix);
            POOL.execute(task);
            tasks.add(task);
        }

        for (int i = 0; i < numRows; i++) {
            result[i] = tasks.get(i).join();
        }

        return result;
    }

    @TearDown(Level.Trial)
    public void tearDownPool() {
        POOL.shutdown();
    }

}
