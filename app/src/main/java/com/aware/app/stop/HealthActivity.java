package com.aware.app.stop;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

public class HealthActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private Button healthSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health);

        // Get an instance of the NotificationManager service
        // Cancel SurveyNotification when HealthActivity opens
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(MainActivity.SURVEY_NOTIFICATION_ID);

        // Tracking notification opening state
        Intent intent  = getIntent();
        String time = intent.getStringExtra(MainActivity.NOTIFICATION_TRIGGER_EVENT);

        if (time!=null && time.equalsIgnoreCase(getString(R.string.notification_survey))) {
            //Insert notification opened event to db
            String event = time + MainActivity.NOTIFICATION_EVENT_OPENED;

            ContentValues values = new ContentValues();
            values.put(Provider.Notification_Data.TIMESTAMP, System.currentTimeMillis());
            values.put(Provider.Notification_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            values.put(Provider.Notification_Data.EVENT, event);
            getContentResolver().insert(Provider.Notification_Data.CONTENT_URI, values);
        }

        //UI initialization
        radioGroup = findViewById(R.id.radioGroup);
        healthSubmit = findViewById(R.id.healthSubmit);

        healthSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int selected = radioGroup.getCheckedRadioButtonId();

                if (selected == -1) {
                    Toast.makeText(getApplicationContext(), R.string.health_empty_response, Toast.LENGTH_SHORT).show();

                } else {
                    RadioButton radioButton = findViewById(selected);
                    String pd_value = radioButton.getText().toString().toLowerCase();

                    ContentValues values = new ContentValues();
                    values.put(Provider.Health_Data.TIMESTAMP, System.currentTimeMillis());
                    values.put(Provider.Health_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    values.put(Provider.Health_Data.PD_VALUE, pd_value);
                    getContentResolver().insert(Provider.Health_Data.CONTENT_URI, values);

                    Toast.makeText(getApplicationContext(), R.string.health_survey_saved, Toast.LENGTH_SHORT).show();
                    finish();
                }

            }
        });
    }
}
