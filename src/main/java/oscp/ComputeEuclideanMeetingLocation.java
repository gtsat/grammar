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
import shortestpath.Point2D;
import java.util.Comparator;
import java.util.TreeSet;
import grammar.MinPQ;
import grammar.Graph;
import grammar.Edge;

final class ComputeEuclideanMeetingLocation {

    private static int maxSetSize = 0;
    private static int maxHeapSize = 0;

    public static void resetStats () {maxSetSize=0;maxHeapSize=0;}
    public static int getMaxMemRequirements () {return maxSetSize+maxHeapSize;}

    public static<NodeT> NodeT compute (Group<NodeT> group,Graph<NodeT> graph,ScatterMap<NodeT> map) {
		float relaxation = 0.0f;

		Point2D target;
		float x=0.0f, y=0.0f;
		for (Group<NodeT> subgroup : group.getSubgroups()) {
			target = map.getSpatialPosition (subgroup.getTarget());
			x += subgroup.getLambdas().get(subgroup) * target.x();
			y += subgroup.getLambdas().get(subgroup) * target.y();

			float product = 1.0f;
			for(float lambda : subgroup.getLambdas().values()) 
				product *= lambda;
			relaxation += product;
		}

		target = map.getSpatialPosition (group.getTarget());
		x += relaxation * target.x();
		y += relaxation * target.y();
		return map.getNearestVertices(new Point2D(x,y),1).remove(0);
	}
}
