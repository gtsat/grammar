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
import java.util.Map;

/*
 * Computes the k most diversified sets of n vertices 
 * around query centre q using hill climbing starting 
 * from an arbitrary initial set (either random or 
 * created using greedy heuristics).
 */

final public class SwapDiversion {

    private static final long swapTimeOut = 15000;
    private static final boolean activateTimeOut = false;

    private static final boolean useCoordinatedSearch = true;

    private static boolean useComposite = true;


    private static long maxMemConsumption = 0L;
    public static long getMaxMemRequirements () {return useCoordinatedSearch?maxMemConsumption:-1;}

    /*
     * NOTE: we need to trace cycles. However, comparing every time 
     * each newly derived set against all the ones that precede it 
     * is expensive, especially when each comprises a large number 
     * of elements. So, let's cut corners and keep track of all the 
     * replacements that led to a specific set. Even though not 
     * entirely correct, when a sequence of replacements is found 
     * more than a certain number of times, a cycle during search 
     * has been traced and we stop refining that particular set.
     */
    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<ArrayList<NodeT>> seeds, ArrayList<NodeT> R, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                                              int k, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b, 
                                                              ArrayList<Float> scores) {

        return compute (q,seeds,R,graph,context,k,Integer.MAX_VALUE,minsum,symmetric,l,a,b,scores,-1);
    }

    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<ArrayList<NodeT>> seeds, ArrayList<NodeT> R, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                                              int k, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b, 
                                                              ArrayList<Float> scores, 
                                                              long timeout) {

        return compute (q,seeds,R,graph,context,k,Integer.MAX_VALUE,minsum,symmetric,l,a,b,scores,timeout);
    }

    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<ArrayList<NodeT>> seeds, ArrayList<NodeT> R, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                                              int k, int n, boolean minsum, boolean symmetric, 
                                                              float l,float a,float b, 
                                                              ArrayList<Float> scores) {

        return compute (q,seeds,R,graph,context,k,n,minsum,symmetric,l,a,b,scores,-1);
    }

    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<ArrayList<NodeT>> compute (NodeT q, Collection<ArrayList<NodeT>> seeds, ArrayList<NodeT> R, 
                                                              Graph<NodeT> graph, ContentSimilarity<NodeT> context,
                                                              int k, int n, boolean minsum, boolean symmetric,
                                                              float l,float a,float b,
                                                              ArrayList<Float> scores,
                                                              long timeout) {

        Comparator<ArrayList<NodeT>> cmp = minsum?
                                                  (symmetric?
                                                  new SumDivSetComparatorSymmetric<>(q,graph,context,l,a,b)
                                                  :new SumDivSetComparatorOrdered<>(q,graph,context,l,a,b))
                                                  :(symmetric?
                                                  new MaxDivSetComparatorSymmetric<>(q,graph,context,l,a,b)
                                                  :new MaxDivSetComparatorOrdered<>(q,graph,context,l,a,b));

        Map<ArrayList<NodeT>,Iterator<NodeT>> iterators = new HashMap<>();
        Map<ArrayList<NodeT>,Map<NodeT,NodeT>> lineages = new HashMap<>();
        MinPQ<ArrayList<NodeT>> subsets = new MinPQ<>(cmp);
        long memConsumption = 0L;
        for (ArrayList<NodeT> S : seeds) {
            for (NodeT s : S.subList(n>S.size()?0:S.size()-n,S.size())) {
                ArrayList<NodeT> subset = new ArrayList<>(S);
                subsets.insert (subset);
                subset.remove (s);

                memConsumption += S.size()*4;

                if (iterators.containsKey(subset)) continue; ////throw new RuntimeException ("\n!! ERROR - Duplicate seed detected !!");

                if (useCoordinatedSearch) {
                    Iterator<NodeT> newiterator = useComposite?
                                                  (symmetric?
                                                    new GraphDivIterableSymmetricComposite<>(q,subset,R,graph,context,minsum,l,a,b).iterator()
                                                    :new GraphDivIterableOrderedComposite<>(q,subset,R,graph,context,minsum,l,a,b).iterator())
                                                  :new GraphDivIterable<>(q,subset,R,graph,minsum).iterator();

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

                    iterators.put (subset,newiterator);

                    memConsumption += 8;
                    memConsumption += useComposite?
                                      (symmetric?
                                      ((GraphDivIterableSymmetricComposite.GraphDivIterator)newiterator).getMaxMemRequirements()
                                      :((GraphDivIterableOrderedComposite.GraphDivIterator)newiterator).getMaxMemRequirements())
                                      :((GraphDivIterable.GraphDivIterator)newiterator).getMaxMemRequirements();
                }else{
                    iterators.put (subset,useComposite?
                                  new GraphDivExhaustiveComposite<>(q,subset,R,graph,context,minsum,l,a,b).iterator()
                                  :new GraphDivIterable<>(q,subset,R,graph,minsum).iterator()); ///////////////////////////////
                }

                Map<NodeT,NodeT> lineage = new HashMap<>();
                lineages.put (subset,lineage);
                lineage.put (s,null);
            }
        }

        MaxPQ<ArrayList<NodeT>> result = new MaxPQ<>(cmp);
        for (ArrayList<NodeT> seed : seeds) {
            if (result.size()<k) result.insert(seed);
            else if (cmp.compare(seed,result.max())<0) {
                result.delMax();
                result.insert(seed);
            }
        }

        maxMemConsumption = memConsumption;

        long variable = 0;
        long start = System.currentTimeMillis();
        while (!subsets.isEmpty() && cmp.compare(subsets.min(),result.max())<0 
               && (!activateTimeOut || System.currentTimeMillis()-start<swapTimeOut)) {

            ArrayList<NodeT> subset = subsets.delMin();
            Map<NodeT,NodeT> lineage = lineages.get (subset);
            Iterator<NodeT> iterator = iterators.remove (subset); /////////////////////////////////////////
            if (iterator==null) throw new RuntimeException ("\n!! ERROR - Unable to retrieve iterator !!");
            assert (lineage!=null);

            memConsumption -= variable; ///////////////////////////////////////////////////////////////////
            variable = useComposite?
                        (symmetric?
                        ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).getMaxMemRequirements()
                        :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).getMaxMemRequirements())
                       :((GraphDivIterable.GraphDivIterator)iterator).getMaxMemRequirements();

            repeat:
            while (iterator.hasNext() && (!activateTimeOut || System.currentTimeMillis()-start<swapTimeOut)) {
                NodeT replacement = iterator.next();

                long newvariable = useComposite?
                                   (symmetric?
                                   ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).getMaxMemRequirements()
                                   :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).getMaxMemRequirements())
                                   :((GraphDivIterable.GraphDivIterator)iterator).getMaxMemRequirements();

                memConsumption += newvariable - variable;
                variable = newvariable;

                if (memConsumption>maxMemConsumption) maxMemConsumption=memConsumption;

                if (replacement==null) break;

                ArrayList<NodeT> augmented = new ArrayList<>(subset);
                augmented.add (replacement);

                memConsumption += augmented.size()*4;

                if (result.size()<k) result.insert(augmented);
                else if (cmp.compare(augmented,result.max())<0) {
                    result.delMax();
                    result.insert(augmented);
                }else break;

                /* be a little greedy, just process the top-k derived subsets */
                MaxPQ<ArrayList<NodeT>> newsubsets = new MaxPQ<>(cmp);
                Map<ArrayList<NodeT>,NodeT> substitutes = new HashMap<>();
                for (NodeT s : subset.subList(n>subset.size()?0:subset.size()-n,subset.size())) {
                    ArrayList<NodeT> newsubset = new ArrayList<>(augmented);
                    newsubset.remove(s);

                    memConsumption += newsubset.size()*4;

                    Map<NodeT,NodeT> newlineage = new HashMap<>(lineage);
                    lineages.put (newsubset,newlineage);
                    newlineage.put (s,lineage.get(replacement));


                    if (newsubsets.size()<k) {
                        newsubsets.insert(newsubset);
                        substitutes.put(newsubset,s);
                    }else if (cmp.compare(newsubset,newsubsets.max())<0) {
                        newsubsets.delMax();
                        newsubsets.insert(newsubset);
                        substitutes.put(newsubset,s);
                    }
                }

                for(ArrayList<NodeT> newsubset : newsubsets) {
                    if (cmp.compare(newsubset,subset)<0) {
                        if (result.size()<k || cmp.compare(newsubset,result.max())<0) {
                            NodeT s = substitutes.get(newsubset);

/* USE THIS WHEN ONLY BEST DERIVED SUBSET IS FURTHER PROCESSED
                            if (useComposite) ((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).replaceSet(sbest,replacement);
                            else  ((GraphDivIterable.GraphDivIterator)iterator).replaceSet(sbest,replacement);
                            iterators.put (newsubset,iterator);
*/
                            /** ITERATOR CONSTRUCTION FROM SCRATCH FOLLOWING, AND HENCE SLOWER *
                            iterators.put (newsubset, useComposite?
                                    new GraphDivIterableOrderedComposite<>(q,newsubset,R,graph,context,minsum).iterator()
                                    :new GraphDivIterable<>(q,newsubset,R,graph,minsum).iterator());
                            **/
                            if (useCoordinatedSearch) {
                                if (iterators.containsKey(newsubset)) continue; ////throw new RuntimeException ("\n!! ERROR - Duplicate subset generated !!");

                                Iterator<NodeT> newiterator = useComposite?
                                                             (symmetric?
                                                             ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).replaceSetReturn(s,replacement)
                                                             :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).replaceSetReturn(s,replacement))
                                                             :((GraphDivIterable.GraphDivIterator)iterator).replaceSetReturn(s,replacement);

                                iterators.put (newsubset,newiterator);

                                memConsumption += 8;
                                memConsumption += useComposite?
                                                  (symmetric?
                                                  ((GraphDivIterableSymmetricComposite.GraphDivIterator)iterator).getMaxMemRequirements()
                                                  :((GraphDivIterableOrderedComposite.GraphDivIterator)iterator).getMaxMemRequirements())
                                                  :((GraphDivIterable.GraphDivIterator)iterator).getMaxMemRequirements();
                            }else{
                                iterators.put (newsubset,useComposite?
                                    ((GraphDivExhaustiveComposite.GraphDivIterator)iterator).replaceSetReturn(s,replacement)
                                    :((GraphDivIterable.GraphDivIterator)iterator).replaceSetReturn(s,replacement)); ///////////////////////
                            }
                            /**/
                            subsets.insert(newsubset);
                        }
                    }
                }
            }
        }

        /* keep the best set only */
        while (result.size()>1)
            result.delMax();

        int i = 0;
        if (scores!=null) scores.clear();
        ArrayList<ArrayList<NodeT>> output = new ArrayList<>();
        for (ArrayList<NodeT> set : result) {
            //summedscore += cmp.computeScore(set);
            /**
            System.out.print ("("+cmp.computeScore(set)+") ");
            System.out.print (++i + ": ");
            for (NodeT s : set) System.out.print (s + " ");
            System.out.println ();
            **/
            output.add (0,set);
            if (scores!=null)
                scores.add (0,minsum?(
                              symmetric?
                              ((SumDivSetComparatorSymmetric)cmp).computeScore(set)
                              :((SumDivSetComparatorOrdered)cmp).computeScore(set))
                              :symmetric?
                              ((MaxDivSetComparatorSymmetric)cmp).computeScore(set)
                              :((MaxDivSetComparatorOrdered)cmp).computeScore(set));
        }
        return output;
    }

    static public float summedscore = 0.0f;

    /*
     * The pipeline of the greedy scheme followed by the interchange algorithm. 
     * Seeding the interchange algorithm with many instances so as to produce 
     * even more outputs and opt for the best to return in a Monte Carlo fashion 
     * to approach the true optimal set. A parallelizable process quite similar 
     * to performing many searches from random points towards an unknown solution.
     */
    public static void main (String[] args) {
        if (args.length<6) 
            throw new IllegalArgumentException("\n!! ERROR - Should provide a filename to a graph, the size of each result, the number of seeds, and the max heap-size of the interchange algorithm !!");
        String graphfilename = args[0];
        int n = Integer.parseInt (args[1]);
        int k = Integer.parseInt (args[2]);
        int l = Integer.parseInt (args[3]);
        int W = Integer.parseInt (args[4]);
        if (W<0) {useComposite=false; W =-W;}
        boolean minsum = false;
        boolean symmetric = true;
        if (n<=0) {n=-n; minsum=true;}
        DirectedGraph<Long> graph = new DirectedGraph<>(graphfilename,NumberFormat.getNumberInstance(),true);
        ContentSimilarity<Long> context = new ContentSimilarity<>();
        Collection<Long> vertices = graph.getNodes();
        for (long u : vertices) {
            Map<String,Float> description = new HashMap<>();
            for (int i=0;i<W;++i) description.put ("lemma"+i, (float) Math.random()); // random weights
            context.insertNode(u,description);
        }

        int i=0, Q=Integer.parseInt (args[5]);
        long summeddurationgreedy = 0L;
        long summeddurationtotal = 0L;
        for (long u : vertices) {
            if (i++>=Q) break;
            long start = System.currentTimeMillis();
            Collection<ArrayList<Long>> seeds = GreedyDiversion.compute(u,null,graph,context,k,n,minsum,symmetric,0.5f,1.0f,1.0f,null);
            long durationgreedy = System.currentTimeMillis() - start;
            compute (u,seeds,null,graph,context,l,minsum,symmetric,0.5f,1.0f,1.0f,null);
            long durationtotal = System.currentTimeMillis() - start;

            summeddurationgreedy += durationgreedy;
            summeddurationtotal += durationtotal;

            System.out.println ("** Max memory requirements for query "+u+": "+(getMaxMemRequirements()>>10)+" kBs");
        }
        System.out.println ("-------------------------------------------");
        System.out.println ("** Average greedy score: " + GreedyDiversion.summedscore/Q);
        System.out.println ("** Average final score: " + summedscore/Q);
        System.out.println ("** Average seeding duration: " + summeddurationgreedy/1000.0f/Q + " sec");
        System.out.println ("** Average total duration: " + summeddurationtotal/1000.0f/Q + " sec");
    }
}
