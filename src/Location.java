/**
 * Created by ruudandriessen on 21/02/2017.
 */
public class Location {
    double longitude;
    double latitude;

    Location(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "[" + longitude + "," + latitude + "]";
    }
}
