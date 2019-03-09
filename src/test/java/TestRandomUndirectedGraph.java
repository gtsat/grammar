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
import java.util.Map.Entry;
import java.util.Collection;

import grammar.Edge;
import grammar.Graph;
import grammar.UndirectedGraph;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import junit.framework.TestCase;

public final class TestRandomUndirectedGraph extends TestCase {
    private static final UndirectedGraph<Integer> random = new UndirectedGraph<>();
    private static final int vertices = 500;

    @AfterClass
    public static void exitlude () {System.out.println("!! Done with testing random graphs !!");}

    @BeforeClass
    public static void createRandomUndirectedGraph () {
	System.out.println("!! Testing random graphs !!");
        for (int i=0; i<vertices; ++i) 
            assertTrue ("\n!! Unable to insert node "+i+" !!",random.addNode(i));

        assertEquals ("\n!! Problematic vertex insertion !!",vertices,random.numberNodes());

	random.setErdosRenyiGraph ((float) Math.log(vertices)/vertices);
	//System.out.println (random.printStatistics());
    }

    @Test
    public void testStructure () {
        assertEquals ("\n!! Problematic vertex insertion !!",vertices,random.numberNodes());
    }

    @Test
    public void testConnectivity () {
        for (int u : random.getNodes()) {
            Collection<Edge<Integer>> from = random.getEdgesFrom(u);
            Collection<Edge<Integer>> to = random.getEdgesFrom(u);
            assertEquals ("\n!! Problematic in/out connectivity (part 1)!!",from.size(),to.size());
            from.retainAll(to);
            assertEquals ("\n!! Problematic in/out connectivity (part 2)!!",from.size(),to.size());
            assertEquals ("\n!! Problematic in/out connectivity (part 3)!!",from.size(),random.getNeighbors(u).size());
        }
    }

    @Test
    public void testEdgeIterator () {
        int counter=0;
        for (Edge<Integer> edge : random) {
            assertTrue ("\n!! Null pointer during edge iteration !!", edge!=null);
            assertTrue ("\n!! Unbound edge probing !!", counter < random.numberEdges());
            ++counter;
        }
        assertEquals ("\n!! Not all edges where visited during traversal  !!",random.numberEdges(),counter);
    }

    @Test
    public void testDFStraversal () {
        int counter=0;
        for (int u : random.getNodes()){
            ++counter;
            assertTrue ("\n!! Unbound DFS traversal !!",counter<=random.numberNodes()*random.numberNodes());
            int subsumesCounter = 0;
            for (Integer v : random.DFS(u)) {
                assertTrue ("\n!! Null pointer during DFS traversal !!", v!=null);
                ++subsumesCounter;
            }
            for (Integer v : random.postorder(u)) {
                assertTrue ("\n!! Null pointer during reverse DFS traversal !!", v!=null);
                --subsumesCounter;
            }
            assertEquals ("\n!! DFS and reverseDFS do not traverse the same number of nodes",0,subsumesCounter);
        }
    }

    @Test
    public void testBFStraversal () {
        int counter=0;
        for (int u : random.getNodes()){
            assertTrue ("\n!! Unbound BFS traversal !!",counter<random.numberNodes()*random.numberNodes());
            for (Integer v : random.BFS(u))
                assertTrue ("\n!! Null pointer during BFS traversal !!", v!=null);
            ++counter;
        }
    }

    @Test
    public void testPostorderTraversal () {
        int counter=0;
        for (Integer u : random.postorder()) {
            assertTrue ("\n!! Null pointer during postorder traversal !!", u!=null);
            assertTrue ("\n!! Unbound postorder traversal !!", counter < random.numberNodes());
            ++counter;
        }
    }

    @Test
    public void testInorderTraversal () {
        int counter=0;
        for (Integer u : random.inorder()) {
            assertTrue ("\n!! Null pointer during inorder traversal !!", u!=null);
            assertTrue ("\n!! Unbound inorder traversal !!", counter < random.numberNodes());
            ++counter;
        }
    }

    @Test
    public void testConnectedComponentsComputation () {
        Map<Integer,Integer> components = random.getConnectedComponents();
        assertEquals ("\n!! Improper assignment of vertices to components !!",vertices,components.size());
        int size = random.numberConnectedComponents();
        for (Entry<Integer,Integer> entry : components.entrySet()) 
            assertTrue ("\n!! Incorrect component identifier used !!",entry.getValue()<size);
    }

    @Test
    public void testMinimumSpanningTreesComputation () {
	System.out.println("!! Testing methods for computing MSTs !!");
	UndirectedGraph<Integer> rmst1 = random.prim();
        assertEquals ("\n!! Problematic vertex insertion for Prim's method !!",vertices,rmst1.numberNodes());
	for (Edge<Integer> edge : random) {
            assertTrue ("\n!! Exists no path in Prim's spanning tree between nodes " 
                + edge.from + " and " + edge.to + " !!",rmst1.pathExistence (edge.from, edge.to));
            assertTrue ("\n!! Exists no path in Prim's spanning tree between nodes " 
                + edge.to + " and " + edge.from + " !!",rmst1.pathExistence (edge.to, edge.from));
        }

        UndirectedGraph<Integer> rmst2 = random.kruskal();
        assertEquals ("\n!! Problematic vertex insertion for Kruskal's method !!",vertices,rmst2.numberNodes());
        for (Edge<Integer> edge : random) {
            assertTrue ("\n!! Exists no path in Kruskal's spanning tree between nodes " 
                + edge.from + " and " + edge.to + " !!",rmst2.pathExistence (edge.from, edge.to));
            assertTrue ("\n!! Exists no path in Kruskal's spanning tree between nodes " 
                + edge.to + " and " + edge.from + " !!",rmst2.pathExistence (edge.to, edge.from));
        }
/*
        UndirectedGraph<Integer> rmst3 = random.boruvka();
        assertEquals ("\n!! Problematic vertex insertion for Boruvka's method !!",vertices,rmst3.numberNodes());
        for (Edge<Integer> edge : random) {
            assertTrue ("\n!! Exists no path in Boruvka's spanning tree between nodes " 
                + edge.from + " and " + edge.to + " !!",rmst3.pathExistence (edge.from, edge.to));
            assertTrue ("\n!! Exists no path in Boruvka's spanning tree between nodes " 
                + edge.to + " and " + edge.from + " !!",rmst3.pathExistence (edge.to, edge.from));
        }
*/
        float mst1sum=0.0f;
        for (Edge<Integer> edge : rmst1) mst1sum += edge.weight;

        float mst2sum=0.0f;
        for (Edge<Integer> edge : rmst2) mst2sum += edge.weight;

        float mst3sum=0.0f;
        //for (Edge<Integer> edge : rmst3) mst3sum += edge.weight;

        assertEquals ("\n!! Different MST computations do not provide equivalent solutions !!",mst1sum,mst2sum,0.0f);
        //assertEquals ("\n!! Different MST computations do not provide equivalent solutions !!",mst2sum,mst3sum,0.0f);
    }

    @Test
    public void testSingleSourceShortestPathsComputation () {
        System.out.println("!! Testing methods for computing SSSPs !!");
        for (int u : random.getNodes()) {
            Graph<Integer> randomSSSP = random.sssp(u);
            assertEquals ("\n!! Problematic vertex insertion for SSSPs computation method !!",vertices,randomSSSP.numberNodes());
            float sssp0sum=0.0f;
            for (Edge<Integer> edge : randomSSSP) {
                sssp0sum += edge.weight;
                assertTrue ("\n!! Exists no path in the spanning tree between nodes " 
                    + u + " and " + edge.to + " !!",randomSSSP.pathExistence (u,edge.to));
            }

            Graph<Integer> randomDijkstra = random.dijkstra(u);
            assertEquals ("\n!! Problematic vertex insertion for Dijkstra's method !!",vertices,randomDijkstra.numberNodes());
            float sssp1sum=0.0f;
            for (Edge<Integer> edge : randomDijkstra) {
                sssp1sum += edge.weight;
                assertTrue ("\n!! Exists no path in the spanning tree between nodes " 
                    + u + " and " + edge.to + " !!",randomDijkstra.pathExistence (u,edge.to));
            }

            Graph<Integer> randomBellman = random.bellman(u);
            assertEquals ("\n!! Problematic vertex insertion for Bellman-Ford's method !!",vertices,randomBellman.numberNodes());
            float sssp2sum=0.0f;
            for (Edge<Integer> edge : randomBellman) {
                sssp2sum += edge.weight;
                assertTrue ("\n!! Exists no path in the spanning tree between nodes " 
                    + u + " and " + edge.to + " !!",randomBellman.pathExistence (u,edge.to));
            }

            assertEquals("\n!! Different SSSP computations (sssp/dijkstra) do not provide equivalent solutions (" 
                + sssp0sum + "/" + sssp1sum + ") !!",sssp0sum,sssp1sum,0.0f); 
            assertEquals("\n!! Different SSSP computations (dijkstra/bellman) do not provide equivalent solutions (" 
                + sssp1sum + "/" + sssp2sum + ") !!",sssp1sum,sssp2sum,0.0f);
        }
    }

    @Test
    public void testAllPairsShortestPaths () {
	System.out.println("!! Testing methods for computing APSPs !!");
	Map<Integer,Graph<Integer>> rjohnson = random.johnson();
        assertEquals ("\n!! There should be a SSSP graph for each node after running Johnson's algorithm !!",vertices,rjohnson.size());
	Map<Integer,Graph<Integer>> rfloyd = random.floyd();
        assertEquals ("\n!! There should be a SSSP graph for each node after running Floyd-Warshal's algorithm !!",vertices,rfloyd.size());
	for (int u : random.getNodes()) {
            assertEquals ("\n!! Problematic vertex insertion for Johnson's method !!",vertices,rjohnson.get(u).numberNodes());
            float sssp1sum=0.0f;
            for (Edge<Integer> edge : rjohnson.get(u)) {
                sssp1sum += edge.weight;
		assertTrue ("\n!! Exists no path in the spanning tree between nodes " 
                    + u + " and " + edge.to + " !!",rjohnson.get(u).pathExistence (u,edge.to));
            }

            assertEquals ("\n!! Problematic vertex insertion for Floyd-Warshal's method !!",vertices,rfloyd.get(u).numberNodes());
            float sssp2sum=0.0f;
            for (Edge<Integer> edge : rfloyd.get(u)){
                sssp2sum += edge.weight;
		assertTrue ("\n!! Exists no path in the spanning tree between nodes " 
                    + u + " and " + edge.to + " !!",rfloyd.get(u).pathExistence (u,edge.to));
            }

            assertEquals("\n!! Different APSP computations do not provide equivalent solutions (" 
                + sssp1sum + "/" + sssp2sum + ") !!",sssp1sum,sssp2sum,0.0f);
        }
    }

    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(TestRandomUndirectedGraph.class);
        for (Failure failure : result.getFailures())
            System.out.println (failure.toString());
        System.out.println (result.wasSuccessful()?"OK!":"!! ERROR !!");
    }
}
