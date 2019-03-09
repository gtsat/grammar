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

import shortestpath.ShortestPath;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import grammar.Graph;

final public class LocationComparatorMinSum<NodeT> implements Comparator<NodeT> {
    final private Group<NodeT> grouping;
    final private Graph<NodeT> graph;
    final private NodeT target;
    
    final private float relaxation;

    final private Map<NodeT,Float> cache = new HashMap<>();
    final private ShortestPath<NodeT> sp;

    final private float Cw;
    final private boolean boundWithinRelevantArea;

    public LocationComparatorMinSum (Group<NodeT> group,Graph<NodeT> network,ShortestPath<NodeT> shp,boolean bound) {
        target = group.getTarget();
        grouping = group;
        graph = network;
        sp = shp;

        float summedproduct = 0.0f;
        for (int i=0; i<group.getSubgroups().size(); ++i) {
            Group<NodeT> over = group.getSubgroups().get(i);

            float product=1.0f;
            for (Group<NodeT> under : group.getSubgroups())
                product *= over.getLambdas().get(under);
            summedproduct += product;
        }
        relaxation = summedproduct;

        Cw = bound?computeScore(target):Float.MAX_VALUE;
        boundWithinRelevantArea = bound;
    }

    @Override
    public int compare (NodeT x, NodeT y) {
        if (x==null && y==null) return 0;
        else if (y==null) return -1;
        else if (x==null) return 1;
        else{
            float Cx=computeScore(x), Cy=computeScore(y);
            if (boundWithinRelevantArea) {
                if (Cx<=Cw && Cy>Cw) return -1;
                else if (Cx>Cw && Cy<=Cw) return 1;
                else return 0;
            }else{
                float balance=Cx-Cy;
                if (balance<0) return -1;
                else if (balance>0) return 1;
                else return 0;
           }
       }
    }

    /* Chech whether node u is within the relevant area */
    public boolean isRelevant (NodeT u) {
        float groupcost = 0.0f, individualcost = 0.0f;
        float remainingdistance = sp==null?graph.getPathCost(u,target):sp.getPathCost(u,target);
        for (Group<NodeT> group : grouping.getSubgroups()) {
            groupcost += group.getLambdas().get(group)
                      * (sp==null?graph.getPathCost(group.getTarget(),u):sp.getPathCost(group.getTarget(),u));
            individualcost += group.getLambdas().get(group)
                           * (sp==null?graph.getPathCost(group.getTarget(),target):sp.getPathCost(group.getTarget(),target));
        }
        groupcost += relaxation * remainingdistance;
        return groupcost<=individualcost;
    }

    public float computeScore (NodeT u) {
        if (cache.containsKey(u)) return cache.get(u);
        else {
            float score = 0.0f;
            for (Group<NodeT> group : grouping.getSubgroups()) {
                score += (sp==null?graph.getPathCost(group.getTarget(),u):sp.getPathCost(group.getTarget(),u))
                        * group.getLambdas().get(group);
            }
            score += relaxation * graph.getPathCost (u, target);
            score /= grouping.getSubgroups().size();
            cache.put(u, score);
            return score;
        }
    }
}

