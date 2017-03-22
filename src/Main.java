public class Main
{
    public static void main(String [] args) {
//        CSVReader.listen(new GetisOrdComputer());
        CSVReader.listen(new QuadTreeComputer());
        CSVReader.parse("./data/yellow_tripdata_2016-01.csv", 1000000);
    }
}