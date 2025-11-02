package app;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;

import graph.dagsp.DagSP;
import graph.metrics.Metrics;
import graph.scc.TarjanSCC;
import graph.scc.TarjanSCC.Edge;
import graph.topo.KahnTopo;

/**
 * Main runner that reads JSON graphs from /data and runs SCC, condensation, topo, DAG-SP.
 * Automatically writes metrics to data/metrics_summary.csv.
 */
public class Main {
    static class Node { String id; }
    static class EdgeJson { String from; String to; double weight; }

    public static class GraphJson {
        List<Node> nodes;
        List<EdgeJson> edges;
    }

    public static Map<String, List<Edge>> buildGraph(GraphJson gj) {
        Map<String, List<Edge>> g = new LinkedHashMap<>();
        for (Node n : gj.nodes) g.put(n.id, new ArrayList<>());
        for (EdgeJson e : gj.edges) {
            if (g.containsKey(e.from) && g.containsKey(e.to)) {
                g.get(e.from).add(new Edge(e.to, e.weight));
            }
        }
        return g;
    }

    public static Map<String, List<String>> toAdjList(Map<String, List<Edge>> g) {
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (String k : g.keySet()) {
            List<String> t = new ArrayList<>();
            for (Edge e : g.get(k)) t.add(e.to);
            adj.put(k, t);
        }
        return adj;
    }

    public static void main(String[] args) throws Exception {
        String dataDir = "data";
        if (args.length > 0) dataDir = args[0];
        File d = new File(dataDir);
        if (!d.exists() || !d.isDirectory()) {
            System.err.println("Provide a valid data directory with JSON graphs (default ./data)");
            return;
        }

        Gson gson = new Gson();

        // Ensure CSV is saved inside the data folder
        File csvFile = new File(d, "metrics_summary.csv");

        try (PrintWriter csv = new PrintWriter(new FileWriter(csvFile))) {
            // CSV header
            csv.println("dataset,scc_count,dfs_visits,edges_visited,kahn_push,kahn_pop,relaxations,elapsed_ns");

            for (File f : Objects.requireNonNull(d.listFiles((dir, name) -> name.endsWith(".json")))) {
                System.out.println("--- Processing: " + f.getName());
                GraphJson gj = gson.fromJson(new FileReader(f), GraphJson.class);
                Map<String, List<Edge>> g = buildGraph(gj);

                Metrics metrics = new Metrics();
                metrics.startTimer();
                TarjanSCC scc = new TarjanSCC(g, metrics);
                List<List<String>> comps = scc.run();
                metrics.stopTimer();

                System.out.println("SCCs (count=" + comps.size() + ")");
                System.out.println("DFS visits=" + metrics.getDfsVisits() + " edgesVisited=" + metrics.getEdgesVisited());

                // Build condensation graph
                Map<String, Integer> compId = new HashMap<>();
                for (int i = 0; i < comps.size(); i++) {
                    for (String v : comps.get(i)) compId.put(v, i);
                }
                Map<String, List<Edge>> cond = new LinkedHashMap<>();
                for (int i = 0; i < comps.size(); i++) cond.put("C" + i, new ArrayList<>());
                for (String u : g.keySet()) {
                    for (Edge e : g.get(u)) {
                        int a = compId.get(u), b = compId.get(e.to);
                        if (a != b) cond.get("C" + a).add(new Edge("C" + b, e.weight));
                    }
                }

                Map<String, List<String>> condAdj = toAdjList(cond);
                KahnTopo kt = new KahnTopo(condAdj, metrics);
                List<String> topo = kt.topoSort();

                // Run DAG-SP on condensation
                if (!topo.isEmpty()) {
                    DagSP dsp = new DagSP(cond, metrics);
                    String src = topo.get(0);
                    dsp.shortestFrom(src, topo);
                    dsp.longestFrom(src, topo);
                }

                // Write metrics to CSV
                csv.printf(
                    "%s,%d,%d,%d,%d,%d,%d,%d%n",
                    f.getName(),
                    comps.size(),
                    metrics.getDfsVisits(),
                    metrics.getEdgesVisited(),
                    metrics.getKahnPush(),
                    metrics.getKahnPop(),
                    metrics.getRelaxations(),
                    metrics.getElapsedNanos()
                );

                System.out.println("Metrics written for: " + f.getName());
                System.out.println();
            }

            System.out.println("Metrics summary written to " + csvFile.getAbsolutePath());
        }
    }
}
