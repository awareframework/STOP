package com.aware.app.stop;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Gyroscope;
import com.aware.LinearAccelerometer;
import com.aware.Rotation;
import com.aware.app.stop.database.Provider;
import com.google.gson.Gson;

public class GameFragment extends Fragment {

    // UI elements
    private FrameLayout containerLayout;
    private TextView timer;
    private ImageButton playBtn;
    private BallView ballView;

    // Sensors variables
    private Accelerometer.AWARESensorObserver observerAccelerometer;
    private LinearAccelerometer.AWARESensorObserver observerLinearAccelerometer;
    private Gyroscope.AWARESensorObserver observerGyroscope;
    private Rotation.AWARESensorObserver observerRotation;

    // Timer component
    private CountDownTimer countDownTimer;

    // ball game variables
    private float ballXpos, ballXmax, ballXaccel, ballXvel = 0.0f;
    private float ballYpos, ballYmax, ballYaccel, ballYvel = 0.0f;
    private float bigCircleXpos, bigCircleYpos;
    private float smallCircleXpos, smallCircleYpos;
    private double ballMaxDistance, scoreRaw;
    private int deviceXres, deviceYres, scoreCounter;
    private Bitmap ball;
    private Bitmap circleBig;
    private Bitmap circleSmall;

    // Ball game settings variables
    private int ballSize;
    private int smallCircleSize;
    private int bigCircleSize;
    private float sensitivity; // 3.0 is default
    private int gameTime; // in milliseconds

    // sampling flag
    boolean sampling = false;

    // Strings for storing sampling data in JSON format
    private String gameData;
    private String accelSamples;
    private String linaccelSamples;
    private String gyroSamples;
    private String rotationSamples;

    private static final String SAMPLE_KEY_TIMESTAMP = "timestamp";
    private static final String SAMPLE_KEY_DEVICE_ID = "device_id";
    private static final String SAMPLE_KEY_DOUBLE_VALUES_0 = "double_values_0";
    private static final String SAMPLE_KEY_DOUBLE_VALUES_1 = "double_values_1";
    private static final String SAMPLE_KEY_DOUBLE_VALUES_2 = "double_values_2";
    private static final String SAMPLE_KEY_ACCURACY = "accuracy";
    private static final String SAMPLE_KEY_LABEL = "label";

    public GameFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        // Initializing views
        timer = view.findViewById(R.id.timer);
        containerLayout = view.findViewById(R.id.container);
        playBtn = view.findViewById(R.id.playBtn);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });


        // Initializing sensor observers
        observerAccelerometer = new Accelerometer.AWARESensorObserver() {
            @Override
            public void onAccelerometerChanged(ContentValues data) {
                ballXaccel = data.getAsFloat(SAMPLE_KEY_DOUBLE_VALUES_0);
                ballYaccel = -data.getAsFloat(SAMPLE_KEY_DOUBLE_VALUES_1);
                updateBall(data.getAsLong(SAMPLE_KEY_TIMESTAMP));

                if (sampling) {
                    Sample accelSample = new Sample(data.getAsLong(SAMPLE_KEY_TIMESTAMP),
                            data.getAsString(SAMPLE_KEY_DEVICE_ID),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_0),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_1),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_2),
                            data.getAsInteger(SAMPLE_KEY_ACCURACY),
                            data.getAsString(SAMPLE_KEY_LABEL));

                    accelSamples += new Gson().toJson(accelSample) + ",";
                }
            }
        };

        observerLinearAccelerometer = new LinearAccelerometer.AWARESensorObserver() {
            @Override
            public void onLinearAccelChanged(ContentValues data) {
                if (sampling) {
                    Sample linaccelSample = new Sample(data.getAsLong(SAMPLE_KEY_TIMESTAMP),
                            data.getAsString(SAMPLE_KEY_DEVICE_ID),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_0),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_1),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_2),
                            data.getAsInteger(SAMPLE_KEY_ACCURACY),
                            data.getAsString(SAMPLE_KEY_LABEL));

                    linaccelSamples += new Gson().toJson(linaccelSample) + ",";
                }
            }
        };

        observerGyroscope = new Gyroscope.AWARESensorObserver() {
            @Override
            public void onGyroscopeChanged(ContentValues data) {
                if (sampling) {
                    Sample gyroSample = new Sample(data.getAsLong(SAMPLE_KEY_TIMESTAMP),
                            data.getAsString(SAMPLE_KEY_DEVICE_ID),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_0),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_1),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_2),
                            data.getAsInteger(SAMPLE_KEY_ACCURACY),
                            data.getAsString(SAMPLE_KEY_LABEL));

                    gyroSamples += new Gson().toJson(gyroSample) + ",";
                }
            }
        };

        observerRotation = new Rotation.AWARESensorObserver() {
            @Override
            public void onRotationChanged(ContentValues data) {
                if (sampling) {
                    Sample rotationSample = new Sample(data.getAsLong(SAMPLE_KEY_TIMESTAMP),
                            data.getAsString(SAMPLE_KEY_DEVICE_ID),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_0),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_1),
                            data.getAsDouble(SAMPLE_KEY_DOUBLE_VALUES_2),
                            data.getAsInteger(SAMPLE_KEY_ACCURACY),
                            data.getAsString(SAMPLE_KEY_LABEL));

                    rotationSamples += new Gson().toJson(rotationSample) + ",";
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // reading settings values from SettingsActivity
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        ballSize = Integer.parseInt(sPref.getString(getString(R.string.key_ball_size), String.valueOf(R.string.key_ball_size_value)));
        smallCircleSize = Integer.parseInt(sPref.getString(getString(R.string.key_small_circle_size), String.valueOf(R.string.key_small_circle_size_value)));
        bigCircleSize = Integer.parseInt(sPref.getString(getString(R.string.key_big_circle_size), String.valueOf(R.string.key_big_circle_size_value)));
        sensitivity = Float.parseFloat(sPref.getString(getString(R.string.key_sensitivity), String.valueOf(R.string.key_sensitivity_value)));
        gameTime = Integer.parseInt(sPref.getString(getString(R.string.key_game_time), String.valueOf(R.string.key_game_time_value)))*1000;

        // detection of the display size
        Point size = new Point();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        display.getSize(size);
        deviceXres = size.x;
        deviceYres = size.y;

        // setting up the maximum allowed X and Y values
        ballXmax = (float) size.x - ballSize;
        ballYmax = (float) size.y - ballSize - 235 - 175; // toolbar = 235, bottom nav bar = 175

        // put ball to the center
        ballXpos = ballXmax /2;
        ballYpos = ballYmax /2;

        // count maximum possible distance ball can cover from center
        ballMaxDistance = Math.sqrt(ballXpos*ballXpos + ballYpos*ballYpos);

        // put circles to the center
        smallCircleXpos = (size.x - smallCircleSize)/2;
        smallCircleYpos = (size.y - smallCircleSize - 235 - 175)/2;
        bigCircleXpos = (size.x - bigCircleSize)/2;
        bigCircleYpos = (size.y - bigCircleSize - 235 -175)/2;

        // Initializing timer
        countDownTimer = new CountDownTimer(gameTime + 5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // updating UI according to timeframes and handling sensors
                if ((millisUntilFinished >= gameTime + 1000) && (millisUntilFinished <= gameTime + 4000)) {
                    String counter = String.valueOf((millisUntilFinished/1000 - gameTime/1000)) + "...";
                    timer.setText(counter);
                }

                if ((millisUntilFinished >= gameTime) && (millisUntilFinished < gameTime + 1000)) {
                    timer.setText(R.string.game_start);
                    sampling = true;
                }

                if ((millisUntilFinished >= 0) && (millisUntilFinished < gameTime)) {
                    String counter = String.valueOf((millisUntilFinished/1000 - gameTime/1000)*(-1));
                    timer.setText(counter);
                }
            }

            @Override
            public void onFinish() {
                sampling = false;
                if (getContext() != null) {
                    stopGame();
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (countDownTimer != null) countDownTimer.cancel();

        Accelerometer.setSensorObserver(null);
        Aware.stopAccelerometer(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_ACCELEROMETER, false);

        LinearAccelerometer.setSensorObserver(null);
        Aware.stopLinearAccelerometer(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_LINEAR_ACCELEROMETER, false);

        Gyroscope.setSensorObserver(null);
        Aware.stopGyroscope(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_GYROSCOPE, false);

        Rotation.setSensorObserver(null);
        Aware.stopRotation(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_ROTATION, false);

    }

    // Inflate BallView and start sensors
    private void startGame() {

        playBtn.setVisibility(View.INVISIBLE);
        playBtn.setEnabled(false);
        timer.setText(R.string.game_get_ready);

        // adding custom BallView to the fragment
        ballView = new BallView(getContext());
        containerLayout.addView(ballView);

        // starting sensors
        Aware.startAccelerometer(getContext());
        Aware.startLinearAccelerometer(getContext());
        Aware.startGyroscope(getContext());
        Aware.startRotation(getContext());

        Accelerometer.setSensorObserver(observerAccelerometer);
        LinearAccelerometer.setSensorObserver(observerLinearAccelerometer);
        Gyroscope.setSensorObserver(observerGyroscope);
        Rotation.setSensorObserver(observerRotation);

        // recording game settings to gamedata
        gameData = "{\"gamedata\":[{\"ball_radius\":" + ballSize + ",";
        gameData += "\"sensitivity\":" + sensitivity + ",";
        gameData += "\"device_x_res\":" + deviceXres + ",";
        gameData += "\"device_y_res\":" + deviceYres + "," + "\"samples\":[";

        // making sample values empty (for second and following games)
        accelSamples = "";
        linaccelSamples = "";
        gyroSamples = "";
        rotationSamples = "";
        scoreRaw = 0;
        scoreCounter = 0;

        // starting timer
        countDownTimer.start();

    }

    // Stop data sampling
    private void stopGame() {

        // calculating game score
        double finalScore = 100 - ((scoreRaw/scoreCounter)/ ballMaxDistance)*100;
        String gameDone = getString(R.string.game_done) + String.format("%.1f", finalScore);

        // updating UI
        containerLayout.removeAllViews();
        ballView = null;
        playBtn.setVisibility(View.VISIBLE);
        playBtn.setEnabled(true);
        timer.setText(gameDone);

        // set ball coordinates to center for playinig again
        ballXpos = ballXmax /2;
        ballYpos = ballYmax /2;

        // Stopping sensors
        Accelerometer.setSensorObserver(null);
        Aware.stopAccelerometer(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_ACCELEROMETER, false);

        LinearAccelerometer.setSensorObserver(null);
        Aware.stopLinearAccelerometer(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_LINEAR_ACCELEROMETER, false);

        Gyroscope.setSensorObserver(null);
        Aware.stopGyroscope(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_GYROSCOPE, false);

        Rotation.setSensorObserver(null);
        Aware.stopRotation(getContext());
        Aware.setSetting(getContext(), Aware_Preferences.STATUS_ROTATION, false);

        // Adjusting data to final JSON format
        String gamedata = gameData.substring(0, gameData.length()-1) + "],\"score\":"+ finalScore +"}],";
        String acccel = "\"accelerometer\":[" + accelSamples.substring(0, accelSamples.length()-1) + "],";
        String linaccel = "\"linearaccelerometer\":[" + linaccelSamples.substring(0, linaccelSamples.length()-1) + "],";
        String gyro = "\"gyroscope\":[" + gyroSamples.substring(0, gyroSamples.length()-1) + "],";
        String rotation = "\"rotation\":[" + rotationSamples.substring(0, rotationSamples.length()-1) + "]}";
        String result = gamedata + acccel + linaccel + gyro + rotation;

        // Inserting data to database
        ContentValues values = new ContentValues();
        values.put(Provider.Game_Data.TIMESTAMP, System.currentTimeMillis());
        values.put(Provider.Game_Data.DEVICE_ID, Aware.getSetting(getContext(), Aware_Preferences.DEVICE_ID));
        values.put(Provider.Game_Data.DATA, result);
        getContext().getContentResolver().insert(Provider.Game_Data.CONTENT_URI, values);
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
        float changeY = ballYpos - ballYmax/2;
        double distance = Math.sqrt(changeX*changeX + changeY*changeY);

        if (sampling) {
            gameData += "{\"timestamp\":" + timestamp + ",";
            gameData += "\"ball_x\":" + changeX + ",";
            gameData += "\"ball_y\":" + changeY + ",";
            gameData += "\"distance\":" + distance + "},";

            scoreRaw += distance;
            scoreCounter += 1;
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
}
