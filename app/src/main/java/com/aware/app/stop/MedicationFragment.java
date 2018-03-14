package com.aware.app.stop;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
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
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;


public class MedicationFragment extends Fragment{

    // Text-to-speech components
    private TextToSpeech tts;
    private HashMap<String, String> params;

    // UI components
    private Button nowBtn;
    private ImageButton micBtn;

    // calendar variables
    private int hour, minute, day, month, year;

    private final static int RC_SPEECH_INPUT = 1;

    public MedicationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Text-to-speech initializing
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

        // Now button immidiately records timestamp to db
        nowBtn = view.findViewById(R.id.nowBtn);
        nowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inserting data to database
                ContentValues values = new ContentValues();
                values.put(Provider.Medication_Data.TIMESTAMP, System.currentTimeMillis());
                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);
                Toast.makeText(getActivity(), "Medication recorded", Toast.LENGTH_SHORT).show();

                Log.d(MainActivity.MYO_TAG, "Now timestamp inserted");
            }
        });

        // Mic button listens for users voice
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

        nowBtn.setEnabled(true);
        micBtn.setEnabled(true);

        if (requestCode == RC_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d(MainActivity.MYO_TAG, "Voice: " + results.get(0));

                // parsing timestamp from users response
                TimestampParser tp = new TimestampParser();
                try {
                    Log.d(MainActivity.MYO_TAG, "Parse started");
                    verifyTime(tp.execute(results.get(0)).get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Log.d(MainActivity.MYO_TAG, "UI:Parser error: " + e.getMessage());
                }
            }
        }
    }

    // running Recognizer Intent
    private void listenVoice() {
        Intent listenToUser = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK);
        listenToUser.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        listenToUser.putExtra(RecognizerIntent.EXTRA_PROMPT, "I'm listening...");
        startActivityForResult(listenToUser, RC_SPEECH_INPUT);
    }

    // offer user to verify date received from voice
    private void verifyTime(final long time) {

        if (time != 0) {

            final Date date = new Date(time*1000L);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);
            day = calendar.get(Calendar.DATE);
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);
            Log.d(MainActivity.MYO_TAG, "Parsed: " + String.valueOf(time));

            // transforming timestamp to readable date format to show in dialog
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd.MM.yyyy");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            String formattedDate = sdf.format(date);

            // AlertDialog to verify the date
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getActivity());
            }
            builder.setTitle("Check medication time")
                    .setMessage(formattedDate)
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // Date is OK, write it to db
                            ContentValues values = new ContentValues();
                            values.put(Provider.Medication_Data.TIMESTAMP, time);
                            values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                            getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);

                            Toast.makeText(getActivity(), "Medication recorded", Toast.LENGTH_SHORT).show();
                            Log.d(MainActivity.MYO_TAG, "Confirmed: " + time);

                        }

                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                            dialog.dismiss();
                            Log.d(MainActivity.MYO_TAG, "Cancelled: " + time);
                        }

                    })
                    .setNeutralButton("Edit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // Date is wrong, editing it manually
                            TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourPicked, int minutePicked) {

                                    hour = hourPicked;
                                    minute = minutePicked;

                                    DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {
                                            year = yearPicked;
                                            month = monthPicked;
                                            day = dayPicked;
                                            calendar.set(year, month, day, hour, minute);

                                            // Date is fixed, write it to db
                                            ContentValues values = new ContentValues();
                                            values.put(Provider.Medication_Data.TIMESTAMP, calendar.getTimeInMillis()/1000);
                                            values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                            getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);
                                            Toast.makeText(getActivity(), "Medication recorded", Toast.LENGTH_SHORT).show();
                                            Log.d(MainActivity.MYO_TAG, "Updated: " + String.valueOf(calendar.getTimeInMillis()/1000));

                                        }
                                    }, year, month, day);
                                    datePickerDialog.setTitle("Check date");
                                    datePickerDialog.show();

                                }
                            }, hour, minute, true);
                            timePickerDialog.setTitle("Check time");
                            timePickerDialog.show();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();

        } else {
            Toast.makeText(getActivity(), "Cannot recognize date, please try again", Toast.LENGTH_SHORT).show();
        }

    }
}
