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

package centroid;

import java.util.TreeSet;
import java.util.Comparator;
import shortestpath.ScatterMap;
import shortestpath.Point2D;
import grammar.MinPQ;
import grammar.Graph;
import oscp.Group;

final public class ComputeMusterPoint {

    private static int maxSetSize = 0;
    private static int maxHeapSize = 0;

    public static void resetStats () {maxSetSize=0;maxHeapSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxHeapSize;}

    public static<NodeT> NodeT compute (Group<NodeT> grouping,Graph<NodeT> graph,ScatterMap<NodeT> scatter,Comparator<NodeT> cmp,boolean useBestFirstSearch) {
        float x=0.0f, y=0.0f;
        for (Group<NodeT> group : grouping.getSubgroups()) {
            Point2D point = scatter.getSpatialPosition (group.getTarget());
            float weight = group.getLambdas().get(group);
            x += weight * point.x();
            y += weight * point.y();
        }

        NodeT mu = scatter.getNearestVertices(new Point2D(x,y),1).get(0);
        if (useBestFirstSearch) {
            TreeSet<NodeT> marked = new TreeSet<>();
            MinPQ<NodeT> heap = new MinPQ<>(cmp);
            heap.insert(mu);
            marked.add(mu);
            while (!heap.isEmpty()) {
                NodeT top = heap.delMin();
                for (NodeT u : graph.getNeighbors(mu)) {
                    if (!marked.contains(u)) {
                        marked.add(u);
                        if (cmp.compare(u,top)<=0) {
                            /*
                            if (cmp instanceof MinSumComparator<?>) {
                                // additional pruning for <>MinSumComparator</> 
                                float costDiff=0.0f;
                                for (Group<NodeT> group : grouping.getSubgroups())
                                    costDiff += group.getLambdas().get(group);
                                //(graph.getPathCost(group.getTarget(),u)-graph.getPathCost(group.getTarget(),mu));
                                costDiff *= graph.getPathCost(u,mu);
                                if (costDiff>((MinSumComparator)cmp).computeScore(u)-((MinSumComparator)cmp).computeScore(mu))
                                    heap.insert(u);
                            }else if (cmp instanceof MinMaxComparator<?>) {
                                // additional pruning for <>MinMaxComparator</> 
                                float Cmu=0.0f,Cu=0.0f;
                                Group<NodeT> Gmu,Gu;
                                for (Group<NodeT> group : grouping.getSubgroups()) {
                                    float Tmu = group.getLambdas().get(group)*graph.getPathCost(group.getTarget(),u);
                                    float Tu = group.getLambdas().get(group)*graph.getPathCost(group.getTarget(),u);

                                    if (Tmu>Cmu) {
                                        Gmu = group;
                                        Cmu = Tmu;
                                    }
                                    if (Tu>Cu) {
                                        Gu = group;
                                        Cu = Tu;
                                    }
                                }
                                
                                
                                float costDiff = ((MinMaxComparator)cmp).computeScore(u)-((MinMaxComparator)cmp).computeScore(mu);
                                if (costDiff < )
                                    heap.insert(u);
                            }else throw new RuntimeException ("!! ERROR - Incompatible comparator for Optimal Muster Point computation !!");
                            */
                            heap.insert(u);
                        }
                    }
                }
                if (cmp.compare(top,mu)<0) mu=top;

                if (heap.size()>maxHeapSize) maxHeapSize = heap.size();
                if (marked.size()>maxSetSize) maxSetSize = marked.size();
            }
        }else{
            for (boolean newSolutionFound=false; newSolutionFound; newSolutionFound=false) {
                for (NodeT u : graph.getNeighbors(mu)) {
                    if (cmp.compare(u,mu)<0) {
                        newSolutionFound = true;
                        mu = u;
                    }
                }
            }
        }
        return mu;
    }
}

