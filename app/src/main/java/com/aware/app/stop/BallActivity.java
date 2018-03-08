package com.aware.app.stop;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Gyroscope;
import com.aware.LinearAccelerometer;
import com.aware.Rotation;
import com.aware.app.stop.database.Provider;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BallActivity extends AppCompatActivity implements SensorEventListener2 {

    private float ballXpos, ballXaccel, ballXvel = 0.0f;
    private float ballYpos, ballYaccel, ballYvel = 0.0f;
    private float ballXmax, ballYmax;
    private float bigCircleXpos, bigCircleYpos;
    private float smallCircleXpos, smallCircleYpos;
    private Bitmap ball;
    private Bitmap circleBig;
    private Bitmap circleSmall;
    private SensorManager sensorManager;

    private FrameLayout container;
    private TextView timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ball);

        // adding custom BallView to the activity
        BallView ballView = new BallView(this);
        container = findViewById(R.id.container);
        container.addView(ballView);

        timer = findViewById(R.id.timer);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // starting sensors
        Aware.startAccelerometer(getApplicationContext());
        Aware.startLinearAccelerometer(getApplicationContext());
        Aware.startGyroscope(getApplicationContext());
        Aware.startRotation(getApplicationContext());

        // timer initialization
        new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) {
                // updating UI according to timeframes and handling sensors
                if ((millisUntilFinished >= 10999) && (millisUntilFinished <= 13999)) {
                    String counter = String.valueOf((millisUntilFinished - 10000)/1000) + "...";
                    timer.setText(counter);
                }

                if ((millisUntilFinished >= 9999) && (millisUntilFinished < 10999)) {
                    timer.setText("Start!");
                    startSensors();
                }

                if ((millisUntilFinished >= 0) && (millisUntilFinished < 9999)) {
                    String counter = String.valueOf(((millisUntilFinished/1000) - 10)*(-1));
                    timer.setText(counter);
                }
            }

            public void onFinish() {
                timer.setText("Done!");
                stopSensors();
                finish();
            }
        }.start();

        // detection of the display size
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        ballXmax = (float) size.x - 100;
        ballYmax = (float) size.y - 100 - 235; // need to fix Y

        // put ball to the center
        ballXpos = ballXmax /2;
        ballYpos = ballYmax /2;

        // put circles to the center
        smallCircleXpos = ballXpos - 100;
        smallCircleYpos = ballYpos - 100;
        bigCircleXpos = ballXpos - 200;
        bigCircleYpos = ballYpos - 200;

        // intitializing sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // registering sensor manager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        // unregistering sensor manager
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // changing ball acceleration and position according to accelerometer data
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ballXaccel = event.values[0];
            ballYaccel = -event.values[1];
            updateBall();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // updating ball's X and Y positioning
    private void updateBall() {
        float frameTime = 3.000f;
        ballXvel = (ballXaccel * frameTime);
        ballYvel = (ballYaccel * frameTime);

        float xS = (ballXvel / 2) * frameTime;
        float yS = (ballYvel / 2) * frameTime;

        ballXpos -= xS;
        ballYpos -= yS;

        //off screen movements
        if (ballXpos > ballXmax) {
            ballXpos = ballXmax;
        } else if (ballXpos < 0) {
            ballXpos = 0;
        }

        if (ballYpos > ballYmax) {
            ballYpos = ballYmax;
        } else if (ballYpos < 0) {
            ballYpos = 0;
        }
    }


    private class BallView extends View {

        public BallView(Context context) {
            super(context);

            // ball bitmap initializing
            Bitmap ballSrc = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
            final int dstWidth = 100;
            final int dstHeight = 100;
            ball = Bitmap.createScaledBitmap(ballSrc, dstWidth, dstHeight, true);

            // circles bitmap initializing
            Bitmap smallSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_small);
            final int smallWidth = 300;
            final int smallHeight = 300;
            circleSmall = Bitmap.createScaledBitmap(smallSrc, smallWidth, smallHeight, true);

            Bitmap bigSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_big);
            final int bigWidth = 500;
            final int bigHeight = 500;
            circleBig = Bitmap.createScaledBitmap(bigSrc, bigWidth, bigHeight, true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // drawing (and redrawing) the ball
            canvas.drawBitmap(ball, ballXpos, ballYpos, null);
            invalidate();

            // drawing circles
            canvas.drawBitmap(circleSmall, smallCircleXpos, smallCircleYpos, null);
            canvas.drawBitmap(circleBig, bigCircleXpos, bigCircleYpos, null);
        }
    }

    // Strings for storing sampling data in JSON format
    private String accelSamples;
    private String linaccelSamples;
    private String gyroSamples;
    private String rotationSamples;

    // Start data sampling
    private void startSensors() {

        accelSamples = "[";
        linaccelSamples = "[";
        gyroSamples = "[";
        rotationSamples = "[";

        Accelerometer.setSensorObserver(new Accelerometer.AWARESensorObserver() {
            @Override
            public void onAccelerometerChanged(ContentValues data) {
                Sample accelSample = new Sample();
                accelSample.setTimestamp(data.getAsLong("timestamp"));
                accelSample.setDevice_id(data.getAsString("device_id"));
                accelSample.setAccuracy(data.getAsInteger("accuracy"));
                accelSample.setLabel(data.getAsString("label"));
                accelSample.setDouble_values_0(data.getAsDouble("double_values_0"));
                accelSample.setDouble_values_1(data.getAsDouble("double_values_1"));
                accelSample.setDouble_values_2(data.getAsDouble("double_values_2"));
                accelSamples = accelSamples + new Gson().toJson(accelSample) + ",";
            }
        });

        LinearAccelerometer.setSensorObserver(new LinearAccelerometer.AWARESensorObserver() {
            @Override
            public void onLinearAccelChanged(ContentValues data) {
                Sample linaccelSample = new Sample();
                linaccelSample.setTimestamp(data.getAsLong("timestamp"));
                linaccelSample.setDevice_id(data.getAsString("device_id"));
                linaccelSample.setAccuracy(data.getAsInteger("accuracy"));
                linaccelSample.setLabel(data.getAsString("label"));
                linaccelSample.setDouble_values_0(data.getAsDouble("double_values_0"));
                linaccelSample.setDouble_values_1(data.getAsDouble("double_values_1"));
                linaccelSample.setDouble_values_2(data.getAsDouble("double_values_2"));
                linaccelSamples = linaccelSamples + new Gson().toJson(linaccelSample) + ",";
            }
        });

        Gyroscope.setSensorObserver(new Gyroscope.AWARESensorObserver() {
            @Override
            public void onGyroscopeChanged(ContentValues data) {
                Sample gyroSample = new Sample();
                gyroSample.setTimestamp(data.getAsLong("timestamp"));
                gyroSample.setDevice_id(data.getAsString("device_id"));
                gyroSample.setAccuracy(data.getAsInteger("accuracy"));
                gyroSample.setLabel(data.getAsString("label"));
                gyroSample.setDouble_values_0(data.getAsDouble("double_values_0"));
                gyroSample.setDouble_values_1(data.getAsDouble("double_values_1"));
                gyroSample.setDouble_values_2(data.getAsDouble("double_values_2"));
                gyroSamples = gyroSamples + new Gson().toJson(gyroSample) + ",";
            }
        });

        Rotation.setSensorObserver(new Rotation.AWARESensorObserver() {
            @Override
            public void onRotationChanged(ContentValues data) {
                Sample rotationSample = new Sample();
                rotationSample.setTimestamp(data.getAsLong("timestamp"));
                rotationSample.setDevice_id(data.getAsString("device_id"));
                rotationSample.setAccuracy(data.getAsInteger("accuracy"));
                rotationSample.setLabel(data.getAsString("label"));
                rotationSample.setDouble_values_0(data.getAsDouble("double_values_0"));
                rotationSample.setDouble_values_1(data.getAsDouble("double_values_1"));
                rotationSample.setDouble_values_2(data.getAsDouble("double_values_2"));
                rotationSamples = rotationSamples + new Gson().toJson(rotationSample) + ",";
            }
        });
    }

    // Stop data sampling
    private void stopSensors() {

        // Stopping sensors
        Aware.stopAccelerometer(getApplicationContext());
        Aware.stopLinearAccelerometer(getApplicationContext());
        Aware.stopGyroscope(getApplicationContext());
        Aware.stopRotation(getApplicationContext());

        // Adjusting data to final JSON format
        String acccel = "{\"accelerometer\":" + accelSamples.substring(0, accelSamples.length()-1) + "],";
        String linaccel = "\"linearaccelerometer\":" + linaccelSamples.substring(0, linaccelSamples.length()-1) + "],";
        String gyro = "\"gyroscope\":" + gyroSamples.substring(0, gyroSamples.length()-1) + "],";
        String rotation = "\"rotation\":" + rotationSamples.substring(0, rotationSamples.length()-1) + "]}";
        String result = acccel + linaccel + gyro + rotation;

        // Inserting data to database
        ContentValues values = new ContentValues();
        values.put(Provider.Game_Data.TIMESTAMP, System.currentTimeMillis());
        values.put(Provider.Game_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        values.put(Provider.Game_Data.DATA, result);
        getContentResolver().insert(Provider.Game_Data.CONTENT_URI, values);

        Log.d(MainActivity.MYO_TAG, "JSON length " + String.valueOf(result.length()));

        // Recording result to local .txt file                 // for testing only
        File root = new File(Environment.getExternalStorageDirectory().toString());
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        File gpxfile = new File(root, "samples" + ts +".txt");
        FileWriter writer = null;
        try {
            writer = new FileWriter(gpxfile);
            writer.append(result);
            writer.flush();
            writer.close();
            Log.d(MainActivity.MYO_TAG, "logging done");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(MainActivity.MYO_TAG, "logging not done");
            Log.d(MainActivity.MYO_TAG, e.getMessage());
        }
    }
}
