package com.aware.app.stop;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class MedicationFragment extends Fragment{

    // Text-to-speech components
    private TextToSpeech tts;
    private HashMap<String, String> params;

    // UI components
    private Button nowBtn;
    private ImageButton micBtn;

    private final static int RC_SPEECH_INPUT = 1;

    public MedicationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0");

        tts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.UK);
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            listenVoice();
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });

                } else {
                    Intent install = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(install);
                }
            }
        });

        nowBtn = view.findViewById(R.id.nowBtn);
        nowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inserting data to database
                ContentValues values = new ContentValues();
                values.put(Provider.Medication_Data.TIMESTAMP, System.currentTimeMillis());
                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);

                Log.d(MainActivity.MYO_TAG, "Now timestamp inserted");
            }
        });

        micBtn = view.findViewById(R.id.micBtn);
        micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nowBtn.setEnabled(false);
                micBtn.setEnabled(false);
                tts.speak("When have you taken medication last time?", TextToSpeech.QUEUE_FLUSH, params);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(MainActivity.MYO_TAG, "result");

        nowBtn.setEnabled(true);
        micBtn.setEnabled(true);

        if (requestCode == RC_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d(MainActivity.MYO_TAG, results.toString());
                //voice_recog.setText(results.get(0));
            }
        }
    }

    private void listenVoice() {
        Intent listenToUser = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK);
        listenToUser.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        listenToUser.putExtra(RecognizerIntent.EXTRA_PROMPT, "I'm listening...");
        startActivityForResult(listenToUser, RC_SPEECH_INPUT);
    }

}
