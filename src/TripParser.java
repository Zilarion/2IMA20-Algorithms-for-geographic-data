import core.Location;
import core.SpaceTimeCube;
import core.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ruudandriessen on 21/02/2017.
 */
public class TripParser implements TripListener {
    private int count = 0;
    private SpaceTimeCube stc;
    private double latMin = 40.5, latMax = 40.9, lonMin = -73.7, lonMax = -74.25;
    private long timeDmin, timeDmax;
    private String timeMin = "01/01/2016", timeMax = "31/01/2016";
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private double lonDelta, latDelta, timeDelta;
    private int xSize = 50, ySize = 50, zSize = 50;

    TripParser() {
        // Parse location
        latDelta = (latMax - latMin)/xSize;
        lonDelta = (lonMax - lonMin)/ySize;

        // Parse time
        try {
            timeDmin = sdf.parse(timeMin).getTime();
            timeDmax = sdf.parse(timeMax).getTime();
            timeDelta = (timeDmax - timeDmin)/zSize;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        stc = new SpaceTimeCube(xSize, ySize, zSize);
    }

    public void run() {
        CSVReader.parse("./data/yellow_tripdata_2016-01.csv", this);

    }

    private int[] cubeMap(Location location, Date time) throws IllegalArgumentException {
        double lat = location.latitude();
        double lon = location.longitude();
        long lTime = time.getTime();

        int[] loc = new int[3];

        loc[0] = (int) Math.floor((lat-latMin) / latDelta);
        loc[1] = (int) Math.floor((lon-lonMin) / lonDelta);
        loc[2] = (int) Math.floor((lTime-timeDmin) / timeDelta);

        if (loc[0] < 0 || loc[0] >= xSize || loc[1] < 0 || loc[1] >= ySize || loc[2] < 0 || loc[2] >= zSize ) {
            // Invalid trip
            throw new IllegalArgumentException("Invalid trip");
        }
        return loc;
    }

    @Override
    public void done() {
        stc.max();
    }

    @Override
    public void newTrip(Trip t) {
        count++;

        if (count % 100000 == 0) {
            System.out.println(count);
        }
        int[] pickupTile, dropoffTile;
        try {
            // Try to find the location
            Location pickup = t.pickup_location;
            Date pickup_time = t.pickup_datetime;
            pickupTile = cubeMap(pickup, pickup_time);

            Date dropoff_time = t.dropoff_datetime;
            Location dropoff = t.dropoff_location;
            dropoffTile = cubeMap(dropoff, dropoff_time);
        } catch (IllegalArgumentException e ) {
            return;
        }

        stc.increment(pickupTile);
        stc.increment(pickupTile);
    }
}
