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

import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import grammar.Edge;
import grammar.Graph;
import grammar.MinPQ;
import grammar.UndirectedGraph;


final class GraphDivIterable<NodeT> implements Iterable<NodeT> {

    private final Graph<NodeT> graph;

    private final boolean useMinSum;

    private final Collection<NodeT> R;

    private final Collection<NodeT> S;
    private final NodeT q;

    public GraphDivIterable (NodeT q, Collection<NodeT> S, Collection<NodeT> R, 
                             Graph<NodeT> graph,
                             boolean useMinSum) {

        this.useMinSum = useMinSum;

        this.graph = graph;

        this.R = R;
        this.S = S;
        this.q = q;
    }

    @Override public Iterator<NodeT> iterator () {return new GraphDivIterator<>(q,S,R,graph,useMinSum);}

    final public class GraphDivIterator<NodeT> implements Iterator<NodeT> {
        private final Map<NodeT,MinPQ<Edge<NodeT>>> distanceHeaps = new HashMap<>();
        private final Map<NodeT,Map<NodeT,Float>> dists = new HashMap<>();
        private final MinPQ<NodeT> completeScoresHeap;

        private final ArrayList<NodeT> partialScoresList = new ArrayList<>();

        private final ArrayDeque<NodeT> returnedResults = new ArrayDeque<>();

        private final VertexComparator<NodeT> cmp;
        private final Graph<NodeT> graph;

        private final Collection<NodeT> R;

        private final Collection<NodeT> S;
        private final NodeT q;

        private final boolean minsum;



        private long timeoutmillis = 5000;
        private boolean activateTimeOut = false;

        public void activateTimeOut () {activateTimeOut=true;}
        public void setTimeOutParam (long newvalue) {timeoutmillis = newvalue;}



        private static final int C = 5;



        private final boolean logMemory = true;
        private int reservedBytesProcessing = 80;
        public int getMaxMemRequirements () {return logMemory?reservedBytesProcessing:-1;}



        @SuppressWarnings ("unchecked")
        private float calculatePartialScoresLowerBound () {
            if (partialScoresList.isEmpty()) return Float.MAX_VALUE;
            float threshold = Float.MAX_VALUE;
            for (int i=0; i<(partialScoresList.size()<C?partialScoresList.size():C); ++i) {
                Map<NodeT,Float> tempmap = new HashMap<>();
                for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                    Float tempdist = dists.get(partialScoresList.get(i)).get(entry.getKey());
                    if (tempdist==null) tempmap.put(entry.getKey(),entry.getValue().min().weight);
                    //tempmap.put(entry.getKey(),entry.getValue().isEmpty()?0:entry.getValue().min().weight);
                    //if (tempdist==null) tempmap.put(entry.getKey(),0.0f);
                    else tempmap.put(entry.getKey(),tempdist);
/*
                    float lambda = entry.getKey().getLambdas().get(entry.getKey());
                    if (!entry.getValue().isEmpty() && lambda * entry.getValue().min().weight < mindist)
                        mindist = lambda * entry.getValue().min().weight;
*/
                    float tempscore = cmp.computeScore(tempmap);
                    if (tempscore<threshold) threshold = tempscore;
                }
            }

            float minscore = Float.MAX_VALUE;
            Object dummy = new Object();
            cmp.overrideStoreScore ((NodeT)dummy, threshold);
            for (int i=0; !partialScoresList.isEmpty() && i<partialScoresList.size() 
                    && cmp.compare(partialScoresList.get(i),(NodeT)dummy)<=0
                    ; ++i) {
                NodeT unmatched = partialScoresList.get(i);
                Map<NodeT,Float> encounters = dists.get(unmatched);
                Map<NodeT,Float> tempmap = new HashMap<>();
                for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                    Float tempdist = encounters.get(entry.getKey());
                    if (tempdist!=null) tempmap.put (entry.getKey(),tempdist);
                    else //tempmap.put (entry.getKey(),entry.getValue().isEmpty()?0:entry.getValue().min().weight);
                        tempmap.put (entry.getKey(),entry.getValue().min().weight);
                }
                float tempscore = cmp.computeScore(tempmap);
                if (tempscore<minscore) minscore = tempscore;
            }
            cmp.overrideStoreScore ((NodeT)dummy, null);
            return minscore;
        }

        private float calculateUnprocessedLowerBound () {
            Map<NodeT,Float> tempmap = new HashMap<>();
            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet())
                if (!entry.getValue().isEmpty())
                    tempmap.put (entry.getKey(),entry.getValue().min().weight);
            return cmp.computeScore (tempmap);
        }

        private void addToSortedList (NodeT key, ArrayList<NodeT> list) {
            int lo=0,hi=list.size(),m=hi>>1;
            for (;lo<hi;m=(hi+lo)>>1) {
                int balance = cmp.compare(key,list.get(m));
                if (balance==0)
                    break;
                else if (balance<0) hi=m;
                else lo=m+1;
            }
            list.add(m,key);
        }

        private boolean removeFromSortedList (NodeT key, ArrayList<NodeT> list) {
            int lo=0,hi=list.size(),m=hi>>1;
            for (;lo<hi;m=(hi+lo)>>1) {
                int balance = cmp.compare(key,list.get(m));
                if (balance==0) {
                    list.remove(m);
                    return true;
                }else if (balance<0) hi=m;
                else lo=m+1;
            }
            return false;
        }

        public GraphDivIterator (NodeT q, Collection<NodeT> S, 
                                 Collection<NodeT> R, 
                                 Graph<NodeT>graph,
                                 boolean useMinSum) {
            this.graph = graph;

            this.R = R;

            this.q = q;
            this.S = new ArrayList<>(S);

            this.minsum = useMinSum;

            cmp = useMinSum?new VertexSumComparator<>(q):new VertexMaxComparator<>(q);
            completeScoresHeap = new MinPQ<>(cmp);

            {
                MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
                newHeap.insert (new Edge<>(q,q,0.0f));
                distanceHeaps.put (q,newHeap);

                if (logMemory) reservedBytesProcessing += 30;

                Map<NodeT,Float> tempmap = dists.get(q);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    dists.put(q,tempmap);

                    if (logMemory) reservedBytesProcessing += 12;
                }
                tempmap.put (q,0.0f);
                cmp.computeStoreScore (q,tempmap);
                if (logMemory) reservedBytesProcessing += 20;

                partialScoresList.add (q);
                if (logMemory) reservedBytesProcessing += 4;
            }

            for (NodeT s : S) {
                MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
                newHeap.insert (new Edge<>(s,s,0.0f));
                distanceHeaps.put (s,newHeap);

                if (logMemory) reservedBytesProcessing += 30;

                Map<NodeT,Float> tempmap = dists.get(s);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    dists.put(s,tempmap);

                    if (logMemory) reservedBytesProcessing += 12;
                }
                tempmap.put (s,0.0f);
                cmp.computeStoreScore (s,tempmap);
                if (logMemory) reservedBytesProcessing += 20;

                addToSortedList(s,partialScoresList);
                if (logMemory) reservedBytesProcessing += 4;
            }
        }

        @Override public boolean hasNext () {
            if (!partialScoresList.isEmpty() || !completeScoresHeap.isEmpty()) return true;
            for (MinPQ<Edge<NodeT>> distanceHeap : distanceHeaps.values())
                if (!distanceHeap.isEmpty())
                    return true;
            return false;
        }
        @Override public void remove () {throw new UnsupportedOperationException("\n!! ERROR - Cannot remove nodes from the graph !!");}
        @Override public NodeT next () {
            long start = System.currentTimeMillis();
            int cardinality = S.size() + 1;
            float minCostExpansion;
            while (completeScoresHeap.isEmpty() 
                    || (!activateTimeOut || System.currentTimeMillis() - start < timeoutmillis) 
                    && cmp.scores.get(completeScoresHeap.min())>calculatePartialScoresLowerBound()) {
                    //|| cmp.scores.get(completeScoresHeap.min())>calculateUnprocessedLowerBound()) {

                minCostExpansion = Float.MAX_VALUE;
                MinPQ<Edge<NodeT>> minCostExpansionHeap = null;
                for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        if (minCostExpansionHeap==null || entry.getValue().min().weight<minCostExpansion) {
                            minCostExpansion = entry.getValue().min().weight;
                            minCostExpansionHeap = entry.getValue();
                        }
                    }
                }

                if (minCostExpansionHeap!=null) {
                    if (minCostExpansionHeap.isEmpty()) continue;

                    Edge<NodeT> top = minCostExpansionHeap.delMin();

                    if (logMemory) reservedBytesProcessing -= 20;

                    if (dists.get(top.to).size()==cardinality) {
                        if (!q.equals(top.to) && !S.contains(top.to)) //&& R.contains(top.to))
                            completeScoresHeap.insert (top.to);
                            if (logMemory) reservedBytesProcessing += 4;

                        boolean exists = removeFromSortedList (top.to,partialScoresList);
                        if (logMemory && !exists) reservedBytesProcessing += 4;
                    }

                    for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                        Map<NodeT,Float> tempmap = dists.get(edge.to);
                        float newdist = edge.weight + top.weight;
                        if (tempmap==null) {
                            tempmap = new HashMap<>();
                            dists.put(edge.to,tempmap);
                            tempmap.put(top.from,newdist);
                            minCostExpansionHeap.insert (new Edge<>(top.from,edge.to,newdist));

                            if (logMemory) reservedBytesProcessing += 48;

                            boolean exists = removeFromSortedList (top.to,partialScoresList);
                            if (logMemory && !exists) reservedBytesProcessing += 4;

                            cmp.computeStoreScore (edge.to,tempmap);
                            addToSortedList(top.to,partialScoresList);
                        }else if (!tempmap.containsKey(top.from) || newdist<tempmap.get(top.from)) {
                            tempmap.put (top.from,newdist);
                            minCostExpansionHeap.insert (new Edge<>(top.from,edge.to,newdist));

                            if (logMemory) reservedBytesProcessing += 32;

                            boolean exists = removeFromSortedList (top.to,partialScoresList);
                            if (logMemory && !exists) reservedBytesProcessing += 4;

                            cmp.computeStoreScore (edge.to,tempmap);
                            addToSortedList(top.to,partialScoresList);
                        }
                    }
                }else break;
            }

            /**
            System.out.println ("** Complete-scores: "+completeScoresHeap.size()+"\t Partial-scores: "+partialScoresList.size());
            for (MinPQ<Edge<NodeT>> heap : distanceHeaps.values())
                System.out.println ("** Distance-heap size: "+heap.size());
            **/
            //System.out.println ("** Score: "+cmp.scores.get(completeScoresHeap.min())+"\t for node "+completeScoresHeap.min());
            if (completeScoresHeap.isEmpty()) return null;
            else{
                returnedResults.add (completeScoresHeap.min());
                return completeScoresHeap.delMin();
            }
        }

        public void replaceSet (NodeT s,NodeT r) {
            distanceHeaps.remove(s);
            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(r,r,0.0f));
            distanceHeaps.put (r,newHeap);
            
            if (logMemory) reservedBytesProcessing += 30;

            Map<NodeT,Float> tempmap = dists.get(r);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                dists.put(r,tempmap);

                if (logMemory) reservedBytesProcessing += 16;
            }
            tempmap.put(r,0.0f);
            if (logMemory) reservedBytesProcessing += 12;

            removeFromSortedList (s,partialScoresList);

            for (Entry<NodeT,Map<NodeT,Float>> entry : dists.entrySet()) {
                entry.getValue().remove(s);

                if (logMemory) reservedBytesProcessing -= 12;

                boolean exists = removeFromSortedList (entry.getKey(),partialScoresList);
                cmp.computeStoreScore(entry.getKey(),entry.getValue());
                if (!entry.getValue().isEmpty()) {
                    addToSortedList(entry.getKey(),partialScoresList);
                    if (logMemory && !exists) reservedBytesProcessing += 4;
                }
            }

            addToSortedList (r,partialScoresList);

            S.remove(s);
            S.add(r);
        }

        public GraphDivIterator<NodeT> replaceSetReturn (NodeT s,NodeT r) {
            GraphDivIterator<NodeT> expanded = new GraphDivIterator<> (q,S,R,graph,minsum);

            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                if (!s.equals(entry.getKey())) {
                    MinPQ<Edge<NodeT>> dh = expanded.distanceHeaps.get(entry.getKey());

                    if (logMemory) expanded.reservedBytesProcessing -= dh.size()*20;

                    while (!dh.isEmpty()) dh.delMin();
                    for (Edge<NodeT> edge : entry.getValue()) dh.insert(edge);

                    if (logMemory) expanded.reservedBytesProcessing += dh.size()*20;
                }
            }

            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(r,r,0.0f));
            expanded.distanceHeaps.put(r,newHeap);
            expanded.distanceHeaps.remove(s);

            if (logMemory) expanded.reservedBytesProcessing += 30;

            if (logMemory) expanded.reservedBytesProcessing -= expanded.partialScoresList.size()*4;

            expanded.partialScoresList.clear();
            expanded.cmp.scores.clear();

            Map<NodeT,Float> tempmap = expanded.dists.get(r);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                if (logMemory) expanded.reservedBytesProcessing += 8;
            }
            tempmap.put(r,0.0f);
            expanded.dists.put(r,tempmap);
            expanded.cmp.computeStoreScore(r,tempmap);
            if (logMemory) expanded.reservedBytesProcessing += 20;

            expanded.partialScoresList.add(r);

            if (logMemory) expanded.reservedBytesProcessing += 4;

            for (Entry<NodeT,Map<NodeT,Float>> entry : dists.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());
                newmap.remove(s);
                expanded.dists.put (entry.getKey(),newmap);
                expanded.cmp.computeStoreScore(entry.getKey(),newmap);

                if (!newmap.isEmpty()) {
                    expanded.addToSortedList(entry.getKey(),expanded.partialScoresList);

                    if (logMemory) expanded.reservedBytesProcessing += 4;
                }

                if (logMemory) expanded.reservedBytesProcessing += 16;
            }

            tempmap = expanded.dists.get(s);
            if (tempmap!=null) {
                tempmap.remove(s);
                expanded.cmp.computeStoreScore(r,tempmap);

                if (logMemory) expanded.reservedBytesProcessing -= 12;
            }

            expanded.S.remove(s);
            expanded.S.add(r);

            return expanded;
        }

        public void expandSet (NodeT s) {
            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(s,s,0.0f));
            distanceHeaps.put(s,newHeap);

            if (logMemory) reservedBytesProcessing += 30;

            Map<NodeT,Float> tempmap = dists.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                dists.put (s,tempmap);

                if (logMemory) reservedBytesProcessing += 16;
            }
            tempmap.put (s,0.0f);
            boolean exists = removeFromSortedList (s,partialScoresList);
            if (logMemory && !exists) reservedBytesProcessing += 4;

            cmp.computeStoreScore (s,tempmap);
            addToSortedList(s,partialScoresList);

            while (!completeScoresHeap.isEmpty())
                addToSortedList (completeScoresHeap.delMin(),partialScoresList);

            while (!returnedResults.isEmpty())
                addToSortedList (returnedResults.remove(),partialScoresList);

            S.add (s);

            if (logMemory) reservedBytesProcessing += 12;
        }

        /*
        public GraphDivIterator<NodeT> expandSetReturn (NodeT s) {
            GraphDivIterator<NodeT> expanded = new GraphDivIterator<> (q,S,R,graph,minsum);

            expanded.cmp.scores.clear(); //////////////////////////////////////////////////////////////////////////
            //expanded.cmp.scores.putAll(cmp.scores); //////////////////////////////////////////////////////

            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                MinPQ<Edge<NodeT>> dh = expanded.distanceHeaps.get(entry.getKey());
                while (!dh.isEmpty()) dh.delMin();
                for (Edge<NodeT> edge : entry.getValue()) {
                    dh.insert(edge);
                }
            }

            expanded.partialScoresList.clear();
            expanded.partialScoresList.add(s);

            for (Entry<NodeT,Map<NodeT,Float>> entry : dists.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());
                expanded.dists.put (entry.getKey(),newmap);

                expanded.cmp.computeStoreScore (s,newmap); //////////////////////////////////////////////////////////

                expanded.addToSortedList(entry.getKey(),expanded.partialScoresList);
            }

            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(s,s,0.0f));
            expanded.distanceHeaps.put(s,newHeap);

            Map<NodeT,Float> tempmap = dists.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                expanded.dists.put (s,tempmap);
            }
            tempmap.put (s,0.0f);
            expanded.cmp.computeStoreScore (s,tempmap);

            expanded.S.add (s);
            return expanded;
        }
        */
    }

    public static void main (String[] args) {
        if (args.length<2) 
            throw new IllegalArgumentException("\n!! ERROR - Should provide a filename to a graph, and the desired size of the result !!");

        String graphfilename = args[0];

        boolean useMinSum = false;
        int k = Integer.parseInt(args[1]);
        if (k<0) {k=-k;useMinSum=true;}

        ArrayDeque<Long> S = new ArrayDeque<>();
        S.add(1000L);
        S.add(2000L);
        S.add(3000L);
        long q = 4000L;

        UndirectedGraph<Long> graph = new UndirectedGraph<>(graphfilename,NumberFormat.getNumberInstance(),true);

        int i=0;
        for (Long mu : new GraphDivIterable<>(q,S,graph.getNodes(),graph,useMinSum))
            if (++i>=k)
                break;
    }
}
