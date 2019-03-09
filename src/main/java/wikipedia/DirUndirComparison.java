package wikipedia;

import grammar.Edge;
import grammar.Graph;
import grammar.DirectedGraph;

public class DirUndirComparison {
    public static void main (String[] args) {
        int dcounter=0;
        String filename = args[0];
        Graph<String> dgraph = new DirectedGraph<>(filename,null,true);
        for (Edge<String> edge : dgraph) {
            if (dgraph.containsEdge(edge.to,edge.from)) {
                ++dcounter;
            }
        }
        System.out.println ("** From "+dgraph.numberEdges()+" edges overall, "+dcounter+
                            " are symmetric ("+(double)dcounter/dgraph.numberEdges()+")");
    }
}
