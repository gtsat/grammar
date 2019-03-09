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

package omcp;

import oscp.Group;
import grammar.Graph;
import grammar.UndirectedGraph;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public class ProduceNextPhaseGroupings {

    public static<NodeT> Collection<Group<NodeT>> compute (Group<NodeT> grouping, Graph<NodeT> graph, 
                                                           GroupingComparator<NodeT> cmp,
                                                           boolean minsum) {
        return compute (grouping,graph,cmp,Integer.MAX_VALUE,minsum);
    }

    public static<NodeT> Collection<Group<NodeT>> compute (Group<NodeT> grouping, Graph<NodeT> graph, 
                                                           GroupingComparator<NodeT> cmp,
                                                           int M, boolean minsum) {

        ArrayList<Group<NodeT>> produced = new ArrayList<>();
        int head=0, tail=0;

        for (Group<NodeT> group : grouping.getSubgroups()) {
            produced.add (group);
            ++tail;
        }

        while (head<tail) {
            Group<NodeT> over = produced.get (head++);
            Group<NodeT> overWrapper = new Group<>(grouping.getTarget(),new ArrayList<Group<NodeT>>(),new HashMap<Group<NodeT>,Float>(),graph,cmp.getClass().equals(GroupingComparatorMinSum.class));
            overWrapper.addSubgroup(over);
            float tauOver = cmp instanceof GroupingComparatorMinSum<?>?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(overWrapper)
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(overWrapper);

            for (int i=head; i<tail; ++i) {
                Group<NodeT> under = produced.get (i);
                if (over.isDisjoint(under)) {
                    Group<NodeT> underWrapper = new Group<>(grouping.getTarget(),new ArrayList<Group<NodeT>>(),new HashMap<Group<NodeT>,Float>(),graph,cmp.getClass().equals(GroupingComparatorMinSum.class));
                    underWrapper.addSubgroup(under);
                    float tauUnder = cmp instanceof GroupingComparatorMinSum<?>?
                            ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(underWrapper)
                            :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(underWrapper);

                    Group<NodeT> joined = null;
                    Group<NodeT> merged = null;
                    float tauJoined = Float.MAX_VALUE;
                    float tauMerged = Float.MAX_VALUE;

                    if (over.getMeetupsNumber() + under.getMeetupsNumber() < M) {
                        joined = over.joinWith (under,grouping.getTarget());

                        Group<NodeT> joinedWrapper = new Group<>(grouping.getTarget(),
                                                            new ArrayList<Group<NodeT>>(),
                                                            new HashMap<Group<NodeT>,Float>(),
                                                            graph,
                                                            cmp.getClass().equals(GroupingComparatorMinSum.class));
                        joinedWrapper.addSubgroup(joined);
                        tauJoined = cmp instanceof GroupingComparatorMinSum<?>?
                                           ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(joinedWrapper)
                                           :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(joinedWrapper);


                        if ((!over.isTrivial() || !under.isTrivial()) && over.getMeetupsNumber() + under.getMeetupsNumber() - 1 <= M) {
                            merged = over.mergeWith(under,grouping.getTarget());

                            Group<NodeT> mergedWrapper = new Group<>(grouping.getTarget(),
                                                                     new ArrayList<Group<NodeT>>(),
                                                                     new HashMap<Group<NodeT>,Float>(),
                                                                     graph,
                                                                     cmp.getClass().equals(GroupingComparatorMinSum.class));
                            mergedWrapper.addSubgroup(merged);

                            tauMerged = merged.getMeetupsNumber()>M?
                                        Float.MAX_VALUE
                                        :cmp instanceof GroupingComparatorMinSum<?>?
                                            ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(mergedWrapper)
                                            :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(mergedWrapper);
                        }
                    }

                    if (joined!=null && tauJoined<=tauMerged &&
                            (cmp instanceof GroupingComparatorMinSum<?> && tauJoined<tauOver+tauUnder 
                            || cmp instanceof GroupingComparatorMinMax<?> && tauJoined<tauOver && tauJoined<tauUnder)) {

                        boolean isNewGrouping = true;
                        /**/
                        for (int j=tail-1;j>=0;--j) {
                            if (joined.equals(produced.get(j))) {
                                isNewGrouping = false;
                                break;
                            }
                        }
                        /**/

                        if (isNewGrouping) {
                            produced.add (joined);
                            /*
                            for (int j=0; j<tail; ++j) {
                                Group<NodeT> tempgroup = (Group<NodeT>) produced [j];
                                if (tempgroup.getLambdas().containsKey(over) && tempgroup.getLambdas().containsKey(under))
                                    tempgroup.getLambdas().put (joined,tempgroup.getLambdas().get(over)*tempgroup.getLambdas().get(under));
                            }
                            */
                            ++tail;
                        }
                    }else if (merged!=null
                            && (cmp instanceof GroupingComparatorMinSum<?> && tauMerged<tauOver+tauUnder 
                            || cmp instanceof GroupingComparatorMinMax<?> && tauMerged<tauOver && tauMerged<tauUnder)) {

                        boolean isNewGrouping = true;
                        /**/
                        for (int j=tail-1;j>=0;--j) {
                            if (merged.equals(produced.get(j))) {
                                isNewGrouping = false;
                                break;
                            }
                        }
                        /**/

                        if (isNewGrouping) {
                            produced.add (merged);
                            /*
                            for (int j=0; j<tail; ++j) {
                                Group<NodeT> tempgroup = (Group<NodeT>) produced [j];
                                if (tempgroup.getLambdas().containsKey(over) && tempgroup.getLambdas().containsKey(under))
                                    tempgroup.getLambdas().put (merged,tempgroup.getLambdas().get(over)*tempgroup.getLambdas().get(under));
                            }
                            */
                            ++tail;
                        }
                    }
                }
            }
        }

        if (tail>grouping.getSubgroups().size()) {
            GroupingTree<NodeT> tree = new GroupingTree<>(graph);

            for (int i=tail-1;i>=0;--i)
                tree.addGroup ((Group<NodeT>)produced.get(i), i>=grouping.getSubgroups().size(), M);

            float threshold = Float.MAX_VALUE;
            Collection<Group<NodeT>> newgroupings = tree.produceGroupings (grouping.getTarget());
            for (Group<NodeT> newgrouping : newgroupings) {
                if (!grouping.equals(newgrouping)) {
                    float tauNew = cmp instanceof GroupingComparatorMinSum<?>?
                                ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(newgrouping)
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(newgrouping);
                    if (tauNew<threshold)
                        threshold = tauNew;
                }
            }

            ArrayDeque<Group<NodeT>> qualified = new ArrayDeque<>();
            for (Group<NodeT> newgrouping : newgroupings) {
                if ((cmp instanceof GroupingComparatorMinSum<?>?
                                ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(newgrouping)
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(newgrouping))<=threshold 
                                && !newgrouping.equals(grouping)) {
                    if (minsum) {
                        float sumprod = 0.0f;
                        for (Group<NodeT> over : newgrouping.getSubgroups()) {
                            float product = 1.0f;
                            for (Group<NodeT> under : newgrouping.getSubgroups())
                                if (!over.equals(under) && over.getLambdas().containsKey(under))
                                    product *= over.getLambdas().get(under);
                            sumprod += product;
                        }
                        newgrouping.getLambdas().put (newgrouping,sumprod);
                    }else{
                        /**
                         * Add min-max code here...
                         */

                    }
                    qualified.add(newgrouping);
                }
            }
            return qualified;
        }
        return null;
    }


    public static<NodeT> Collection<Group<NodeT>> compute (Group<NodeT> grouping, 
                                                           Graph<NodeT> graph, 
                                                           GroupingComparator<NodeT> cmp, 
                                                           int k, int M,
                                                           boolean minsum) {
        ArrayList<Group<NodeT>> produced = new ArrayList<>();
        int head=0, tail=0;

        for (Group<NodeT> group : grouping.getSubgroups()) {
            produced.add (group);
            ++tail;
        }

        while (head<tail) {
            Group<NodeT> over = produced.get(head++);
            Group<NodeT> overWrapper = new Group<>(grouping.getTarget(),new ArrayList<Group<NodeT>>(),new HashMap<Group<NodeT>,Float>(),graph,cmp.getClass().equals(GroupingComparatorMinSum.class));
            overWrapper.addSubgroup(over);
            float tauOver = cmp instanceof GroupingComparatorMinSum<?>?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(overWrapper)
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(overWrapper);
            for (int i=head; i<tail; ++i) {
                Group<NodeT> under = produced.get(i);
                if (over.isDisjoint(under)) {
                    Group<NodeT> underWrapper = new Group<>(grouping.getTarget(), new ArrayList<Group<NodeT>>(), new HashMap<Group<NodeT>, Float>(), graph, cmp.getClass().equals(GroupingComparatorMinSum.class));
                    underWrapper.addSubgroup(under);
                    float tauUnder = cmp instanceof GroupingComparatorMinSum<?> ?
                            ((GroupingComparatorMinSum<NodeT>) cmp).computeUpperBound(underWrapper)
                            : ((GroupingComparatorMinMax<NodeT>) cmp).computeUpperBound(underWrapper);


                    ArrayList<Group<NodeT>> joinedGroups = over.joinWith(under, grouping.getTarget(), k);
                    ArrayList<Group<NodeT>> mergedGroups = over.mergeWith(under, grouping.getTarget(), k);

                    int j = over.getMeetupsNumber() + under.getMeetupsNumber() < M ? 0 : Integer.MAX_VALUE;
                    int m = over.getMeetupsNumber() + under.getMeetupsNumber() - 1 <= M ? 0 : Integer.MAX_VALUE;
                    ;

                    //for (int timestamp=tail; tail<timestamp+k;) {
                    while (true) {
                        if (j < joinedGroups.size() && m < mergedGroups.size()) {
                            Group<NodeT> joinedWrapper = new Group<>(grouping.getTarget(),
                                    new ArrayList<Group<NodeT>>(),
                                    new HashMap<Group<NodeT>, Float>(),
                                    graph,
                                    cmp.getClass().equals(GroupingComparatorMinSum.class));

                            joinedWrapper.addSubgroup(joinedGroups.get(j));

                            float tauJoined = cmp instanceof GroupingComparatorMinSum<?> ?
                                    ((GroupingComparatorMinSum<NodeT>) cmp).computeUpperBound(joinedWrapper)
                                    : ((GroupingComparatorMinMax<NodeT>) cmp).computeUpperBound(joinedWrapper);
/*
                            if (!over.isTrivial() || !under.isTrivial()) {
                                m = mergedGroups.size();
                                continue;
                            }
*/
                            Group<NodeT> mergedWrapper = new Group<>(grouping.getTarget(),
                                    new ArrayList<Group<NodeT>>(),
                                    new HashMap<Group<NodeT>, Float>(),
                                    graph,
                                    cmp.getClass().equals(GroupingComparatorMinSum.class));

                            mergedWrapper.addSubgroup(mergedGroups.get(m));

                            float tauMerged = cmp instanceof GroupingComparatorMinSum<?> ?
                                    ((GroupingComparatorMinSum<NodeT>) cmp).computeUpperBound(mergedWrapper)
                                    : ((GroupingComparatorMinMax<NodeT>) cmp).computeUpperBound(mergedWrapper);


                            if (tauJoined <= tauMerged) {
                                if (cmp instanceof GroupingComparatorMinSum<?>
                                        && tauJoined < tauOver + tauUnder
                                        || !(cmp instanceof GroupingComparatorMinSum<?>)
                                        && tauJoined < tauOver && tauJoined < tauUnder) {

                                    boolean isNewGrouping = true;
                                    for (int x = tail - 1; x >= 0; --x) {
                                        if (produced.get(x).equals(joinedGroups.get(j))) {
                                            isNewGrouping = false;
                                            break;
                                        }
                                    }
                                    if (isNewGrouping) {
                                        produced.add(joinedGroups.get(j));
                                        ++tail;
                                    }
                                }
                                ++j;
                            } else {
                                if (cmp instanceof GroupingComparatorMinSum<?>
                                        && tauMerged < tauOver + tauUnder
                                        || cmp instanceof GroupingComparatorMinMax<?>
                                        && tauMerged < tauOver && tauMerged < tauUnder) {

                                    boolean isNewGrouping = true;
                                    for (int x = tail - 1; x >= 0; --x) {
                                        if (produced.get(x).equals(mergedGroups.get(m))) {
                                            isNewGrouping = false;
                                            m = mergedGroups.size();
                                            break;
                                        }
                                    }
                                    if (isNewGrouping) {
                                        produced.add(mergedGroups.get(m));
                                        tail++;
                                    }
                                }
                                ++m;
                            }
                        } else if (j < joinedGroups.size()) {
                            Group<NodeT> joinedWrapper = new Group<>(grouping.getTarget(),
                                    new ArrayList<Group<NodeT>>(),
                                    new HashMap<Group<NodeT>, Float>(),
                                    graph,
                                    cmp.getClass().equals(GroupingComparatorMinSum.class));

                            joinedWrapper.addSubgroup(joinedGroups.get(j));

                            float tauJoined = cmp instanceof GroupingComparatorMinSum<?> ?
                                    ((GroupingComparatorMinSum<NodeT>) cmp).computeUpperBound(joinedWrapper)
                                    : ((GroupingComparatorMinMax<NodeT>) cmp).computeUpperBound(joinedWrapper);

                            if (cmp instanceof GroupingComparatorMinSum<?>
                                    && tauJoined < tauOver + tauUnder
                                    || cmp instanceof GroupingComparatorMinMax<?>
                                    && tauJoined < tauOver && tauJoined < tauUnder) {

                                boolean isNewGrouping = true;
                                for (int x = tail - 1; x >= 0; --x) {
                                    if (produced.get(x).equals(joinedGroups.get(j))) {
                                        isNewGrouping = false;
                                        break;
                                    }
                                }
                                if (isNewGrouping) {
                                    produced.add(joinedGroups.get(j));
                                    ++tail;
                                }
                            }
                            ++j;
                        } else if (m < mergedGroups.size()) {
                            Group<NodeT> mergedWrapper = new Group<>(grouping.getTarget(),
                                    new ArrayList<Group<NodeT>>(),
                                    new HashMap<Group<NodeT>, Float>(),
                                    graph,
                                    cmp.getClass().equals(GroupingComparatorMinSum.class));

                            mergedWrapper.addSubgroup(mergedGroups.get(m));

                            float tauMerged = cmp instanceof GroupingComparatorMinSum<?> ?
                                    ((GroupingComparatorMinSum<NodeT>) cmp).computeUpperBound(mergedWrapper)
                                    : ((GroupingComparatorMinMax<NodeT>) cmp).computeUpperBound(mergedWrapper);


                            if (cmp instanceof GroupingComparatorMinSum<?>
                                    && tauMerged < tauOver + tauUnder
                                    || cmp instanceof GroupingComparatorMinMax<?>
                                    && tauMerged < tauOver && tauMerged < tauUnder) {

                                boolean isNewGrouping = true;
                                for (int x = tail - 1; x >= 0; --x) {
                                    if (produced.get(x).equals(mergedGroups.get(m))) {
                                        isNewGrouping = false;
                                        m = mergedGroups.size();
                                        break;
                                    }
                                }
                                if (isNewGrouping) {
                                    produced.add(mergedGroups.get(m));
                                    ++tail;
                                }
                            }
                            ++m;
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        if (tail>grouping.getSubgroups().size()) {
            GroupingTree<NodeT> tree = new GroupingTree<>(graph);
            for (int i=tail-1;i>=0;--i)
                tree.addGroup (produced.get(i),i>=grouping.getSubgroups().size(),M);

            float threshold = Float.MAX_VALUE;
            Collection<Group<NodeT>> newgroupings = tree.produceGroupings (grouping.getTarget());
            for (Group<NodeT> newgrouping : newgroupings) {
                if (!grouping.equals(newgrouping)) {
                    float tauNew = cmp instanceof GroupingComparatorMinSum<?>?
                                ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(newgrouping)
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(newgrouping);
                    if (tauNew<threshold)
                        threshold = tauNew;
                }
            }

            ArrayDeque<Group<NodeT>> qualified = new ArrayDeque<>();
            for (Group<NodeT> newgrouping : newgroupings) {
                if ((cmp instanceof GroupingComparatorMinSum<?>?
                                ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(newgrouping)
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(newgrouping))<=threshold
                                && !newgrouping.equals(grouping)) {
                    if (minsum) {
                        float sumprod = 0.0f;
                        for (Group<NodeT> over : newgrouping.getSubgroups()) {
                            float product = 1.0f;
                            for (Group<NodeT> under : newgrouping.getSubgroups())
                                if (!over.equals(under) && over.getLambdas().containsKey(under))
                                    product *= over.getLambdas().get(under);
                            sumprod += product;
                        }
                        newgrouping.getLambdas().put (newgrouping,sumprod);
                    }else{
                        /**
                         * Add min-max code here...
                         */

                    }
                    qualified.add(newgrouping);
                }
            }
            return qualified;
        }else return null;
    }



    public static void main (String[] args) {
        Graph<Long> graph = new UndirectedGraph<>("/home/gtsat/roadnet/data/lifeifei/OL.graph",NumberFormat.getNumberInstance(),true);
        //graph.draw(800,600);

        boolean minsum = true;
        long target = 4500L;
        Long[] sources = {1000L,2000L,3000L,4000L};
        float[][] lambdas = {{.8f,.1f,.1f,.1f},
                              {.1f,.8f,.1f,.1f},
                              {.1f,.1f,.8f,.1f},
                              {.1f,.1f,.1f,.8f}};

        //for (Group<Long> derived : compute(new Group<>(target,sources,lambdas,false),graph,new GroupingComparatorMinMax<>(graph)))
        //    System.out.println (derived+"\n\n\n");
        System.out.println(compute(new Group<>(target,sources,lambdas,graph,true),
                                    graph,new GroupingComparatorMinMax<>(graph),2,minsum).size());
    }
}
