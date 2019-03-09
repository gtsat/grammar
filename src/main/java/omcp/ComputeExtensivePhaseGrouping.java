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

public class ComputeExtensivePhaseGrouping {

    public static<NodeT> Group<NodeT> compute (Group<NodeT> grouping,Graph<NodeT> graph,boolean useMinSum) {
        GroupingComparator<NodeT> cmp = useMinSum?new GroupingComparatorMinSum<>(graph):new GroupingComparatorMinMax<>(graph);
        ArrayDeque<Group<NodeT>> processed = new ArrayDeque<>();
        ArrayDeque<Group<NodeT>> pool = new ArrayDeque<>();
        processed.add(grouping);
        pool.add(grouping);

        while (!pool.isEmpty()) {
            Group<NodeT> head = pool.remove();

            if (useMinSum?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(head)<((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(grouping)
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(head)<((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(grouping))
                grouping = head;

            Collection<Group<NodeT>> newgroupings = GenerateNextPhaseGroupings.compute(head,graph,cmp);
            if (newgroupings!=null)
                for (Group<NodeT> newgrouping : newgroupings)
                    if (!processed.contains(newgrouping)) {
                        processed.add(newgrouping);
                        pool.add(newgrouping);
                    }
        }

        System.out.println ("** [ComputeExhaustivePhaseGrouping] Score of optimal grouping retrieved "
                            +(useMinSum?((GroupingComparatorMinSum<NodeT>)cmp).computeScore(grouping)
                            :((GroupingComparatorMinMax<NodeT>)cmp).computeScore(grouping)));

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
