import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {
    static ArrayList<TripListener> listeners = new ArrayList<>();

    public static void parse(String csvFile, TripListener listener) {
        listen(listener);
        parse(csvFile);
    }

    public static void parse(String csvFile) {
        String line = "";
        String cvsSplitBy = ",";
        boolean firstLine = true;

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
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void listen(TripListener s) {
        listeners.add(s);
    }

    private static void broadcast(Trip t) {
        for (TripListener tl : listeners) {
            tl.newTrip(t);
        }
    }

}