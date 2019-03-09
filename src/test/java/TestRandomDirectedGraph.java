/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2014 George Tsatsanifos <gtsatsanifos@gmail.com>
 *
 *  The GRA.M.MA.R. toolkit is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published 
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.ArrayDeque;
import java.util.Collection;
import java.text.NumberFormat;

import grammar.Edge;
import grammar.Graph;
import grammar.DirectedGraph;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;


public class TestRandomDirectedGraph {
    private static DirectedGraph<Long> graph;
    private static final int vertices = 500;

    @AfterClass
    public static void exitlude () {System.out.println("!! Done with testing graph digraphs !!");}

    @Ignore
    public static void createUndirectedGraph () {
	System.out.println("!! Testing graph digraphs !!");
        graph = new DirectedGraph<>("/home/gtsat/toygraph.txt",NumberFormat.getNumberInstance(),true);
        graph.printStatistics();
    }

    @BeforeClass
    public static void createRandomDirectedGraph () {
	System.out.println("!! Testing graph digraphs !!");
        graph = new DirectedGraph<>();
        for (long i=0; i<vertices; ++i) 
            assertTrue ("\n!! Unable to insert node "+i+" !!",graph.addNode(i));
        assertEquals ("\n!! Problematic vertex insertions !!",vertices,graph.numberNodes());

	graph.setErdosRenyiGraph ((float) Math.log(graph.numberNodes())/graph.numberNodes());
	//System.out.println (graph.printStatistics());
    }

    @Ignore
    public void testStructure () {
        assertEquals ("\n!! Problematic vertex insertion !!",graph.numberNodes(),graph.numberNodes());
    }

    @Test
    public void testConnectivity () {
        for (long u : graph.getNodes()) {
            Collection<Edge<Long>> from = graph.getEdgesFrom(u);
            Collection<Edge<Long>> to = graph.getEdgesTo(u);

            Collection<Long> neighbors = graph.getNeighbors(u);
            assertEquals ("\n!! Problematic in/out connectivity (part 1)!!",from.size(),neighbors.size());

            ArrayDeque<Long> fromNodes = new ArrayDeque<>();
            for (Edge<Long> edge : from) fromNodes.push(edge.to);
            fromNodes.retainAll(neighbors);
            assertEquals ("\n!! Problematic in/out connectivity (part 2)!!",fromNodes.size(),neighbors.size());
            for (Edge<Long> edge : to)
                assertTrue ("\n!! Problematic in/out connectivity (part 3)!!",edge.to==u);

            Collection<Long> backlinks = graph.getBacklinks(u);
            assertEquals ("\n!! Problematic in/out connectivity (part 4)!!",to.size(),backlinks.size());
            ArrayDeque<Long> toNodes = new ArrayDeque<>();
            for (Edge<Long> edge : to) toNodes.push(edge.from);
            toNodes.retainAll (backlinks);
            assertEquals ("\n!! Problematic in/out connectivity (part 5)!!",toNodes.size(),backlinks.size());
        }
    }

    @Test
    public void testEdgeIterator () {
        int counter=0;
        for (Edge<Long> edge : graph) {
            assertTrue ("\n!! Null pointer during edge iteration !!", edge!=null);
            assertTrue ("\n!! Unbound edge probing !!", counter < graph.numberEdges());
            ++counter;
        }
        assertEquals ("\n!! Not all edges where visited during traversal  !!",graph.numberEdges(),counter);
    }

    @Test
    public void testDFStraversal () {
        int counter=0;
        for (long u : graph.getNodes()){
            assertTrue ("\n!! Unbound DFS traversal !!",counter<=graph.numberNodes()*graph.numberNodes());
            for (Long v : graph.DFS(u))
                assertTrue ("\n!! Null pointer during DFS traversal !!", v!=null);
            ++counter;
        }
    }

    @Test
    public void testBFStraversal () {
        int counter=0;
        for (long u : graph.getNodes()){
            assertTrue ("\n!! Unbound BFS traversal !!",counter<graph.numberNodes()*graph.numberNodes());
            for (Long v : graph.BFS(u))
                assertTrue ("\n!! Null pointer during BFS traversal !!", v!=null);
            ++counter;
        }
    }

    @Test
    public void testTopologicalSort () {
        int counter = 0;
        for (Long u : graph.topologicalOrder()) {
            assertTrue ("\n!! Null pointer during topological sort !!", u!=null);
            assertTrue ("\n!! Unbound traversal during topological sort !!",counter<graph.numberNodes());
            ++counter;
        }
        if (!graph.containsCycles())
            assertEquals ("\n!! Not all nodes were checked with topological order !!",graph.numberNodes(),counter);
    }

    @Test
    public void testPostorderTraversal () {
        boolean containsCycles = graph.containsCycles();

        int counter = 0;
        TreeSet<Long> marked = new TreeSet<> ();
        for (Long u : graph.postorder()) {
            if (!containsCycles)
                for (Long v : graph.getNeighbors(u)) 
                    assertTrue ("\n!! Erroneous postorder traversal; "
                        + "all child nodes should have been visited before the parent (" 
                        + u + " --> " + v + ") !!",marked.contains(v));

            assertTrue ("\n!! Null pointer during postorder traversal !!", u!=null);
            assertTrue ("\n!! Unbound postorder traversal !!", counter<graph.numberNodes());
            ++counter;

            marked.add(u);
        }
    }

    @Test
    public void testPreorderTraversal () {
        int counter = 0;
        TreeSet<Long> marked = new TreeSet<> ();
        for (Long u : graph.preorder()) {
            assertTrue ("\n!! Null pointer during preorder traversal !!", u!=null);
            assertTrue ("\n!! Unbound preorder traversal !!", counter<graph.numberNodes());
            ++counter;

            marked.add(u);
        }
    }

    @Test
    public void testReverseGraphPostorderTraversal () {
        int counter=0;
        for (Long u : graph.reverseGraphPostorder()) {
            assertTrue ("\n!! Null pointer during reverse postorder traversal !!", u!=null);
            assertTrue ("\n!! Unbound reverse postorder traversal !!", counter<graph.numberNodes());
            ++counter;
        }
    }

    @Test
    public void testInorderTraversal () {
        int counter=0;
        for (Long u : graph.inorder()) {
            assertTrue ("\n!! Null pointer during inorder traversal !!", u!=null);
            assertTrue ("\n!! Unbound inorder traversal !!", counter<graph.numberNodes());
            ++counter;
        }
    }

    @Ignore
    public void testConnectedComponentsComputation () {
        int size = graph.numberConnectedComponents();

        Map<Long,Integer> ccmap = graph.getConnectedComponents();
        assertEquals ("\n!! Improper assignment of graph.numberNodes() to components !!",graph.numberNodes(),ccmap.size());
        for (Entry<Long,Integer> entry : ccmap.entrySet()) 
            assertTrue ("\n!! Incorrect component identifier used !!",entry.getValue()<size);

        Map<Long,Integer> tmap = graph.tarjan();
        assertEquals ("\n!! Improper assignment of graph.numberNodes() to components in tarjan() !!",graph.numberNodes(),tmap.size());
        for (Entry<Long,Integer> entry : tmap.entrySet()) 
            assertTrue ("\n!! Incorrect component identifier used in tarjan() !!",entry.getValue()<size);

        Map<Long,Integer> gmap = graph.gabow();
        assertEquals ("\n!! Improper assignment of graph.numberNodes() to components in gabow() !!",graph.numberNodes(),gmap.size());
        for (Entry<Long,Integer> entry : gmap.entrySet()) 
            assertTrue ("\n!! Incorrect component identifier used in gabow() !!",entry.getValue()<size);
/**/
        Map<Long,Integer> kmap = graph.kosaraju();
        assertEquals ("\n!! Improper assignment of graph.numberNodes() to components in kosaraju() !!",graph.numberNodes(),kmap.size());
        for (Entry<Long,Integer> entry : kmap.entrySet()) 
            assertTrue ("\n!! Incorrect component identifier used in kosaraju() !!",entry.getValue()<size);
/**/
    }

    @Ignore
    public void testAborescenceComputation () {
        assertEquals ("\n!! Problematic vertex insertions !!",graph.numberNodes(),graph.numberNodes());
	System.out.println("!! Testing arborescence computation !!");
	DirectedGraph<Long> rbranching = graph.arborescence ();
        assertEquals ("\n!! Problematic vertex insertions in branching !!",graph.numberNodes(),rbranching.numberNodes());

	for (Edge<Long> edge : graph)
            assertTrue ("\n!! Exists no path in the branching between nodes " 
                +edge.from+" and "+edge.to+" !!",rbranching.pathExistence(edge.from, edge.to));

	float sssp0sum = 0.0f;
	for (Edge<Long> edge : rbranching)
            sssp0sum += edge.weight;
        System.out.println("!! Summed branching weight " + sssp0sum + " !!");
        assertTrue (graph.numberNodes()<=0 || sssp0sum>0);
    }

    @Test
    public void testSingleSourceShortestPathsComputation () {
	System.out.println("!! Testing methods for computing SSSPs !!");
	for (long u : graph.getNodes()) {
            Graph<Long> graphDijkstra = graph.dijkstra(u);
            assertEquals ("\n!! Problematic vertex insertion for Dijkstra's method !!",graph.numberNodes(),graphDijkstra.numberNodes());
            float sssp1sum=0.0f;
            for (Edge<Long> edge : graphDijkstra) sssp1sum += edge.weight;

            Graph<Long> graphBellman = graph.bellman(u);
            assertEquals ("\n!! Problematic vertex insertion for Bellman-Ford's method !!",graph.numberNodes(),graphBellman.numberNodes());
            float sssp2sum=0.0f;
            for (Edge<Long> edge : graphBellman) sssp2sum += edge.weight;

            Graph<Long> graphSSSP = graph.sssp(u);
            assertEquals ("\n!! Problematic vertex insertion for SSSPs computation method !!",graph.numberNodes(),graphSSSP.numberNodes());
            float sssp3sum=0.0f;
            for (Edge<Long> edge : graphSSSP) sssp3sum += edge.weight;

            assertEquals("\n!! Different SSSP computations (dijkstra/bellman) do not provide equivalent solutions (" 
                + sssp1sum + "/" + sssp2sum + ") !!",sssp1sum,sssp2sum,0.0f); 
            assertEquals("\n!! Different SSSP computations (bellman/sssp) do not provide equivalent solutions (" 
                + sssp2sum + "/" + sssp3sum + ") !!",sssp2sum,sssp3sum,0.0f);
	}
    }

    @Test
    public void testAllPairsShortestPaths () {
	System.out.println("!! Testing methods for computing APSPs !!");
	Map<Long,Graph<Long>> rjohnson = graph.johnson();
        assertEquals ("\n!! There should be a SSSP graph for each node after running Johnson's algorithm !!",graph.numberNodes(),rjohnson.size());
	Map<Long,Graph<Long>> rfloyd = graph.floyd();
        assertEquals ("\n!! There should be a SSSP graph for each node after running Floyd-Warshal's algorithm !!",graph.numberNodes(),rfloyd.size());
	for (long u : graph.getNodes()) {
            assertEquals ("\n!! Problematic vertex insertion for Johnson's method !!",graph.numberNodes(),rjohnson.get(u).numberNodes());
            float sssp5sum=0.0f;
            for (Edge<Long> edge : ((DirectedGraph<Long>)rjohnson.get(u))) sssp5sum += edge.weight;

            assertEquals ("\n!! Problematic vertex insertion for Floyd-Warshal's method !!",graph.numberNodes(),rfloyd.get(u).numberNodes());
            float sssp6sum=0.0f;
            for (Edge<Long> edge : ((DirectedGraph<Long>)rfloyd.get(u))) sssp6sum += edge.weight;

            assertEquals("\n!! Different APSP computations (johnson/floyd) do not provide equivalent solutions (" 
                + sssp5sum + "/" + sssp6sum + ") !!",sssp5sum,sssp6sum,0.0f); 
	}
    }

    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(TestRandomDirectedGraph.class);
        for (Failure failure : result.getFailures())
            System.out.println (failure.toString());
        System.out.println (result.wasSuccessful()?"OK!":"!! ERROR !!");
    }
}
