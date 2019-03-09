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

import grammar.UndirectedGraph;
import grammar.Graph;
import grammar.MaxPQ;
import grammar.MinPQ;
import oscp.Group;
import omcp.GroupingComparator;
import omcp.GroupingComparatorMinMax;
import omcp.GroupingComparatorMinSum;
import omcp.ProduceNextPhaseGroupings;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class ComputeClusteredGroupings {

    final private static boolean refineBestFirst = false;

    public static int getMaxMemRequirements() {return 0;}
    public static void resetStats () {}

    public static<NodeT> ArrayList<Group<NodeT>> compute (Group<NodeT> grouping,
                                                          Graph<NodeT> graph,
                                                          int k, int M,
                                                          boolean useMinSum) {
        GroupingComparator<NodeT> cmp = useMinSum?
                                        new GroupingComparatorMinSum<> (graph)
                                        :new GroupingComparatorMinMax<> (graph);

        MinPQ<Group<NodeT>> groupings = new MinPQ<> (cmp.BYLOWERBOUND);
        MaxPQ<Group<NodeT>> result = new MaxPQ<> (cmp.BYUPPERBOUND);
        ArrayDeque<Group<NodeT>> seen = new ArrayDeque<>();

        if (grouping.getLambdas().get(grouping) == null) {
            throw new RuntimeException("!! Grouping has nulled association with itself !!");
        }

        if (grouping.getTarget()!=null) {
            groupings.insert (grouping);
            result.insert (grouping);
            seen.add (grouping);
        }else{
            System.out.println("!! Computing meeting locations of targetless groupings !!");
            int i = 0;
            for (NodeT mu : new ConnectingLocationsIterable<>(grouping,graph,null,useMinSum)) {
                if (mu == null) {
                    throw new RuntimeException("!! Targetless meeting location should not be null !!");
                }
                if (i++>=k) break;
                else{
                    Group<NodeT> newgrouping = new Group<> (mu,grouping.getSubgroups(),grouping.getLambdas(),graph,useMinSum);
                    groupings.insert (newgrouping);
                    result.insert (newgrouping);
                    seen.add (newgrouping);
                }
            }
        }

        while (!groupings.isEmpty()) {
            Group<NodeT> top = groupings.delMin();
            if (grouping.getTarget() != top.getTarget()) {
                throw new RuntimeException("!! Produced grouping has invalid target "+top.getTarget()+" instead of "+grouping.getTarget()+" !!");
            }
            if (result.size()>=k && 
               (useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(top)
                    >=((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(result.max())
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(top)
                    >=((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(result.max())))
                        break;

            Collection<Group<NodeT>> newgroupings = k>1?
                    ProduceNextPhaseGroupings.compute(top,graph,cmp,k,M,useMinSum)
                    :ProduceNextPhaseGroupings.compute(top,graph,cmp,M,useMinSum);

            if (newgroupings!=null && !newgroupings.isEmpty()) {
                for (Group<NodeT> newgrouping : newgroupings) {
                    if (!seen.contains(newgrouping)) {
                        if (newgrouping.getLambdas().get(newgrouping) == null) {
                            throw new RuntimeException("!! New grouping has nulled association with itself !!");
                        }

                        seen.add (newgrouping);

                        if (result.size()<k) {
                            groupings.insert (newgrouping);
                            result.insert (newgrouping);
                        }else{
                            if (useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(newgrouping) 
                                <((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(result.max())
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(newgrouping)
                                <((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(result.max()))
                                    groupings.insert (newgrouping);

                            if (useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(newgrouping) 
                                <((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(result.max())
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(newgrouping)
                                <((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(result.max())) {
                                    result.delMax ();
                                    result.insert (newgrouping);
                                }
                        }
                    }
                }
            }
        }
/*
        if (refineBestFirst) refineResultBestFirst (result,cmp,grouping.getTarget(),graph,k,useMinSum);
        else refineResultBreadthFirst (result,cmp,grouping.getTarget(),graph,k,useMinSum);
*/
        ArrayList<Group<NodeT>> resultlist = new ArrayList<>();
        while (!result.isEmpty()) resultlist.add(result.delMax());
        return resultlist;
    }


    private static<NodeT> void refineResultBreadthFirst (MaxPQ<Group<NodeT>> result,
                                                         GroupingComparator<NodeT> cmp,
                                                         NodeT target,
                                                         Graph<NodeT> graph,
                                                         int k,
                                                         boolean useMinSum) {

        MinPQ<Group<NodeT>> resultminheap = new MinPQ<>(cmp.BYUPPERBOUND);
        for (Group<NodeT> resultgroup : result)
            resultminheap.insert (resultgroup);

        while (!resultminheap.isEmpty()) {
            Group<NodeT> resultgroup = resultminheap.delMin();

            ArrayDeque<Group<NodeT>> queue = new ArrayDeque<>();
            queue.add(resultgroup);

            while (!queue.isEmpty()) {
                Group<NodeT> group = queue.remove();
                Group<NodeT> wrapper = new Group<> (target,
                                                    group.getSubgroups(),
                                                    group.getLambdas(),
                                                    graph,useMinSum);
                //wrapper.getLambdas().remove (wrapper,group.getLambdas().get(group));

                int i = 0;
                for (NodeT meetup : new ConnectingLocationsIterable<> (wrapper,graph,null,useMinSum)) {
                    if (i>=result.size()) break;
                    ++i;

                    Group<NodeT> newsubgroup = new Group<> (meetup,
                                                            group.getSubgroups(),
                                                            group.getLambdas(),
                                                            graph,useMinSum);
                    //newsubgroup.getLambdas().remove (newsubgroup,group.getLambdas().get(group));

                    Group<NodeT> newresultgroup = wrapper.clone(); //group==resultgroup?newsubgroup:resultgroup.clone();
                    if (newresultgroup.replace (group,newsubgroup))
                        throw new RuntimeException ("\n!! ERROR - Unable to replace subgroup !!");

                    if (result.size() < k) {
                        result.insert (newresultgroup);
                    }else if (cmp.computeUpperBound(newresultgroup)<cmp.computeUpperBound(result.max())) {
                        result.delMax ();
                        result.insert (newresultgroup);
                    }else break;

                    for (Group<NodeT> subgroup : group.getSubgroups())
                        if (!subgroup.isTrivial())
                            queue.add (subgroup);
                }
            }
        }
    }


    private static<NodeT> void refineResultBestFirst (MaxPQ<Group<NodeT>> result,
                                                      GroupingComparator<NodeT> cmp,
                                                      NodeT target,
                                                      Graph<NodeT> graph,
                                                      int k,
                                                      boolean useMinSum) {

        MinPQ<Group<NodeT>> resultminheap = new MinPQ<>(cmp.BYUPPERBOUND);
        for (Group<NodeT> resultgroup : result)
            resultminheap.insert (resultgroup);

        while (!resultminheap.isEmpty()) {
            MinPQ<Group<NodeT>> pqueue = new MinPQ<>(cmp.BYUPPERBOUND);
            ArrayDeque<Group<NodeT>> queue = new ArrayDeque<>();

            Group<NodeT> resultgroup = resultminheap.delMin();

            if (cmp.computeUpperBound(resultgroup)>=cmp.computeUpperBound(result.max()))
                break;

            pqueue.insert(resultgroup);
            queue.add (resultgroup);
            while (!queue.isEmpty()) {
                Group<NodeT> group = queue.remove();

                for (Group<NodeT> subgroup : group.getSubgroups()) {
                    if (!subgroup.isTrivial()) {
                        Group<NodeT> wrapper = new Group<>(target,graph,useMinSum);
                        wrapper.getLambdas().put (wrapper,subgroup.getLambdas().get(subgroup));
                        wrapper.addSubgroup (subgroup);

                        pqueue.insert (wrapper);
                        queue.add (subgroup);
                    }
                }
            }

            while (!pqueue.isEmpty()) {
                Group<NodeT> top = pqueue.delMin();
                Group<NodeT> group = top==resultgroup?top:top.getSubgroups().get(0);
/*
                Group<NodeT> wrapper = new Group<> (target,
                                                    group.getSubgroups(),
                                                    group.getLambdas(),
                                                    graph,useMinSum);
                //wrapper.getLambdas().put (wrapper,group.getLambdas().get(group));
*/
                int i=1;
                for (NodeT meetup : new ConnectingLocationsIterable<> (group,graph,null,useMinSum)) {
                    if (i>=result.size()) break;
                    ++i;

                    Group<NodeT> newsubgroup = new Group<> (meetup,
                                                            group.getSubgroups(),
                                                            group.getLambdas(),
                                                            graph,useMinSum);
                    //newsubgroup.getLambdas().remove(group);
                    /*
                    Group<NodeT> newresultgroup = group==resultgroup?newsubgroup:resultgroup.clone();
                    if (group!=resultgroup && !newresultgroup.replace (group,newsubgroup))
                        throw new RuntimeException ("\n!! ERROR - Unable to replace subgroup !!");
                    */
                    Group<NodeT> newresultgroup = newsubgroup;
                    //////////////////////////////////////////

                    if (result.size() < k) {
                        result.insert(newresultgroup);
                    }else if (cmp.computeUpperBound(newresultgroup) < cmp.computeUpperBound(result.max())) {
                        result.delMax();
                        result.insert(newresultgroup);
                    }else break;
                }
            }
        }
    }


    public static void main (String[] args) {
        Graph<Long> graph = new UndirectedGraph<>("/Users/gtsat/HomeGrown/data/lifeifei/SF.graph",NumberFormat.getNumberInstance(),true);
        //graph.draw(800,600);

        boolean useMinSum = true;

        long target = 5000L;
        /*
        Long[] sources = {1000L,2000L,3000L,4000L};
        float[][] lambdas = {{.6f,.1f,.1f,.1f},
                             {.1f,.6f,.1f,.1f},
                             {.1f,.1f,.6f,.1f},
                             {.1f,.1f,.1f,.6f}};
        */
        Long[] sources = {1000L,2000L,3000L};
        float[][] lambdas = {{.1f,.45f,.45f}, {.45f,.1f,.45f}, {.45f,.45f,.1f}};
        System.out.println (compute(new Group<>(target,sources,lambdas,graph,useMinSum),graph,5,sources.length-1,useMinSum).size());
   }
}
