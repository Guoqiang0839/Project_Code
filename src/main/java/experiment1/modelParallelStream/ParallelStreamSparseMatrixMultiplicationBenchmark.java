package experiment1.modelParallelStream;
import org.openjdk.jmh.annotations.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ParallelStreamSparseMatrixMultiplicationBenchmark {
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
    @Setup
    public void setCore(){
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(numCores)); // set the parallelism level to the needed
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

        IntStream.range(0, numRows).parallel().forEach(row -> {
            List<SparseMatrixEntry> entries = matrixRows.get(row);
            for (SparseMatrixEntry entry : entries) {
                for (int j = 0; j < numCols; j++) {
                    result[entry.row][j] += entry.value * denseMatrix[entry.col][j];
                }
            }
        });

        return result;
    }
}