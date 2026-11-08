import java.io.*;
import java.util.*;

public final class Main {
    @SuppressWarnings("unchecked")
    static Object handle(Map<String, Object> input) {
        switch ((String) input.get("task")) {
            case "ping": return Map.of("status", "ready");
            case "lcs": return Algorithms.lcs(input);
            case "merge": return Algorithms.merge(input);
            case "tree": return Algorithms.tree(input);
            case "naive": return Algorithms.naive(input);
            case "kmp": return Algorithms.kmp(input);
            case "rk": return Algorithms.rk(input);
            case "suffix": return Algorithms.suffix(input);
            case "aho": return Algorithms.aho(input);
            default: throw new IllegalArgumentException("Unknown task");
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                Map<String, Object> input = (Map<String, Object>) Json.parse(line);
                System.out.println(Json.write(handle(input)));
            } catch (UnsupportedOperationException e) {
                System.out.println(Json.write(Map.of("error", e.getMessage())));
            }
        }
    }
}
