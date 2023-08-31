package experiment2.modelParallelStream;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ParallelStreamWordCounterBenchmark {

    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();

    @Param({"10000000","15000000","20000000","25000000","30000000"})
    public int word_count;

    @Param({"1", "2", "4", "8", "16", "32"})
    public int numCores;

    private List<String> words;
    private final Map<String, Integer> wordCount = new ConcurrentHashMap<>();

    @Setup(Level.Trial)
    public void prepare() {
        if (numCores > CORES_MAX) {
            throw new IllegalArgumentException("The number of cores is more than the available processors.");
        }
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(numCores));
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
    public void countWords() {
        words.parallelStream().forEach(word -> {
            wordCount.merge(word, 1, Integer::sum);
        });
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
