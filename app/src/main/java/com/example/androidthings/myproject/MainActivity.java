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
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;

/**
 * MainActivity is a sample activity that use an Accelerometer driver to
 * read data from a Grove accelerator and log them.
 */
public class MainActivity extends Activity implements SensorEventListener, ChunkTracker {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_EXTRAS = 50;
    public static final int POLLS_PER_SECOND = 125;
    private static final double TILT_THRESHOLD = 5;
    private static final double SPEED_THRESHOLD = 0.3;

    private Mma7660FcAccelerometerDriver mAccelerometerDriver;
    private Gpio redGpio1;
    private Gpio redGpio2;
    private Gpio greenGpio2;
    private Gpio greenGpio1;
//    private Pwm red;
//    private Pwm green;
    private SensorManager mSensorManager;
    private ChunkAllocator chunkAllocator;
    private ChunkUploader chunkUploader;
    private long numChunks = 0;

    private long points = 0;
    private long total = 0;

    private float baseX = 0;
    private float baseY = 0;
    private float baseZ = 0;

    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;
    private long lastTime = 0;

    boolean logging = false;
    boolean good = true;

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

        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            Log.i(TAG, "Configuring GPIO pins");
            redGpio1 = pioService.openGpio("GPIO_34");
            redGpio1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            redGpio1.setActiveType(Gpio.ACTIVE_HIGH);
//            redGpio2 = pioService.openGpio("GPIO_33");
//            redGpio2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//            redGpio2.setActiveType(Gpio.ACTIVE_HIGH);

            greenGpio1 = pioService.openGpio("GPIO_32");
            greenGpio1.setActiveType(Gpio.ACTIVE_HIGH);
            greenGpio1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//            greenGpio2 = pioService.openGpio("GPIO_37");
//            greenGpio2.setActiveType(Gpio.ACTIVE_HIGH);
//            greenGpio2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

//            red = pioService.openPwm("PWM1");
//            green = pioService.openPwm("PWM2");
//            red.setPwmFrequencyHz(120);
//            green.setPwmFrequencyHz(120);
//            red.setEnabled(true);
//            green.setEnabled(true);
        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
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

        ((Button)findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (logging) {
                    double score = points / (total + 0.0);
                    score *= 100;
                    logging = false;
                    baseX = 0;
                    baseY = 0;
                    baseZ = 0;
                    lastX = 0;
                    lastY = 0;
                    lastZ = 0;
                    lastTime = 0;
                    points = 0;
                    total = 0;
                    good = true;

                    System.out.println("YOUR SCORE WAS " + score);

                    setLights(false, false);
                    Log.i(TAG, "logging off");
                    Log.i(TAG, "Data size: " + data.size());
                    Log.i(TAG, "Num Chunks: " + numChunks);
                } else {
                    logging = true;
                    setLights(false, true);

                    Log.i(TAG, "logging on");
                    Log.i(TAG, "Num chunks: " + numChunks);
                }
            }
        });
    }

    private void setLights(boolean value1, boolean value2) {
        try {
            redGpio1.setValue(value1);
//            redGpio2.setValue(value1);
            greenGpio1.setValue(value2);
//            greenGpio2.setValue(value2);
//            red.setPwmDutyCycle((value1) ? 100 : 0);
//            green.setPwmDutyCycle((value2) ? 100 : 0);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
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

        // This should never get triggered IRL.  But if it does, better to hiccup than crash
        if (data.get((int) numChunks) == null) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if (logging) {
            if (baseX == 0 && baseY == 0 && baseZ == 0) {
                baseX = event.values[0];
                baseY = event.values[1];
                baseZ = event.values[2];

                lastX = baseX;
                lastY = baseY;
                lastZ = baseZ;
                lastTime = System.currentTimeMillis();
            } else {
                double velocity = Math.sqrt(Math.pow(lastX - event.values[0], 2) +
                        Math.pow(lastY - event.values[1], 2) +
                        Math.pow(lastZ - event.values[2], 2)) / (System.currentTimeMillis() - lastTime);

                double orientation = Math.abs((baseX - event.values[0]) +
                        (baseY - event.values[1]) +
                        (baseZ - event.values[2]));

                lastX = event.values[0];
                lastY = event.values[1];
                lastZ = event.values[2];
                lastTime = System.currentTimeMillis();

                System.out.println("orientation: " + orientation + ", velocity: " + velocity);

                if (orientation > TILT_THRESHOLD || velocity > SPEED_THRESHOLD) {
                    if (good) {
                        setLights(true, false);
                        good = false;
                    }
                } else {
                    if (!good) {
                        setLights(false, true);
                        good = true;
                        points++;
                    }
                }
                total++;
            }
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
}
