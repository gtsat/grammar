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

public class ComputeExtensiveGroupings {

    public static int getMaxMemRequirements() {return 0;}
    public static void resetStats () {}

    public static<NodeT> ArrayList<Group<NodeT>> compute (Group<NodeT> grouping,Graph<NodeT> graph,int k,int M,boolean useMinSum) {

        GroupingComparator<NodeT> cmp = useMinSum?new GroupingComparatorMinSum<>(graph):new GroupingComparatorMinMax<>(graph);

        MaxPQ<Group<NodeT>> result = new MaxPQ<>(cmp.BYUPPERBOUND);
        ArrayDeque<Group<NodeT>> processed = new ArrayDeque<>();
        ArrayDeque<Group<NodeT>> pool = new ArrayDeque<>();

        if (grouping.getTarget()!=null) {
            result.insert (grouping);
            processed.add (grouping);
            pool.add (grouping);
        }else{
            int i = 0;
            for (NodeT mu : new ConnectingLocationsIterable<>(grouping,graph,null,useMinSum)) {
                if (i++>=k) break;
                else{
                    Group<NodeT> newgrouping = new Group<> (mu,grouping.getSubgroups(),grouping.getLambdas(),graph,useMinSum);

                    result.insert (newgrouping);
                    processed.add (newgrouping);
                    pool.add (newgrouping);
                }
            }
        }

        while (!pool.isEmpty()) {
            Group<NodeT> head = pool.remove();
            if (result.size()>=k && 
                    (useMinSum?
                    ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(head)>=((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(result.max())
                    :((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(head)>=((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(result.max())))
                    continue;

            if (result.size()<k) {
                if (head.getMeetupsNumber()<=M) {
                    result.insert(head);
                }
            }else if (cmp.BYUPPERBOUND.compare(head,result.max())<0) {
                if (head.getMeetupsNumber()<=M) {
                    result.delMax();
                    result.insert(head);
                }
            }

            for (Group<NodeT> over : head.getSubgroups()) {
                for (Group<NodeT> under : head.getSubgroups()) {
                    if (over==under) continue;
                    else{
                        {
                            for (Group<NodeT> mhead : head.mergeSubgroups (over,under,k)) {
                                if (!processed.contains(mhead)) {
                                    processed.add(mhead);

                                    if (useMinSum?
                                       (result.size()<k || 
                                       ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(mhead)<((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(result.max())
                                        || ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(mhead)<=((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(head))
                                       :(result.size()<k || 
                                       ((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(mhead)<((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(result.max())
                                       || ((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(mhead)<=((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(head))) {
                                            pool.add(mhead);
                                    }else break;
                                }
                            }
                        }
                        if ((!over.isTrivial() || !under.isTrivial()) && over.getMeetupsNumber() + under.getMeetupsNumber() < M)
                        {
                            for (Group<NodeT> jhead : head.joinSubgroups (over,under,k)) {
                                if (!processed.contains(jhead)) {
                                    processed.add(jhead);

                                    if (useMinSum?
                                       (result.size()<k || 
                                       ((GroupingComparatorMinSum<NodeT>)cmp).computeLowerBound(jhead)<=((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(result.max())
                                        || ((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(jhead)<=((GroupingComparatorMinSum<NodeT>)cmp).computeUpperBound(head))
                                       :(result.size()<k || 
                                       ((GroupingComparatorMinMax<NodeT>)cmp).computeLowerBound(jhead)<=((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(result.max())
                                       || ((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(jhead)<=((GroupingComparatorMinMax<NodeT>)cmp).computeUpperBound(head))) {
                                            pool.add(jhead);
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
        for (Group<Long> routeplan : compute(new Group<>(target,sources,lambdas,graph,useMinSum),graph,5,3,useMinSum)){
            //System.out.println (routeplan.getMeetupsNumber()+"\t"+routeplan.getSubgroups().get(0).getTarget());
            System.out.println (routeplan);
        }
    }
}
