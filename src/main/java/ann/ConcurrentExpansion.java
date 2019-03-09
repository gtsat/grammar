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

package ann;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.HashMap;
import grammar.UndirectedGraph;
import grammar.MinPQ;
import grammar.Graph;
import grammar.Edge;
import oscp.Group;

public class ConcurrentExpansion<NodeT> {

    private static int maxSetSize = 0;
    private static int maxHeapSize = 0;

    public static void resetStats () {maxSetSize=0;maxHeapSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxHeapSize;}


    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,boolean useMinSum) {
        LocationComparatorCE<NodeT> cmp = (useMinSum?new MinSumLocationComparatorCE<NodeT>(grouping):new MinMaxLocationComparatorCE<NodeT>(grouping));
        MinPQ<NodeT> heap = new MinPQ<>(); // to cmp or not to cmp

        HashMap<Group<NodeT>,Float> tempmap = new HashMap<>();
        tempmap.put (grouping,0.0f);
        cmp.dists.put (grouping.getTarget(),tempmap);
        heap.insert (grouping.getTarget());

        for (Group<NodeT> group : grouping.getSubgroups()) {
            tempmap = cmp.dists.get(group.getTarget());
            if (tempmap==null) {
                tempmap = new HashMap<Group<NodeT>,Float>();
                cmp.dists.put (group.getTarget(),tempmap);
            }
            tempmap.put (group,0.0f);
            heap.insert (group.getTarget());
        }

        NodeT mu = null;
        while (!heap.isEmpty()) {
            NodeT top = heap.delMin();
            if (cmp.compare(top,mu)<0) mu = top;
            for (Edge<NodeT> edge : graph.getEdgesFrom(top))
                if (cmp.traverse(edge))
                    heap.insert(edge.to);
        }
        return mu;
    }

    static private abstract class LocationComparatorCE<NodeT> implements Comparator<NodeT> {
        final protected HashMap<NodeT,HashMap<Group<NodeT>,Float>> dists = new HashMap<>();
        final protected HashMap<NodeT,Float> scores = new HashMap<>();
        final protected Group<NodeT> target; 

        public LocationComparatorCE (Group<NodeT> dest) {target = dest;}
        abstract public boolean traverse (Edge<NodeT> e);
        public int compare (NodeT x,NodeT y) {
            if (x==null && y==null) return 0;
            else if (y==null) return -1;
            else if (x==null) return 1;
            else{
                float Cx = scores.containsKey(x)?scores.get(x):Float.MAX_VALUE;
                float Cy = scores.containsKey(y)?scores.get(y):Float.MAX_VALUE;
                if (Cx<Cy) return -1;
                else if (Cx>Cy) return 1;
                else return 0;
            }
        }
    }

    final static private class MinMaxLocationComparatorCE<NodeT> extends LocationComparatorCE<NodeT> {
        public MinMaxLocationComparatorCE (Group<NodeT> target) {super(target);}
        @Override public boolean traverse (Edge<NodeT> e) {
            float newScore = 0.0f;
            boolean leadsToNewlySeenVertex = false;
            HashMap<Group<NodeT>,Float> sources = dists.get(e.to);
            if (sources==null) {
                sources = new HashMap<Group<NodeT>,Float>();
                dists.put (e.to,sources);
            }

            for (Entry<Group<NodeT>,Float> entry : dists.get(e.from).entrySet()) {
                if (!sources.containsKey(entry.getKey())) {
                    sources.put (entry.getKey(),entry.getValue()+e.weight);
                    leadsToNewlySeenVertex = true;
                }
                if (!entry.getKey().equals(target)) {
                    float tempScore = entry.getKey().getLambdas().get(entry.getKey()) * (entry.getValue()+e.weight);
                    if (tempScore>newScore) newScore = tempScore;
                }
            }

            if (sources.containsKey(target))
                newScore += target.getLambdas().get(target) * sources.get(target);

            if (leadsToNewlySeenVertex)
                scores.put (e.to,newScore);
            return leadsToNewlySeenVertex;
        }
    }

    final static private class MinSumLocationComparatorCE<NodeT> extends LocationComparatorCE<NodeT> {
        public MinSumLocationComparatorCE (Group<NodeT> target) {super(target);}
        @Override public boolean traverse (Edge<NodeT> e) {
            float newScore = 0.0f;
            boolean leadsToNewlySeenVertex = false;
            HashMap<Group<NodeT>,Float> sources = dists.get(e.to);
            if (sources==null) {
                sources = new HashMap<Group<NodeT>,Float>();
                dists.put (e.to,sources);
            }
            for (Entry<Group<NodeT>,Float> entry : dists.get(e.from).entrySet()){
                if (!sources.containsKey(entry.getKey())) {
                    sources.put (entry.getKey(),entry.getValue()+e.weight);
                    leadsToNewlySeenVertex = true;
                }
                newScore += entry.getKey().getLambdas().get(entry.getKey()) * (entry.getValue()+e.weight);
            }

            if (leadsToNewlySeenVertex) 
                scores.put (e.to,newScore/sources.size());
            return leadsToNewlySeenVertex;
        }
    }


    public static void main (String[] args) {
        long target = 4000L;
        Long[] sources = {1000L,2000L,3000L};
        float[][] lambdas = {{.2f,.4f,.4f},{.4f,.2f,.4f},{.4f,.4f,.2f}};

        UndirectedGraph<Long> graph = new UndirectedGraph<Long>("../data/lifeifei/OL.graph",java.text.NumberFormat.getNumberInstance(),true).kruskal();
        Group<Long> grouping = new Group<>(target,sources,lambdas,graph,true);

        long mu = compute(grouping,graph,true);

        oscp.LocationComparatorMinSum<Long> cmp = new oscp.LocationComparatorMinSum<>(grouping,graph,null,true);
        System.out.println (mu+" with score "+cmp.computeScore(mu));
    }
}


