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
import topk.ConnectingLocationsIterable;
import java.text.NumberFormat;

final public class ComputeHeuristicGrouping {

    public static int getMaxMemRequirements() {return 0;}
    public static void resetStats () {}

    public static<NodeT> Group<NodeT> compute (Group<NodeT> grouping,Graph<NodeT> graph,boolean useMinSum) {
        GroupingComparator<NodeT> cmp = useMinSum?new GroupingComparatorMinSum<>(graph):new GroupingComparatorMinMax<>(graph);

        if (grouping.getTarget()==null) {
            for (NodeT mu : new ConnectingLocationsIterable<>(grouping,graph,null,useMinSum)) {
                grouping = new Group<> (mu,grouping.getSubgroups(),grouping.getLambdas(),graph,useMinSum);
                break;
            }
        }

        float minTauJ = useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeScore(grouping):((GroupingComparatorMinMax<NodeT>)cmp).computeScore(grouping), minTauM = minTauJ;
        Group<NodeT> minGroupingJ = grouping, minGroupingM = grouping, minGrouping = grouping;
        while (true) {
            for (Group<NodeT> over : grouping.getSubgroups()) {
                for (Group<NodeT> under : grouping.getSubgroups()) {
                    if (over==under) break;
                    else{
                        if (!over.isTrivial() || !under.isTrivial()) {
                            Group<NodeT> newgrouping = grouping.joinSubgroups(over,under);

                            assert (newgrouping.isValidGrouping());
                            assert (newgrouping.numberTravelers()==grouping.numberTravelers());

                            float tauNew = useMinSum?
                                          ((GroupingComparatorMinSum<NodeT>)cmp).computeScore(newgrouping)
                                          :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(newgrouping);

                            if (tauNew<minTauJ) {
                                minGroupingJ = newgrouping;
                                minTauJ = tauNew;
                            }
                        }

                        {
                            Group<NodeT> newgrouping = grouping.mergeSubgroups(over,under);

                            assert (newgrouping.isValidGrouping());
                            assert (newgrouping.numberTravelers()==grouping.numberTravelers());

                            float tauNew = useMinSum?
                                          ((GroupingComparatorMinSum<NodeT>)cmp).computeScore(newgrouping)
                                          :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(newgrouping);

                            if (tauNew<minTauM) {
                                minGroupingM = newgrouping;
                                minTauM = tauNew;
                            }
                        }

                        minGrouping = minTauJ<minTauM?minGroupingJ:minGroupingM;
                    }
                }
            }
            if (grouping.equals(minGrouping)) break;
            else grouping = minGrouping;
        }
/**
        System.out.println ("** [ComputeHeuristicGrouping] Score of optimal grouping retrieved "
                            +(useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeScore(grouping)
                            :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(grouping)));
**/
        return grouping;
    }

    public static void main (String[] args) {
        Graph<Long> graph = new UndirectedGraph<>("/home/gtsat/roadnet/data/lifeifei/OL.graph",NumberFormat.getNumberInstance(),true);

        boolean useMinSum = true;

        long target = 4500L;
        Long[] sources = {1000L,1500L,2000L,2500L,3000L};
        float[][] lambdas = {{.6f,.1f,.1f,.1f,.1f},
                             {.1f,.6f,.1f,.1f,.1f},
                             {.1f,.1f,.6f,.1f,.1f},
                             {.1f,.1f,.1f,.6f,.1f},
                             {.1f,.1f,.1f,.1f,.6f}};
        System.out.println (compute(new Group<>(target,sources,lambdas,graph,useMinSum),graph,useMinSum));
    }
}
