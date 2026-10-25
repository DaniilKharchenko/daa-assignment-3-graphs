# Full-limit stress run

Command:
```sh
python tests/stress_test.py
```

Actual output:
```text
PASS DSU n=100000, q=250000
PASS Kruskal and Prim n=100000, m=250000; equal optimum
PASS SCC n=100000, m=250000
PASS topological DFS n=100000, m=250000
PASS DAG shortest paths n=100000, m=250000
```

The test uses deterministic pseudo-random input with seed base `314159`. The Kruskal and Prim run use the exact same weighted edge list. It checks DSU final component/size state, forest size and optimum cost agreement, SCC output size, topological precedence for every edge, and DAG array dimensions/source invariants.
