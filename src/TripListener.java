import core.Trip;

/**
 * Created by ruudandriessen on 21/02/2017.
 */
public interface TripListener {
    void newTrip(Trip t);
    void done();
}
