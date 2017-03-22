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

    QuadTreeComputer() {
        quad = new QuadTree.PointRegionQuadTree(latMin, lonMin, latMax-latMin, lonMax-lonMin);
        locations = new ArrayList<>();
    }

    @Override
    public void newTrip(Trip t) {
        if (t.pickup_location.latitude() == 0 || t.pickup_location.longitude() == 0
                || t.dropoff_location.latitude() == 0 || t.dropoff_location.longitude() == 0) {
            return;
        }

        locations.add(t.dropoff_location);
        locations.add(t.pickup_location);
    }

    @Override
    public void done() {
        write();
    }

    private void write() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("qtree.json"), "utf-8"))) {
            writer.write("[");
            for (int i = 0; i < locations.size(); i++) {
                Location l = locations.get(i);
                writer.write(l.toString() + (i == locations.size()-1 ? "" :","));
            }
            writer.write("]");

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
