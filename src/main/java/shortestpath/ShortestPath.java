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

import grammar.UndirectedGraph;
import grammar.Graph;
import grammar.Edge;
import grammar.MinPQ;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.text.NumberFormat;

final public class ShortestPath<NodeT> {
    final private ScatterMap<NodeT> scatter;
    final private Graph<NodeT> graph;

    final private Map<NodeT,Map<NodeT,Float>> cached = new HashMap<>();

    public int getMaxMemRequirements () {return cached.size()<<1;}

    public ShortestPath (Graph<NodeT> network,ScatterMap<NodeT> map) {graph=network;scatter=map;}
    private float distance (NodeT from,NodeT to) {return scatter.getSpatialPosition(from).distanceTo(scatter.getSpatialPosition(to));}

    public float getPathCost (NodeT from,NodeT to) {
        if (from.equals(to)) return 0.0f;
        if (cached.containsKey(from)) {
            Map<NodeT,Float> dists = cached.get(from);
            if (dists.containsKey(to)) return dists.get(to);
        }
        return computePathCost (from,to);
    }

    private float computePathCost (NodeT from,NodeT to) {return computePathCost(from,to,false);}
    private float computePathCost (NodeT from,NodeT to,boolean storeCache) {
        if (from.equals(to)) return 0.0f;
        float Threshold = Float.MAX_VALUE;
        HashMap<NodeT,NodeT> paths = new HashMap<>();
        HashMap<NodeT,Float> dists = new HashMap<>();
        MinPQ<NodeT> heap = new MinPQ<>(new PathComparator(from,to,dists));
        if (storeCache) paths.put (from,from);
        dists.put (from,0.0f);
        heap.insert (from);
        while (!heap.isEmpty()) {
            NodeT top = heap.delMin();
            float topdist = dists.get(top);
            if (top.equals(to)) 
                if (dists.get(to)<Threshold)
                    Threshold = dists.get(to);
            for (Edge<NodeT> edge : graph.getEdgesFrom(top)) {
                if (!dists.containsKey(edge.to) || dists.get(edge.to)>topdist+edge.weight) {
                    if (topdist+edge.weight<Threshold) {
                        if (storeCache) paths.put (edge.to,top);
                        dists.put (edge.to,topdist+edge.weight);
                        heap.insert (edge.to);
                    }
                }
            }
        }

        if (storeCache) {
            for (Entry<NodeT,NodeT> pair : paths.entrySet()) {
                NodeT curr = pair.getKey();
                float pathcost = dists.get(curr);
                for (NodeT prec = pair.getValue(); !prec.equals(curr);) {
                    Map<NodeT,Float> dprec;
                    if (cached.containsKey(prec)) {
                        dprec = cached.get(prec);
                    }else{
                        dprec = new HashMap<>();
                        cached.put (prec,dprec);
                    }

                    float partialdist = pathcost - dists.get(prec);
                    if (dprec.containsKey(curr) && dprec.get(curr)<=partialdist) continue; //break;
                    else dprec.put(curr,partialdist);

                    if (graph instanceof UndirectedGraph<?>) {
                        Map<NodeT,Float> dcurr;
                        if (cached.containsKey(curr)) {
                            dcurr = cached.get(curr);
                        }else{
                            dcurr = new HashMap<>();
                            cached.put (curr,dcurr);
                        }

                        if (!dcurr.containsKey(prec) || dcurr.get(prec)>partialdist) 
                            dcurr.put(prec,partialdist);
                    }

                    curr = prec;
                    prec = paths.get(prec);
                }
            }
        }
        return Threshold;
    }

    public ArrayDeque<NodeT> getPath (NodeT from, NodeT to) {
        return null; // no such path exists
    }

    final private class PathComparator implements Comparator<NodeT> {
        private final NodeT source, destination;
        private final HashMap<NodeT,Float> dists;
        public PathComparator (NodeT src,NodeT dest,HashMap<NodeT,Float> dsx) {source=src;destination=dest;dists=dsx;}
        @Override
        public int compare (NodeT u,NodeT v) {
            if (u==null && v==null) return 0;
            else if (v==null) return -1;
            else if (u==null) return 1;

            float Csu = dists.get (u);
            float Csv = dists.get (v);
            float Hsu = distance (source,u);
            float Hsv = distance (source,v);
            float Hud = distance (u,destination);
            float Hvd = distance (v,destination);

            float Cud = Csu * Hud / Hsu; // approximation of remaining route cost
            float Cvd = Csv * Hvd / Hsv; // approximation 
            float balance = Csu-Csv+Cud-Cvd;
            //float balance = Hud-Hvd;
            //float balance = Cud-Cvd;

            if (balance<0) return -1;
            else if (balance>0) return 1;
            else return 0;
        }
    }

    public void resetCache () {
        if (!cached.isEmpty()) {
            for (Entry<NodeT,Map<NodeT,Float>> entry : cached.entrySet()) 
                entry.getValue().clear();
            cached.clear();
        }
    }

    public void printCache () {
        for (Entry<NodeT,Map<NodeT,Float>> over : cached.entrySet())
            for (Entry<NodeT,Float> under : over.getValue().entrySet())
                System.out.println (over.getKey()+"\t"+under.getKey()+"\t"+under.getValue());
    }


    public static void main (String[] args) {
        testDistances (args);
    }

    public static void testCache (String[] args) {
        ShortestPath<Long> sp = new ShortestPath<>(new UndirectedGraph<Long>(args[0],NumberFormat.getNumberInstance(),true),
                                               new ScatterMap<Long>(args[1],NumberFormat.getNumberInstance()));
        Long source = Long.parseLong(args[2]);
        for (long u : sp.graph.getNodes()) 
            sp.getPathCost(source, u);
        sp.printCache();
    }

    public static void testDistances (String[] args) {
        if (args.length<2) throw new IllegalArgumentException("!! ERROR - Paths to an edge-list and to its nodes' coordinates are needed !!");
        long startTime, endTime;
        float sum = 0.0f;
	if (args.length>2){
            ShortestPath<Long> sp = new ShortestPath<>(new UndirectedGraph<Long>(args[0],NumberFormat.getNumberInstance(),true).kruskal(),
                                                   new ScatterMap<Long>(args[1],NumberFormat.getNumberInstance()));
            startTime = System.nanoTime();
            for (long i=0;i<Long.parseLong(args[2]);++i) 
                for (long j=i+1;j<sp.graph.numberNodes()-1;++j) 
                    //System.out.println ("** Distance from "+i+" to "+j+" is equal to "+sp.getPathCost(i,j));
                    sum += sp.getPathCost(i,j);
            sp.printCache();
        }else{
            Graph<Long> graph = new UndirectedGraph<Long>(args[0],NumberFormat.getNumberInstance(),true).kruskal();
            startTime = System.nanoTime();
            for (long i=0;i<Long.parseLong(args[1])-1;++i) 
                for (long j=i+1;j<graph.numberNodes();++j) 
                    //System.out.println ("** Distance from "+i+" to "+j+" is equal to "+graph.getPathCost(i,j));
                    sum += graph.getPathCost(i,j);
        }
        endTime = System.nanoTime();
        System.out.println ("** Summed computed distances: "+sum+" at "+(endTime-startTime)/Math.pow(10,9)+" seconds.");
    }
}

