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

package shortestpath;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Scanner;
import java.text.Format;
import java.io.File;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.ArrayList;

final public class ScatterMap<NodeT> implements Iterable<Entry<NodeT,Point2D>> {
    final private Map<NodeT,Point2D> vertexmap = new TreeMap<>();
    final private Map<Point2D,NodeT> pointmap = new TreeMap<>();
    final private KdTree kdtree = new KdTree();

    private int records = 0;

    public int size() {return records;}
    public boolean isEmpty() {return records==0;}

    public Point2D remove (NodeT vertex) {
        Point2D p = vertexmap.remove(vertex);
        pointmap.remove(p);
        kdtree.remove(p);
        --records;
        return p;
    }

    public NodeT remove (Point2D point) {
        NodeT u = pointmap.remove(point);
        kdtree.remove(point);
        vertexmap.remove(u);
        --records;
        return u;
    }

    public void put (NodeT vertex,Point2D p) {
        if (!vertexmap.containsKey(vertex) && !pointmap.containsKey(p)) {
            vertexmap.put (vertex,p);
            pointmap.put (p,vertex);
            kdtree.add(p);

            ++records;

            if (LOX>p.x()) {LOX=p.x();pointLOX=p;}
            if (LOY>p.y()) {LOY=p.y();pointLOY=p;}
            if (HIX<p.x()) {HIX=p.x();pointHIX=p;}
            if (HIY<p.y()) {HIY=p.y();pointHIY=p;}
        }
    }

    private float LOX,LOY,HIX,HIY;
    private Point2D pointLOX=null,pointLOY=null,pointHIX=null,pointHIY=null;

    public float getXspread () {return HIX-LOX;}
    public float getYspread () {return HIY-LOY;}
    public Point2D getLOXpoint () {return pointLOX;}
    public Point2D getLOYpoint () {return pointLOY;}
    public Point2D getHIXpoint () {return pointHIX;}
    public Point2D getHIYpoint () {return pointHIY;}
    public int getMostSpreadDim () {return HIX-LOX>HIY-LOY?0:1;}
    public float getDiagonalSpread () {return (float)Math.sqrt(getXspread()*getXspread()+getYspread()*getYspread());}

    public boolean isEnclosed (Point2D p) {return p.x()>=LOX && p.x()<=HIX && p.y()>=LOY && p.y()<=HIY;}

    public int getMaxMemRequirements () {return (records<<3)+(records<<2)-records;}

    public ScatterMap () {
        LOX = LOY = Float.MAX_VALUE;
        HIX = HIY = -Float.MAX_VALUE;
    }
    public ScatterMap (String filename, Format format) {
        LOX = LOY = Float.MAX_VALUE;
        HIX = HIY = -Float.MAX_VALUE;
        loadMap(filename,format);
    }

    @Override
    public Iterator<Entry<NodeT,Point2D>> iterator () {return vertexmap.entrySet().iterator();}
    public Point2D getSpatialPosition (NodeT id) {return vertexmap.get(id);}
    public NodeT getVertex (Point2D point) {return pointmap.get(point);}
    public Set<NodeT> getVertices () {return vertexmap.keySet();}
    public Set<Point2D> getPoints () {return pointmap.keySet();}
    public Set<Entry<NodeT,Point2D>> getContent() {return vertexmap.entrySet();}

    public ArrayList<NodeT> getNearestVertices (Point2D query, int k) {
        ArrayList<NodeT> neighbors = new ArrayList<>();
        for (Point2D point : kdtree.nearestNeighbourSearch (k,query))
            neighbors.add(pointmap.get(point));
        return neighbors;
    }

    public ArrayList<NodeT> getRangeVertices (Point2D lo, Point2D hi) {
        if (lo==null || hi==null) 
            throw new IllegalArgumentException("!! ERROR - Nullable arguments not allowed !!");
        ArrayList<NodeT> result = new ArrayList<>();
        for (Point2D point : kdtree.rangeSearch(lo, hi))
            result.add(pointmap.get(point));
        return result;
    }

    @SuppressWarnings ("unchecked")
    private void loadMap (String filename, Format format) {
        try{
            Scanner in = new Scanner(new File(this.getClass().getClassLoader().getResource(filename).getFile())) ;
            System.out.println ("!! Importing now data from file '" + filename + "'... !!");
            long startTime = System.nanoTime();
            for (int i=1; in.hasNextLine(); ++i) {
                String[] line = in.nextLine().split("\\s+");
                if (line.length>0 && line[0].startsWith("#")) continue;
                if (line.length==0) continue;

                Point2D p = new Point2D (Float.parseFloat(line[1]),Float.parseFloat(line[2]));
                NodeT id = (NodeT) (format==null? line[0] : format.parseObject(line[0]));
                vertexmap.put (id,p);
                pointmap.put (p,id);
                kdtree.add(p);

                if (LOX>p.x()) {LOX=p.x();pointLOX=p;}
                if (LOY>p.y()) {LOY=p.y();pointLOY=p;}
                if (HIX<p.x()) {HIX=p.x();pointHIX=p;}
                if (HIY<p.y()) {HIY=p.y();pointHIY=p;}

                if ((i%1000000)==0) System.out.println("!! So far " +i + " points have been read !!");
            }
            long endTime = System.nanoTime();
            records = vertexmap.size();
            System.out.println("!! Loaded "+records+" points in "+(endTime-startTime)/Math.pow(10,9)+" secs !!");
            in.close();
        }catch (FileNotFoundException e){
            System.out.println ("\n!! Input file '"+filename+"' does not exist !!");
        }catch (ParseException e){
            System.out.println ("\n!! Incorrectly defined domain type in input file '"+filename+"' !!");
        }
    }
}
