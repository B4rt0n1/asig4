
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.google.gson.*;
import java.io.*;
import app.Main;
import graph.scc.TarjanSCC;
import graph.metrics.Metrics;

public class SmallGraphTest {
    @Test
    public void testSimpleSCC() {
        Metrics m = new Metrics();
        Map<String, java.util.List<graph.scc.TarjanSCC.Edge>> g = new LinkedHashMap<>();
        g.put("A", Arrays.asList(new graph.scc.TarjanSCC.Edge("B",1)));
        g.put("B", Arrays.asList(new graph.scc.TarjanSCC.Edge("A",1)));
        g.put("C", Arrays.asList());
        TarjanSCC t = new TarjanSCC(g, m);
        java.util.List<java.util.List<String>> comps = t.run();
        // should have SCC {A,B} and {C}
        assertEquals(2, comps.size());
    }
}
