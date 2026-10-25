# Custom tests

`custom_cases.json` contains inputs and independently reasoned expected answers for DSU repeated/self unions, empty forests, loops/parallel/negative edges, SCC condensation deduplication, cycle detection, negative DAG paths, unreachable cycles, and 64-bit forest totals.

Run the implementation with:
```sh
python tools/run.py --input tests/custom_inputs.jsonl
```
`custom_inputs.jsonl` contains one request per line. The launcher prints actual JSON outputs; compare them to the `expected` entries in `custom_cases.json`. For direct route-helper calls, compile and run `tests/RouteHelperTest.java` with the command documented in `ANALYSIS.md`.

For full-limit stress coverage (100,000 vertices and 250,000 operations/edges), run:
```sh
python tests/stress_test.py
```
This generates deterministic graphs using seed `314159`, checks DSU final connectivity, compares Kruskal/Prim optimum costs, validates the topological order, and checks result-array dimensions for SCC and DAG. It is intentionally separate from the quick public check.
