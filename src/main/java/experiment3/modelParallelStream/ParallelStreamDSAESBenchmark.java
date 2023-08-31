package experiment3.modelParallelStream;

import org.openjdk.jmh.annotations.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
public class ParallelStreamDSAESBenchmark {
    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();
    private static final String ALGORITHM = "AES";
    private static final byte[] keyValue =
            new byte[]{'T', 'h', 'i', 's', 'I', 's', 'A', 'S', 'e', 'c', 'r', 'e', 't', 'K', 'e', 'y'};
    private byte[][] dataBlocks;
    private byte[][] encryptedData;

    @Param({"800000","850000","900000","950000","1000000"})
    private int dataSize;
    @Param({"1", "2", "4", "8", "16", "32"})
    private int numCores;

    @Setup(Level.Trial)
    public void setUp() {
        dataBlocks = generateData();
        encryptedData = new byte[dataBlocks.length][];
    }

    @Setup(Level.Iteration)
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(numCores));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void encryptData() {
        IntStream.range(0, dataBlocks.length).parallel().forEach(i -> {
            try {
                Key key = new SecretKeySpec(keyValue, ALGORITHM);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                encryptedData[i] = cipher.doFinal(dataBlocks[i]);
            } catch (Exception e) {
               e.printStackTrace();
            }
        });
    }

    private byte[][] generateData() {
        byte[][] data = new byte[dataSize][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new byte[1024];
            Arrays.fill(data[i], (byte) i);
        }
        return data;
    }
}
