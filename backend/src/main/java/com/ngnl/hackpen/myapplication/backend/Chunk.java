package com.ngnl.hackpen.myapplication.backend;

/**
 * Created by Arthur on 9/9/17.
 */

public class Chunk {

    private long[] timestamps;
    private float[] xs;
    private float[] ys;
    private float[] zs;
    public int size;

    public Chunk(long[] timestamps, float[] xs, float[] ys, float[] zs) {
        this.timestamps = timestamps;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.size = zs.length;
        if (!validate()) throw new IllegalArgumentException();
    }

    public Point getPoint(int index) {
        return new Point(
                timestamps[index],
                xs[index],
                ys[index],
                zs[index]);
    }

    private boolean validate() {
       if (timestamps.length != xs.length) return false;
        if (timestamps.length != ys.length) return false;
        if (timestamps.length != zs.length) return false;
        return true;
    }

    class Point {
        public long timestamp;
        public float x;
        public float y;
        public float z;

        public Point(long time, float x, float y, float z) {
            this.timestamp = time;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

}
