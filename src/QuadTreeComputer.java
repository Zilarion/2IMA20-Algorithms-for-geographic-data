import core.QuadTree;
import core.Trip;

/**
 * Created by ruudandriessen on 21/03/2017.
 */
public class QuadTreeComputer implements TripListener {
    private QuadTree.PointRegionQuadTree quad;
    private double latMax = 40.9, latMin = 40.5, lonMin = -74.25, lonMax = -73.7;

    QuadTreeComputer() {
        quad = new QuadTree.PointRegionQuadTree(latMin, lonMin, latMax-latMin, lonMax-lonMin);
    }

    @Override
    public void newTrip(Trip t) {
        if (t.pickup_location.latitude() == 0 || t.pickup_location.longitude() == 0) {
            return;
        }
        System.out.println(t.dropoff_location + ",");
        System.out.println(t.pickup_location + ",");
    }

    @Override
    public void done() {
    }
}
