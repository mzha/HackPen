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
import android.graphics.Color;
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
import android.widget.TextView;

import com.example.androidthings.myproject.Logging.ChunkAllocator;
import com.example.androidthings.myproject.Logging.ChunkTracker;
import com.example.androidthings.myproject.Logging.ChunkUploader;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.mma7660fc.Mma7660FcAccelerometerDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

/**
 * MainActivity is a sample activity that use an Accelerometer driver to
 * read data from a Grove accelerator and log them.
 */
public class MainActivity extends Activity implements SensorEventListener, ChunkTracker {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_EXTRAS = 50;
    public static final int POLLS_PER_SECOND = 60;
    private static final double TILT_THRESHOLD = 5;
    private static final double SPEED_THRESHOLD = 0.5;
    private static final int COUNT_THRESHOLD = 50;
    private static final int BUZZER_THRESHOLD = 50;
    private static final String DEFAULT_DISPLAY = "ERGO";

    private Mma7660FcAccelerometerDriver mAccelerometerDriver;
    private Gpio redGpio1;
    private Gpio greenGpio1;

    private SensorManager mSensorManager;
    private ChunkAllocator chunkAllocator;
    private ChunkUploader chunkUploader;
    private AlphanumericDisplay segment;
    private Speaker buzzer;
    private long numChunks = 0;
    private boolean isBuzzer = false;

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
    boolean cleared = true;
    boolean good = true;

    ArrayList<Chunk> data;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Accelerometer demo created");

//        data = new ArrayList<>();
//        chunkAllocator = new ChunkAllocator(data, MainActivity.this, MAX_EXTRAS);
//        new Thread(chunkAllocator).start();
//
//        chunkUploader = new ChunkUploader(data);
//        new Thread(chunkUploader).start();

        // Give chunk allocator some time to catch up
//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            buzzer = RainbowHat.openPiezo();

            Log.i(TAG, "Configuring GPIO pins");
            redGpio1 = pioService.openGpio("GPIO_34");
            redGpio1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            redGpio1.setActiveType(Gpio.ACTIVE_HIGH);

            segment = RainbowHat.openDisplay();
            segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            segment.setEnabled(true);

            setDisplay(DEFAULT_DISPLAY);

            greenGpio1 = pioService.openGpio("GPIO_32");
            greenGpio1.setActiveType(Gpio.ACTIVE_HIGH);
            greenGpio1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            Gpio blue = RainbowHat.openLedBlue();
            blue.setValue(false);
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

        ((TextView)findViewById(R.id.score)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (logging) {
                    logging = false;
                    good = true;
                    isBuzzer = false;
                    cleared = false;

                    setBuzzer(false);
                    setLights(false, false);

                    Log.i(TAG, "logging off");
//                    Log.i(TAG, "Data size: " + data.size());
//                    Log.i(TAG, "Num Chunks: " + numChunks);
                } else {
                    if (cleared) {
                        logging = true;
                        cleared = false;
                        setLights(false, true);

                        Log.i(TAG, "logging on");
//                        Log.i(TAG, "Num chunks: " + numChunks);
                    } else {
                        cleared = true;
                        setDisplay(DEFAULT_DISPLAY);
                        ((TextView)findViewById(R.id.score)).setText("Tap to measure");
                        ((TextView)findViewById(R.id.score)).setBackgroundColor(Color.rgb(0, 0, 0));
                        baseX = 0;
                        baseY = 0;
                        baseZ = 0;
                        lastX = 0;
                        lastY = 0;
                        lastZ = 0;
                        lastTime = 0;
                        points = 0;
                        total = 0;
                    }
                }
            }
        });
    }

    private void setLights(final boolean value1, final boolean value2) {
        new Thread() {
            @Override
            public void run() {
                try {
                    redGpio1.setValue(value1);
                    greenGpio1.setValue(value2);
                } catch (IOException e) {
                    Log.e(TAG, "Error updating GPIO value", e);
                }
            }
        }.run();
    }

    private void setDisplay(final String value) {
        new Thread() {
            @Override
            public void run() {
                try {
                    segment.display(value);
                } catch (IOException e) {
                    Log.e(TAG, "Error updating GPIO value", e);
                }
            }
        }.run();
    }

    private void setBuzzer(final boolean value) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (value) {
                        buzzer.play(440);
                    } else {
                        buzzer.stop();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error updating GPIO value", e);
                }
            }
        }.run();
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
        if (buzzer != null) {
            try {
                buzzer.close();
            } catch (Exception e) {

            }
        }
        if (segment != null) {
            try {
                segment.close();
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // This should never get triggered IRL.  But if it does, better to hiccup than crash
//        if (data.get((int) numChunks) == null) {
//            try {
//                Thread.sleep(20);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }


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

//                System.out.println("orientation: " + orientation + ", velocity: " + velocity);

                if (orientation > TILT_THRESHOLD || velocity > SPEED_THRESHOLD) {
                    if (good) {
                        setLights(true, false);
                        good = false;
                    }
                } else {
                    if (!good) {
                        setLights(false, true);
                        good = true;
                    }
                    points++;
                }
                total++;
                double score = 100 - 200 * (total - points) / (total + 0.0);
                String result = String.format("%.2f", score);
                double green = 100;
                double red = 100;
                if (score >= 50) {
                    red = (100 - score) * 2;
                } else {
                    green = score * 2;
                }
                red = 255 * (red / 100.0);
                green = 255 * (green / 100.0);
                ((TextView)findViewById(R.id.score)).setText(result + " %");
                ((TextView)findViewById(R.id.score)).setBackgroundColor(Color.rgb((int)red, (int)green, 0));
                if (total > COUNT_THRESHOLD && score < BUZZER_THRESHOLD && !isBuzzer) {
                    isBuzzer = true;
                    setBuzzer(isBuzzer);
                } else if (total > COUNT_THRESHOLD && score > BUZZER_THRESHOLD && isBuzzer){
                    isBuzzer = false;
                    setBuzzer(isBuzzer);
                }
                setDisplay(result);
            }
//            data.get((int) numChunks)
//                    .add(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]);
//
//            if (data.get((int) numChunks).isFull()) {
//                numChunks++;
//                Log.i("fakewifi", "{\"score\": " + points +
//                        ",\"total\": " + total + "}");
////                Log.i(TAG, "New Chunk Created");
//            }

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
