
package graph.dagsp;

import java.util.*;
import graph.metrics.Metrics;
import graph.scc.TarjanSCC.Edge;

/**
 * Shortest and Longest paths on DAG using topological order.
 * Assumes edge-weighted DAG.
 */
public class DagSP {
    private final Map<String, List<Edge>> g;
    private final Metrics metrics;

    public DagSP(Map<String, List<Edge>> graph, Metrics metrics) {
        this.g = graph;
        this.metrics = metrics;
    }

    public static class Result {
        public final Map<String, Double> dist;
        public final Map<String, String> parent;
        public Result(Map<String, Double> d, Map<String,String> p) { dist=d; parent=p; }
    }

    public Result shortestFrom(String src, List<String> topoOrder) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        for (String v : topoOrder) dist.put(v, Double.POSITIVE_INFINITY);
        if (!dist.containsKey(src)) throw new IllegalArgumentException("Source not in DAG");
        dist.put(src, 0.0);

        for (String u : topoOrder) {
            if (dist.get(u).isInfinite()) continue;
            for (Edge e : g.getOrDefault(u, Collections.emptyList())) {
                metrics.countRelaxation();
                double nd = dist.get(u) + e.weight;
                if (nd < dist.get(e.to)) {
                    dist.put(e.to, nd);
                    parent.put(e.to, u);
                }
            }
        }
        return new Result(dist, parent);
    }

    public Result longestFrom(String src, List<String> topoOrder) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        for (String v : topoOrder) dist.put(v, Double.NEGATIVE_INFINITY);
        if (!dist.containsKey(src)) throw new IllegalArgumentException("Source not in DAG");
        dist.put(src, 0.0);

        for (String u : topoOrder) {
            if (dist.get(u).isInfinite() && dist.get(u)>0) continue; // skip unreachable
            for (Edge e : g.getOrDefault(u, Collections.emptyList())) {
                metrics.countRelaxation();
                double nd = dist.get(u) + e.weight;
                if (nd > dist.get(e.to)) {
                    dist.put(e.to, nd);
                    parent.put(e.to, u);
                }
            }
        }
        return new Result(dist, parent);
    }

    public static List<String> reconstruct(Map<String,String> parent, String target) {
        List<String> path = new ArrayList<>();
        String cur = target;
        while (cur!=null) {
            path.add(cur);
            cur = parent.get(cur);
        }
        Collections.reverse(path);
        return path;
    }
}
