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

package omcp;

import oscp.Group;
import grammar.Graph;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

final public class GroupingComparatorMinMax<NodeT> extends GroupingComparator<NodeT> {

    public GroupingComparatorMinMax (Graph<NodeT> network) {super(network);}

    @Override public float computeUpperBound (Group<NodeT> group) {return computeScore (group);}

    @SuppressWarnings("unchecked")
    public float computeScore (Group<NodeT> group) {
        if (group==null || group.getTarget() ==null) return Float.MAX_VALUE;
        else if (group.getSubgroups().isEmpty()) return 0.0f;
        else if (hicache.containsKey(group)) {
            return hicache.get(group);
        }else{
            float score = 0.0f;
            for (float cost : group.getTravelersCosts().values())
                if (cost>score) score = cost;
            return score;
        }
    }

    @SuppressWarnings("unchecked")
    @Override public float computeLowerBound (Group<NodeT> group) {
        if (group==null || group.getTarget()==null) return Float.MAX_VALUE;
        else if (group.getSubgroups().isEmpty()) return 0.0f;
        else if (locache.containsKey(group)) {
            return locache.get(group);
        }else{
            Map<Group<NodeT>,Float> costs = new HashMap<>();
            Map<Group<NodeT>,ArrayDeque<Float>> weights = new HashMap<>();
            Map<Group<NodeT>,ArrayDeque<Group<NodeT>>> paths = new HashMap<>();
            group.getTravelersCosts (costs,weights,paths);

            float score = 0.0f;
            Collection<Group<NodeT>> travelers = costs.keySet();
            for (Group<NodeT> traveler : travelers) {
                float tscore = 0.0f;
                float weight = weights.get(traveler).getLast();
                ArrayDeque<Group<NodeT>> path = paths.get(traveler);
                Group<NodeT> previous = traveler;
                for (Group<NodeT> meetup : path)
                    if (meetup.getTarget()!=null)
                        tscore += weight * graph.getPathCost(previous.getTarget(),meetup.getTarget());
                    else return Float.MAX_VALUE;
                if (tscore>score) score = tscore;
            }
            return score;
        }
    }
}
