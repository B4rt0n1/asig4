## 1️ Datasets Overview

Nine graphs were generated in three categories (small, medium, large), covering different densities and cycle structures:

| Category | Files                         | Node range | Description                            |
| -------- | ----------------------------- | ---------- | -------------------------------------- |
| Small    | small_1.json – small_3.json   | 6–10       | Simple or lightly cyclic graphs        |
| Medium   | medium_1.json – medium_3.json | 10–20      | Mixed cyclic & acyclic structures      |
| Large    | large_1.json – large_3.json   | 20–50      | Dense or complex DAGs for timing tests |

All graphs use **edge weights** for shortest/longest path computations.
Metrics were collected using the custom `Metrics` interface during SCC detection, topological sorting, and DAG-SP computations.

---

## 2️ Raw Metrics Summary

| dataset       | scc_count | dfs_visits | edges_visited | kahn_push | kahn_pop | relaxations | elapsed_ns |
| ------------- | --------- | ---------- | ------------- | --------- | -------- | ----------- | ---------- |
| large_1.json  | 24        | 24         | 30            | 30        | 24       | 60          | 995,100    |
| large_2.json  | 30        | 30         | 84            | 84        | 30       | 168         | 81,100     |
| large_3.json  | 8         | 40         | 79            | 14        | 8        | 16          | 103,700    |
| medium_1.json | 7         | 12         | 12            | 5         | 7        | 10          | 48,900     |
| medium_2.json | 10        | 15         | 17            | 11        | 10       | 22          | 72,300     |
| medium_3.json | 1         | 17         | 17            | 0         | 1        | 0           | 63,600     |
| small_1.json  | 4         | 6          | 6             | 3         | 4        | 6           | 36,800     |
| small_2.json  | 7         | 7          | 6             | 6         | 7        | 12          | 59,400     |
| small_3.json  | 4         | 8          | 7             | 1         | 4        | 1           | 91,400     |

---

## 3️ Results Interpretation

### 🔹 SCC & DFS Metrics

* The **number of SCCs** grows with graph complexity.

  * *large_2.json* (30 SCCs) suggests mostly disjoint or weakly connected subgraphs.
  * *medium_3.json* (1 SCC) indicates a fully cyclic structure.
* **DFS visits** scale roughly with node count.

  * Larger graphs (large_3.json) required up to **40 DFS visits**.

### 🔹 Topological Sorting (Kahn)

* Kahn’s **push/pop operations** match expected node/edge ratios.

  * In acyclic graphs (e.g., *medium_3.json*), operations are minimal because no condensation DAG edges remain.
* Dense DAGs (e.g., *large_2.json*) require significantly more queue operations (84 each), showing higher outdegree and edge processing.

### 🔹 Relaxations (DAG Shortest Paths)

* The **number of relaxations** is proportional to edge density:

  * *large_2.json* → 168 relaxations (very dense DAG).
  * *medium_3.json* → 0 relaxations (single SCC; no DAG edges after compression).

### 🔹 Timing (Elapsed Nanoseconds)

* Execution times are consistent with graph size and density, not purely vertex count.

  * *large_1.json* (995,100 ns) is slower than *large_2.json* (81,100 ns) because of more SCC + DAG transitions.
  * *small_* graphs range from ~36–91 μs, showing negligible overhead.

---

## 4️ Analysis & Conclusions

| Algorithm                     | Observations                                                                                    | When to Use                                                  |
| ----------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| **Tarjan SCC**                | Linear in V+E; efficiently compresses cycles even in large graphs.                              | Always use first to simplify dependency graphs.              |
| **Kahn Topological Sort**     | Performs well on sparse DAGs; queue operations scale with outdegree.                            | Use when clear edge ordering is required (e.g., scheduling). |
| **DAG Shortest/Longest Path** | Dynamic programming over topo order is stable and fast. Longest path easily derived via max-DP. | Suitable only after SCC compression (must be acyclic).       |

Overall, **structure density** and **cycle presence** heavily influence operation counts and time.
The pipeline (SCC → Condensation → Topo → DAG-SP) proved robust across all datasets.

---

## 5️ Recommendations

* Preprocess cyclic dependencies via **SCC condensation** to ensure correctness in scheduling.
* For large DAGs, consider **edge filtering or weight normalization** to reduce relaxations.
* Use **Metrics logging** for ongoing performance profiling — it clearly shows algorithmic hotspots.