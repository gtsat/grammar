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
import grammar.MinPQ;
import grammar.Graph;
import grammar.UndirectedGraph;
import topk.ConnectingLocationsIterable;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayDeque;

final public class ComputeClusteredGrouping {

    public static int getMaxMemRequirements() {return 0;}
    public static void resetStats () {}

    public static<NodeT> Group<NodeT> compute (Group<NodeT> grouping,Graph<NodeT> graph,boolean useMinSum) {
        GroupingComparator<NodeT> cmp = useMinSum?new GroupingComparatorMinSum<>(graph):new GroupingComparatorMinMax<>(graph);
        MinPQ<Group<NodeT>> groupings = new MinPQ<>(cmp.BYLOWERBOUND);
        ArrayDeque<Group<NodeT>> processed = new ArrayDeque<>();

        if (grouping.getTarget()==null) {
            for (NodeT mu : new ConnectingLocationsIterable<>(grouping,graph,null,useMinSum)) {
                grouping = new Group<> (mu,grouping.getSubgroups(),grouping.getLambdas(),graph,useMinSum);
                break;
            }
        }

        groupings.insert (grouping);
        processed.add (grouping);

        float Threshold = Float.MAX_VALUE;
        for (int i=0; !groupings.isEmpty(); ++i) {
            //System.out.println("** [ComputeOptimalGrouping] Now processing the "+i+"-th element of "+groupings.size()+" from the heap.");

            Group<NodeT> top = groupings.delMin();
            if ((useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(top)
               :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(top))>=Threshold) 
                   break;

            Group<NodeT> bestnewgrouping = null;
            Collection<Group<NodeT>> newgroupings = ProduceNextPhaseGroupings.compute(top,graph,cmp,useMinSum);

            //System.out.println("** [ComputeOptimalGrouping] Retrieved "+(newgroupings==null?0:newgroupings.size())+" new groupings.");
            if (newgroupings!=null && !newgroupings.isEmpty()) {
                for (Group<NodeT> newgrouping : newgroupings)
                    if (useMinSum?
                        ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(newgrouping)<((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(bestnewgrouping)
                        :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(newgrouping)<((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(bestnewgrouping))
                            bestnewgrouping = newgrouping;

                //System.out.println("** [ComputeOptimalGrouping] Best newly retrieved group is "+bestnewgrouping.hashCode());

                if (useMinSum?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(bestnewgrouping)<((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(grouping)
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(bestnewgrouping)<((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(grouping)) {
                    //System.out.println("** [ComputeOptimalGrouping] Replacing optimal solution "+grouping.hashCode()+" to group "+bestnewgrouping.hashCode());
                    grouping = bestnewgrouping;
                    Threshold = useMinSum?
                                ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(grouping)
                                :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(grouping);
                }

                for (Group<NodeT> newgrouping : newgroupings)
                    if ((useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(newgrouping)
                       :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(newgrouping))<Threshold)
                        if (!processed.contains(newgrouping)) {
                           groupings.insert(newgrouping);
                           processed.add(newgrouping);
                        }
            }
            //System.out.println("   ------------------------------");
        }
/**
        System.out.println ("** [ComputeOptimalGrouping] Score of optimal grouping retrieved "
                            +(useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeScore(grouping)
                            :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(grouping)));
**/
        return grouping;
    }

    public static void main (String[] args) {
        Graph<Long> graph = new UndirectedGraph<>("/Users/gtsat/HomeGrown/data/lifeifei/SF.graph",NumberFormat.getNumberInstance(),true);
        //graph.draw(800,600);

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
