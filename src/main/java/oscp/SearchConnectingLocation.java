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

import grammar.MinPQ;
import grammar.Edge;
import grammar.Graph;
import java.util.Stack;
import java.util.TreeSet;
import java.util.ArrayDeque;
import java.util.Comparator;

/**
 * Baseline search method for the Optimum Connecting Point problem
 */
public class SearchConnectingLocation {

    private static int maxSetSize = 0;
    private static int maxStructSize = 0;

    public static void resetStats () {maxSetSize=0;maxStructSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxStructSize;}

    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,Comparator<NodeT> cmp) {
        return compute (grouping,graph,cmp,-1);
    }
    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,Comparator<NodeT> cmp,int searchType) {
        switch (searchType) {
            case 0: return DFS (grouping,graph,cmp);
            case 1: return BFS (grouping,graph,cmp);
            default: return BestFirstSearch (grouping,graph,cmp);
        }
    }

    public static<NodeT> NodeT DFS (Group<NodeT> grouping,Graph<NodeT> graph,Comparator<NodeT> cmp) {
        TreeSet<NodeT> marked = new TreeSet<>();
        Stack<NodeT> stack = new Stack<>();
        marked.add (grouping.getTarget());
        stack.push (grouping.getTarget());
        NodeT mu = null;
        while (!stack.isEmpty()) {
            NodeT top = stack.pop();
            if (cmp.compare(top,mu)<0) mu = top;
            for (Edge<NodeT> e : graph.getEdgesTo(top)) {
                NodeT u = e.from;
                if (!marked.contains(u)) {
                    marked.add(u);
                    if (cmp.compare(u,grouping.getTarget())<0)
                        stack.push(u);
                }
            }

            if (marked.size()>maxSetSize) maxSetSize = marked.size();
            if (stack.size()>maxStructSize) maxStructSize = stack.size();
        }
        return mu;
    }

    public static<NodeT> NodeT BFS (Group<NodeT> grouping, Graph<NodeT> graph,Comparator<NodeT> cmp) {
        ArrayDeque<NodeT> queue = new ArrayDeque<>();
        TreeSet<NodeT> marked = new TreeSet<>();
        queue.addLast (grouping.getTarget());
        marked.add (grouping.getTarget());
        NodeT mu = null;
        while (!queue.isEmpty()) {
            NodeT top = queue.removeFirst();
            if (cmp.compare(top,mu)<0) mu = top;
            for (Edge<NodeT> e : graph.getEdgesTo(top)) {
                NodeT u = e.from;
                if (!marked.contains(u)) {
                    marked.add(u);
                    if (cmp.compare(u,grouping.getTarget())<0)
                        queue.addLast(u);
                }
            }

            if (marked.size()>maxSetSize) maxSetSize = marked.size();
            if (queue.size()>maxStructSize) maxStructSize = queue.size();
        }
        return mu;
    }

    public static<NodeT> NodeT BestFirstSearch (Group<NodeT> grouping, Graph<NodeT> graph,Comparator<NodeT> cmp) {
        TreeSet<NodeT> marked = new TreeSet<>();
        MinPQ<NodeT> queue = new MinPQ<>();
        queue.insert (grouping.getTarget());
        marked.add (grouping.getTarget());
        NodeT mu = null;
        while (!queue.isEmpty()) {
            NodeT top = queue.delMin();
            if (cmp.compare(top,mu)<0) mu = top;
            for (Edge<NodeT> e : graph.getEdgesTo(top)) {
                NodeT u = e.from;
                if (!marked.contains(u)) {
                    marked.add(u);
                    if (cmp.compare(u,grouping.getTarget())<0)
                        queue.insert(u);
                }
            }

            if (marked.size()>maxSetSize) maxSetSize = marked.size();
            if (queue.size()>maxStructSize) maxStructSize = queue.size();
        }
        return mu;
    }
}

