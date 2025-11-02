
package graph.scc;

import java.util.*;
import graph.metrics.Metrics;

/**
 * Tarjan's SCC implementation.
 */
public class TarjanSCC {
    private final Map<String, List<Edge>> g;
    private final Metrics metrics;
    private int index = 0;
    private final Map<String, Integer> idx = new HashMap<>();
    private final Map<String, Integer> low = new HashMap<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private final Set<String> onStack = new HashSet<>();
    private final List<List<String>> components = new ArrayList<>();

    public static class Edge {
        public final String to;
        public final double weight;
        public Edge(String to, double weight) { this.to = to; this.weight = weight; }
    }

    public TarjanSCC(Map<String, List<Edge>> graph, Metrics metrics) {
        this.g = graph;
        this.metrics = metrics;
    }

    public List<List<String>> run() {
        for (String v : g.keySet()) {
            if (!idx.containsKey(v)) dfs(v);
        }
        return components;
    }

    private void dfs(String v) {
        metrics.countDFSVisit();
        idx.put(v, index);
        low.put(v, index);
        index++;
        stack.push(v);
        onStack.add(v);

        for (Edge e : g.getOrDefault(v, Collections.emptyList())) {
            metrics.countEdgesVisited();
            String w = e.to;
            if (!idx.containsKey(w)) {
                dfs(w);
                low.put(v, Math.min(low.get(v), low.get(w)));
            } else if (onStack.contains(w)) {
                low.put(v, Math.min(low.get(v), idx.get(w)));
            }
        }

        if (low.get(v).equals(idx.get(v))) {
            List<String> comp = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                comp.add(w);
            } while (!w.equals(v));
            components.add(comp);
        }
    }
}
