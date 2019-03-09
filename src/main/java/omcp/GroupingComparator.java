
package omcp;

import oscp.Group;
import grammar.Graph;
import java.util.HashMap;
import java.util.Comparator;

abstract public class GroupingComparator<NodeT> {

    final protected Graph<NodeT> graph;
    final protected HashMap<Group<NodeT>,Float> locache = new HashMap<>();
    final protected HashMap<Group<NodeT>,Float> hicache = new HashMap<>();

    final public Comparator<Group<NodeT>> BYLOWERBOUND = new LowerBoundsComparator();
    final public Comparator<Group<NodeT>> BYUPPERBOUND = new UpperBoundsComparator();
    
    abstract public float computeLowerBound (Group<NodeT>x);
    abstract public float computeUpperBound (Group<NodeT>x);


    public GroupingComparator (Graph<NodeT> network) {graph=network;}

    private class LowerBoundsComparator implements Comparator<Group<NodeT>> {
        @Override
        public int compare (Group<NodeT> x, Group<NodeT> y) {
            if (x==null && y==null) return 0;
            else if (y==null) return -1;
            else if (x==null) return 1;
            else{
                float balance = computeLowerBound(x)-computeLowerBound(y);
                if (balance<0) return -1;
                else if (balance>0) return 1;
                else return 0;
            }
        }
    }

    private class UpperBoundsComparator implements Comparator<Group<NodeT>> {
        @Override
        public int compare (Group<NodeT> x, Group<NodeT> y) {
            if (x==null && y==null) return 0;
            else if (y==null) return -1;
            else if (x==null) return 1;
            else{
                float balance = computeUpperBound(x)-computeUpperBound(y);
                if (balance<0) return -1;
                else if (balance>0) return 1;
                else return 0;
            }
        }
    }
}
