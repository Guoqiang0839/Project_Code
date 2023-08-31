package experiment2.modelThreads;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class ThreadWordCounterBenchmark {
    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();

    @Param({"10000000","15000000","20000000","25000000","30000000"})
    private int word_count;

    @Param({"1", "2", "4", "8", "16", "32"})
    private int numCores;
    private List<String> words;
    Map<String, Integer> wordCount;
    private static final Object LOCK = new Object();




    @Setup
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
    }
    @Setup
    public void setup() {
        Random random = new Random(12345);
        words = new ArrayList<>(word_count);
        for (int i = 0; i < word_count; i++) {
            String word = generateRandomWord(random);
            words.add(word);
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void countWords() throws InterruptedException {
        wordCount = new ConcurrentHashMap<>();
        int numThreads = numCores;
        Thread[] threads = new Thread[numThreads];

        for (int t = 0; t < numThreads; t++) {
            int start = t * (words.size() / numThreads);
            int end = (t == numThreads - 1) ? words.size() : (t + 1) * (words.size() / numThreads);

            threads[t] = new Thread(() -> {
                for (int i = start; i < end; i++) {
                    wordCount.merge(words.get(i), 1, Integer::sum);
                }
            });
            threads[t].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }


    private String generateRandomWord(Random random) {
        int wordLength = random.nextInt(3) + 1;
        StringBuilder word = new StringBuilder(wordLength);

        for (int i = 0; i < wordLength; i++) {
            char letter = (char) ('a' + random.nextInt(26));
            word.append(letter);
        }

        return word.toString();
    }
}
