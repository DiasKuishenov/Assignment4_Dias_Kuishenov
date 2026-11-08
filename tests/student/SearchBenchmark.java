import java.util.*;

// Run with Java 17 after compiling Algorithms.java. No external libraries.
public class SearchBenchmark {
    static long checksum = 0;

    static Map<String, Object> run(String algorithm, Map<String, Object> data) {
        if (algorithm.equals("naive")) return Algorithms.naive(data);
        if (algorithm.equals("kmp")) return Algorithms.kmp(data);
        return Algorithms.rk(data);
    }

    static void bench(String type, int n, int m) {
        Random rng = new Random(20261108L + n * 1000L + m);
        String text, pattern;
        if (type.equals("repetitive")) {
            text = "a".repeat(n);
            pattern = "a".repeat(m);
        } else {
            StringBuilder a = new StringBuilder(), b = new StringBuilder();
            for (int i = 0; i < n; i++) a.append("abcde".charAt(rng.nextInt(5)));
            for (int i = 0; i < m; i++) b.append("abcde".charAt(rng.nextInt(5)));
            text = a.toString();
            pattern = b.toString();
        }
        Map<String, Object> data = Map.of("text", text, "pattern", pattern);
        String[] methods = {"naive", "kmp", "rk"};
        Object correct = run(methods[0], data).get("positions");
        for (String method : methods) {
            if (!correct.equals(run(method, data).get("positions")))
                throw new AssertionError("Search outputs differ for " + type + " n=" + n);
        }
        for (String method : methods) {
            for (int i = 0; i < 150; i++)
                checksum += ((List<?>) run(method, data).get("positions")).size();
            double[] times = new double[3];
            int repetitions = 100;
            for (int sample = 0; sample < 3; sample++) {
                long before = System.nanoTime();
                for (int i = 0; i < repetitions; i++)
                    checksum += ((List<?>) run(method, data).get("positions")).size();
                long elapsed = System.nanoTime() - before;
                times[sample] = elapsed / (repetitions * 1_000_000.0);
            }
            double[] sorted = times.clone();
            Arrays.sort(sorted);
            System.out.printf(Locale.US, "%s,%d,%d,20261108+%d*1000+%d,%s,%.6f,%.6f,%.6f,%.6f,true%n",
                    type, n, m, n, m, method, times[0], times[1], times[2], sorted[1]);
        }
    }

    public static void main(String[] args) {
        System.out.println("case,n,m,seed,algorithm,run1_ms,run2_ms,run3_ms,median_ms,correct");
        for (String type : new String[]{"random", "repetitive"}) {
            bench(type, 300, 15);
            bench(type, 1200, 40);
            bench(type, 3000, 90);
        }
        System.err.println("Checksum: " + checksum);
    }
}
