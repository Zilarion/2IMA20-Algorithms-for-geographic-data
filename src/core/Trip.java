package core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ruudandriessen on 21/02/2017.
 */
public class Trip {
    public int VendorID;
    public Date pickup_datetime;
    public Date dropoff_datetime;
    public int passenger_count;
    public double trip_distance;
    public Location pickup_location;
    public int RatecodeID;
    public boolean store_and_fwd_flag;
    public Location dropoff_location;
    String payment_type;
    public double fare_amount;
    public double extra;
    public double mta_tax;
    public double tip_amount;
    public double tolls_amount;
    public double improvement_surcharge;
    public double total_amount;

    public Trip(String[] data) {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        try {
            VendorID = Integer.valueOf(data[0]);
            pickup_datetime = sdf.parse(data[1]);
            dropoff_datetime = sdf.parse(data[2]);
            passenger_count = Integer.valueOf(data[3]);
            trip_distance = Double.valueOf(data[4]);
            pickup_location = new Location(Double.valueOf(data[5]), Double.valueOf(data[6]));
            RatecodeID = Integer.valueOf(data[7]);
            store_and_fwd_flag = data[8].equals("Y");
            dropoff_location = new Location(Double.valueOf(data[9]), Double.valueOf(data[10]));
            payment_type = data[11];
            fare_amount = Double.valueOf(data[12]);
            extra = Double.valueOf(data[13]);
            mta_tax = Double.valueOf(data[14]);
            tip_amount = Double.valueOf(data[15]);
            tolls_amount = Double.valueOf(data[16]);
            improvement_surcharge = Double.valueOf(data[17]);
            total_amount = Double.valueOf(data[18]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
//        System.out.println(pickup_location + " => " + dropoff_location);
    }
}
