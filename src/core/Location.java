package core;

/**
 * Created by ruudandriessen on 21/02/2017.
 */
public class Location {
    private double longitude;
    private double latitude;

    Location(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double longitude() {
        return longitude;
    }

    public double latitude() {
        return latitude;
    }

    @Override
    public String toString() {
        return "{ \"long\":" + longitude + ", \"lat\":" + latitude + "}";
    }
}
