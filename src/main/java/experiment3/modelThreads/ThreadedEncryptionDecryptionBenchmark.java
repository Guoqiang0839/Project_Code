package experiment3.modelThreads;

import org.openjdk.jmh.annotations.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ThreadedEncryptionDecryptionBenchmark {
    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();

    private static final String ALGORITHM = "AES";
    private static final byte[] keyValue = new byte[]{'T', 'h', 'i', 's', 'I', 's', 'A', 'S', 'e', 'c', 'r', 'e', 't', 'K', 'e', 'y'};
    private byte[][] encryptedDataBlocks;
    private byte[][] dataBlocks;


    @Param({"800000","850000","900000","950000","1000000"})
    private int dataSize;
    @Param({"1", "2", "4", "8", "16", "32"})
    private int numCores;


    @Setup(Level.Trial)
    public void setUp() {
        dataBlocks = generateData();
        encryptedDataBlocks = new byte[dataBlocks.length][];
    }

    @Setup
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void encryptData() throws InterruptedException {

        Thread[] threads = new Thread[numCores];

        for (int t = 0; t < numCores; t++) {
            final int startBlock = t * (dataBlocks.length / numCores);
            final int endBlock = (t == numCores - 1) ? dataBlocks.length : (t + 1) * (dataBlocks.length / numCores);

            threads[t] = new Thread(() -> {
                try {
                    Key key = new SecretKeySpec(keyValue, ALGORITHM);
                    Cipher cipher = Cipher.getInstance(ALGORITHM);
                    cipher.init(Cipher.ENCRYPT_MODE, key);
                    for (int i = startBlock; i < endBlock; i++) {
                        byte[] encryptedData = cipher.doFinal(dataBlocks[i]);
                        encryptedDataBlocks[i] = encryptedData;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads[t].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
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
