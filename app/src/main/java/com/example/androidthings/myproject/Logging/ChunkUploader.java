package com.example.androidthings.myproject.Logging;

import android.util.Log;

import com.example.androidthings.myproject.Chunk;
import com.example.androidthings.myproject.MainActivity;

import java.util.ArrayList;

/**
 * Created by Arthur on 9/9/17.
 * Allocates new chunks as quickly as possible while reasonable
 */

public class ChunkUploader implements Runnable {

    private ArrayList<Chunk> data;
    private ChunkTracker chunkTracker;
    private int extras; // number of extra chunks to allocate.  Do nothing while this is exceeded
    private boolean stopped = false;
    private final int warningThreshold = 10; // when to start warning us that this thread is too slow
    private int index;

    public ChunkUploader(ArrayList<Chunk> data, ChunkTracker tracker, int extras) {
        this.data = data;
        this.chunkTracker = tracker;
        this.extras = extras;
        index = 0;
    }

    @Override
    public void run() {
        // Always run in background priority.  If main thread ever catches up to this, we can simply
        // increase chunk size
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        while (!stopped) {
            if (shouldAllocate()) {
                synchronized (data) {
                    Log.d("logger", "Chunk uploaded.  Chunks: "+chunkTracker.getNumChunks());
                    data.add(new Chunk(MainActivity.POLLS_PER_SECOND));
                }
            } else {
                Log.d("logger", "waiting");
                try {
                    Thread.sleep(10); // give the main thread time to catch up
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void stop() {
        stopped = true;
    }

    private boolean shouldAllocate() {
        if (data.size() <= chunkTracker.getNumChunks() + warningThreshold)
            Log.w("logger", "WARNING: main thread catching up.  numChunks = "+ chunkTracker.getNumChunks() + " ; size = " + data.size());

        // Check that difference between allocated space and utilized space is less than extras
        return data.size() - chunkTracker.getNumChunks() < extras;
    }
}
