package com.aware.app.stop;

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BallActivity extends AppCompatActivity implements SensorEventListener2 {

    private float xPos, xAccel, xVel = 0.0f;
    private float yPos, yAccel, yVel = 0.0f;
    private float xMax, yMax;
    private Bitmap ball;
    private SensorManager sensorManager;

    private Bitmap circleBig;
    private float bigXpos, bigYpos;

    private Bitmap circleSmall;
    private float smallXpos, smallYpos;

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

        // timer initialization
        new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) {
                //here you can have your logic to set text to edittext
                if ((millisUntilFinished >= 11500) && (millisUntilFinished <= 14500)) {
                    String counter = String.valueOf((millisUntilFinished - 10000)/1000) + "...";
                    timer.setText(counter);
                }

                if ((millisUntilFinished >= 10500) && (millisUntilFinished < 11500)) {
                    timer.setText("Start!");
                }

                if ((millisUntilFinished >= 0) && (millisUntilFinished < 10500)) {
                    String counter = String.valueOf(((millisUntilFinished/1000) - 10)*(-1));
                    timer.setText(counter);
                }
            }

            public void onFinish() {
                timer.setText("Done!");
                finish();
            }
        }.start();

        // detection of the display size
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        xMax = (float) size.x - 100;
        yMax = (float) size.y - 335; // need to fix Y

        // put ball to the center
        xPos = xMax/2;
        yPos = yMax/2;

        // put circles to the center
        smallXpos = xPos - 100;
        smallYpos = yPos - 100;
        bigXpos = xPos - 200;
        bigYpos = yPos - 200;

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
            xAccel = event.values[0];
            yAccel = -event.values[1];
            updateBall();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // updating ball's X and Y positioning
    private void updateBall() {
        float frameTime = 3.000f;
        xVel = (xAccel * frameTime);
        yVel = (yAccel * frameTime);

        float xS = (xVel / 2) * frameTime;
        float yS = (yVel / 2) * frameTime;

        xPos -= xS;
        yPos -= yS;

        //off screen movements
        if (xPos > xMax) {
            xPos = xMax;
        } else if (xPos < 0) {
            xPos = 0;
        }

        if (yPos > yMax) {
            yPos = yMax;
        } else if (yPos < 0) {
            yPos = 0;
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

            // hole bitmap
            Bitmap smallSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_small);
            final int smallWidth = 300;
            final int smallHeight = 300;
            circleSmall = Bitmap.createScaledBitmap(smallSrc, smallWidth, smallHeight, true);

            // hole bitmap
            Bitmap bigSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_big);
            final int bigWidth = 500;
            final int bigHeight = 500;
            circleBig = Bitmap.createScaledBitmap(bigSrc, bigWidth, bigHeight, true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // drawing (and redrawing) the ball
            canvas.drawBitmap(ball, xPos, yPos, null);
            invalidate();

            // drawing circles
            canvas.drawBitmap(circleSmall, smallXpos, smallYpos, null);
            canvas.drawBitmap(circleBig, bigXpos, bigYpos, null);
        }
    }

}
