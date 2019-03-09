/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2014 George Tsatsanifos <gtsatsanifos@gmail.com>
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

package grammar;

import java.util.Map;
import java.util.Stack;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Collection;
import java.util.Comparator;

import java.text.NumberFormat;
import java.text.Format;
import java.util.Arrays;


@SuppressWarnings("unchecked") //(value={"unchecked","rawtypes","unused"})
public class DirectedGraph<NodeT> extends Graph<NodeT> {

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DirectedGraph.class);

	private Map<NodeT,TreeSet<NodeT>> backlinks; // simple backlink structure

	public DirectedGraph () {}
	public DirectedGraph (Graph<NodeT> other) {super(other);}
	public DirectedGraph (String filename, Format format, boolean isEdgeList) {super(filename,format,isEdgeList);}

	@Override public Iterator<Edge<NodeT>> iterator () {return new GraphEdgesIterable();}
        public Iterable<NodeT> reverseGraphPostorder () {return new ReverseGraphPostorderIterable(getNodes());}
	public Iterable<NodeT> topologicalOrder () {return new TopologicalSorter();}

	@Override
	public DirectedGraph<NodeT> clone () {return new DirectedGraph<>(this);}
	public DirectedGraph<NodeT> reverse () {
		DirectedGraph<NodeT> reversed = new DirectedGraph<>();
		for (Edge<NodeT> edge : this) 
                    reversed.setEdge(edge.to, edge.from, edge.weight);
		return reversed;
	}

	@Override
	public void setEdge (NodeT from, NodeT to, Float weight) {
            if (from==null || to==null)
                throw new IllegalArgumentException ("\n!! ERROR - Neither end of the edge should be null !!");
            Float previous = getEdgeWeight (from, to);
            if (!(weight.equals(Float.NaN) || previous.equals(weight))) {
		if (previous.equals (Float.NaN)) {
                    if (from.equals(to)) ++countSelfLoops;
                    if (weight<0) ++countNegativeWeightEdges;
                    ++countEdges;

                    if (backlinks==null) backlinks = new TreeMap<>();

                    if (backlinks.containsKey(to)) {
                        backlinks.get(to).add(from);
                    }else{
                        TreeSet<NodeT> preceding = new TreeSet<>();
                        preceding.add(from);
                        backlinks.put (to,preceding);
                    }

                    if (isSink(to)) sinks.add(to);
                    sinks.remove(from);
                }
                matrix.set(from,to,weight);
                resetCache();
            }
	}

	@Override
	public boolean removeEdge (NodeT from, NodeT to) {
            if (!(containsNode(to) && containsNode(from))) 
                throw new RuntimeException("!! ERROR - Both nodes should belong in the graph !!");
            Float weight = matrix.remove (from, to);
            if (!weight.equals(Float.NaN)) {
                assert (backlinks.get(to).contains(from));
                backlinks.get(to).remove(from);

		if (weight.compareTo(0.0f)<0) --countNegativeWeightEdges;
		if (from.equals(to)) --countSelfLoops;
                --countEdges;
                resetCache();
                return true;
            }else return false;
	}

	@Override
	public boolean removeNode (NodeT u) {
		if (containsNode(u)) {
			if (sinks.contains(u)) return sinks.remove(u);

			/* take care of backlinks first */
                        if (backlinks.containsKey(u)) {
                            resetCache();
                            for (NodeT preceding : backlinks.get(u)) {
                                if (matrix.mat.get(preceding).remove(u).compareTo(0.0f)<0)
                                    --countNegativeWeightEdges;
                                --countEdges;
                            }
                        }

			if (matrix.mat.containsKey(u)) {
                                resetCache();
				for (Entry<NodeT,Float> entry : matrix.mat.get(u).entrySet()) {
					if (entry.getValue()<0) --countNegativeWeightEdges;
                                        if (entry.getKey().equals(u)) --countSelfLoops;
                                }
                        }
			matrix.mat.get(u).clear();

			/* remove from node-list */
			matrix.mat.remove(u);
			return true;
		}else return false;
	}

        @Override
        public void mergeNodes (NodeT u, NodeT v) {
            if (!(containsNode(u) && containsNode(v)))
                throw new RuntimeException("!! ERROR - Both nodes must belong in the graph !!");
            /*
            if (getDegree(u) < getDegree(v)) {
                NodeT swap = u;
                u = v;
                v = swap;
            }
            */

            for (Edge<NodeT> edge : getEdgesFrom(v)) {
                if (!edge.to.equals(u)) {
                    if (containsEdge(u,edge.to)) setEdge(u,edge.to,getEdgeWeight(u,edge.to)+edge.weight);
                    else setEdge(u,edge.to,edge.weight);
                    resetCache();
                }
            }

            for (Edge<NodeT> edge : getEdgesTo(v)) {
                if (!edge.from.equals(u)) {
                    if (containsEdge(edge.from,u)) setEdge(edge.from,u,getEdgeWeight(edge.from,u)+edge.weight);
                    else setEdge(edge.from,u,edge.weight);
                    resetCache();
                }
            }

            removeNode(v);
        }
	public Collection<NodeT> getBacklinks (NodeT to) {
            return backlinks.containsKey(to)?new ArrayDeque<>(backlinks.get(to)):new ArrayDeque<NodeT>();
	}

	@Override
	public ArrayDeque<Edge<NodeT>> getEdgesTo (NodeT to) {
            ArrayDeque<Edge<NodeT>> result = new ArrayDeque<>();
            if (backlinks.containsKey(to))
                for (NodeT u : backlinks.get(to))
                    result.add(new Edge<>(u,to,getEdgeWeight(u,to)));
            return result;
	}

	public Collection<NodeT> getRoots () {return new TopologicalSorter().buffer;}

	@Override public float getDensity () {return numberEdges()/(float)(numberNodes()*(numberNodes()-1));}
	public boolean isSink (NodeT u) {return !matrix.mat.containsKey(u)||matrix.mat.get(u).isEmpty();}
	@Override public int getInDegree (NodeT u) {return backlinks.containsKey(u)?backlinks.get(u).size():0;}

	public int[] getHistOutDegree () {
		VectorSparse<Integer,Integer> histvec = new VectorSparse<> (0);
		int maxDegree=0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			int degree = getOutDegree(u);
			histvec.set (degree, histvec.get(degree)+1);
			if (degree>maxDegree)
				maxDegree = degree;
		}
		histvec.set (0,histvec.get(0)+sinks.size());

		int[] histarr = new int[maxDegree+1];
		for (Entry<Integer,Integer> entry : histvec.entrySet()) 
			histarr [entry.getKey()] = entry.getValue();
		return histarr;
	}

	public int[] getHistInDegree () {
		VectorSparse<Integer,Integer> histvec = new VectorSparse<> (0);
		int maxDegree=0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			int degree = getInDegree(u);
			histvec.set (degree, histvec.get(degree)+1);
			if (degree>maxDegree)
				maxDegree = degree;
		}
		histvec.set (0,histvec.get(0)+sinks.size());

		int[] histarr = new int[maxDegree+1];
		for (Entry<Integer,Integer> entry : histvec.entrySet()) 
			histarr [entry.getKey()] = entry.getValue();
		return histarr;
	}

	public int getMaxOutDegree () {
		int maxDegree = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			int degree = getOutDegree(u);
			if (degree > maxDegree)
				maxDegree = degree;
		}
		return maxDegree;
	}
	
	public int getMaxInDegree () {
		int maxDegree = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			int degree = getInDegree(u);
			if (degree > maxDegree)
				maxDegree = degree;
		}
		return maxDegree;
	}

	public float getAverageOutDegree () {
		float sumDegree = 0;
		for (NodeT u : getActiveNodesShallowCopy()) sumDegree += getOutDegree(u);
		return sumDegree / numberNodes();
	}

	public float getAverageInDegree () {
		float sumDegree = 0;
		for (NodeT u : getActiveNodesShallowCopy()) sumDegree += getInDegree(u);
		return sumDegree / numberNodes();
	}

	@Override
	protected long numberTriples () {
		long triples=0L;
		for (NodeT u : getActiveNodesShallowCopy())
			for (NodeT v : getNeighbors(u)) {
				Collection<NodeT> vlinks = getNeighbors(v);
				vlinks.remove(u);
				triples += vlinks.size();
			}
		return triples;
	}
/*
        protected final class PostorderIterable implements Iterator<NodeT>, Iterable<NodeT> {
		private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
		private final Stack<NodeT> stack = new Stack<>();

                public PostorderIterable (NodeT source) {marked.set(source); stack.push(source);}
                public PostorderIterable (Collection<NodeT> sources) {
                    for (NodeT source : sources) {
                        marked.set(source); 
                        stack.push(source);
                    }
                }
		public PostorderIterable () {
                    for (NodeT u : sinks)
                        if (getInDegree(u)==0) {
                            marked.set(u);
                            stack.push(u);
                        }

                    for (NodeT u : getActiveNodesShallowCopy())
                        if (getInDegree(u)==0) {
                            marked.set(u);
                            stack.push(u);
                        }
                }
                @Override public Iterator<NodeT> iterator () {return this;}
                @Override public boolean hasNext () {return !stack.isEmpty();}
                @Override public void remove () {throw new UnsupportedOperationException();}
                @Override public NodeT next () {
                    while (!stack.isEmpty()) {
			NodeT top = stack.peek();
                        boolean newElements = false;
			if (matrix.mat.containsKey(top)) {
				for (NodeT u : matrix.mat.get(top).keySet()) {
                                    if (!marked.get(u)) {
                                        marked.set(u);
					stack.push(u);

                                        newElements = true;
                                        break;
                                    }
                                }
                        }
                        if (!newElements) 
                            return stack.pop();
                    }
                    assert (false);
                    return null; // should not reach this point !!!
		}
	}
*/
	private class TopologicalSorter implements Iterator<NodeT>, Iterable<NodeT> {
		private final ArrayDeque<NodeT> buffer = new ArrayDeque<>();
		private final ArrayDeque<NodeT> sorted = new ArrayDeque<>();
		private final VectorSparse<NodeT,Integer> inputDegree = new VectorSparse<> (0);

		public TopologicalSorter () {
			for (Edge<NodeT> edge : DirectedGraph.this)
				inputDegree.set (edge.to, inputDegree.get(edge.to)+1);

			for (NodeT u : getActiveNodesShallowCopy())
				if (inputDegree.get(u)==0)
					buffer.add(u);

                        for (NodeT u : sinks)
				if (inputDegree.get(u)==0) 
					buffer.add(u);
		}
                @Override public Iterator<NodeT> iterator () {return this;} 
                @Override public void remove () {throw new UnsupportedOperationException();}
                @Override public boolean hasNext () {return !buffer.isEmpty();}
                @Override public NodeT next () {
			while (sorted.isEmpty() && !buffer.isEmpty()) {
				NodeT probed = buffer.remove();
				sorted.add (probed);
				for (NodeT adjacent : getNeighbors(probed)) {
					int fanin = inputDegree.get(adjacent);
					if (fanin>1) inputDegree.set(adjacent,fanin-1);
					else{
						inputDegree.remove(adjacent);
						buffer.add(adjacent);
					}
				}
			}
                        assert (!sorted.isEmpty());
			return sorted.remove();
		}
	}

        @Override
        public boolean containsCycles () {
            int counter = 0;
            for (NodeT u : topologicalOrder()) ++counter;
            return counter<numberNodes();
        }

	/**
	 * Counts the strongly connected components of the directed graph.
	 * @return The number of strongly connected components in the graph.
	 */
        @Override
	public int numberConnectedComponents () {
                if (isEmpty()) return 0;
		BitsetSparse<NodeT> marked = new BitsetSparse<>();
		Stack<NodeT> stack = new Stack<>();
		int counter = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			if (!marked.get(u)) { // unprocessed node
				marked.set(u);
				stack.push(u);
				while (!stack.isEmpty()) {
					NodeT v = stack.pop();
					for (NodeT w : getNeighbors(v)) {
						if (!marked.get(w) && pathExistence(w,u)) {
							marked.set(w);
							stack.push(w);
						}
					}
				}
				++counter;
			}
		}
		return counter + sinks.size();
	}

	/**
	 * Traces the strongly connected components of the directed graph
	 * @return A mapping of each node to the component it belongs.
	 */
	@Override
	public Map<NodeT,Integer> getConnectedComponents () {
		TreeMap<NodeT,Integer> components = new TreeMap<>();
		int counterid = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			if (!components.containsKey(u)) { // unprocessed node
				components.put (u,counterid);
                                for (NodeT v : DFS(u))
                                    if (!components.containsKey(v) && pathExistence(v,u))
                                        components.put (v,counterid);
				++counterid;
			}
		}
		for (NodeT v : sinks) components.put(v,counterid++);
		return components;
	}

	public Map<NodeT,Integer> gabow () {
		int counter = 0;
		TreeMap<NodeT,Integer> components = new TreeMap<>();
		for (NodeT u : getActiveNodesShallowCopy()) {
                    if (!components.containsKey(u)) {
                        TreeMap<NodeT,Integer> localcomponents = new TreeMap<>();

                        counter = gabow (u,0,counter,
                            new TreeMap<NodeT,Integer>(),
                            localcomponents,
                            new Stack<NodeT>(),new Stack<NodeT>());
                        //++counter;

                        for (Entry<NodeT,Integer> entry : localcomponents.entrySet())
                            components.put (entry.getKey(),entry.getValue());
                    }
                }

		for (NodeT v : sinks)
                    if (!components.containsKey(v))
                        components.put(v,counter++);
		return components;
	}

	private int gabow (NodeT u, int order, int counter, 
                    TreeMap<NodeT,Integer> preorder, 
                    TreeMap<NodeT,Integer> components, 
                    Stack<NodeT> forward, Stack<NodeT> backward) {

                forward.push(u);
                backward.push(u);
                preorder.put(u, order++);
		for (NodeT v : getNeighbors(u))
			if (!preorder.containsKey(v))
                            counter = gabow (v,order,counter,preorder,components,forward,backward);
			else if (!components.containsKey(v))
                            while (preorder.get(backward.peek()) > preorder.get(v))
                                backward.pop();

		if (!backward.peek().equals(u)) return counter;
		else backward.pop();

		while (!forward.isEmpty()) {
			NodeT v = forward.pop();
                        //if (!components.containsKey(v)) {
                        {
                            //System.out.println ("** gabow sets node " + v + " to cluster " + counter);
                            components.put (v,counter);
                        }
			if (u.equals(v)) break;
		}
		return ++counter;
	}

	public Map<NodeT,Integer> tarjan () {
            int counter = 0;
            Map<NodeT,Integer> components = new TreeMap<>();
            for (NodeT u : getActiveNodesShallowCopy()) {
                if (!components.containsKey(u)) {
                    counter = tarjan (u,0,counter,
                        new TreeMap<NodeT,Integer>(),
                        components,
                        new TreeMap<NodeT,Integer>(),
                        new Stack<NodeT>());
                    //++counter;
                }
            }

            for (NodeT v : sinks) 
                if (!components.containsKey(v)) 
                    components.put(v,counter++);
            return components;
	}

	private int tarjan (NodeT u, int order, int counter, 
                Map<NodeT,Integer> preorder, 
                Map<NodeT,Integer> components, 
                Map<NodeT,Integer> low, 
                Stack<NodeT> stack) {

            int min = order;
            stack.push(u);
            low.put (u,order);
            preorder.put (u,order++);
            for (NodeT v : getNeighbors(u))
                if (!preorder.containsKey(v))
                    counter = tarjan (v,order,counter,preorder,components,low,stack);
                else if (!components.containsKey(v))
                    while (low.get(v) < min)
                        min = low.get(v);

            if (min<low.get(u)) {
                low.put (u,min);
                return counter;
            }

            while (!stack.isEmpty()) {
                NodeT v = stack.pop();
                components.put (v,counter);
                low.put (v,numberNodes());
                if (u.equals(v)) break;
            }
            return ++counter;
	}

        public Map<NodeT,Integer> kosaraju () {
            BitsetSparse<NodeT> marked = new BitsetSparse<>();
            Map<NodeT,Integer> components = new TreeMap<>();
            int counter = 0;
            for (NodeT u : reversedReverseGraphPostorder()) { //reverseReverseGraphPostorder()) {
                if (!marked.get(u)) {
                    for (NodeT v : DFS(u)) {
                        if (!marked.get(v)) {
                            marked.set (v);
                            components.put (v, counter);
                        }
                    }
                    counter++;
                }
            }
            return components;
        }
        
        private Stack<NodeT> reversedReverseGraphPostorder () {
            Stack<NodeT> stack = new Stack<>();
            for (NodeT u : reverseGraphPostorder()) stack.add(u);
            return stack;
        }

	private final class ReverseGraphPostorderIterable implements Iterator<NodeT>, Iterable<NodeT> {
		private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
		private final Stack<NodeT> stack = new Stack<>();
                private final ArrayList<NodeT> buffer;

                public ReverseGraphPostorderIterable (ArrayList<NodeT> sources) {buffer=sources;}
                public ReverseGraphPostorderIterable (NodeT source) {buffer = new ArrayList<>(); buffer.add(source);}
                @Override public Iterator<NodeT> iterator () {return this;}
                @Override public boolean hasNext () {return !(buffer.isEmpty()&&stack.isEmpty());}
                @Override public void remove () {throw new UnsupportedOperationException();}
                @Override public NodeT next () {
                    while (stack.isEmpty()) {
                        if (stack.isEmpty()) {
                            while (!buffer.isEmpty()) {
                                NodeT u = buffer.remove(0);
                                if (!marked.get(u)) {
                                    marked.set(u);
                                    stack.push(u);
                                    break;
                                }
                            }
                        }
                    }

                    while (!stack.isEmpty()) {
			NodeT top = stack.peek();
                        boolean newElements = false;
			if (backlinks!=null && backlinks.containsKey(top)) {
                            for (NodeT u : backlinks.get(top)) {
                                if (!marked.get(u)) {
                                    marked.set(u);
                                    stack.push(u);

                                    buffer.remove(u);

                                    newElements = true;
                                    break;
                                }
                            }
                        }
                        if (!newElements) 
                            return stack.pop();
                    }
                    return null;
		}
	}

	/**
	 * Computes graph's optimal branchings 
	 * (Minimum Spanning Arborescence - MSA) 
	 * over arbitrary roots
	 * @return the Minimum Spanning Arborescence
	 */
	public DirectedGraph<NodeT> arborescence () {return arborescence(new ArrayDeque<NodeT>());}
	public DirectedGraph<NodeT> arborescence (Collection<NodeT> sources) {
		DirectedGraph<NodeT> branching = new DirectedGraph<>();
		if (isEmpty()) return branching;

		if (!sources.isEmpty()){
			for (NodeT source : sources) 
				for (Edge<NodeT> edge : getEdgesTo(source))
					if (edge.weight<0)
						throw new RuntimeException ("\n!! No edge with negative weight should lead to a source node !!");
		}else sources = getRoots();
		if (sources.isEmpty()) throw new RuntimeException ("\n!! Cannot compute graph arborescence without any source nodes !!");

		VectorSparse<NodeT,NodeT> paths = new VectorSparse<>();
                PriorityQueue<Edge<NodeT>> queue = new PriorityQueue<>();
		for (NodeT u : sources) {
			paths.set (u,u);
                        for (Edge<NodeT> edge : getEdgesFrom(u))
                            queue.add(edge);
		}

                while (!queue.isEmpty()) {
                    Edge<NodeT> probed = queue.poll();
                    if (paths.get(probed.to)==null) {				// process newly discovered node
                        paths.set(probed.to,probed.from);
			if (matrix.mat.containsKey(probed.to))			// append all its edges
                            for (Entry<NodeT,Float> entry : matrix.mat.get(probed.to).entrySet())
                                if (paths.get(entry.getKey())==null)
                                    queue.offer(new Edge<>(probed.to,entry.getKey(),entry.getValue()));
                    }else{
                        //if (probed.weight < getEdgeWeight (paths.get(probed.to), probed.to))
                        //    paths.set (probed.to, probed.from);         	// relaxation
                    }
                }

		for (Entry<NodeT,NodeT> pair : paths.entrySet())
			if (pair.getKey()!=pair.getValue() || containsEdge(pair.getValue(),pair.getKey()))
				branching.setEdge(pair.getValue(),pair.getKey(),getEdgeWeight(pair.getValue(),pair.getKey()));

                branching.sinks.addAll(sinks);
		return branching;
	}

	/**
	 * Edmond's algorithm for computing 
	 * the Minimum Spanning Arborescence 
	 * relying on dynamic programming 
	 * @return the Minimum Spanning Arborescence
	 */
	public DirectedGraph<NodeT> edmonds () {
		DirectedGraph<NodeT> branching = new DirectedGraph<>();
		if (isEmpty()) return branching;

		return branching;
	}

	private class GraphEdgesIterable  implements Iterator<Edge<NodeT>>, Iterable<Edge<NodeT>> {
		private final Iterator<NodeT> nodesIterator = matrix.mat.keySet().iterator();
		private Iterator<Edge<NodeT>> edgesIterator = null;
		private NodeT currentSource = null;
		private Edge<NodeT> nextEdge = null;

		public GraphEdgesIterable () {
			while (nodesIterator.hasNext()) {
				currentSource = nodesIterator.next();
				edgesIterator = getEdgesFrom (currentSource).iterator();
				while (edgesIterator.hasNext()) {
					nextEdge = edgesIterator.next();
					return;
				}
			}
		}
                @Override public boolean hasNext () {return nextEdge!=null;}
                @Override public void remove () {throw new UnsupportedOperationException();}
                @Override public Iterator<Edge<NodeT>> iterator() {return this;}
                @Override public Edge<NodeT> next () {
			Edge<NodeT> oldEdge = nextEdge;
			nextEdge = null;
			while (edgesIterator.hasNext()) {
				nextEdge = edgesIterator.next();
				return oldEdge;
			}

			while (nodesIterator.hasNext()) {
				currentSource = nodesIterator.next();
				edgesIterator = getEdgesFrom (currentSource).iterator();
				while (edgesIterator.hasNext()) {
					nextEdge = edgesIterator.next();
					return oldEdge;
				}
			}
			return oldEdge;
		}
	}

	@Override
	protected void setErdosRenyiGraph (float p, float weight) {
		sinks.addAll (getActiveNodesShallowCopy());
		TreeSet<NodeT> allNodes = new TreeSet<>(sinks);
		matrix.clear();
		for (NodeT u : allNodes)
			for (NodeT v : allNodes)
				if (!u.equals(v) && Math.random()<p) 
					setEdge (u, v, weight);
	}

	@Override
	public void printStatistics () {
		if (isSparse()) System.out.print ("---------- SPARSE DIGRAPH STATISTICS ----------");
		else System.out.print ("---------- DENSE DIGRAPH STATISTICS -----------");
		System.out.print ("\nEdges: " + numberEdges() + "  (negative: " + numberNegativeEdges() + ", self-loops: " + numberSelfLoops() + ")");
		System.out.print ("\nNodes:\t\t\t" + numberNodes());
		System.out.print ("\nRoots:\t\t\t" + getRoots().size());
		System.out.print ("\nTriangles:\t\t" + numberTriangles());
		System.out.print ("\nDensity:\t\t"+ String.format("%.5g",getDensity()));
		System.out.print ("\nGlobal clust. coef.:\t" + String.format("%.5g", getGlobalClusteringCoefficient()));
		System.out.print ("\nLocal clust. coef.:\t" + String.format("%.5g", getLocalClusteringCoefficient()));
		System.out.print ("\nConnected components:\t" + numberConnectedComponents());
		System.out.print ("\nMaximum fan-in degree:\t" + getMaxInDegree());
		System.out.print ("\nMaximum fan-out degree:\t" + getMaxOutDegree());
		System.out.print ("\nAverage fan-in degree:\t" + String.format("%.4g",getAverageInDegree()));
		System.out.print ("\nAverage fan-out degree:\t" + String.format("%.4g",getAverageOutDegree()));
		System.out.print ("\nMaximum path hops:\t" + getMaxPathHops());
		System.out.print ("\nAverage path hops:\t" + String.format("%.5g",getAveragePathHops()));
		System.out.print ("\nMaximum path cost: \t" + getMaxPathCost());
		System.out.print ("\nAverage path cost: \t" + String.format("%.5g",getAveragePathCost()));
		if (containsNegativeCycles()) System.out.print ("\nThe graph DOES contain negative cycles!");
                else if (containsCycles()) System.out.print ("\nThe graph DOES contain cycles.");
                else System.out.print ("\nThe graph contains NO cycles.");
		System.out.println ("\n-----------------------------------------------");
	}

	@Override
	public String toString () {
		StringBuilder print = new StringBuilder("############################\n## (source,target,weight) ##\n");
		for (Edge<NodeT> edge : this)
			System.out.println (edge.toString());
		System.out.println("############################\n");
		return print.toString();
	}

        @SuppressWarnings("rawtypes")
        static private class EntriesComparator implements Comparator {
            @Override
            public int compare (Object x, Object y) {
                Entry<Integer,Integer> a = (Entry<Integer,Integer>) x;
                Entry<Integer,Integer> b = (Entry<Integer,Integer>) y;

                if (a.getValue()<b.getValue()) return -1;
                else if (a.getValue()>b.getValue()) return 1;
                else return 0;
            }
        }

	public static void main(String[] args) {
		//if (args.length<1) throw new IllegalArgumentException ("\n!! User should provide a valid filename for unit testing !!");

		DirectedGraph<Long> graph = new DirectedGraph<>("/home/gtsat/toygraph2.txt",NumberFormat.getNumberInstance(),true);

                Map<Long,Integer> node2scc = graph.gabow();
                for (Entry<Long,Integer> entry : node2scc.entrySet()) {
                    System.out.println ("node "+entry.getKey() + ": cluster " + entry.getValue());
                }
                VectorSparse<Integer,Integer> size = new VectorSparse<>(0);
                for (int id : node2scc.values())
                    size.set (id, size.get(id)+1);

                Object[] entries = size.entrySet().toArray();
                EntriesComparator ec = new EntriesComparator();
                Arrays.sort (entries, ec);
                for (int i=Math.min(20,entries.length)-1; i>=0; --i)
                    System.out.println (((Entry<Integer,Integer>)entries[i]).getKey() + ": " + ((Entry<Integer,Integer>)entries[i]).getValue());

                System.out.println("-------");
                node2scc = graph.tarjan();
                for (Entry<Long,Integer> entry : node2scc.entrySet()) {
                    System.out.println ("node "+entry.getKey() + ": cluster " + entry.getValue());
                }
                size.clear();
                for (int id : node2scc.values())
                    size.set (id, size.get(id)+1);
                entries = size.entrySet().toArray();
                Arrays.sort (entries, ec);
                for (int i=Math.min(20,entries.length)-1; i>=0; --i)
                    System.out.println (((Entry<Integer,Integer>)entries[i]).getKey() + ": " + ((Entry<Integer,Integer>)entries[i]).getValue());

                System.out.println("-------");
                node2scc = graph.kosaraju();
                for (Entry<Long,Integer> entry : node2scc.entrySet()) {
                    System.out.println ("node "+entry.getKey() + ": cluster " + entry.getValue());
                }
                size.clear();
                for (int id : node2scc.values())
                    size.set (id, size.get(id)+1);
                entries = size.entrySet().toArray();
                Arrays.sort (entries, ec);
                for (int i=Math.min(20,entries.length)-1; i>=0; --i)
                    System.out.println (((Entry<Integer,Integer>)entries[i]).getKey() + ": " + ((Entry<Integer,Integer>)entries[i]).getValue());

                System.out.println("-------");
                node2scc = graph.getConnectedComponents();
                for (Entry<Long,Integer> entry : node2scc.entrySet()) {
                    System.out.println ("node "+entry.getKey() + ": cluster " + entry.getValue());
                }
                size.clear();
                for (int id : node2scc.values())
                    size.set (id, size.get(id)+1);
                entries = size.entrySet().toArray();
                Arrays.sort (entries, ec);
                for (int i=Math.min(20,entries.length)-1; i>=0; --i)
                    System.out.println (((Entry<Integer,Integer>)entries[i]).getKey() 
                            + ": " + ((Entry<Integer,Integer>)entries[i]).getValue());

                //System.exit(1);
                /**/
                graph.printStatistics();

                System.out.println("graph.reverse().topologicalOrder(): ");
                for (long u : graph.reverse().topologicalOrder())
                    System.out.print (u + " ");
                System.out.println();

                System.out.println("graph.reverse().postorder(): ");
                for (long u : graph.reverse().postorder())
                    System.out.print (u + " ");
                System.out.println();
                
                System.out.println("graph.reverseGraphPostorder(): ");
                for (long u : graph.reverseGraphPostorder())
                    System.out.print (u + " ");
                System.out.println();

                System.out.println("graph.postorder(): ");
                for (long u : graph.postorder())
                    System.out.print (u + " ");
                System.out.println();

                System.out.println("graph.preorder(): ");
                for (long u : graph.preorder())
                    System.out.print (u + " ");
                System.out.println();

                System.exit(1);
/**/
		/* test strong components computation method */
		System.out.println("!! Testing method for computing Strong Components !!");
		graph.getConnectedComponents();
		graph.gabow();


		/* test arborescence computations */
		System.out.println("!! Testing arborescence computation !!");
		DirectedGraph<Long> branching = graph.arborescence ();
		float sumBranching = 0.0f;
		for (Edge<Long> edge : branching)
			sumBranching += edge.weight;
                System.out.println("!! Summed branching weight " + sumBranching + " !!");


		/* test sssp computation methods */
		System.out.println("!! Testing methods for computing APSPs & SSSPs !!");
		Map<Long,Graph<Long>> johnson = graph.johnson();
		Map<Long,Graph<Long>> floyd = graph.floyd();
		for (long u : graph.getNodes()) {
			float sssp1sum=0.0f;
			for (Edge<Long> edge : graph.dijkstra(u)) sssp1sum += edge.weight;

			float sssp2sum=0.0f;
			for (Edge<Long> edge : graph.bellman(u)) sssp2sum += edge.weight;

			float sssp4sum=0.0f;
			for (Edge<Long> edge : graph.sssp(u)) sssp4sum += edge.weight;

			float sssp5sum=0.0f;
			for (Edge<Long> edge : ((DirectedGraph<Long>)johnson.get(u)).sssp(u)) sssp5sum += edge.weight;

			float sssp6sum=0.0f;
			for (Edge<Long> edge : ((DirectedGraph<Long>)floyd.get(u)).sssp(u)) sssp6sum += edge.weight;

			if (sssp1sum!=sssp2sum || sssp2sum!=sssp4sum || sssp4sum!=sssp5sum || sssp5sum!=sssp6sum) 
				throw new RuntimeException("\n!! Different computations do not provide equivalent solutions ("+
					sssp1sum+"/"+sssp2sum+"/"+sssp4sum+"/"+sssp5sum+"/"+sssp6sum+") !!");
		}


		/* test random digraphs */
		System.out.println("!! Testing random digraph !!");
		int n = 100;
		DirectedGraph<Integer> random = new DirectedGraph<>();
		for (int i=0; i<n; ++i) random.addNode (i);

		random.setErdosRenyiGraph ((float)Math.log10(n)/n);
                random.printStatistics();


		/* test strong components computation method */
		System.out.println("!! Testing method for computing Strong Components !!");
		random.getConnectedComponents();


                /* test arborescence computations */
		System.out.println("!! Testing arborescence computation !!");
		DirectedGraph<Integer> rbranching = random.arborescence ();
		float sumRBranching = 0.0f;
		for (Edge<Integer> edge : rbranching)
			sumRBranching += edge.weight;
                System.out.println("!! Summed branching weight " + sumRBranching + " !!");


		/* test sssp computation methods */
		System.out.println("!! Testing methods for computing APSPs & SSSPs !!");
		Map<Integer,Graph<Integer>> rjohnson = random.johnson();
		Map<Integer,Graph<Integer>> rfloyd = random.floyd();

		for (int u : random.getNodes()) {
			float sssp1sum=0.0f;
			for (Edge<Integer> edge : random.dijkstra(u)) sssp1sum += edge.weight;

			float sssp2sum=0.0f;
			for (Edge<Integer> edge : random.bellman(u)) sssp2sum += edge.weight;

			float sssp4sum=0.0f;
			for (Edge<Integer> edge : random.sssp (u)) sssp4sum += edge.weight;

			float sssp5sum=0.0f;
			for (Edge<Integer> edge : ((DirectedGraph<Integer>)rjohnson.get(u)).sssp(u)) sssp5sum += edge.weight;

			float sssp6sum=0.0f;
			for (Edge<Integer> edge : ((DirectedGraph<Integer>)rfloyd.get(u)).sssp(u)) sssp6sum += edge.weight;

			if (sssp1sum!=sssp2sum || sssp2sum!=sssp4sum || sssp4sum!=sssp5sum || sssp5sum!=sssp6sum) 
				throw new RuntimeException ("\n!! Different computations do not provide equivalent solutions (" + 
					sssp1sum + "/" + sssp2sum + "/" + sssp4sum + "/" + sssp5sum + "/" + sssp6sum + ") !!");
		}
                
                System.out.println("!! Testing random digraphs !!");
                for (int i=0; i<n; ++i) random.addNode(i);

                random.setErdosRenyiGraph ((float)Math.log(n)/n);
                random.printStatistics();

                Map<Integer,Integer> gmap = random.gabow();

		random.draw (800,600);
                System.out.println ("!! Done with test for directed graphs !!");
                
                random.printGraph();
	}
}
