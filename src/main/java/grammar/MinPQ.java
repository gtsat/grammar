/*************************************************************************
 *  Compilation:  javac MinPQ.java
 *  Execution:    java MinPQ < input.txt
 *  
 *  Generic min priority queue implementation with a binary heap.
 *  Can be used with a comparator instead of the natural order.
 *
 *  % java MinPQ < tinyPQ.txt
 *  E A E (6 left on pq)
 *
 *  We use a one-based array to simplify parent and child calculations.
 *
 *************************************************************************/

package grammar;

import java.util.Iterator;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;

/**
 *  The <tt>MinPQ</tt> class represents a priority queue of generic keys.
 *  It supports the usual <em>insert</em> and <em>delete-the-minimum</em>
 *  operations, along with methods for peeking at the minimum key,
 *  testing if the priority queue is empty, and iterating through
 *  the keys.
 *  <p>
 *  The <em>insert</em> and <em>delete-the-minimum</em> operations take
 *  logarithmic amortized time.
 *  The <em>min</em>, <em>size</em>, and <em>is-empty</em> operations take constant time.
 *  Construction takes time proportional to the specified capacity or the number of
 *  items used to initialize the data structure.
 *  <p>
 *  This implementation uses a binary heap.
 *  <p>
 *  For additional documentation, see <a href="http://algs4.cs.princeton.edu/24pq">Section 2.4</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */

@SuppressWarnings (value={"unchecked"})
public class MinPQ<Key> implements Iterable<Key> {
    private Key[] pq;                    // store items at indices 1 to N
    private int N;                       // number of items on priority queue
    private Comparator<Key> comparator;  // optional comparator

    public void clear () {while (N>0) pq[N--]=null;}

    public Key[] toArray () {
        Key[] arr = (Key[]) new Object[N];
        for (int i=0; i<N; ++i) arr[i] = pq[i];
        return arr;
    }

    public MinPQ() {this(1);}
    public MinPQ(int initCapacity) {
        pq = (Key[]) new Object[initCapacity + 1];
        N = 0;
    }

    public MinPQ(Comparator<Key> comparator) { this(1, comparator); }
    public MinPQ(int initCapacity, Comparator<Key> comparator) {
        this.comparator = comparator;
        pq = (Key[]) new Object[initCapacity + 1];
        N = 0;
    }

   /**
     * Create a priority queue with the given items.
     * Takes time proportional to the number of items using sink-based heap construction.
     */
    public MinPQ(Key[] keys) {
        N = keys.length;
        pq = (Key[]) new Object[keys.length + 1];
        for (int i = 0; i < N; i++)
            pq[i+1] = keys[i];
        for (int k = N>>1; k >= 1; k--)
            sink(k);
        assert isMinHeap();
    }

    public MinPQ(Key[] keys,Comparator<Key> comparator) {
        this.comparator = comparator;
        N = keys.length;
        pq = (Key[]) new Object[keys.length + 1];
        for (int i = 0; i < N; i++)
            pq[i+1] = keys[i];
        for (int k = N>>1; k >= 1; k--)
            sink(k);
        assert isMinHeap();
    }
    
    public MinPQ (MinPQ<Key> other) {
        comparator = other.comparator;
        N = other.N;
        pq = (Key[]) new Object[other.pq.length];
        for (int i=0; i<=N; ++i)
            pq[i] = other.pq[i];
        assert isMinHeap();
    }

    public boolean isEmpty() {return N == 0;}
    public int size() {return N;}

   /**
     * Return the smallest key on the priority queue.
     * @throws NoSuchElementException if priority queue is empty.
     */
    public Key min() {
        if (isEmpty()) throw new NoSuchElementException("Priority queue underflow");
        return pq[1];
    }

    // helper function to double the size of the heap array
    private void resize(int capacity) {
        assert capacity > N;
        Key[] temp = (Key[]) new Object[capacity];
        for (int i = 1; i <= N; i++) temp[i] = pq[i];
        pq = temp;
    }

   /**
     * Add a new key to the priority queue.
     */
    public void insert(Key x) {
        // double size of array if necessary
        if (N==pq.length-1) resize(pq.length<<1);

        // add x, and percolate it up to maintain heap invariant
        pq[++N] = x;
        swim(N);
        assert isMinHeap();
    }

   /**
     * Delete and return the smallest key on the priority queue.
     * @throws NoSuchElementException if priority queue is empty.
     */
    public Key delMin() {
        if (isEmpty()) throw new NoSuchElementException("Priority queue underflow");
        exch(1, N);
        Key min = pq[N--];
        sink(1);
        pq[N+1] = null;         // avoid loitering and help with garbage collection
        if ((N>0) && (N==((pq.length-1)>>2))) resize(pq.length>>1);
        assert isMinHeap();
        return min;
    }


    /**
     * Containment and deletion of arbitrary key value.
     */
    public boolean contains (Key key) {return contains(1,key);}

    private boolean contains (int i,Key key) {
        while (i<=N) {
            if (!greater(key,pq[i]) && !greater(pq[i],key)) return true;
            else if ((i<<1)+1<=N) return contains(i<<1,key)||contains((i<<1)+1,key);
            else if (i<<1<=N) i<<=1;
            else return containsLinear (i+1,key);
        }
        return false;
    }

    private boolean containsLinear (int i,Key key) {
        for (;i<=N;++i)
            if (!greater(key,pq[i]) && !greater(pq[i],key))
                return true;
        return false;
    }

    public boolean delKey (Key key) {return delKey(1,key);}
    private boolean delKey (int delPos,Key key) {
        if (isEmpty()) throw new NoSuchElementException("Priority queue underflow");

        while (delPos<=N) {
            if (!greater(key,pq[delPos]) && !greater(pq[delPos],key)) {
                exch(delPos,N--);
                sink(delPos);
                return true;
            }else if ((delPos<<1)+1<N) return delKey((delPos<<1)+1,key)||delKey(delPos<<1,key);
            else if ((delPos<<1)<N) return delKey(delPos<<1,key);
            else return delKeyLinear(delPos+1,key);
        }
        return false;
    }

    private boolean delKeyLinear (int delPos,Key key) {
        for (;delPos<=N;++delPos) {
            if (!greater(key,pq[delPos]) && !greater(pq[delPos],key)) {
                exch(delPos,N--);
                sink(delPos);
                return true;
            }
        }
        return false;
    }

    public boolean upgradeKeyDown (Key key) {return upgradeKey (1,key,false);}
    public boolean upgradeKeyUp (Key key) {return upgradeKey (1,key,true);}
    private boolean upgradeKey (int pos, Key key, boolean up) {
        if (isEmpty()) throw new NoSuchElementException("Priority queue underflow");

        while (pos<=N) {
            if (!greater(key,pq[pos]) && !greater(pq[pos],key)) {
                if (up) sink(pos);
                else swim(pos);
                return true;
            }else if ((pos<<1)+1<N) return upgradeKey((pos<<1)+1,key,up) || upgradeKey(pos<<1,key,up);
            else if ((pos<<1)<N) return upgradeKey(pos<<1,key,up);
            else return upgradeLinear(pos+1,key,up);
        }
        return false;
    }

    private boolean upgradeLinear (int pos, Key key, boolean up) {
        for (;pos<=N;++pos) {
            if (!greater(key,pq[pos]) && !greater(pq[pos],key)) {
                if (up) sink (pos);
                else swim (pos);
                return true;
            }
        }
        return false;
    }

   /***********************************************************************
    * Helper functions to restore the heap invariant.
    **********************************************************************/

    private void swim(int k) {
        while (k>1 && greater(k>>1,k)) {
            exch(k, k>>1);
            k = k>>1;
        }
    }

    private void sink(int k) {
        while ((k<<1) <= N) {
            int j = k<<1;
            if (j < N && greater(j, j+1)) j++;
            if (!greater(k, j)) break;
            exch(k, j);
            k = j;
        }
    }

    public ArrayDeque<Key> filter (Key threshold) {
        ArrayDeque<Key> result = new ArrayDeque<>();
        filter (threshold,1,result);
        
        System.out.println(result.size()+"/"+size());
        return result;
    }

    private void filter (Key threshold, int pos, ArrayDeque<Key> result) {
        if (greater(threshold,pq[pos])) {
            result.add(pq[pos]);
            if (N<(pos<<1)) filter (threshold,pos<<1,result);
            if (N<(pos<<1)+1) filter (threshold,(pos<<1)+1,result);
        }
    }

   /***********************************************************************
    * Helper functions for compares and swaps.
    **********************************************************************/
    private boolean greater(int i, int j) {
        return comparator==null?((Comparable<Key>)pq[i]).compareTo(pq[j])>0:comparator.compare(pq[i],pq[j])>0;
    }

    private boolean greater(Key x, Key y) {
        return comparator==null?((Comparable<Key>)x).compareTo(y)>0:comparator.compare(x,y)>0;
    }

    private void exch(int i, int j) {
        Key swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
    }

    // is pq[1..N] a min heap?
    private boolean isMinHeap() {
        return isMinHeap(1);
    }

    // is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeap(int k) {
        if (k>N) return true;
        int left = k<<1, right = k<<1 + 1;
        if (left  <= N && greater(k, left))  return false;
        if (right <= N && greater(k, right)) return false;
        return isMinHeap(left) && isMinHeap(right);
    }


   /***********************************************************************
    * Iterators
    **********************************************************************/

   /**
     * Return an iterator that iterates over all of the keys on the priority queue
     * in ascending order.
     * <p>
     * The iterator doesn't implement <tt>remove()</tt> since it's optional.
     */
    public Iterator<Key> iterator() { return new HeapIterator(); }

    private class HeapIterator implements Iterator<Key> {
        // create a new pq
        private MinPQ<Key> copy;

        // add all items to copy of heap
        // takes linear time since already in heap order so no keys move
        public HeapIterator() {
            if (comparator == null) copy = new MinPQ<>(size());
            else                    copy = new MinPQ<>(size(), comparator);
            for (int i = 1; i <= N; i++)
                copy.insert(pq[i]);
        }

        public boolean hasNext()  { return !copy.isEmpty();                     }
        public void remove()      { throw new UnsupportedOperationException();  }

        public Key next() {
            if (!hasNext()) throw new NoSuchElementException();
            return copy.delMin();
        }
    }

   /**
     * A test client.
     *
    public static void main(String[] args) {
        MinPQ<String> pq = new MinPQ<String>();
        while (!StdIn.isEmpty()) {
            String item = StdIn.readString();
            if (!item.equals("-")) pq.insert(item);
            else if (!pq.isEmpty()) StdOut.print(pq.delMin() + " ");
        }
        StdOut.println("(" + pq.size() + " left on pq)");
    }*/

    public static void main (String[] args) {
        MinPQ<String> heap = new MinPQ<>();
        heap.insert ("trhrgrbr");
        heap.insert ("rfergrbr");
        heap.insert ("sabcd");
        heap.insert ("icrfergr");
        for (String str : heap) System.out.println (str);
        System.out.println("-------------------");
        for (String str : heap) System.out.println ("** Contains element '"+str+"': "+heap.contains(str));
        System.out.println("-------------------");
        System.out.println("** Remove element 'sabcd': "+heap.delKey("sabcd"));
        System.out.println("-------------------");
        for (String str : heap) System.out.println ("** Contains element '"+str+"': "+heap.contains(str));

        System.out.println("-------------------");
        System.out.println("-------------------");

        MinPQ<Integer> intheap = new MinPQ<>();
        for (int i=1;i<=100;++i) intheap.insert(i);
        System.out.println (intheap.size());

        for (int i=0;i<=100;++i) System.out.println("** Containment for key "+i+" returned "+intheap.contains(i));
        for (int i=80;i<=90;++i) System.out.println("** Removing key "+i+" returned "+intheap.delKey(i));
        for (int i=0;i<=100;++i) System.out.println("** Containment for key "+i+" returned "+intheap.contains(i));
    }
}

