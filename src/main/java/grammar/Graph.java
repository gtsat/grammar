/**
 * The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit Copyright (C)
 * 2014 George Tsatsanifos <gtsatsanifos@gmail.com>
 *
 * The GRA.M.MA.R. toolkit is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package grammar;

import java.io.*;
import java.util.Map;
import java.util.Stack;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;

import java.util.Scanner;
import java.text.Format;
import java.text.ParseException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.util.HashMap;
import javax.swing.JFrame;

import org.apache.log4j.Logger;

@SuppressWarnings("unchecked") //(value={"unchecked","rawtypes","unused"})
abstract public class Graph<NodeT> implements Iterable<Edge<NodeT>> {

    private static final Logger logger = Logger.getLogger (Graph.class);

    protected final MatrixSparse<NodeT, NodeT> matrix;
    protected final HashSet<NodeT> sinks;

    protected int countEdges = 0;
    protected int countSelfLoops = 0;
    protected int countNegativeWeightEdges = 0;

    private int maxCachingRadius = 0;

    public int getMaxCachingRadius() {
        return maxCachingRadius;
    }

    public void setMaxCachingRadius(int radius) {
        maxCachingRadius = radius;
    }

    protected final Map<NodeT, Map<NodeT, Float>> cached = new HashMap<>();

    public Map<NodeT, Float> getCachedDistancesFrom(NodeT u) {
        return cached.get(u);
    }

    public void resetCache() {
        if (!cached.isEmpty()) {
            for (Entry<NodeT, Map<NodeT, Float>> entry : cached.entrySet()) {
                entry.getValue().clear();
            }
            cached.clear();
        }
    }

    public boolean containsNegativeWeightEdges() {
        return countNegativeWeightEdges > 0;
    }

    public MatrixSparse<NodeT, NodeT> getAdjacencyMatrix() {
        return matrix;
    }

    public Iterable<NodeT> BFS(NodeT source) {
        return new BFSiterable(source);
    }

    public Iterable<NodeT> DFS(NodeT source) {
        return new DFSiterable(source);
    }

    public Iterable<NodeT> inorder(NodeT source) {
        return new InorderIterable(source);
    }

    public Iterable<NodeT> preorder(NodeT source) {
        return new PreorderIterable(source);
    }

    public Iterable<NodeT> postorder(NodeT source) {
        return new PostorderIterable(source);
    }

    public Iterable<NodeT> inorder() {
        return new InorderIterable(getNodes());
    }

    public Iterable<NodeT> preorder() {
        return new PreorderIterable(getNodes());
    }

    public Iterable<NodeT> postorder() {
        return new PostorderIterable(getNodes());
    }

    public Graph() {
        matrix = new MatrixSparse<>(Float.NaN);
        sinks = new HashSet<>();
    }

    public Graph(Graph<NodeT> other) {
        matrix = new MatrixSparse<>(other.matrix);
        sinks = new HashSet<>(other.sinks);

        countEdges = other.countEdges;
        countNegativeWeightEdges = other.countNegativeWeightEdges;
    }

    public Graph(String filename, Format format, boolean isWeightedEdgeList) {
        matrix = new MatrixSparse<>(Float.NaN);
        sinks = new HashSet<>();
        String inputline = null;
        try{
            //Scanner in = new Scanner(new File(filename)); ///////////////////////////////////////////////////////
            //Scanner in = new Scanner(new File(this.getClass().getClassLoader().getResource(filename).getFile()));
            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Importing now data from file '" + filename + "'...");
            long startTime = System.nanoTime();

            File file = new File(this.getClass().getClassLoader().getResource(filename).getFile());
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            for (inputline = in.readLine(); inputline != null ; inputline = in.readLine()) {
                //inputline = in.nextLine();
                String[] line = inputline.split("\\s+");

                if (line.length == 0) {
                    continue;
                }
                if (line.length > 0 && line[0].startsWith("#")) {
                    continue;
                }

                String row = line[0];

                if (!isWeightedEdgeList) { //line.length > 3) {
                    for (int i = 1; i < line.length; ++i) {
                        String col = line[i];
                        if (format == null) {
                            setEdge((NodeT) row, (NodeT) col, 1.0f);
                        } else {
                            setEdge((NodeT) format.parseObject(row), (NodeT) format.parseObject(col), 1.0f);
                        }

                        if ((numberEdges() % 1000000) == 0) {
                            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> So far " + numberEdges() + " edges have been set.");
                        }
                    }
                } else {
                    String col = line[1];
                    float val = line.length == 3f ? Float.parseFloat(line[2]) : 1.0f;
                    if (format == null) {
                        setEdge((NodeT) row, (NodeT) col, val);
                    } else {
                        setEdge((NodeT) format.parseObject(row), (NodeT) format.parseObject(col), val);
                    }

                    if ((numberEdges() % 1000000) == 0) {
                        logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> So far " + numberEdges() + " edges have been set.");
                    }
                }
            }
            in.close();
            long endTime = System.nanoTime();
            logger.info(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Loaded " + numberEdges() + " edges in " + (endTime - startTime) / Math.pow(10, 9) + " secs.");
        }catch (FileNotFoundException e) {
            logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Input file '" + filename + "' does not exist...");
        }catch (ParseException e) {
            logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Unable to parse line '" + inputline + "' in file '" + filename + "'...");
        }catch (UnsupportedEncodingException e) {
            logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Unsupported encoding exception at line '" + inputline + "' in file '" + filename + "'...");
        }catch (IOException e) {
            logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> IOException while processing file '" + filename + "' at line '" + inputline + "'.");
        }
    }

    public int numberNodes() {
        return matrix.mat.size() + sinks.size();
    }

    public int numberEdges() {
        return countEdges;
    }

    public int numberNegativeEdges() {
        return countNegativeWeightEdges;
    }

    public int numberPositiveEdges() {
        return countEdges - countNegativeWeightEdges;
    }

    public int numberSelfLoops() {
        return countSelfLoops;
    }

    public boolean containsSelfLoops() {
        return countSelfLoops > 0;
    }

    public Float getEdgeWeight(NodeT from, NodeT to) {
        return matrix.get(from, to);
    }

    public boolean containsEdge(NodeT from, NodeT to) {
        return matrix.mat.containsKey(from) && matrix.mat.get(from).containsKey(to);
    }

    public Edge<NodeT> getEdge(NodeT from, NodeT to) {
        return containsEdge(from, to) ? new Edge<>(from, to, getEdgeWeight(from, to)) : null;
    }

    public void setEdge(Edge<NodeT> edge) {
        setEdge(edge.from, edge.to, edge.weight);
    }

    abstract public void setEdge(NodeT from, NodeT to, Float weight);

    abstract public ArrayDeque<Edge<NodeT>> getEdgesTo(NodeT to);

    public ArrayDeque<Edge<NodeT>> getEdgesFrom(NodeT from) {
        ArrayDeque<Edge<NodeT>> result = new ArrayDeque<>();
        if (matrix.mat.containsKey(from)) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(from).entrySet()) {
                result.add(new Edge<>(from, entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    abstract public boolean removeEdge(NodeT from, NodeT to);

    public void removeSelfLoops() {
        for (NodeT u : matrix.mat.keySet()) {
            removeEdge(u, u);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return other instanceof Graph<?>
                && matrix.equals(((Graph<NodeT>) other).matrix)
                && sinks.equals(((Graph<NodeT>) other).sinks);
    }

    public boolean isEmpty() {
        return matrix.isEmpty() && sinks.isEmpty();
    }

    public void clear() {
        matrix.clear();
        sinks.clear();
    }

    public void printGraph() {
        for (Edge<NodeT> edge : this) {
            System.out.println(edge.from + "\t" + edge.to + "\t" + edge.weight);
        }
    }

    public void printCached() {
        for (Entry<NodeT, Map<NodeT, Float>> over : cached.entrySet()) {
            for (Entry<NodeT, Float> under : over.getValue().entrySet()) {
                System.out.println(over.getKey() + "\t" + under.getKey() + "\t" + under.getValue());
            }
        }
    }

    public int numberNeighbors(NodeT u) {
        return matrix.mat.containsKey(u) ? matrix.mat.get(u).size() : 0;
    }

    public ArrayDeque<NodeT> getNeighbors(NodeT from) {
        return matrix.mat.containsKey(from) ? new ArrayDeque<>(matrix.mat.get(from).keySet()) : new ArrayDeque<NodeT>();
    }

    protected Collection<NodeT> getActiveNodesShallowCopy() {
        return matrix.mat.keySet();
    } // use carefully

    abstract public Map<NodeT, Integer> getConnectedComponents();

    abstract public int numberConnectedComponents();

    abstract public boolean removeNode(NodeT u);

    public boolean addNode(NodeT u) {
        return containsNode(u) ? false : sinks.add(u);
    }

    public boolean containsNode(NodeT u) {
        return sinks.contains(u) || matrix.mat.containsKey(u);
    }

    public ArrayList<NodeT> getSinks() {
        return new ArrayList<NodeT>(sinks);
    }

    public ArrayList<NodeT> getNodes() {
        ArrayList<NodeT> nodes = new ArrayList<>(matrix.mat.keySet());
        nodes.addAll(sinks);
        return nodes;
    }

    abstract public void mergeNodes(NodeT u, NodeT v);

    abstract public boolean containsCycles();

    public boolean containsNegativeCycles() {
        return containsNegativeWeightEdges() && bellman() == null;
    }

    private final class BFSiterable implements Iterator<NodeT>, Iterable<NodeT> {

        private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
        private final ArrayDeque<NodeT> queue = new ArrayDeque<>();

        public BFSiterable(NodeT source) {
            queue.add(source);
            marked.set(source);
        }

        @Override
        public Iterator<NodeT> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeT next() {
            NodeT w = queue.remove();
            if (matrix.mat.containsKey(w)) {
                for (NodeT v : matrix.mat.get(w).keySet()) {
                    if (!marked.get(v)) {
                        marked.set(v);
                        queue.add(v);
                    }
                }
            }
            return w;
        }
    }

    private final class DFSiterable implements Iterator<NodeT>, Iterable<NodeT> {

        private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
        private final Stack<NodeT> stack = new Stack<>();

        public DFSiterable(NodeT source) {
            stack.push(source);
            marked.set(source);
        }

        @Override
        public Iterator<NodeT> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeT next() {
            NodeT top = stack.pop();
            if (matrix.mat.containsKey(top)) {
                for (NodeT u : matrix.mat.get(top).keySet()) {
                    if (!marked.get(u)) {
                        marked.set(u);
                        stack.push(u);
                    }
                }
            }
            return top;
        }
    }

    private final class PreorderIterable implements Iterator<NodeT>, Iterable<NodeT> {

        private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
        private final Stack<NodeT> stack = new Stack<>();
        private final ArrayList<NodeT> buffer;

        public PreorderIterable(ArrayList<NodeT> sources) {
            buffer = sources;
        }

        public PreorderIterable(NodeT source) {
            buffer = new ArrayList<>();
            buffer.add(source);
        }

        @Override
        public Iterator<NodeT> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return !(buffer.isEmpty() && stack.isEmpty());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeT next() {
            if (stack.isEmpty()) {
                while (!buffer.isEmpty()) {
                    NodeT u = buffer.remove(0);
                    if (!marked.get(u)) {
                        marked.set(u);
                        stack.push(u);
                        break;
                    }
                }
            }

            while (!stack.isEmpty()) {
                NodeT top = stack.pop();
                if (matrix.mat.containsKey(top)) {
                    for (NodeT u : matrix.mat.get(top).keySet()) {
                        if (!marked.get(u)) {
                            marked.set(u);
                            stack.push(u);

                            buffer.remove(u);
                        }
                    }
                }
                return top;
            }
            return null;    // should never reach this point
        }
    }

    private final class PostorderIterable implements Iterator<NodeT>, Iterable<NodeT> {

        //private final BitsetSparse<NodeT> processed = new BitsetSparse<>();

        private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
        private final Stack<NodeT> stack = new Stack<>();
        private final ArrayList<NodeT> buffer;

        public PostorderIterable(ArrayList<NodeT> sources) {
            buffer = sources;
        }

        public PostorderIterable(NodeT source) {
            buffer = new ArrayList<>();
            buffer.add(source);
        }

        @Override
        public Iterator<NodeT> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return !(buffer.isEmpty() && stack.isEmpty());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeT next() {
            if (stack.isEmpty()) {
                while (!buffer.isEmpty()) {
                    NodeT u = buffer.remove(0);
                    if (!marked.get(u)) {
                        marked.set(u);
                        stack.push(u);
                        break;
                    }
                }
            }

            while (!stack.isEmpty()) {
                NodeT top = stack.peek();
                boolean newElements = false;
                if (matrix.mat.containsKey(top)) {
                    for (NodeT u : matrix.mat.get(top).keySet()) {
                        if (!marked.get(u)) {
                            marked.set(u);
                            stack.push(u);

                            buffer.remove(u);

                            newElements = true;
                            break;
                        }
                    }
                }
                if (!newElements) {
                    return stack.pop();
                }
            }
            /*
             while (!stack.isEmpty()) {
             NodeT top = stack.peek();
             if (processed.get(top)) {
             return stack.pop();
             }else{
             processed.set(top);
             if (matrix.mat.containsKey(top)) {
             for (NodeT u : matrix.mat.get(top).keySet()) {
             if (!marked.get(u)) {
             marked.set(u);
             stack.push(u);

             buffer.remove(u);
             }
             }
             }
             }
             }
             */
            return null; // should never happen!
        }
    }

    private final class InorderIterable implements Iterator<NodeT>, Iterable<NodeT> {

        private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
        private final Stack<NodeT> stack = new Stack<>();
        private final ArrayList<NodeT> buffer;

        public InorderIterable(ArrayList<NodeT> sources) {
            buffer = sources;
        }

        public InorderIterable(NodeT source) {
            buffer = new ArrayList<>();
            buffer.add(source);
        }

        @Override
        public Iterator<NodeT> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return !(buffer.isEmpty() && stack.isEmpty());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeT next() {
            if (stack.isEmpty()) {
                while (!buffer.isEmpty()) {
                    NodeT u = buffer.remove(0);
                    if (!marked.get(u)) {
                        marked.set(u);
                        stack.push(u);
                        break;
                    }
                }
            }

            while (!stack.isEmpty()) {
                NodeT top = stack.pop();
                int counter = 0;
                if (matrix.mat.containsKey(top)) {
                    for (NodeT neighbor : matrix.mat.get(top).keySet()) {
                        if (!marked.get(neighbor)) {
                            marked.set(neighbor);
                            stack.push(neighbor);
                            if (counter == 0) {
                                stack.push(top);
                            }

                            buffer.remove(neighbor);

                            ++counter;
                        }
                    }
                }
                if (counter == 0) {
                    return top;
                }
            }
            return null; // should never reach this point !!!
        }
    }

    /**
     * Generally, for a dense graph |E| is in \Theta(|V|^2). Since there is no
     * solid definition on this, we will have to make the following convention:
     * This function returns true whenever Floyd's algorithm is expected to run
     * slower than Johnson's. This of course depends on the implementation.
     * Therefore, another fast function analyzing the graph's characteristic's
     * is needed so as to predict these cases. We might just as easily have
     * incrementally pre-computed this at every edge insertion! On the safe
     * side, we will use the formal complexity definition from the theory for
     * the time being. Improvements will be made later on after a closer look on
     * how this behaves actually.
     *
     * @return true if sparse; false otherwise
     */
    public boolean isSparse() {
        return numberEdges() * Math.log(numberNodes()) < numberNodes() * numberNodes();
    }	// Should I multiply the number of edges by two for undirected graphs for each edge is considered twice ???

    public boolean pathExistence(NodeT u, NodeT v) {
        return isSparse() ? existsBFSpath(u, v) : existsDFSpath(u, v);
    }

    public ArrayList<NodeT> rangeQuery(NodeT q, float radius) {
        ArrayList<NodeT> collection = new ArrayList<>();
        MinPQ<Edge<NodeT>> heap = new MinPQ<>();
        HashSet<NodeT> marked = new HashSet<>();
        marked.add(q);

        for (Edge<NodeT> edge : getEdgesFrom(q)) {
            if (edge.weight < radius && !edge.to.equals(q)) {
                marked.add(edge.to);
                heap.insert(edge);
            }
        }

        while (!heap.isEmpty()) {
            Edge<NodeT> top = heap.delMin();
            collection.add(top.to);

            for (Edge<NodeT> edge : getEdgesFrom(top.to)) {
                if (top.weight + edge.weight < radius && !marked.contains(edge.to)) {
                    heap.insert(new Edge<>(q, edge.to, top.weight + edge.weight));
                    marked.add(edge.to);
                }
            }
        }

        return collection;
    }

    public ArrayList<NodeT> reverseRangeQuery(NodeT q, float radius) {
        ArrayList<NodeT> collection = new ArrayList<>();
        MinPQ<Edge<NodeT>> heap = new MinPQ<>();
        HashSet<NodeT> marked = new HashSet<>();
        marked.add(q);

        for (Edge<NodeT> edge : getEdgesTo(q)) {
            if (edge.weight < radius && !edge.from.equals(q)) {
                marked.add(edge.from);
                heap.insert(edge);
            }
        }

        while (!heap.isEmpty()) {
            Edge<NodeT> top = heap.delMin();
            collection.add(top.from);

            for (Edge<NodeT> edge : getEdgesTo(top.from)) {
                if (top.weight + edge.weight < radius && !marked.contains(edge.from)) {
                    heap.insert(new Edge<>(edge.from, q, top.weight + edge.weight));
                    marked.add(edge.from);
                }
            }
        }

        return collection;
    }

    public ArrayList<NodeT> nearestQuery(NodeT q, int k) {
        ArrayList<NodeT> collection = new ArrayList<>();
        MinPQ<Edge<NodeT>> heap = new MinPQ<>();
        HashSet<NodeT> marked = new HashSet<>();
        marked.add(q);

        for (Edge<NodeT> edge : getEdgesFrom(q)) {
            if (!edge.to.equals(q)) {
                marked.add(edge.to);
                heap.insert(edge);
            }
        }

        while (!heap.isEmpty()) {
            Edge<NodeT> top = heap.delMin();
            collection.add(top.to);
            if (collection.size() >= k) {
                break;
            }

            for (Edge<NodeT> edge : getEdgesFrom(top.to)) {
                if (!marked.contains(edge.to)) {
                    heap.insert(new Edge<>(q, edge.to, top.weight + edge.weight));
                    marked.add(edge.to);
                }
            }
        }

        while (!heap.isEmpty()) {
            heap.delMin();
        }

        return collection;
    }

    public ArrayList<NodeT> reverseNearestQuery(NodeT q, int k) {
        ArrayList<NodeT> collection = new ArrayList<>();
        MinPQ<Edge<NodeT>> heap = new MinPQ<>();
        HashSet<NodeT> marked = new HashSet<>();
        marked.add(q);

        while (!heap.isEmpty()) {
            heap.delMin();
        }

        return collection;
    }

    /* a preferable choice in the presence of sparse graphs (just a few links per node), 
     * or when suspecting that such a path really exists and it is also not that long */
    private boolean existsBFSpath(NodeT u, NodeT v) {
        if (u.equals(v)) {
            return true;
        }
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        ArrayDeque<NodeT> queue = new ArrayDeque<>();
        queue.add(u);
        marked.set(u);
        while (!queue.isEmpty()) {
            NodeT top = queue.remove();
            if (top.equals(v)) {
                return true;
            }
            for (NodeT w : getNeighbors(top)) {
                if (!marked.get(w)) {
                    queue.add(w);
                    marked.set(w);
                }
            }
        }
        return false;
    }

    /* better choice in the presence of small world phenomena, where the diameter of the graph is not long. */
    private boolean existsDFSpath(NodeT u, NodeT v) {
        if (u.equals(v)) {
            return true;
        }
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        Stack<NodeT> stack = new Stack<>();
        marked.set(u);
        stack.push(u);
        while (!stack.isEmpty()) {
            NodeT w = stack.pop();
            if (w.equals(v)) {
                return true;
            }
            for (NodeT y : getNeighbors(w)) {
                if (!marked.get(y)) {
                    stack.push(y);
                    marked.set(y);
                }
            }
        }
        return false;
    }

    public float getAveragePathHops() {
        if (isEmpty() || matrix.mat.isEmpty()) {
            return 0.0f;
        }
        int counter = 0;
        long sumhops = 0L;
        ArrayDeque<NodeT> queue = new ArrayDeque<>();
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        VectorSparse<NodeT, Integer> dists = new VectorSparse<>(Integer.MAX_VALUE);
        for (NodeT u : getActiveNodesShallowCopy()) {
            queue.add(u);
            marked.set(u);
            dists.set(u, 0);

            while (!queue.isEmpty()) {
                NodeT top = queue.remove();
                int hops = dists.get(top);

                sumhops += hops;
                for (NodeT w : getNeighbors(top)) {
                    if (!marked.get(w)) {
                        queue.add(w);
                        marked.set(w);
                        dists.set(w, hops + 1);
                        ++counter;
                    }
                }
            }
            queue.clear();
            dists.clear();
            marked.clear();
        }
        return sumhops / (float) (counter); // decrement so as not to account for the source node at zero distance
    }

    public int getMaxPathHops() {
        if (isEmpty() || matrix.mat.isEmpty()) {
            return 0;
        }
        int maxhops = 0;
        ArrayDeque<NodeT> queue = new ArrayDeque<>();
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        VectorSparse<NodeT, Integer> dists = new VectorSparse<>(Integer.MAX_VALUE);
        for (NodeT u : getActiveNodesShallowCopy()) {
            queue.add(u);
            marked.set(u);
            dists.set(u, 0);

            int hops = 0;
            while (!queue.isEmpty()) {
                NodeT top = queue.remove();
                hops = dists.get(top);
                for (NodeT w : getNeighbors(top)) {
                    if (!marked.get(w)) {
                        queue.add(w);
                        marked.set(w);
                        dists.set(w, hops + 1);
                    }
                }
            }
            queue.clear();
            dists.clear();
            marked.clear();
            if (hops > maxhops) {
                maxhops = hops;
            }
        }
        return maxhops;
    }

    public int getPathHops(NodeT u, NodeT v) {
        if (u.equals(v)) {
            return 0;
        }
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        ArrayDeque<NodeT> queue = new ArrayDeque<>();
        VectorSparse<NodeT, Integer> dists = new VectorSparse<>(Integer.MAX_VALUE);
        queue.add(u);
        marked.set(u);
        dists.set(u, 0);

        while (!queue.isEmpty()) {
            NodeT top = queue.remove();
            int hops = dists.get(top);
            if (top.equals(v)) {
                return hops;
            }

            for (NodeT w : getNeighbors(u)) {
                if (!marked.get(w)) {
                    queue.add(w);
                    marked.set(w);
                    dists.set(w, hops + 1);
                }
            }
        }
        return -1;
    }

    abstract public float getDensity();

    abstract public void printStatistics();

    abstract protected long numberTriples();

    public long numberTriangles() {
        long triangles = 0L;
        for (NodeT u : getActiveNodesShallowCopy()) {
            Collection<NodeT> ulinks = getNeighbors(u);
            for (NodeT v : getNeighbors(u)) {
                Collection<NodeT> vlinks = getNeighbors(v);
                vlinks.retainAll(ulinks);
                triangles += vlinks.size();
            }
        }
        return triangles;
    }

    public float getGlobalClusteringCoefficient() {
        return numberTriangles() / (float) numberTriples();
    }

    public float getLocalClusteringCoefficient() {
        long counter = 0L;
        float sumcoeff = 0.0f;
        for (NodeT u : getActiveNodesShallowCopy()) {
            Collection<NodeT> ulinks = getNeighbors(u);
            if (ulinks.size() > 1) {
                int compl = numberNeighbors(u);
                int crossing = 0;
                for (NodeT v : ulinks) {
                    Collection<NodeT> vlinks = getNeighbors(v);
                    vlinks.retainAll(ulinks);
                    crossing += vlinks.size();
                }
                sumcoeff += crossing / (compl * (compl - 1));
                ++counter;
            }
        }
        return sumcoeff / counter;
    }

    public float karger() {
        float cutCost = 0;
        try {
            Graph<NodeT> copy = (Graph<NodeT>) clone();
            while (copy.numberNodes() > 2) {
                copy.randomContraction();
            }

            for (Edge<NodeT> edge : copy) {
                cutCost += edge.weight;
            }
        }catch(CloneNotSupportedException e){
            logger.error(Thread.currentThread().getName() + "<" + Thread.currentThread().getId() + "> Cannot clone graph for min cut computation.");
            e.printStackTrace();
        }
        return cutCost;
    }

    private void randomContraction() {
        long index = Math.round(numberEdges() * Math.random());
        NodeT V = null;
        for (NodeT u : getActiveNodesShallowCopy()) {
            if (matrix.mat.containsKey(u)) {
                int degree = matrix.mat.get(u).size();
                if (index < degree) {
                    for (NodeT v : getNeighbors(u)) {
                        V = v;
                        break;
                    }
                    mergeNodes(u, V);
                    return;
                } else {
                    index -= degree;
                }
            }
        }
    }

    public Graph<NodeT> sssp(NodeT source) {
        return sssp(source, false);
    }

    public Graph<NodeT> sssp(NodeT source, boolean enableCache) {
        Graph<NodeT> sssp;
        if (UndirectedGraph.class.equals(getClass())) {
            sssp = new UndirectedGraph<>();
        } else if (DirectedGraph.class.equals(getClass())) {
            sssp = new DirectedGraph<>();
        } else {
            throw new AbstractMethodError("\n!! Call of uncertain origin for SSSPs computation method !!");
        }
        if (isEmpty()) {
            return sssp;
        }

        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        ArrayDeque<NodeT> queue = new ArrayDeque<>();
        dists.set (source, 0.0f);
        queue.add (source);
        while (!queue.isEmpty()) {
            NodeT u = queue.pop();
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    /* TEST FOR NEGATIVE CYCLES : traversing again and again from a negative edge to an already visited node */
                    if (entry.getValue() < 0 && paths.get(entry.getKey()) != null && UndirectedGraph.class.equals(getClass())) {
                        throw new RuntimeException("\n!! Traced a cycle with negative weight edges !!");
                    }

                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        dists.set(entry.getKey(), dists.get(u) + entry.getValue());
                        paths.set(entry.getKey(), u);
                        queue.add(entry.getKey());
                    }
                }
            }
        }

        for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
            sssp.setEdge(pair.getValue(), pair.getKey(), getEdgeWeight(pair.getValue(), pair.getKey()));
        }

        if (enableCache) {
            for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
                NodeT curr = pair.getKey();
                float pathcost = dists.get(curr);
                for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                    Map<NodeT, Float> dprec;
                    if (cached.containsKey(prec)) {
                        dprec = cached.get(prec);
                    } else {
                        dprec = new HashMap<>();
                        cached.put(prec, dprec);
                    }

                    float partialdist = pathcost - dists.get(prec);
                    if (dprec.containsKey(curr) && dprec.get(curr) <= partialdist) {
                        break;
                    } else {
                        dprec.put(curr, partialdist);
                    }

                    if (this instanceof UndirectedGraph<?>) {
                        Map<NodeT, Float> dcurr;
                        if (cached.containsKey(curr)) {
                            dcurr = cached.get(curr);
                        } else {
                            dcurr = new HashMap<>();
                            cached.put(curr, dcurr);
                        }

                        if (!dcurr.containsKey(prec) || dcurr.get(prec) > partialdist) {
                            dcurr.put(prec, partialdist);
                        }
                    }

                    curr = prec;
                    prec = paths.get(prec);
                }
            }
        }

        sssp.sinks.addAll(getActiveNodesShallowCopy());
        sssp.sinks.removeAll(sssp.matrix.mat.keySet());
        sssp.sinks.addAll(sinks);
        return sssp;
    }

    /**
     * @param source a source node upon which the shortest distances will be
     * computed
     * @return null in case of a negative cycle, or a graph containing all
     * Single Source Shortest Paths (SSSP) from the source node in any other
     * case
     */
    public Graph<NodeT> bellman(NodeT source) {
        return bellman(source, false);
    }

    public Graph<NodeT> bellman(NodeT source, boolean enableCache) {
        Graph<NodeT> sssp;
        if (UndirectedGraph.class.equals(getClass())) {
            sssp = new UndirectedGraph<>();
        } else if (DirectedGraph.class.equals(getClass())) {
            sssp = new DirectedGraph<>();
        } else {
            throw new AbstractMethodError("\n!! Call of uncertain origin for Bellman-Ford's algorithm !!");
        }
        if (isEmpty()) {
            return sssp;
        }

        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        paths.set(source, source);
        dists.set(source, 0.0f);
        for (NodeT u : new BFSiterable(source)) {
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        dists.set(entry.getKey(), dists.get(u) + entry.getValue());
                        paths.set(entry.getKey(), u);
                    }
                }
            }
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        return null;	// exists at least one cycle with a negative edge from u
                    }
                }
            }
        }
        for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
            sssp.setEdge(pair.getValue(), pair.getKey(), getEdgeWeight(pair.getValue(), pair.getKey()));
        }

        if (enableCache) {
            for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
                NodeT curr = pair.getKey();
                float pathcost = dists.get(curr);
                for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                    Map<NodeT, Float> dprec;
                    if (cached.containsKey(prec)) {
                        dprec = cached.get(prec);
                    } else {
                        dprec = new HashMap<>();
                        cached.put(prec, dprec);
                    }

                    float partialdist = pathcost - dists.get(prec);
                    if (dprec.containsKey(curr) && dprec.get(curr) <= partialdist) {
                        break;
                    } else {
                        dprec.put(curr, partialdist);
                    }

                    if (this instanceof UndirectedGraph<?>) {
                        Map<NodeT, Float> dcurr;
                        if (cached.containsKey(curr)) {
                            dcurr = cached.get(curr);
                        } else {
                            dcurr = new HashMap<>();
                            cached.put(curr, dcurr);
                        }

                        if (!dcurr.containsKey(prec) || dcurr.get(prec) > partialdist) {
                            dcurr.put(prec, partialdist);
                        }
                    }

                    curr = prec;
                    prec = paths.get(prec);
                }
            }
        }

        sssp.sinks.addAll(getActiveNodesShallowCopy());
        sssp.sinks.removeAll(sssp.matrix.mat.keySet());
        sssp.sinks.addAll(sinks);
        return sssp;
    }

    private VectorSparse<NodeT, Float> bellman() {
        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        if (isEmpty()) {
            return dists;
        }
        for (NodeT u : getActiveNodesShallowCopy()) {
            dists.set(u, 0.0f);
        }
        for (NodeT u : sinks) {
            dists.set(u, 0.0f);
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                    dists.set(entry.getKey(), dists.get(u) + entry.getValue());
                }
            }
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                    return null;	// exists at least one cycle with a negative edge from u
                }
            }
        }
        return dists;
    }

    public ArrayDeque<NodeT> getPath(NodeT u, NodeT v, boolean enableCache) {
        //return containsNegativeWeightEdges() || !isSparse() ? bellmanPath(u, v, enableCache) : dijkstraPath(u, v, enableCache);
        return dijkstraPath(u, v, enableCache);
    }

    public ArrayDeque<NodeT> getPath(NodeT u, NodeT v) {
        return getPath(u, v, false);
    }

    public float getPathCost(NodeT u, NodeT v) {
        if (cached.containsKey(u)) {
            if (cached.get(u).containsKey(v)) {
                return cached.get(u).get(v);
            } else {
                float cost = getPathCost(u, v, true);
                //cached.get(u).put(v,cost);
                return cost;
            }
        } else {
            float cost = getPathCost(u, v, true);
                //Map<NodeT,Float> tempmap = new HashMap<>();
            //tempmap.put(v, cost);
            //cached.put(u, tempmap);
            return cost;
        }
    }

    public float getPathCost(NodeT u, NodeT v, boolean enableCache) {
        if (enableCache && cached.containsKey(u)) {
            Map<NodeT, Float> dux = cached.get(u);
            if (dux.containsKey(v)) {
                return dux.get(v);
            }
        }
        return containsNegativeWeightEdges() ? bellmanPathCost(u, v, enableCache) : dijkstraPathCost(u, v, enableCache);
    }

    private float bellmanPathCost(NodeT source, NodeT target) {
        return bellmanPathCost(source, target, false);
    }

    private float bellmanPathCost(NodeT source, NodeT target, boolean enableCache) {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty() || source.equals(target)) {
            return 0.0f;
        }

        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        paths.set (source,source);
        dists.set (source,0.0f);
        if (cached.containsKey(source)) {
            for (Entry<NodeT, Float> entry : cached.get(source).entrySet()) {
                dists.set(entry.getKey(), entry.getValue());
                paths.set(entry.getKey(), source);
            }
        }
        for (NodeT u : new BFSiterable(source)) {
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        dists.set(entry.getKey(), dists.get(u) + entry.getValue());
                        paths.set(entry.getKey(), u);
                    }
                }
            }
        }

        /* exists at least one cycle on the route from u to v with a negative edge */
        for (NodeT u : getActiveNodesShallowCopy()) {
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        return Float.NEGATIVE_INFINITY;
                    }
                }
            }
        }

        if (enableCache) {
            for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
                NodeT curr = pair.getKey();
                float pathcost = dists.get(curr);
                for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                    Map<NodeT, Float> dprec;
                    if (cached.containsKey(prec)) {
                        dprec = cached.get(prec);
                    } else {
                        dprec = new HashMap<>();
                        cached.put(prec, dprec);
                    }

                    float partialdist = pathcost - dists.get(prec);
                    if (dprec.containsKey(curr) && dprec.get(curr) <= partialdist) {
                        break;
                    } else {
                        dprec.put(curr, partialdist);
                    }

                    if (this instanceof UndirectedGraph<?>) {
                        Map<NodeT, Float> dcurr;
                        if (cached.containsKey(curr)) {
                            dcurr = cached.get(curr);
                        } else {
                            dcurr = new HashMap<>();
                            cached.put(curr, dcurr);
                        }

                        if (!dcurr.containsKey(prec) || dcurr.get(prec) > partialdist) {
                            dcurr.put(prec, partialdist);
                        }
                    }

                    curr = prec;
                    prec = paths.get(prec);
                }
            }
        }
        return dists.get(target);
    }

    private ArrayDeque<NodeT> bellmanPath(NodeT source, NodeT target) {
        return bellmanPath(source, target, false);
    }

    private ArrayDeque<NodeT> bellmanPath(NodeT source, NodeT target, boolean enableCache) {
        if (isEmpty()) {
            return new ArrayDeque<>();
        }
        if (matrix.mat.isEmpty() || source.equals(target)) {
            return new ArrayDeque<>();
        }

        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        paths.set(source, source);
        dists.set(source, 0.0f);
        for (NodeT u : new BFSiterable(source)) {
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        dists.set(entry.getKey(), dists.get(u) + entry.getValue());
                        paths.set(entry.getKey(), u);
                    }
                }
            }
        }

        /* exists at least one cycle on the route from u to v with a negative edge */
        for (NodeT u : getActiveNodesShallowCopy()) {
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    if (dists.get(u) + entry.getValue() < dists.get(entry.getKey())) {
                        return null;
                    }
                }
            }
        }

        if (enableCache) {
            for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
                NodeT curr = pair.getKey();
                float pathcost = dists.get(curr);
                for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                    Map<NodeT, Float> dprec;
                    if (cached.containsKey(prec)) {
                        dprec = cached.get(prec);
                    } else {
                        dprec = new HashMap<>();
                        cached.put(prec, dprec);
                    }

                    float partialdist = pathcost - dists.get(prec);
                    if (dprec.containsKey(curr) && dprec.get(curr) <= partialdist) {
                        break;
                    } else {
                        dprec.put(curr, partialdist);
                    }

                    if (this instanceof UndirectedGraph<?>) {
                        Map<NodeT, Float> dcurr;
                        if (cached.containsKey(curr)) {
                            dcurr = cached.get(curr);
                        } else {
                            dcurr = new HashMap<>();
                            cached.put(curr, dcurr);
                        }

                        if (!dcurr.containsKey(prec) || dcurr.get(prec) > partialdist) {
                            dcurr.put(prec, partialdist);
                        }
                    }

                    curr = prec;
                    prec = paths.get(prec);
                }
            }
        }

        ArrayDeque<NodeT> path = new ArrayDeque<>();
        for (NodeT u = target; !u.equals(source); u = paths.get(u)) {
            path.addFirst(u);
        }
        path.addFirst(source);
        return path;
    }

    public Graph<NodeT> dijkstra(NodeT source) {
        return dijkstra(source, false);
    }

    private Graph<NodeT> dijkstra(NodeT source, boolean enableCache) {
        if (containsNegativeWeightEdges()) {
            throw new RuntimeException("\n!! Dijkstra's algortihm cannot be used in the presence of edges with negative weights !!");
        }

        Graph<NodeT> sssp;
        if (UndirectedGraph.class.equals(getClass())) {
            sssp = new UndirectedGraph<>();
        } else if (DirectedGraph.class.equals(getClass())) {
            sssp = new DirectedGraph<>();
        } else {
            throw new AbstractMethodError("\n!! Call of uncertain origin for Dijksrta's algorithm !!");
        }
        if (isEmpty()) {
            return sssp;
        }

        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        MinPQ<Edge<NodeT>> edges = new MinPQ<>();
        HashSet<NodeT> processed = new HashSet<>();

        processed.add(source);
        dists.set(source, 0.0f);
        paths.set(source, source);

        if (matrix.mat.containsKey(source)) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(source).entrySet()) {
                edges.insert(new Edge<>(source, entry.getKey(), entry.getValue()));
            }
        } else {
            sssp.sinks.addAll(getActiveNodesShallowCopy());
            sssp.sinks.addAll(sinks);
            return sssp;
        }

        while (!edges.isEmpty()) {
            Edge<NodeT> probed = edges.delMin();
            if (probed.weight < dists.get(probed.to)) {
                processed.add(probed.to);
                paths.set(probed.to, probed.from);
                dists.set(probed.to, probed.weight);
                /*
                 edges.clear();
                 for (NodeT u : processed)
                 for (NodeT v : getNeighbors(u)) {
                 float newdist = dists.get(u)+getEdgeWeight(u,v);
                 if (newdist < dists.get(v))
                 edges.insert(new Edge<>(u,v,newdist));
                 }
                 */
                for (Edge<NodeT> edge : getEdgesFrom(probed.to)) {
                    if (probed.weight + edge.weight < dists.get(edge.to)) {
                        edges.insert(new Edge<>(probed.to, edge.to, probed.weight + edge.weight));
                    }
                }
            }
        }

        for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
            sssp.setEdge(pair.getValue(), pair.getKey(), getEdgeWeight(pair.getValue(), pair.getKey()));
        }

        if (enableCache) {
            for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
                NodeT curr = pair.getKey();
                float pathcost = dists.get(curr);
                for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                    Map<NodeT, Float> dprec;
                    if (cached.containsKey(prec)) {
                        dprec = cached.get(prec);
                    } else {
                        dprec = new HashMap<>();
                        cached.put(prec, dprec);
                    }

                    float partialdist = pathcost - dists.get(prec);
                    if (dprec.containsKey(curr) && dprec.get(curr) <= partialdist) {
                        break;
                    } else {
                        dprec.put(curr, partialdist);
                    }

                    if (this instanceof UndirectedGraph<?>) {
                        Map<NodeT, Float> dcurr;
                        if (cached.containsKey(curr)) {
                            dcurr = cached.get(curr);
                        } else {
                            dcurr = new HashMap<>();
                            cached.put(curr, dcurr);
                        }

                        if (!dcurr.containsKey(prec) || dcurr.get(prec) > partialdist) {
                            dcurr.put(prec, partialdist);
                        }
                    }

                    curr = prec;
                    prec = paths.get(prec);
                }
            }
        }

        sssp.sinks.addAll(getActiveNodesShallowCopy());
        sssp.sinks.removeAll(sssp.matrix.mat.keySet());
        sssp.sinks.addAll(sinks);
        return sssp;
    }

    public Map<NodeT, Float> dijkstraDistances(NodeT source) {
        if (containsNegativeWeightEdges()) {
            throw new RuntimeException("\n!! Dijkstra's algortihm cannot be used in the presence of edges with negative weights !!");
        }

        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        MinPQ<Edge<NodeT>> edges = new MinPQ<>();
        HashSet<NodeT> processed = new HashSet<>();

        processed.add(source);
        dists.set(source, 0.0f);
        paths.set(source, source);

        if (matrix.mat.containsKey(source)) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(source).entrySet()) {
                edges.insert(new Edge<>(source, entry.getKey(), entry.getValue()));
            }
        } else {
            return dists.thisMap();
        }

        while (!edges.isEmpty()) {
            Edge<NodeT> probed = edges.delMin();
            if (probed.weight < dists.get(probed.to)) {
                processed.add(probed.to);
                paths.set(probed.to, probed.from);
                dists.set(probed.to, probed.weight);
                /*
                 edges.clear();
                 for (NodeT u : processed)
                 for (NodeT v : getNeighbors(u)) {
                 float newdist = dists.get(u)+getEdgeWeight(u,v);
                 if (newdist < dists.get(v))
                 edges.insert(new Edge<>(u,v,newdist));
                 }
                 */
                for (Edge<NodeT> edge : getEdgesFrom(probed.to)) {
                    if (probed.weight + edge.weight < dists.get(edge.to)) {
                        edges.insert(new Edge<>(probed.to, edge.to, probed.weight + edge.weight));
                    }
                }
            }
        }
        return dists.thisMap();
    }

    private float dijkstraPathCost(NodeT source, NodeT target) {
        return dijkstraPathCost(source, target, false);
    }

    private float dijkstraPathCost(NodeT source, NodeT target, boolean enableCache) {
        if (isEmpty() || matrix.mat.isEmpty()) {
            return Float.NaN;
        }
        if (source.equals(target)) {
            return 0.0f;
        } else if (containsNegativeWeightEdges()) {
            throw new RuntimeException("\n!! Dijkstra's algortihm cannot be used in the presence of edges with negative weights !!");
        }

        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        MinPQ<Edge<NodeT>> edges = new MinPQ<>();

        dists.set(source, 0.0f);
        /**/
        Map<NodeT, Float> sourcecache = cached.get(source);
        if (enableCache) {
            if (sourcecache != null) { // exploit cached distances
                ArrayDeque<NodeT> pool = new ArrayDeque<>();
                for (Entry<NodeT, Float> cachedEntry : sourcecache.entrySet()) {
                    dists.set(cachedEntry.getKey(), cachedEntry.getValue());
                }

                for (Entry<NodeT, Float> cachedEntry : sourcecache.entrySet()) {
                    if (matrix.mat.containsKey(cachedEntry.getKey())) {
                        for (Entry<NodeT, Float> entry : matrix.mat.get(cachedEntry.getKey()).entrySet()) {
                            if (cachedEntry.getValue() + entry.getValue() < dists.get(entry.getKey())) {
                                edges.insert(new Edge<>(cachedEntry.getKey(), entry.getKey(), cachedEntry.getValue() + entry.getValue()));

                                dists.set(entry.getKey(), cachedEntry.getValue() + entry.getValue());

                                if (cachedEntry.getValue() + entry.getValue() <= maxCachingRadius) {
                                    pool.add(entry.getKey());
                                }
                            }
                        }
                    }
                }
                for (NodeT u : pool) {
                    sourcecache.put(u, dists.get(u));
                }
            } else {
                edges.insert(new Edge<>(source, source, 0.0f));

                sourcecache = new HashMap<>();
                cached.put (source,sourcecache);
                sourcecache.put (source,0.0f);
            }
        } else {
            edges.insert(new Edge<>(source, source, 0.0f));
        }
        /**/
        while (!edges.isEmpty()) {
            Edge<NodeT> probed = edges.delMin();
            if (target.equals(probed.to)) {
                return probed.weight;
            } else {
                Map<NodeT, Float> nodecache = cached.get(probed.to);
                if (nodecache != null) {
                    for (Entry<NodeT, Float> cachedEntry : nodecache.entrySet()) {
                        if (probed.weight + cachedEntry.getValue() < dists.get(cachedEntry.getKey())) {
                            dists.set(cachedEntry.getKey(), probed.weight + cachedEntry.getValue());

                            if (enableCache && cachedEntry.getValue() + probed.weight <= maxCachingRadius) {
                                sourcecache.put(cachedEntry.getKey(), probed.weight + cachedEntry.getValue());
                            }
                        }
                    }
                }

                for (Edge<NodeT> edge : getEdgesFrom(probed.to)) {
                    if (probed.weight + edge.weight < dists.get(edge.to)) {
                        edges.insert(new Edge<>(probed.to, edge.to, probed.weight + edge.weight));
                        dists.set(edge.to, probed.weight + edge.weight);

                        if (enableCache && edge.weight + probed.weight <= maxCachingRadius) {
                            sourcecache.put(edge.to, probed.weight + edge.weight);
                        }
                    }
                }
            }
        }
        return Float.MAX_VALUE;
        //throw new RuntimeException("\n!! ERROR - Source node "+source+" and target node "+target+" not connected !!");
    }

    private ArrayDeque<NodeT> dijkstraPath(NodeT source, NodeT target) {
        return dijkstraPath(source, target, false);
    }

    private ArrayDeque<NodeT> dijkstraPath(NodeT source, NodeT target, boolean enableCache) {
        if (isEmpty()) {
            return new ArrayDeque<>();
        }
        if (matrix.mat.isEmpty() || source.equals(target)) {
            return new ArrayDeque<>();
        } else if (containsNegativeWeightEdges()) {
            throw new RuntimeException("\n!! Dijkstra's algortihm cannot be used in the presence of edges with negative weights !!");
        }

        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        MinPQ<Edge<NodeT>> edges = new MinPQ<>();
        HashSet<NodeT> processed = new HashSet<>();

        dists.set(source, 0.0f);
        paths.set(source, source);
        processed.add(source);
        if (matrix.mat.containsKey(source)) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(source).entrySet()) {
                edges.insert(new Edge<>(source, entry.getKey(), entry.getValue()));
            }
        } else {
            throw new RuntimeException("\n!! ERROR - Source of path not in the graph !!");
        }

        while (!edges.isEmpty()) {
            Edge<NodeT> probed = edges.delMin();
            if (target.equals(probed.to)) {
                if (enableCache) {
                    for (Entry<NodeT, NodeT> pair : paths.entrySet()) {
                        NodeT curr = pair.getKey();
                        float pathcost = dists.get(curr);
                        for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                            Map<NodeT, Float> dprec;
                            if (cached.containsKey(prec)) {
                                dprec = cached.get(prec);
                            } else {
                                dprec = new HashMap<>();
                                cached.put(prec, dprec);
                            }

                            float partialdist = pathcost - dists.get(prec);
                            if (dprec.containsKey(curr) && dprec.get(curr) <= partialdist) {
                                break;
                            } else {
                                dprec.put(curr, partialdist);
                            }

                            if (this instanceof UndirectedGraph<?>) {
                                Map<NodeT, Float> dcurr;
                                if (cached.containsKey(curr)) {
                                    dcurr = cached.get(curr);
                                } else {
                                    dcurr = new HashMap<>();
                                    cached.put(curr, dcurr);
                                }

                                if (!dcurr.containsKey(prec) || dcurr.get(prec) > partialdist) {
                                    dcurr.put(prec, partialdist);
                                }
                            }

                            curr = prec;
                            prec = paths.get(prec);
                        }
                    }
                }

                ArrayDeque<NodeT> path = new ArrayDeque<>();
                path.addFirst(target);
                for (NodeT u = probed.from; !u.equals(source); u = paths.get(u)) {
                    path.addFirst(u);
                }
                path.addFirst(source);
                return path;
            }

            if (probed.weight < dists.get(probed.to)) {
                paths.set(probed.to, probed.from);
                dists.set(probed.to, probed.weight);
                processed.add(probed.to);
                /*
                 edges.clear();
                 for (NodeT u : processed)
                 for (NodeT v : getNeighbors(u))
                 if (!processed.contains(v))
                 edges.insert(new Edge<>(u,v,dists.get(u)+getEdgeWeight(u,v)));
                 */
                for (Edge<NodeT> edge : getEdgesFrom(probed.to)) {
                    if (probed.weight + edge.weight < dists.get(edge.to)) {
                        edges.insert(new Edge<>(probed.to, edge.to, probed.weight + edge.weight));
                    }
                }
            }
        }
        throw new RuntimeException("\n!! ERROR - Source and target nodes not connected !!");
    }

    /**
     * @param source node upon which the distances are computed
     * @return The maximum distance of the path originated from the specified
     * node, or Float.NaN when no path exists.
     */
    private float dijkstraMaxPathCost(NodeT source) {
        return dijkstraMaxPathCost(source, null);
    }

    private float dijkstraMaxPathCost(NodeT source, VectorSparse<NodeT, Float> distTransformation) {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty()) {
            return 0.0f;
        } else if (containsNegativeWeightEdges()) {
            throw new RuntimeException("\n!! Dijkstra's algortihm cannot be used in the presence of edges with negative weights !!");
        }

        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        MinPQ<Edge<NodeT>> edges = new MinPQ<>();
        HashSet<NodeT> processed = new HashSet<>();

        dists.set(source, 0.0f);
        paths.set(source, source);
        processed.add(source);
        if (matrix.mat.containsKey(source)) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(source).entrySet()) {
                edges.insert(new Edge<>(source, entry.getKey(), entry.getValue()));
            }
        } else {
            return Float.NaN;
        }

        float maxdist = Float.NEGATIVE_INFINITY;
        while (!edges.isEmpty()) {
            Edge<NodeT> probed = edges.delMin();
            if (probed.weight < dists.get(probed.to)) {
                paths.set(probed.to, probed.from);
                dists.set(probed.to, probed.weight);
                processed.add(probed.to);

                if (distTransformation == null) {
                    if (maxdist < probed.weight) {
                        maxdist = probed.weight;
                    }
                } else {
                    if (maxdist < probed.weight + distTransformation.get(probed.to)) {
                        maxdist = probed.weight + distTransformation.get(probed.to);
                    }
                }
                /*
                 edges.clear();
                 for (NodeT u : processed)
                 for (NodeT v : getNeighbors(u))
                 if (!processed.contains(v))
                 edges.insert(new Edge<>(u,v,dists.get(u)+getEdgeWeight(u,v)));
                 */
                for (Edge<NodeT> edge : getEdgesFrom(probed.to)) {
                    if (probed.weight + edge.weight < dists.get(edge.to)) {
                        edges.insert(new Edge<>(probed.to, edge.to, probed.weight + edge.weight));
                    }
                }
            }
        }
        return distTransformation == null ? maxdist : maxdist - distTransformation.get(source);
    }

    private float dijkstraAveragePathCost(NodeT source) {
        return dijkstraAveragePathCost(source, null);
    }

    private float dijkstraAveragePathCost(NodeT source, VectorSparse<NodeT, Float> distTransformation) {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty()) {
            return 0.0f;
        } else if (containsNegativeWeightEdges()) {
            throw new RuntimeException("\n!! Dijkstra's algortihm cannot be used in the presence of edges with negative weights !!");
        }

        VectorSparse<NodeT, NodeT> paths = new VectorSparse<>();
        VectorSparse<NodeT, Float> dists = new VectorSparse<>(Float.POSITIVE_INFINITY);
        MinPQ<Edge<NodeT>> edges = new MinPQ<>();
        HashSet<NodeT> processed = new HashSet<>();

        dists.set(source, 0.0f);
        paths.set(source, source);
        processed.add(source);
        if (matrix.mat.containsKey(source)) {
            for (Entry<NodeT, Float> entry : matrix.mat.get(source).entrySet()) {
                edges.insert(new Edge<>(source, entry.getKey(), entry.getValue()));
            }
        } else {
            return Float.NaN;
        }

        float sumdist = 0.0f;
        while (!edges.isEmpty()) {
            Edge<NodeT> probed = edges.delMin();
            if (probed.weight < dists.get(probed.to)) {
                paths.set(probed.to, probed.from);
                dists.set(probed.to, probed.weight);
                processed.add(probed.to);

                sumdist += distTransformation == null ? probed.weight : probed.weight + distTransformation.get(probed.to);
                /*
                 edges.clear();
                 for (NodeT u : processed)
                 for (NodeT v : getNeighbors(u))
                 if (!processed.contains(v))
                 edges.insert(new Edge<>(u,v,dists.get(u)+getEdgeWeight(u,v)));
                 */
                for (Edge<NodeT> edge : getEdgesFrom(probed.to)) {
                    if (probed.weight + edge.weight < dists.get(edge.to)) {
                        edges.insert(new Edge<>(probed.to, edge.to, probed.weight + edge.weight));
                    }
                }
            }
        }
        return distTransformation == null ? sumdist / (paths.size() - 1) : (sumdist - (paths.size() - 1) * distTransformation.get(source)) / (paths.size() - 1);
    }

    public Map<NodeT, Graph<NodeT>> johnson() {
        HashMap<NodeT, Graph<NodeT>> apsp = new HashMap<>();
        if (isEmpty()) {
            return apsp;
        }

        if (!containsNegativeWeightEdges()) {
            for (NodeT u : getActiveNodesShallowCopy()) {
                apsp.put(u, dijkstra(u, false));
            }

            for (NodeT u : sinks) {
                Graph<NodeT> sinkGraph = null;
                if (UndirectedGraph.class.equals(getClass())) {
                    sinkGraph = new UndirectedGraph<>();
                } else if (DirectedGraph.class.equals(getClass())) {
                    sinkGraph = new DirectedGraph<>();
                } else {
                    throw new AbstractMethodError("\n!! Call of uncertain origin for Johnson's algorithm !!");
                }

                sinkGraph.sinks.addAll(getActiveNodesShallowCopy());
                sinkGraph.sinks.addAll(sinks);

                apsp.put(u, sinkGraph);
            }
        } else {
            /* trace negative cycles and compute re-weighting function */
            VectorSparse<NodeT, Float> distTransform = bellman();
            if (distTransform == null) {
                throw new RuntimeException("\n!! Johnson's algortihm cannot be used in the presence of cycles with negative weight edges !!");
            }

            /* re-weighting taking place here */
            Graph<NodeT> reweighted = null;
            if (UndirectedGraph.class.equals(getClass())) {
                reweighted = new UndirectedGraph<>();
            } else if (DirectedGraph.class.equals(getClass())) {
                reweighted = new DirectedGraph<>();
            } else {
                throw new AbstractMethodError("\n!! Call of uncertain origin for Johnson's algorithm !!");
            }

            for (Edge<NodeT> edge : this) {
                reweighted.setEdge(edge.from, edge.to, edge.weight + distTransform.get(edge.from) - distTransform.get(edge.to));
            }

            /* Contruct SSSPs from the reweighted graph */
            for (NodeT u : getActiveNodesShallowCopy()) {
                Graph<NodeT> sssp = null;
                if (UndirectedGraph.class.equals(getClass())) {
                    sssp = new UndirectedGraph<>();
                } else if (DirectedGraph.class.equals(getClass())) {
                    sssp = new DirectedGraph<>();
                } else {
                    throw new AbstractMethodError("\n!! Call of uncertain origin for Johnson's algorithm !!");
                }

                for (Edge<NodeT> edge : reweighted.dijkstra(u, false)) // set original edge weight though
                {
                    sssp.setEdge(edge.from, edge.to, getEdgeWeight(edge.from, edge.to));
                }

                sssp.sinks.addAll(getActiveNodesShallowCopy());
                sssp.sinks.removeAll(sssp.matrix.mat.keySet());
                sssp.sinks.addAll(sinks);
                apsp.put(u, sssp);
            }

            /* for the sake of consistency add sinks in the separate structure */
            for (NodeT u : sinks) {
                Graph<NodeT> sssp = null;
                if (UndirectedGraph.class.equals(getClass())) {
                    sssp = new UndirectedGraph<>();
                } else if (DirectedGraph.class.equals(getClass())) {
                    sssp = new DirectedGraph<>();
                } else {
                    throw new AbstractMethodError("\n!! Call of uncertain origin for Johnson's algorithm !!");
                }

                sssp.sinks.addAll(getActiveNodesShallowCopy());
                sssp.sinks.addAll(sinks);
                apsp.put(u, sssp);
            }
        }
        return apsp;
    }

    public Map<NodeT, Graph<NodeT>> floyd() {
        return floyd(false);
    }

    public Map<NodeT, Graph<NodeT>> floyd(boolean enableCache) {
        if (containsNegativeCycles()) {
            throw new RuntimeException("\n!! Floyd-Warshall algortihm cannot be used in the presence of cycles with negative weight edges !!");
        }
        HashMap<NodeT, Graph<NodeT>> apsp = new HashMap<>();
        if (isEmpty()) {
            return apsp;
        }

        MatrixSparse<NodeT, NodeT> dists = new MatrixSparse<>(Float.POSITIVE_INFINITY);
        VectorSparse<NodeT, VectorSparse<NodeT, NodeT>> paths = new VectorSparse<>();
        for (NodeT u : getActiveNodesShallowCopy()) {
            Graph<NodeT> sssp = null;
            if (UndirectedGraph.class.equals(getClass())) {
                sssp = new UndirectedGraph<>();
            } else if (DirectedGraph.class.equals(getClass())) {
                sssp = new DirectedGraph<>();
            } else {
                throw new RuntimeException("\n!! Call of uncertain origin for APSPs computation method !!");
            }
            VectorSparse<NodeT, NodeT> duxy = new VectorSparse<>();
            paths.set(u, duxy);
            duxy.set(u, u);
            dists.set(u, u, 0.0f);
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    dists.set(u, entry.getKey(), entry.getValue());
                    paths.get(u).set(entry.getKey(), u);
                }
            }
            sssp.sinks.addAll(sinks);
            apsp.put(u, sssp);
        }

        for (NodeT u : sinks) {
            Graph<NodeT> sssp = null;
            if (UndirectedGraph.class.equals(getClass())) {
                sssp = new UndirectedGraph<>();
            } else if (DirectedGraph.class.equals(getClass())) {
                sssp = new DirectedGraph<>();
            } else {
                throw new RuntimeException("\n!! Call of uncertain origin for APSPs computation method !!");
            }
            VectorSparse<NodeT, NodeT> duxy = new VectorSparse<>();
            paths.set(u, duxy);
            duxy.set(u, u);
            dists.set(u, u, 0.0f);
            sssp.sinks.addAll(getActiveNodesShallowCopy());
            sssp.sinks.addAll(sinks);
            apsp.put(u, sssp);
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            for (NodeT v : getActiveNodesShallowCopy()) {
                for (NodeT w : getActiveNodesShallowCopy()) {
                    if (dists.get(v, w) > dists.get(v, u) + dists.get(u, w)) {
                        dists.set(v, w, dists.get(v, u) + dists.get(u, w));
                        paths.get(v).set(w, u);
                    }
                }
            }
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            for (NodeT v : getActiveNodesShallowCopy()) {
                for (NodeT w : sinks) {
                    if (dists.get(v, w) > dists.get(v, u) + dists.get(u, w)) {
                        dists.set(v, w, dists.get(v, u) + dists.get(u, w));
                        paths.get(v).set(w, u);
                    }
                }
            }
        }

        if (enableCache) {
            resetCache();
            for (NodeT u : getActiveNodesShallowCopy()) {
                Map<NodeT, Float> dux = new HashMap<>();
                cached.put(u, dux);
                for (NodeT v : getActiveNodesShallowCopy()) {
                    float dist = apspPathReconstruction(u, v, paths, apsp.get(u));
                    if (dist != Float.NaN) {
                        dux.put(v, dist);
                    }
                }
                if (paths.get(u).isEmpty()) {
                    apsp.get(u).sinks.add(u);
                }
            }

            for (NodeT u : getActiveNodesShallowCopy()) {
                Map<NodeT, Float> dux = cached.get(u);
                for (NodeT v : sinks) {
                    dux.put(v, apspPathReconstruction(u, v, paths, apsp.get(u)));
                }
            }
        } else {
            for (NodeT u : getActiveNodesShallowCopy()) {
                for (NodeT v : getActiveNodesShallowCopy()) {
                    apspPathReconstruction(u, v, paths, apsp.get(u));
                }
                if (paths.get(u).isEmpty()) {
                    apsp.get(u).sinks.add(u);
                }
            }

            for (NodeT u : getActiveNodesShallowCopy()) {
                for (NodeT v : sinks) {
                    apspPathReconstruction(u, v, paths, apsp.get(u));
                }
            }
        }
        return apsp;
    }

    private float apspPathReconstruction(NodeT u, NodeT v, VectorSparse<NodeT, VectorSparse<NodeT, NodeT>> paths, Graph<NodeT> sssp) {
        if (u.equals(v)) {
            return 0.0f;
        }
        NodeT w = paths.get(u).get(v);
        if (w != null) {
            Float dwv = getEdgeWeight(w, v);
            if (!dwv.equals(Float.NaN)) {
                sssp.setEdge(w, v, dwv);
            } else {
                if (!w.equals(v)) {
                    dwv = apspPathReconstruction(w, v, paths, sssp);
                } else {
                    dwv = 0.0f;
                }
            }

            Float duw = getEdgeWeight(u, w);
            if (!duw.equals(Float.NaN)) {
                sssp.setEdge(u, w, duw);
            } else {
                if (!u.equals(w)) {
                    duw = apspPathReconstruction(u, w, paths, sssp);
                } else {
                    duw = 0.0f;
                }
            }

            return duw + dwv;
        } else {
            return Float.NaN;
        }
    }

    public float getMaxPathCost() {
        return isSparse() ? getMaxSparsePathCost() : getMaxDensePathCost();
    }

    public float getAveragePathCost() {
        return isSparse() ? getAverageSparsePathCost() : getAverageDensePathCost();
    }

    /**
     * Based on floyd distance matrix
     *
     * @return
     */
    private float getMaxDensePathCost() {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty()) {
            return 0.0f;
        }
        if (containsNegativeCycles()) {
            return Float.NEGATIVE_INFINITY;
        }
        MatrixSparse<NodeT, NodeT> dists = new MatrixSparse<>(Float.POSITIVE_INFINITY);
        for (NodeT u : getActiveNodesShallowCopy()) {
            dists.set(u, u, 0.0f);
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    dists.set(u, entry.getKey(), entry.getValue());
                }
            }
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            for (NodeT v : getActiveNodesShallowCopy()) {
                for (NodeT w : getActiveNodesShallowCopy()) {
                    float newdist = dists.get(v, u) + dists.get(u, w);
                    if (dists.get(v, w) > newdist) {
                        dists.set(v, w, newdist);
                    }
                }
            }
        }

        float maxdist = Float.NEGATIVE_INFINITY;
        for (Entry<NodeT, Map<NodeT, Float>> outer : dists.mat.entrySet()) {
            for (Entry<NodeT, Float> inner : outer.getValue().entrySet()) {
                if (maxdist < inner.getValue()) {
                    maxdist = inner.getValue();
                }
            }
        }
        return maxdist;
    }

    /**
     * Based on floyd distance matrix
     *
     * @return
     */
    private float getAverageDensePathCost() {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty()) {
            return 0.0f;
        }
        if (containsNegativeCycles()) {
            return Float.NEGATIVE_INFINITY;
        }
        MatrixSparse<NodeT, NodeT> dists = new MatrixSparse<>(Float.POSITIVE_INFINITY);
        for (NodeT u : getActiveNodesShallowCopy()) {
            dists.set(u, u, 0.0f);
            if (matrix.mat.containsKey(u)) {
                for (Entry<NodeT, Float> entry : matrix.mat.get(u).entrySet()) {
                    dists.set(u, entry.getKey(), entry.getValue());
                }
            }
        }

        for (NodeT u : getActiveNodesShallowCopy()) {
            for (NodeT v : getActiveNodesShallowCopy()) {
                for (NodeT w : getActiveNodesShallowCopy()) {
                    float newdist = dists.get(v, u) + dists.get(u, w);
                    if (dists.get(v, w) > newdist) {
                        dists.set(v, w, newdist);
                    }
                }
            }
        }

        long counter = 0L;
        float sumdist = 0.0f;
        for (NodeT u : getActiveNodesShallowCopy()) {
            for (NodeT v : getActiveNodesShallowCopy()) {
                if ((!u.equals(v) || containsEdge(u, v)) && dists.get(u, v) != Float.POSITIVE_INFINITY) {
                    sumdist += dists.get(u, v);
                    ++counter;
                }
            }
        }

        return sumdist / counter;
    }

    /**
     * Based on Johnson's algorithm which transforms edge weights before running
     * Dijkstra's algorithm from each graph vertex
     *
     * @return maximal path distance
     */
    private float getMaxSparsePathCost() {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty()) {
            return 0.0f;
        }
        float maxdist = Float.NEGATIVE_INFINITY;
        if (containsNegativeWeightEdges()) {
            /* trace negative cycles and compute re-weighting function */
            VectorSparse<NodeT, Float> distTransform = bellman();
            if (distTransform == null) {
                return Float.NaN;
            }

            /* re-weighting taking place here */
            Graph<NodeT> reweighted = null;
            if (UndirectedGraph.class.equals(getClass())) {
                reweighted = new UndirectedGraph<>();
            } else if (DirectedGraph.class.equals(getClass())) {
                reweighted = new DirectedGraph<>();
            } else {
                throw new AbstractMethodError("\n!! Call of uncertain origin for SSSPs computation method !!");
            }

            for (Edge<NodeT> edge : this) {
                reweighted.setEdge(edge.from, edge.to,
                        edge.weight + distTransform.get(edge.from) - distTransform.get(edge.to));
            }

            for (NodeT u : getActiveNodesShallowCopy()) {	// transformed edge weight can be different than the initial
                float newdist = reweighted.dijkstraMaxPathCost(u, distTransform);
                if (newdist > maxdist) {
                    maxdist = newdist;
                }
            }
        } else {
            for (NodeT u : getActiveNodesShallowCopy()) {
                float newdist = dijkstraMaxPathCost(u);
                if (newdist > maxdist) {
                    maxdist = newdist;
                }
            }
        }
        return maxdist;
    }

    /**
     * Based on Johnson's algorithm which transforms edge weights before running
     * Dijkstra's algorithm from each graph vertex
     *
     * @return average path distance
     */
    private float getAverageSparsePathCost() {
        if (isEmpty()) {
            return Float.NaN;
        }
        if (matrix.mat.isEmpty()) {
            return 0.0f;
        }
        long counter = 0L;
        float sumdist = 0.0f;
        if (containsNegativeWeightEdges()) {
            /* trace negative cycles and compute re-weighting function */
            VectorSparse<NodeT, Float> distTransform = bellman();
            if (distTransform == null) {
                return Float.NEGATIVE_INFINITY;
            }

            /* re-weighting taking place here */
            Graph<NodeT> reweighted = null;
            if (UndirectedGraph.class.equals(getClass())) {
                reweighted = new UndirectedGraph<NodeT>();
            } else if (DirectedGraph.class.equals(getClass())) {
                reweighted = new DirectedGraph<NodeT>();
            } else {
                throw new AbstractMethodError("\n!! Call of uncertain origin for SSSPs computation method !!");
            }

            for (Edge<NodeT> edge : this) {
                reweighted.setEdge(edge.from, edge.to, edge.weight + distTransform.get(edge.from) - distTransform.get(edge.to));
            }

            for (NodeT u : getActiveNodesShallowCopy()) {	// transformed edge weight can be different than the initial
                sumdist += reweighted.dijkstraAveragePathCost(u, distTransform);
                ++counter;
            }
        } else {
            for (NodeT u : getActiveNodesShallowCopy()) {
                sumdist += dijkstraAveragePathCost(u);
                ++counter;
            }
        }
        return sumdist / counter;
    }

    public void setErdosRenyiGraph(float p) {
        setErdosRenyiGraph(p, 1.0f);
    }

    abstract protected void setErdosRenyiGraph(float p, float weight);

    public void addConnectedNode(NodeT u, int m, float w) {
        ArrayList<NodeT> vertices = getNodes();
        for (int i = 0; i < m; ++i) {
            int index = 0;
            do {
                index = (int) (Math.random() * vertices.size());
            } while (containsEdge(u, vertices.get(index)));
            setEdge(new Edge<>(u, vertices.get(index), w));
        }
    }

    public void addPreferentialNode(NodeT u, int m, float w) {
        ArrayList<NodeT> vertices = getNodes();
        ArrayList<NodeT> expanded = new ArrayList<>(vertices);
        int j = 0;
        for (NodeT v : vertices) {
            ++j;
            for (int i = 0; i < getInDegree(v); ++i) {
                expanded.add(j, v);
                ++j;
            }
        }

        for (int i = 0; i < m; ++i) {
            int index = 0;
            do {
                index = (int) (Math.random() * expanded.size());
            } while (u.equals(expanded.get(index))
                    || this.containsEdge(u, expanded.get(index)));
            setEdge(u, expanded.get(index), w);
        }
    }

    abstract public int getInDegree(NodeT u);

    public int getOutDegree(NodeT u) {
        return matrix.mat.containsKey(u) ? matrix.mat.get(u).size() : 0;
    }

    public void draw(int width, int height) {
        JFrame frame = new JFrame("Graph Window");
        JFrame.setDefaultLookAndFeelDecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setVisible(true);

        paint(frame.getGraphics(), width, height);
    }

    private void paint(Graphics g, int width, int height) {
        FontMetrics f = g.getFontMetrics();
        g.setColor(Color.black);

        height = Math.max(height, f.getHeight());
        width = Math.max(width, height);

        int Radius = Math.min(width, height) / 2;
        int nodeHeight = (int) (Radius * Math.PI / (float) numberNodes()) / 2;
        int nodeWidth = nodeHeight;
        Radius -= (nodeHeight << 1);

        int i = 0;
        for (NodeT u : getActiveNodesShallowCopy()) {
            float theta = i++ % (2 * (float) Math.PI);
            int fromx = width / 2 + (int) (Radius * Math.cos(theta));
            int fromy = height / 2 + (int) (Radius * Math.sin(theta));

            for (NodeT link : getNeighbors(u)) {
                int j = 0;
                for (NodeT v : getActiveNodesShallowCopy()) {
                    if (v.equals(link)) {
                        break;
                    } else {
                        ++j;
                    }
                }

                float phi = j % (2 * (float) Math.PI);
                int tox = width / 2 + (int) (Radius * Math.cos(phi));
                int toy = height / 2 + (int) (Radius * Math.sin(phi));
                g.drawLine(fromx, fromy, tox, toy);

                if (!matrix.mat.containsKey(link)) {
                    g.setColor(Color.white);
                    g.fillOval(tox - nodeWidth / 2, toy - nodeHeight / 2, nodeWidth, nodeHeight);
                    g.setColor(Color.black);
                    g.drawOval(tox - nodeWidth / 2, toy - nodeHeight / 2, nodeWidth, nodeHeight);

                    String toName = link.toString();
                    g.drawString(toName, tox - f.stringWidth(toName) / 2, toy + f.getHeight() / 2);
                }
            }
            g.setColor(Color.white);
            g.fillOval(fromx - nodeWidth / 2, fromy - nodeHeight / 2, nodeWidth, nodeHeight);
            g.setColor(Color.black);
            g.drawOval(fromx - nodeWidth / 2, fromy - nodeHeight / 2, nodeWidth, nodeHeight);

            String fromName = u.toString();
            g.drawString(fromName, fromx - f.stringWidth(fromName) / 2, fromy + f.getHeight() / 2);
        }
    }
}
