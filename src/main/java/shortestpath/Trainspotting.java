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

import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.File;
import java.util.Scanner;
import java.util.HashMap;
import shortestpath.Point2D;
import shortestpath.ScatterMap;
import shortestpath.ClosestPair;
import grammar.UndirectedGraph;
import grammar.Edge;

/*
 * (1) for each line: retrieve the closest two stations in the euclidean space
 * (2) connect those two with an edge
 * (3) while there are remaining unconnected nodes expand any end to the closest vertex
 *     (otherwise, if we connect the vertex to any connected node the min spanning tree is formed)
 */
public class Trainspotting {

    public static void main (String[] args) {
        if (args.length<2) throw new IllegalArgumentException ("\n!! ERROR - Expected as input is "
                    + "(i) a list with each line representing the stations of a single route, "
                    + "and (ii) stations' coordinates !!");

        HashMap<String,UndirectedGraph<String>> lines = new HashMap<>();

        ScatterMap<String> map = loadMap (args[1]);
        String linesfile = args[0];
        try {
            Scanner in = new Scanner(new File (linesfile)) ;
            while (in.hasNextLine()) {
                ScatterMap<String> linemap = new ScatterMap<>();
                UndirectedGraph<String> linegraph = new UndirectedGraph<>();

                String nextline = in.nextLine();
                String[] pair = nextline.split(":");
                if (pair.length<2) throw new RuntimeException ("\n!! ERROR - Unable to parse line: "+nextline+" !!");

                lines.put (pair[0],linegraph);

                String[] addresses = pair[1].split(",");
                for (String rawaddress : addresses) {
                    if (rawaddress.length()==0) continue;
                    String address = rawaddress.trim().toLowerCase();

                    Point2D coordinates = map.getSpatialPosition (address);
                    if (coordinates!=null) linemap.put (address,coordinates);
                    //else throw new RuntimeException ("\n!! ERROR - Unindexed vertex node '"+address+"' in the map !!");
                    else System.err.println ("!! ERROR - Unindexed vertex node '"+address+"' in the map !!");
                }
                System.err.println("!! Loaded "+linemap.size()+" stations for line "+pair[0]+" !!");

                if (linemap.isEmpty()) continue;

                Point2D[] points = new Point2D [linemap.size()];
                linemap.getPoints().toArray(points);

                ClosestPair cp = new ClosestPair (points);

                String start = linemap.remove(cp.either());
                String end = linemap.remove(cp.other());

                linegraph.setEdge (start,end,cp.distance());

                Point2D startpos = cp.either();
                Point2D endpos = cp.other();

                while (!linemap.isEmpty()) {
                    String closest2start = linemap.getNearestVertices(startpos,1).get(0);
                    String closest2end = linemap.getNearestVertices(endpos,1).get(0);

                    float distance2start = startpos.distanceTo(linemap.getSpatialPosition(closest2start));
                    float distance2end = endpos.distanceTo(linemap.getSpatialPosition(closest2end));

                    if (distance2start < distance2end) {
                        startpos = linemap.remove (closest2start);
                        linegraph.setEdge (start,closest2start,distance2start);
                        start = closest2start;
                    }else{
                        endpos = linemap.remove (closest2end);
                        linegraph.setEdge (end,closest2end,distance2end);
                        end = closest2end;
                    }
                }

                PrintWriter writer = new PrintWriter (pair[0]+".edgelist.dat","UTF-8");
                for (Edge<String> edge : linegraph) 
                    writer.println (edge.from.replace(' ','_')+"\t"+edge.to.replace(' ','_')+"\t"+edge.weight);
                writer.close();
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static ScatterMap<String> loadMap (String filename) {
        ScatterMap<String> map = new ScatterMap<>();
        try{
            Scanner in = new Scanner(new File (filename)) ;
            while (in.hasNextLine()) {
                String[] parsed = in.nextLine().split(",");
                map.put(parsed[0].trim().toLowerCase(),new Point2D(Float.parseFloat(parsed[1]),Float.parseFloat(parsed[2])));
            }
        }catch (Exception e) {e.printStackTrace();}
        System.err.println("!! Inserted "+map.size()+" stations' coordinates !!");
        return map;
    }
}
