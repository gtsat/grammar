/*************************************************************************
 *  Compilation:  javac Point2D.java
 *
 *  Immutable point data type for points in the plane.
 *
 *************************************************************************/

package shortestpath;

import java.util.Comparator;

/**
 *  The <tt>Point</tt> class is an immutable data type to encapsulate a
 *  two-dimensional point with real-value coordinates.
 *  <p>
 *  For additional documentation, see <a href="/algs4/12oop">Section 1.2</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
final public class Point2D implements Comparable<Point2D> {
   /**
     * Compares two points by x-coordinate.
     */
    public static final Comparator<Point2D> X_ORDER = new XOrder();

   /**
     * Compares two points by y-coordinate.
     */
    public static final Comparator<Point2D> Y_ORDER = new YOrder();

   /**
     * Compares two points by polar radius.
     */
    public static final Comparator<Point2D> R_ORDER = new ROrder();

   /**
     * Compares two points by polar angle (between 0 and 2pi) with respect to this point.
     */
    public final Comparator<Point2D> POLAR_ORDER = new PolarOrder();

   /**
     * Compares two points by atan2() angle (between -pi and pi) with respect to this point.
     */
    public final Comparator<Point2D> ATAN2_ORDER = new Atan2Order();

   /**
     * Compares two points by distance to this point.
     */
    public final Comparator<Point2D> DISTANCE_TO_ORDER = new DistanceToOrder();

    private final float x;    // x coordinate
    private final float y;    // y coordinate

   /**
     * Creates a new point (x, y).
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public Point2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

   /**
     * Returns the x-coordinate.
     */
    public float x() {return x;}

   /**
     * Returns the y-coordinate.
     */
    public float y() {return y;}

   /**
     * Returns the polar radius of this point in polar coordinates: sqrt(x*x + y*y).
     */
    public float r() {return (float) Math.sqrt(x*x + y*y);}

   /**
     * Returns the angle (in radians) of this point in polar coordinates (between -pi/2 and pi/2).
     */
    public float theta() {return (float) Math.atan2(y, x);}

   /**
     * Returns the angle in radians (between -pi and pi) between this point and that point (0 if equal).
     */
    private float angleTo(Point2D that) {
        float dx = that.x - this.x;
        float dy = that.y - this.y;
        return (float) Math.atan2(dy, dx);
    }

   /**
     * Is a->b->c a counterclockwise turn?
     * -1 if clockwise, +1 if counter-clockwise, 0 if collinear.
     */
    public static int ccw(Point2D a, Point2D b, Point2D c) {
        float area2 = (b.x-a.x)*(c.y-a.y) - (b.y-a.y)*(c.x-a.x);
        if      (area2 < 0) return -1;
        else if (area2 > 0) return +1;
        else                return  0;
    }

   /**
     * Returns twice the signed area of the triangle a-b-c.
     */
    public static float area2(Point2D a, Point2D b, Point2D c) {
        return (b.x-a.x)*(c.y-a.y) - (b.y-a.y)*(c.x-a.x);
    }

   /**
     * Returns the Euclidean distance between this point and that point.
     */
    public float distanceTo(Point2D that) {
        float dx = this.x - that.x;
        float dy = this.y - that.y;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

   /**
     * Returns the square of the Euclidean distance between this point and that point.
     */
    public float distanceSquaredTo(Point2D that) {
        float dx = this.x - that.x;
        float dy = this.y - that.y;
        return dx*dx + dy*dy;
    }

   /**
     * Compares this point to that point by y-coordinate, breaking ties by x-coordinate.
     */
    public int compareTo(Point2D that) {
        if (this.y < that.y) return -1;
        if (this.y > that.y) return +1;
        if (this.x < that.x) return -1;
        if (this.x > that.x) return +1;
        return 0;
    }

    // compare points according to their x-coordinate
    private static class XOrder implements Comparator<Point2D> {
        public int compare(Point2D p, Point2D q) {
            if (p.x < q.x) return -1;
            if (p.x > q.x) return +1;
            return 0;
        }
    }

    // compare points according to their y-coordinate
    private static class YOrder implements Comparator<Point2D> {
        public int compare(Point2D p, Point2D q) {
            if (p.y < q.y) return -1;
            if (p.y > q.y) return +1;
            return 0;
        }
    }

    // compare points according to their polar radius
    private static class ROrder implements Comparator<Point2D> {
        public int compare(Point2D p, Point2D q) {
            float delta = (p.x*p.x + p.y*p.y) - (q.x*q.x + q.y*q.y);
            if (delta < 0) return -1;
            if (delta > 0) return +1;
            return 0;
        }
    }
 
    // compare other points relative to atan2 angle (bewteen -pi/2 and pi/2) they make with this Point
    private class Atan2Order implements Comparator<Point2D> {
        public int compare(Point2D q1, Point2D q2) {
            float angle1 = angleTo(q1);
            float angle2 = angleTo(q2);
            if      (angle1 < angle2) return -1;
            else if (angle1 > angle2) return +1;
            else                      return  0;
        }
    }

    // compare other points relative to polar angle (between 0 and 2pi) they make with this Point
    private class PolarOrder implements Comparator<Point2D> {
        public int compare(Point2D q1, Point2D q2) {
            float dx1 = q1.x - x;
            float dy1 = q1.y - y;
            float dx2 = q2.x - x;
            float dy2 = q2.y - y;

            if      (dy1 >= 0 && dy2 < 0) return -1;    // q1 above; q2 below
            else if (dy2 >= 0 && dy1 < 0) return +1;    // q1 below; q2 above
            else if (dy1 == 0 && dy2 == 0) {            // 3-collinear and horizontal
                if      (dx1 >= 0 && dx2 < 0) return -1;
                else if (dx2 >= 0 && dx1 < 0) return +1;
                else                          return  0;
            }
            else return -ccw(Point2D.this, q1, q2);     // both above or below

            // Note: ccw() recomputes dx1, dy1, dx2, and dy2
        }
    }

    // compare points according to their distance to this point
    private class DistanceToOrder implements Comparator<Point2D> {
        public int compare(Point2D p, Point2D q) {
            float dist1 = distanceSquaredTo(p);
            float dist2 = distanceSquaredTo(q);
            if      (dist1 < dist2) return -1;
            else if (dist1 > dist2) return +1;
            else                    return  0;
        }
    }


   /**
     * Does this point equal y?
     */
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
        Point2D that = (Point2D) other;
        return this.x == that.x && this.y == that.y;
    }

   /**
     * Return a string representation of this point.
     */
    public String toString() {return "(" + x + ", " + y + ")";}
}
