package core;

/**
 * Created by ruudandriessen on 24/02/2017.
 */
public class SpaceTimeCube {
    float[][][] cube;
    private int xSize, ySize, zSize;

    /**
     * Creates a 3 dimensional space time cube
     * @param xSize The x size
     * @param ySize The y size
     * @param zSize The z size (time)
     */
    public SpaceTimeCube(int xSize, int ySize, int zSize) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        cube = new float[xSize][ySize][zSize];
    }

    public float get(int x, int y, int z) {
        return cube[x][y][z];
    }

    public float get(int[] location) {
        return get(location[0], location[1], location[2]);
    }

    public void increment(int x, int y, int z, float value) {
        cube[x][y][z] += value;
    }

    public void increment(int[] location, float value) {
        increment(location[0], location[1], location[2], value);
    }

    public void max() {
        float max = 0;
        int xm = -1, ym = -1, zm = -1;
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    float value = get(x,y,z);
                    if (value > max) {
                        max = value;
                        xm = x;
                        ym = y;
                        zm = z;
                    }
                }
            }
        }
        System.out.println(xm + ", " + ym + ", " + zm + " = " + max);
    }
}
