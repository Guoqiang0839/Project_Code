package experiment2.modelExecutors;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.*;

@State(Scope.Benchmark)
public class ExecutorsWordCounterBenchmark {

    private static final int CORES_MAX = Runtime.getRuntime().availableProcessors();

    @Param({"10000000","15000000","20000000","25000000","30000000"})
    public int word_count;

    @Param({"1", "2", "4", "8", "16", "32"})
    public int numCores;

    private List<String> words;
    Map<String, Integer> wordCount;

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
        ExecutorService executor = Executors.newFixedThreadPool(numCores);
        int wordsPerThread = words.size() / numCores;

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numCores; i++) {
            int start = i * wordsPerThread;
            int end = (i == numCores - 1) ? words.size() : (i + 1) * wordsPerThread;

            Future<?> future = executor.submit(new WordCounterTask(words, start, end, wordCount));
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
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

    class WordCounterTask implements Runnable {

        private final List<String> words;
        private final int start;
        private final int end;
        private final Map<String, Integer> wordCount;

        WordCounterTask(List<String> words, int start, int end, Map<String, Integer> wordCount) {
            this.words = words;
            this.start = start;
            this.end = end;
            this.wordCount = wordCount;
        }

        @Override
        public void run() {
            for (int i = start; i < end; i++) {
                wordCount.merge(words.get(i), 1, Integer::sum);
            }
        }
    }
}
