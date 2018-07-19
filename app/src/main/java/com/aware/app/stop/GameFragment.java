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
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Gyroscope;
import com.aware.LinearAccelerometer;
import com.aware.Rotation;
import com.aware.app.stop.database.Provider;
import com.google.gson.Gson;

import java.text.DecimalFormat;

import static android.content.Context.MODE_PRIVATE;

public class GameFragment extends Fragment {

    // UI elements
    private FrameLayout containerLayout;
    private TextView timer;
    private ImageButton playBtn;
    private BallView ballView;
    private Button playAgain, openMedications;

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
    private String lastScore;

    // Ball game settings variables
    private int ballSize;
    private int smallCircleSize;
    private int bigCircleSize;
    private float sensitivity; // 3.0 is default
    private int gameTime; // in milliseconds

    // sampling flag
    static boolean sampling;

    // Strings for storing sampling data in JSON format
    private String gameData;
    private StringBuffer accelSamples = new StringBuffer();
    private StringBuffer linaccelSamples = new StringBuffer();
    private StringBuffer gyroSamples = new StringBuffer();
    private StringBuffer rotationSamples = new StringBuffer();

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
        playAgain = view.findViewById(R.id.playAgain);
        openMedications = view.findViewById(R.id.openMedications);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ballSize>0 && gameTime>0 && sensitivity>0) {
                    startGame();
                } else {
                    Toast.makeText(getContext(), R.string.game_invalid_settings, Toast.LENGTH_SHORT).show();
                }
            }
        });

        playAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ballSize>0 && gameTime>0 && sensitivity>0) {
                    startGame();
                } else {
                    Toast.makeText(getContext(), R.string.game_invalid_settings, Toast.LENGTH_SHORT).show();
                }
            }
        });

        openMedications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomNavigationView nav = getActivity().findViewById(R.id.main_nav);
                nav.setSelectedItemId(R.id.nav_medication);

                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.main_frame, new MedicationFragment());
                fragmentTransaction.commit();
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

                    accelSamples.append(new Gson().toJson(accelSample)).append(",");
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

                    linaccelSamples.append(new Gson().toJson(linaccelSample)).append(",");
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

                    gyroSamples.append(new Gson().toJson(gyroSample)).append(",");
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

                    rotationSamples.append(new Gson().toJson(rotationSample)).append(",");
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
        sensitivity = Float.parseFloat(sPref.getString(getString(R.string.key_sensitivity), String.valueOf(R.string.key_sensitivity_value)));
        gameTime = Integer.parseInt(sPref.getString(getString(R.string.key_game_time), String.valueOf(R.string.key_game_time_value)))*1000;

        // detection of the display size
        Point size = new Point();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        display.getSize(size);
        deviceXres = size.x;
        deviceYres = size.y;

        // Relative size of game elements
        // TEST for "Doro" smartphone
        ballSize = deviceXres/7;
        smallCircleSize = ballSize*3;
        bigCircleSize = ballSize*5;

        // setting up the maximum allowed X and Y values
        ballXmax = (float) size.x - ballSize;
        ballYmax = (float) size.y*0.77f - ballSize; // Relative size test for "Doro"

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

        // sampling to false to prevent unnecessary data recording
        sampling = false;

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
    public void onStop() {
        super.onStop();

        // reset UI to the initial state
        containerLayout.removeAllViews();
        ballView = null;
        playBtn.setVisibility(View.VISIBLE);
        playAgain.setVisibility(View.INVISIBLE);
        openMedications.setVisibility(View.INVISIBLE);
        playBtn.setEnabled(true);
        playAgain.setEnabled(false);
        openMedications.setEnabled(false);
        timer.setText(R.string.game_press_button_to_play);

        // sampling to false to prevent unnecessary data recording
        sampling = false;

        // cancelling timer
        if (countDownTimer != null) countDownTimer.cancel();

        // stopping sensors
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
        playAgain.setVisibility(View.INVISIBLE);
        openMedications.setVisibility(View.INVISIBLE);
        playBtn.setEnabled(false);
        playAgain.setEnabled(false);
        openMedications.setEnabled(false);
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

        // Retrieve last game score value
        lastScore = getActivity().getSharedPreferences("scorePref", MODE_PRIVATE).getString("lastScore", "0");

        // making sample values empty (for second and following games)
        accelSamples.setLength(0);
        linaccelSamples.setLength(0);
        gyroSamples.setLength(0);
        rotationSamples.setLength(0);
        scoreRaw = 0;
        scoreCounter = 0;

        // starting timer
        countDownTimer.start();

    }

    // Stop data sampling
    private void stopGame() {

        // calculating game score
        double finalScore = 100 - ((scoreRaw/scoreCounter)/ ballMaxDistance)*100;
        String gameDone = getString(R.string.game_done_1) + String.format("%.1f", finalScore) +
                getString(R.string.game_done_2) + lastScore + getString(R.string.game_done_3);

        // updating UI
        containerLayout.removeAllViews();
        ballView = null;
        playAgain.setVisibility(View.VISIBLE);
        openMedications.setVisibility(View.VISIBLE);
        playAgain.setEnabled(true);
        openMedications.setEnabled(true);
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
        String accel, linaccel, gyro, rotation;

        try {
            accel = "\"accelerometer\":[" + accelSamples.substring(0, accelSamples.length()-1) + "],";
        } catch (Exception e) {
            accel = "\"accelerometer\":[\"not_activated\"],";
            Toast.makeText(getContext(), R.string.game_collection_error, Toast.LENGTH_LONG).show();
        }

        try {
            linaccel = "\"linearaccelerometer\":[" + linaccelSamples.substring(0, linaccelSamples.length()-1) + "],";
        } catch (Exception e) {
            linaccel = "\"linearaccelerometer\":[\"not_activated\"],";
        }

        try {
            gyro = "\"gyroscope\":[" + gyroSamples.substring(0, gyroSamples.length()-1) + "],";
        } catch (Exception e) {
            gyro = "\"gyroscope\":[\"not_activated\"],";
        }

        try {
            rotation = "\"rotation\":[" + rotationSamples.substring(0, rotationSamples.length()-1) + "]}";
        } catch (Exception e) {
            rotation = "\"rotation\":[\"not_activated\"],";
        }

        String result = gamedata + accel + linaccel + gyro + rotation;


        // Record game score to SharedPref as the last one
        SharedPreferences score = getActivity().getSharedPreferences("scorePref", MODE_PRIVATE);
        SharedPreferences.Editor editor = score.edit();
        editor.putString("lastScore", String.format("%.1f", finalScore));
        editor.commit();

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
