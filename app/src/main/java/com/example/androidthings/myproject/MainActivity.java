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
import android.util.Log;

import com.google.android.things.contrib.driver.mma7660fc.Mma7660FcAccelerometerDriver;

import java.io.IOException;
import java.util.ArrayList;

/**
 * MainActivity is a sample activity that use an Accelerometer driver to
 * read data from a Grove accelerator and log them.
 */
public class MainActivity extends Activity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int POLLS_PER_SECOND = 125;

    private Mma7660FcAccelerometerDriver mAccelerometerDriver;
    private SensorManager mSensorManager;

    Chunk chunk;

    ArrayList<Chunk> data;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Accelerometer demo created");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(new SensorManager.DynamicSensorCallback() {
            @Override
            public void onDynamicSensorConnected(Sensor sensor) {
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    Log.i(TAG, "Accelerometer sensor connected");
                    mSensorManager.registerListener(MainActivity.this, sensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                    data = new ArrayList<>();
                    chunk = new Chunk(POLLS_PER_SECOND);
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
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        chunk.add(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]);

        if (chunk.isFull()) {
            data.add(chunk);
            chunk = new Chunk(POLLS_PER_SECOND);
        }

        Log.i(TAG, "Accelerometer event: " +
                event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accelerometer accuracy changed: " + accuracy);
    }
}
