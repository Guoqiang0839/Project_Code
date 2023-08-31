package experiment2.modelForkJoin;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ForkJoinWordCounterBenchmark {

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
    public void countWords() {
        wordCount = new ConcurrentHashMap<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool(numCores);
        int wordsPerThread = words.size() / numCores;

        List<WordCounterTask> tasks = new ArrayList<>();

        for (int i = 0; i < numCores; i++) {
            int start = i * wordsPerThread;
            int end = (i == numCores - 1) ? words.size() : (i + 1) * wordsPerThread;

            WordCounterTask task = new WordCounterTask(words, start, end, wordCount);
            tasks.add(task);
        }

        for (WordCounterTask task : tasks) {
            forkJoinPool.execute(task);
        }

        for (WordCounterTask task : tasks) {
            task.join();
        }

        forkJoinPool.shutdown();
    }



    class WordCounterTask extends RecursiveAction {

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
        protected void compute() {
            for (int i = start; i < end; i++) {
                wordCount.merge(words.get(i), 1, Integer::sum);
            }
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
