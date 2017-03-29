import core.Location;
import core.SpaceTimeCube;
import core.Trip;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ruudandriessen on 21/02/2017.
 */
public class GetisOrdComputer implements TripListener {
    private int count = 0;


    private SpaceTimeCube<GetisOrdData> stc;
    private double latMin = 40.9, latMax = 40.5, lonMin = -74.25, lonMax = -73.7;
    private long timeDmin, timeDmax;

    // Date format
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private double lonDelta, latDelta, timeDelta;
    private int xSize = 225, ySize = 225, zSize = 225;

    GetisOrdComputer() {
        // Define bounds of the data
        String timeMin = "01/01/2016", timeMax = "31/01/2016";

        // Parse location delta
        latDelta = (latMax - latMin)/xSize;
        lonDelta = (lonMax - lonMin)/ySize;

        // Parse time delta
        try {
            timeDmin = sdf.parse(timeMin).getTime();
            timeDmax = sdf.parse(timeMax).getTime();
            timeDelta = (timeDmax - timeDmin)/zSize;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Create empty STC
        stc = new SpaceTimeCube<>(xSize, ySize, zSize);
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    stc.set(x, y, z, new GetisOrdData());
                }
            }
        }
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

    private void writeJson() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("results.json"), "utf-8"))) {
            writer.write("{");
            for (int z = 0; z < zSize; z++) {
                long time = (long) (timeDelta * z + timeDmin);
                writer.write("\"" + time + "\": [");
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        GetisOrdData value = stc.get(x,y,z);
                        writer.write(value.g + (xSize-1 == x && ySize-1 == y ? "" : ","));
                    }
                }
                writer.write("]" + (zSize - 1 == z ? "" : ","));
            }
            writer.write("}");

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Override
    public void done() {
        // Computing getis ord
        System.out.println("Computing Getis-Ord statistic");
        double max = -5000000, min = 1000000000;

        double sumXj = 0;
        double sumXj2 = 0;
        int sumWij = 27;
        int sumWij2 = 27;
        int n = (xSize * ySize * zSize);
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    GetisOrdData god = stc.get(x, y, z);
                    sumXj += god.x;
                    sumXj2 += (god.x * god.x);
                }
            }
        }
        double xbar = sumXj/n;
        double S = Math.sqrt(sumXj2/n - xbar * xbar);

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    double sumWijXj = 0;

                    // calculate sumwij2
                    sumWijXj = 0;
                    for (int xp = Math.max(0, x-1); xp < Math.min(xSize, x+1); xp++) {
                        for (int yp = Math.max(0, y-1); yp < Math.min(ySize, y+1); yp++) {
                            for (int zp = Math.max(0, z-1); zp < Math.min(zSize, z+1); zp++) {
                                sumWijXj += stc.get(xp, yp, zp).x;
                            }
                        }
                    }

                    double above = sumWijXj - xbar * sumWij;
                    double below = S * Math.sqrt( (n * sumWij2 - sumWij*sumWij ) / (n-1) );

                    stc.get(x, y, z).g = above / below;
                    if (above/below > max) {
                        max = above/below;
                    } else if (above/below < min) {
                        min = above/below;
                    }
                }
            }
        }
        System.out.println("max: " + max + " , min: " + min);
        writeJson();
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
            // Trip has invalid data
            return;
        }

        // Get the current data
        GetisOrdData pickupData = stc.get(pickupTile);
        GetisOrdData dropOffData = stc.get(dropoffTile);

        // Update according to the new trip
        pickupData.x++;
        dropOffData.x++;
    }
}
