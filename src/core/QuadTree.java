package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * A quadtree is a tree data structure in which each internal node has exactly four children. Quadtrees
 * are most often used to partition a two dimensional space by recursively subdividing it into four
 * quadrants or regions. The regions may be square or rectangular, or may have arbitrary shapes.
 *
 * http://en.wikipedia.org/wiki/Quadtree
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
@SuppressWarnings("unchecked")
public abstract class QuadTree<G extends QuadTree.XYPoint> {

    /**
     * Get the root node.
     *
     * @return Root QuadNode.
     */
    protected abstract QuadNode<G> getRoot();

    /**
     * Range query of the quadtree.
     */
    public abstract Collection<G> queryRange(double x, double y, double width, double height);

    /**
     * Insert point at X,Y into tree.
     *
     * @param x X position of point.
     * @param y Y position of point.
     */
    public abstract boolean insert(double x, double y);

    /**
     * Remove point at X,Y from tree.
     *
     * @param x X position of point.
     * @param y Y position of point.
     */
    public abstract boolean remove(double x, double y);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return TreePrinter.getString(this);
    }

    public String toJson() {
        return TreePrinter.getJson(this);
    }

    /**
     * A PR (Point Region) Quadtree is a four-way search trie. This means that each node has either
     * four (internal guide node) or zero (leaf node) children. Keys are only stored in the leaf nodes,
     * all internal nodes act as guides towards the keys.
     *
     * This implementation is a PR QuadTree which uses "Buckets" to prevent stalky trees.
     */
    public static class PointRegionQuadTree<P extends QuadTree.XYPoint> extends QuadTree<P> {

        private static final XYPoint XY_POINT = new XYPoint();
        private static final AxisAlignedBoundingBox RANGE = new AxisAlignedBoundingBox();
        public static int currentHeight = 0;

        private PointRegionQuadNode<P> root = null;

        /**
         * Create a quadtree who's upper left coordinate is located at x,y and it's bounding box is described
         * by the height and width. This uses a default leafCapacity of 4 and a maxTreeHeight of 20.
         *
         * @param x Upper left X coordinate
         * @param y Upper left Y coordinate
         * @param width Width of the bounding box containing all points
         * @param height Height of the bounding box containing all points
         */
        public PointRegionQuadTree(double x, double y, double width, double height) {
            this(x,y,width,height,4,20);
        }

        /**
         * Create a quadtree who's upper left coordinate is located at x,y and it's bounding box is described
         * by the height and width.
         *
         * @param x Upper left X coordinate
         * @param y Upper left Y coordinate
         * @param width Width of the bounding box containing all points
         * @param height Height of the bounding box containing all points
         * @param leafCapacity Max capacity of leaf nodes. (Note: All data is stored in leaf nodes)
         */
        public PointRegionQuadTree(double x, double y, double width, double height, int leafCapacity) {
            this(x,y,width,height,leafCapacity,20);
        }

        /**
         * Create a quadtree who's upper left coordinate is located at x,y and it's bounding box is described
         * by the height and width.
         *
         * @param x Upper left X coordinate
         * @param y Upper left Y coordinate
         * @param width Width of the bounding box containing all points
         * @param height Height of the bounding box containing all points
         * @param leafCapacity Max capacity of leaf nodes. (Note: All data is stored in leaf nodes)
         * @param maxTreeHeight Max height of the quadtree. (Note: If this is defined, the tree will ignore the
         *                                                   max capacity defined by leafCapacity)
         */
        public PointRegionQuadTree(double x, double y, double width, double height, int leafCapacity, int maxTreeHeight) {
            XYPoint xyPoint = new XYPoint(x,y);
            AxisAlignedBoundingBox aabb = new AxisAlignedBoundingBox(xyPoint,width,height, 0);
            PointRegionQuadNode.maxCapacity = leafCapacity;
            PointRegionQuadNode.maxHeight = maxTreeHeight;
            root = new PointRegionQuadNode<P>(aabb, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QuadTree.QuadNode<P> getRoot() {
            return root;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean insert(double x, double y) {
            XYPoint xyPoint = new XYPoint(x,y);
            return root.insert((P)xyPoint);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(double x, double y) {
            XY_POINT.set(x,y);

            return root.remove((P)XY_POINT);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<P> queryRange(double x, double y, double width, double height) {
            if (root == null)
                return Collections.EMPTY_LIST;

            XY_POINT.set(x,y);
            RANGE.set(XY_POINT,width,height);

            List<P> pointsInRange = new LinkedList<P>();
            root.queryRange(RANGE,pointsInRange);
            return pointsInRange;
        }

        public static class PointRegionQuadNode<XY extends QuadTree.XYPoint> extends QuadNode<XY> {

            // max number of children before sub-dividing
            protected static int maxCapacity = 0;
            // max height of the tree (will over-ride maxCapacity when height==maxHeight)
            protected static int maxHeight = 0;

            public List<XY> points = new LinkedList<XY>();
            protected int height = 1;

            protected PointRegionQuadNode(AxisAlignedBoundingBox aabb, PointRegionQuadNode<XY> parent) {
                super(aabb, parent);
            }

            /**
             * {@inheritDoc}
             *
             * returns True if inserted.
             * returns False if not in bounds of tree OR tree already contains point.
             */
            @Override
            protected boolean insert(XY p) {
                // Ignore objects which do not belong in this quad tree
                if (!aabb.containsPoint(p) || (isLeaf() && points.contains(p)))
                    return false; // object cannot be added

                // If there is space in this quad tree, add the object here
                if ((height==maxHeight) || (isLeaf() && points.size() < maxCapacity)) {
                    points.add(p);
                    return true;
                }

                // Otherwise, we need to subdivide then add the point to whichever node will accept it
                if (isLeaf() && height<maxHeight) {
                    currentHeight = aabb.level + 1 > currentHeight ? aabb.level + 1 : currentHeight;
                    subdivide();
                }
                return insertIntoChildren(p);
            }

            /**
             * {@inheritDoc}
             *
             * This method will merge children into self if it can without overflowing the maxCapacity param.
             */
            @Override
            protected boolean remove(XY p) {
                // If not in this AABB, don't do anything
                if (!aabb.containsPoint(p))
                    return false;

                // If in this AABB and in this node
                if (points.remove(p))
                    return true;

                // If this node has children
                if (!isLeaf()) {
                    // If in this AABB but in a child branch
                    boolean removed = removeFromChildren(p);
                    if (!removed)
                        return false;

                    // Try to merge children
                    merge();

                    return true;
                }

                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected int size() {
                return points.size();
            }

            private void subdivide() {
                double h = aabb.height/2d;
                double w = aabb.width/2d;

                AxisAlignedBoundingBox aabbNW = new AxisAlignedBoundingBox(aabb,w,h, aabb.level + 1);
                northWest = new PointRegionQuadNode<XY>(aabbNW, this);
                ((PointRegionQuadNode<XY>)northWest).height = height+1;

                XYPoint xyNE = new XYPoint(aabb.x+w,aabb.y);
                AxisAlignedBoundingBox aabbNE = new AxisAlignedBoundingBox(xyNE,w,h, aabb.level + 1);
                northEast = new PointRegionQuadNode<XY>(aabbNE, this);
                ((PointRegionQuadNode<XY>)northEast).height = height+1;

                XYPoint xySW = new XYPoint(aabb.x,aabb.y+h);
                AxisAlignedBoundingBox aabbSW = new AxisAlignedBoundingBox(xySW,w,h, aabb.level + 1);
                southWest = new PointRegionQuadNode<XY>(aabbSW, this);
                ((PointRegionQuadNode<XY>)southWest).height = height+1;

                XYPoint xySE = new XYPoint(aabb.x+w,aabb.y+h);
                AxisAlignedBoundingBox aabbSE = new AxisAlignedBoundingBox(xySE,w,h, aabb.level + 1);
                southEast = new PointRegionQuadNode<XY>(aabbSE, this);
                ((PointRegionQuadNode<XY>)southEast).height = height+1;

                // points live in leaf nodes, so distribute
                for (XY p : points)
                    insertIntoChildren(p);
                points.clear();
            }

            private void merge() {
                // If the children aren't leafs, you cannot merge
                if (!northWest.isLeaf() || !northEast.isLeaf() || !southWest.isLeaf() || !southEast.isLeaf())
                    return;

                // Children and leafs, see if you can remove point and merge into this node
                int nw = northWest.size();
                int ne = northEast.size();
                int sw = southWest.size();
                int se = southEast.size();
                int total = nw+ne+sw+se;

                // If all the children's point can be merged into this node
                if ((size()+total) < maxCapacity) {
                    this.points.addAll(((PointRegionQuadNode<XY>)northWest).points);
                    this.points.addAll(((PointRegionQuadNode<XY>)northEast).points);
                    this.points.addAll(((PointRegionQuadNode<XY>)southWest).points);
                    this.points.addAll(((PointRegionQuadNode<XY>)southEast).points);

                    this.northWest = null;
                    this.northEast = null;
                    this.southWest = null;
                    this.southEast = null;
                }
            }

            private boolean insertIntoChildren(XY p) {
                // A point can only live in one child.
                if (northWest.insert(p)) return true;
                if (northEast.insert(p)) return true;
                if (southWest.insert(p)) return true;
                if (southEast.insert(p)) return true;
                return false; // should never happen
            }

            private boolean removeFromChildren(XY p) {
                // A point can only live in one child.
                if (northWest.remove(p)) return true;
                if (northEast.remove(p)) return true;
                if (southWest.remove(p)) return true;
                if (southEast.remove(p)) return true;
                return false; // should never happen
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void queryRange(AxisAlignedBoundingBox range, List<XY> pointsInRange) {
                // Automatically abort if the range does not collide with this quad
                if (!aabb.intersectsBox(range))
                    return;

                // If leaf, check objects at this level
                if (isLeaf()) {
                    for (XY xyPoint : points) {
                        if (range.containsPoint(xyPoint))
                            pointsInRange.add(xyPoint);
                    }
                    return;
                }

                // Otherwise, add the points from the children
                northWest.queryRange(range,pointsInRange);
                northEast.queryRange(range,pointsInRange);
                southWest.queryRange(range,pointsInRange);
                southEast.queryRange(range,pointsInRange);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append(super.toString()).append(", ");
                builder.append("[");
                for (XYPoint p : points) {
                    builder.append(p).append(", ");
                }
                builder.append("]");
                return builder.toString();
            }
        }
    }

    public static abstract class QuadNode<G extends QuadTree.XYPoint> implements Comparable<QuadNode<G>> {

        public final AxisAlignedBoundingBox aabb;

        enum Loc {
            NW, NE, SW, SE
        };

        enum Dir {
            N, E, S, W
        }

        public QuadNode<G> northWest = null;
        public QuadNode<G> northEast = null;
        public QuadNode<G> southWest = null;
        public QuadNode<G> southEast = null;
        protected QuadNode<G> parent = null;

        protected QuadNode(AxisAlignedBoundingBox aabb, QuadNode<G> parent) {
            this.aabb = aabb;
            this.parent = parent;
        }

        /**
         * Insert object into tree.
         *
         * @param g Geometric object to insert into tree.
         * @return True if successfully inserted.
         */
        protected abstract boolean insert(G g);

        /**
         * Remove object from tree.
         *
         * @param g Geometric object to remove from tree.
         * @return True if successfully removed.
         */
        protected abstract boolean remove(G g);

        /**
         * How many GeometricObjects this node contains.
         *
         * @return Number of GeometricObjects this node contains.
         */
        protected abstract int size();

        /**
         * Find all objects which appear within a range.
         *
         * @param range Upper-left and width,height of a axis-aligned bounding box.
         * @param geometricObjectsInRange Geometric objects inside the bounding box.
         */
        protected abstract void queryRange(AxisAlignedBoundingBox range, List<G> geometricObjectsInRange);

        /**
         * Is current node a leaf node.
         * @return True if node is a leaf node.
         */
        public boolean isLeaf() {
            return (northWest==null && northEast==null && southWest==null && southEast==null);
        }

        /**
         * Find the north neighbor
         * @return The north neighboring node if it exists, null otherwise
         */
        public QuadNode<G> northNeighbor() {
            if(parent == null) {
                return null;
            }

            Loc thisLoc = location();

            QuadNode<G> pi = parent;

            if (thisLoc == Loc.SW) {
                return pi.northWest;
            } else if (thisLoc == Loc.SE) {
                return pi.northEast;
            }

            QuadNode<G> mu = pi.northNeighbor();
            if ( mu == null || mu.isLeaf()) {
                return mu;
            } else {
                if (thisLoc == Loc.NW) {
                    return mu.southWest;
                } else if (thisLoc == Loc.NE) {
                    return mu.southEast;
                }
            }
            return null;
        }

        public QuadNode<G> eastNeighbor() {
            if(parent == null) {
                return null;
            }

            Loc thisLoc = location();

            QuadNode<G> pi = parent;

            if (thisLoc == Loc.SW) {
                return pi.southEast;
            } else if (thisLoc == Loc.NW) {
                return pi.northEast;
            }

            QuadNode<G> mu = pi.eastNeighbor();
            if ( mu == null || mu.isLeaf()) {
                return mu;
            } else {
                if (thisLoc == Loc.SE) {
                    return mu.southWest;
                } else if (thisLoc == Loc.NE) {
                    return mu.northWest;
                }
            }
            return null;
        }

        public QuadNode<G> southNeighbor() {
            if(parent == null) {
                return null;
            }

            Loc thisLoc = location();

            QuadNode<G> pi = parent;

            if (thisLoc == Loc.NW) {
                return pi.southWest;
            } else if (thisLoc == Loc.NE) {
                return pi.southEast;
            }

            QuadNode<G> mu = pi.southNeighbor();
            if ( mu == null || mu.isLeaf()) {
                return mu;
            } else {
                if (thisLoc == Loc.SW) {
                    return mu.northWest;
                } else if (thisLoc == Loc.SE) {
                    return mu.northEast;
                }
            }
            return null;
        }

        public QuadNode<G> westNeighbor() {
            if(parent == null) {
                return null;
            }

            Loc thisLoc = location();

            QuadNode<G> pi = parent;

            if (thisLoc == Loc.NE) {
                return pi.northWest;
            } else if (thisLoc == Loc.SE) {
                return pi.southWest;
            }

            QuadNode<G> mu = pi.westNeighbor();
            if ( mu == null || mu.isLeaf()) {
                return mu;
            } else {
                if (thisLoc == Loc.NW) {
                    return mu.northEast;
                } else if (thisLoc == Loc.SW) {
                    return mu.southEast;
                }
            }
            return null;
        }


        private Loc location() {
            if (this.equals(parent.northEast)) {
                return Loc.NE;
            } else if (this.equals(parent.northWest)) {
                return Loc.NW;
            } else if (this.equals(parent.southEast)) {
                return Loc.SE;
            } else if (this.equals(parent.southWest)) {
                return Loc.SW;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = aabb.hashCode();
            hash = hash * 13 + ((northWest!=null)?northWest.hashCode():1);
            hash = hash * 17 + ((northEast!=null)?northEast.hashCode():1);
            hash = hash * 19 + ((southWest!=null)?southWest.hashCode():1);
            hash = hash * 23 + ((southEast!=null)?southEast.hashCode():1);
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof QuadNode))
                return false;

            QuadNode<G> qNode = (QuadNode<G>) obj;
            if (this.compareTo(qNode) == 0)
                return true;

            return false;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public int compareTo(QuadNode o) {
            return this.aabb.compareTo(o.aabb);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(aabb.toString());
            return builder.toString();
        }
    }

    public static class XYPoint implements Comparable<Object> {

        protected double x = Float.MIN_VALUE;
        protected double y = Float.MIN_VALUE;

        public XYPoint() { }

        public XYPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }
        public double getY() {
            return y;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 13 + (int)x;
            hash = hash * 19 + (int)y;
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof XYPoint))
                return false;

            XYPoint xyzPoint = (XYPoint) obj;
            return compareTo(xyzPoint) == 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Object o) {
            if ((o instanceof XYPoint)==false)
                throw new RuntimeException("Cannot compare object.");

            XYPoint p = (XYPoint) o;
            int xComp = X_COMPARATOR.compare(this, p);
            if (xComp != 0)
                return xComp;
            return Y_COMPARATOR.compare(this, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            builder.append(x).append(", ");
            builder.append(y);
            builder.append(")");
            return builder.toString();
        }
    }

    public static class AxisAlignedBoundingBox extends XYPoint {

        private double height = 0;
        private double width = 0;
        public int level = 0;

        private double minX = 0;
        private double minY = 0;
        private double maxX = 0;
        private double maxY = 0;

        public AxisAlignedBoundingBox() { }

        public AxisAlignedBoundingBox(XYPoint upperLeft, double width, double height, int level) {
            super(upperLeft.x, upperLeft.y);
            this.width = width;
            this.height = height;

            minX = upperLeft.x;
            minY = upperLeft.y;
            maxX = upperLeft.x+width;
            maxY = upperLeft.y+height;
            this.level = level;
        }

        public void set(XYPoint upperLeft, double width, double height) {
            set(upperLeft.x, upperLeft.y);
            this.width = width;
            this.height = height;

            minX = upperLeft.x;
            minY = upperLeft.y;
            maxX = upperLeft.x+width;
            maxY = upperLeft.y+height;
        }

        public double getHeight() {
            return height;
        }
        public double getWidth() {
            return width;
        }

        public boolean containsPoint(XYPoint p) {
            if (p.x>=maxX) return false;
            if (p.x<minX) return false;
            if (p.y>=maxY) return false;
            if (p.y<minY) return false;
            return true;
        }

        /**
         * Is the inputted AxisAlignedBoundingBox completely inside this AxisAlignedBoundingBox.
         *
         * @param b AxisAlignedBoundingBox to test.
         * @return True if the AxisAlignedBoundingBox is completely inside this AxisAlignedBoundingBox.
         */
        public boolean insideThis(AxisAlignedBoundingBox b) {
            if (b.minX >= minX && b.maxX <= maxX && b.minY >= minY && b.maxY <= maxY) {
                // INSIDE
                return true;
            }
            return false;
        }

        /**
         * Is the inputted AxisAlignedBoundingBox intersecting this AxisAlignedBoundingBox.
         *
         * @param b AxisAlignedBoundingBox to test.
         * @return True if the AxisAlignedBoundingBox is intersecting this AxisAlignedBoundingBox.
         */
        public boolean intersectsBox(AxisAlignedBoundingBox b) {
            if (insideThis(b) || b.insideThis(this)) {
                // INSIDE
                return true;
            }

            // OUTSIDE
            if (maxX < b.minX || minX > b.maxX) return false;
            if (maxY < b.minY || minY > b.maxY) return false;

            // INTERSECTS
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = hash * 13 + (int)height;
            hash = hash * 19 + (int)width;
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof AxisAlignedBoundingBox))
                return false;

            AxisAlignedBoundingBox aabb = (AxisAlignedBoundingBox) obj;
            return compareTo(aabb) == 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Object o) {
            if ((o instanceof AxisAlignedBoundingBox)==false)
                throw new RuntimeException("Cannot compare object.");

            AxisAlignedBoundingBox a = (AxisAlignedBoundingBox) o;
            int p = super.compareTo(a);
            if (p!=0) return p;

            if (height>a.height) return 1;
            if (height<a.height) return -1;

            if (width>a.width) return 1;
            if (width<a.width) return -1;

            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            builder.append(super.toString()).append(", ");
            builder.append("height").append("=").append(height).append(", ");
            builder.append("width").append("=").append(width);
            builder.append(")");
            return builder.toString();
        }

        public String toJsonString() {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("\"x1\"").append(":").append(minX).append(", ");
            builder.append("\"y1\"").append(":").append(minY).append(", ");
            builder.append("\"x2\"").append(":").append(maxX).append(", ");
            builder.append("\"y2\"").append(":").append(maxY).append(", ");
            builder.append("\"depth\"").append(":").append(level);
            builder.append("}");

            return builder.toString();
        }
    }

    private static final Comparator<XYPoint> X_COMPARATOR = new Comparator<XYPoint>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(XYPoint o1, XYPoint o2) {
            if (o1.x < o2.x)
                return -1;
            if (o1.x > o2.x)
                return 1;
            return 0;
        }
    };

    private static final Comparator<XYPoint> Y_COMPARATOR = new Comparator<XYPoint>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(XYPoint o1, XYPoint o2) {
            if (o1.y < o2.y)
                return -1;
            if (o1.y > o2.y)
                return 1;
            return 0;
        }
    };

    protected static class TreePrinter {

        public static <T extends XYPoint> String getString(QuadTree<T> tree) {
            if (tree.getRoot() == null) return "Tree has no nodes.";
            return getString(tree.getRoot(), "", true);
        }

        private static <T extends XYPoint> String getString(QuadNode<T> node, String prefix, boolean isTail) {
            StringBuilder builder = new StringBuilder();

            builder.append(prefix + (isTail ? "└── " : "├── ") + " node={" + node.toString() + "}\n");
            List<QuadNode<T>> children = null;
            if (node.northWest != null || node.northEast != null || node.southWest != null || node.southEast != null) {
                children = new ArrayList<QuadNode<T>>(4);
                if (node.northWest != null) children.add(node.northWest);
                if (node.northEast != null) children.add(node.northEast);
                if (node.southWest != null) children.add(node.southWest);
                if (node.southEast != null) children.add(node.southEast);
            }
            if (children != null) {
                for (int i = 0; i < children.size() - 1; i++) {
                    builder.append(getString(children.get(i), prefix + (isTail ? "    " : "│   "), false));
                }
                if (children.size() >= 1) {
                    builder.append(getString(children.get(children.size() - 1), prefix + (isTail ? "    " : "│   "), true));
                }
            }

            return builder.toString();
        }


        public static <T extends XYPoint> String getJson(QuadTree<T> tree) {
            if (tree.getRoot() == null) return "Tree has no nodes.";
            return "[" + getJson(tree.getRoot(), true) + "]";
        }

        private static <T extends XYPoint> String nodeJson(QuadNode<T> node) {
            return "{\"x1\": " + node.aabb.minX +
                    ", \"y1\": " + node.aabb.minY +
                    ", \"x2\": " + node.aabb.maxX +
                    ", \"y2\": " + node.aabb.maxY +
                    ", \"depth\":" + node.aabb.level +
                    "}";
        }

        private static <T extends XYPoint> String getJson(QuadNode<T> node, boolean isFirst) {
            StringBuilder builder = new StringBuilder();

            builder.append((isFirst ? "" : ",\n") + nodeJson(node));
            List<QuadNode<T>> children = null;


            if (node.northWest != null || node.northEast != null || node.southWest != null || node.southEast != null) {
                children = new ArrayList<>(4);
                if (node.northWest != null) children.add(node.northWest);
                if (node.northEast != null) children.add(node.northEast);
                if (node.southWest != null) children.add(node.southWest);
                if (node.southEast != null) children.add(node.southEast);
            }
            if (children != null) {
                for (int i = 0; i < children.size() - 1; i++) {
                    builder.append(getJson(children.get(i), false));
                }
                if (children.size() >= 1) {
                    builder.append(getJson(children.get(children.size() - 1), false));
                }
            }

            return builder.toString();
        }
    }
}