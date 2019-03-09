/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2015 George Tsatsanifos<gtsatsanifos@gmail.com>
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
 *  along with this program.  If not, see<http://www.gnu.org/licenses/>.
 * 
 * Author: Alexandr Petcovici<petcovici_alexandr@yahoo.com>
 */

package setbased;

import grammar.Graph;
import java.util.Iterator;
import oscp.Group;
import grammar.UndirectedGraph;
import grammar.Graph;
import grammar.MinPQ;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;


public class ComputeMeetingLocationDijkstra<NodeT> implements Iterable<NodeT> 
{
    final private Group<NodeT> group;
    final private Graph<NodeT> graph;
    final private boolean useMinSum;

    public ComputeMeetingLocationDijkstra (Group<NodeT> group,Graph<NodeT> graph,boolean useMinSum) {
        this.useMinSum = useMinSum;
        this.group = group;
        this.graph = graph;
    }
        
    @Override
    public Iterator<NodeT> iterator() {
        {return new ConnectingLocationsIterator<NodeT>(group,graph,useMinSum);}
    }
    
    final private class ConnectingLocationsIterator<NodeT> implements Iterator<NodeT> {
        final private MinPQ<NodeT> ompHeap;
        final private Group<NodeT> group;
        final private Graph<NodeT> graph;
        final private VertexComparator cmp;
        
        public ConnectingLocationsIterator(Group<NodeT>grouping,Graph<NodeT> graph,boolean useMinSum)
        {
            this.group = grouping;
            this.graph = graph;
            
            int numSources = group.getSubgroups().size();
            Map<NodeT,Float>[] ds = new HashMap [numSources];
            Map<NodeT,Float> dt = new Hashtable<>();
            
            for (int i=0; i<numSources; i++){
                ds[i] = new HashMap<> ();
                ds[i] = graph.dijkstraDistances(group.getSubgroups().get(i).getTarget()); 
            }
            
            dt = new HashMap<>();
            dt = graph.dijkstraDistances(group.getTarget());
            
            this.cmp = (useMinSum? new VertexMinSumComparator<>(group, ds, dt, graph):new VertexMinMaxComparator<>(group, ds, dt, graph));
            this.ompHeap = new MinPQ<>(cmp);
            
            for (NodeT node: ds[0].keySet()) {
                cmp.computeScore(node);
                ompHeap.insert(node);
            }
        }
        @Override public NodeT next() {return (ompHeap.delMin());}
        @Override public boolean hasNext() { return !ompHeap.isEmpty();}
        @Override public void remove () {throw new UnsupportedOperationException("\n!! ERROR - Cannot remove nodes from the graph !!");}
    }
    
    
    abstract private class VertexComparator<NodeT> implements Comparator<NodeT> {
        final protected Map<NodeT,Float> scores = new HashMap<>();
        final protected Group<NodeT> group;
        Graph<NodeT> graph;
        Map<NodeT,Float>[] ds;
        Map<NodeT,Float> dt;
        
        public VertexComparator(Group<NodeT> group,  Map<NodeT,Float>[] ds,  Map<NodeT,Float> dt, Graph<NodeT> graph)
        {
            this.group = group; this.graph = graph;
            this.ds = ds;
            this.dt = dt;
        }   
        abstract public float computeScore(NodeT newVertex);
        
        public int compare (NodeT x,NodeT y) {
            if (scores.get(x).floatValue() > (scores.get(y)).floatValue()) return 1;
            else if (scores.get(x).floatValue() < (scores.get(y)).floatValue()) return -1;
            else return 0;  
        }
    }
    
    
    final private class VertexMinMaxComparator<NodeT> extends VertexComparator<NodeT> {
        public VertexMinMaxComparator(Group<NodeT> group,  Map<NodeT,Float>[] ds,  Map<NodeT,Float> dt, Graph<NodeT> graph)
        {super(group, ds, dt, graph);}
        
        @Override
        public float computeScore (NodeT newVertex) 
        {
            float maxDS = 0;
            for (int i = 0 ; i< (ds.length); ++i)
                if (maxDS< ds[i].get(newVertex).longValue())
                        maxDS = ds[i].get(newVertex);
            
            float product = 1.0f;
            for (float lambda : group.getLambdas().values())
                product *= (lambda * dt.get(newVertex));

            maxDS += product;
            scores.put(newVertex, maxDS);
            return maxDS;
        }
    }
    
    final private class VertexMinSumComparator<NodeT> extends VertexComparator<NodeT> 
    {
        public VertexMinSumComparator(Group<NodeT> group,  Map<NodeT,Float>[] ds,  Map<NodeT,Float> dt, Graph<NodeT> graph) 
        {super(group, ds, dt, graph);}

        @Override
        public float computeScore (NodeT newVertex) 
        {
            float sum = 0;
            
            for (int i=0 ; i<ds.length; ++i)
                sum += ds[i].get(newVertex);

            float product = 1.0f;
            for (float lambda : group.getLambdas().values())
                product *= (lambda * dt.get(newVertex));

            sum += product;
            scores.put(newVertex, sum);
            return sum;
        }
    }
        public static void main (String[] args) 
        {
            long target = 4000L;
            Long[] sources = {1000L,2000L,3000L};
            float[][] lambdas = {{.2f,.4f,.4f},{.4f,.2f,.4f},{.4f,.4f,.2f}};

            UndirectedGraph<Long> graph = new UndirectedGraph<Long>("D:\\rd2\\data\\lifeifei\\OL.graph",NumberFormat.getNumberInstance(),true).kruskal();
            Group<Long> grouping = new Group<>(target,sources,lambdas,graph,true);


            for (Long mu : new ComputeMeetingLocationDijkstra<Long>(grouping,graph,false)) {
                oscp.LocationComparatorMinSum<Long> cmp = new oscp.LocationComparatorMinSum<>(grouping,graph,null,false);
                System.out.println ("** Score "+cmp.computeScore(mu)+ "\tfor node "+mu);
            }
        }
}

