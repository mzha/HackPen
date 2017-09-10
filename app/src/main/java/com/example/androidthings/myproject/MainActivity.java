/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.myproject;


/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.androidthings.myproject.Logging.ChunkAllocator;
import com.example.androidthings.myproject.Logging.ChunkTracker;
import com.example.androidthings.myproject.Logging.ChunkUploader;
import com.google.android.things.contrib.driver.mma7660fc.Mma7660FcAccelerometerDriver;

import java.io.IOException;
import java.util.ArrayList;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;

/**
 * MainActivity is a sample activity that use an Accelerometer driver to
 * read data from a Grove accelerator and log them.
 */
public class MainActivity extends Activity implements SensorEventListener, ChunkTracker {
    private static final long PLAYBACK_NOTE_DELAY = 80L;

    private Speaker mSpeaker;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_EXTRAS = 50;
    public static final int POLLS_PER_SECOND = 125;

    private Mma7660FcAccelerometerDriver mAccelerometerDriver;
    private SensorManager mSensorManager;
    private ChunkAllocator chunkAllocator;
    private ChunkUploader chunkUploader;
    private long numChunks = 0;

    boolean logging = false;
    Chunk chunk;

    ArrayList<Chunk> data;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Accelerometer demo created");

        data = new ArrayList<>();
        chunkAllocator = new ChunkAllocator(data, MainActivity.this, MAX_EXTRAS);
        new Thread(chunkAllocator).start();

        chunkUploader = new ChunkUploader(data);
        new Thread(chunkUploader).start();

        // Give chunk allocator some time to catch up
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
            @Override
            public void onDynamicSensorConnected(Sensor sensor) {
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    Log.i(TAG, "Accelerometer sensor connected");
                    mSensorManager.registerListener(MainActivity.this, sensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                }
            }
        });
        try {
            mAccelerometerDriver = new Mma7660FcAccelerometerDriver("I2C1");
            mAccelerometerDriver.register();
            Log.i(TAG, "Accelerometer driver registered");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing accelerometer driver: ", e);
        }

        try {
            mSpeaker = new Speaker("PWM2");
            mSpeaker.stop(); // in case the PWM pin was enabled already
        } catch (IOException e) {
            Log.e(TAG, "Error initializing speaker");
            return; // don't initilize the handler
        }

        mHandlerThread = new HandlerThread("pwm-playback");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(mPlaybackRunnable);

        ((Button)findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (logging) {
                    logging = false;
                    //STOP LOGGING
                    mPlaybackRunnable.run();
                    Log.i(TAG, "logging off");
                    Log.i(TAG, "Data size: " + data.size());
                    Log.i(TAG, "Num Chunks: " + numChunks);
                } else {
                    logging = true;
                    //LOG SHIT HERE
                    Log.i(TAG, "logging on");
                    Log.i(TAG, "Num chunks: " + numChunks);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAccelerometerDriver != null) {
            mSensorManager.unregisterListener(this);
            mAccelerometerDriver.unregister();
            try {
                mAccelerometerDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing accelerometer driver: ", e);
            } finally {
                mAccelerometerDriver = null;
            }
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(mPlaybackRunnable);
            mHandlerThread.quitSafely();
        }
        if (mSpeaker != null) {
            try {
                mSpeaker.stop();
                mSpeaker.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing speaker", e);
            } finally {
                mSpeaker = null;
            }
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // This should never get triggered IRL.  But if it does, better to hiccup than crash
        if (data.get((int) numChunks) == null) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if (logging) {
            data.get((int) numChunks)
                    .add(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]);

            if (data.get((int) numChunks).isFull()) {
                numChunks++;
//                Log.i(TAG, "New Chunk Created");
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accelerometer accuracy changed: " + accuracy);
    }

    @Override
    public long getNumChunks() {
        return numChunks;
    }

    private Runnable mPlaybackRunnable = new Runnable() {

        private int index = 0;

        @Override
        public void run() {
            if (mSpeaker == null) {
                return;
            }

            try {
                if (index == MusicNotes.DRAMATIC_THEME.length) {
                    // reached the end
                    mSpeaker.stop();
                } else {
                    double note = MusicNotes.DRAMATIC_THEME[index++];
                    if (note > 0) {
                        mSpeaker.play(note);
                    } else {
                        mSpeaker.stop();
                    }
                    mHandler.postDelayed(this, PLAYBACK_NOTE_DELAY);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error playing speaker", e);
            }
        }
    };
}
