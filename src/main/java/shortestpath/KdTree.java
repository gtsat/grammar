package shortestpath;

import shortestpath.Point2D;

import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A k-d tree (short for k-dimensional tree) is a space-partitioning data
 * structure for organizing points in a k-dimensional space. k-d trees are a
 * useful data structure for several applications, such as searches involving a
 * multidimensional search key (e.g. range searches and nearest neighbor
 * searches). k-d trees are a special case of binary space partitioning trees.
 * 
 * http://en.wikipedia.org/wiki/K-d_tree
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
class KdTree {
    private int k = 2;
    private KdNode root = null;

    protected static final int X_AXIS = 0;
    protected static final int Y_AXIS = 1;
    protected static final int Z_AXIS = 2;

    /**
     * Default constructor.
     */
    public KdTree() {}

    /**
     * Constructor for creating a more balanced tree. It uses the
     * "median of points" algorithm.
     * 
     * @param list
     *            of Point2Ds.
     */
    public KdTree(List<Point2D> list) {root = createNode(list, k, 0);}

    /**
     * Create node from list of Point2Ds.
     * 
     * @param list
     *            of Point2Ds.
     * @param k
     *            of the tree.
     * @param depth
     *            depth of the node.
     * @return node created.
     */
    private static KdNode createNode(List<Point2D> list, int k, int depth) {
        if (list==null || list.isEmpty())
            return null;

        int axis = depth % k;
        if (axis == X_AXIS) Collections.sort(list, Point2D.X_ORDER);
        else if (axis == Y_AXIS) Collections.sort(list, Point2D.Y_ORDER);

        KdNode node = null;
        if (list.size() > 0) {
            int medianIndex = list.size() / 2;
            node = new KdNode(k, depth, list.get(medianIndex));
            List<Point2D> less = new ArrayList<>(list.size() - 1);
            List<Point2D> more = new ArrayList<>(list.size() - 1);
            // Process list to see where each non-median point lies
            for (int i = 0; i < list.size(); i++) {
                if (i == medianIndex)
                    continue;
                Point2D p = list.get(i);
                if (KdNode.compareTo(depth, k, p, node.id) <= 0) {
                    less.add(p);
                } else {
                    more.add(p);
                }
            }
            if ((medianIndex - 1) >= 0) {
                // Cannot assume points before the median are less since they
                // could be equal
                // List<Point2D> less = list.subList(0, mediaIndex);
                if (less.size() > 0) {
                    node.lesser = createNode(less, k, depth + 1);
                    node.lesser.parent = node;
                }
            }
            if ((medianIndex + 1) <= (list.size() - 1)) {
                // Cannot assume points after the median are less since they
                // could be equal
                // List<Point2D> more = list.subList(mediaIndex + 1,
                // list.size());
                if (more.size() > 0) {
                    node.greater = createNode(more, k, depth + 1);
                    node.greater.parent = node;
                }
            }
        }

        return node;
    }

    /**
     * Add value to the tree. Tree can contain multiple equal values.
     * 
     * @param value
     *            Point2D to add to the tree.
     * @return True if successfully added to tree.
     */
    public boolean add(Point2D value) {
        if (value == null)
            return false;

        if (root == null) {
            root = new KdNode(value);
            return true;
        }

        KdNode node = root;
        while (true) {
            if (KdNode.compareTo(node.depth, node.k, value, node.id) <= 0) {
                // Lesser
                if (node.lesser == null) {
                    KdNode newNode = new KdNode(k, node.depth + 1, value);
                    newNode.parent = node;
                    node.lesser = newNode;
                    break;
                }
                node = node.lesser;
            } else {
                // Greater
                if (node.greater == null) {
                    KdNode newNode = new KdNode(k, node.depth + 1, value);
                    newNode.parent = node;
                    node.greater = newNode;
                    break;
                }
                node = node.greater;
            }
        }

        return true;
    }

    /**
     * Does the tree contain the value.
     * 
     * @param value
     *            Point2D to locate in the tree.
     * @return True if tree contains value.
     */
    public boolean contains(Point2D value) {
        if (value == null)
            return false;

        KdNode node = getNode(this, value);
        return (node != null);
    }

    /**
     * Locate Point2D in the tree.
     * 
     * @param tree
     *            to search.
     * @param value
     *            to search for.
     * @return KdNode or NULL if not found
     */
    private static final KdNode getNode(KdTree tree, Point2D value) {
        if (tree == null || tree.root == null || value == null)
            return null;

        KdNode node = tree.root;
        while (true) {
            if (node.id.equals(value)) {
                return node;
            } else if (KdNode.compareTo(node.depth, node.k, value, node.id) <= 0) {
                // Lesser
                if (node.lesser == null) {
                    return null;
                }
                node = node.lesser;
            } else {
                // Greater
                if (node.greater == null) {
                    return null;
                }
                node = node.greater;
            }
        }
    }

    /**
     * Remove first occurrence of value in the tree.
     * 
     * @param value
     *            Point2D to remove from the tree.
     * @return True if value was removed from the tree.
     */
    public boolean remove(Point2D value) {
        if (value == null)
            return false;

        KdNode node = getNode(this, value);
        if (node == null)
            return false;

        KdNode parent = node.parent;
        if (parent != null) {
            if (parent.lesser != null && node.equals(parent.lesser)) {
                List<Point2D> nodes = getTree(node);
                if (nodes.size() > 0) {
                    parent.lesser = createNode(nodes, node.k, node.depth);
                    if (parent.lesser != null) {
                        parent.lesser.parent = parent;
                    }
                } else {
                    parent.lesser = null;
                }
            } else {
                List<Point2D> nodes = getTree(node);
                if (nodes.size() > 0) {
                    parent.greater = createNode(nodes, node.k, node.depth);
                    if (parent.greater != null) {
                        parent.greater.parent = parent;
                    }
                } else {
                    parent.greater = null;
                }
            }
        } else {
            // root
            List<Point2D> nodes = getTree(node);
            if (nodes.size() > 0)
                root = createNode(nodes, node.k, node.depth);
            else
                root = null;
        }

        return true;
    }

    /**
     * Get the (sub) tree rooted at root.
     * 
     * @param root
     *            of tree to get nodes for.
     * @return points in (sub) tree, not including root.
     */
    private static final List<Point2D> getTree(KdNode root) {
        List<Point2D> list = new ArrayList<>();
        if (root == null)
            return list;

        if (root.lesser != null) {
            list.add(root.lesser.id);
            list.addAll(getTree(root.lesser));
        }
        if (root.greater != null) {
            list.add(root.greater.id);
            list.addAll(getTree(root.greater));
        }

        return list;
    }

    public Collection<Point2D> rangeSearch (Point2D lo,Point2D hi) {
        ArrayDeque<Point2D> result = new ArrayDeque<>();
        if (root!=null) rangeSearch (lo,hi,root,result);
        return result;
    }

    private void rangeSearch (Point2D lo,Point2D hi,KdNode node,Collection<Point2D> result) {
        int splitdim = node.depth%2;
        float val = (splitdim==0?node.id.x():node.id.y());
        float loo = (splitdim==0?lo.x():lo.y());
        float hii = (splitdim==0?hi.x():hi.y());
        if (val<=loo) {
            if (node.greater!=null) rangeSearch (lo,hi,node.greater,result);
            if (val==loo) result.add (node.id);
        }else if (val>=hii) {
            if (node.lesser!=null) rangeSearch (lo,hi,node.lesser,result);
            if (val==hii) result.add (node.id);
        }else{
            if (node.greater!=null) rangeSearch (lo,hi,node.greater,result);
            if (node.lesser!=null) rangeSearch (lo,hi,node.lesser,result);
            result.add (node.id);
        }
    }

    /**
     * K Nearest Neighbor search
     * 
     * @param K
     *            Number of neighbors to retrieve. Can return more than K, if
     *            last nodes are equal distances.
     * @param value
     *            to find neighbors of.
     * @return collection of Point2D neighbors.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Point2D> nearestNeighbourSearch(int K, Point2D value) {
        if (value == null)
            return null;

        // Map used for results
        TreeSet<KdNode> results = new TreeSet<>(new EuclideanComparator(value));

        // Find the closest leaf node
        KdNode prev = null;
        KdNode node = root;
        while (node != null) {
            if (KdNode.compareTo(node.depth, node.k, value, node.id) <= 0) {
                // Lesser
                prev = node;
                node = node.lesser;
            } else {
                // Greater
                prev = node;
                node = node.greater;
            }
        }
        KdNode leaf = prev;

        if (leaf != null) {
            // Used to not re-examine nodes
            Set<KdNode> examined = new HashSet<>();

            // Go up the tree, looking for better solutions
            node = leaf;
            while (node != null) {
                // Search node
                searchNode(value, node, K, results, examined);
                node = node.parent;
            }
        }

        // Load up the collection of the results
        ArrayList<Point2D> collection = new ArrayList<>(K);
        for (KdNode kdNode : results) {
            collection.add(kdNode.id);
        }
        return collection;
    }

    private static final void searchNode(Point2D value, KdNode node, int K,
            TreeSet<KdNode> results, Set<KdNode> examined) {
        examined.add(node);

        // Search node
        KdNode lastNode = null;
        Float lastDistance = Float.MAX_VALUE;
        if (results.size() > 0) {
            lastNode = results.last();
            lastDistance = lastNode.id.distanceTo(value);
        }
        Float nodeDistance = node.id.distanceTo(value);
        if (nodeDistance.compareTo(lastDistance) < 0) {
            if (results.size() == K && lastNode != null)
                results.remove(lastNode);
            results.add(node);
        } else if (nodeDistance.equals(lastDistance)) {
            results.add(node);
        } else if (results.size() < K) {
            results.add(node);
        }
        lastNode = results.last();
        lastDistance = lastNode.id.distanceTo(value);

        int axis = node.depth % node.k;
        KdNode lesser = node.lesser;
        KdNode greater = node.greater;

        // Search children branches, if axis aligned distance is less than
        // current distance
        if (lesser != null && !examined.contains(lesser)) {
            examined.add(lesser);

            float nodePoint = Float.MIN_VALUE;
            float valuePlusDistance = Float.MIN_VALUE;
            if (axis == X_AXIS) {
                nodePoint = node.id.x();
                valuePlusDistance = value.x() - lastDistance;
            } else if (axis == Y_AXIS) {
                nodePoint = node.id.y();
                valuePlusDistance = value.y() - lastDistance;
            }
            boolean lineIntersectsCube = valuePlusDistance <= nodePoint;

            // Continue down lesser branch
            if (lineIntersectsCube)
                searchNode(value, lesser, K, results, examined);
        }
        if (greater != null && !examined.contains(greater)) {
            examined.add(greater);

            float nodePoint = Float.MIN_VALUE;
            float valuePlusDistance = Float.MIN_VALUE;
            if (axis == X_AXIS) {
                nodePoint = node.id.x();
                valuePlusDistance = value.x() + lastDistance;
            } else if (axis == Y_AXIS) {
                nodePoint = node.id.y();
                valuePlusDistance = value.y() + lastDistance;
            }
            boolean lineIntersectsCube = valuePlusDistance >= nodePoint;

            // Continue down greater branch
            if (lineIntersectsCube)
                searchNode(value, greater, K, results, examined);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return TreePrinter.getString(this);
    }

    protected static class EuclideanComparator implements Comparator<KdNode> {

        private Point2D point = null;

        public EuclideanComparator(Point2D point) {
            this.point = point;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(KdNode o1, KdNode o2) {
            Float d1 = point.distanceTo(o1.id);
            Float d2 = point.distanceTo(o2.id);
            if (d1.compareTo(d2) < 0)
                return -1;
            else if (d2.compareTo(d1) < 0)
                return 1;
            return o1.id.compareTo(o2.id);
        }
    };

    private static class KdNode implements Comparable<KdNode> {

        private int k = 2;
        private int depth = 0;
        private Point2D id = null;
        private KdNode parent = null;
        private KdNode lesser = null;
        private KdNode greater = null;

        public KdNode(Point2D id) {
            this.id = id;
        }

        public KdNode(int k, int depth, Point2D id) {
            this(id);
            this.k = k;
            this.depth = depth;
        }

        public static int compareTo (int depth, int k, Point2D o1, Point2D o2) {
            return depth % k == X_AXIS?Point2D.X_ORDER.compare(o1, o2):Point2D.Y_ORDER.compare(o1, o2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof KdNode))
                return false;

            KdNode kdNode = (KdNode) obj;
            if (this.compareTo(kdNode) == 0)
                return true;
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(KdNode o) {
            return compareTo(depth, k, this.id, o.id);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("k=").append(k);
            builder.append(" depth=").append(depth);
            builder.append(" id=").append(id.toString());
            return builder.toString();
        }
    }

    protected static class TreePrinter {

        public static String getString(KdTree tree) {
            if (tree.root == null)
                return "Tree has no nodes.";
            return getString(tree.root, "", true);
        }

        private static String getString(KdNode node, String prefix, boolean isTail) {
            StringBuilder builder = new StringBuilder();

            if (node.parent != null) {
                String side = "left";
                if (node.parent.greater != null && node.id.equals(node.parent.greater.id))
                    side = "right";
                builder.append(prefix + (isTail ? "+-- " : "+-- ") + "[" + side + "] " + "depth=" + node.depth + " id="
                        + node.id + "\n");
            } else {
                builder.append(prefix + (isTail ? "+-- " : "+-- ") + "depth=" + node.depth + " id=" + node.id + "\n");
            }
            List<KdNode> children = null;
            if (node.lesser != null || node.greater != null) {
                children = new ArrayList<>(2);
                if (node.lesser != null)
                    children.add(node.lesser);
                if (node.greater != null)
                    children.add(node.greater);
            }
            if (children != null) {
                for (int i = 0; i < children.size() - 1; i++) {
                    builder.append(getString(children.get(i), prefix + (isTail ? "    " : "|   "), false));
                }
                if (children.size() >= 1) {
                    builder.append(getString(children.get(children.size() - 1), prefix + (isTail ? "    " : "|   "), true));
                }
            }

            return builder.toString();
        }
    }
}

