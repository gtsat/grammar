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

package ann;

import oscp.LocationComparatorMinSum;
import java.util.Comparator;
import java.util.TreeSet;
import grammar.UndirectedGraph;
import grammar.MinPQ;
import grammar.Graph;
import oscp.Group;

public class StartingPostsExpansion {

    private static int maxSetSize = 0;
    private static int maxHeapSize = 0;

    public static void resetStats () {maxSetSize=0;maxHeapSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxHeapSize;}


    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,Comparator<NodeT> cmp) {
        NodeT threshold = grouping.getTarget();
        MinPQ<NodeT> heap = new MinPQ<>(cmp);
        TreeSet<NodeT> marked = new TreeSet<>();
        heap.insert (grouping.getTarget());
        marked.add (grouping.getTarget());
        for (Group<NodeT> group : grouping.getSubgroups()) {
            if (cmp.compare(group.getTarget(),threshold)<0) 
                threshold = group.getTarget();
            heap.insert (group.getTarget());
            marked.add (group.getTarget());
        }

        NodeT mu = null;
        while (!heap.isEmpty()) {
            NodeT top = heap.delMin();
            for (NodeT u : graph.getNeighbors(top)) {
                if (!marked.contains(u)) {
                    if (cmp.compare(u,threshold)<=0) 
                        heap.insert(u);
                    marked.add(u);
                }

                if (cmp.compare(top,mu)<0) mu = top;

                if (marked.size()>maxSetSize) maxSetSize = marked.size();
                if (heap.size()>maxHeapSize) maxHeapSize = heap.size();
            }
        }
        return mu;
    }

    public static void main (String[] args) {
        long target = 4000L;
        Long[] sources = {1000L,2000L,3000L};
        float[][] lambdas = {{.2f,.4f,.4f},{.4f,.2f,.4f},{.4f,.4f,.2f}};

        UndirectedGraph<Long> graph = new UndirectedGraph<Long>("../data/lifeifei/OL.graph",java.text.NumberFormat.getNumberInstance(),true).kruskal();
        Group<Long> grouping = new Group<>(target,sources,lambdas,graph,true);

        LocationComparatorMinSum<Long> cmp = new LocationComparatorMinSum<>(grouping,graph,null,true);

        long mu = compute(grouping,graph,cmp);

        System.out.println (mu+" with score "+cmp.computeScore(mu));
    }
}


