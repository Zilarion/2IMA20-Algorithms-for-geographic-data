import core.Location;
import core.QuadTree;
import core.Trip;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ruudandriessen on 21/03/2017.
 */
public class QuadTreeComputer implements TripListener {
    private QuadTree.PointRegionQuadTree quad;
    private double latMax = 40.9, latMin = 40.5, lonMin = -74.25, lonMax = -73.7;
    private ArrayList<Location> locations;
    private int count = 0;

    QuadTreeComputer() {
        quad = new QuadTree.PointRegionQuadTree(latMin, lonMin, latMax-latMin, lonMax-lonMin, 10, 10000);
        locations = new ArrayList<>();
    }

    @Override
    public void newTrip(Trip t) {
        count++;
        if (count % 100000 == 0) {
            System.out.println("Processed: " + count + " trips");
        }
        if (t.pickup_location.latitude() == 0 || t.pickup_location.longitude() == 0
                || t.dropoff_location.latitude() == 0 || t.dropoff_location.longitude() == 0) {
            return;
        }
        quad.insert(t.dropoff_location.latitude(), t.dropoff_location.longitude());
        quad.insert(t.pickup_location.latitude(), t.pickup_location.longitude());

        locations.add(t.dropoff_location);
        locations.add(t.pickup_location);
    }

    @Override
    public void done() {
        ArrayList<ArrayList<QuadTree.QuadNode>> hotspots = findHotspots(50);
        writeHotspots(hotspots);
    }

    private ArrayList<ArrayList<QuadTree.QuadNode>> findHotspots(int count) {
        int level = quad.currentHeight;
        int hotspotCount = 0;
        ArrayList<ArrayList<QuadTree.QuadNode>> hotspots = new ArrayList<>();

        while (hotspotCount < count || level < 0) {
            ArrayList<QuadTree.QuadNode> levelNodes = new ArrayList<>();
            getNodes(quad.getRoot(), levelNodes, level);
            System.out.println("On level " + level + " we have " + levelNodes.size() + " nodes");
            ArrayList<ArrayList<QuadTree.QuadNode>> levelHotspots = new ArrayList<>();

            // Find list of potential hotspots
            for (int i = 0; i < levelNodes.size(); i++) {
                // Find start node of a cluster
                QuadTree.QuadNode node = levelNodes.get(i);
                // Find the cluster
                ArrayList<QuadTree.QuadNode> cluster = new ArrayList<>();
                findCluster(node, levelNodes, cluster);

                // Add it to potential hotspot list
                levelHotspots.add(cluster);
            }

            // Sort on hotspot size
            levelHotspots.sort(new Comparator<ArrayList<QuadTree.QuadNode>>() {
                @Override
                public int compare(ArrayList<QuadTree.QuadNode> o1, ArrayList<QuadTree.QuadNode> o2) {
                    return o2.size() - o1.size();
                }
            });

            // Add all potential hotspots based on their size
            for (int i = 0; i < levelHotspots.size() && hotspotCount < count; i++) {
                hotspots.add(levelHotspots.get(i));

                System.out.println("Added hotspot at level " + level + " of size " + levelHotspots.get(i).size());
                // Increase hotspot count
                hotspotCount++;
            }

            System.out.println(hotspotCount + " / " + count + " @ level: " + level);
            level--;
        }

         return hotspots;
    }

    private void findCluster(QuadTree.QuadNode node, List<QuadTree.QuadNode> nodes, List<QuadTree.QuadNode> cluster) {
        ArrayList<QuadTree.QuadNode> neighbors = new ArrayList<>(4);
        QuadTree.QuadNode NN = node.northNeighbor();
        QuadTree.QuadNode SN = node.southNeighbor();
        QuadTree.QuadNode WN = node.westNeighbor();
        QuadTree.QuadNode EN = node.eastNeighbor();

        if (NN != null) neighbors.add(NN);
        if (SN != null) neighbors.add(SN);
        if (WN != null) neighbors.add(WN);
        if (EN != null) neighbors.add(EN);

        for (int j = 0; j < neighbors.size(); j++) {
            QuadTree.QuadNode neighbor = neighbors.get(j);
            if (nodes.remove(neighbor)) {
                cluster.add(neighbor);
                findCluster(neighbor, nodes, cluster);
            }
        }
    }

    private void getNodes(QuadTree.QuadNode node, ArrayList<QuadTree.QuadNode> result, int level) {
        List<QuadTree.QuadNode> children = new ArrayList<>();
        if (node.northWest != null || node.northEast != null || node.southWest != null || node.southEast != null) {
            children = new ArrayList<>(4);
            if (node.northWest != null) children.add(node.northWest);
            if (node.northEast != null) children.add(node.northEast);
            if (node.southWest != null) children.add(node.southWest);
            if (node.southEast != null) children.add(node.southEast);
        }
        for (int i = 0; i < children.size(); i++) {
            QuadTree.QuadNode childNode = children.get(i);
            if (childNode.aabb.level == level) {
                result.add(childNode);
            } else {
                getNodes(childNode, result, level);
            }
        }
    }

    private void writeHotspots(ArrayList<ArrayList<QuadTree.QuadNode>> hotspots) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("visualize/qtree/qtree.json"), "utf-8"))) {

            writer.write("[");
            for (int i = 0; i < hotspots.size(); i++) {
                ArrayList<QuadTree.QuadNode> hotspot = hotspots.get(i);
                for (int j = 0; j < hotspot.size(); j++) {
                    if (i != 0 || j != 0) {
                        writer.write(", \n");
                    }
                    writer.write(hotspot.get(j).aabb.toJsonString());
                }

            }
            writer.write("]");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void writeTree() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("visualize/qtree/qtree.json"), "utf-8"))) {

            writer.write(quad.toJson());

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
