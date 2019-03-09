/*************************************************************************
 *  Compilation:  javac MaxPQ.java
 *  Execution:    java MaxPQ < input.txt
 *  
 *  Generic max priority queue implementation with a binary heap.
 *  Can be used with a comparator instead of the natural order,
 *  but the generic Key type must still be Comparable.
 *
 *  % java MaxPQ < tinyPQ.txt 
 *  Q X P (6 left on pq)
 *
 *  We use a one-based array to simplify parent and child calculations.
 *
 *************************************************************************/

package grammar;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *  The <tt>MaxPQ</tt> class represents a priority queue of generic keys.
 *  It supports the usual <em>insert</em> and <em>delete-the-maximum</em>
 *  operations, along with methods for peeking at the maximum key,
 *  testing if the priority queue is empty, and iterating through
 *  the keys.
 *  <p>
 *  The <em>insert</em> and <em>delete-the-maximum</em> operations take
 *  logarithmic amortized time.
 *  The <em>max</em>, <em>size</em>, and <em>is-empty</em> operations take constant time.
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

@SuppressWarnings("unchecked")
public class MaxPQ<Key> implements Iterable<Key> {
    private Key[] pq;                    // store items at indices 1 to N
    private int N;                       // number of items on priority queue
    private Comparator<Key> comparator;  // optional Comparator

    public void clear () {while (N>0) pq[N--]=null;}

    public Key[] toArray () {
        Key[] arr = (Key[]) new Object[N];
        for (int i=0; i<N; ++i) arr[i] = pq[i];
        return arr;
    }

   /**
     * Create an empty priority queue with the given initial capacity.
     */
    public MaxPQ(int capacity) {
        pq = (Key[]) new Object[capacity + 1];
        N = 0;
    }

   /**
     * Create an empty priority queue.
     */
    public MaxPQ() { this(1); }

   /**
     * Create an empty priority queue with the given initial capacity,
     * using the given comparator.
     */
    public MaxPQ(int initCapacity, Comparator<Key> comparator) {
        this.comparator = comparator;
        pq = (Key[]) new Object[initCapacity + 1];
        N = 0;
    }

   /**
     * Create an empty priority queue using the given comparator.
     */
    public MaxPQ(Comparator<Key> comparator) { this(1, comparator); }

   /**
     * Create a priority queue with the given items.
     * Takes time proportional to the number of items using sink-based heap construction.
     */
    public MaxPQ(Key[] keys) {
        N = keys.length;
        pq = (Key[]) new Object[keys.length + 1]; 
        for (int i = 0; i < N; i++)
            pq[i+1] = keys[i];
        for (int k = N>>1; k >= 1; k--)
            sink(k);
        assert isMaxHeap();
    }

    public MaxPQ(Key[] keys, Comparator<Key> comparator) {
        this.comparator = comparator;
        N = keys.length;
        pq = (Key[]) new Object[keys.length + 1]; 
        for (int i = 0; i < N; i++)
            pq[i+1] = keys[i];
        for (int k = N>>1; k >= 1; k--)
            sink(k);
        assert isMaxHeap();
    }

    public MaxPQ (MaxPQ<Key> other) {
        comparator = other.comparator;
        N = other.N;
        pq = (Key[]) new Object[other.pq.length];
        for (int i=0; i<=N; ++i)
            pq[i] = other.pq[i];
        assert isMaxHeap();
    }

   /**
     * Is the priority queue empty?
     */
    public boolean isEmpty() {
        return N == 0;
    }

   /**
     * Return the number of items on the priority queue.
     */
    public int size() {
        return N;
    }

   /**
     * Return the largest key on the priority queue.
     * @throws NoSuchElementException if priority queue is empty.
     */
    public Key max() {
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
        if (N >= pq.length - 1) resize(pq.length<<1);

        // add x, and percolate it up to maintain heap invariant
        pq[++N] = x;
        swim(N);
        assert isMaxHeap();
    }

   /**
     * Delete and return the largest key on the priority queue.
     * @throws NoSuchElementException if priority queue is empty.
     */
    public Key delMax() {
        if (isEmpty()) throw new NoSuchElementException("Priority queue underflow");
        Key max = pq[1];
        exch(1, N--);
        sink(1);
        pq[N+1] = null;     // to avoid loiterig and help with garbage collection
        if ((N > 0) && (N == (pq.length-1)>>2)) resize(pq.length>>1);
        assert isMaxHeap();
        return max;
    }


   /***********************************************************************
    * Helper functions to restore the heap invariant.
    **********************************************************************/

    private void swim(int k) {
        while (k>1 && less(k>>1,k)) {
            exch(k,k>>1);
            k = k>>1;
        }
    }

    private void sink(int k) {
        while (k<<1 <= N) {
            int j = k<<1;
            if (j < N && less(j, j+1)) j++;
            if (!less(k, j)) break;
            exch(k, j);
            k = j;
        }
    }


    /**
     * Containment and deletion of arbitrary key value.
     */
    public boolean contains (Key key) {return contains(1,key);}

    private boolean contains (int i,Key key) {
        while (i<=N) {
            if (!less(key,pq[i]) && !less(pq[i],key)) return true;
            else if ((i<<1)+1<=N) return contains(i<<1,key)||contains((i<<1)+1,key);
            else if (i<<1<=N) i<<=1;
            else return containsLinear (i+1,key);
        }
        return false;
    }

    private boolean containsLinear (int i,Key key) {
        for (;i<=N;++i)
            if (!less(key,pq[i]) && !less(pq[i],key))
                return true;
        return false;
    }

    public boolean delKey (Key key) {return delKey(1,key);}
    private boolean delKey (int delPos,Key key) { 
        if (isEmpty()) throw new NoSuchElementException("Priority queue underflow");

        while (delPos<=N) {
            if (!less(key,pq[delPos]) && !less(pq[delPos],key)) {
                exch(delPos,N--);
                sink(delPos);
                return true;
            }else if ((delPos<<1)+1<N) return delKey((delPos<<1)+1,key)||delKey(delPos<<1,key);
            else if (delPos<<1<N) return delKey(delPos<<1,key);
            else return delKeyLinear(delPos+1,key);
        }
        return false;
    }

    private boolean delKeyLinear (int delPos,Key key) {
        for (;delPos<=N;++delPos) {
            if (!less(key,pq[delPos]) && !less(pq[delPos],key)) {
                exch(delPos,N--);
                sink(delPos);
                return true;
            }
        }
        return false;
    }

   /***********************************************************************
    * Helper functions for compares and swaps.
    **********************************************************************/
    private boolean less(int i, int j) {
        return comparator==null?((Comparable<Key>)pq[i]).compareTo(pq[j])<0:comparator.compare(pq[i],pq[j])<0;
    }

    private boolean less(Key x, Key y) {
        return comparator==null?((Comparable<Key>) x).compareTo(y)<0:comparator.compare(x,y)<0;
    }

    private void exch(int i, int j) {
        Key swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
    }

    // is pq[1..N] a max heap?
    private boolean isMaxHeap() {
        return isMaxHeap(1);
    }

    // is subtree of pq[1..N] rooted at k a max heap?
    private boolean isMaxHeap(int k) {
        if (k > N) return true;
        int left = k<<1, right = (k<<1) + 1;
        if (left  <= N && less(k, left))  return false;
        if (right <= N && less(k, right)) return false;
        return isMaxHeap(left) && isMaxHeap(right);
    }


   /***********************************************************************
    * Iterator
    **********************************************************************/

   /**
     * Return an iterator that iterates over all of the keys on the priority queue
     * in descending order.
     * <p>
     * The iterator doesn't implement <tt>remove()</tt> since it's optional.
     */
    public Iterator<Key> iterator() { return new HeapIterator(); }

    private class HeapIterator implements Iterator<Key> {

        // create a new pq
        private MaxPQ<Key> copy;

        // add all items to copy of heap
        // takes linear time since already in heap order so no keys move
        public HeapIterator() {
            if (comparator == null) copy = new MaxPQ<Key>(size());
            else                    copy = new MaxPQ<Key>(size(), comparator);
            for (int i = 1; i <= N; i++)
                copy.insert(pq[i]);
        }

        public boolean hasNext()  { return !copy.isEmpty();                     }
        public void remove()      { throw new UnsupportedOperationException();  }

        public Key next() {
            if (!hasNext()) throw new NoSuchElementException();
            return copy.delMax();
        }
    }
}

