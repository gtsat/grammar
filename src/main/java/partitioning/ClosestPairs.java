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
import grammar.MinPQ;
import grammar.MaxPQ;
import grammar.Graph;
import shortestpath.ScatterMap;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Stack;
import java.util.Set;
import java.util.Map;

import java.util.concurrent.locks.ReentrantReadWriteLock;

final public class ClosestPairs<NodeT> {

    private static final MaxPQ<Edge> result = new MaxPQ<>();

    private static float globaldistancethreshold = Float.MAX_VALUE;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static<NodeT> ArrayDeque<Edge<NodeT>> compute (GraphPartition<NodeT> gp,
                                                            Collection<NodeT> R,
                                                            Collection<NodeT> S,
                                                            int K,float threshold) {

        Thread threads[] = new Thread [gp.hierarchySize()];
        Map<NodeT,Map<NodeT,Float>> bordernodes = new HashMap<>();
        //MaxPQ<Edge<NodeT>> result = computeRecursive(gp,R,S,K,threshold,bordernodes);
        ArrayDeque<Edge<NodeT>> ascending = new ArrayDeque<>();
        while (!result.isEmpty()) ascending.addFirst(result.delMax());
        return ascending;
    }

    private static<NodeT> MaxPQ<Edge> computeRecursive (GraphPartition<NodeT> gp,
                                                                Collection<NodeT> R,
                                                                Collection<NodeT> S,
                                                                int K,float threshold,
                                                                Map<NodeT,Map<NodeT,Float>> bordernodes) {

        if (gp.isLeaf()) {
            gp.graphconcurrentjoinonresult(R,S,K,result,bordernodes);
            return result;
            //return gp.graphjoin(R,S,K,threshold,bordernodes);
        }else{
            /* get closest pairs of each child node and combine them appropriately *
            Map<NodeT,Map<NodeT,Float>> leftbordernodes = new HashMap<>();
            MaxPQ<Edge<NodeT>> leftpairs = computeRecursive (gp.getLeftChild(),R,S,K,threshold,leftbordernodes);
            Map<NodeT,Map<NodeT,Float>> rightbordernodes = new HashMap<>();
            MaxPQ<Edge<NodeT>> rightpairs = computeRecursive (gp.getRightChild(),R,S,K,threshold,rightbordernodes);

            return combineChildResults (gp,leftpairs,rightpairs,leftbordernodes,rightbordernodes,R,S,K);
            */
            return null;
            /**/
        }
    }

    final private static class LevelComparator<NodeT> implements Comparator<XThread<NodeT>> {
        @Override public int compare (XThread<NodeT> gp1, XThread<NodeT> gp2) {
            if (gp1.partition.isLeaf() && gp2.partition.isLeaf()) return 0;
            else if (gp1.partition.isLeaf()) return -1;
            else if (gp2.partition.isLeaf()) return 1;
            else{
                int balance=gp1.partition.levelsToClosestLeaf()-gp2.partition.levelsToClosestLeaf();
                if (balance<0) return -1;
                else if (balance>0) return 1;
                else return 0;
            }
        }
    }

    public static<NodeT> ArrayDeque<Edge<NodeT>> computeMultiThreaded (GraphPartition<NodeT> gp,
                                                                        Collection<NodeT>R,
                                                                        Collection<NodeT>S,
                                                                        int K,float threshold,
									int parallelism) {
        return computeMultiThreadedFlat (gp,R,S,K,threshold,parallelism);
    }

    public static<NodeT> ArrayDeque<Edge<NodeT>> computeMultiThreadedFlat (GraphPartition<NodeT> gp,
                                                                        Collection<NodeT>R,
                                                                        Collection<NodeT>S,
                                                                        int K,float threshold,
									int parallelism) {
        Stack<XThread<NodeT>> stack0 = new Stack<>();
        Stack<XThread<NodeT>> stack1 = new Stack<>();
        if (gp.isLeaf()) return compute (gp,R,S,K,threshold);
        XThread<NodeT> root = new XThread<>(gp,null,R,S,K);
        ArrayDeque<XThread<NodeT>> queue = new ArrayDeque<>();
        queue.addLast(root);
        while (!queue.isEmpty()) {
            XThread<NodeT> thread = queue.removeFirst();
            if (!thread.partition.isLeaf()) {
                queue.addLast (new XThread<>(thread.partition.getLeftChild(),thread,R,S,K));
                queue.addLast (new XThread<>(thread.partition.getRightChild(),thread,R,S,K));
            }else{
                stack0.push(thread);
                stack1.push(thread);
            }
        }
        for (int counter=0; !stack0.isEmpty();) {
            if (counter < parallelism) {
                stack0.pop().start();
                ++counter;
                System.out.println ("** Number of running threads: "+counter);
            }else{
                try{
                    stack1.pop().join();
                }catch(InterruptedException e){}
                --counter;
                System.out.println ("** Number of running threads: "+counter);
            }
        }
        while (!stack1.isEmpty()) {
            try{
                stack1.pop().join();
            }catch(InterruptedException e){}
        }

        ArrayDeque<Edge<NodeT>> ascending = new ArrayDeque<>();
        while (!result.isEmpty()) ascending.addFirst(result.delMax());
        return ascending;
    }

    public static<NodeT> ArrayDeque<Edge<NodeT>> computeMultiThreadedHierarchical (GraphPartition<NodeT> gp,
                                                                        Collection<NodeT>R,
                                                                        Collection<NodeT>S,
                                                                        int K,float threshold,
									int parallelism) {

        Stack<XThread<NodeT>> stack0 = new Stack<>();
        Stack<XThread<NodeT>> stack1 = new Stack<>();
        if (gp.isLeaf()) return compute (gp,R,S,K,threshold);
        XThread<NodeT> root = new XThread<>(gp,null,R,S,K);
        ArrayDeque<XThread<NodeT>> queue = new ArrayDeque<>();
        queue.addLast(root);
        while (!queue.isEmpty()) {  //&& queue.size()<(parallelism<<1)) {
            XThread<NodeT> thread = queue.removeFirst();
            if (!thread.partition.isLeaf()) {
                //if (thread.partition.getLeftChild().intersects(R) && thread.partition.getLeftChild().intersects(S))
                    queue.addLast (new XThread<>(thread.partition.getLeftChild(),thread,R,S,K));
                //else thread.leftresult = new MaxPQ<>();

                //if (thread.partition.getRightChild().intersects(R) && thread.partition.getRightChild().intersects(S))
                    queue.addLast (new XThread<>(thread.partition.getRightChild(),thread,R,S,K));
                //else thread.rightresult = new MaxPQ<>();
            }
            stack0.push(thread);
            stack1.push(thread);
        }

        for (int counter=0; !stack0.isEmpty();) {
            if (counter < parallelism) {
                stack0.pop().start();
                ++counter;
                System.out.println ("** Number of running threads: "+counter);
            }else{
                try{
                    stack1.pop().join();
                }catch(InterruptedException e){}
                --counter;
                System.out.println ("** Number of running threads: "+counter);
            }
        }
        while (!stack1.isEmpty()) {
            try{
                stack1.pop().join();
            }catch(InterruptedException e){}
        }

        MaxPQ<Edge<NodeT>> result = root.partition.isLeaf()?
                                    root.partition.graphjoin(R,S,K,threshold,root.leftbordernodes)
                                    :combineChildResults (gp,root.leftresult,root.rightresult,
                                                          root.leftbordernodes,root.rightbordernodes,
                                                          R,S,K);

        ArrayDeque<Edge<NodeT>> ascending = new ArrayDeque<>();
        while (!result.isEmpty()) ascending.addFirst(result.delMax());
        return ascending;
    }

    /* dfs-based scheduling */
    public static<NodeT> ArrayDeque<Edge<NodeT>> computeMultiThreadedOld(GraphPartition<NodeT> gp,
                                                                        Collection<NodeT>R,
                                                                        Collection<NodeT>S,
                                                                        int K,float threshold,
									int parallelism) {

        MinPQ<XThread<NodeT>> runningthreads = new MinPQ<>(new LevelComparator<NodeT>());
        if (gp.isLeaf()) return compute (gp,R,S,K,threshold);
        HashSet<XThread<NodeT>> expanded = new HashSet<>();
        XThread<NodeT> root = new XThread<>(gp,null,R,S,K);
        Stack<XThread<NodeT>> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            XThread<NodeT> thread = stack.peek();
            if (thread.isReadyToRun()) { //|| runningthreads.size()==parallelism) { //|| stack.size()==(parallelism<<1)) {
                if (runningthreads.size() >= parallelism)
                    try{runningthreads.delMin().join();
                    }catch(InterruptedException e){}
                runningthreads.insert(thread);
                thread.start();
                stack.pop();
            }else{
                if (expanded.contains(thread)) {
                    while (thread.leftresult==null || thread.rightresult==null)
                        try{runningthreads.delMin().join();
                        }catch(InterruptedException e){}
                }else if (thread.partition.getLeftChild()!=null && thread.partition.getRightChild()!=null) {
                    if (thread.partition.getLeftChild().intersects(R)) {
                        XThread<NodeT> leftthread = new XThread<>(thread.partition.getLeftChild(),thread,R,S,K);
                        stack.push(leftthread);
                    }else thread.leftresult = new MaxPQ<>();

                    if (thread.partition.getRightChild().intersects(R)) {
                        XThread<NodeT> rightthread = new XThread<>(thread.partition.getRightChild(),thread,R,S,K);
                        stack.push(rightthread);
                    }else thread.rightresult = new MaxPQ<>();

                    expanded.add(thread);
                }else{
                    thread.leftresult = new MaxPQ<>();
                    thread.rightresult = new MaxPQ<>();
                }
            }
        }

        MaxPQ<Edge<NodeT>> result = combineChildResults (gp,root.leftresult,root.rightresult,
							 root.leftbordernodes,root.rightbordernodes,
							 R,S,K);

        ArrayDeque<Edge<NodeT>> ascending = new ArrayDeque<>();
        while (!result.isEmpty()) ascending.addFirst(result.delMax());
        return ascending;
    }

    final private static class XThread<NodeT> extends Thread {
        final private GraphPartition<NodeT> partition;
        final private XThread<NodeT> parentthread;
        final private Collection<NodeT> R,S;
        final private int K;

        private MaxPQ<Edge> leftresult=null,rightresult=null;
        private Map<NodeT,Map<NodeT,Float>> leftbordernodes,rightbordernodes;

        public boolean isReadyToRun () {return partition.isLeaf() || leftresult!=null && rightresult!=null;}

        @Override public void run () {
            if (parentthread!=null) {
                if (parentthread.partition.getLeftChild()==partition) {
                    if (leftresult==null || rightresult==null) {
                        float threshold=Float.MAX_VALUE;
                        lock.readLock().lock();
                        try{threshold=globaldistancethreshold;
                        }finally{lock.readLock().unlock();}
                        partition.graphconcurrentjoinonresult(R,S,K,parentthread.leftresult,parentthread.leftbordernodes);
                        //parentthread.leftresult=partition.graphjoin(R,S,K,threshold,parentthread.leftbordernodes);
                    }else{
                        //parentthread.leftresult=combineChildResults(partition,leftresult,rightresult,leftbordernodes,rightbordernodes,R,S,K);
                    }
                }else if (parentthread.partition.getRightChild()==partition) {
                    if (leftresult==null || rightresult==null) {
                        float threshold=Float.MAX_VALUE;
                        lock.readLock().lock();
                        try{threshold=globaldistancethreshold;
                        }finally{lock.readLock().unlock();}
                        partition.graphconcurrentjoinonresult(R,S,K,parentthread.rightresult,parentthread.rightbordernodes);
                        //parentthread.rightresult=partition.graphjoin(R,S,K,threshold,parentthread.rightbordernodes);
                    }else{
                        //parentthread.rightresult=combineChildResults(partition,leftresult,rightresult,leftbordernodes,rightbordernodes,R,S,K);
                    }
                }else throw new RuntimeException("\n!! ERROR - Unattended thread called to run !!");
            }
        }

        public XThread (GraphPartition<NodeT> gp,
                        XThread<NodeT> parentthread,
                        Collection<NodeT>R,
                        Collection<NodeT>S,
                        int K) {

            this.parentthread = parentthread;
            this.partition = gp;
            this.R = R;
            this.S = S;
            this.K = K;
            this.leftbordernodes = new HashMap<>();
            this.rightbordernodes = new HashMap<>();
        }
    }

    private static<NodeT> MaxPQ<Edge<NodeT>> combineChildResults (GraphPartition<NodeT> gp, 
                                                                  MaxPQ<Edge> leftpairs, 
                                                                  MaxPQ<Edge> rightpairs, 
                                                                  Map<NodeT,Map<NodeT,Float>> leftbordernodes,
                                                                  Map<NodeT,Map<NodeT,Float>> rightbordernodes,
                                                                  Collection<NodeT> R, Collection<NodeT> S, 
                                                                  int K) {

            /* keep the top-k closest pairs of the children */
            assert (leftpairs!=null && rightpairs!=null);
            while ((leftpairs.size()+rightpairs.size())>K)
                if (leftpairs.max().weight<rightpairs.max().weight) rightpairs.delMax();
                else leftpairs.delMax();

            /* merge the partial results accordingly */
            MaxPQ<Edge<NodeT>> merged = new MaxPQ<>();
            while (!leftpairs.isEmpty() || !rightpairs.isEmpty()) // fast insertion in asc order
                if (rightpairs.isEmpty() || !leftpairs.isEmpty() && leftpairs.max().weight<rightpairs.max().weight)
                    merged.insert (leftpairs.delMax());
                else if (leftpairs.isEmpty() || !rightpairs.isEmpty() && rightpairs.max().weight<leftpairs.max().weight)
                    merged.insert (rightpairs.delMax());

            if (gp.isLeaf()) return merged;

if(true){
            float threshold = merged.size()<K?Float.MAX_VALUE:merged.max().weight;

            lock.readLock().lock();
            if (threshold < globaldistancethreshold) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try{
                    globaldistancethreshold = threshold;
                    //lock.readLock().lock();
                }finally{
                    lock.writeLock().unlock();
                    //lock.readLock().unlock();
                }
            }else lock.readLock().unlock();

            /* check if additional closest pairs exist that run through both children */
            if (gp.getLeftChild().intersects(R) && gp.getRightChild().intersects(S)) {
                for (Edge<NodeT> crossedge : gp.getLeftChild().getCrossEdges()) {
                    if (merged.size()>=K && crossedge.weight>=merged.max().weight) break; // crossedges appear in ascending order from the head of the deque

                    lock.readLock().lock();
                    try {
                        if (globaldistancethreshold < threshold)
                            threshold = globaldistancethreshold;
                    }finally{lock.readLock().unlock();}

                    for (Edge<NodeT> betterpair : expandCrossEdge (gp,leftbordernodes.get(crossedge.from),
                                                                            crossedge,S,K,threshold)) {
                        if (merged.size()<K) merged.insert(betterpair);
                        else{
                            merged.delMax();
                            merged.insert(betterpair);
                            threshold = merged.max().weight;

                            lock.readLock().lock();
                            if (threshold < globaldistancethreshold) {
                                lock.readLock().unlock();
                                lock.writeLock().lock();
                                try {
                                    globaldistancethreshold = threshold;
                                    //lock.readLock().lock();
                                }finally{
                                    lock.writeLock().unlock();
                                    //lock.readLock().unlock();
                                }
                            }else lock.readLock().unlock();
                        }
                    }
                }
            }

            if (gp.getLeftChild().intersects(S) && gp.getRightChild().intersects(R)) {
                for (Edge<NodeT> crossedge : gp.getRightChild().getCrossEdges()) {
                    if (merged.size()>=K && crossedge.weight>=merged.max().weight) break; // crossedges appear in ascending order from the head of the deque

                    lock.readLock().lock();
                    try {
                        if (globaldistancethreshold < threshold)
                            threshold = globaldistancethreshold;
                    }finally{lock.readLock().unlock();}

                    for (Edge<NodeT> betterpair : expandCrossEdge (gp,rightbordernodes.get(crossedge.from),
                                                                   crossedge,S,K,threshold)) {
                        if (merged.size()<K) merged.insert(betterpair);
                        else{
                            merged.delMax();
                            merged.insert(betterpair);
                            threshold = merged.max().weight;

                            lock.readLock().lock();
                            if (threshold < globaldistancethreshold) {
                                lock.readLock().unlock();
                                lock.writeLock().lock();
                                try {
                                    globaldistancethreshold = threshold;
                                    //lock.readLock().lock();
                                }finally{
                                    lock.writeLock().unlock();
                                    //lock.readLock().unlock();
                                }
                            }else lock.readLock().unlock();
                        }
                    }
                }
            }
}
            return merged;
    }

    private static<NodeT> MaxPQ<Edge<NodeT>> combineChildResults (GraphPartition<NodeT> gp, 
                                                                  MaxPQ<Edge<NodeT>> leftpairs, 
                                                                  MaxPQ<Edge<NodeT>> rightpairs, 
                                                                  Collection<NodeT> R, Collection<NodeT> S, 
                                                                  int K) {
            /* keep the top-k closest pairs of the children */
            while ((leftpairs.size()+rightpairs.size())>K)
                if (leftpairs.max().weight<rightpairs.max().weight) rightpairs.delMax();
                else leftpairs.delMax();

            /* merge the partial results accordingly */
            MaxPQ<Edge<NodeT>> merged = new MaxPQ<>();
            while (!leftpairs.isEmpty() || !rightpairs.isEmpty()) { // fast insertion in asc order
                if (rightpairs.isEmpty() || (!leftpairs.isEmpty() && leftpairs.max().weight<rightpairs.max().weight))
                    merged.insert (leftpairs.delMax());
                else if (leftpairs.isEmpty() || (!rightpairs.isEmpty() && rightpairs.max().weight<leftpairs.max().weight))
                    merged.insert (rightpairs.delMax());
            }

            float threshold = merged.size()<K?Float.MAX_VALUE:merged.max().weight;

            lock.readLock().lock();
            if (threshold < globaldistancethreshold) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try{
                    globaldistancethreshold = threshold;
                    //lock.readLock().lock();
                }finally{
                    lock.writeLock().unlock();
                    //lock.readLock().unlock();
                }
            }else lock.readLock().unlock();

            /* check if additional closest pairs exist that run through both children */
            for (Edge<NodeT> crossedge : gp.getLeftChild().getCrossEdges()) {
                if (merged.size()>=K && crossedge.weight>=merged.max().weight) break; // crossedges appear in ascending order from the head of the deque

                lock.readLock().lock();
                try {
                    if (globaldistancethreshold < threshold)
                        threshold = globaldistancethreshold;
                }finally{lock.readLock().unlock();}

                for (Edge<NodeT> betterpair : expandCrossEdge (gp,crossedge,R,S,K,threshold)) {
                    if (merged.size()<K) merged.insert(betterpair);
                    else{
                        merged.delMax();
                        merged.insert(betterpair);
                        threshold = merged.max().weight;

                        lock.readLock().lock();
                        if (threshold < globaldistancethreshold) {
                            lock.readLock().unlock();
                            lock.writeLock().lock();
                            try{
                                globaldistancethreshold = threshold;
                                //lock.readLock().lock();
                            }finally{
                                lock.writeLock().unlock();
                                //lock.readLock().unlock();
                            }
                        }else lock.readLock().unlock();
                    }
                }
            }
            for (Edge<NodeT> crossedge : gp.getRightChild().getCrossEdges()) {
                if (merged.size()>=K && crossedge.weight>=merged.max().weight) break; // crossedges appear in ascending order from the head of the deque

                lock.readLock().lock();
                try {
                    if (globaldistancethreshold < threshold)
                        threshold = globaldistancethreshold;
                }finally{lock.readLock().unlock();}

                for (Edge<NodeT> betterpair : expandCrossEdge (gp,crossedge,R,S,K,threshold)) {
                    if (merged.size()<K) merged.insert(betterpair);
                    else{
                        merged.delMax();
                        merged.insert(betterpair);
                        threshold = merged.max().weight;

                        lock.readLock().lock();
                        if (threshold < globaldistancethreshold) {
                            lock.readLock().unlock();
                            lock.writeLock().lock();
                            try {
                                globaldistancethreshold = threshold;
                                //lock.readLock().lock();
                            }finally{
                                lock.writeLock().unlock();
                                //lock.readLock().unlock();
                            }
                        }else lock.readLock().unlock();
                    }
                }
            }
            return merged;
    }

    private static<NodeT> MaxPQ<Edge<NodeT>> expandCrossEdge (GraphPartition<NodeT> gp,
                                                                    Map<NodeT,Float> insideroutes,
                                                                    Edge<NodeT> crossedge,Collection<NodeT> S,
                                                                    int K,float threshold) {

        MaxPQ<Edge<NodeT>> matches = new MaxPQ<>();
        if (insideroutes==null || insideroutes.isEmpty()) return matches;
        MinPQ<Edge<NodeT>> pathends = new MinPQ<>();
        Set<NodeT> marked = new HashSet<>();
        pathends.insert(crossedge);
        marked.add(crossedge.to);

        float offset = Float.MAX_VALUE;
        for (Map.Entry<NodeT,Float> entry : insideroutes.entrySet())
            if (entry.getValue()<offset)
                offset = entry.getValue();

        while (!pathends.isEmpty()) {
            Edge<NodeT> top = pathends.delMin();
            if (top.weight+offset>=threshold) return matches;
            if (S.contains(top.to)) {
                for (Map.Entry<NodeT,Float> entry : insideroutes.entrySet()) {
                    if(matches.size()<K || entry.getValue()+top.weight<matches.max().weight) {
                        if (matches.size()>=K) matches.delMax();
                        matches.insert(new Edge<>(entry.getKey(),top.to,entry.getValue()+top.weight));
                    }
                }
            }
            for (Edge<NodeT> edge : gp.getReferenceGraph().getEdgesFrom(top.to)) {
                if (!marked.contains(edge.to) && !gp.getCrossEdges().contains(edge)) {
                    pathends.insert(new Edge<>(top.from,edge.to,edge.weight+top.weight));
                    marked.add(edge.to);
                }
            }
        }
        return matches;
    }

    private static<NodeT> ArrayDeque<Edge<NodeT>> expandCrossEdge (GraphPartition<NodeT> gp,
                                                                    Edge<NodeT> crossedge,
                                                                    Collection<NodeT> R,
                                                                    Collection<NodeT> S,
                                                                    int K,float threshold) {
        ArrayDeque<Edge<NodeT>> matches = new ArrayDeque<>();
        Edge<NodeT> nextclosest = null;
        float nextclosesttodist = 0.0f;
        float nextclosestfromdist = 0.0f;

        /* find closest pointt from <>S</> to the right side of the <>crossedge</> */
        Set<NodeT> fMarked = new HashSet<>();
        MinPQ<Edge<NodeT>> forward = new MinPQ<>();
        forward.insert(new Edge<>(crossedge.to,crossedge.to,0.0f));
        fMarked.add(crossedge.to);
        while (!forward.isEmpty()) {
            Edge<NodeT> top = forward.delMin();
            if (top.weight>=threshold) return matches;
            if (S.contains(top.to)) {
                nextclosest = new Edge<>(null,top.to,crossedge.weight+top.weight);
                nextclosesttodist = top.weight;
                break;
            }
            for (Edge<NodeT> edge : gp.getReferenceGraph().getEdgesFrom(top.to)) {
                if (!fMarked.contains(edge.to) && !gp.getCrossEdges().contains(edge)) {
                    forward.insert(new Edge<>(top.to,edge.to,top.weight+edge.weight));
                    fMarked.add(edge.to);
                }
            }
        }

        if (nextclosest==null) return matches;

        /* find closest pointt from <>R</> to the left side of the <>crossedge</> */
        Set<NodeT> bMarked = new HashSet<>();
        MinPQ<Edge<NodeT>> backward = new MinPQ<>();
        backward.insert(new Edge<>(crossedge.from,crossedge.from,0.0f));
        bMarked.add(crossedge.from);
        while (!backward.isEmpty()) {
            Edge<NodeT> top = backward.delMin();
            if (top.weight>=threshold) return matches;
            if (R.contains(top.from)) {
                nextclosest = new Edge<>(top.from,nextclosest.to,nextclosest.weight+top.weight);
                nextclosestfromdist = top.weight;
                break;
            }
            for (Edge<NodeT> edge : gp.getReferenceGraph().getEdgesTo(top.from)) {
                if (!bMarked.contains(edge.from) && !gp.getCrossEdges().contains(edge)) {
                    backward.insert(new Edge<>(edge.from,top.from,top.weight+edge.weight));
                    bMarked.add(edge.from);
                }
            }
        }

        /* if no pairs exist, then terminate */
        if (nextclosest.from==null) return matches;
        else{
            matches.addLast (nextclosest);
            while (matches.size()<K && (!forward.isEmpty() || !backward.isEmpty())) {
                Edge<NodeT> fExpansion = forward.isEmpty()?null:forward.min();
                Edge<NodeT> bExpansion = backward.isEmpty()?null:backward.min();
                if (bExpansion==null || fExpansion!=null && fExpansion.compareTo(bExpansion)<0) {
                    /* extend crossedge forward for points from <>S</> */
                    if (fExpansion.weight>=threshold) return matches;
                    else if (S.contains(fExpansion.to)) {
                        nextclosesttodist = fExpansion.weight;
                        nextclosest = new Edge<>(nextclosest.from,fExpansion.to,nextclosestfromdist+crossedge.weight+nextclosesttodist);
                        matches.add(nextclosest);
                    }
                    for (Edge<NodeT> edge : gp.getReferenceGraph().getEdgesFrom(fExpansion.to)) {
                        if (!fMarked.contains(edge.to)) {
                            forward.insert(new Edge<>(edge.from,edge.to,fExpansion.weight+edge.weight));
                            fMarked.add(edge.to);
                        }
                    }
                }else{
                    /* extend crossedge backwards for points from <>R</> */
                    if (bExpansion.weight>=threshold) return matches;
                    else if (R.contains(bExpansion.from)) {
                        nextclosestfromdist = bExpansion.weight;
                        nextclosest = new Edge<>(bExpansion.from,nextclosest.to,nextclosestfromdist+crossedge.weight+nextclosesttodist);
                        matches.add(nextclosest);
                    }
                    for (Edge<NodeT> edge : gp.getReferenceGraph().getEdgesTo(bExpansion.from)) {
                        if (!bMarked.contains(edge.to)) {
                            backward.insert(new Edge<>(edge.from,edge.to,bExpansion.weight+edge.weight));
                            bMarked.add(edge.from);
                        }
                    }
                }
            }
        }
        return matches;
    }

    public static void main (String[] args) {
        if (args.length<4) 
            throw new IllegalArgumentException("!! ERROR - Provide the filepath to the graph," + 
            " the nodes' coordinates, the number of leaf-nodes, and the expected result-size !!");
        Graph<Long> graph = new grammar.UndirectedGraph<>(args[0],NumberFormat.getNumberInstance(),true);
        ScatterMap<Long> map = new ScatterMap<>(args[1],NumberFormat.getNumberInstance());
        GraphPartition<Long> gp = new GraphPartition<>(graph,map,Integer.parseInt(args[2]),0.0f);

	long startTime = System.nanoTime();
        Collection<Edge<Long>> result =  computeMultiThreaded (gp,gp.getVertices(),gp.getVertices(),
                                                     Integer.parseInt(args[3]),
                                                     Integer.parseInt(args[4]),
                                                     Integer.parseInt(args[5]));
	long endTime = System.nanoTime();

        for (Edge<Long> match : result) System.out.print (match);
	System.out.println ("\n** Processing time: "+((endTime-startTime)/Math.pow(10,-6))+" msec.");
    }
}

