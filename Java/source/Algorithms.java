import java.util.*;

public final class Algorithms {
    // Longest common subsequence using a table for prefixes.
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder answer = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                answer.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return Map.of("length", dp[n][m], "sequence", answer.reverse().toString());
    }

    // Minimum cost of merging neighbouring blocks, with a chronological plan.
    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> values = (List<?>) input.get("sizes");
        int n = values.size();
        List<int[]> plan = new ArrayList<>();
        if (n <= 1) return Map.of("cost", 0L, "plan", plan);
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + ((Number) values.get(i)).longValue();
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int l = 0; l + length <= n; l++) {
                int r = l + length - 1;
                dp[l][r] = Long.MAX_VALUE;
                long sum = prefix[r + 1] - prefix[l];
                for (int k = l; k < r; k++) {
                    long cost = dp[l][k] + dp[k + 1][r] + sum;
                    if (cost < dp[l][r]) {
                        dp[l][r] = cost;
                        split[l][r] = k;
                    }
                }
            }
        }
        buildMergePlan(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    private static void buildMergePlan(int l, int r, int[][] split, List<int[]> plan) {
        if (l == r) return;
        int k = split[l][r];
        buildMergePlan(l, k, split, plan);
        buildMergePlan(k + 1, r, split, plan);
        plan.add(new int[]{l, k, r});
    }

    // Tree DP: choose vertices with no chosen neighbours.
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        if (n == 0) return Map.of("weight", 0L, "selected", selected);

        List<?> weights = (List<?>) input.get("weights");
        List<?> edges = (List<?>) input.get("edges");
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (Object edge : edges) {
            List<?> e = (List<?>) edge;
            int u = ((Number) e.get(0)).intValue();
            int v = ((Number) e.get(1)).intValue();
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // Build an order without recursion to handle very long chains.
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        int[] order = new int[n];
        order[0] = 0;
        int count = 1;
        for (int i = 0; i < count; i++) {
            int v = order[i];
            for (int u : graph.get(v)) {
                if (u == parent[v]) continue;
                parent[u] = v;
                order[count++] = u;
            }
        }

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int i = 0; i < n; i++) take[i] = ((Number) weights.get(i)).longValue();
        // Children come before parents in this reverse order.
        for (int i = n - 1; i > 0; i--) {
            int v = order[i];
            int p = parent[v];
            take[p] += skip[v];
            skip[p] += Math.max(take[v], skip[v]);
        }

        boolean[] chosen = new boolean[n];
        for (int v : order) {
            if ((parent[v] == -1 || !chosen[parent[v]]) && take[v] > skip[v]) {
                chosen[v] = true;
                selected.add(v);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    private static List<Integer> emptyPatternPositions(int n) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i <= n; i++) result.add(i);
        return result;
    }

    // Baseline: compare every possible window character by character.
    static Map<String, Object> naive(Map<String, Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        List<Integer> positions = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m == 0) return Map.of("positions", emptyPatternPositions(n));
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
            if (j == m) positions.add(i);
        }
        return Map.of("positions", positions);
    }

    // KMP: build prefix lengths and reuse them after a mismatch or a match.
    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        List<Integer> positions = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m == 0) return Map.of("positions", emptyPatternPositions(n));
        int[] pi = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }
        for (int i = 0, j = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add(i - m + 1);
                j = pi[j - 1];
            }
        }
        return Map.of("positions", positions);
    }

    // Rabin-Karp: the requested base=31 and modulus=101, with exact verification.
    static Map<String, Object> rk(Map<String, Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        List<Integer> positions = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m == 0) return Map.of("positions", emptyPatternPositions(n));
        if (m > n) return Map.of("positions", positions);
        final long base = 31, mod = 101;
        long power = 1, target = 0, window = 0;
        for (int i = 0; i < m; i++) {
            target = (target * base + pattern.charAt(i)) % mod;
            window = (window * base + text.charAt(i)) % mod;
            if (i < m - 1) power = power * base % mod;
        }
        for (int i = 0; i + m <= n; i++) {
            if (window == target) {
                int j = 0;
                while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
                if (j == m) positions.add(i);
            }
            if (i + m < n) {
                window = ((window - text.charAt(i) * power % mod + mod) * base
                        + text.charAt(i + m)) % mod;
            }
        }
        return Map.of("positions", positions);
    }

    // Prefix doubling for the suffix array, followed by Kasai LCP.
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        Integer[] order = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
            rank[i] = text.charAt(i);
        }
        for (int step = 1; step < n; step *= 2) {
            final int k = step;
            final int[] old = rank;
            Arrays.sort(order, (a, b) -> {
                if (old[a] != old[b]) return Integer.compare(old[a], old[b]);
                int r1 = a + k < n ? old[a + k] : -1;
                int r2 = b + k < n ? old[b + k] : -1;
                return Integer.compare(r1, r2);
            });
            int[] newRank = new int[n];
            for (int i = 1; i < n; i++) {
                int a = order[i - 1], b = order[i];
                int a2 = a + k < n ? old[a + k] : -1;
                int b2 = b + k < n ? old[b + k] : -1;
                newRank[b] = newRank[a] + ((old[a] != old[b] || a2 != b2) ? 1 : 0);
            }
            rank = newRank;
            if (rank[order[n - 1]] == n - 1) break;
        }
        int[] sa = new int[n], lcp = new int[n], pos = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = order[i];
            pos[sa[i]] = i;
        }
        for (int i = 0, matched = 0; i < n; i++) {
            int r = pos[i];
            if (r == 0) {
                matched = 0;
                continue;
            }
            int j = sa[r - 1];
            while (i + matched < n && j + matched < n &&
                    text.charAt(i + matched) == text.charAt(j + matched)) matched++;
            lcp[r] = matched;
            if (matched > 0) matched--;
        }
        int best = 0, start = 0;
        for (int i = 1; i < n; i++) {
            if (lcp[i] > best) {
                best = lcp[i];
                start = sa[i];
            }
        }
        return Map.of("sa", sa, "lcp", lcp,
                "repeated", best == 0 ? "" : text.substring(start, start + best));
    }

    // Aho-Corasick: one trie for all patterns, with failure links.
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        final int alphabet = 95; // Printable ASCII characters 32..126.

        int totalLength = 0;
        for (Object p : patterns) totalLength += ((String) p).length();
        int[][] next = new int[totalLength + 1][alphabet];
        int[] fail = new int[totalLength + 1];
        int[] terminal = new int[patterns.size()];
        int states = 1;

        // Build the trie. Duplicate patterns share the same terminal state.
        for (int i = 0; i < patterns.size(); i++) {
            String pattern = (String) patterns.get(i);
            int state = 0;
            for (int j = 0; j < pattern.length(); j++) {
                int c = pattern.charAt(j) - 32;
                if (next[state][c] == 0) next[state][c] = states++;
                state = next[state][c];
            }
            terminal[i] = state;
        }

        // BFS calculates failure links and fills missing transitions.
        int[] queue = new int[states];
        int count = 0;
        for (int c = 0; c < alphabet; c++) {
            if (next[0][c] != 0) queue[count++] = next[0][c];
        }
        for (int i = 0; i < count; i++) {
            int v = queue[i];
            for (int c = 0; c < alphabet; c++) {
                int u = next[v][c];
                if (u != 0) {
                    fail[u] = next[fail[v]][c];
                    queue[count++] = u;
                } else {
                    next[v][c] = next[fail[v]][c];
                }
            }
        }

        List<long[]> counts = new ArrayList<>();
        for (Object item : texts) {
            String text = (String) item;
            long[] visits = new long[states];
            int state = 0;
            for (int i = 0; i < text.length(); i++) {
                state = next[state][text.charAt(i) - 32];
                visits[state]++;
            }
            // Suffix-pattern matches are counted through failure links.
            for (int i = count - 1; i >= 0; i--) {
                int v = queue[i];
                visits[fail[v]] += visits[v];
            }
            long[] row = new long[patterns.size()];
            for (int i = 0; i < row.length; i++) row[i] = visits[terminal[i]];
            counts.add(row);
        }
        return Map.of("counts", counts);
    }

}
