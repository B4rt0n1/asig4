
package graph.metrics;

/**
 * Simple metrics counter and timer.
 */
public class Metrics {
    private long dfsVisits = 0;
    private long edgesVisited = 0;
    private long kahnPush = 0;
    private long kahnPop = 0;
    private long relaxations = 0;
    private long startTime = 0;
    private long endTime = 0;

    public void startTimer() { startTime = System.nanoTime(); }
    public void stopTimer() { endTime = System.nanoTime(); }
    public long getElapsedNanos() { return endTime - startTime; }

    public void countDFSVisit() { dfsVisits++; }
    public void countEdgesVisited() { edgesVisited++; }
    public void countKahnPush() { kahnPush++; }
    public void countKahnPop() { kahnPop++; }
    public void countRelaxation() { relaxations++; }

    public long getDfsVisits() { return dfsVisits; }
    public long getEdgesVisited() { return edgesVisited; }
    public long getKahnPush() { return kahnPush; }
    public long getKahnPop() { return kahnPop; }
    public long getRelaxations() { return relaxations; }
}
