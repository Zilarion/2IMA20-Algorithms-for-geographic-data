public class Main
{
    public static void main(String [] args) {
//        CSVReader.listen(new GetisOrdComputer());
        CSVReader.listen(new QuadTreeComputer());

//        int[] testSet = {100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000, 4000000};
        int[] testSet = {1000000};
        for (int i = 0; i < testSet.length; i++) {
            long start = System.nanoTime();
            CSVReader.parse("./data/yellow_tripdata_2016-01.csv", testSet[i]);
            long end = System.nanoTime();
            System.out.println(testSet[i] + "\t" + (end-start));
        }
    }
}