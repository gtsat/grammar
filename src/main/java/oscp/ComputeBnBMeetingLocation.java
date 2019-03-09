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

package oscp;

import shortestpath.ScatterMap;
import java.util.Comparator;
import java.util.HashSet;
import grammar.MinPQ;
import grammar.Edge;
import grammar.Graph;
import grammar.UndirectedGraph;

final public class ComputeBnBMeetingLocation {

    private static int maxSetSize = 0;
    private static int maxHeapSize = 0;

    public static void resetStats () {maxSetSize=0;maxHeapSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxHeapSize;}

    public static<NodeT> NodeT compute (Group<NodeT> group,Graph<NodeT> graph,Comparator<NodeT> cmp) {
        return compute (group,graph,null,cmp);
    }
    public static<NodeT> NodeT compute (Group<NodeT> group,Graph<NodeT> graph,ScatterMap<NodeT> map,Comparator<NodeT> cmp) {
        HashSet<NodeT> marked = new HashSet<>();
        MinPQ<NodeT> queue = new MinPQ<>(cmp);

        NodeT mu = map==null?group.getTarget():ComputeEuclideanMeetingLocation.compute (group,graph,map);

        marked.add(mu);
        queue.insert(mu);
        while (!queue.isEmpty()) {
            NodeT top = queue.delMin();
            for (Edge<NodeT> e : graph.getEdgesTo(top)) {
                NodeT u = e.from;
                if (!marked.contains(u)) {
                    marked.add(u);
                    if (cmp.compare(u,top)<=0) {
                        queue.insert(u);
                    }
                }
            }
            if (cmp.compare(top, mu)<0) mu = top;

            if (marked.size()>maxSetSize) maxSetSize = marked.size();
            if (queue.size()>maxHeapSize) maxHeapSize = queue.size();
        }
        return mu;
    }

    public static void main (String[] args) {
        long target = 4000L;
        Long[] sources = {1000L,2000L,3000L};
        float[][] lambdas = {{.5f,.25f,.25f},
                             {.25f,.5f,.25f},
                             {.25f,.25f,.5f}};

        UndirectedGraph<Long> graph = new UndirectedGraph<Long>("/home/gtsat/roadnet/data/lifeifei/OL.graph",java.text.NumberFormat.getNumberInstance(),true).kruskal();
        Group<Long> group = new Group<>(target,sources,lambdas,graph,true);

        boolean useMinSum = false;

        Comparator<Long> cmp = useMinSum?new LocationComparatorMinSum<>(group,graph,null,false):new LocationComparatorMinMax<>(group,graph,null,false);
        long mu = compute(group,graph,cmp);
        System.out.println (mu+" with score "+(useMinSum?((LocationComparatorMinSum<Long>)cmp).computeScore(mu):((LocationComparatorMinMax<Long>)cmp).computeScore(mu)));
    }
}
