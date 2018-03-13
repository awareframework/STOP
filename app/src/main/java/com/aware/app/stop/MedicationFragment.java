package com.aware.app.stop;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class MedicationFragment extends Fragment {

    private final static int RC_SPEECH_INPUT = 1;

    public MedicationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Button nowBtn = view.findViewById(R.id.nowBtn);
        nowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Inserting data to database
                ContentValues values = new ContentValues();
                values.put(Provider.Medication_Data.TIMESTAMP, System.currentTimeMillis());
                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);

                Log.d(MainActivity.MYO_TAG, "Done");

            }
        });

        ImageButton micBtn = view.findViewById(R.id.micBtn);
        micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent listenToUser = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                listenToUser.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK);
                listenToUser.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                listenToUser.putExtra(RecognizerIntent.EXTRA_PROMPT, "I'm listening...");

                startActivityForResult(listenToUser, RC_SPEECH_INPUT);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d(MainActivity.MYO_TAG, results.toString());
                //voice_recog.setText(results.get(0));
            }
        }

    }
}
