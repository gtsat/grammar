package contraction;

import grammar.Edge;
import grammar.MinPQ;
import grammar.Graph;
import grammar.DirectedGraph;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ComputeContractedGraph {

    private static class NodeComparator<NodeT> implements Comparator<NodeT> {
        private final Map<NodeT,Integer> cache = new HashMap<>();
        private final Graph<NodeT> graph;

        public NodeComparator (Graph<NodeT> reference) {graph = reference;}

        @Override public int compare (NodeT u, NodeT v) {
            Integer diffU = cache.get(u);
            if (diffU == null) {
                diffU = difference(u);
                cache.put(u, diffU);
            }

            Integer diffV = cache.get(v);
            if (diffV == null) {
                diffV = difference(v);
                cache.put(v, diffV);
            }

            return diffU - diffV;
        }

        private int difference (NodeT u) {
            int diff = 0;
            for (Edge<NodeT> toedge : graph.getEdgesTo(u))
                for (Edge<NodeT> fromedge : graph.getEdgesFrom(u))
                    if (!toedge.from.equals(fromedge.to))
                        if (fromedge.weight+toedge.weight<=graph.getPathCost(toedge.from,fromedge.to))
                            ++diff;
            return diff;
        }
    }

    public static<NodeT> Graph<NodeT> compute (Graph<NodeT> graph) {
        MinPQ<NodeT> importanceheap = new MinPQ<>(new NodeComparator<>(graph));
        for (NodeT u : graph.getNodes()) importanceheap.insert(u);

        Graph<NodeT> compressed = new DirectedGraph<>();
        while (!importanceheap.isEmpty()) {
            NodeT top = importanceheap.delMin();
            for (Edge<NodeT> toedge : graph.getEdgesTo(top))
                for (Edge<NodeT> fromedge : graph.getEdgesFrom(top))
                    if (!toedge.from.equals(fromedge.to))
                        if (fromedge.weight+toedge.weight<=graph.getPathCost(toedge.from,fromedge.to))
                            compressed.setEdge(new Edge<>(toedge.from,fromedge.to,fromedge.weight+toedge.weight));
        }

        for (NodeT u : graph.getNodes()) {
            if (!compressed.containsNode(u)) {
                restoreNode (u,graph,compressed);
                if (!compressed.containsNode(u)) {
                    compressed.addNode(u);
                }
            }
        }

        return compressed;
    }

    private static<NodeT> NodeT restoreNode (NodeT u, Graph<NodeT> graph, Graph<NodeT> compressed) {
        for (Edge<NodeT> edge : graph.getEdgesFrom(u)) {
            if (!compressed.containsNode(edge.to)) {
                restoreNode (edge.to,graph,compressed);
            }
            compressed.setEdge (edge);
        }
        return u;
    }



    public static void main (String[] args) {
        Graph<Long> graph = new DirectedGraph<>(args[0],java.text.NumberFormat.getNumberInstance(),true);
        Graph<Long> compressed = compute(graph);
        for (Edge<Long> edge : compressed)
            System.out.println(edge.from+"\t"+edge.to+"\t"+edge.weight);
    }
}
