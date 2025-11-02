
package graph.topo;

import java.util.*;
import graph.metrics.Metrics;

/**
 * Kahn's algorithm for topological sort on DAG.
 */
public class KahnTopo {
    private final Map<String, List<String>> g;
    private final Metrics metrics;

    public KahnTopo(Map<String, List<String>> graph, Metrics metrics) {
        this.g = graph;
        this.metrics = metrics;
    }

    public List<String> topoSort() {
        Map<String,Integer> indeg = new HashMap<>();
        for (String u : g.keySet()) {
            indeg.putIfAbsent(u, 0);
            for (String v : g.getOrDefault(u, Collections.emptyList())) {
                indeg.put(v, indeg.getOrDefault(v,0)+1);
            }
        }

        Deque<String> q = new ArrayDeque<>();
        for (Map.Entry<String,Integer> e : indeg.entrySet()) {
            if (e.getValue()==0) q.add(e.getKey());
        }

        List<String> order = new ArrayList<>();
        while (!q.isEmpty()) {
            String u = q.remove();
            metrics.countKahnPop();
            order.add(u);
            for (String v : g.getOrDefault(u, Collections.emptyList())) {
                indeg.put(v, indeg.get(v)-1);
                metrics.countKahnPush();
                if (indeg.get(v)==0) q.add(v);
            }
        }
        return order;
    }
}
