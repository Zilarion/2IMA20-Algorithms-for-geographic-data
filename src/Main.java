public class Main
{
    public static void main(String [] args) {
        CSVReader.listen(new GetisOrdComputer());
        CSVReader.parse("./data/yellow_tripdata_2016-01.csv");
    }
}