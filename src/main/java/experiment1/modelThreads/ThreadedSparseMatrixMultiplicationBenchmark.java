package experiment1.modelThreads;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ThreadedSparseMatrixMultiplicationBenchmark {
    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();
    List<List<SparseMatrixEntry>> matrixRows;
    double[][] denseMatrix;
    @Param({"5000","6000","7000","8000","9000","10000"})
    private int intialSize;
    @Param({"1", "2", "4", "8", "16", "32"})
    private int numCores;
    @Setup
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
    }
    @Setup(Level.Trial)
    public void setup() {
        // Initialize the sparseMatrix and denseMatrix here
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

    @Benchmark
    public double[][] multiply() {
        int numRows = matrixRows.size();
        int numCols = denseMatrix[0].length;
        double[][] result = new double[numRows][numCols];

        Thread[] threads = new Thread[numCores];

        int rowsPerThread = numRows / numCores;
        for (int threadNum = 0; threadNum < numCores; threadNum++) {
            final int startRow = threadNum * rowsPerThread;
            final int endRow = (threadNum == numCores - 1) ? numRows : (threadNum + 1) * rowsPerThread;

            threads[threadNum] = new Thread(() -> {
                for (int row = startRow; row < endRow; row++) {
                    List<SparseMatrixEntry> entries = matrixRows.get(row);
                    for (SparseMatrixEntry entry : entries) {
                        for (int j = 0; j < numCols; j++) {
                            result[entry.row][j] += entry.value * denseMatrix[entry.col][j];
                        }
                    }
                }
            });

            threads[threadNum].start();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }
}
