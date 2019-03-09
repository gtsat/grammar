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

package partitioning;

import grammar.Edge;
import grammar.Graph;
import grammar.MinPQ;
import grammar.MaxPQ;
import shortestpath.Point2D;
import shortestpath.ScatterMap;
import java.text.NumberFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;

import java.util.concurrent.locks.ReentrantReadWriteLock;


public class GraphPartition<NodeT> implements Comparable<GraphPartition<NodeT>>{

    final private Graph<NodeT> reference;
    final private ScatterMap<NodeT> map;
    
    private Map<NodeT,Float> in,out;

    final private float alpha;

    private static float globaldistancethreshold = Float.MAX_VALUE;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private int size = 1;
    public int hierarchySize () {return size;}

    /* there is no way going from a node to the left subgraph, 
     * to a node to the right subgraph without encountering at 
     * least one crossedge.
     */
    private GraphPartition<NodeT> left=null, right=null;
    final private ArrayDeque<Edge<NodeT>> crossedges = new ArrayDeque<>();

    private float separationdegree = 0.0f;

    public int numberOfElements () {return map.size();}
    public boolean isLeaf () {return left==null && right==null;}
    public float getSeparationDegree () {return separationdegree;}
    public Graph<NodeT> getReferenceGraph () {return reference;}
    public GraphPartition<NodeT> getRightChild () {return right;}
    public GraphPartition<NodeT> getLeftChild () {return left;}
    public Set<NodeT> getVertices() {return map.getVertices();}
    public ArrayDeque<Edge<NodeT>> getCrossEdges() {return crossedges;}
    public boolean containsVertex (NodeT vertex) {return map.getSpatialPosition(vertex)!=null;}
    public boolean intersects (Collection<NodeT> collection) {
        Collection<NodeT> vertices = map.getVertices();
        if (collection.size()<vertices.size()) {
            for (NodeT u : collection)
                if (vertices.contains(u))
                    return true;
        }else{
            for (NodeT u : vertices)
                if (collection.contains(u))
                    return true;
        }
        return false;
    }


    private GraphPartition (Graph<NodeT> graph,ScatterMap<NodeT> scatter,float a) {reference=graph;map=scatter;alpha=a;}
    public GraphPartition (Graph<NodeT> graph,ScatterMap<NodeT> scatter,int leafnodes,float a) {
        if (leafnodes<1) throw new IllegalArgumentException("** ERROR - Number of leaf nodes is at least 1.");
        reference = graph;
        map = scatter;
	alpha = a;

        MaxPQ<GraphPartition<NodeT>> heap = new MaxPQ<>(new PartitionSpreadComparator<NodeT>());
        heap.insert(this);
        for (int i=1;i<leafnodes && !heap.isEmpty();++i) {
            GraphPartition<NodeT> top = heap.delMax();
            if (!top.isLeaf()) 
                throw new RuntimeException("\n!! ERROR - Only leaf-nodes should be allowed in the heap !!");
            top.partitionOverMostSpreadDim();
            heap.insert(top.right);
            heap.insert(top.left);
            size += 2;
        }
        while (!heap.isEmpty())
            heap.delMax();
    }

    private class PartitionSpreadComparator<NodeT> implements Comparator<GraphPartition<NodeT>> {
        @Override public int compare (GraphPartition<NodeT> a, GraphPartition<NodeT> b) {
            float balance = (a.map.getMostSpreadDim()==0?a.map.getXspread():a.map.getYspread()) 
                           - (b.map.getMostSpreadDim()==0?b.map.getXspread():b.map.getYspread());

            if (balance<0) return -1;
            else if (balance>0) return 1;
            else return 0;
        }
    }

    @Override public int compareTo (GraphPartition<NodeT> other) {
        if (separationdegree<other.separationdegree) return -1;
        else if (separationdegree>other.separationdegree) return 1;
        else return 0;
    }

    private void preprocessChildPartitions () {
        Edge<NodeT> minLEdge = null;
        float minWeight = Float.MAX_VALUE;
        for (Edge<NodeT> cedge : left.crossedges) {
            if (cedge.weight<minWeight) {
                minWeight = cedge.weight;
                minLEdge = cedge;
            }
        }

        if (minLEdge==null) return;

        MinPQ<Edge<NodeT>> heap = new MinPQ<>();
        left.out = new HashMap<>();
        heap.insert (new Edge<>(minLEdge.from,minLEdge.from,0.0f));
        left.out.put (minLEdge.from,0.0f);
        while(!heap.isEmpty()) {
            Edge<NodeT> top = heap.delMin();
            for (Edge<NodeT> backedge : reference.getEdgesTo(top.from)) {
                if (!left.out.containsKey(backedge.from) && !left.crossedges.contains(backedge)) {
                    heap.insert (new Edge<> (backedge.from, minLEdge.from, backedge.weight+top.weight));
                    left.out.put (backedge.from, backedge.weight+top.weight);
                }
            }
        }
        
        
        Edge<NodeT> minREdge = null;
        float minRWeight = Float.MAX_VALUE;
        for (Edge<NodeT> cedge : right.crossedges) {
            if (cedge.weight<minWeight) {
                minWeight = cedge.weight;
                minREdge = cedge;
            }
        }

        if (minREdge==null) return;

        //MinPQ<Edge<NodeT>> heap = new MinPQ<>();
        left.in = new HashMap<>();
        heap.insert (new Edge<>(minREdge.from,minREdge.from,0.0f));
        left.in.put (minREdge.from,0.0f);
        while(!heap.isEmpty()) {
            Edge<NodeT> top = heap.delMin();
            for (Edge<NodeT> edge : reference.getEdgesFrom(top.to)) {
                if (!left.in.containsKey(edge.from) && !right.crossedges.contains(edge)) {
                    heap.insert (new Edge<> (edge.from, minREdge.from, edge.weight+top.weight));
                    left.in.put (edge.from, edge.weight+top.weight);
                }
            }
        }
    }


    /*
     * Finds the <>K</> local closest pairs between points from <>R</> and <>S</>.
     */
    public MaxPQ<Edge<NodeT>> graphjoin (Collection<NodeT> R, 
                                         Collection<NodeT> S, 
                                         int K, float threshold, 
                                         Map<NodeT,Map<NodeT,Float>> filteredbordernodes) {

        return graphconcurrentjoinonthreshold (R,S,K,threshold,filteredbordernodes);
    }

    /*
     * More concurrent but the final result congregates no more than K elements.
     */
    public void graphconcurrentjoinonresult (Collection<NodeT> R, 
                                         Collection<NodeT> S, 
                                         int K, MaxPQ<Edge> result, 
                                         Map<NodeT,Map<NodeT,Float>> filteredbordernodes) {

        MinPQ<Edge<NodeT>> horizon = new MinPQ<>();
        Collection<NodeT> iterateThrough, filter;
        if (map.size()<R.size()) {
            iterateThrough = getVertices();
            filter = R;
        }else{
            iterateThrough = R;
            filter = getVertices();
        }
        Map<NodeT,Map<NodeT,Float>> bordernodes = new HashMap<>();
        Map<NodeT,Set<NodeT>> marked = new HashMap<>();
        for (NodeT u : iterateThrough) {
            if (filter.contains(u)) {
                Set<NodeT> markedset = new HashSet<>();
                marked.put (u,markedset);
                for (Edge<NodeT> edge : reference.getEdgesFrom(u)) {
                    lock.readLock().lock();
                    if (result.size()<K || edge.weight<result.max().weight) {
                        lock.readLock().unlock();
                        if (!markedset.contains(edge.to) && !crossedges.contains(edge)) {
                            markedset.add (edge.to);
                            horizon.insert (edge);
                        }
                    }else lock.readLock().unlock();
		}
            }
        }

        while (!horizon.isEmpty()) {
            Edge<NodeT> top = horizon.delMin();
            if (S.contains(top.to)) {
                lock.readLock().lock();
                if (result.size()<K) {
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try{result.insert(top);}
                    finally{lock.writeLock().unlock();}
                }else if (top.compareTo(result.max())<0){
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    result.delMax();
                    try{result.insert(top);}
                    finally{lock.writeLock().unlock();}
                }else lock.readLock().unlock();
            }

            for (Edge<NodeT> edge : reference.getEdgesFrom(top.to)) {
                lock.readLock().lock();
                if (result.size()<K || edge.weight<result.max().weight) {
                    lock.readLock().unlock();
                    if (marked.get(top.from).contains(edge.to) && !crossedges.contains(edge)) {
                        horizon.insert (new Edge<>(top.from,edge.to,top.weight+edge.weight));
                        marked.get(top.from).add(edge.to);
                    }
                }else lock.readLock().unlock();
            }
         }
    }

    /*
     * Less concurent/contention but in total the results to be merged are up to K times the degree of parallelism
     */
    private MaxPQ<Edge<NodeT>> graphconcurrentjoinonthreshold (Collection<NodeT> R, 
                                         Collection<NodeT> S, 
                                         int K, float threshold, 
                                         Map<NodeT,Map<NodeT,Float>> filteredbordernodes) {

        MaxPQ<Edge<NodeT>> result = new MaxPQ<>();
        MinPQ<Edge<NodeT>> horizon = new MinPQ<>();
        Collection<NodeT> iterateThrough, filter;
        if (map.size()<R.size()) {
            iterateThrough = getVertices();
            filter = R;
        }else{
            iterateThrough = R;
            filter = getVertices();
        }
        Map<NodeT,Map<NodeT,Float>> bordernodes = new HashMap<>();
        Map<NodeT,Set<NodeT>> marked = new HashMap<>();
        for (NodeT u : iterateThrough) {
            if (filter.contains(u)) {
                Set<NodeT> markedset = new HashSet<>();
                marked.put (u,markedset);
                for (Edge<NodeT> edge : reference.getEdgesFrom(u)) {
                    if (edge.weight<threshold && !markedset.contains(edge.to) && !crossedges.contains(edge)) {
                        markedset.add (edge.to);
                        horizon.insert (edge);
                    }
		}
            }
        }

        while (!horizon.isEmpty()) {
            Edge<NodeT> top = horizon.delMin();
            if (S.contains(top.to)) {
                if (result.size()<K)
                    result.insert(top);
                else if (top.compareTo(result.max())<0){
                    result.delMax();
                    result.insert(top);

                    threshold = result.max().weight;
/**/
                    lock.readLock().lock();
                    if (globaldistancethreshold < threshold) {
                        threshold = globaldistancethreshold;
                        lock.readLock().unlock();
                    }else if (threshold < globaldistancethreshold) {
                        lock.readLock().unlock();
                        lock.writeLock().lock();
                        try {globaldistancethreshold = threshold;}
                        finally{lock.writeLock().unlock();}
                    }
/**/
                }
            }

            for (Edge<NodeT> edge : reference.getEdgesFrom(top.to)) {
                if (edge.weight<threshold && marked.get(top.from).contains(edge.to) && !crossedges.contains(edge)) {
                    horizon.insert (new Edge<>(top.from,edge.to,top.weight+edge.weight));
                    marked.get(top.from).add(edge.to);
                }
            }
         }

         return result;
    }

    private MaxPQ<Edge<NodeT>> graphnestedloopjoin (Collection<NodeT> R, 
                                         Collection<NodeT> S, 
                                         int K, float threshold, 
                                         Map<NodeT,Map<NodeT,Float>> filteredbordernodes) {

        MaxPQ<Edge<NodeT>> heap = new MaxPQ<>();
        Collection<NodeT> iterateThrough, filter;
        if (map.size()<R.size()) {
            iterateThrough = getVertices();
            filter = R;
        }else{
            iterateThrough = R;
            filter = getVertices();
        }
        Map<NodeT,Map<NodeT,Float>> bordernodes = new HashMap<>();
        for (NodeT u : iterateThrough) {
            if (filter.contains(u)) {
                Map<NodeT,Float> ubordernodes = new HashMap<>();

                lock.readLock().lock();
                try {
                    if (globaldistancethreshold<threshold)
                        threshold = globaldistancethreshold;
                }finally{lock.readLock().unlock();}

                for (Edge<NodeT> match : match (u,S,K,threshold,ubordernodes)) {
                    if (heap.size()<K) heap.insert(match);
                    else if (match.compareTo(heap.max())<0) {
                        heap.delMax();
                        heap.insert(match);
			threshold = heap.max().weight;

                        //lock.readLock().lock();
                        //lock.readLock().unlock();
                        lock.writeLock().lock();
                        try {
                            if (threshold < globaldistancethreshold)
                                globaldistancethreshold = threshold;
                            //lock.readLock().lock();
                        }finally{
                            lock.writeLock().unlock();
                            //lock.readLock().unlock();
                        }
                    }
                }
            }
            bordernodes.put(u,new HashMap<NodeT,Float>());
        }

        //Map<NodeT,Map<NodeT,Float>> filteredbordernodes = new HashMap<>();
        if (filteredbordernodes!=null) {
            for (Entry<NodeT,Map<NodeT,Float>> outer : bordernodes.entrySet()) {
                for (Entry<NodeT,Float> inner : outer.getValue().entrySet())
                    if (inner.getValue()<heap.max().weight) {
                        if (!filteredbordernodes.containsKey(inner.getKey()))
                            filteredbordernodes.put(inner.getKey(),new HashMap<NodeT,Float>());
                        filteredbordernodes.get(inner.getKey()).put(outer.getKey(),inner.getValue());
                    }
            }
        }
        return heap;
    }

    public MaxPQ<Edge<NodeT>> match (NodeT source, 
                                    Collection<NodeT> R, 
                                    int k, float threshold, 
                                    Map<NodeT,Float> bordernodes) {

        MaxPQ<Edge<NodeT>> qualified = new MaxPQ<>();
        MinPQ<Edge<NodeT>> pathends = new MinPQ<>();
        Set<NodeT> marked = new HashSet<>();

        if (containsVertex(source)) { 
            // <>source</> is a local vertex.
            marked.add (source);
            for (Edge<NodeT> edge : reference.getEdgesFrom(source)) {
                if (edge.weight<threshold) {
                    marked.add(edge.to);
                    pathends.insert(edge);
                }
            }
        }else return qualified;

        while (!pathends.isEmpty()) {
            Edge<NodeT> top = pathends.delMin();
            if (R.contains(top.to)) 
                if (qualified.size()<k) qualified.insert (new Edge<>(source,top.to,top.weight));
                else{
                    qualified.delMax();
                    qualified.insert (new Edge<>(source,top.to,top.weight));
                    threshold = qualified.max().weight;
                }
            for (Edge<NodeT> edge : reference.getEdgesFrom(top.to)) {
                if (!crossedges.contains(edge)) {
                    if (edge.weight+top.weight<threshold && !marked.contains(edge.to)) {
                        pathends.insert (new Edge<>(source,edge.to,edge.weight+top.weight));
                        marked.add (edge.to);
                    }
                }else if (bordernodes.containsKey(edge.to))
                    bordernodes.put (edge.to,edge.weight+top.weight);
            }
        }
        return qualified;
    }

    private void partitionOverMostSpreadDim () {
        if (map.getLOXpoint()==null||map.getLOYpoint()==null||map.getHIXpoint()==null||map.getHIYpoint()==null)
            throw new RuntimeException("\n!! ERROR - Unable to define the most spread dimension !!");
        if (map.getMostSpreadDim()==0) partition(map.getVertex(map.getLOXpoint()),map.getVertex(map.getHIXpoint()));
        else partition(map.getVertex(map.getLOYpoint()),map.getVertex(map.getHIYpoint()));
    }

    /*
     * Creates left and right child-nodes
     */
    private void partition (NodeT lo,NodeT hi) {
        if (lo==null || hi==null) throw new IllegalArgumentException("\n!! ERROR - One of the provided arguments was null !!");
        if (lo.equals(hi)) throw new RuntimeException("\n!! ERROR - The two sources correspond to the same node !!");
        MinPQ<Edge<NodeT>> loheap = new MinPQ<>();
        MinPQ<Edge<NodeT>> hiheap = new MinPQ<>();
        for (Edge<NodeT> edge : reference.getEdgesFrom(lo)) 
            if (!hi.equals(edge.to))
                loheap.insert(edge);
        for (Edge<NodeT> edge : reference.getEdgesFrom(hi)) 
            if (!lo.equals(edge.to))
                hiheap.insert(edge);
        ArrayDeque<NodeT> locluster = new ArrayDeque<>();
        ArrayDeque<NodeT> hicluster = new ArrayDeque<>();
        locluster.add(lo);
        hicluster.add(hi);

        int i=0;
        separationdegree = Float.MAX_VALUE;
        left = new GraphPartition<>(reference,new ScatterMap<NodeT>(),alpha);
        right = new GraphPartition<>(reference,new ScatterMap<NodeT>(),alpha);
        for (Edge<NodeT> crossedge : crossedges) {
            left.crossedges.addLast(crossedge);
            right.crossedges.addLast(crossedge);
        }
        while (!loheap.isEmpty() || !hiheap.isEmpty()) {
            Edge<NodeT> lotop = loheap.isEmpty()?null:loheap.min();
            Edge<NodeT> hitop = hiheap.isEmpty()?null:hiheap.min();
            if (hitop==null || lotop!=null && 
		locluster.size()*lotop.weight/(locluster.size()+alpha*hicluster.size()) 
		< hicluster.size()*hitop.weight/(alpha*locluster.size()+hicluster.size())) {
                loheap.delMin();
                if (!locluster.contains(lotop.to) && !hicluster.contains(lotop.to)) {
                    locluster.add (lotop.to);
                    for (Edge<NodeT> edge : reference.getEdgesFrom(lotop.to))
                        if (!crossedges.contains(edge)) {
                            if (!locluster.contains(edge.to) && !hicluster.contains(edge.to))
                                loheap.insert (edge);
                            else if (hicluster.contains(edge.to)) {
                                left.crossedges.addLast(edge);
                                if (edge.weight<separationdegree)
                                    separationdegree=edge.weight;
                            }
                        }
                }
            }else{
                hiheap.delMin();
                if (!locluster.contains(hitop.to) && !hicluster.contains(hitop.to)) {
                    hicluster.add (hitop.to);
                    for (Edge<NodeT> edge : reference.getEdgesFrom(hitop.to))
                        if (!crossedges.contains(edge)) {
                            if (!locluster.contains(edge.to) && !hicluster.contains(edge.to))
                                hiheap.insert (edge);
                            else if (locluster.contains(edge.to)) {
                                right.crossedges.addLast(edge);
                                if (edge.weight<separationdegree)
                                    separationdegree=edge.weight;
                            }
                        }
                }
            }
        }

        for (NodeT vertex : locluster) 
            if (map.getSpatialPosition(vertex)!=null)
                left.map.put (vertex,map.getSpatialPosition(vertex));
        for (NodeT vertex : hicluster) 
            if (map.getSpatialPosition(vertex)!=null)
                right.map.put (vertex,map.getSpatialPosition(vertex));
        System.out.println("Left size: "+left.numberOfElements()+"/"+numberOfElements()
                            +"\t\t Right size: "+right.numberOfElements()+"/"+numberOfElements()
                            +"\t with degree of separation equal to "+separationdegree);
    }

    public int levelsToClosestLeaf () {return isLeaf()?0:levelsToClosestLeaf(0);}
    private int levelsToClosestLeaf (int level) {
        if (isLeaf()) return level;
        else{
            int leftlevels = left.levelsToClosestLeaf(level+1);
            int rightlevels = right.levelsToClosestLeaf(level+1);
            return leftlevels<rightlevels?leftlevels:rightlevels;
        }
    }

    public void dump (String foldername) throws IOException {
        File folder = new File (foldername);
        if (!folder.exists()) folder.mkdirs();
        if (foldername.endsWith("/")) dumpRec(foldername);
        else dumpRec (foldername+"/");
    }

    private void dumpRec (String filepath) throws IOException {
        dumpLocal (filepath);
        if (this.left!=null) left.dumpRec(filepath+"0");
        if (this.right!=null) right.dumpRec(filepath+"1");
    }

    private void dumpLocal (String filepath) throws IOException {
        File file = new File (filepath+".vert");
        if (!file.exists()) file.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        for (Entry<NodeT,Point2D> entry : map.getContent())
            bw.write(entry.getKey()+"\t"+entry.getValue().x()+"\t"+entry.getValue().y()+"\n");
        bw.close();

        file = new File (filepath+".cross");
        if (!file.exists()) file.createNewFile();
        bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        for (Edge<NodeT> edge : getCrossEdges())
            bw.write(edge.from+"\t"+edge.to+"\t"+edge.weight+"\n");
        bw.close();
    }


    public GraphPartition (Graph<NodeT> graph, String filepath) {
        reference = graph;
        alpha = -1.0f ;
        if (new File(filepath+".vert").exists() && new File(filepath+".cross").exists()) {
            map = new ScatterMap<>(filepath+".vert",NumberFormat.getNumberInstance());
            Graph<NodeT> temp = new grammar.UndirectedGraph<>(filepath+".cross",NumberFormat.getNumberInstance(),true);
            for (Edge<NodeT> edge : temp) crossedges.add(edge);

            if (new File(filepath+"1.vert").exists() && new File(filepath+"1.cross").exists())
                right = new GraphPartition<>(graph,filepath+"1");
            if (new File(filepath+"0.vert").exists() && new File(filepath+"0.cross").exists())
                left = new GraphPartition<>(graph,filepath+"0");
        }else throw new RuntimeException("\n!! ERROR - Invalid filepath '"+filepath+"' !!");
    }


    public static void main (String[] args) throws IOException {
        if (args.length<5) 
            throw new IllegalArgumentException("!! ERROR - Provide the filepath to the graph, the nodes' coordinates, "+
                                                "the number of leaf node partitions, smoothing parameter and the path "+
                                                "to the folder where it will be dumped !!");

        Graph<Long> graph = new grammar.UndirectedGraph<>(args[0],NumberFormat.getNumberInstance(),true);
        ScatterMap<Long> map = new ScatterMap<>(args[1],NumberFormat.getNumberInstance());
        GraphPartition<Long> gp = new GraphPartition<>(graph,map,Integer.parseInt(args[2]),Float.parseFloat(args[3]));
        gp.dump(args[4]);

        //GraphPartition<Long> loadedgp = new GraphPartition<>(graph,args[3],1.0f);
    }
}
