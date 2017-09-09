package com.example.androidthings.myproject;

/**
 * Created by Michael on 9/9/17.
 */

public class Chunk {
    public long[] timestamps;
    public float[] xs;
    public float[] ys;
    public float[] zs;
    int index;

    public Chunk(int size) {
        timestamps = new long[size];
        xs = new float[size];
        ys = new float[size];
        zs = new float[size];
        index = 0;
    }

    public void add(long t, float x, float y, float z) {
        timestamps[index] = t;
        xs[index] = x;
        ys[index] = y;
        zs[index] = z;
        index ++;
    }

    public boolean isFull() {
        return index >= timestamps.length;
    }
}
