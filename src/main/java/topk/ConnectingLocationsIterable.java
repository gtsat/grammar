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

package topk;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import oscp.Group;
import grammar.Edge;
import grammar.Graph;
import grammar.MinPQ;
import shortestpath.Point2D;
import grammar.UndirectedGraph;
import shortestpath.ScatterMap;

/*
 * Iterates through the graph nodes starting from the OSCP, and 
 * proceeding with the next best meeting location, and so on. 
 */

final public class ConnectingLocationsIterable<NodeT> implements Iterable<NodeT> {

    final private Group<NodeT> grouping;
    final private ScatterMap<NodeT> map;
    final private Graph<NodeT> graph;
    final private boolean useMinSum;

    public ConnectingLocationsIterable (Group<NodeT> grouping, 
                                        Graph<NodeT> graph, 
                                        ScatterMap<NodeT> map, 
                                        boolean useMinSum) {
        this.useMinSum = useMinSum;
        this.grouping = grouping;
        this.graph = graph;
        this.map = map;
    }

    public int getMaxMemRequirements () {return 0;}

    @Override public Iterator<NodeT> iterator () {return new ConnectingLocationsIterator<>(grouping,graph,map,useMinSum);}

    final private class ConnectingLocationsIterator<NodeT> implements Iterator<NodeT> {
        final private Map<Group<NodeT>,MinPQ<Edge<NodeT>>> distanceHeaps = new HashMap<>();
        final private Map<NodeT,Map<Group<NodeT>,Float>> dists = new HashMap<>();
        final private MinPQ<NodeT> completeScoresHeap;

        final private Map<NodeT,Set<Group<NodeT>>> partialScores = new HashMap<>();
        final private ArrayList<NodeT> partialScoresList = new ArrayList<>();

        //final private Set<NodeT> processed = new HashSet<>();

        final private VertexComparator<NodeT> cmp;
        final private Group<NodeT> grouping;
        final private ScatterMap<NodeT> map;
        final private Graph<NodeT> graph;

        @SuppressWarnings (value={"unchecked"})
        private float calculatePartialScoresLowerBound () {
            float mindist = Float.MAX_VALUE;
            if (partialScoresList.isEmpty()) return mindist;
            Map<Group<NodeT>,Float> tempmap = new HashMap<>();
            for (Entry<Group<NodeT>,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                Float tempdist = dists.get(partialScoresList.get(0)).get(entry.getKey());
                if (tempdist==null) tempmap.put(entry.getKey(),entry.getValue().min().weight);
                //if (tempdist==null) tempmap.put(entry.getKey(),0.0f);
                else tempmap.put(entry.getKey(),tempdist);
/*
                float lambda = entry.getKey().getLambdas().get(entry.getKey());
                if (!entry.getValue().isEmpty() && lambda * entry.getValue().min().weight < mindist)
                    mindist = lambda * entry.getValue().min().weight;
*/
            }
            float threshold = cmp.computeScore(tempmap) ;//- mindist;

            float minscore = Float.MAX_VALUE;
            Object dummy = new Object();
            cmp.overrideStoreScore ((NodeT)dummy, threshold);
            for (int i=0; !partialScoresList.isEmpty() && i < partialScoresList.size() 
                    && cmp.compare(partialScoresList.get(i),(NodeT)dummy)<=0
                    ; ++i) {
                NodeT unmatched = partialScoresList.get(i);
                Map<Group<NodeT>,Float> encounters = dists.get(unmatched);
                tempmap = new HashMap<>();
                for (Entry<Group<NodeT>,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                    Float tempdist = encounters.get(entry.getKey());
                    if (tempdist!=null) tempmap.put (entry.getKey(),tempdist);
                    else tempmap.put (entry.getKey(),entry.getValue().min().weight);
                }
                float tempscore = cmp.computeScore(tempmap);
                if (tempscore<minscore) minscore = tempscore;
            }
            cmp.overrideStoreScore ((NodeT)dummy, null);
            return minscore;
        }

        private float calculatePartialScoresLowerBoundOld () {
            float minscore = Float.MAX_VALUE;
            for (Entry<NodeT,Set<Group<NodeT>>> unmatched : partialScores.entrySet()) {
                Map<Group<NodeT>,Float> tempmap = new HashMap<>();
                Map<Group<NodeT>,Float> encounters = dists.get(unmatched.getKey());
                for (Entry<Group<NodeT>,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet())
                    if (encounters.containsKey(entry.getKey())) tempmap.put (entry.getKey(),encounters.get(entry.getKey()));
                    else tempmap.put (entry.getKey(),entry.getValue().min().weight);
                float tempscore = cmp.computeScore(tempmap);
                if (tempscore<minscore) minscore = tempscore;
            }
            return minscore;
        }

        private float calculateUnprocessedLowerBound () {
            Map<Group<NodeT>,Float> tempmap = new HashMap<>();
            for (Entry<Group<NodeT>,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet())
                if (!entry.getValue().isEmpty())
                    tempmap.put (entry.getKey(),entry.getValue().min().weight);
            return cmp.computeScore (tempmap);
        }

        private void addToSortedList (NodeT key, ArrayList<NodeT> list) {
            int lo=0,hi=list.size(),m=hi>>1;
            for (;lo<hi;m=(hi+lo)>>1) {
                int balance = cmp.compare(key,list.get(m));
                if (balance==0) break;
                else if (balance<0) hi=m;
                else lo=m+1;
            }
            list.add(m,key);
        }

        private void deleteFromSortedList (NodeT key, ArrayList<NodeT> list) {
            int lo=0,hi=list.size(),m=hi>>1;
            for (;lo<hi;m=(hi+lo)>>1) {
                int balance = cmp.compare(key,list.get(m));
                if (balance==0) {
                    list.remove(m);
                    return;
                }else if (balance<0) hi=m;
                else lo=m+1;
            }
        }

        public class DistanceFromMedianComparator implements Comparator<Edge<NodeT>> {
            final private Point2D median;

            public DistanceFromMedianComparator (Collection<NodeT> collection) {
                float x = 0.0f, y = 0.0f;
                for (NodeT vertex : collection) {
                    Point2D location = map.getSpatialPosition(vertex);
                    x += location.x();
                    y += location.y();
                }
                median = new Point2D (x/collection.size(),y/collection.size());
            }

            @Override public int compare (Edge<NodeT> x, Edge<NodeT> y) {
                Point2D locx = map.getSpatialPosition(x.to);
                Point2D locy = map.getSpatialPosition(y.to);
                float balance = locx.distanceSquaredTo(median) - locy.distanceSquaredTo(median);
                if (balance<0) return -1;
                else if (balance>0) return 1;
                else return 0;
            }
        }

        public ConnectingLocationsIterator (Group<NodeT>grouping, 
                                            Graph<NodeT>graph, 
                                            ScatterMap<NodeT> map, 
                                            boolean useMinSum) {
            this.grouping = grouping;
            this.graph = graph;
            this.map = map;

            cmp = useMinSum?
                  new VertexMinSumComparator<>(grouping)
                  :new VertexMinMaxComparator<>(grouping);
            completeScoresHeap = new MinPQ<>(cmp);

            Collection<NodeT> collection = new ArrayDeque<>();

            for (Group<NodeT> group : grouping.getSubgroups())
		if (group.getTarget()!=null)
                	collection.add(group.getTarget());

            if (grouping.getTarget()!=null)
                collection.add (grouping.getTarget());

            DistanceFromMedianComparator mediancmp = map==null?null:new DistanceFromMedianComparator (collection);

            if (grouping.getTarget()!=null) {
                MinPQ<Edge<NodeT>> newHeap = map==null?new MinPQ<Edge<NodeT>>():new MinPQ<Edge<NodeT>>(mediancmp);
                newHeap.insert (new Edge<>(grouping.getTarget(),grouping.getTarget(),0.0f));
                distanceHeaps.put (grouping,newHeap);

                Map<Group<NodeT>,Float> tempmap = dists.get (grouping.getTarget());
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    dists.put (grouping.getTarget(),tempmap);
                }
                tempmap.put (grouping,0.0f);
                cmp.computeStoreScore (grouping.getTarget(),tempmap);

                partialScoresList.add (grouping.getTarget());
            }
            for (Group<NodeT> group : grouping.getSubgroups()) {
                if (group.getTarget()==null) continue;
                MinPQ<Edge<NodeT>> newHeap = map==null?new MinPQ<Edge<NodeT>>():new MinPQ<Edge<NodeT>>(mediancmp);
                newHeap.insert (new Edge<>(group.getTarget(),group.getTarget(),0.0f));
                distanceHeaps.put (group,newHeap);

                Map<Group<NodeT>,Float> tempmap = dists.get (group.getTarget());
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    dists.put (group.getTarget(),tempmap);
                }
                tempmap.put (group,0.0f);
                cmp.computeStoreScore (group.getTarget(),tempmap);

                addToSortedList(group.getTarget(),partialScoresList);
            }
        }

        @Override public boolean hasNext () {
            if (distanceHeaps.isEmpty()) return false;
            if (!partialScoresList.isEmpty() || !completeScoresHeap.isEmpty()) return true;
            for (MinPQ<Edge<NodeT>> distanceHeap : distanceHeaps.values())
                if (!distanceHeap.isEmpty())
                    return true;
            return false;
        }
        @Override public void remove () {throw new UnsupportedOperationException("\n!! ERROR - Cannot remove nodes from the graph !!");}
        @Override public NodeT next () {
            float minCostExpansion;
            int cardinality = grouping.getTarget()==null?grouping.getSubgroups().size():grouping.getSubgroups().size()+1;
            while (completeScoresHeap.isEmpty()
                    || cmp.scores.get(completeScoresHeap.min())>=calculatePartialScoresLowerBound()) {
                minCostExpansion = Float.MAX_VALUE;
                Group<NodeT> minCostExpansionGroup = null;
                MinPQ<Edge<NodeT>> minCostExpansionHeap = null;
                for (Entry<Group<NodeT>,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        float lambda = entry.getKey().getLambdas().get(entry.getKey());
                        if (minCostExpansionHeap==null || lambda*entry.getValue().min().weight<minCostExpansion) {
                            minCostExpansion = lambda*entry.getValue().min().weight;
                            minCostExpansionHeap = entry.getValue();
                            minCostExpansionGroup = entry.getKey();
                        }
                    }
                }

                if (minCostExpansionHeap!=null) {
                    if (minCostExpansionHeap.isEmpty()) continue;

                    Edge<NodeT> top = minCostExpansionHeap.delMin();

//                    if (processed.contains(top.to)) continue;
/*
                    Set<Group<NodeT>> accessed = partialScores.get(top.to);

                    if (accessed==null) {
                        accessed = new HashSet<>();
                        partialScores.put(top.to,accessed);
                    }
                    accessed.add(minCostExpansionGroup);
*/
                    if (dists.get(top.to).size()==cardinality) {
                        cmp.computeStoreScore(top.to,dists.get(top.to));
                        completeScoresHeap.insert(top.to);
                        //partialScores.remove(top.to);
//                        processed.add(top.to);

                        deleteFromSortedList(top.to,partialScoresList);
                    }else{
                        deleteFromSortedList(top.to,partialScoresList);
                        addToSortedList(top.to,partialScoresList);
                    }

                    for (Edge<NodeT> edge : minCostExpansionGroup.equals(grouping)?graph.getEdgesTo(top.to):graph.getEdgesFrom(top.to)) {
                        NodeT newvertex = minCostExpansionGroup.equals(grouping)?edge.from:edge.to;
                        Map<Group<NodeT>,Float> tempmap = dists.get(newvertex);
                        float newdist = edge.weight + top.weight;
                        if (tempmap==null) {
                            tempmap = new HashMap<>();
                            dists.put(newvertex,tempmap);
                            tempmap.put(minCostExpansionGroup,newdist);
                            distanceHeaps.get(minCostExpansionGroup).insert (new Edge<>(top.to,newvertex,newdist));
                        }else if (!tempmap.containsKey(minCostExpansionGroup) || newdist<tempmap.get(minCostExpansionGroup)) {
                            tempmap.put(minCostExpansionGroup,newdist);
                            distanceHeaps.get(minCostExpansionGroup).insert (new Edge<>(top.to,newvertex,newdist));
                        }
                    }
                }else break;
            }
            //System.out.println ("** Score "+cmp.scores.get(completeScoresHeap.min())+ "\tfor node "+completeScoresHeap.min());
            ++COUNTER;
            if (completeScoresHeap.isEmpty()) {
                for (Group g : grouping.getTravelers()) {
                    System.out.println ((NodeT)g.getTarget() + " of elements: " + ((Group)g.getTravelers().getFirst()).getTarget());
                }
                throw new RuntimeException("!! ERRROR - Unable to retrieve next connecting location "+COUNTER+" of "+ this.grouping.getTravelers().size()+" travellers !!");
            }
            return completeScoresHeap.isEmpty()?null:completeScoresHeap.delMin();
        }
        int COUNTER = 0;
    }

    abstract private class VertexComparator<NodeT> implements Comparator<NodeT> {
        final protected Map<NodeT,Float> scores = new HashMap<>();
        final protected Group<NodeT> target;

        public VertexComparator (Group<NodeT> target) {this.target=target;}
        public void overrideStoreScore (NodeT vertex,Float score) {
            if (score==null) scores.remove(vertex);
            else scores.put(vertex,score);
        }
        abstract public float computeScore (Map<Group<NodeT>,Float> vertexdists);
        public float computeStoreScore (NodeT newvertex, Map<Group<NodeT>,Float> vertexdists) {
            float score = computeScore(vertexdists);
            scores.put (newvertex,score);
            return score;
        }

        @Override public int compare (NodeT x,NodeT y) {
            if (!scores.containsKey(x) && !scores.containsKey(y)) return 0;
            else if (!scores.containsKey(y)) return -1;
            else if (!scores.containsKey(x)) return 1;
            else{
                float balance = scores.get(x) - scores.get(y);
                if (balance<0) return -1;
                else if (balance>0) return 1;
                else return 0; 
            }
        }
    }

    final private class VertexMinMaxComparator<NodeT> extends VertexComparator<NodeT> {
        public VertexMinMaxComparator (Group<NodeT> target) {super(target);}
        @Override public float computeScore (Map<Group<NodeT>,Float> vertexdists) {
            float score = 0.0f;
            for (Entry<Group<NodeT>,Float> entry : vertexdists.entrySet()) {
                if (entry.getKey()==null)
                    throw new RuntimeException("\n!! ERROR - Null instead of Group instance !!");
                if (entry.getValue()==null)
                    throw new RuntimeException("\n!! ERROR - Group "+entry.getKey().hashCode()+" is mapped to <>null</> !!");
                if (!entry.getKey().getLambdas().containsKey(entry.getKey()))
                    throw new RuntimeException("\n!! ERROR - Group "+entry.getKey().hashCode()+" is not associated with itself (MinMax) !!");
                if (entry.getKey().getLambdas().get(entry.getKey()) == null)
                    throw new RuntimeException("\n!! ERROR - Group "+entry.getKey().hashCode()+" has nulled association with itself (MinSum) !!");
                if (!target.equals(entry.getKey())) {
                    float temp = entry.getKey().getLambdas().get(entry.getKey()) * entry.getValue();
                    if (target!=null && vertexdists.containsKey(target)) {
                        float product = 1.0f;
                        for (float lambda : entry.getKey().getLambdas().values())
                            product *= lambda;
                        temp += product * vertexdists.get(target);
                    }
                    if (temp>score) score = temp;
                }
            }
            return score;
        }
    }

    final private class VertexMinSumComparator<NodeT> extends VertexComparator<NodeT> {
        public VertexMinSumComparator (Group<NodeT> target) {super(target);}
        @Override public float computeScore (Map<Group<NodeT>,Float> vertexdists) {
            float score = 0.0f;
            for (Entry<Group<NodeT>,Float> entry : vertexdists.entrySet()) {
                if (entry.getKey()==null)
                    throw new RuntimeException("\n!! ERROR - Null instead of Group instance !!");
                if (entry.getValue()==null) 
                    throw new RuntimeException("\n!! ERROR - Group "+entry.getKey().hashCode()+" is mapped to <>null</> !!");
                if (!entry.getKey().getLambdas().containsKey(entry.getKey()))
                    throw new RuntimeException("\n!! ERROR - Group "+entry.getKey().hashCode()+" is not associated with itself (MinSum) !!");
                if (entry.getKey().getLambdas().get(entry.getKey()) == null)
                    throw new RuntimeException("\n!! ERROR - Group "+entry.getKey().hashCode()+" has nulled association with itself (MinSum) !!");
                score += entry.getKey().getLambdas().get(entry.getKey()) * entry.getValue();
            }
            score /= vertexdists.size();
            return score;
        }
    }


    public static void main (String[] args) {
        if (args.length<3) 
            throw new IllegalArgumentException("\n!! ERROR - Should provide a filename to a graph, a map, and the desired size of the result !!");

        String graphfilename = args[0];
        String mapfilename = args[1];

        boolean useMinSum = true;
        int k = Integer.parseInt(args[2]);
        if (k<0) {k=-k;useMinSum=true;}

        long target = 4000L;
        Long[] sources = {1000L,2000L,3000L};
        float[][] lambdas = {{.8f,.1f,.1f},{.1f,.8f,.1f},{.1f,.1f,.8f}};
        //float[][] lambdas = {{1,1,1},{1,1,1},{1,1,1}};

        UndirectedGraph<Long> graph = new UndirectedGraph<>(graphfilename,NumberFormat.getNumberInstance(),true);
        ScatterMap<Long> map = new ScatterMap<>(mapfilename,NumberFormat.getNumberInstance());
        Group<Long> grouping = new Group<>(null,sources,lambdas,graph,useMinSum);

        int i=0;
        for (Long mu : new ConnectingLocationsIterable<>(grouping,graph,null,useMinSum)) {
            System.out.println (mu);
            if (++i>=k)
                break;
        }
    }
}
