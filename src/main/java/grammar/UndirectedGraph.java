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

import java.util.Stack;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.PriorityQueue;

import java.text.Format;
import java.text.NumberFormat;
import org.apache.log4j.Logger;


@SuppressWarnings("unchecked") //(value={"unchecked","rawtypes","unused"})
public final class UndirectedGraph<NodeT> extends Graph<NodeT> {

	private static final Logger logger = Logger.getLogger (UndirectedGraph.class);

	public UndirectedGraph (UndirectedGraph<NodeT> other) {super ((Graph<NodeT>)other);}
	public UndirectedGraph (String filename, Format format, boolean isEdgeList) {super(filename,format,isEdgeList);}
	public UndirectedGraph () {}

	@Override
	public UndirectedGraph<NodeT> clone () {return new UndirectedGraph<>(this);}
	public DirectedGraph<NodeT> toDirectedGraph () {return new DirectedGraph<>(this);} // each arc replaced by two directed edges

	@Override public Iterator<Edge<NodeT>> iterator () {return new GraphEdgesIterable();}

	@Override
	public void setEdge (NodeT from, NodeT to, Float weight) {
            if (from==null || to==null)
                throw new IllegalArgumentException ("\n!! ERROR - Neither end of the edge should be null !!");
            Float previous = getEdgeWeight (from, to);
            if (!weight.equals(Float.NaN) && !previous.equals(weight)) {
		if (previous.equals(Float.NaN)) {
                    if (weight<0) ++countNegativeWeightEdges;
                    if (from.equals(to)) ++countSelfLoops;
                    ++countEdges;

                    sinks.remove(from);
                    sinks.remove(to);
                }else if (weight<0 && previous>=0) {
                    ++countNegativeWeightEdges;
                }
		matrix.set(from,to,weight);
		matrix.set(to,from,weight);
                resetCache();
            }
	}

	@Override
	public boolean removeEdge (NodeT from, NodeT to) {
		Float weightFromTo = matrix.remove (from, to);
		Float weightToFrom = matrix.remove (to, from);
		if (!weightFromTo.equals(weightToFrom))
			throw new RuntimeException ("\n!! Detection of a non-symmetric matrix for representing an undirected graph !!");

		if (!weightFromTo.equals(Float.NaN)) {
			--countEdges;
			if (weightFromTo.compareTo(0.0f)<0) 
				--countNegativeWeightEdges;
			if (from.equals(to))
				--countSelfLoops;
                        resetCache();
			return true;
		}else return false;
	}

	@Override
	public boolean removeNode (NodeT u) {
		if (containsNode(u)) {
			if (sinks.contains(u)) return sinks.remove(u);

			for (Entry<NodeT,Float> entry :  matrix.mat.get(u).entrySet()) {
				assert (matrix.mat.get(entry.getKey()).containsKey(u));
                                --countEdges;
                                if (entry.getKey().equals(u)) --countSelfLoops;
				if (matrix.mat.get(entry.getKey()).remove(u).compareTo(0.0f)<0)
					--countNegativeWeightEdges;
                                resetCache();
			}
			matrix.mat.get(u).clear();
			matrix.mat.remove(u);
			return true;
		}else return false;
	}

        @Override
        public void mergeNodes (NodeT u, NodeT v) {
            if (!(containsNode(u) && containsNode(v)))
                throw new RuntimeException("!! ERROR - Cannot merge nodes that do not belong in the graph !!");
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
            removeNode(v);
        }
	
	@Override
	public ArrayDeque<Edge<NodeT>> getEdgesTo (NodeT to) {
		ArrayDeque<Edge<NodeT>> result = new ArrayDeque<>();
		if (matrix.mat.containsKey(to))
			for (Entry<NodeT,Float> entry : matrix.mat.get(to).entrySet())
				result.add (new Edge<>(entry.getKey(),to,entry.getValue()));
		return result;
	}
	public boolean hasEdges (NodeT u) {return matrix.mat.containsKey(u) && !matrix.mat.get(u).isEmpty();}
	public int getDegree (NodeT u) {return matrix.mat.containsKey(u)?matrix.mat.get(u).size():0;}
        @Override public int getInDegree (NodeT u) {return getDegree(u);}

	@Override public float getDensity () {return 2*numberEdges()/(float)(numberNodes()*(numberNodes()-1));}

	public int[] getHistDegree () {
		VectorSparse<Integer,Integer> histvec = new VectorSparse<>(0);
		int maxDegree=0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			int degree = getDegree(u);
			histvec.set (degree, histvec.get(degree)+1);
			if (degree>maxDegree)
				maxDegree = degree;
		}
		histvec.set (0,sinks.size());

		int[] histarr = new int [maxDegree+1];
		for (Entry<Integer,Integer> entry : histvec.entrySet()) 
			histarr [entry.getKey()] = entry.getValue();
		return histarr;
	}

	public int getMaxDegree () {
		int maxDegree = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			int degree = getDegree(u);
			if (degree > maxDegree)
				maxDegree = degree;
		}
		return maxDegree;
	}
	
	public float getAverageDegree () {
		float sumDegree = 0;
		for (NodeT u : getActiveNodesShallowCopy()) 
			sumDegree += getDegree(u);
		return sumDegree / numberNodes();
	}

	@Override
	protected long numberTriples () {
		long triples=0L;
		for (NodeT u : getActiveNodesShallowCopy()) 
                    triples += numberNeighbors(u)*(numberNeighbors(u)-1)>>1;
		return triples;
	}

	@Override
	public long numberTriangles () {
		long triangles=super.numberTriangles();
		assert (triangles%6L==0L);
		return triangles/6L;
	}

        @Override
        public boolean containsCycles () {
            BitsetSparse<NodeT> marked = new BitsetSparse<>();
            Stack<NodeT> stack = new Stack<>();
            for (NodeT v : getActiveNodesShallowCopy()) {
                if (!marked.get(v)) {
                    stack.push(v);
                    while (!stack.isEmpty()) {
                        NodeT top = stack.pop();
                        if (matrix.mat.containsKey(top)) {
                            for (NodeT u : matrix.mat.get(top).keySet()) {
                                if (marked.get(u)){ 
                                    return true;
                                }else{
                                    marked.set(u);
                                    stack.push(u);
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
/*
        protected final class PostorderIterable implements Iterator<NodeT>, Iterable<NodeT> {
		private final BitsetSparse<NodeT> marked = new BitsetSparse<>();
		//private final Stack<NodeT> stack = new Stack<>();

                private boolean processedActiveNodes = false;
                private Iterator<NodeT> nodesIterator;
                private NodeT next;

                private Iterator<NodeT> postorderIterator;

		public PostorderIterable () {
                    if (!getActiveNodesShallowCopy().isEmpty()) {
                        nodesIterator = getActiveNodesShallowCopy().iterator();
                    }else{
                        processedActiveNodes = true;
                        nodesIterator = sinks.iterator();
                    }
                    next();
                }
                public PostorderIterable (NodeT source) {
                    processedActiveNodes = true;

                    ArrayDeque<NodeT> tmp = new ArrayDeque<> ();
                    tmp.add(source);
                    nodesIterator = tmp.iterator();
                    next();
                }
                @Override public Iterator<NodeT> iterator () {return this;}
                @Override public boolean hasNext () {return next!=null;}
                @Override public void remove () {throw new UnsupportedOperationException();}
                @Override public NodeT next () {
                    NodeT previous = next;
                    next = null;
                    
                    if (postorderIterator!=null && postorderIterator.hasNext()) {
                        next = postorderIterator.next();
                        marked.set (next);
                        return previous;
                    }

                    while (nodesIterator.hasNext()) {
                        NodeT source = nodesIterator.next();
                        while (marked.get(source))
                            if (nodesIterator.hasNext()) source = nodesIterator.next();
                            else return previous;

                        postorderIterator = reverseDFSiterator(source).iterator();
                        if (postorderIterator.hasNext()) {
                            next = postorderIterator.next();
                            marked.set (next);
                            return previous;
                        }
                    }

                    if (!processedActiveNodes) {
                        processedActiveNodes = true;
                        nodesIterator = sinks.iterator();
                        next = nodesIterator.next();
                        while (marked.get(next))
                            if (nodesIterator.hasNext()) next = nodesIterator.next();
                            else return previous;
                        marked.set (next);
                    }
                    
                    return previous;
                }
	}
*/
	/**
	 * Computes the weakly connected components of the graph.
	 * @return The number of weakly connected components in the graph.
	 */
        @Override
	public int numberConnectedComponents () {
                if (isEmpty()) return 0;
                if (getActiveNodesShallowCopy().isEmpty()) return sinks.size();
		BitsetSparse<NodeT> marked = new BitsetSparse<>();
		Stack<NodeT> stack = new Stack<>();
		int counter = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			if (!marked.get(u)) {
				marked.set(u);
				stack.push(u);
				while (!stack.isEmpty()) {
					NodeT v = stack.pop();
					for (NodeT w : getNeighbors(v)) {
						if (!marked.get(w)) {
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
	 * Computes the weakly connected components of the graph.
	 * @return A mapping of each node to the component it beIntegers.
	 */
	@Override
	public Map<NodeT,Integer> getConnectedComponents () {
		Map<NodeT,Integer> components = new TreeMap<>();
		Stack<NodeT> stack = new Stack<>();
		int counterid = 0;
		for (NodeT u : getActiveNodesShallowCopy()) {
			if (!components.containsKey(u)) {
				components.put(u,counterid);
				stack.push(u);
				while (!stack.isEmpty()) {
					NodeT v = stack.pop();
					for (NodeT w : getNeighbors(v)) {
						if (!components.containsKey(w)) {
							components.put(w,counterid);
							stack.push(w);
						}
					}
				}
				++counterid;
			}
		}
		for (NodeT v : sinks) 
			components.put(v,counterid++);
		return components;
	}

	public UndirectedGraph<NodeT> kruskal () {
		UndirectedGraph<NodeT> mst = new UndirectedGraph<>();
		if (isEmpty()) return mst;

		WQUF<NodeT> uf = new WQUF<> ();
		PriorityQueue<Edge<NodeT>> edges = new PriorityQueue<>(numberEdges());
		for (Edge<NodeT> edge : this){
			uf.union (edge.from,edge.to);
			edges.offer (edge);
		}

		WQUF<NodeT> mstuf = new WQUF<> ();
		while (!edges.isEmpty()) {
			Edge<NodeT> edge = edges.poll();
			if (uf.connected(edge.from, edge.to) && !mstuf.connected(edge.from, edge.to)) {
				mst.setEdge (edge.from, edge.to, edge.weight); 
				mstuf.union (edge.from, edge.to);
			}
		}
                mst.sinks.addAll(sinks);
		return mst;
	}

	public UndirectedGraph<NodeT> prim () {
		UndirectedGraph<NodeT> mst = new UndirectedGraph<>();
		if (isEmpty()) return mst;

		VectorSparse<NodeT,NodeT> paths = new VectorSparse<>();
                PriorityQueue<Edge<NodeT>> edges = new PriorityQueue<>();
		for (NodeT u : getActiveNodesShallowCopy()) {
			if (paths.get(u)!=null) continue;

			paths.set (u,u);
			if (matrix.mat.containsKey(u))
				for (Entry<NodeT,Float> entry : matrix.mat.get(u).entrySet())
					edges.offer (new Edge<>(u,entry.getKey(),entry.getValue()));

			while (!edges.isEmpty()) {
				Edge<NodeT> probed = edges.poll();
				if (paths.get (probed.to)==null) {				// process newly discovered node
					paths.set (probed.to, probed.from);
					if (matrix.mat.containsKey(probed.to))			// append all its edges
						for (Entry<NodeT,Float> entry : matrix.mat.get(probed.to).entrySet())
							if (paths.get(entry.getKey())==null)
								edges.offer (new Edge<>(probed.to,entry.getKey(),entry.getValue()));
				}else{
					if (probed.weight < getEdgeWeight (paths.get(probed.to), probed.to))
						paths.set (probed.to, probed.from);		// relaxation
				}
			}
		}

		for (Entry<NodeT,NodeT> pair : paths.entrySet())
			if (pair.getKey()!=pair.getValue() || containsEdge(pair.getValue(),pair.getKey()))
				mst.setEdge(pair.getValue(), pair.getKey(), getEdgeWeight(pair.getValue(), pair.getKey()));

                mst.sinks.addAll(sinks);
		return mst;
	}

        public UndirectedGraph<NodeT> boruvka () {
            UndirectedGraph<NodeT> mst = new UndirectedGraph<>();
            if (isEmpty()) return mst;

            WQUF<NodeT> uf = new WQUF<>();
            // repeat at most log V times or until we have V-1 edges
            for (int t=1; t<numberNodes() && mst.numberNodes()<numberNodes(); t<<=1) {
                // foreach tree in forest, find closest edge
                // if edge weights are equal, ties are broken in favor of first edge
                VectorSparse<NodeT,Edge<NodeT>> closest = new VectorSparse<>();
                for (Edge<NodeT> edge : this) {
                    NodeT u = uf.find(edge.from);
                    NodeT v = uf.find(edge.to);
                    if (!u.equals(v)) {
                        Edge<NodeT> edgeu = closest.get(u);
                        Edge<NodeT> edgev = closest.get(v);
                        if (edgeu==null || edge.weight<edgeu.weight) closest.set(u,edge);
                        if (edgev==null || edge.weight<edgev.weight) closest.set(v,edge);
                    }
                }

                // add newly discovered edges to MST
                for (NodeT u : getActiveNodesShallowCopy()) {
                    Edge<NodeT> edge = closest.get(u);
                    if (edge!=null && !uf.connected(edge.from,edge.to)) {
                        mst.setEdge(edge.from,edge.to,edge.weight);
                        uf.union (edge.from,edge.to);
                    }
                }
            }
            mst.sinks.addAll(sinks);
            return mst;
        }

	private class GraphEdgesIterable  implements Iterator<Edge<NodeT>>, Iterable<Edge<NodeT>> {
		private final TreeSet<NodeT> processedNodes = new TreeSet<>();
		private final Iterator<NodeT> nodesIterator = matrix.mat.keySet().iterator();
		private Iterator<Edge<NodeT>> edgesIterator = null;
		private Edge<NodeT> nextEdge = null;
		private NodeT currentSource = null;

		public GraphEdgesIterable () {
			while (nodesIterator.hasNext()) {
				currentSource = nodesIterator.next();
				edgesIterator = getEdgesFrom (currentSource).iterator();
				while (edgesIterator.hasNext()) {
					nextEdge = edgesIterator.next();
					if (!processedNodes.contains(nextEdge.to))
						return;
				}
                                processedNodes.add(currentSource);
			}
		}
                @Override public boolean hasNext () {return nextEdge!=null&&!processedNodes.contains(nextEdge.to);}
                @Override public void remove () {throw new UnsupportedOperationException();}
                @Override public Iterator<Edge<NodeT>> iterator() {return this;}
                @Override public Edge<NodeT> next () {
			Edge<NodeT> oldEdge = nextEdge;
			nextEdge = null;
			while (edgesIterator.hasNext() && !processedNodes.contains(currentSource)) {
				nextEdge = edgesIterator.next();
				if (!processedNodes.contains(nextEdge.to))
					return oldEdge;
			}
			processedNodes.add (currentSource);

			while (nodesIterator.hasNext()) {
				currentSource = nodesIterator.next();
                                if (processedNodes.contains(currentSource)) continue;
				edgesIterator = getEdgesFrom (currentSource).iterator();
				while (edgesIterator.hasNext()) {
					nextEdge = edgesIterator.next();
					if (!processedNodes.contains(nextEdge.to))
						return oldEdge;
				}
                                processedNodes.add (currentSource);
			}
			return oldEdge;
		}
	}

	@Override
	protected void setErdosRenyiGraph (float p, float weight) {
		sinks.addAll (getActiveNodesShallowCopy());
		TreeSet<NodeT> allNodes = new TreeSet<>(sinks);
		matrix.clear();
		for (NodeT u : allNodes) {
			for (NodeT v : allNodes) {
				if (u.equals(v)) break;
				if (Math.random()<p) 
					setEdge (u,v,weight);
			}
		}
	}

	@Override
	public void printStatistics () {
		if (isSparse()) System.out.print ("----------- SPARSE GRAPH STATISTICS ------------");
		else System.out.print ("----------- DENSE GRAPH STATISTICS -------------");
		System.out.print ("\nEdges: " + numberEdges() + "  (negative: " + numberNegativeEdges() + ", self-loops: " + numberSelfLoops() + ")");
		System.out.print ("\nNodes:\t\t\t" + numberNodes());
		System.out.print ("\nTriangles:\t\t" + numberTriangles());
		System.out.print ("\nDensity:\t\t" + String.format("%.5g",getDensity()));
		System.out.print ("\nGlobal clust. coef.:\t" + String.format("%.5g", getGlobalClusteringCoefficient()));
		System.out.print ("\nLocal clust. coef.:\t" + String.format("%.5g", getLocalClusteringCoefficient()));
		System.out.print ("\nConnected components:\t" + numberConnectedComponents());
		System.out.print ("\nMaximum fan degree:\t" + getMaxDegree());
		System.out.print ("\nAverage fan degree:\t" + String.format("%.4g",getAverageDegree()));
		System.out.print ("\nMaximum path hops: \t" + getMaxPathHops());
		System.out.print ("\nAverage path hops: \t" + String.format("%.5g",getAveragePathHops()));
		System.out.print ("\nMaximum path cost: \t" + getMaxPathCost());
		System.out.print ("\nAverage path cost: \t" + String.format("%.5g",getAveragePathCost()));
		if (containsNegativeCycles()) System.out.print ("\nThe graph DOES contain negative cycles!");
                else if (containsCycles()) System.out.print ("\nThe graph DOES contain cycles.");
                else System.out.print ("\nThe graph contains NO cycles.");
		System.out.println ("\n------------------------------------------------");
	}

	@Override
	public String toString () {
		StringBuilder print = new StringBuilder("############################\n## (source,target,weight) ##\n");
		for (Edge<NodeT> edge : this) {
			System.out.println (edge.reverse().toString());
			System.out.println (edge.toString());
		}
		System.out.println("############################\n");
		return print.toString();
	}

	public static void main(String[] args) {
		//if (args.length<1) throw new IllegalArgumentException ("\n!! User should provide a valid filename for unit testing !!");
                UndirectedGraph<Long> graph = new UndirectedGraph<>("/home/gtsat/grammar/data/lifeifei/sf.graph", NumberFormat.getNumberInstance(), true);
                graph.printStatistics();
                graph.draw (800,600);
/*
                for (int i=0;i<200;++i)
                    System.out.println (i + ": " + graph.karger());
                System.exit(1);
*/
		// test MST computation methods
		System.out.println("!! Testing methods for computing MSTs !!");
		UndirectedGraph<Long> mst1 = graph.prim();
		for (Edge<Long> edge : graph) {
			if (!mst1.pathExistence (edge.from, edge.to))
				throw new RuntimeException ("\n!! Exists no path in the Prim's spanning tree between nodes " + edge.from + " and " + edge.to + " !!");
			if (!mst1.pathExistence (edge.to, edge.from))
				throw new RuntimeException ("\n!! Exists no path in the Prim's spanning tree between nodes " + edge.to + " and " + edge.from + " !!");
		}

		UndirectedGraph<Long> mst2 = graph.kruskal();
		for (Edge<Long> edge : graph) {
			if (!mst2.pathExistence (edge.from, edge.to))
				throw new RuntimeException ("\n!! Exists no path in the Kruskal's spanning tree between nodes " + edge.from + " and " + edge.to + " !!");
			if (!mst2.pathExistence (edge.to, edge.from))
				throw new RuntimeException ("\n!! Exists no path in the Kruskal's spanning tree between nodes " + edge.to + " and " + edge.from + " !!");
		}

		UndirectedGraph<Long> mst3 = graph.boruvka();
		for (Edge<Long> edge : graph) {
			if (!mst3.pathExistence (edge.from, edge.to))
				throw new RuntimeException ("\n!! Exists no path in the Boruvka's spanning tree between nodes " + edge.from + " and " + edge.to + " !!");
			if (!mst3.pathExistence (edge.to, edge.from))
				throw new RuntimeException ("\n!! Exists no path in the Boruvka's spanning tree between nodes " + edge.to + " and " + edge.from + " !!");
		}

		float mst1sum=0.0f;
		for (Edge<Long> edge : mst1) mst1sum += edge.weight;

		float mst2sum=0.0f;
		for (Edge<Long> edge : mst2) mst2sum += edge.weight;

		if (mst1sum!=mst2sum) 
			throw new RuntimeException ("\n!! Different MST computations do not provide equivalent solutions !!");


		// test sssp computation methods
		System.out.println("!! Testing methods for computing SSSPs !!");
		for (long u : graph.getNodes()) {
			float sssp0sum=0.0f;
			for (Edge<Long> edge : graph.sssp(u)) sssp0sum += edge.weight;

			float sssp1sum=0.0f;
			for (Edge<Long> edge : graph.dijkstra(u)) sssp1sum += edge.weight;

			float sssp2sum=0.0f;
			for (Edge<Long> edge : graph.bellman(u)) sssp2sum += edge.weight;

			if (sssp2sum!=sssp1sum || sssp1sum!=sssp0sum) 
				throw new RuntimeException ("\n!! Different SSSP computations do not provide equivalent solutions (" + sssp0sum + " / " + sssp1sum + " / " + sssp2sum + ") !!");
		}


		// test dynamic structure 
		System.out.println("!! Testing graph structure updates !!");
		for (long u : graph.getNodes())
			if (!mst1.removeNode(u) || !mst2.removeNode(u))
				throw new RuntimeException ("\n!! Problematic node deletion !!");

		for (Edge<Long> edge : graph) {
			//graph.removeEdge (edge.from, edge.to);
			mst1.removeEdge (edge.from, edge.to);
			mst2.removeEdge (edge.from, edge.to);
		}


		// test sssp computation methods 
		System.out.println("!! Testing methods for computing APSPs !!");
		Map<Long,Graph<Long>> johnson = graph.johnson();
		Map<Long,Graph<Long>> floyd = graph.floyd();
		for (long u : graph.getNodes()) {
			float sssp1sum=0.0f;
			for (Edge<Long> edge : (UndirectedGraph<Long>)johnson.get(u)) sssp1sum += edge.weight;

			float sssp2sum=0.0f;
			for (Edge<Long> edge : (UndirectedGraph<Long>)floyd.get(u)) sssp2sum += edge.weight;

			if (sssp1sum!=sssp2sum) 
				throw new RuntimeException ("\n!! Different APSP computations do not provide equivalent solutions (" + sssp1sum + " != " + sssp2sum + ") !!");
		}
/**/
		// testing random graphs 
		int n=100;
		System.out.println("!! Testing random graphs !!");
		UndirectedGraph<Integer> random = new UndirectedGraph<>();
		for (int i=0; i<n; ++i) random.addNode(i);
		random.setErdosRenyiGraph ((float)Math.log(n)/n);
                random.printStatistics ();
		random.draw (800,600);

		// test MST computation methods
		System.out.println("!! Testing methods for computing MSTs !!");
		UndirectedGraph<Integer> rmst1 = random.prim();
		for (Edge<Integer> edge : random) {
			if (!rmst1.pathExistence (edge.from, edge.to))
				throw new RuntimeException ("\n!! Exists no path in the spanning tree between nodes " + edge.from + " and " + edge.to + " !!");
			if (!rmst1.pathExistence (edge.to, edge.from))
				throw new RuntimeException ("\n!! Exists no path in the spanning tree between nodes " + edge.to + " and " + edge.from + " !!");
		}

		UndirectedGraph<Integer> rmst2 = random.kruskal();
		for (Edge<Integer> edge : random) {
			if (!rmst2.pathExistence (edge.from, edge.to))
				throw new RuntimeException ("\n!! Exists no path in the spanning tree between nodes " + edge.from + " and " + edge.to + " !!");
			if (!rmst2.pathExistence (edge.to, edge.from))
				throw new RuntimeException ("\n!! Exists no path in the spanning tree between nodes " + edge.to + " and " + edge.from + " !!");
		}

		mst1sum=0.0f;
		for (Edge<Integer> edge : rmst1) mst1sum += edge.weight;

		mst2sum=0.0f;
		for (Edge<Integer> edge : rmst2) mst2sum += edge.weight;

		if (mst1sum!=mst2sum) 
			throw new RuntimeException ("\n!! Different MST computations do not provide equivalent solutions !!");


		// test sssp computation methods
		System.out.println("!! Testing methods for computing SSSPs !!");
		for (int u : random.getNodes()) {
			float sssp0sum=0.0f;
			for (Edge<Integer> edge : random.sssp(u)) sssp0sum += edge.weight;

			float sssp1sum=0.0f;
			for (Edge<Integer> edge : random.dijkstra(u)) sssp1sum += edge.weight;

			float sssp2sum=0.0f;
			for (Edge<Integer> edge : random.bellman(u)) sssp2sum += edge.weight;

			if (sssp2sum!=sssp1sum || sssp1sum!=sssp0sum) 
				throw new RuntimeException ("\n!! Different SSSP computations do not provide equivalent solutions (" + sssp0sum + " / " + sssp1sum + " / " + sssp2sum + ") !!");
		}


		// test dynamic structure 
		System.out.println("!! Testing graph structure updates !!");
		for (int u : random.getNodes())
			if (!rmst1.removeNode(u) || !rmst2.removeNode(u))
				throw new RuntimeException ("\n!! Problematic node deletion !!");

		for (Edge<Integer> edge : random) {
			//graph.removeEdge (edge.from, edge.to);
			rmst1.removeEdge (edge.from, edge.to);
			rmst2.removeEdge (edge.from, edge.to);
		}


		// test apsp computation methods 
		System.out.println("!! Testing methods for computing APSPs !!");
		Map<Integer,Graph<Integer>> rjohnson = random.johnson();
		Map<Integer,Graph<Integer>> rfloyd = random.floyd();
		for (int u : random.getNodes()) {
			float sssp1sum=0.0f;
			for (Edge<Integer> edge : (UndirectedGraph<Integer>)rjohnson.get(u)) sssp1sum += edge.weight;

			float sssp2sum=0.0f;
			for (Edge<Integer> edge : (UndirectedGraph<Integer>)rfloyd.get(u)) sssp2sum += edge.weight;

			if (sssp1sum!=sssp2sum) 
				throw new RuntimeException ("\n!! Different APSP computations do not provide equivalent solutions (" + sssp1sum + " != " + sssp2sum + ") !!");
		}
/**/
		System.out.println ("!! Done with test for undirected graphs !!");
                
                random.printGraph();
	}
}
