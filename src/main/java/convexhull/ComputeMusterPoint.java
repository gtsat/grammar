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

package convexhull;

import oscp.Group;
import grammar.Graph;
import osmp.MinMaxComparator;
import osmp.MinSumComparator;
import shortestpath.ScatterMap;
import grammar.UndirectedGraph;
import grammar.DirectedGraph;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.ArrayDeque;

final public class ComputeMusterPoint {

    public static void resetStats () {ConvexFilter.resetStats();}
    public static int getMaxMemRequirements () {return ConvexFilter.getMaxMemRequirements();}

    @SuppressWarnings("unchecked")
    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,ScatterMap<NodeT> scatter,Comparator<NodeT> cmp) {
        int i=0;
        NodeT Q[] = (NodeT[]) new Object[grouping.getSubgroups().size()];
        for (Group<NodeT> group : grouping.getSubgroups()) 
            Q[i++] = group.getTarget();

        ArrayDeque<NodeT> perimeter = new ArrayDeque<>();
        for (NodeT q : Q) {
            perimeter.add(q);
            for (NodeT u : graph.getNeighbors(q))
                perimeter.add (u);
        }
        return ConvexFilter.compute(perimeter,graph,scatter,cmp);
    }
    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,ScatterMap<NodeT> scatter) {return compute(grouping,graph,scatter,null);}
}

