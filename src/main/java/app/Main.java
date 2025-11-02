
package app;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import graph.scc.TarjanSCC;
import graph.scc.TarjanSCC.Edge;
import graph.topo.KahnTopo;
import graph.dagsp.DagSP;
import graph.metrics.Metrics;

/**
 * Main runner that reads JSON graphs from /data and runs SCC, condensation, topo, DAG-SP.
 * JSON format:
 * {
 *   "nodes": [{"id":"A"}],
 *   "edges": [{"from":"A","to":"B","weight":5.0}]
 * }
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
            g.get(e.from).add(new Edge(e.to, e.weight));
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
        if (args.length>0) dataDir = args[0];
        File d = new File(dataDir);
        if (!d.exists() || !d.isDirectory()) {
            System.err.println("Provide a data directory with JSON graphs (default ./data)");
            return;
        }
        Gson gson = new Gson();
        for (File f : d.listFiles((dir,name)->name.endsWith(".json"))) {
            System.out.println("--- Processing: " + f.getName());
            GraphJson gj = gson.fromJson(new FileReader(f), GraphJson.class);
            Map<String, List<Edge>> g = buildGraph(gj);

            Metrics metrics = new Metrics();
            metrics.startTimer();
            TarjanSCC scc = new TarjanSCC(g, metrics);
            List<List<String>> comps = scc.run();
            metrics.stopTimer();

            System.out.println("SCCs (count="+comps.size()+"):");
            for (List<String> c: comps) System.out.println(c + " size="+c.size());
            System.out.println("SCC metrics: dfsVisits="+metrics.getDfsVisits()+" edgesVisited="+metrics.getEdgesVisited()+" time(ns)="+metrics.getElapsedNanos());

            // build condensation
            Map<String,Integer> compId = new HashMap<>();
            for (int i=0;i<comps.size();i++) {
                for (String v: comps.get(i)) compId.put(v, i);
            }
            Map<String, List<Edge>> cond = new LinkedHashMap<>();
            for (int i=0;i<comps.size();i++) cond.put("C"+i, new ArrayList<>());
            for (String u : g.keySet()) {
                for (Edge e : g.get(u)) {
                    int a = compId.get(u), b = compId.get(e.to);
                    if (a!=b) cond.get("C"+a).add(new Edge("C"+b, e.weight));
                }
            }
            System.out.println("Condensation nodes="+cond.size());

            Map<String, List<String>> condAdj = toAdjList(cond);
            KahnTopo kt = new KahnTopo(condAdj, metrics);
            List<String> topo = kt.topoSort();
            System.out.println("Topological order of components: " + topo);

            // DAG SP on condensation (edge-weighted). Use first component as source.
            if (!topo.isEmpty()) {
                DagSP dsp = new DagSP(cond, metrics);
                String src = topo.get(0);
                DagSP.Result shortest = dsp.shortestFrom(src, topo);
                DagSP.Result longest = dsp.longestFrom(src, topo);
                System.out.println("Shortest distances from " + src + ": " + shortest.dist);
                System.out.println("Longest distances from " + src + ": " + longest.dist);

                // reconstruct an example path: farthest by longest
                String best = null;
                double bestVal = Double.NEGATIVE_INFINITY;
                for (Map.Entry<String,Double> e : longest.dist.entrySet()) {
                    if (e.getValue()!=Double.NEGATIVE_INFINITY && e.getValue()>bestVal) { bestVal = e.getValue(); best = e.getKey(); }
                }
                if (best!=null) {
                    System.out.println("Critical path length="+bestVal+" target="+best);
                    System.out.println("Path: " + DagSP.reconstruct(longest.parent, best));
                }
            }
            System.out.println();
        }
    }
}
