import core.Trip;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {
    static ArrayList<TripListener> listeners = new ArrayList<>();

    public static void parse(String csvFile, TripListener listener) {
        parse(csvFile, listener, -1);
    }
    public static void parse(String csvFile, TripListener listener, int tripCount) {
        listen(listener);
        parse(csvFile, tripCount);
    }

    public static void parse(String csvFile) {
        parse(csvFile, -1);
    }

    public static void parse(String csvFile, int tripCount) {
        String line = "";
        String cvsSplitBy = ",";
        boolean firstLine = true;
        int trips = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                // Skip header
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                // Split this line of data
                String[] data = line.split(cvsSplitBy);
                broadcast(new Trip(data));
                trips++;
                if (tripCount != -1 && tripCount <= trips) {
                    done();
                    return;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        done();
    }

    public static void listen(TripListener s) {
        listeners.add(s);
    }

    private static void done() {
        for (TripListener tl : listeners) {
            tl.done();
        }
    }

    private static void broadcast(Trip t) {
        for (TripListener tl : listeners) {
            tl.newTrip(t);
        }
    }

}