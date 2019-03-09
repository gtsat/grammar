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
import grammar.MaxPQ;
import grammar.DirectedGraph;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Computes the k most diversified sets of n vertices from 
 * a predefined set of people in a social network, around 
 * query centre q (standing for some user) using greedy 
 * heuristics. 
 */

final public class GreedyDiversion {

    private static final boolean useCoordinatedSearch = true;


    private static long maxMemConsumption = 0L;
    public static long getMaxMemRequirements () {return recursiveSetExpansion||!useCoordinatedSearch?-1:maxMemConsumption;}

    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<NodeT> R, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                                              int k, int n, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b, 
                                                              ArrayList<Float> scores) {

        return compute (q,R,graph,context,k,n,minsum,symmetric,l,a,b,scores,-1);
    }
    
    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<NodeT> R, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context,
                                                              int k, int n, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b,
                                                              ArrayList<Float> scores,
                                                              long timeout) {

        maxMemConsumption = 0L;

        if (!graph.containsNode(q)) throw new RuntimeException ("\n!! ERROR - Selected query center "+q+" is not a vertex of the graph !!");

        /* first, get the k vertices from R closest to query centre q */
        DivSetComparatorOrdered<NodeT> cmp = minsum?new SumDivSetComparatorOrdered<>(q,graph,context,l,a,b):new MaxDivSetComparatorOrdered<>(q,graph,context,l,a,b);

        MinPQ<Edge<NodeT>> pathsto = new MinPQ<>();
        MinPQ<Edge<NodeT>> pathsfrom = new MinPQ<>();

        Map<NodeT,Float> distanceto = new HashMap<>();
        Map<NodeT,Float> distancefrom = new HashMap<>();

        MaxPQ<Edge<NodeT>> closestheap = new MaxPQ<>();
        Set<NodeT> closestset = new HashSet<>();

        pathsfrom.insert(new Edge<>(q,q,0.0f));
        distancefrom.put (q,0.0f);

        if (symmetric) {
            pathsto.insert(new Edge<>(q,q,0.0f));
            distanceto.put (q,0.0f);
        }

        while (!pathsfrom.isEmpty() || (symmetric && !pathsto.isEmpty())) {

            if (!pathsfrom.isEmpty()) {
                Edge<NodeT> top = pathsfrom.delMin();

                if (top.to!=q && !closestset.contains(top.to)) {
                    Float symmetricdistance = symmetric?distanceto.get(top.to):top.weight;
                    if (symmetricdistance==null) symmetricdistance = Float.MAX_VALUE;
                    float newweight = cmp.alpha*.5f *(top.weight+symmetricdistance)+(1-cmp.alpha)*(1-context.similarity(q,top.to));
                    if (closestheap.size() < k) {
                        closestset.add (top.to);
                        closestheap.insert (new Edge<>(top.from,top.to,newweight));
                    }else if (newweight < closestheap.max().weight) {
                        closestset.add (top.to);
                        closestheap.delMax ();
                        closestheap.insert (new Edge<>(top.from,top.to,newweight));
                    }
                }

                for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                    if (!distancefrom.containsKey(edge.to)) {
                        if (closestheap.size()<k || cmp.alpha * .5f * (top.weight + edge.weight) < closestheap.max().weight) {
                            pathsfrom.insert (new Edge<>(top.from,edge.to,top.weight+edge.weight));
                            distancefrom.put (edge.to,top.weight+edge.weight);
                        }
                    }
                }
            }
/**/
            if (symmetric && !pathsto.isEmpty()) {
                Edge<NodeT> top = pathsto.delMin();

                if (top.from!=q && !closestset.contains(top.from)) {
                    Float symmetricdistance = distancefrom.get (top.from);
                    if (symmetricdistance==null) symmetricdistance = Float.MAX_VALUE;
                    float newweight = cmp.alpha * .5f * (top.weight + symmetricdistance) + (1-cmp.alpha) * (1 - context.similarity(q,top.from));
                    if (closestheap.size() < k) {
                        closestset.add (top.from);
                        closestheap.insert (new Edge<>(top.to,top.from,newweight));
                    }else if (newweight < closestheap.max().weight) {
                        closestset.add (top.from);
                        closestheap.delMax ();
                        closestheap.insert (new Edge<>(top.to,top.from,newweight));
                    }
                }

                for (Edge<NodeT> edge : graph.getEdgesTo(top.from)) {
                    if (!distanceto.containsKey(edge.from)) {
                        if (closestheap.size()<k || cmp.alpha * .5f * (top.weight + edge.weight) < closestheap.max().weight) {
                            pathsto.insert (new Edge<>(edge.from,top.to,top.weight+edge.weight));
                            distanceto.put (edge.from,top.weight+edge.weight);
                        }
                    }
                }
            }
/**/
        }

        /* second, initialize the candidate sets with the k nearest neighbours of q */
        ArrayList<ArrayList<NodeT>> candidates = new ArrayList<>();
        for (Edge<NodeT> path : closestheap) {
            ArrayList<NodeT> newset = new ArrayList<>();
            newset.add (path.to);
            candidates.add (newset);
        }

        return compute (q,R,candidates,graph,context,n,minsum,symmetric,l,a,b,scores,timeout);
    }

    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<NodeT> R, ArrayList<ArrayList<NodeT>> candidates, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                                              int n, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b, 
                                                              ArrayList<Float> scores) {

        return compute (q,R,candidates,graph,context,n,minsum,symmetric,l,a,b,scores,-1);
    }

    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<NodeT> R, ArrayList<ArrayList<NodeT>> candidates, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                                              int n, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b, 
                                                              ArrayList<Float> scores, 
                                                              long timeout) {

        Comparator<ArrayList<NodeT>> cmp = minsum?
                                           (symmetric?
                                            new diversion.SumDivSetComparatorSymmetric<>(q,graph,context,l,a,b)
                                            :new diversion.SumDivSetComparatorOrdered<>(q,graph,context,l,a,b))
                                            :(symmetric?
                                            new diversion.MaxDivSetComparatorSymmetric<>(q,graph,context,l,a,b)
                                            :new diversion.MaxDivSetComparatorOrdered<>(q,graph,context,l,a,b));

        long memConsumption = 0L;
        Map<ArrayList<NodeT>,Iterator<NodeT>> iterators = new HashMap<>();
        for (ArrayList<NodeT> candidate : candidates) {
            if (useCoordinatedSearch) {
                Iterator<NodeT> newiterator = useComposite?
                                              (symmetric?
                                              new GraphDivIterableSymmetricComposite<>(q,candidate,R,graph,context,minsum,l,a,b).iterator()
                                              :new GraphDivIterableOrderedComposite<>(q,candidate,R,graph,context,minsum,l,a,b).iterator())
                                              :new GraphDivIterable<>(q,candidate,R,graph,minsum).iterator();
                iterators.put (candidate,newiterator);

                if (timeout>0) {
                    if (useComposite) {
                        if (symmetric) {
                            ((GraphDivIterableSymmetricComposite.GraphDivIterator)newiterator).activateTimeOut();
                            ((GraphDivIterableSymmetricComposite.GraphDivIterator)newiterator).setTimeOutParam(timeout);
                        }else{
                            ((GraphDivIterableOrderedComposite.GraphDivIterator)newiterator).activateTimeOut();
                            ((GraphDivIterableOrderedComposite.GraphDivIterator)newiterator).setTimeOutParam(timeout);
                        }
                    }else{
                        ((GraphDivIterable.GraphDivIterator)newiterator).activateTimeOut();
                        ((GraphDivIterable.GraphDivIterator)newiterator).setTimeOutParam(timeout);
                    }
                }

                memConsumption += useComposite?
                                  (symmetric?
                                  ((GraphDivIterableSymmetricComposite.GraphDivIterator)newiterator).getMaxMemRequirements()
                                  :((GraphDivIterableOrderedComposite.GraphDivIterator)newiterator).getMaxMemRequirements())
                                  :((GraphDivIterable.GraphDivIterator)newiterator).getMaxMemRequirements();
            }else{
                Iterator<NodeT> newiterator = useComposite?
                    new GraphDivExhaustiveComposite<>(q,candidate,R,graph,context,minsum,l,a,b).iterator()
                    :new GraphDivIterable<>(q,candidate,R,graph,minsum).iterator(); /////////////////////////////////////////////////////////////
                iterators.put (candidate,newiterator);
            }
        }
        
        maxMemConsumption = memConsumption;

        /* third, produce the respective candidate sets */
        MaxPQ<ArrayList<NodeT>> result = new MaxPQ<>(cmp);
        int k = candidates.size();
        for (int j=0; j<candidates.size(); ++j) {
            ArrayList<NodeT> S = candidates.get(j);

            if (recursiveSetExpansion) recursion (S,n,iterators.get(S),cmp,result,k,symmetric);
            else{
                int i=0;
                Iterator<NodeT> iterator = iterators.remove(S);

                long variable = useComposite?
                                (symmetric?
                                ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).getMaxMemRequirements()
                                :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).getMaxMemRequirements())
                                :((GraphDivIterable.GraphDivIterator)iterator).getMaxMemRequirements();

                if (iterator==null) throw new RuntimeException("\n!! ERROR - Requested iterator does not exist !!");
                while (iterator.hasNext()) {
                    NodeT s = iterator.next();

                    long newvariable = useComposite?
                                       (symmetric?
                                       ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).getMaxMemRequirements()
                                       :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).getMaxMemRequirements())
                                       :((GraphDivIterable.GraphDivIterator)iterator).getMaxMemRequirements();

                    memConsumption += newvariable - variable;
                    variable = newvariable;

                    if (memConsumption>maxMemConsumption) maxMemConsumption=memConsumption;

                    if (s==null) break;

                    ArrayList<NodeT> candidate;
                    if (expandAllPossibilities) {
                        candidate = new ArrayList<>(S);
                        candidates.add(candidate);
                        candidate.add(s);
/**/
                        if (useCoordinatedSearch) {
                            Iterator<NodeT> newiterator = useComposite?
                                                          (symmetric?
                                                          new GraphDivIterableSymmetricComposite<>(q,candidate,null,graph,context,minsum,l,a,b).iterator()
                                                          :new GraphDivIterableOrderedComposite<>(q,candidate,null,graph,context,minsum,l,a,b).iterator())
                                                          :new GraphDivIterable<>(q,candidate,null,graph,minsum).iterator();
                            iterators.put(candidate, newiterator);
                            
                            memConsumption += useComposite?
                                              (symmetric?
                                              ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).getMaxMemRequirements()
                                              :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).getMaxMemRequirements())
                                              :((GraphDivIterable.GraphDivIterator)iterator).getMaxMemRequirements();
                        }else{
                            Iterator<NodeT> newiterator = useComposite?
                                new GraphDivExhaustiveComposite<>(q,candidate,null,graph,context,minsum,l,a,b).iterator()
                                :new GraphDivIterable<>(q,candidate,null,graph,minsum).iterator(); ///////////////////////////////
                        }

/**                    DEPRECATED ITERATOR INVOCATION
                       iterators.put(candidate,useComposite?
                                ((GraphDivIterableComposite.GraphDivIterator)iterator).expandSetReturn(s)
                                :((GraphDivIterable.GraphDivIterator)iterator).expandSetReturn(s));
**/
                    }else{
                        candidate = S;
                        candidate.add(s);

                        if (useComposite) {
                            //iterators.put(candidate,new GraphDivIterableComposite<>(q,candidate,graph.getNodes(),graph,context,minsum).iterator());
                            if (useCoordinatedSearch) {
                                if (symmetric) ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).expandSet(s);
                                else ((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).expandSet(s);
                            }else ((GraphDivExhaustiveComposite.GraphDivIterator)iterator).expandSet(s);

                            iterators.put(candidate,iterator);
                        }else{
                            //iterators.put(candidate,new GraphDivIterable<>(q,candidate,graph.getNodes(),graph,minsum).iterator());
                            ((GraphDivIterable.GraphDivIterator)iterator).expandSet(s); 
                            iterators.put(candidate,iterator);
                        }
                    }

                    if (candidate.size()==n) {
                        if (result.size()<k) result.insert(candidate);
                        else if (cmp.compare(candidate,result.max())<0) {
                            result.delMax();
                            result.insert(candidate);
                        }
                    }else candidates.add(candidate);

                    if (result.isEmpty() && ++i>=k 
                        || !result.isEmpty() && cmp.compare(candidate,result.max())>=0) 
                        break;

                    if (!expandAllPossibilities) break;
                }
            }
        }

        /* fourth, generate and return output in appropriate form */
        ArrayList<ArrayList<NodeT>> output = new ArrayList<>();
        if (scores!=null) scores.clear();
        int i = 0;
        for (ArrayList<NodeT> set : result) {
            /**/
            //System.out.print ("("+cmp.computeScore(set)+") ");
            System.out.print (++i + ": ");
            for (NodeT s : set) System.out.print (s + " ");
            System.out.println ();
            /**/
            output.add (0,set);
            if (scores!=null)
                scores.add (0,minsum?
                              (symmetric?
                              ((SumDivSetComparatorSymmetric<NodeT>)cmp).computeScore(set)
                              :((SumDivSetComparatorOrdered<NodeT>)cmp).computeScore(set))
                              :(symmetric?
                              ((MaxDivSetComparatorSymmetric<NodeT>)cmp).computeScore(set)
                              :((MaxDivSetComparatorOrdered<NodeT>)cmp).computeScore(set)));
        }

        if (scores!=null && !output.isEmpty()) summedscore += scores.get(0);

        return output;
    }

    private static<NodeT> boolean recursion (ArrayList<NodeT> S,int n,
                                             Iterator<NodeT> iterator,
                                             Comparator<ArrayList<NodeT>> cmp,
                                             MaxPQ<ArrayList<NodeT>> result,int k,
                                             boolean symmetric) {

        if (S.size()==n) {
            if (result.size()<k) {
                result.insert(S);
                return true;
            }else if (cmp.compare(S,result.max())<0) {
                result.delMax();
                result.insert(S);
                return true;
            }else return false;
        }else{
            boolean flag = false;
            if (iterator==null) 
                throw new RuntimeException("\n!! ERROR - Requested iterator is not hashed !!");
            while (iterator.hasNext()) {
                NodeT s = iterator.next();
                if (s==null) break;
                else{
                    ArrayList<NodeT> Snew = new ArrayList<>(S);
                    Snew.add(s);

                    if (useComposite) 
                        if (useCoordinatedSearch) {
                            if (symmetric) ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).expandSet(s);
                            else ((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).expandSet(s);
                        }else ((GraphDivExhaustiveComposite.GraphDivIterator)iterator).expandSet(s);
                    else ((GraphDivIterable.GraphDivIterator)iterator).expandSet(s);

                    if (recursion(Snew,n,iterator,cmp,result,k,symmetric)) flag = true;
                    else break;

                    /* DEPRECATED ITERATOR INVOCATION
                    if (!recursion(Snew,n,useComposite?
                            ((GraphDivIterableComposite.GraphDivIterator)iterator).expandSetReturn(s)
                            :((GraphDivIterable.GraphDivIterator)iterator).expandSetReturn(s),
                            cmp,result,k)) break;
                    else flag = true;
                    */

                    if (!expandAllPossibilities) break;
                }
            }
            return flag;
        }
    }

    public static float summedscore = 0.0f;


    /**
     * PLEASE, DO NOT TOUCH THIS SPECIFIC PARAMETER, 
     * (i)  ITERATORS OPTIMISED FOR THIS CONFIGUARATION, 
     * (ii) THE BENEFIT IN RESULT QUALITY DOES NOT 
     *           JUSTIFY THE COMPUTATIONAL OVERHEAD!
     */
    private static final boolean expandAllPossibilities = false;
    private static final boolean recursiveSetExpansion = false;

    /*
     * NOTE: network weights and similarities should be normalized accordingly, 
     *       as the one should not be exceedingly disproportional to the other.
     */
    private static boolean useComposite = true;


    public static void main (String[] args) {
        if (args.length<5)
            throw new IllegalArgumentException("\n!! ERROR - Should provide a filename to a graph, the size of each result, the number of results, the number of features, and the number of queries to be processed !!");
        String graphfilename = args[0];
        int n = Integer.parseInt (args[1]);
        int k = Integer.parseInt (args[2]);
        int W = Integer.parseInt (args[3]);
        if (W<0) {useComposite=false; W =-W;}
        boolean minsum = false;
        boolean symmetric = true;
        if (n<=0) {n=-n; minsum=true;}
        Graph<Long> graph = new DirectedGraph<>(graphfilename,NumberFormat.getNumberInstance(),true);
        ContentSimilarity<Long> context = new ContentSimilarity<>();
        Collection<Long> vertices = graph.getNodes();
        for (long u : vertices) {
            Map<String,Float> description = new HashMap<>();
            for (int i=0;i<W;++i) description.put ("lemma"+i, (float) Math.random()); // random weights
            context.insertNode(u,description);
        }

        int i=0, Q=Integer.parseInt (args[4]);
        long summedduration = 0L;
        for (long u : vertices) {
            if (i++>=Q) break;

            long start = System.currentTimeMillis();
            compute (u,null,graph,context,k,n,minsum,symmetric,0.5f,1.0f,1.0f,null);
            long duration = System.currentTimeMillis() - start;
            summedduration += duration;

            System.out.println ("** Max memory requirements for query "+u+": "+(getMaxMemRequirements()>>10)+" kBs");
        }
        System.out.println ("-------------------------------------------");
        System.out.println ("** Average score: " + summedscore/Q);
        System.out.println ("** Average duration: " + summedduration/1000.0f/Q + " sec");
    }
}
