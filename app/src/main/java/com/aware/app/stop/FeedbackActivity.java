package com.aware.app.stop;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

import java.util.ArrayList;
import java.util.Locale;

public class FeedbackActivity extends AppCompatActivity {

    private TextInputEditText feedback, deviceName, deviceId;
    private String strName, strId, voiceText = "";

    private final static int RC_SPEECH_INPUT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // UI elements
        deviceName = findViewById(R.id.deviceName);
        deviceId = findViewById(R.id.deviceId);
        feedback = findViewById(R.id.feedback);
        Button feedbackClear = findViewById(R.id.feedbackClear);
        ImageButton feedbackMic = findViewById(R.id.feedbackMic);
        Button feedbackSubmit = findViewById(R.id.feedbackSubmit);

        // receiving device id and name
        strName = Build.MANUFACTURER + " " + Build.MODEL;
        strId = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
        deviceName.setText(strName);
        deviceId.setText(strId);

        // button to clear feedback editText
        feedbackClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedback.setText("");
                voiceText = "";
            }
        });

        // button to run voice recognizer
        feedbackMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent listenToUser = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK);
                listenToUser.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                listenToUser.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.voice_what_think);
                startActivityForResult(listenToUser, RC_SPEECH_INPUT);
            }
        });

        // button to send data to db
        feedbackSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String result = String.valueOf(feedback.getText());

                if (!result.equals("")) {
                    //Insert feedback to db
                    ContentValues values = new ContentValues();
                    values.put(Provider.Feedback_Data.TIMESTAMP, System.currentTimeMillis());
                    values.put(Provider.Feedback_Data.DEVICE_ID, strId);
                    values.put(Provider.Feedback_Data.DEVICE_NAME, strName);
                    values.put(Provider.Feedback_Data.FEEDBACK, result);
                    getContentResolver().insert(Provider.Feedback_Data.CONTENT_URI, values);

                    Toast.makeText(getApplicationContext(), R.string.feedback_saved, Toast.LENGTH_SHORT).show();
                    finish();

                } else {
                    Toast.makeText(getApplicationContext(), R.string.feedback_enter, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                voiceText += results.get(0) + ". ";
                feedback.setText(voiceText);
            }
        }
    }
}
