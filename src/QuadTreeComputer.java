import core.Location;
import core.QuadTree;
import core.Trip;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by ruudandriessen on 21/03/2017.
 */
public class QuadTreeComputer implements TripListener {
    private QuadTree.PointRegionQuadTree quad;
    private double latMax = 40.9, latMin = 40.5, lonMin = -74.25, lonMax = -73.7;
    private ArrayList<Location> locations;
    private int count = 0;

    QuadTreeComputer() {
        quad = new QuadTree.PointRegionQuadTree(latMin, lonMin, latMax-latMin, lonMax-lonMin, 400, 10000);
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
        write();
    }

    private void write() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("visualize/qtree/qtree.json"), "utf-8"))) {

            writer.write(quad.toJson());

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
