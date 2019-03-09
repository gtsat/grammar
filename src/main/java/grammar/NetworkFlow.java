/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2014 George Tsatsanifos <gtsatsanifos@gmail.com>
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

package grammar;

import java.util.Stack;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.PriorityQueue;

public class NetworkFlow<NodeT> {
    private final DirectedGraph<NodeT> network = new DirectedGraph<>();
    private final DirectedGraph<NodeT> flows = new DirectedGraph<>();

    public NetworkFlow () {}
    
    public boolean setPipe (NodeT from, NodeT to, Float capacity) {
        if (capacity<0) return setPipe (to,from,-capacity);

        Float flow = flows.getEdgeWeight(from, to);
        if (flow.equals(Float.NaN) || capacity >= flow) {
            network.setEdge (from, to, capacity);
            return true;
        }else return false;
    }

    public boolean setFlow (NodeT from, NodeT to, Float flow) {
        if (flow<0) return setFlow (to,from,-flow);

        Float capacity = network.getEdgeWeight(from, to);
        if (!capacity.equals(Float.NaN) && flow<=capacity) {
            flows.setEdge (from, to, flow);
            return true;
        }else return false;
    }
    
    public boolean addFlow (NodeT from, NodeT to, Float flow) {
        if (flow<0) return addFlow (to,from,-flow);

        Float capacity = network.getEdgeWeight(from, to);
        Float oldFlow = flows.getEdgeWeight(from, to);
        if (!oldFlow.equals(Float.NaN)) flow += oldFlow;

        if (!capacity.equals(Float.NaN) && flow<=capacity) {
            flows.setEdge (from, to, flow);
            return true;
        }else return false;
    }

    public Float getResidualCapacity (NodeT from, NodeT to) {
        Float capacity = network.getEdgeWeight(from,to);
        Float flow = flows.getEdgeWeight(from,to);
        if (!capacity.equals(Float.NaN)) {
            float residual;
            if (flow.equals(Float.NaN)) residual = capacity;
            else residual = capacity - flows.getEdgeWeight(from, to);

            if (!network.getEdgeWeight(to,from).equals(Float.NaN))
                residual += flow.equals(Float.NaN)?0.0:flow;
            return residual;
        }else
        if (!network.getEdgeWeight(to,from).equals(Float.NaN))
            return flows.getEdgeWeight(to,from).equals(Float.NaN)?0.0f:flow;
        else return Float.NaN;
    }

    /**
     * Uses Breadth First Search.
     * @param source, the node where the augmenting path starts from.
     * @param target, the node where the augmenting path finishes.
     * @return true if there is at least one 
     * augmenting path from <>source</> node 
     * to <>target</> node; false otherwise.
     */
    public boolean hasAugmentingPath (NodeT source, NodeT target) {
        PriorityQueue<NodeT> queue = new PriorityQueue<>();
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        queue.add(source);
        marked.set(source);
        while (!queue.isEmpty()) {
            NodeT top = queue.poll();
            if (top.equals(target)) return true;
            else{
                for (NodeT u : network.getNeighbors(top)) {
                    if (!marked.get(u)) {
                        Float residual = getResidualCapacity(top,u);
                        if (!residual.equals(Float.NaN) && residual>0.0) {
                            marked.set(u);
                            queue.add(u);
                        }
                    }
                }

                for (NodeT u : network.getBacklinks(top)) {
                    if (!marked.get(u)) {
                        Float residual = getResidualCapacity(top,u);
                        if (!residual.equals(Float.NaN) && residual>0.0) {
                            marked.set(u);
                            queue.add(u);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Uses Depth First Search.
     * @param source, the node where the augmenting path starts from.
     * @param target, the node where the augmenting path finishes.
     * @return 
     */
    public Collection<NodeT> getAugmentingPath (NodeT source, NodeT target) {
        ArrayDeque<NodeT> path = new ArrayDeque<>();
        BitsetSparse<NodeT> marked = new BitsetSparse<>();
        Stack<NodeT> stack = new Stack<>();
        marked.set (source);
        stack.push (source);
        path.add (source);
        while (!stack.isEmpty()) {
            NodeT top = stack.pop();
            if (top.equals(target)) {
                path.add(target);
                return path;
            }else{
                boolean newElementsAdded = false;
                for (NodeT u : network.getNeighbors(top)) {
                    if (!marked.get(u)) {
                        Float residual = getResidualCapacity(top,u);
                        if (!residual.equals(Float.NaN) && residual>0.0) {
                            marked.set(u);
                            stack.add(u);

                            newElementsAdded = true;
                        }
                    }
                }
                if (newElementsAdded) path.addLast (top);
                else path.removeLast();
            }
        }
        path.clear();
        return new ArrayDeque<>();
    }

    public Float fulkerson (NodeT source, NodeT target) {
        float value = 0.0f;
        do{
            Collection<NodeT> path = getAugmentingPath(source,target);
            if (path==null || path.isEmpty()) break;

            float bottleneck = Float.POSITIVE_INFINITY;
            NodeT u = null;
            for (NodeT v : path) {
                if (u != null) {
                    Float flowIncrement = getResidualCapacity(u, v);
                    if (flowIncrement<bottleneck) bottleneck = flowIncrement;
                }
                u=v;
            }

            u = null;
            for (NodeT v : path) {
                if (u != null) {
                    if (!addFlow(u, v, bottleneck))
                        throw new RuntimeException ("\n!! Incremented flow cannot exceed capacity !!");
                }
                u=v;
            }

            value += bottleneck;
        }while(true);
        return value;
    }

    public Collection<Edge<NodeT>> minCutEdges (NodeT source, NodeT target) {
        return null;
    }

    public static void main (String[] args) {
        System.out.println ("!! Testing demo network flow !!");

        NetworkFlow<Integer> nf = new NetworkFlow<> ();

        /** level 1 **/
        nf.setPipe (0,1,10.0f);
        nf.setPipe (0,2,5.0f);
        nf.setPipe (0,3,15.0f);

        /** level 2 **/
        nf.setPipe (1,4,9.0f);
        nf.setPipe (1,5,15.0f);
        nf.setPipe (2,5,8.0f);
        nf.setPipe (3,6,16.0f);
        nf.setPipe (2,6,-6.0f);

        nf.setPipe (1,2,4.0f);
        nf.setPipe (2,3,4.0f);

        /** level 3 **/
        nf.setPipe(4,7,10.0f);
        nf.setPipe(5,7,10.0f);
        nf.setPipe(6,7,10.0f);

        nf.setPipe (4,5,15.0f);
        nf.setPipe (5,6,15.0f);
        /********************/

        boolean test = true;

        /** level 1 **/
        test &= nf.setFlow (0,1,10.0f);
        test &= nf.setFlow (0,2,5.0f);
        test &= nf.setFlow (0,3,13.0f);

        /** level 2 **/
        test &= nf.setFlow (1,4,8.0f);
        test &= nf.setFlow (1,5,2.0f);
        test &= nf.setFlow (2,5,8.0f);
        test &= nf.setFlow (3,6,16.0f);
        test &= nf.setFlow (2,6,-6.0f);
        test &= nf.setFlow (2,3,3.0f);

        /** level 3 **/
        test &= nf.setFlow(4,7,8.0f);
        test &= nf.setFlow(5,7,10.0f);
        test &= nf.setFlow(6,7,10.0f);

        assert (test);
        /********************/

        if (nf.hasAugmentingPath(0,7)) System.out.println ("!! Demo graph has at least one more augmenting path !!");
        else System.out.println ("!! Demo graph has no augmenting paths !!");

        System.out.print (" Augmenting path: ");
        for (int u : nf.getAugmentingPath(0,7))
            System.out.print (u + " ");
        System.out.println();

        Float flow = nf.fulkerson (0,7);
        System.out.println("!! Ford-Fulkerson's algorithm increments value by " + flow + " units !!");

        System.out.println ("!! Done processing demo network flow !!");
    }
}
