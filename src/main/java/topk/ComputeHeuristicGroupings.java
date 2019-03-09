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

import oscp.Group;
import grammar.MaxPQ;
import grammar.Graph;
import grammar.UndirectedGraph;
import omcp.GroupingComparatorMinMax;
import omcp.GroupingComparatorMinSum;
import omcp.GroupingComparator;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;

final public class ComputeHeuristicGroupings {

    public static int getMaxMemRequirements() {return 0;}
    public static void resetStats () {}

    public static<NodeT> ArrayList<Group<NodeT>> compute (Group<NodeT> grouping,Graph<NodeT> graph,int k,int M,boolean useMinSum) {
        GroupingComparator<NodeT> cmp = useMinSum?new GroupingComparatorMinSum<>(graph):new GroupingComparatorMinMax<>(graph);
        float minTau = useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeScore(grouping):((GroupingComparatorMinMax<NodeT>)cmp).computeScore(grouping);

        MaxPQ<Group<NodeT>> result = new MaxPQ<>(cmp.BYUPPERBOUND);
        ArrayDeque<Group<NodeT>> processed = new ArrayDeque<>();

        if (grouping.getTarget()!=null) {
            result.insert (grouping);
            processed.add (grouping);
        }else{
            int i = 0;
            for (NodeT mu : new ConnectingLocationsIterable<>(grouping,graph,null,useMinSum)) {
                if (i++>=k) break;
                else{
                    Group<NodeT> newgrouping = new Group<> (mu,grouping.getSubgroups(),grouping.getLambdas(),graph,useMinSum);
                    if (result.isEmpty() || cmp.BYUPPERBOUND.compare(newgrouping,grouping)<0)
                        grouping = newgrouping;

                    result.insert (newgrouping);
                    processed.add (newgrouping);
                }
            }
        }

        for (boolean isResultUpdated=true; isResultUpdated;) {
            isResultUpdated=false;
            Group<NodeT> minGrouping = grouping;
            for (Group<NodeT> over : grouping.getSubgroups()) {
                for (Group<NodeT> under : grouping.getSubgroups()) {

                    if (over==under) break;
                    else{
                        if ((!over.isTrivial() || !under.isTrivial()) && over.getMeetupsNumber() + under.getMeetupsNumber() < M)
                        {
                            for (Group<NodeT> newgrouping : grouping.joinSubgroups(over,under,k)) {

                                if (!newgrouping.isValidGrouping()) {
                                    throw new RuntimeException("!! Invalid grouping encountered !!");
                                }
                                if (newgrouping.numberTravelers()!=grouping.numberTravelers()) {
                                    throw new RuntimeException("!! Grouping construction error !!");
                                }

                                if (!processed.contains(newgrouping)) {
                                    processed.add (newgrouping);

                                    float tauNew = useMinSum?
                                        ((GroupingComparatorMinSum<NodeT>)cmp).computeScore(newgrouping)
                                        :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(newgrouping);

                                    if (tauNew<minTau) {
                                        minGrouping = newgrouping;
                                        minTau = tauNew;
                                        isResultUpdated = true;
                                    }

                                    if (result.size()<k) {
                                        if (newgrouping.getMeetupsNumber()<=M) {
                                            //if (!result.contains(newgrouping))
                                                result.insert(newgrouping);
                                            isResultUpdated = true;
                                        }
                                    }else if (cmp.BYUPPERBOUND.compare(newgrouping,result.max())<0) {
                                        if (newgrouping.getMeetupsNumber()<=M) {
                                            //if (!result.contains(newgrouping)) {
                                                result.delMax();
                                                result.insert(newgrouping);
                                            //}
                                            isResultUpdated = true;
                                        }
                                    }else break;
                                }else if (result.size()>=k && cmp.BYUPPERBOUND.compare(newgrouping,result.max())>=0)
                                    break;
                            }
                        }

                        {
                            for (Group<NodeT> newgrouping : grouping.mergeSubgroups(over,under,k)) {

                                if (!newgrouping.isValidGrouping()) {
                                    throw new RuntimeException("!! Invalid grouping encountered !!");
                                }
                                if (newgrouping.numberTravelers()!=grouping.numberTravelers()) {
                                    throw new RuntimeException("!! Grouping construction error !!");
                                }

                                if (!processed.contains(newgrouping)) {
                                    processed.add (newgrouping);

                                    float tauNew = useMinSum?
                                        ((GroupingComparatorMinSum<NodeT>)cmp).computeScore(newgrouping)
                                        :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(newgrouping);

                                    if (tauNew<minTau) {
                                        minGrouping = newgrouping;
                                        minTau = tauNew;
                                        isResultUpdated = true;
                                    }

                                    if (result.size()<k) {
                                        if (newgrouping.getMeetupsNumber()<=M) {
                                            //if (!result.contains(newgrouping))
                                                result.insert(newgrouping);
                                            isResultUpdated = true;
                                        }
                                    }else if (cmp.BYUPPERBOUND.compare(newgrouping,result.max())<0) {
                                        if (newgrouping.getMeetupsNumber()<=M) {
                                            //if (!result.contains(newgrouping)) {
                                                result.delMax();
                                                result.insert(newgrouping);
                                            //}
                                            isResultUpdated = true;
                                        }
                                    }else break;
                                }else if (result.size()>=k && cmp.BYUPPERBOUND.compare(newgrouping,result.max())>=0)
                                    break;
                            }
                        }
                    }
                }
            }
            grouping = minGrouping;
        }
	ArrayList<Group<NodeT>> resultlist = new ArrayList<>();
	while (!result.isEmpty()) resultlist.add(result.delMax());
        return resultlist;
    }

    public static void main (String[] args) {
        Graph<Long> graph = new UndirectedGraph<>("/Users/gtsat/HomeGrown/data/lifeifei/SF.graph",NumberFormat.getNumberInstance(),true);

        boolean useMinSum = true;

        long target = 5000L;
        Long[] sources = {1000L,2000L,3000L,4000L};

        float[][] lambdas = {{.6f,.1f,.1f,.1f},
                             {.1f,.6f,.1f,.1f},
                             {.1f,.1f,.6f,.1f},
                             {.1f,.1f,.1f,.6f}};

        int i=0;
        for (Group<Long> routeplan : compute(new Group<>(null,sources,lambdas,graph,useMinSum),graph,5,3,useMinSum))
            System.out.println (++i+": "+routeplan+"\n");
    }
}
