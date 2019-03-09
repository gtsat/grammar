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
import grammar.Graph;
import shortestpath.ShortestPath;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.Map;

final public class MinSumComparator<NodeT> implements Comparator<NodeT> {
    final private Map<NodeT,Double> cache = new TreeMap<>();
    final private ShortestPath<NodeT> sp;
    final private Group<NodeT> grouping;
    final private Graph<NodeT> graph;

    public MinSumComparator (Group<NodeT> group,Graph<NodeT> network,ShortestPath<NodeT> shp) {
        grouping = group;
        graph = network;
        sp = shp;
    }

    public MinSumComparator (Group<NodeT> group,Graph<NodeT> network) {
        grouping = group;
        graph = network;
        sp = null;
    }

    @Override
    public int compare (NodeT x, NodeT y) {
        if (x==null && y==null) return 0;
        else if (y==null) return -1;
        else if (x==null) return 1;

        double balance = computeScore(x) - computeScore(y);
        if (balance<0) return -1;
        else if (balance>0) return 1;
        else return 0;
    }
    
    public double computeScore (NodeT u) {
        if (cache.containsKey(u)) return cache.get(u);
        else if (sp!=null){
            double sum=0.0;
            for (Group<NodeT> subgroup : grouping.getSubgroups())
                sum += subgroup.getLambdas().get(subgroup) 
                        * sp.getPathCost (subgroup.getTarget(),u); // from ShortestPath<>
            cache.put(u, sum);
            return sum;

        }else{
            double sum=0.0;
            for (Group<NodeT> subgroup : grouping.getSubgroups())
                sum += subgroup.getLambdas().get(subgroup) 
                        * graph.getPathCost (subgroup.getTarget(),u); // from Graph<>
            cache.put(u, sum);
            return sum;
        }
    }
}

