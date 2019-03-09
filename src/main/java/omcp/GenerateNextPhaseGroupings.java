/**
 * The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit 
 * Copyright (C) 2015 George Tsatsanifos <gtsatsanifos@gmail.com>
 *
 * The GRA.M.MA.R. toolkit is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package omcp;

import oscp.Group;
import grammar.MaxPQ;
import grammar.Graph;
import grammar.UndirectedGraph;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

final class GenerateNextPhaseGroupings {
    public static<NodeT> Collection<Group<NodeT>> computeXXX (Group<NodeT> grouping, Graph<NodeT> graph, GroupingComparator<NodeT> cmp) {
        if (!grouping.isValidGrouping()) throw new RuntimeException("\n!! ERROR - Input not a valid grouping !!");

        ArrayDeque<Group<NodeT>> pool = new ArrayDeque<>();
        MaxPQ<Group<NodeT>> processed = new MaxPQ<>(cmp.BYLOWERBOUND);
        processed.insert(grouping);
        pool.add(grouping);

        float threshold = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                             ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(grouping)
                             :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(grouping);

        while (!pool.isEmpty()) {
            Group<NodeT> head = pool.remove();
            for (Group<NodeT> over : head.getSubgroups()) {
                for (Group<NodeT> under : head.getSubgroups()) {
                    if (over==under) break;
                    else{
                        {
                            Group<NodeT> mhead = head.mergeSubgroups (over,under);
                            if (mhead.numberTravelers()!=grouping.numberTravelers())
                                throw new RuntimeException("\n!! ERROR - Derived invalid grouping by merging !!");

                            float lbound = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                            ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(mhead)
                                            :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(mhead);

                            if (lbound<=threshold && !processed.contains(mhead)) {
                                float ubound = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                                ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(mhead)
                                                :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(mhead);

                                if (ubound < threshold) {
                                    threshold = ubound;
                                    while (!processed.isEmpty()) {
                                        if (cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                                ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(processed.max())<=threshold
                                                :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(processed.max())<=threshold)
                                                    break;
                                        processed.delMax();
                                    }
                                }
                                processed.insert(mhead);
                                pool.add(mhead);
                            }
                        }
                        if (!over.isTrivial() || !under.isTrivial()) {
                            Group<NodeT> jhead = head.joinSubgroups (over,under);
                            if (jhead.numberTravelers()!=grouping.numberTravelers())
                                throw new RuntimeException("\n!! ERROR - Derived invalid grouping by joining !!");

                            float lbound = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                            ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(jhead)
                                            :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(jhead);

                            if (lbound<=threshold && !processed.contains(jhead)) {
                                float ubound = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                                ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(jhead)
                                                :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(jhead);

                                if (ubound < threshold) {
                                    threshold = ubound;
                                    while (!processed.isEmpty()) {
                                        if (cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                                ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(processed.max())<=threshold
                                                :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(processed.max())<=threshold)
                                                    break;
                                        processed.delMax();
                                    }
                                }
                                processed.insert(jhead);
                                pool.add(jhead);
                            }
                        }
                    }
                }
            }
        }
        ArrayDeque<Group<NodeT>> nextphase = new ArrayDeque<>();
        for (Group<NodeT> g : processed)
            if (grouping!=g)
                nextphase.add(g);
        return nextphase;
    }

    public static<NodeT> Collection<Group<NodeT>> compute (Group<NodeT> grouping, Graph<NodeT> graph, GroupingComparator<NodeT> cmp) {
        if (!grouping.isValidGrouping()) throw new RuntimeException("\n!! ERROR - Input not a valid grouping !!");

        ArrayDeque<Group<NodeT>> pool = new ArrayDeque<>();
        ArrayDeque<Group<NodeT>> queue = new ArrayDeque<>();
        Stack<Group<NodeT>> congregator = new Stack<>();

        queue.addAll(grouping.getSubgroups());
        congregator.addAll(grouping.getSubgroups());

        int numberDerivedGroups;
        while (!queue.isEmpty()) {
            Group<NodeT> over = queue.remove();

            Group<NodeT> overWrapper = new Group<>(grouping.getTarget(),new ArrayList<Group<NodeT>>(),new HashMap<Group<NodeT>,Float>(),graph,cmp.getClass().equals(GroupingComparatorMinSum.class));
            overWrapper.addSubgroup(over);
            float tauOver = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                             ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(overWrapper)
                             :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(overWrapper);

            for (Group<NodeT> under : queue) {
                if (!over.isDisjoint(under)) continue;
                //if (!under.isDisjoint(over)) continue;
                //if (over.contains(under)) continue;
                //if (under.contains(over)) continue;

                Group<NodeT> underWrapper = new Group<>(grouping.getTarget(),new ArrayList<Group<NodeT>>(),new HashMap<Group<NodeT>,Float>(),graph,cmp.getClass().equals(GroupingComparatorMinSum.class));
                underWrapper.addSubgroup(under);
                float tauUnder = (cmp.getClass().equals(GroupingComparatorMinSum.class))?
                                  ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(underWrapper)
                                  :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(underWrapper);

                Group<NodeT> merged = over.mergeWith(under,grouping.getTarget());
                if (!merged.getTarget().equals(grouping.getTarget()) 
                        && !congregator.contains(merged)) {

                    Group<NodeT> mergedWrapper = new Group<>(grouping.getTarget(),new ArrayList<Group<NodeT>>(),new HashMap<Group<NodeT>,Float>(),graph,cmp.getClass().equals(GroupingComparatorMinSum.class));
                    mergedWrapper.addSubgroup(merged);
                    float tauNew = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                                     ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(mergedWrapper)
                                     :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(mergedWrapper);

                    //if (cmp.getClass().equals(GroupingComparatorMinSum.class)?tauNew<tauOver+tauUnder:tauNew<tauOver&&tauNew<tauUnder) {
/**/
                    {
                        System.out.println ("** [GenerateNextPhaseGroupings] Added element "+(congregator.size()+pool.size())
                                //+" by combining elements "+i+" and "+j+" into a group of " 
                                +" by combining elements "+over.hashCode()+" and "+under.hashCode()+" into group "
                                +merged.hashCode()+" of "+merged.getSubgroups().size()+" elements.\t");
/**/
                        for (Group<NodeT> element : pool) // queue
                            element.getLambdas().put(merged,element.getLambdas().get(over)*element.getLambdas().get(under));

                        for (Group<NodeT> element : congregator)
                            element.getLambdas().put(merged,element.getLambdas().get(over)*element.getLambdas().get(under));

                        congregator.push(merged);
                        pool.addLast (merged);
                    }
                }

                Group<NodeT> joined = over.joinWith (under,grouping.getTarget());
                //if (!joined.getTarget().equals(grouping.getTarget()) 
                if (!congregator.contains(joined)) {
                    for (Group<NodeT> element : pool) // queue
                        element.getLambdas().put(joined,element.getLambdas().get(over)*element.getLambdas().get(under));

                    for (Group<NodeT> element : congregator)
                        element.getLambdas().put(joined,element.getLambdas().get(over)*element.getLambdas().get(under));

                    //congregator.push(joined);
                    //pool.addLast (joined);
                }
            }

            //System.out.println("** [GenerateNextPhaseGroupings] Temp pool size: "+pool.size());
            for (Group<NodeT> outer : pool) {
                for (Group<NodeT> inner : pool) {
                    if (outer!=inner) {
                        float newlambda = 1.0f;
                        for (Group<NodeT> innersubgroup : inner.getTravelers())
                            if (outer.getLambdas().containsKey(innersubgroup)) newlambda *= outer.getLambdas().get(innersubgroup);
                            else throw new RuntimeException("\n!! ERROR - Cannot update lambdas for not associated groups !!");
                        outer.getLambdas().put(inner,newlambda);
                    }
                }
            }
            //numberDerivedGroups += pool.size();
            //congregator.addAll(pool);
            queue.addAll(pool);
            pool.clear();
        }

        numberDerivedGroups = congregator.size() - grouping.getSubgroups().size();
        if (numberDerivedGroups > 0) {
            GroupingTree<NodeT> tree = new GroupingTree<>(graph);
            for (int j=0; !congregator.isEmpty(); ++j)
                tree.addGroup (congregator.pop(), j<numberDerivedGroups);

            Collection<Group<NodeT>> newgroupings = tree.produceGroupings(grouping.getTarget());

            //System.out.println ("** [GenerateNextPhaseGroupings] Grouping-tree size: "+tree.size());

            float threshold = Float.MAX_VALUE;
            for (Group<NodeT> newgrouping : newgroupings) {
                if (newgrouping.numberTravelers()!=grouping.numberTravelers())
                    throw new RuntimeException ("\n!! ERROR - Descrepancy in the number of travelers of the generated group !!");

                if (!newgrouping.equals(grouping)) {
                    float groupscore = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                        ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(newgrouping)
                        :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(newgrouping);
                    if (groupscore < threshold) threshold = groupscore;
                }
            }

            ArrayDeque<Group<NodeT>> qualified = new ArrayDeque<>();
            for (Group<NodeT> newgrouping : newgroupings) {
                float groupbound = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(newgrouping)
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(newgrouping);

                float groupscore = cmp.getClass().equals(GroupingComparatorMinSum.class)?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(newgrouping)
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(newgrouping);
                if (!newgrouping.equals(grouping)) { //groupbound<=threshold) {
                    qualified.add(newgrouping);
                    System.out.println("** [GenerateNextPhaseGroupings] Including grouping "+newgrouping.hashCode()
                        +" of "+newgrouping.getSubgroups().size()+" elements with lower bound "+groupbound+" and score "+groupscore);
                }else{
                    System.out.println("** [GenerateNextPhaseGroupings] Filtering OUT grouping "+newgrouping.hashCode()
                        +" of "+newgrouping.getSubgroups().size()+" elements with lower bound  "+groupbound+" and score "+groupscore
                        +" for surpassing the threshold value "+threshold);
                }
            }
            return qualified;
        }else return null; // otherwise no next phase!
    }

    public static void main (String[] args) {
        Graph<Long> graph = new UndirectedGraph<Long>("/home/gtsat/roadnet/data/lifeifei/OL.graph",NumberFormat.getNumberInstance(),true).kruskal();
        //graph.draw(800,600);

        long target = 4500L;
        Long[] sources = {1000L,1500L,2000L,3000L};
        float[][] lambdas = {{.6f,.1f,.1f,.1f},
                             {.1f,.6f,.1f,.1f},
                             {.1f,.1f,.6f,.1f},
                             {.1f,.1f,.1f,.6f}};

        //for (Group<Long> derived : compute(new Group<>(target,sources,lambdas,false),graph,new GroupingComparatorMinMax<>(graph)))
        //    System.out.println (derived+"\n\n\n");
        System.out.println(compute(new Group<>(target,sources,lambdas,graph,false),graph,new GroupingComparatorMinMax<>(graph)).size());
    }
}
