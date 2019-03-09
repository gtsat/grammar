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

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;


final public class Edge<NodeT> implements Comparable<Edge<NodeT>> {	// add color
	public final NodeT from,to;
	public final Float weight;
        
	public Edge (NodeT f, NodeT t, Float w) {from=f;to=t;weight=w;}
        
	public Edge<NodeT> reverse () {return new Edge<>(to,from,weight);}
        
        @Override
	public String toString () {return "('from': " + from + ", 'to': " + to + ", 'weight': " + weight + ")";}
        
        @Override
	@SuppressWarnings("unchecked")
	public boolean equals (Object other) {
            if (other == null) return false;
            if (other == this) return true;
            return other instanceof Edge<?> 
	    && from.equals(((Edge<NodeT>)other).from) 
	    && to.equals(((Edge<NodeT>)other).to) 
	    && weight.equals(((Edge<NodeT>)other).weight);}
        
        @Override
	public Edge<NodeT> clone () {return new Edge<>(from,to,weight);}
        
        @Override
	public int compareTo (Edge<NodeT> other) {
                if (other==null) return -1;
		if (weight<other.weight) return -1;
		else if (weight>other.weight) return 1;
		else return 0;
	}
}

final class BitsetSparse<DomT> {
	private final HashSet<DomT> used = new HashSet<>();
	private final boolean predefined;

	public BitsetSparse () {predefined=false;}
	public BitsetSparse (boolean init) {predefined=init;}
	public boolean contains (DomT key) {return used.contains(key);}
	public boolean get (DomT i) {return used.contains(i)?!predefined:predefined;}
	public boolean set (DomT i) {return used.add(i);}
	public boolean flip (DomT i) {return used.contains(i)?used.remove(i):used.add(i);}
	public boolean remove (DomT i) {return used.remove(i);}
	public void clear () {used.clear();}
	public boolean isEmpty () {return used.isEmpty();}
        public int numberEntries () {return used.size();}
        public Iterable<DomT> iterator () {return used;}

        @Override
	public BitsetSparse<DomT> clone () {
		BitsetSparse<DomT> bitset = new BitsetSparse<> (predefined);
		bitset.used.addAll (used);
		return bitset;
	}

	public boolean equals (BitsetSparse<DomT> other) {
            if (other == null) return false;
            if (other == this) return true;
            if (getClass().equals(other.getClass())) {
                if(predefined & other.predefined) {
                    if (used.containsAll(other.used) && other.used.containsAll(used)) {
                        return true;
                    }
                }else{ // local set should contain none element of the other set
                    HashSet<DomT> localcopy = new HashSet<>(used); // set to predefined
                    HashSet<DomT> remotecopy = new HashSet<>(other.used); // set to ~predefined
                    localcopy.retainAll (other.used); // keep the ones that are set differently in the remote copy
                    remotecopy.retainAll(used); // keep the ones that are set differently in the local copy
                    if (localcopy.isEmpty() && remotecopy.isEmpty() ) {
                        return true;
                    }
                }
            }
            return false;
	}
}


final class VectorSparse<DomT,ImT> {
	private final HashMap<DomT,ImT> map = new HashMap<>();
	private final ImT predefined;

	public VectorSparse () {predefined=null;}
	public VectorSparse (ImT init) {predefined=init;}
	public boolean contains (DomT key) {return map.containsKey(key);}
	public ImT get (DomT key) {return map.containsKey(key)?map.get(key):predefined;}
	public void set (DomT key, ImT value) {map.put(key,value);}
	public ImT remove (DomT i) {ImT temp=map.remove(i);return temp!=null?temp:predefined;}
	public void clear () {map.clear();}
	public boolean isEmpty() {return map.isEmpty();}
	public int size () {return map.size();}
        public Map<DomT,ImT> thisMap () {return map;}

        public float innerproduct (VectorSparse<DomT,ImT> other) { // for cosine similarity
            //assuming predefined == 0
            if (!predefined.equals(0.0) || !other.predefined.equals(0.0))
                throw new RuntimeException("\n!! ERROR - Predefined values are not equal to zero !!");

            Float sum = 0.0f;
            if (map.size()<other.map.size()) {
                for (Map.Entry<DomT,ImT> entry : map.entrySet()) {
                    ImT otherval = other.map.get(entry.getKey());
                    if (otherval!=null) { // throws exception when casting is not possible
                        sum += ((Float)entry.getValue())*((Float)otherval);
                    }
                }
            }else{
                for (Map.Entry<DomT,ImT> entry : other.map.entrySet()) {
                    ImT thisval = map.get(entry.getKey());
                    if (thisval!=null) { // throws exception when casting is not possible
                        sum += ((Float)entry.getValue())*((Float)thisval);
                    }
                }
            }
            return sum;
        }

        @Override
	public VectorSparse<DomT,ImT> clone () {
		VectorSparse<DomT,ImT> vectorsparse = new VectorSparse<>(predefined);
		vectorsparse.map.putAll(map);
		return vectorsparse;
	}

	public boolean equals (VectorSparse<DomT,ImT> other) {
            if (other == null) return false;
            if (other == this) return true;
            if (getClass().equals(other.getClass())) {
                if (predefined.equals(other.predefined)) {
                    if (map.entrySet().equals(other.map.entrySet()))
                        return true;
                }
            }
            return false;
	}

        @Override
	public String toString () {
		int charcounter=0;
		int lastkeychars=0;
		StringBuilder print = new StringBuilder();
		print.append("      ");
		for (DomT key : map.keySet()) {
			print.append (key);
			for (int i=key.toString().length(); i<12; ++i) 
				print.append (" ");
			charcounter += 12;
			lastkeychars = key.toString().length();
		}
		charcounter -= 12 - lastkeychars;
		
		print.append("\n      ");
		for (int i=0; i<charcounter; ++i) 
			print.append("-");

		print.append ("\n [");
		for (Map.Entry<DomT,ImT> entry : map.entrySet())
			print.append (String.format("%10.3f ", entry.getValue())) ;
		print.append ("  ]\n");
		return print.toString();
	}
	public Collection<DomT> keySet () {return map.keySet();}
	public Collection<Map.Entry<DomT,ImT>> entrySet () {return map.entrySet();}
}


final class WQUF<NodeT> {
	private final VectorSparse<NodeT,Integer> sz = new VectorSparse<>(0);
	private final VectorSparse<NodeT,NodeT> id = new VectorSparse<>();

	public boolean connected (NodeT u, NodeT v) {return find(u).equals(find(v));}
	public NodeT find (NodeT u) {
		NodeT prev=u, next=id.get(u);
		while (next!=null) {
			if (prev!=u) id.set(prev,next);
			prev=u; u=next;
			next=id.get(u);
		}
		return u;
	}

	public void union (NodeT u, NodeT v) {
		NodeT U=find(u), V=find(v);
		if (!U.equals(V)) {
			if (sz.get(U)<sz.get(V)) {
				id.set (U,V);
				sz.set (V,sz.get(V)+sz.get(U));
			}else{
				id.set (V,U);
				sz.set (U,sz.get(V)+sz.get(U));
			}
		}
	}
}

