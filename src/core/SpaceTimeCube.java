package core;

/**
 * Created by ruudandriessen on 24/02/2017.
 */
public class SpaceTimeCube<T> {
    T[] cube;
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
        cube = (T[]) new Object[xSize * ySize * zSize];
    }

    public void set(int x, int y, int z, T value) {
        cube[x + xSize * y + (xSize * ySize) * z] = value;
    }

    public void set(int[] location, T value) {
        set(location[0], location[1], location[2], value);
    }

    public T get(int x, int y, int z) {
        return cube[x + xSize * y + (xSize * ySize) * z];
    }

    public T get(int[] location) {
        return get(location[0], location[1], location[2]);
    }
}
