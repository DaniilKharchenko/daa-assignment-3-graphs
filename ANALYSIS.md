# Analysis — Assignment 3: Graph Algorithms

## Implementation and protocol

The implementation uses Java 17-compatible syntax and the supplied JSON Lines adapters. `Main` handles one request per line and emits one JSON object per request. All mutable algorithm state is allocated inside the current method, so repeated requests do not share graph or DSU state. `ping` remains unchanged. DFS traversals use explicit stacks rather than Java recursion, avoiding stack overflow on long chains of up to 100,000 vertices.

The code uses original zero-based edge positions as edge IDs. MST totals and DAG distances use Java `long`; unreachable DAG distances are emitted as JSON `null`, and `parent_edge` contains edge IDs rather than predecessor vertices.

## Algorithms, invariants and correctness

### DSU

Each vertex begins in its own set. `find` first locates a root and then compresses the traversed path. `union` attaches the smaller root tree to the larger root tree and updates the component count and largest set size only when two different sets merge. Therefore every operation preserves the represented partition, and each recorded `[components, largest]` pair describes the state immediately after that operation. Repeated unions and self-unions leave the state unchanged.

Time: `O(n + q α(n))` amortized. Memory: `O(n + q)` including returned states.

### Kruskal minimum spanning forest

The algorithm sorts original edge IDs by weight and processes edges in that order. An edge is selected only when its endpoints belong to different DSU components. Thus selected edges never form a cycle. By the cut property, repeatedly selecting the lightest edge joining two current components yields a minimum spanning forest. DSU's final component count includes isolated vertices.

Time: `O(n + m log(m+1))`. Memory: `O(n + m)`.

### Prim minimum spanning forest

The graph is stored as undirected adjacency lists, with each entry retaining the original edge ID. Prim starts from each not-yet-visited vertex, pushes its incident edges into a min-priority queue and accepts only edges whose destination has not yet been visited. The queue grows the current component by its cheapest available crossing edge; after it empties, the outer loop starts the next component. This is the cut-property greedy step for each connected component, and no accepted edge creates a cycle. Self-loops are never selected.

Time: `O(n + m log(m+1))` with a lazy priority queue. Memory: `O(n + m)`.

### Kosaraju strongly connected components

The first iterative DFS records vertices on completion, preserving DFS finish order. The second pass visits the transposed graph in reverse finish order and labels each reached set as one SCC. Kosaraju's ordering ensures that a second-pass traversal cannot incorrectly merge distinct SCCs. A set of encoded label pairs deduplicates all cross-component edges; internal edges are omitted. Therefore the output contains every condensation edge exactly once.

Time: `O(n + m)` expected, including hash-set deduplication. Memory: `O(n + m)`.

### Three-state DFS topological ordering

Each vertex is unvisited (`0`), active (`1`) or finished (`2`). The explicit stack frame stores the next outgoing edge index, so each adjacency entry is examined once and suspended DFS calls resume at the correct neighbour. An edge to an active vertex is a back edge and proves a directed cycle; edges to finished vertices do not. If no cycle is found in any component, reverse finish order places every vertex before all of its outgoing neighbours. If any cycle is found, the method returns `acyclic=false` and an empty order.

Time: `O(n + m)`. Memory: `O(n + m)`.

### DAG shortest paths and route reconstruction

The entire directed graph is topologically checked before relaxing any edge, so a cycle in a disconnected component also invalidates the request. Distances are relaxed in topological order; when a vertex is reachable, all possible predecessor vertices have already been processed. The algorithm therefore computes shortest distances even when edge weights are negative. A separate `reachable` flag distinguishes an unreachable vertex from a valid distance such as `-1`. On a strict improvement, `parent_edge` stores the original incoming edge ID. The source remains at distance zero with parent `-1`; unreachable vertices remain `null` with parent `-1`.

The helper `reconstructRoute(source, target, parentEdge, edges)` follows edge IDs backwards, verifies each edge ends at the current vertex, then reverses the collected sequence. It returns `[source]` for the source itself and an empty list for an unreachable or invalid parent chain. The hop limit prevents infinite loops if the helper is accidentally given a malformed parent array.

Time: `O(n + m)` for shortest paths; `O(L)` for a route with `L` vertices. Memory: `O(n + m)`.

## Boundary and adversarial cases

The public checks and `tests/custom_cases.json` cover empty graphs, isolated vertices, repeated and self unions, negative weights, self-loops, parallel edges, equal-cost alternatives, 64-bit MST totals, unreachable vertices, negative DAG distances, cycles outside source reachability, and duplicate condensation edges. The supplied public checks also include deep chains for SCC, topological ordering and DAG shortest paths. `tests/RouteHelperTest.java` directly checks a reachable target, the source, an unreachable target and an alternative parent edge for parallel edges.

Commands from the repository root:

```sh
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/custom_inputs.jsonl
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/RouteHelperTest.java
java -cp .build/Java RouteHelperTest
python tools/benchmark_mst.py
```

`tests/actual_outputs.jsonl` records the outputs from `tests/custom_inputs.jsonl`; compare each line with the corresponding `expected` field in `tests/custom_cases.json`. `python tests/stress_test.py` was also run successfully at the full limits (`n=100000`, `m/q=250000`): DSU, Kruskal, Prim, SCC, topological ordering and DAG shortest paths all passed their stress assertions.

## Kruskal vs. Prim benchmark

The benchmark is implemented in `tools/benchmark_mst.py`. It builds each graph once and passes the exact same edge list to both methods. The graph generator uses `random.Random(20261025 + n)`, first adds a chain to ensure connectivity, then adds random non-self edges until `m = 3n`. We warmed up both methods on a separate 200-vertex graph. Compilation is excluded: Java classes are built before running the benchmark. The timer is Python `time.perf_counter_ns`; the measured boundary is JVM process launch through receiving and parsing the JSON result. Thus JVM startup and JSON input/output are included, and graph generation/compilation are excluded. Each entry below is one actual run, in milliseconds; medians are calculated from the three runs.

Environment: Linux x86_64 container, OpenJDK 21.0.12.1 (compiled with `--release 17`), Python 3.13.5, 3 reported CPUs. Hardware and background load are virtualized/shared, so small timing differences should not be overinterpreted.

| Vertices `n` | Edges `m` | Kruskal runs (ms) | Kruskal median (ms) | Prim runs (ms) | Prim median (ms) | MST cost |
|---:|---:|---|---:|---|---:|---:|
| 1,000 | 3,000 | 94.462, 149.563, 94.866 | 94.866 | 133.112, 97.143, 92.786 | 97.143 | -609,673 |
| 5,000 | 15,000 | 178.996, 190.255, 161.112 | 178.996 | 151.459, 146.788, 175.136 | 151.459 | -3,083,793 |
| 12,000 | 36,000 | 185.927, 215.716, 204.881 | 204.881 | 285.682, 286.243, 219.859 | 285.682 | -7,435,282 |

Both methods returned the same cost for each identical graph, and the benchmark verified that each result was a connected spanning tree with `n-1` selected edges. The observed winner varies by size, and process startup, garbage collection and shared-container noise are visible in the spread. These measurements are not a substitute for the asymptotic analysis: both methods use sorting/priority-queue operations and are expected to scale approximately as `O(n + m log m)` here.

## Sources and AI assistance

The assignment PDF file and the provided README repository were used as the source of the necessary interface, algorithmic contracts, and testing requirements. ChatGPT assisted in developing the wording for the analysis and the testing tools.