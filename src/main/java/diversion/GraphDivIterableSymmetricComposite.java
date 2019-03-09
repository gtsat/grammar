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
import grammar.DirectedGraph;

final class GraphDivIterableSymmetricComposite<NodeT> implements Iterable<NodeT> {

    private final Graph<NodeT> graph;
    private final ContentSimilarity<NodeT> sim;

    private final boolean useMinSum;

    private final Collection<NodeT> R;

    private final Collection<NodeT> S;
    private final NodeT q;

    private final float lambda;
    private final float alpha;
    private final float beta;

    public GraphDivIterableSymmetricComposite (NodeT q, Collection<NodeT> S, Collection<NodeT> R, 
                             Graph<NodeT> graph,
                             ContentSimilarity<NodeT> sim,
                             boolean useMinSum,
                             float l,float a,float b) {

        lambda = l;
        alpha = a;
        beta = b;

        this.useMinSum = useMinSum;

        this.graph = graph;
        this.sim = sim;

        this.R = R;
        this.S = S;
        this.q = q;
    }


    @Override public Iterator<NodeT> iterator () {return new GraphDivIterator<>(q,S,R,graph,sim,useMinSum,lambda,alpha,beta);}

    final public class GraphDivIterator<NodeT> implements Iterator<NodeT> {

        private final Map<NodeT,Map<NodeT,Float>> scores = new HashMap<>();
        private final Map<NodeT,MinPQ<Edge<NodeT>>> scoreHeaps = new HashMap<>();

        private final Map<NodeT,MinPQ<Edge<NodeT>>> distanceHeapsTo = new HashMap<>();
        private final Map<NodeT,MinPQ<Edge<NodeT>>> distanceHeapsFrom = new HashMap<>();

        private final Map<NodeT,Map<NodeT,Float>> distsTo = new HashMap<>();
        private final Map<NodeT,Map<NodeT,Float>> distsFrom = new HashMap<>();

        private final ArrayList<NodeT> partialScoresList = new ArrayList<>();

        private final ArrayDeque<NodeT> returnedResults = new ArrayDeque<>();

        private final MinPQ<NodeT> completeScoresHeap;

        private final VertexComparatorComposite<NodeT> cmp;
        private final ContentSimilarity<NodeT> sim;
        private final Graph<NodeT> graph;

        private final Collection<NodeT> R;

        private final Collection<NodeT> S;
        private final NodeT q;

        private final boolean minsum;

        private final float lambda;
        private final float alpha;
        private final float beta;



        private final boolean logMemory = true;
        private int reservedBytesProcessing = 40 + 88 + 8;
        public int getMaxMemRequirements () {return logMemory?reservedBytesProcessing:-1;}



        private long timeoutmillis = 5000;
        private boolean activateTimeOut = false;

        public void activateTimeOut () {activateTimeOut=true;}
        public void setTimeOutParam (long newvalue) {timeoutmillis = newvalue;}



        /*
         * NOTE: Suggested optimization when computing the threshold for retrieving 
         *       the list elements achieving low scores: instead of examining just 
         *       the first and then calculating its respective bound, do this for 
         *       the x first elements and then opt for the lowest bound. Depending on 
         *       the value of x, less elements will have to be accessed overall as a result.
         */

        private static final int C = 5;

        @SuppressWarnings (value={"unchecked"})
        private float calculatePartialScoresLowerBound () {
            if (partialScoresList.isEmpty()) return Float.MAX_VALUE;
            float threshold = Float.MAX_VALUE;
            for (int i=0; i<(partialScoresList.size()<C?partialScoresList.size():C); ++i) {
                Map<NodeT,Float> tempmap = new HashMap<>();
                for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : scoreHeaps.entrySet()) {
                    Float tempdist = scores.get(partialScoresList.get(i)).get(entry.getKey());
                    if (tempdist==null) //tempmap.put(entry.getKey(),entry.getValue().min().weight);
                    tempmap.put(entry.getKey(),entry.getValue().isEmpty()?0:entry.getValue().min().weight);
                    //if (tempdist==null) tempmap.put(entry.getKey(),0.0);
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
                Map<NodeT,Float> encounters = scores.get(unmatched);
                Map<NodeT,Float> tempmap = new HashMap<>();
                for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : scoreHeaps.entrySet()) {
                    Float tempdist = encounters.get(entry.getKey());
                    if (tempdist!=null) tempmap.put (entry.getKey(),tempdist);
                    else tempmap.put (entry.getKey(),entry.getValue().isEmpty()?0:entry.getValue().min().weight);
                        //tempmap.put (entry.getKey(),entry.getValue().min().weight);
                }
                float tempscore = cmp.computeScore(tempmap);
                if (tempscore<minscore) minscore = tempscore;
            }
            cmp.overrideStoreScore ((NodeT)dummy, null);
            return minscore;
        }

        private float calculateUnprocessedLowerBound () {
            Map<NodeT,Float> tempmap = new HashMap<>();
            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : scoreHeaps.entrySet())
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
                                 ContentSimilarity<NodeT>sim,
                                 boolean useMinSum,
                                 float l,float a,float b) {

            lambda = l;
            alpha = a;
            beta = b;

            this.graph = graph;
            this.sim = sim;

            this.R = R;

            this.q = q;
            this.S = new ArrayList<>(S);

            this.minsum = useMinSum;

            cmp = useMinSum?
                            new VertexSumComparatorComposite<>(q,graph,sim,l,a,b)
                           :new VertexMaxComparatorComposite<>(q,graph,sim,l,a,b);
            completeScoresHeap = new MinPQ<>(cmp);


            if (logMemory) reservedBytesProcessing += 32;

            /* initialize partial score heap for the contribution of q */
            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(q,q,0.0f));
            scoreHeaps.put (q,newHeap);

            if (logMemory) reservedBytesProcessing += 32;

            Map<NodeT,Float> tempmap = scores.get(q);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                scores.put (q,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put (q,0.0f);

            if (logMemory) reservedBytesProcessing += 12;

            cmp.computeScore (q,tempmap);
            partialScoresList.add (q);

            if (logMemory) reservedBytesProcessing += 4;


            /* initialize distance heap for keeping track of the distances to q */
            newHeap = new MinPQ<>();
            distanceHeapsTo.put (q,newHeap);

            if (logMemory) reservedBytesProcessing += 20;

            tempmap = distsTo.get(q);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                distsTo.put (q,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put (q,0.0f);
            
            if (logMemory) reservedBytesProcessing += 12;

            for (Edge<NodeT> edge : graph.getEdgesTo(q)) {
                newHeap.insert (edge);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsTo.get(edge.from);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsTo.put (edge.from,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put (q,edge.weight);

                if (logMemory) reservedBytesProcessing += 12;
            }


            /* initialize distance heap for keeping track of the distances from q */
            newHeap = new MinPQ<>();
            distanceHeapsFrom.put (q,newHeap);

            if (logMemory) reservedBytesProcessing += 20;

            tempmap = distsFrom.get(q);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                distsFrom.put (q,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put (q,0.0f);

            if (logMemory) reservedBytesProcessing += 12;

            for (Edge<NodeT> edge : graph.getEdgesFrom(q)) {
                newHeap.insert (edge);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsFrom.get(edge.to);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsFrom.put(edge.to,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put(q,edge.weight);

                if (logMemory) reservedBytesProcessing += 12;
            }


            /* do the same for each element of S */
            for (NodeT s : S) {

                /* initialize partial score heap for the contribution of s */
                newHeap = new MinPQ<>();
                newHeap.insert (new Edge<>(s,s,0.0f));
                scoreHeaps.put (s,newHeap);

                if (logMemory) reservedBytesProcessing += 32;

                tempmap = scores.get(s);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    scores.put(s,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put(s,0.0f);

                if (logMemory) reservedBytesProcessing += 12;


                boolean exists = removeFromSortedList (s,partialScoresList);
                cmp.computeScore (s,tempmap);
                addToSortedList (s,partialScoresList);
                
                if (logMemory && !exists) reservedBytesProcessing += 4;

                if (logMemory) reservedBytesProcessing += 4;


                /* initialize distance heap for keeping track of the distances to s */
                newHeap = new MinPQ<>();
                distanceHeapsTo.put (s,newHeap);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsTo.get(s);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsTo.put(s,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put(s,0.0f);

                if (logMemory) reservedBytesProcessing += 12;

                for (Edge<NodeT> edge : graph.getEdgesTo(s)) {
                    newHeap.insert (edge);

                    if (logMemory) reservedBytesProcessing += 16;

                    tempmap = distsTo.get(edge.from);
                    if (tempmap==null) {
                        tempmap = new HashMap<>();
                        distsTo.put(edge.from,tempmap);

                        if (logMemory) reservedBytesProcessing += 20;
                    }
                    tempmap.put (s,edge.weight);

                    if (logMemory) reservedBytesProcessing += 12;
                }


                /* initialize distance heap for keeping track of the distances from s */
                newHeap = new MinPQ<>();
                distanceHeapsFrom.put (s,newHeap);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsFrom.get(s);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsFrom.put(s,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put(s,0.0f);

                if (logMemory) reservedBytesProcessing += 12;

                for (Edge<NodeT> edge : graph.getEdgesFrom(s)) {
                    newHeap.insert (edge);

                    if (logMemory) reservedBytesProcessing += 20;

                    tempmap = distsFrom.get(edge.to);
                    if (tempmap==null) {
                        tempmap = new HashMap<>();
                        distsFrom.put(edge.to,tempmap);

                        if (logMemory) reservedBytesProcessing += 20;
                    }
                    tempmap.put (s,edge.weight);

                    if (logMemory) reservedBytesProcessing += 12;
                }
            }
        }

        @Override public boolean hasNext () {
            if (!partialScoresList.isEmpty() || !completeScoresHeap.isEmpty()) return true;

            for (MinPQ<Edge<NodeT>> distanceHeap : distanceHeapsFrom.values())
                if (!distanceHeap.isEmpty())
                    return true;

            for (MinPQ<Edge<NodeT>> distanceHeap : distanceHeapsTo.values())
                if (!distanceHeap.isEmpty())
                    return true;

            for (MinPQ<Edge<NodeT>> scoreHeap : scoreHeaps.values())
                if (!scoreHeap.isEmpty())
                    return true;

            return false;
        }
        @Override public void remove () {throw new UnsupportedOperationException("\n!! ERROR - Cannot remove nodes from the graph !!");}
        @Override public NodeT next () {
            float minCostExpansion;
            int cardinality = S.size()+1;
            long start = System.currentTimeMillis();
            while (completeScoresHeap.isEmpty() 
                    || (!activateTimeOut || System.currentTimeMillis() - start < timeoutmillis) 
                    && cmp.scores.get(completeScoresHeap.min())>calculatePartialScoresLowerBound()) {
                    //|| cmp.scores.get(completeScoresHeap.min())>calculateUnprocessedLowerBound()) {


                minCostExpansion = Float.MAX_VALUE;
                MinPQ<Edge<NodeT>> minCostExpansionHeap = null;
                for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : scoreHeaps.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        if (minCostExpansionHeap==null || entry.getValue().min().weight<minCostExpansion) {
                            minCostExpansion = entry.getValue().min().weight;
                            minCostExpansionHeap = entry.getValue();
                        }
                    }
                }

                if (minCostExpansionHeap!=null) {
                    Edge<NodeT> stop = minCostExpansionHeap.delMin();

                    if (logMemory) reservedBytesProcessing += 20;

                    Map<NodeT,Float> tempmap = scores.get(stop.to);

                    if (tempmap.size()==cardinality) {
                        if (!q.equals(stop.to) && !S.contains(stop.to)) {// && R.contains(top.to))
                            completeScoresHeap.insert (stop.to);
                            if (logMemory) reservedBytesProcessing += 4;
                        }

                        removeFromSortedList (stop.to,partialScoresList);
                    }
/*
if (!distanceHeapsFrom.containsKey(stop.from)) throw new RuntimeException("\n!! ERROR - Unable to retrieve distanceheap pointing inwards !!\t"+j+"\t"+distanceHeapsFrom.containsKey(stop.to));
if (!distanceHeapsTo.containsKey(stop.from)) throw new RuntimeException("\n!! ERROR - Unable to retrieve distanceheap pointing outwards !!"+j);
if (!scoreHeaps.containsKey(stop.from)) throw new RuntimeException("\n!! ERROR - Unable to retrieve scoreheap !!"+j);
*/
                    float gamma = stop.from.equals(q)?cmp.alpha:cmp.beta;
                    MinPQ<Edge<NodeT>> scoreheap = scoreHeaps.get(stop.from);
                    MinPQ<Edge<NodeT>> distanceheapTo = distanceHeapsTo.get(stop.from);
                    MinPQ<Edge<NodeT>> distanceheapFrom = distanceHeapsFrom.get(stop.from);

                    //while (scoreheap.isEmpty() || cmp.scores.get(scoreheap.min().to) >= scoreheap.min().weight) {

                        while (!distanceheapTo.isEmpty() && (scoreheap.isEmpty() || .5 * gamma * distanceheapTo.min().weight < scoreheap.min().weight)) { //cmp.scores.get(scoreheap.min().to))) {
                            Edge<NodeT> top = distanceheapTo.delMin();

                            if (logMemory) reservedBytesProcessing -= 20;

                            Float symmetricdistance = distsFrom.containsKey(top.from)?distsFrom.get(top.from).get(top.to):null;
                            if (symmetricdistance==null) symmetricdistance = Float.MAX_VALUE;
                            float newscore = .5f * gamma * (top.weight + symmetricdistance);
                            scoreheap.insert (new Edge<>(top.to,top.from,newscore));

                            if (logMemory) reservedBytesProcessing += 16;

                            tempmap = scores.get (top.from);
                            if (tempmap==null) {
                                tempmap = new HashMap<>();
                                scores.put (top.from,tempmap);

                                if (logMemory) reservedBytesProcessing += 20;
                            }
                            tempmap.put (top.to,newscore);
                            cmp.computeScore (top.from,tempmap);

                            if (logMemory) reservedBytesProcessing += 12;

                            for (Edge<NodeT> edge : graph.getEdgesTo(top.from)) {
                                float newdist = edge.weight + top.weight;
                                tempmap = distsTo.get(edge.from);
                                if (tempmap==null) {
                                    tempmap = new HashMap<>();
                                    distsTo.put (edge.from,tempmap);
                                    tempmap.put (top.to,newdist);
                                    distanceheapTo.insert(new Edge<>(edge.from,top.to,newdist));

                                    if (logMemory) reservedBytesProcessing += 50;
                                }else if (!tempmap.containsKey(top.to) || newdist<tempmap.get(top.to)) {
                                    tempmap.put (top.to,newdist);
                                    distanceheapTo.insert(new Edge<>(edge.from,top.to,newdist));

                                    if (logMemory) reservedBytesProcessing += 32;
                                }
                            }
                        }

                        while (!distanceheapFrom.isEmpty() && (scoreheap.isEmpty() || .5 * gamma * distanceheapFrom.min().weight < scoreheap.min().weight)) { //cmp.scores.get(scoreheap.min().to))) {
                            Edge<NodeT> top = distanceheapFrom.delMin();

                            if (logMemory) reservedBytesProcessing -= 20;

                            Float symmetricdistance = distsTo.containsKey(top.to)?distsTo.get(top.to).get(top.from):null;
                            if (symmetricdistance==null) symmetricdistance = Float.MAX_VALUE;

                            float newscore = .5f * gamma * (top.weight + symmetricdistance);
                            scoreheap.insert (new Edge<>(top.from,top.to,newscore));

                            if (logMemory) reservedBytesProcessing += 20;

                            tempmap = scores.get (top.to);
                            if (tempmap==null) {
                                tempmap = new HashMap<>();
                                scores.put (top.to,tempmap);

                                if (logMemory) reservedBytesProcessing += 20;
                            }
                            tempmap.put (top.from,newscore);
                            cmp.computeScore (top.to,tempmap);

                            if (logMemory) reservedBytesProcessing += 12;

                            for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                                float newdist = edge.weight + top.weight;
                                tempmap = distsFrom.get(edge.to);
                                if (tempmap==null) {
                                    tempmap = new HashMap<>();
                                    distsFrom.put (edge.to,tempmap);
                                    tempmap.put (top.from,newdist);
                                    distanceheapFrom.insert(new Edge<>(top.from,edge.to,newdist));

                                    if (logMemory) reservedBytesProcessing += 50;
                                }else if (!tempmap.containsKey(top.from) || newdist<tempmap.get(top.from)) {
                                    tempmap.put (top.from,newdist);
                                    distanceheapFrom.insert(new Edge<>(top.from,edge.to,newdist));

                                    if (logMemory) reservedBytesProcessing += 32;
                                }
                            }
                        }
                    //}
                }else throw new RuntimeException("\n!! ERROR - Unable to find score heap to expand !!");
            }

            //System.out.println ("** Score: "+cmp.scores.get(completeScoresHeap.min())+"\t for node "+completeScoresHeap.min());
            if (completeScoresHeap.isEmpty()) return null;
            else{
                returnedResults.add(completeScoresHeap.min());
                return completeScoresHeap.delMin();
            }
        }

        public void expandSet (NodeT s) {
            /* Initialize score heap and maps so as to accomodate search from s */
            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(s,s,0.0f));
            scoreHeaps.put(s,newHeap);

            if (logMemory) reservedBytesProcessing += 32;

            Map<NodeT,Float> tempmap = scores.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                scores.put (s,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put (s,0.0f);


            /* Initialize distance heap and maps so as to accomodate search from s one-way */
            tempmap = distsFrom.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                distsFrom.put (s,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put (s,0.0f);

            if (logMemory) reservedBytesProcessing += 12;

            newHeap = new MinPQ<>();
            distanceHeapsFrom.put (s,newHeap);

            if (logMemory) reservedBytesProcessing += 20;

            for (Edge<NodeT> edge : graph.getEdgesFrom(s)) {
                newHeap.insert (edge);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsFrom.get (edge.to);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsFrom.put (edge.to,tempmap);

                   if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put (s,edge.weight);

                if (logMemory) reservedBytesProcessing += 12;
            }


            /* Initialize distance heap and maps so as to accomodate search from s round-trip */
            tempmap = distsTo.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                distsTo.put(s,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put (s,0.0f);

            if (logMemory) reservedBytesProcessing += 12;

            newHeap = new MinPQ<>();
            distanceHeapsTo.put(s,newHeap);

            if (logMemory) reservedBytesProcessing += 20;

            for (Edge<NodeT> edge : graph.getEdgesTo(s)) {
                newHeap.insert(edge);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsTo.get (edge.from);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsTo.put (edge.from,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put (s,edge.weight);

                if (logMemory) reservedBytesProcessing += 12;
            }


            /* update list of nodes sorted by partial scores */
            boolean exists = removeFromSortedList (s,partialScoresList);
            cmp.computeScore (s,tempmap);
            addToSortedList (s,partialScoresList);

            if (logMemory && !exists) reservedBytesProcessing += 4;

            while (!completeScoresHeap.isEmpty())
                addToSortedList (completeScoresHeap.delMin(),partialScoresList);

            while (!returnedResults.isEmpty())
                addToSortedList (returnedResults.remove(),partialScoresList);

            S.add (s);

            if (logMemory) reservedBytesProcessing += 4;
        }

        public void replaceSet (NodeT s,NodeT r) {
            /* Initialize score heap and maps so as to accomodate search from r and discard heaps for s */
            scoreHeaps.remove(s);
            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(r,r,0.0f));
            scoreHeaps.put (r,newHeap);

            if (logMemory) reservedBytesProcessing += 32;


            Map<NodeT,Float> tempmap = scores.get(r);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                scores.put (r,tempmap);

                if (logMemory) reservedBytesProcessing += 20;
            }
            tempmap.put(r,0.0f);

            if (logMemory) reservedBytesProcessing += 12;


            /* Initialize distance heap and maps so as to accomodate search from r one-way and discard heaps for s */
            distanceHeapsFrom.remove (s);
            newHeap = new MinPQ<>();
            distanceHeapsFrom.put (r,newHeap);
            
            if (logMemory) reservedBytesProcessing += 20;

            for (Edge<NodeT> edge : graph.getEdgesFrom(r)) {
                newHeap.insert (edge);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsFrom.get(edge.to);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsFrom.put(edge.to,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put (r,edge.weight);

                if (logMemory) reservedBytesProcessing += 12;
            }

            for (Entry<NodeT,Map<NodeT,Float>> entry : distsFrom.entrySet()) {
                if (logMemory) reservedBytesProcessing -= 12;
                entry.getValue().remove(s);
            }


            /* Initialize distance heap and maps so as to accomodate search from r round-trip and discard heaps for s */
            distanceHeapsTo.remove(s);
            newHeap = new MinPQ<>();
            distanceHeapsTo.put (r,newHeap);
            
            if (logMemory) reservedBytesProcessing += 20;

            for (Edge<NodeT> edge : graph.getEdgesTo(r)) {
                newHeap.insert (edge);

                if (logMemory) reservedBytesProcessing += 20;

                tempmap = distsTo.get(edge.from);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    distsTo.put(edge.from,tempmap);

                    if (logMemory) reservedBytesProcessing += 20;
                }
                tempmap.put (r,edge.weight);

                if (logMemory) reservedBytesProcessing += 12;
            }

            for (Entry<NodeT,Map<NodeT,Float>> entry : distsTo.entrySet()) {
                entry.getValue().remove(s);

                if (logMemory) reservedBytesProcessing -= 12;
            }


            /* update list of nodes sorted by partial scores */
            removeFromSortedList (s,partialScoresList);

            for (Entry<NodeT,Map<NodeT,Float>> entry : scores.entrySet()) {
                entry.getValue().remove(s);

                if (logMemory) reservedBytesProcessing -= 12;

                removeFromSortedList (entry.getKey(),partialScoresList);
                cmp.computeScore(entry.getKey(),entry.getValue());
                if (!entry.getValue().isEmpty())
                    addToSortedList(entry.getKey(),partialScoresList);
            }

            addToSortedList (r,partialScoresList);

            if (logMemory) reservedBytesProcessing += 4;

            S.remove(s);
            S.add(r);
        }

        public GraphDivIterator<NodeT> replaceSetReturn (NodeT s,NodeT r) {
            GraphDivIterator<NodeT> expanded = new GraphDivIterator<> (q,S,R,graph,sim,minsum,lambda,alpha,beta);

            /* Initialize score heap and maps so as to accomodate search from r and discard heaps for s */
            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : scoreHeaps.entrySet()) {
                if (!s.equals(entry.getKey())) {
                    MinPQ<Edge<NodeT>> dh = expanded.scoreHeaps.get(entry.getKey());

                    if (logMemory) expanded.reservedBytesProcessing -= dh.size()*20;

                    while (!dh.isEmpty()) dh.delMin();
                    for (Edge<NodeT> edge : entry.getValue()) dh.insert(edge);

                    if (logMemory) expanded.reservedBytesProcessing += dh.size()*20;
                }
            }

            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(r,r,0.0f));
            expanded.scoreHeaps.put (r,newHeap);
            expanded.scoreHeaps.remove(s);

            if (logMemory) expanded.reservedBytesProcessing += 30;


            /* Initialize distance heap and maps so as to accomodate search from r one-way and discard heaps for s */
            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeapsFrom.entrySet()) {
                if (!s.equals(entry.getKey())) {
                    MinPQ<Edge<NodeT>> dh = expanded.distanceHeapsFrom.get(entry.getKey());

                    if (logMemory) expanded.reservedBytesProcessing -= dh.size()*20;

                    while (!dh.isEmpty()) dh.delMin();
                    for (Edge<NodeT> edge : entry.getValue()) dh.insert(edge);

                    if (logMemory) expanded.reservedBytesProcessing += dh.size()*20;
                }
            }

            for (Entry<NodeT,Map<NodeT,Float>> entry : distsFrom.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());

                if (logMemory) expanded.reservedBytesProcessing += newmap.size()*12;

                newmap.remove(s);
                expanded.distsFrom.put (entry.getKey(),newmap);

                if (logMemory) expanded.reservedBytesProcessing += 8;
            }

            newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(r,r,0.0f));
            expanded.distanceHeapsFrom.put(r,newHeap);
            expanded.distanceHeapsFrom.remove(s);

            if (logMemory) expanded.reservedBytesProcessing += 30;

            Map<NodeT,Float> tempmap = expanded.distsFrom.get(r);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                expanded.distsFrom.put(r,tempmap);

                if (logMemory) expanded.reservedBytesProcessing += 20;
            }
            tempmap.put(r,0.0f);

            if (logMemory) expanded.reservedBytesProcessing += 12;

            for (Edge<NodeT> edge : graph.getEdgesFrom(r)) {
                newHeap.insert(edge);

                if (logMemory) expanded.reservedBytesProcessing += 20;

                tempmap = expanded.distsFrom.get(edge.to);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    expanded.distsFrom.put(edge.to,tempmap);

                    if (logMemory) expanded.reservedBytesProcessing += 20;
                }
                tempmap.put(r,edge.weight);

                if (logMemory) expanded.reservedBytesProcessing += 12;
            }


            /* Initialize distance heap and maps so as to accomodate search from r round-trip and discard heaps for s */
            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeapsTo.entrySet()) {
                if (!s.equals(entry.getKey())) {
                    MinPQ<Edge<NodeT>> dh = expanded.distanceHeapsTo.get(entry.getKey());

                    if (logMemory) expanded.reservedBytesProcessing -= dh.size()*20;

                    while (!dh.isEmpty()) dh.delMin();
                    for (Edge<NodeT> edge : entry.getValue()) dh.insert(edge);

                    if (logMemory) expanded.reservedBytesProcessing += dh.size()*20;
                }
            }

            for (Entry<NodeT,Map<NodeT,Float>> entry : distsTo.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());

                if (logMemory) expanded.reservedBytesProcessing += newmap.size()*12;

                newmap.remove(s);
                expanded.distsTo.put (entry.getKey(),newmap);
                
                if (logMemory) expanded.reservedBytesProcessing += 8;
            }

            newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(r,r,0.0f));
            expanded.distanceHeapsTo.put (r,newHeap);
            expanded.distanceHeapsTo.remove (s);

            if (logMemory) expanded.reservedBytesProcessing += 30;

            tempmap = expanded.distsTo.get(r);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                expanded.distsTo.put (r,tempmap);

                if (logMemory) expanded.reservedBytesProcessing += 20;
            }
            tempmap.put (r,0.0f);

            if (logMemory) expanded.reservedBytesProcessing += 12;

            for (Edge<NodeT> edge : graph.getEdgesTo(r)) {
                newHeap.insert(edge);

                if (logMemory) expanded.reservedBytesProcessing += 20;

                tempmap = expanded.distsTo.get(edge.from);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    expanded.distsTo.put(edge.from,tempmap);

                    if (logMemory) expanded.reservedBytesProcessing += 20;
                }
                tempmap.put(r,edge.weight);

                if (logMemory) expanded.reservedBytesProcessing += 12;
            }


            /* update list of nodes sorted by partial scores */

            if (logMemory) expanded.reservedBytesProcessing -= expanded.partialScoresList.size()*4;

            expanded.cmp.scores.clear();
            expanded.partialScoresList.clear();

            for (Entry<NodeT,Map<NodeT,Float>> entry : scores.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());

                if (logMemory) expanded.reservedBytesProcessing += newmap.size()*12;

                newmap.remove(s);
                expanded.scores.put (entry.getKey(),newmap);
                expanded.cmp.computeScore(entry.getKey(),newmap);

                if (logMemory) expanded.reservedBytesProcessing += 8;

                if (!newmap.isEmpty()) {
                    expanded.addToSortedList(entry.getKey(),expanded.partialScoresList);
                    if (logMemory) expanded.reservedBytesProcessing += 4;
                }
            }

            tempmap = expanded.scores.get(r);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                expanded.scores.put(r,tempmap);

                if (logMemory) expanded.reservedBytesProcessing += 20;
            }
            tempmap.put(r,0.0f);
            expanded.cmp.computeScore(r,tempmap);

            if (logMemory) expanded.reservedBytesProcessing += 16;

            expanded.partialScoresList.add(r);

            expanded.S.remove(s);
            expanded.S.add(r);

            return expanded;
        }

        /*
        public GraphDivIterator<NodeT> expandSetReturn (NodeT s) {
            GraphDivIterator<NodeT> expanded = new GraphDivIterator<> (q,S,R,graph,sim,minsum);

            expanded.cmp.scores.clear(); //////////////////////////////////////////////////////////////////////////
            expanded.cmp.scores.putAll(cmp.scores); //////////////////////////////////////////////////////

            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : scoreHeaps.entrySet()) {
                MinPQ<Edge<NodeT>> dh = expanded.scoreHeaps.get(entry.getKey());
                while (!dh.isEmpty()) dh.delMin();
                for (Edge<NodeT> edge : entry.getValue()) dh.insert(edge);
            }

            for (Entry<NodeT,MinPQ<Edge<NodeT>>> entry : distanceHeaps.entrySet()) {
                MinPQ<Edge<NodeT>> dh = expanded.distanceHeaps.get(entry.getKey());
                while (!dh.isEmpty()) dh.delMin();
                for (Edge<NodeT> edge : entry.getValue()) dh.insert(edge);
            }

            expanded.partialScoresList.clear();
            expanded.partialScoresList.add(s);

            for (Entry<NodeT,Map<NodeT,Float>> entry : scores.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());
                expanded.scores.put (entry.getKey(),newmap);

                //expanded.cmp.computeScore (s,newmap); //////////////////////////////////////////////////////////

                expanded.addToSortedList(entry.getKey(),expanded.partialScoresList);
            }

            for (Entry<NodeT,Map<NodeT,Float>> entry : dists.entrySet()) {
                Map<NodeT,Float> newmap = new HashMap<> (entry.getValue());
                expanded.dists.put (entry.getKey(),newmap);
            }

            MinPQ<Edge<NodeT>> newHeap = new MinPQ<>();
            newHeap.insert (new Edge<>(s,s,0.0));
            expanded.scoreHeaps.put(s,newHeap);

            Map<NodeT,Float> tempmap = scores.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                expanded.scores.put(s,tempmap);
            }
            tempmap.put (s,0.0);
            expanded.cmp.computeScore (s,tempmap);

            newHeap = new MinPQ<>();
            expanded.distanceHeaps.put(s,newHeap);

            tempmap = dists.get(s);
            if (tempmap==null) {
                tempmap = new HashMap<>();
                expanded.dists.put(s,tempmap);
            }
            tempmap.put(s,0.0);

            for (Edge<NodeT> edge : graph.getEdgesFrom(s)) {
                newHeap.insert (edge);

                tempmap = dists.get(edge.to);
                if (tempmap==null) {
                    tempmap = new HashMap<>();
                    expanded.dists.put(edge.to,tempmap);
                }
                tempmap.put(s,edge.weight);
            }

            expanded.S.add (s);
            return expanded;
        }
        */
    }

    public static void main (String[] args) {
        if (args.length<3) 
            throw new IllegalArgumentException("\n!! ERROR - Should provide a filename to a graph, the desired size of the result, and the number of features !!");

        String graphfilename = args[0];

        boolean useMinSum = false;
        int k = Integer.parseInt(args[1]);
        if (k<0) {k=-k;useMinSum=true;}

        ArrayDeque<Long> S = new ArrayDeque<>();
        S.add(1000L);
        S.add(2000L);
        S.add(3000L);
        long q = 4000L;

        Graph<Long> graph = new DirectedGraph<>(graphfilename,NumberFormat.getNumberInstance(),true);
        ContentSimilarity<Long> context = new ContentSimilarity<>();
        Collection<Long> vertices = graph.getNodes();
        int W = Integer.parseInt(args[2]);
        for (long u : vertices) {
            Map<String,Float> description = new HashMap<>();
            for (int i=0;i<W;++i) description.put ("lemma"+i, (float)Math.random()); // random weights
            context.insertNode(u,description);
        }

        int i=0;
        for (Long mu : new GraphDivIterableSymmetricComposite<>(q,S,graph.getNodes(),graph,context,useMinSum,0.5f,0.0f,1.0f))
            if (++i>=k) break;
    }
}

