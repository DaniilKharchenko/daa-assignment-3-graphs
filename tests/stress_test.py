#!/usr/bin/env python3
"""Optional boundary stress test at published n/m limits; run after --setup."""
import json, pathlib, random, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ["java", "-Xmx384m", "-cp", str(ROOT / ".build" / "Java"), "Main"]
N, M, SEED = 100_000, 250_000, 314159

def invoke(request):
    payload = json.dumps(request, separators=(",", ":")) + "\n"
    p = subprocess.run(JAVA, input=payload, text=True, capture_output=True, timeout=90, check=True)
    return json.loads(p.stdout)

def main():
    dsu_ops = [[i % N, (i + 1) % N] for i in range(M)]
    ans = invoke({"task":"dsu","n":N,"operations":dsu_ops})
    assert len(ans["states"]) == M and ans["states"][-1] == [1, N]
    print("PASS DSU n=100000, q=250000")

    rng = random.Random(SEED)
    weighted = [[i-1, i, rng.randint(-1000,1000)] for i in range(1,N)]
    while len(weighted) < M:
        u, v = rng.randrange(N), rng.randrange(N)
        if u != v:
            weighted.append([u,v,rng.randint(-1000,1000)])
    a = invoke({"task":"kruskal","n":N,"edges":weighted})
    b = invoke({"task":"prim","n":N,"edges":weighted})
    assert a["cost"] == b["cost"] and a["components"] == b["components"] == 1
    assert len(a["edge_ids"]) == len(b["edge_ids"]) == N-1
    print("PASS Kruskal and Prim n=100000, m=250000; equal optimum")

    directed = [[i,i+1] for i in range(N-1)]
    rng = random.Random(SEED + 1)
    while len(directed) < M:
        u = rng.randrange(N-1); v = rng.randrange(u+1,N)
        directed.append([u,v])
    scc = invoke({"task":"scc","n":N,"edges":directed})
    assert len(scc["component"]) == N
    print("PASS SCC n=100000, m=250000")

    topo = invoke({"task":"topo","n":N,"edges":directed})
    assert topo["acyclic"] and len(topo["order"]) == N
    pos = [0]*N
    for i,v in enumerate(topo["order"]): pos[v]=i
    assert all(pos[u] < pos[v] for u,v in directed)
    print("PASS topological DFS n=100000, m=250000")

    weighted_dag = [[u,v,rng.randint(-1000,1000)] for u,v in directed]
    dag = invoke({"task":"dag","n":N,"source":0,"edges":weighted_dag})
    assert dag["acyclic"] and len(dag["distance"]) == len(dag["parent_edge"]) == N
    assert dag["distance"][0] == 0 and dag["parent_edge"][0] == -1
    print("PASS DAG shortest paths n=100000, m=250000")

if __name__ == "__main__":
    main()
