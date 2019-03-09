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

package osmp;

import oscp.Group;
import grammar.MinPQ;
import grammar.Graph;
import java.util.Comparator;
import java.util.TreeSet;

public class ComputeMusterPoint {

    private static int maxSetSize = 0;
    private static int maxHeapSize = 0;

    public static void resetStats () {maxSetSize=0;maxHeapSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxHeapSize;}

    public static<NodeT> NodeT compute (Group<NodeT> grouping, Graph<NodeT> graph, Comparator<NodeT> cmp) {
        NodeT mu = null;
        for (Group<NodeT> subgroup : grouping.getSubgroups())
            if (cmp.compare(subgroup.getTarget(),mu)<0)
                mu = subgroup.getTarget();

        if (mu==null) throw new RuntimeException ("!! ERROR - Cannot initialize graph search !!");

        MinPQ<NodeT> queue = new MinPQ<>(cmp);
        TreeSet<NodeT> marked = new TreeSet<>();
        queue.insert(mu);
        marked.add(mu);
        while (!queue.isEmpty()) {
            NodeT top = queue.delMin();
            for (NodeT u : graph.getNeighbors(top)) {
                if (!marked.contains(u)) {
                    marked.add(u);
                    if (cmp.compare(u,top)<=0)
                        queue.insert(u);
                }
            }
            if (cmp.compare(top, mu)<0) mu = top;

            if (marked.size()>maxSetSize) maxSetSize = marked.size();
            if (queue.size()>maxHeapSize) maxHeapSize = queue.size();
        }
        return mu;
    }
}

