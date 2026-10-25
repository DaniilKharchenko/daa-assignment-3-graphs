#!/usr/bin/env python3
"""Reproducible end-to-end Kruskal/Prim benchmark; compile separately first."""
import json, random, statistics, subprocess, time, pathlib, sys, platform, os
ROOT = pathlib.Path(__file__).resolve().parents[1]
BUILD = ROOT / ".build" / "Java"
JAVA = ["java", "-Xmx384m", "-cp", str(BUILD), "Main"]
SEED = 20261025

def graph(n):
    rng = random.Random(SEED + n)
    edges = []
    # A chain ensures the graph is connected; random extra edges give sparse cycles.
    for v in range(1, n):
        edges.append([v-1, v, rng.randint(-1000, 1000)])
    target_m = 3*n
    while len(edges) < target_m:
        u, v = rng.randrange(n), rng.randrange(n)
        if u != v:
            edges.append([u, v, rng.randint(-1000, 1000)])
    return edges

def run(task, n, edges):
    request = {"task": task, "n": n, "edges": edges}
    payload = json.dumps(request, separators=(",", ":")) + "\n"
    start = time.perf_counter_ns()
    p = subprocess.run(JAVA, input=payload, text=True, capture_output=True, check=True)
    elapsed = (time.perf_counter_ns() - start) / 1_000_000
    answer = json.loads(p.stdout)
    if answer.get("components") != 1 or len(answer.get("edge_ids", [])) != n-1:
        raise RuntimeError(f"invalid forest result for {task}, n={n}")
    return elapsed, answer["cost"]

def main():
    # Warm up both methods on a small graph. Compilation is deliberately outside timing.
    warm_edges = graph(200)
    run("kruskal", 200, warm_edges)
    run("prim", 200, warm_edges)
    print("Environment:", platform.platform(), "|", platform.python_version(), "|", "Java subprocess")
    print("Seed base:", SEED, "| timer: time.perf_counter_ns | boundary: process launch through parsed JSON result")
    print("Graph generation and compilation excluded; JVM startup and JSON input/output included.")
    print("n,m,kruskal_ms,kruskal_median_ms,prim_ms,prim_median_ms,cost")
    for n in (1000, 5000, 12000):
        edges = graph(n)
        times = {"kruskal": [], "prim": []}
        costs = {}
        for task in ("kruskal", "prim"):
            for _ in range(3):
                ms, cost = run(task, n, edges)
                times[task].append(ms); costs[task] = cost
        if costs["kruskal"] != costs["prim"]:
            raise RuntimeError("Kruskal/Prim costs differ on identical graph")
        print(f"{n},{len(edges)},{';'.join(f'{x:.3f}' for x in times['kruskal'])},"
              f"{statistics.median(times['kruskal']):.3f},"
              f"{';'.join(f'{x:.3f}' for x in times['prim'])},"
              f"{statistics.median(times['prim']):.3f},{costs['kruskal']}")
if __name__ == "__main__":
    main()
