/**
 * Created by ruudandriessen on 21/02/2017.
 */
public class TripParser implements TripListener {
    int count = 0;
    public void run() {
        CSVReader.parse("./data/yellow_tripdata_2016-01.csv", this);
    }

    @Override
    public void newTrip(Trip t) {
        count++;

        if (count % 100000 == 0) {
            System.out.println(count);
        }
//        System.out.println(t);
    }
}
