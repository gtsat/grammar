/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2015 George Tsatsanifos <gtsatsanifos@gmail.com>
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

package diversion;

import grammar.Edge;
import grammar.Graph;
import grammar.MinPQ;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

public final class GraphDivExhaustiveComposite<NodeT> implements Iterable<NodeT> {

    private final Graph<NodeT> graph;
    private final ContentSimilarity<NodeT> sim;

    private final boolean useMinSum;

    private final Collection<NodeT> R;

    private final ArrayList<NodeT> S;
    private final NodeT q;

    private final float lambda;
    private final float alpha;
    private final float beta;


    public GraphDivExhaustiveComposite (NodeT q, ArrayList<NodeT> S, Collection<NodeT> R, 
                                         Graph<NodeT> graph,
                                         ContentSimilarity<NodeT> sim,
                                         boolean useMinSum,
                                         float l,float a,float b) {

        this.useMinSum = useMinSum;

        this.graph = graph;
        this.sim = sim;

        this.R = R;
        this.S = S;
        this.q = q;

        lambda = l;
        alpha = a;
        beta = b;
    }

    @Override public Iterator<NodeT> iterator () {return new GraphDivIterator<>(q,S,R,graph,sim,useMinSum,lambda,alpha,beta);}

    final public class GraphDivIterator<NodeT> implements Iterator<NodeT> {

        private final boolean minsum;

        private final Graph<NodeT> graph;
        private final ContentSimilarity<NodeT> sim;
        private final Collection<NodeT> R;

        private final ArrayList<NodeT> S;
        private final NodeT q;

        private final HashMap<NodeT,Float> scores;

        private final float lambda;
        private final float alpha;
        private final float beta;

        private final Set<NodeT> nodes;
        private final HashSet<NodeT> processed;
        private final Map<NodeT,Map<NodeT,Float>> distances = new HashMap<>();

        @Override public GraphDivIterator<NodeT> clone () {return new GraphDivIterator<>(this);}
        public GraphDivIterator (GraphDivIterator<NodeT> other) {
            minsum = other.minsum;
            graph = other.graph;
            sim = other.sim;
            R = other.R;
            q = other.q;
            S = new ArrayList<>(other.S);

            lambda = other.lambda;
            alpha = other.alpha;
            beta = other.beta;

            nodes = new HashSet<>(other.nodes);
            scores = new HashMap<>(other.scores);
            processed = new HashSet<>(other.processed);

            for (NodeT s : other.S)
                distances.put(s,new HashMap<>(other.distances.get(s)));
            distances.put(q,new HashMap<>(other.distances.get(q)));
        }

        public GraphDivIterator (NodeT q, Collection<NodeT> S, 
                                 Collection<NodeT> R, 
                                 Graph<NodeT>graph,
                                 ContentSimilarity<NodeT>sim,
                                 boolean useMinSum,
                                 float l,float a,float b) {

            this.graph = graph;
            this.sim = sim;

            this.R = R;

            this.q = q;
            this.S = new ArrayList<>(S);

            minsum = useMinSum;

            lambda = l;
            alpha = a;
            beta = b;

            nodes = new HashSet<>(graph.getNodes());
            for (NodeT s : S) nodes.remove(s);
            nodes.remove(q);

            for (NodeT s : S) distances.put(s,new HashMap<NodeT,Float>());
            distances.put(q,new HashMap<NodeT,Float>());

            processed = new HashSet<>();
            scores = new HashMap<>();

            compute();
        }
        @Override public void remove () {throw new UnsupportedOperationException("\n!! ERROR - Cannot remove nodes from the graph !!");}
        @Override public boolean hasNext () {return !nodes.isEmpty();}
        @Override public NodeT next () {
            NodeT optimal = null;
            for (NodeT u : nodes)
                if (optimal==null || scores.get(u)<scores.get(optimal))
                    optimal = u;

            if (optimal!=null) {
                processed.add (optimal);
                nodes.remove (optimal);
            }
            return optimal;
        }
        private void compute () {
            MinPQ<Edge<NodeT>> heap = new MinPQ<>();
            HashSet<NodeT> marked = new HashSet<>();

            for (NodeT s : S) {
                marked.add (q);
                for (NodeT s0 : S) 
                    marked.add (s0);

                distances.get(s).put(s,0.0f);
                for (Edge<NodeT> edge : graph.getEdgesFrom(s)) {
                    if (!marked.contains(edge.to)) {
                        marked.add  (edge.to);
                        heap.insert (edge);
                    }
                }
                while (!heap.isEmpty()) {
                    Edge<NodeT> top = heap.delMin();

                    distances.get(s).put(top.to,top.weight);

                    if (!scores.containsKey(top.to)) scores.put (top.to, (1-lambda)*(beta*top.weight+(1-beta)*sim.similarity(s,top.to)));
                    else if (minsum) scores.put (top.to, scores.get(top.to)-(1-lambda)*(beta*top.weight+(1-beta)*sim.similarity(s,top.to)));
                    else{
                        float tempscore = (1-lambda)*(beta*top.weight+(1-beta)*sim.similarity(s,top.to));
                        if (tempscore<scores.get(top.to)) scores.put (top.to, tempscore);
                    }

                    for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                        if (!marked.contains (edge.to)) {
                            marked.add  (edge.to);
                            heap.insert (new Edge<>(top.from,edge.to,edge.weight+top.weight));
                        }
                    }
                }
                heap.clear ();
                marked.clear ();
            }

            marked.add (q);
            for (NodeT s : S)
                marked.add (s);

            distances.get(q).put(q,0.0f);
            for (Edge<NodeT> edge : graph.getEdgesFrom(q)) {
                if (!marked.contains(edge.to)) {
                    marked.add (edge.to);
                    heap.insert (edge);
                }
            }
            while (!heap.isEmpty()) {
                Edge<NodeT> top = heap.delMin();

                distances.get(q).put(top.to,top.weight);

                assert (scores.containsKey(top.to));
                scores.put (top.to, scores.get(top.to)+lambda*(alpha*top.weight+(1-alpha)*sim.similarity(q,top.to)));

                for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                    if (!marked.contains (edge.to)) {
                        marked.add  (edge.to);
                        heap.insert (new Edge<>(top.from,edge.to,edge.weight+top.weight));
                    }
                }
            }

            for (NodeT u : nodes)
                if (!marked.contains(u))
                    scores.put(u, Float.MAX_VALUE);

            for (NodeT s : S) scores.put(s, Float.MAX_VALUE);
            scores.put(q, Float.MAX_VALUE);

            heap.clear ();
            marked.clear ();
        }

        public GraphDivIterator<NodeT> replaceSetReturn (NodeT s, NodeT r) {
            GraphDivIterator<NodeT> expanded = clone();
            expanded.replaceSet (s, r);
            return expanded;
        }

        public void replaceSet (NodeT s, NodeT r) {
            S.remove  (s);
            for (Map.Entry<NodeT,Float> entry : scores.entrySet()) {
                if (minsum){
                    if (distances.get(s).containsKey(entry.getKey()))
                        entry.setValue (entry.getValue()+(1-lambda)*(beta*distances.get(s).get(entry.getKey())+(1-beta)*sim.similarity(s,entry.getKey())));
                }else{
                    NodeT mins = null;
                    Float mindist = Float.MAX_VALUE;
                    for (NodeT s0 : S) {
                        if (distances.get(s0).containsKey(entry.getKey()) && distances.get(s0).get(entry.getKey())<mindist) {
                            mindist = distances.get(s0).get(entry.getKey());
                            mins = s0;
                        }
                    }
                    if (mins!=null && distances.get(q).containsKey(entry.getKey()))
                    entry.setValue (lambda*(alpha*distances.get(q).get(entry.getKey())+(1-alpha)*sim.similarity(q,entry.getKey()))
                                   -(1-lambda)*(beta*distances.get(mins).get(entry.getKey())+(1-beta)*sim.similarity(mins,entry.getKey())));
                }
            }

            distances.remove(s).clear();
            expandSet (r);
        }

        public void expandSet (NodeT s) {
            S.add(s);

            Map<NodeT,Float> distmap = new HashMap<>();
            distances.put (s,distmap);

            HashSet<NodeT> marked = new HashSet<>();
            MinPQ<Edge<NodeT>> heap = new MinPQ<>();
            marked.add (q);
            for (NodeT s0 : S) 
                marked.add (s0);

            for (Edge<NodeT> edge : graph.getEdgesFrom(s)) {
                if (!marked.contains(edge.to)) {
                    marked.add  (edge.to);
                    heap.insert (edge);
                }
            }
            while (!heap.isEmpty()) {
                Edge<NodeT> top = heap.delMin();

                distmap.put (top.to, top.weight);
                assert (scores.containsKey(top.to));
                scores.put (top.to, scores.get(top.to)-(1-lambda)*(beta*top.weight+(1-beta)*sim.similarity(s,top.to)));

                for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                    if (!marked.contains (edge.to)) {
                        marked.add  (edge.to);
                        heap.insert (new Edge<>(top.from,edge.to,edge.weight+top.weight));
                    }
                }
            }
        }
    }
}
