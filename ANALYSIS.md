# Assignment 4 — Analysis

## Environment and commands

**Language:** Java 17. The project uses the official Java starter files and can be compiled with `javac --release 17`.

**Computer for final verification:**
- Operating system: Windows 11 Pro.
- JDK / Java version: OpenJDK 25.0.1, compiled with `--release 17`.
- CPU and RAM: AMD Ryzen 5 7535HS, 15.3 GB RAM reported by Windows.
- JVM memory limit: `-Xmx384m` for the provided launcher and benchmark.

Run these commands from the root of the **complete official repository**, after copying the Java source and tests into it:

```sh
python tools/run.py --setup
python tools/run.py --check
python tests/student/test_cases.py --stress
python tools/run.py --input tests/student/manual_cases.jsonl
javac --release 17 -encoding UTF-8 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
java -Xmx384m -cp Java/build SearchBenchmark > tests/student/benchmark_results.csv
```

The first command compiles Java and checks startup. The second runs public tests. The Python student test script uses the compiled files in `Java/build`. The benchmark is separate from the application. No external Java libraries are needed. On Windows, `py -3` may replace `python`.

## Algorithms

### 1. LCS (`lcs`)

`dp[i][j]` is the best common subsequence length for the first `i` characters of `a` and the first `j` characters of `b`.

- **Start:** If a prefix is empty, its DP value is 0.
- **Update:** If the last two characters are equal, use `dp[i-1][j-1] + 1`. Otherwise use `max(dp[i-1][j], dp[i][j-1])`.
- **Order:** Fill rows from smaller prefixes to larger ones.
- **Why correct:** A common last character can extend a shorter answer. If the last characters differ, at least one of them must be ignored.
- **Reconstruction:** Walk backward through the table and collect matching characters. Reverse them at the end. A tie can use either optimal side.
- **Time and extra memory:** `O((n+1)*(m+1))`. Output has at most `min(n,m)` characters.
- **Special test:** `axbyc` and `abc` give length 3. This checks that LCS is not the same as a substring. An empty string gives length 0.

### 2. Ordered Merge (`merge`)

`dp[l][r]` is the minimum cost to merge all original blocks from index `l` to `r`.

- **Start:** A single block costs 0. An empty input also costs 0.
- **Update:** Try every last split `k`. The cost is `dp[l][k] + dp[k+1][r] + sum(l,r)`.
- **Order:** Solve short intervals first. Prefix sums find `sum(l,r)` quickly.
- **Why correct:** The final operation must join two adjacent intervals. We check every possible final split and use optimal results for both sides.
- **Reconstruction:** Save the best split. Write the left and right child merges first, then `[l,k,r]`. This gives a valid chronological plan. Either minimum is valid in a tie.
- **Time:** `O(n^3)`. **Extra memory:** `O(n^2)` for DP/splits; output is `O(n)`.
- **Special test:** `[2,3,4]` gives cost 14. Three blocks of size `1000000000` give cost `5000000000`, so costs must use `long`.

### 3. Tree Independent Set (`tree`)

The goal is to choose vertices with the biggest total weight, but never choose two neighbouring vertices.

- **States:** `take[v]` is the best result when vertex `v` is chosen. `skip[v]` is the best result when `v` is not chosen.
- **Start:** `take[v] = weight[v]` and `skip[v] = 0` before adding children.
- **Update:** For each child `u`, add `skip[u]` to `take[v]`, and add `max(take[u],skip[u])` to `skip[v]`.
- **Order:** Make a parent-before-child list from root 0. Process that list backward to calculate DP. This avoids recursive stack overflow.
- **Why correct:** A chosen parent forbids its children. If the parent is not chosen, every child can use its better option.
- **Reconstruction:** Process the list forward. Choose a vertex only if its parent is not chosen and `take[v] > skip[v]`. Ties may be skipped.
- **Time and extra memory:** `O(n+1)`, including the adjacency lists and reconstruction.
- **Special test:** All negative weights give an empty set with weight 0. A chain of 100000 vertices tests stack safety.

### 4a. Naive Search (`naive`)

Try all possible starting positions and compare the pattern characters one by one.

- **Start:** An empty pattern matches at every position `0..n`. If `m > n`, the result is empty.
- **Update:** Test starting positions `0..n-m`, including the last one.
- **Why correct:** Every possible start is checked. A start is returned only if all `m` characters match.
- **Overlaps:** Move the start by one, even after a match, so overlapping results are kept.
- **Time:** `O(n*m+n+1)` worst case. **Extra memory:** `O(1)` plus `O(z+1)` output positions.
- **Special test:** `aaaaa` with `aaa` gives `[0,1,2]`.

### 4b. KMP Search (`kmp`)

KMP uses a prefix table. `pi[i]` is the length of the longest proper prefix that is also a suffix of `pattern[0..i]`.

- **Start:** The prefix table starts at zero. An empty pattern returns every text boundary.
- **Update:** On a mismatch, use `pi[j-1]` to find a shorter matched prefix. After a full match, save its start and also use `pi[j-1]`.
- **Why correct:** The prefix table keeps information about a suffix that can still be a possible beginning of the pattern. We do not need to recheck characters before that suffix.
- **Overlaps:** The fallback after a match keeps overlapping answers.
- **Time:** `O(n+m+z+1)`. **Extra memory:** `O(m+1)` plus `O(z+1)` output.
- **Special test:** `abababa` with `aba` gives `[0,2,4]`.

### 4c. Rabin–Karp Search (`rk`)

This task requires **base 31 and modulus 101**, using ASCII character codes.

- **Start:** Compute the pattern hash, the first text window hash and `31^(m-1) mod 101`. Handle an empty or too-long pattern first.
- **Update:** Remove the first character from the window hash, multiply by 31 and add the next character. Normalize a negative value by adding 101 before taking modulo.
- **Why correct:** Different hashes cannot be equal strings. Equal hashes are only possible matches, so the code **also compares the real characters** before returning a position.
- **Overlaps:** Every window is checked, including overlapping ones.
- **Time:** `O(n+m+c*m)` for `c` equal-hash windows; worst case `O(n*m+n+m+1)`. **Extra memory:** `O(1)` plus output.
- **Special test:** In `abdj`, `ab` and `dj` have the same hash. Only `ab` is returned, because the characters are checked.

### 5. Suffix Array and Kasai LCP (`suffix`)

The suffix array `sa` contains the starting indices of all **nonempty** suffixes in sorted order.

- **Start:** Give each suffix a rank based on its first character. Empty text returns empty arrays and an empty repeated string.
- **Update:** With prefix doubling, sort indices using `(rank[i], rank[i+step])`, with `step = 1,2,4,...`. Missing second parts have rank -1. Assign new ranks after each sort.
- **Why correct:** Each round sorts by twice as many starting characters. Once all suffixes have different ranks, the array is fully sorted.
- **Time:** `O(n*log^2(n+1))` using Java `Arrays.sort` and a comparator. **Extra memory:** `O(n+1)`.

**Kasai LCP:** `lcp[i]` is the common prefix length of suffixes `sa[i-1]` and `sa[i]`; `lcp[0]=0`. Use the inverse positions to compare a suffix to its previous sorted neighbour. Reuse the previous common length minus one when moving to the next text position. This gives `O(n)` time and `O(n)` extra memory.

The largest value in `lcp` is the length of the longest repeated substring. It is enough to compare neighbouring sorted suffixes because suffixes with the same starting string are together in sorted order. Overlapping repeats are allowed. A tie may return any valid longest repeat.

- **Special test:** `banana` gives `sa=[5,3,1,0,4,2]`, `lcp=[0,1,3,0,0,2]`, repeated `ana`.

### 6. Aho–Corasick (`aho`)

The algorithm builds one trie for all patterns, then searches several texts using the same trie.

- **Start:** The root has index 0. Each pattern stores the node where it ends. Duplicate patterns can end at the same node. Empty dictionaries are allowed.
- **Update:** BFS builds `fail[v]`, the node for the longest valid suffix when matching cannot continue. Missing transitions use the transition from the failure node. Each new text starts at the root.
- **Counting:** Increase the visit count of the reached state for each text character. After the scan, go through the BFS list backward and add each node's count to its failure parent.
- **Why correct:** A matched state may also end shorter suffix patterns. Passing counts through failure links includes these matches, including overlaps. Separate dictionary entries use their own result columns, even when patterns are identical.
- **Reset:** Start a new visit array for each text. Results of previous texts cannot change the next result.
- **Time:** `O(L+95*S+p)` to build and `O(N+q*(S+p))` for all texts. **Extra memory:** `O(95*S+p)` for the automaton and `O(S)` for counts per text, with `O(q*p)` output. Here `L` is the total pattern length, `S` is trie states, `p` is pattern count, `q` is text count and `N` is the total length of all texts.
- **Special test:** Patterns `['a','aa','a']` in `aaa` give `[3,2,3]`. Empty text gives zeros.


## My tests

I tested the algorithms using `tests/student/test_cases.py`. The tests use random seed `20261108` for repeatable results. I also checked boundary cases and large inputs.

| Task | Input / case | Expected / what is checked | Actual result |
|---|---|---|---|
| lcs | Empty strings, `axbyc` and `abc` | Correct subsequence | PASS |
| merge | Empty arrays, `[2,3,4]`, large values | Minimum cost and merge plan | PASS |
| tree | Negative weights, 100000-vertex chain | Correct result without stack overflow | PASS |
| naive/kmp/rk | Empty patterns, overlapping matches | Correct match positions | PASS |
| rk | Hash collision example | Reject incorrect matches | PASS |
| suffix | `banana`, 50000 equal characters | Correct suffix array and LCP | PASS |
| aho | Duplicate patterns, empty dictionary | Correct match counts | PASS |
| mixed | 1200 random requests, repeated batch | Correct outputs and state reset | PASS |

**Local verification log:**

- `py -3 tools/run.py --setup` — PASS.
- `py -3 tools/run.py --check` — 44/44 public tests passed.
- State reset test — PASS.
- `py -3 tests/student/test_cases.py` — 13 explicit cases and 1200 randomized requests passed.
- `py -3 tests/student/test_cases.py --stress` — all 4 stress cases passed.
- Search Benchmark — 18 measurements, all `correct=true`.
- No test failures remained. Java compilation showed an unchecked operations warning.


## Search performance comparison

The program `tests/student/SearchBenchmark.java` compares Naive, KMP, and Rabin–Karp using the same text and pattern. It checks that all three algorithms return the same results before measuring their speed.

- Sizes: `(300,15)`, `(1200,40)`, and `(3000,90)`.
- Random input: characters `abcde`, using Java `Random` with seed `20261108+n*1000+m`.
- Repetitive input: only `a` characters.
- Warm-up: 150 calls per algorithm and input.
- Measurement: three runs of 100 calls using `System.nanoTime()`.
- Unit: milliseconds per call. Compilation is excluded.
- Environment: Windows, OpenJDK 25.0.1, compiled with Java 17 compatibility.

The following results were measured on my computer.

| Case | n | m | Generator | Algorithm | Run 1 (ms) | Run 2 (ms) | Run 3 (ms) | Median (ms) | Correct |
|---|---:|---:|---|---|---:|---:|---:|---:|---|
| random | 300 | 15 | `20261108+300*1000+15` | naive | 0.007648 | 0.004485 | 0.003871 | 0.004485 | true |
| random | 300 | 15 | `20261108+300*1000+15` | kmp | 0.013081 | 0.004415 | 0.004505 | 0.004505 | true |
| random | 300 | 15 | `20261108+300*1000+15` | rk | 0.013843 | 0.004477 | 0.004347 | 0.004477 | true |
| random | 1200 | 40 | `20261108+1200*1000+40` | naive | 0.022503 | 0.025077 | 0.027214 | 0.025077 | true |
| random | 1200 | 40 | `20261108+1200*1000+40` | kmp | 0.021439 | 0.023187 | 0.021832 | 0.021832 | true |
| random | 1200 | 40 | `20261108+1200*1000+40` | rk | 0.026162 | 0.022727 | 0.022628 | 0.022727 | true |
| random | 3000 | 90 | `20261108+3000*1000+90` | naive | 0.008237 | 0.006313 | 0.005393 | 0.006313 | true |
| random | 3000 | 90 | `20261108+3000*1000+90` | kmp | 0.006271 | 0.005654 | 0.005660 | 0.005660 | true |
| random | 3000 | 90 | `20261108+3000*1000+90` | rk | 0.022470 | 0.022022 | 0.022603 | 0.022470 | true |
| repetitive | 300 | 15 | all `a` | naive | 0.057310 | 0.061851 | 0.037672 | 0.057310 | true |
| repetitive | 300 | 15 | all `a` | kmp | 0.014454 | 0.011808 | 0.011201 | 0.011808 | true |
| repetitive | 300 | 15 | all `a` | rk | 0.053755 | 0.058132 | 0.067363 | 0.058132 | true |
| repetitive | 1200 | 40 | all `a` | naive | 0.050409 | 0.059728 | 0.049755 | 0.050409 | true |
| repetitive | 1200 | 40 | all `a` | kmp | 0.013270 | 0.009253 | 0.009262 | 0.009262 | true |
| repetitive | 1200 | 40 | all `a` | rk | 0.132666 | 0.141270 | 0.124873 | 0.132666 | true |
| repetitive | 3000 | 90 | all `a` | naive | 0.256829 | 0.257846 | 0.253670 | 0.256829 | true |
| repetitive | 3000 | 90 | all `a` | kmp | 0.029635 | 0.030288 | 0.032153 | 0.030288 | true |
| repetitive | 3000 | 90 | all `a` | rk | 0.657135 | 0.651870 | 0.638841 | 0.651870 | true |

### Results

For random text, all three algorithms were fast. The differences were small, and some measurements changed because of JVM warm-up and other factors.

For repetitive text, KMP was the fastest algorithm in all three tests. It reuses information about matched prefixes, so it avoids repeated comparisons.

Naive Search was slower on repetitive text because it compared the same characters many times.

Rabin–Karp was also slower on repetitive text. Its small modulus (101) can produce many equal hashes, and each candidate must be verified by comparing characters.

These measurements show how the algorithms performed on my computer. They do not prove their time complexity, and other computers may give different results.

## Sources and AI assistance

- Sources: Official Assignment 4 README, contracts, testing guide, and rubric.
- AI assistance: I used ChatGPT for Java code, explanations, debugging, tests, and analysis. I ran the tests on my computer and checked the results.
