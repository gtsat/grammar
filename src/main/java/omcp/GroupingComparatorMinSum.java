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
import java.util.ArrayDeque;

final public class GroupingComparatorMinSum<NodeT> extends GroupingComparator<NodeT> {

    public GroupingComparatorMinSum (Graph<NodeT> network) {super(network);}

    @Override public float computeUpperBound (Group<NodeT> group) {return computeScore (group);}

    @SuppressWarnings("unchecked")
    public float computeScore (Group<NodeT> group) {
        if (group==null) return Float.MAX_VALUE;
        else if (group.getSubgroups().isEmpty()) return 0.0f;
        else if (hicache.containsKey(group)) return hicache.get(group);
        else{
            float score = 0.0f;
            for (Group<NodeT> subgroup : group.getSubgroups()) {
                //if (subgroup.getTarget() == null)
                //    throw new RuntimeException ("!! ERROR - Subgroup has nulled target !!");
                if (!subgroup.getLambdas().containsKey(subgroup))
                    throw new RuntimeException ("!! ERROR - Group<NodeT> has no lambda parameter for contained subgroup !!");
                score += computeScore (subgroup);
                if (group.getTarget()!=null) {
                    if (!subgroup.getLambdas().containsKey(subgroup)) {
                        System.exit(1);
                    }else if (subgroup.getLambdas().get(subgroup) == null) {
                        throw new RuntimeException("!! ERROR - Nulled lambda value for self-subgroup containment !!");
                    }
                    float lambda = subgroup.getLambdas().get(subgroup);
                    float pathCost = graph.getPathCost(subgroup.getTarget(), group.getTarget());
                    score += lambda * pathCost;
                }else return Float.MAX_VALUE;
            }
            hicache.put(group, score);
            return score;
        }
    }

    @SuppressWarnings("unchecked")
    @Override public float computeLowerBound (Group<NodeT> group) {
        if (group==null) return Float.MAX_VALUE;
        else if (group.getSubgroups().isEmpty()) return 0.0f;
        else if (locache.containsKey(group)) return locache.get(group);
        else{
            float score = 0.0f;
            ArrayDeque<Group<NodeT>> travelers = group.getTravelers();
            for (Group<NodeT> over : travelers) {
                float relaxation = 1.0f;
                for (Group<NodeT> under : travelers) 
                    relaxation *= over.getLambdas().get(under);
                score += computeScore (over);
                if (group.getTarget()!=null)
                    score += relaxation * graph.getPathCost (over.getTarget(),group.getTarget());
                else return Float.MAX_VALUE;
            }
            locache.put(group, score);
            return score;
        }
    }
}
