package com.aware.app.stop;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
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

public class BallActivity extends AppCompatActivity{

    // ball game variables
    private float ballXpos, ballXmax, ballXaccel, ballXvel = 0.0f;
    private float ballYpos, ballYmax, ballYaccel, ballYvel = 0.0f;
    private float bigCircleXpos, bigCircleYpos;
    private float smallCircleXpos, smallCircleYpos;
    private Bitmap ball;
    private Bitmap circleBig;
    private Bitmap circleSmall;

    // UI elements
    private FrameLayout container;
    private TextView timer;

    // Strings for storing sampling data in JSON format
    private String gameData;
    private String accelSamples = "";
    private String linaccelSamples = "";
    private String gyroSamples = "";
    private String rotationSamples = "";

    boolean sampling = false;

    // Ball game settings variables
    private int ballSize;
    private int smallCircleSize;
    private int bigCircleSize;
    private float sensitivity; // 3.0 is default
    private int gameTime; // in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ball);

        // reading settings values from SettingsActivity
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ballSize = Integer.parseInt(sPref.getString(getString(R.string.key_ball_size), "100"));
        smallCircleSize = Integer.parseInt(sPref.getString(getString(R.string.key_small_circle_size), "300"));
        bigCircleSize = Integer.parseInt(sPref.getString(getString(R.string.key_big_circle_size), "500"));
        sensitivity = Float.parseFloat(sPref.getString(getString(R.string.key_sensitivity), "3.0"));
        gameTime = Integer.parseInt(sPref.getString(getString(R.string.key_game_time), "10"))*1000;

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

        // setting up sensors
        Accelerometer.setSensorObserver(new Accelerometer.AWARESensorObserver() {
            @Override
            public void onAccelerometerChanged(ContentValues data) {
                ballXaccel = data.getAsFloat("double_values_0");
                ballYaccel = -data.getAsFloat("double_values_1");
                updateBall(data.getAsLong("timestamp"));

                Sample accelSample = new Sample(data.getAsLong("timestamp"),
                                                data.getAsString("device_id"),
                                                data.getAsDouble("double_values_0"),
                                                data.getAsDouble("double_values_1"),
                                                data.getAsDouble("double_values_2"),
                                                data.getAsInteger("accuracy"),
                                                data.getAsString("label"));

                if (sampling) {
                    accelSamples += new Gson().toJson(accelSample) + ",";
                }
            }
        });

        LinearAccelerometer.setSensorObserver(new LinearAccelerometer.AWARESensorObserver() {
            @Override
            public void onLinearAccelChanged(ContentValues data) {
                Sample linaccelSample = new Sample(data.getAsLong("timestamp"),
                                                   data.getAsString("device_id"),
                                                   data.getAsDouble("double_values_0"),
                                                   data.getAsDouble("double_values_1"),
                                                   data.getAsDouble("double_values_2"),
                                                   data.getAsInteger("accuracy"),
                                                   data.getAsString("label"));

                if (sampling) {
                    linaccelSamples += new Gson().toJson(linaccelSample) + ",";
                }
            }
        });

        Gyroscope.setSensorObserver(new Gyroscope.AWARESensorObserver() {
            @Override
            public void onGyroscopeChanged(ContentValues data) {
                Sample gyroSample = new Sample(data.getAsLong("timestamp"),
                                               data.getAsString("device_id"),
                                               data.getAsDouble("double_values_0"),
                                               data.getAsDouble("double_values_1"),
                                               data.getAsDouble("double_values_2"),
                                               data.getAsInteger("accuracy"),
                                               data.getAsString("label"));

                if (sampling) {
                    gyroSamples += new Gson().toJson(gyroSample) + ",";
                }
            }
        });

        Rotation.setSensorObserver(new Rotation.AWARESensorObserver() {
            @Override
            public void onRotationChanged(ContentValues data) {
                Sample rotationSample = new Sample(data.getAsLong("timestamp"),
                                                   data.getAsString("device_id"),
                                                   data.getAsDouble("double_values_0"),
                                                   data.getAsDouble("double_values_1"),
                                                   data.getAsDouble("double_values_2"),
                                                   data.getAsInteger("accuracy"),
                                                   data.getAsString("label"));

                if (sampling) {
                    rotationSamples += new Gson().toJson(rotationSample) + ",";
                }
            }
        });

        // timer initialization
        new CountDownTimer(gameTime + 5000, 1000) {
            public void onTick(long millisUntilFinished) {
                // updating UI according to timeframes and handling sensors
                if ((millisUntilFinished >= gameTime + 999) && (millisUntilFinished <= gameTime + 3999)) {
                    String counter = String.valueOf((millisUntilFinished/1000 - gameTime /1000)) + "...";
                    timer.setText(counter);
                }

                if ((millisUntilFinished >= gameTime - 1) && (millisUntilFinished < gameTime + 999)) {
                    timer.setText("Start!");
                    sampling = true;
                }

                if ((millisUntilFinished >= 0) && (millisUntilFinished < gameTime - 1)) {
                    String counter = String.valueOf((millisUntilFinished/1000 - gameTime /1000)*(-1));
                    timer.setText(counter);
                }
            }

            public void onFinish() {
                timer.setText("Done!");
                sampling = false;
                stopSensors();
                finish();
            }
        }.start();

        // detection of the display size
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        ballXmax = (float) size.x - ballSize;
        ballYmax = (float) size.y - ballSize - 235; // 235 is a toolbar?

        gameData = "{\"gamedata\":[{\"ball_radius\":" + ballSize + ",";
        gameData += "\"sensitivity\":" + sensitivity + ",";
        gameData += "\"device_x_res\":" + size.x + ",";
        gameData += "\"device_y_res\":" + size.y + "," + "\"samples\":[";

        // put ball to the center
        ballXpos = ballXmax /2;
        ballYpos = ballYmax /2;

        // put circles to the center
        //smallCircleXpos = ballXpos - ballSize;
        smallCircleXpos = (size.x - smallCircleSize)/2;
        //smallCircleYpos = ballYpos - ballSize;
        smallCircleYpos = (size.y - smallCircleSize - 235)/2;
        bigCircleXpos = (size.x - bigCircleSize)/2;
        bigCircleYpos = (size.y - bigCircleSize - 235)/2;
    }

    // updating ball's X and Y positioning
    private void updateBall(long timestamp) {
        ballXvel = (ballXaccel * sensitivity);
        ballYvel = (ballYaccel * sensitivity);

        float xS = (ballXvel / 2) * sensitivity;
        float yS = (ballYvel / 2) * sensitivity;

        ballXpos -= xS;
        ballYpos -= yS;

        float changeX = ballXpos - ballXmax/2;
        float changey = ballYpos - ballYmax/2;

        if (sampling) {
            gameData += "{\"timestamp\":" + timestamp + ",";
            gameData += "\"ball_x\":" + changeX + ",";
            gameData += "\"ball_y\":" + changey + "},";
        }

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

    // custom view for BallGame
    private class BallView extends View {

        public BallView(Context context) {
            super(context);
            // ball bitmap initializing
            Bitmap ballSrc = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
            ball = Bitmap.createScaledBitmap(ballSrc, ballSize, ballSize, true);

            // circles bitmap initializing
            Bitmap smallSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_small);
            circleSmall = Bitmap.createScaledBitmap(smallSrc, smallCircleSize, smallCircleSize, true);
            Bitmap bigSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_big);
            circleBig = Bitmap.createScaledBitmap(bigSrc, bigCircleSize, bigCircleSize, true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // drawing circles
            canvas.drawBitmap(circleSmall, smallCircleXpos, smallCircleYpos, null);
            canvas.drawBitmap(circleBig, bigCircleXpos, bigCircleYpos, null);

            // drawing (and redrawing) the ball
            canvas.drawBitmap(ball, ballXpos, ballYpos, null);
            invalidate();
        }
    }

    // Stop data sampling
    private void stopSensors() {

        // Stopping sensors
        Aware.stopAccelerometer(getApplicationContext());
        Aware.stopLinearAccelerometer(getApplicationContext());
        Aware.stopGyroscope(getApplicationContext());
        Aware.stopRotation(getApplicationContext());
        Log.d(MainActivity.MYO_TAG, "turned off");

        // Adjusting data to final JSON format
        String gamedata = gameData.substring(0, gameData.length()-1) + "]}],";
        String acccel = "\"accelerometer\":[" + accelSamples.substring(0, accelSamples.length()-1) + "],";
        String linaccel = "\"linearaccelerometer\":[" + linaccelSamples.substring(0, linaccelSamples.length()-1) + "],";
        String gyro = "\"gyroscope\":[" + gyroSamples.substring(0, gyroSamples.length()-1) + "],";
        String rotation = "\"rotation\":[" + rotationSamples.substring(0, rotationSamples.length()-1) + "]}";
        String result = gamedata + acccel + linaccel + gyro + rotation;

        // Inserting data to database
        ContentValues values = new ContentValues();
        values.put(Provider.Game_Data.TIMESTAMP, System.currentTimeMillis());
        values.put(Provider.Game_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        values.put(Provider.Game_Data.DATA, result);
        getContentResolver().insert(Provider.Game_Data.CONTENT_URI, values);

        Log.d(MainActivity.MYO_TAG, "JSON length " + String.valueOf(result.length()));

        // Recording result to local .txt file
        // for testing only
        /*
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
            Log.d(MainActivity.MYO_TAG, "Logging done");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(MainActivity.MYO_TAG, "Logging not done");
            Log.d(MainActivity.MYO_TAG, e.getMessage());
        }
        */
    }
}
