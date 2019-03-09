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

import oscp.Group;
import grammar.Edge;
import grammar.Graph;
import grammar.MaxPQ;
import grammar.MinPQ;
import java.util.Set;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;

@SuppressWarnings(value={"unchecked","rawtypes"})
public class ComputeMeetingLocation
{
    
    static private int maxHeapSize = 0;
    static public void resetStats () {maxHeapSize=0;}
    static public int getMaxMemRequirements () {return maxHeapSize;}

    static private Hashtable cachedDistances = new Hashtable();

    public static<NodeT> LinkedList<NodeT> computeOMP_Dijkstra (Group<NodeT> group,Graph<NodeT> graph,boolean isMinSum,int k) {
        LinkedList<NodeT> topOMP = new LinkedList<NodeT>();
        LinkedList<Float> topScores = new LinkedList<Float>();
        
        int numSources = group.getSubgroups().size();
        NodeT omp = group.getTarget();
        Map<NodeT,Float>[] ds = new HashMap [numSources];
        Map<NodeT,Float> dt = new Hashtable<NodeT,Float>();
        for (int i=0; i<numSources; i++){
            ds[i] = new HashMap<NodeT,Float> ();
            ds[i] = graph.dijkstraDistances(group.getSubgroups().get(i).getTarget()); 
        }

        dt = new HashMap<NodeT,Float>();
        dt = graph.dijkstraDistances(group.getTarget());
        if(isMinSum){
            Float minSum = Float.POSITIVE_INFINITY;
            for (NodeT node: ds[0].keySet()) {
                float sum = 0;
                for (int i=0 ; i<ds.length; ++i)
                    sum += ds[i].get(node);

                float product = 1.0f;
                for(float lambda : group.getLambdas().values())
                    product *= (lambda * dt.get(node));

                sum += product;
                
                
                int index = 0;
                for(Float score : topScores) {
                    if (sum < score.floatValue()) {
                        topScores.add(index, sum);
                        topOMP.add(index, node);
                        break;
                    }
                    index++;
                }
                if (topScores.size() > k) {
                    topScores.removeLast();
                    topOMP.removeLast();
                }else if (topScores.size() == 0) {
                    topScores.addFirst(sum);
                    topOMP.addFirst(node);
                }
        
            }
        }else{
            Float minMax = Float.POSITIVE_INFINITY;
            for (NodeT node: ds[0].keySet()) {
                float maxDS = 0;
                for (int i = 0 ; i< (ds.length); ++i)
                    if (maxDS< ds[i].get(node).longValue())
                            maxDS = ds[i].get(node);

                float product = 1.0f;
                for(float lambda : group.getLambdas().values())
                    product *= (lambda * dt.get(node));

                maxDS += product;
              
                int index = 0;
                for(Float score : topScores) {
                    if (maxDS < score.floatValue()) {
                        topScores.add(index, maxDS);
                        topOMP.add(index, node);
                        break;
                    }
                    index++;
                    
                }
                if (topScores.size() > k) {
                    topScores.removeLast();
                    topOMP.removeLast();
                }else if (topScores.size() == 0){
                    topScores.addFirst(maxDS);
                    topOMP.addFirst(node);
                }
            }
        }
        return topOMP;
    }

    public static<NodeT> LinkedList<NodeT> computeOMP (Group<NodeT> group, Graph<NodeT> graph, boolean isMinSum, int k) {
        int numSources = group.getSubgroups ().size ();
        Hashtable<NodeT,Float>[] ds = new Hashtable[numSources];
        Hashtable<NodeT,Float>[] dt = new Hashtable[numSources];
        for (int i=0 ; i<numSources ; i++) {
            ds[i] = new Hashtable<NodeT,Float>();
            dt[i] = new Hashtable<NodeT,Float>();
            computeForwardBackwardCost_Iterative(graph,group.getSubgroups().get(i).getTarget(),group.getTarget(),ds[i],dt[i]);
        }
        return isMinSum?getOMP_MinSum(dt,ds,group.getTarget(),group, k):getOMP_MinMax(dt,ds,group.getTarget(),group, k,graph);
    }


    public static<NodeT> LinkedList<NodeT> computeOMP_recursive (Group<NodeT> group, Graph<NodeT> graph, boolean isMinSum, int k) {
        int numSources = group.getSubgroups ().size ();
        Hashtable<NodeT,Float>[] ds = new Hashtable[numSources];
        Hashtable<NodeT,Float>[] dt = new Hashtable[numSources];
        for (int i = 0 ; i< numSources ; i++) {
            ds [i] = new Hashtable<NodeT,Float>();
            dt [i] = new Hashtable<NodeT,Float>();
            computeForwardBackwardCost_Recursive (graph,group.getSubgroups().get(i).getTarget(),group.getTarget(),
                                                  group.getSubgroups().get(i).getTarget(),ds[i],dt[i]);
        }
        return isMinSum?getOMP_MinSum(dt,ds,group.getTarget(),group, k):getOMP_MinMax(dt,ds,group.getTarget(),group, k,graph);
    }

	private static class ScoreComparator<NodeT> implements Comparator<NodeT> {

		private Map<NodeT,Float> allScores;
		public ScoreComparator (Map<NodeT,Float> scores) {allScores=scores;}
		public int compare (NodeT x,NodeT y) {
			if (!allScores.containsKey(x) && !allScores.containsKey(y)) return 0;
			else if (!allScores.containsKey(y)) return -1;
			else if (!allScores.containsKey(x)) return 1;
			else{
				float balance = allScores.get(x) - allScores.get(y);
				if (balance<0) return -1;
				else if (balance>0) return 1;
				else return 0;
			}
		}
	}

    private static<NodeT> LinkedList<NodeT> getOMP_MinSum (Hashtable<NodeT,Float>[] dt,
                                               Hashtable<NodeT,Float>[] ds,
                                               NodeT target,
                                               Group<NodeT> group, int k) {

        LinkedList<NodeT> topOMP = new LinkedList<NodeT>();
        LinkedList<Float> topScores = new LinkedList<Float>();

	Map<NodeT,Float> allScores = new HashMap<>();
	MaxPQ<NodeT> topNodes = new MaxPQ<>(new ScoreComparator<>(allScores));


        int numSources = group.getSubgroups().size();
        Set<NodeT> ts = ds[0].keySet(); //new TreeSet<>();
	ts.retainAll (dt[0].keySet());
        for (int i=1 ; i<numSources ; ++i) {
            ts.retainAll (ds[i].keySet());
            ts.retainAll (dt[i].keySet());
        }

        NodeT omp = target;
        Float minSum = Float.POSITIVE_INFINITY;

        for (NodeT node : ts) {
            boolean isInAllTables = true;
/*
            for (int i=0; isInAllTables && i<numSources; ++i)
                if (!dt[i].containsKey(node))
                    isInAllTables = false;
*/
            float relaxation = 0.0f;
            if (isInAllTables) {
                float sum = 0;
                float minDT = Float.POSITIVE_INFINITY;
                for (int i = 0 ; i<numSources ; ++i) {
                    float selfrelax = 0.0f;
                    sum += ds[i].get(node) * group.getSubgroups().get(i).getLambdas().get(group.getSubgroups().get(i));

                    if (dt[i].get(node) < minDT)
                        minDT = dt[i].get (node);

                    //for (Group<NodeT> overtraveler : group.getSubgroups()) { //group.getTravelers()) {
                    for (Group<NodeT> overtraveler : group.getTravelers()) {
                        float product = 1.0f;
                        //for (Group<NodeT> undertraveler : group.getSubgroups()) //group.getTravelers()) 
                        for (Group<NodeT> undertraveler : group.getTravelers()) 
				if (!overtraveler.equals(undertraveler))
                                    if (!overtraveler.getLambdas().containsKey(undertraveler))
                                        throw new RuntimeException ("\n!! ERROR - Traveler "+overtraveler.hashCode()+" is not associated with traveler "+undertraveler.hashCode()+" !!");
	                            else product *= overtraveler.getLambdas().get(undertraveler);
                        relaxation += product;
                    }
                }

                //float product = 1.0f;
                //for(float lambda : group.getLambdas().values())
                //     product *= (lambda*minDT);
                sum += minDT * relaxation;

		allScores.put (node,sum);
		if (topNodes.size()<k) topNodes.insert (node);
		else if (sum<allScores.get(topNodes.max())){
			topNodes.delMax();
			topNodes.insert(node);
		}
/*
                int index = 0;
                for(Float score : topScores) {
                    if (sum < score.floatValue()) {
                        topScores.add (index, sum);
                        topOMP.add (index, node);
                        break;
                    }
                    index++;
                }
                if (topScores.size() > k){
                    topScores.removeLast();
                    topOMP.removeLast();
                }
                else if (topScores.size() == 0){
                    topScores.addFirst (sum);
                    topOMP.addFirst (node);
                }
*/
            }
        }
	while (!topNodes.isEmpty())
		topOMP.addFirst (topNodes.delMax());
        return topOMP;
    }


    private static<NodeT> LinkedList<NodeT> getOMP_MinMax (Hashtable<NodeT,Float>[] dt,
                                               Hashtable<NodeT,Float>[] ds,
                                               NodeT target,
                                               Group<NodeT> group, int k,
                                               Graph<NodeT> graph) {
        LinkedList<NodeT> topOMP = new LinkedList<NodeT>();
        LinkedList<Float> topScores = new LinkedList<Float>();
        
	Map<NodeT,Float> allScores = new HashMap<>();
	MaxPQ<NodeT> topNodes = new MaxPQ<>(new ScoreComparator<>(allScores));

        int numSources = group.getSubgroups().size();
        Set<NodeT> ts = ds[0].keySet(); //new TreeSet<>();
        ts.retainAll (dt[0].keySet());
        for (int i=1; i<numSources; ++i) {
            ts.retainAll (ds[i].keySet());
            ts.retainAll (dt[i].keySet());
	}

        NodeT omp = target;
        Float minMax = Float.POSITIVE_INFINITY;
        for (NodeT node : ts) {
            boolean isInAllTables = true;
/*
            for (int i=0; isInAllTables && i<numSources; ++i)
                if (!dt [i].containsKey (node))
                    isInAllTables = false;
*/
            if (isInAllTables) {
                float maxDS = 0;
                float relaxation = 0.0f;
                float minDT = Float.POSITIVE_INFINITY;
                ArrayDeque<Group<NodeT>> allTravelers = group.getTravelers();
                for (int i = 0 ; i<numSources ; ++i) {
/*
                    Map<Group<NodeT>,Float> precosts = new HashMap<Group<NodeT>,Float>();
                    Map<Group<NodeT>,ArrayDeque<Float>> preweights = new HashMap<Group<NodeT>,ArrayDeque<Float>>();
                    Map<Group<NodeT>,ArrayDeque<Group<NodeT>>> prepaths = new HashMap<Group<NodeT>,ArrayDeque<Group<NodeT>>>();
                    group.getSubgroups().get(i).getTravelersCosts (precosts,preweights,prepaths);

                    for (Map.Entry<Group<NodeT>,Float> entry : precosts.entrySet()) {
                        Group<NodeT> traveler = entry.getKey();
                        float precost = entry.getValue();
                        float dscost = preweights.get(traveler).getLast() * ds[i].get(node);
                        
                        float newweight = 1.0f;
                        for (Group<NodeT> othertraveler : allTravelers)
                            newweight *= traveler.getLambdas().get(othertraveler);
                        float dtcost = dt[i].contains(node)?newweight*dt[i].get(node):0.0f; //Float.MAX_VALUE;
                        
                        if (precost + dscost + dtcost > maxDS) 
                            maxDS = precost + dscost + dtcost;
                    }
*/
                    /**/
                    float lambda = group.getSubgroups().get(i).getLambdas().get(group.getSubgroups().get(i));
                    if (lambda*ds [i].get (node) > maxDS)
                        maxDS = lambda * ds[i].get(node);
                    if (dt[i].get(node) < minDT)
                        minDT = dt[i].get(node);
                    
                    float product = 1.0f;
                    for (Group<NodeT> subgroup : group.getTravelers()) //group.getSubgroups())
				if (!subgroup.equals(group.getSubgroups().get(i)))
                        		product *= group.getSubgroups().get(i).getLambdas().get(subgroup);
                    relaxation += product;
                    /**/
                }
/**/
                float product = 1.0f;
                for(float lambda : group.getLambdas().values())
                    product *= (lambda * minDT);

                maxDS += relaxation * minDT; //product;
/**/
                allScores.put (node,maxDS);
                if (topNodes.size()<k) topNodes.insert (node);
                else if (maxDS<allScores.get(topNodes.max())){
                        topNodes.delMax();
                        topNodes.insert(node);
                }
/*
                int index = 0;
                for(Float score : topScores) {
                    if (maxDS < score.floatValue()) {
                        topScores.add(index, maxDS);
                        topOMP.add(index, node);
                        break;
                    }
                    index++;
                }
                if (topScores.size() > k) {
                    topScores.removeLast();
                    topOMP.removeLast();
                }else if (topScores.size() == 0){
                    topScores.addFirst(maxDS);
                    topOMP.addFirst(node);
                }
*/
            }
        }
	while (!topNodes.isEmpty())
		topOMP.addFirst (topNodes.delMax());
        return topOMP;
    }


    private static<NodeT> void computeForwardBackwardCost_Iterative (Graph<NodeT> graph,
                                                                     NodeT source,NodeT target,
                                                                     Hashtable<NodeT,Float> dsTable,
                                                                     Hashtable<NodeT,Float> dtTable) {
        dsTable.put (source, new Float (0));
        dtTable.put (target, new Float (0));
        Stack<NodeT> stack = new Stack<NodeT>();
        stack.push (source);

        NodeT to;
        NodeT from;
        float weight;
        while (!stack.isEmpty ()) {
            NodeT top = stack.pop ();
            for (Edge<NodeT> edge : graph.getEdgesFrom (top)) {
                to = edge.to;
                from = edge.from;
                weight = graph.getEdgeWeight (from, to);

                if ((!dsTable.containsKey (to) || (dsTable.get (to) > dsTable.get (from) + weight))) {
                    dsTable.put (to, weight + dsTable.get (from));
                    if (!to.equals (target)) stack.push (to);
                }
                if (dtTable.containsKey (to) && (((!dtTable.containsKey (from) || (dtTable.get (from) > dtTable.get (to) + weight))))) {
                    dtTable.put (from, weight + dtTable.get (to));
                    if (!from.equals (target) && !stack.contains (to)) stack.push (from);
                }
                if (dtTable.containsKey (from) && ((!dtTable.containsKey (to)) || (dtTable.get (to) > (weight + dtTable.get (from))))) {
                    dtTable.put (to, weight + dtTable.get (from));
                    if (!from.equals (target) && !stack.contains (to)) stack.push (to);
                }
                if (stack.size()>maxHeapSize) maxHeapSize = stack.size();
            }
        }
    }


    private static<NodeT> void computeForwardBackwardCost_Recursive (Graph<NodeT> graph,
                                                                     NodeT source,NodeT target,NodeT u,
                                                                     Hashtable<NodeT,Float> dsTable,
                                                                     Hashtable<NodeT,Float> dtTable) {
        if (u.equals (source)) dsTable.put (u, new Float (0));
        for (Edge<NodeT> edge: graph.getEdgesFrom (u)) {
            NodeT to = edge.to;
            NodeT from = edge.from;
            float weight = edge.weight;
            if ((!dsTable.containsKey (to) || (dsTable.get (to)>dsTable.get(from)+weight))) {
                dsTable.put (to,weight+dsTable.get(from));
                computeForwardBackwardCost_Recursive (graph,source,target,to,dsTable,dtTable);
            }
        }
        if (u.equals(target)) dtTable.put (u,new Float (0));
        for (Edge<NodeT> edge:graph.getEdgesTo (u)) {
            NodeT to = edge.to;
            NodeT from = edge.from;
            float weight = graph.getEdgeWeight (from, to);
            if (dtTable.containsKey (to) && (((!dtTable.containsKey (from) || (dtTable.get (from) > dtTable.get (to) + weight))))) {
                dtTable.put (from, weight + dtTable.get (to));
                computeForwardBackwardCost_Recursive (graph, source, target, from, dsTable, dtTable );
            }
        }
    }


    public static<NodeT> LinkedList<NodeT> computeOMPfiltered (Group<NodeT> group, Graph<NodeT> graph, boolean isMinSum, int k) {
        Hashtable<NodeT,Hashtable<NodeT,Float>> newCachedDistances = new Hashtable<NodeT,Hashtable<NodeT,Float>>();

        int numSources = group.getSubgroups().size();
        Hashtable<NodeT,Float>[] ds = new Hashtable[numSources];
        Hashtable<NodeT,Float>[] dt = new Hashtable[numSources];
        float maxDistance = 0.0f;
        int mostDistant = 0;
        float sumthreshold = 0.0f;
        float maxthreshold = Float.MAX_VALUE;
        for (int i=0 ; i<numSources ; i++) {
            float sumrelax = 0.0f;
            float minrelax = Float.MAX_VALUE;
            for (Group<NodeT> overtraveler : group.getSubgroups().get(i).getTravelers()) {
                float prodrelax = 1.0f; //group.getSubgroups().get(i).getLambdas().get(group.getSubgroups().get(i));
                //for (Group<NodeT> undertraveler : group.getTravelers()) //!!!!!!!!
                for (Group<NodeT> undertraveler : group.getSubgroups().get(i).getTravelers()) 
                        prodrelax *= overtraveler.getLambdas().get(undertraveler);
                sumrelax += overtraveler.getLambdas().get(overtraveler) * prodrelax;
                if (prodrelax<minrelax) minrelax = prodrelax;

		if (prodrelax<maxthreshold) maxthreshold = prodrelax;
            }

            ds[i] = new Hashtable<NodeT,Float>();
            computeDistancesFiltered(graph,group.getSubgroups().get(i).getTarget(),group.getTarget(),0,Float.MAX_VALUE,ds[i],false);
            //computeDistancesFiltered(graph,group.getSubgroups().get(i).getTarget(),group.getTarget(),maxthreshold,Float.MAX_VALUE,ds[i],false);
            if (ds[i].contains(group.getTarget()) && ds[i].get(group.getTarget())>maxDistance) {
                maxDistance = ds[i].get(group.getTarget());
                mostDistant = i;
            }

            newCachedDistances.put (group.getSubgroups().get(i).getTarget(),ds[i]);

            //System.out.println (ds[i].size()+"/"+graph.numberNodes());

            if (ds[i].contains(group.getTarget()))
            sumthreshold += ds[i].get(group.getTarget())/sumrelax;
            //if (ds[i].get(group.getTarget())/minrelax < maxthreshold) 
            //    maxthreshold = ds[i].get(group.getTarget())/minrelax;
        }

        dt[0] = new Hashtable<NodeT,Float>();
        //computeDistancesFiltered(graph,group.getTarget(),group.getSubgroups().get(mostDistant).getTarget(),maxthreshold,Float.MAX_VALUE,dt[0],true);
        computeDistancesFiltered(graph,group.getTarget(),group.getSubgroups().get(mostDistant).getTarget(),0,Float.MAX_VALUE,dt[0],true);
        //System.out.println (ds[0].size()+"/"+graph.numberNodes()+"\n-----");

        newCachedDistances.put (group.getTarget(),dt[0]);
	cachedDistances = newCachedDistances;

        for (int i=1; i<numSources; i++) dt[i] = dt[0];

        return isMinSum?getOMP_MinSum(dt,ds,group.getTarget(),group,k):getOMP_MinMax(dt,ds,group.getTarget(),group,k,graph);
    }

    private static<NodeT> void computeDistancesFiltered (Graph<NodeT> graph,
                                                        NodeT source,NodeT target,
                                                        float relaxation,float threshold,
                                                        Hashtable dTable,boolean reverse) {

        if (source==null) throw new IllegalArgumentException("!! ERROR - Source node not allowed to be null !!");
        if (cachedDistances.contains((Object)source)) dTable = (Hashtable)cachedDistances.get(source);
        else{
            float diw = Float.MAX_VALUE;

            MinPQ<Edge<NodeT>> heap = new MinPQ<Edge<NodeT>>();
            heap.insert (new Edge<NodeT>(source,source,0.0f));
            dTable.put (source, 0.0f);
            while (!heap.isEmpty () && relaxation*heap.min().weight<diw && heap.min().weight<threshold) {
                Edge<NodeT> top = heap.delMin();
                if (top.to.equals(target)) diw = top.weight;
                if (reverse) {
                    for (Edge<NodeT> edge : graph.getEdgesTo(top.from)) {
                        if (!dTable.containsKey(edge.from) || top.weight+edge.weight<(Float)dTable.get(edge.from)){
                            dTable.put (edge.from, top.weight+edge.weight);
                            heap.insert (new Edge<NodeT>(edge.from,edge.to,top.weight+edge.weight));
                        }
                    }
                }else{
                    for (Edge<NodeT> edge : graph.getEdgesFrom(top.to)) {
                        if (!dTable.containsKey(edge.to) || top.weight+edge.weight<(Float)dTable.get(edge.to)){
                            dTable.put (edge.to, top.weight+edge.weight);
                            heap.insert (new Edge<NodeT>(edge.from,edge.to,top.weight+edge.weight));
                        }
                    }
                }
            }
        }
    }
}

