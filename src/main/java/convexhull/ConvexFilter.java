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

import grammar.Graph;
import shortestpath.Point2D;
import shortestpath.ScatterMap;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.TreeSet;

final class ConvexFilter {

    private static int maxHullSize = 0;
    private static int maxQualifiedSize = 0;

    public static void resetStats () {maxQualifiedSize=0;maxHullSize=0;}
    public static int getMaxMemRequirements () {return maxHullSize+maxQualifiedSize;}

    public static<NodeT> NodeT compute (Collection<NodeT> perimeter,Graph<NodeT> graph,ScatterMap<NodeT> map,Comparator<NodeT> cmp) {
        if (perimeter==null || map==null || graph==null) 
            throw new IllegalArgumentException("!! ERROR - Providing nullable arguments not allowed !!");

        for (NodeT u : perimeter)
            if (!graph.containsNode(u))
                throw new IllegalArgumentException("!! ERROR - Perimeter point does not exist in the graph !!");

        TreeSet<Point2D> points = new TreeSet<>();
        for (NodeT u : perimeter) 
            points.add (map.getSpatialPosition(u));

        System.out.println("!! Computing convex hull for "+points.size()+" spatial points !!");
        int i=0;
        Point2D[] phase1 = new Point2D [points.size()];
        for (Point2D point : points) phase1 [i++] = point;
        GrahamScan hull = new GrahamScan (phase1);
        for (Point2D over : hull)
            for (Point2D under : hull)
                if (over!=under)
                    for (NodeT u : graph.getPath(map.getVertex(over),map.getVertex(under)))
                        points.add(map.getSpatialPosition(u));

        if (hull.size()>maxHullSize) maxHullSize = hull.size();
        System.out.println("!! Computing convex hull for "+points.size()+" spatial points !!");

        i=0;
        Point2D[] phase2 = new Point2D [points.size()];
        for (Point2D point : points) phase2 [i++] = point;
        hull = new GrahamScan (phase2);

        if (hull.size()>maxHullSize) maxHullSize = hull.size();
        System.out.println("!! Done with both convex hull computations !!");

        float xlo=Float.MAX_VALUE,ylo=Float.MAX_VALUE,xhi=-xlo,yhi=xhi;
        for (Point2D p : hull) {
            if (p.x()<xlo) xlo=p.x();
            if (p.x()>xhi) xhi=p.x();
            if (p.y()<ylo) ylo=p.y();
            if (p.y()>yhi) yhi=p.y();
        }

        NodeT mu = null;
        ArrayList<NodeT> qualified=map.getRangeVertices(new Point2D(xlo,ylo),new Point2D(xhi,yhi));
        System.out.println("!! Within MBR there are "+qualified.size()+" points !!");
        for (NodeT u : qualified)
            if (hull.isWithinHull(map.getSpatialPosition(u)) 
                && cmp.compare(u,mu)<0)
                    mu = u;

        if (qualified.size()>maxQualifiedSize) maxQualifiedSize = qualified.size();

        /*
        Stack<NodeT> stack = new Stack<>();
        TreeSet<NodeT> marked = new TreeSet<>();
        for (NodeT u : perimeter) {
            if (!marked.contains(u)) {
                if (cmp.compare(u,mu)<0) mu = u;
                marked.add(u);
                stack.add(u);
            }
        }
        while (!stack.isEmpty()) {
            NodeT top = stack.pop();
            if (cmp.compare(top,mu)<0)
                mu = top;
            for (NodeT u : graph.getNeighbors(top)) {
                if (!marked.contains(u)) {
                    Point2D closest=null, farthest=null;
                    float shortest=Float.MAX_VALUE, longest=0.0f;
                    Point2D p = map.getSpatialPosition(u);
                    for (Point2D h : hull) {
                        float distance = p.distanceTo(h);
                        if (distance < shortest) {
                            shortest = distance;
                            closest = h;
                        }
                        if (distance > longest) {
                            longest = distance;
                            farthest = h;
                        }
                    }
                    if (closest!=null && farthest!=null && p.distanceTo(farthest)<=closest.distanceTo(farthest)) {
                        marked.add(u);
                        stack.push(u);
                    }
                }
            }
            System.out.println ("** stack-size: "+stack.size());
        }
        */
        return mu;
    }
}

