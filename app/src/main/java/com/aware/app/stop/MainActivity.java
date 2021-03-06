package com.aware.app.stop;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;
import com.aware.providers.Aware_Provider;
import com.aware.utils.Scheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // UI elements
    private BottomNavigationView mMainNav;
    private GameFragment gameFragment;
    private MedicationFragment medicationFragment;

    public static final String STOP_TAG = "STOP_TAG";
    public static final String ACTION_STOP_FINGERPRINT = "ACTION_STOP_FINGERPRINT";
    public static final String ACTION_STOP_SURVEY = "ACTION_STOP_SURVEY";
    private static final String PACKAGE_NAME = "com.aware.app.stop";

    // Notification variables
    private static NotificationManager manager;
    public static final String NOTIFICATION_TRIGGER_EVENT = "notification_trigger_event";
    public static final String NOTIFICATION_EVENT_OPENED = "_opened";
    private static final String NOTIFICATION_EVENT_SHOWN = "_shown";

    // Notification type identifiers
    private static final int GAME_NOTIFICATION_ID = 1;
    public static final int SURVEY_NOTIFICATION_ID = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Setting up application preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.pref_ball_game, true);
        Aware.isBatteryOptimizationIgnored(getApplicationContext(), PACKAGE_NAME);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_ACCELEROMETER, 20000);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_DB_SLOW, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);


        // Double checking if the consent data is synced
        Cursor cursorJoined = getApplicationContext().getContentResolver().query(Aware_Provider.Aware_Studies.CONTENT_URI,
                new String[]{Aware_Provider.Aware_Studies.STUDY_JOINED}, null, null, null);

        Cursor cursorConsent = getApplicationContext().getContentResolver().query(Provider.Consent_Data.CONTENT_URI,
                new String[]{Provider.Consent_Data.TIMESTAMP}, null, null, null);

        if (cursorJoined!=null && cursorJoined.getCount()>0 && cursorConsent!=null && cursorConsent.getCount()>0) {
            cursorJoined.moveToFirst();
            cursorConsent.moveToFirst();
            double joined = cursorJoined.getDouble(cursorJoined.getColumnIndexOrThrow("double_join"));
            double consent = cursorConsent.getDouble(cursorConsent.getColumnIndexOrThrow("timestamp"));

            if (consent<joined) {
                ContentValues values = new ContentValues();
                values.put(Provider.Consent_Data.TIMESTAMP, joined);
                getContentResolver().update(Provider.Consent_Data.CONTENT_URI, values,null, null);

                cursorJoined.close();
                cursorConsent.close();
            }
        }

        // Get an instance of the NotificationManager service
        manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

        // Tracking notification opening state
        Intent intent  = getIntent();
        String time = intent.getStringExtra(NOTIFICATION_TRIGGER_EVENT);
        if (time != null) {
            String event = time + NOTIFICATION_EVENT_OPENED;

            //Insert notification opened event to db
            ContentValues values = new ContentValues();
            values.put(Provider.Notification_Data.TIMESTAMP, System.currentTimeMillis());
            values.put(Provider.Notification_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            values.put(Provider.Notification_Data.EVENT, event);
            getContentResolver().insert(Provider.Notification_Data.CONTENT_URI, values);
        }

        // Schedule notifications to show four times per day
        scheduleNotification();

        // UI initialization
        mMainNav = findViewById(R.id.main_nav);

        medicationFragment = new MedicationFragment();
        gameFragment = new GameFragment();
        setFragment(gameFragment);

        mMainNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_game:
                        setFragment(gameFragment);
                        return true;
                    case R.id.nav_medication:
                        setFragment(medicationFragment);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Cancel GameNotification when MainActivity opens
        if (manager != null) manager.cancel(GAME_NOTIFICATION_ID);
    }

    // Replacing fragments method
    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Applying visibility to menu items accordingly to JoinStudy state (consent accepted = study joined; not = not)
        boolean consentAccepted = getSharedPreferences("consentPref", MODE_PRIVATE).getBoolean("consentAccepted", false);
        if (consentAccepted) {
            menu.findItem(R.id.main_demo).setVisible(false);
            menu.findItem(R.id.main_join_study).setVisible(false);
        } else {
            menu.findItem(R.id.main_quit_study).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // demo button, shown only if consent was declined; shows a dialog that offers to join the study
        if (id == R.id.main_demo) {

            // JoinStudy dialog: informs about the applications limited use and offers to join the study
            final AlertDialog demoDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.main_demo_mode)
                    .setMessage(R.string.main_demo_details)
                    .setPositiveButton(R.string.main_join_study, null)
                    .setNegativeButton(R.string.cancel, null)
                    .create();

            demoDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button join = demoDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    join.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            demoDialog.dismiss();

                            // erase user session state (consent acceptance) if the user agrees to join the study
                            SharedPreferences consent = MainActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = consent.edit();
                            editor.putBoolean("consentRead", false);
                            editor.commit();

                            // Open MainActivity
                            Intent consentIntent = new Intent(MainActivity.this, ConsentActivity.class);
                            startActivity(consentIntent);
                            finish();
                        }
                    });

                    Button cancel = demoDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            demoDialog.dismiss();
                        }
                    });

                }
            });
            demoDialog.show();

            return true;
        }

        if (id == R.id.main_health) {
            startActivity(new Intent(this, HealthActivity.class));
            return true;
        }

        if (id == R.id.main_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.main_feedback) {
            startActivity(new Intent(this, FeedbackActivity.class));
            return true;
        }

        // join study option, shown only if consent was declined; shows a dialog that offers to join the study
        if (id == R.id.main_join_study) {

            // JoinStudy dialog: informs about the applications limited use and offers to join the study
            final AlertDialog demoDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.main_demo_mode)
                    .setMessage(R.string.main_demo_details)
                    .setPositiveButton(R.string.main_join_study, null)
                    .setNegativeButton(R.string.cancel, null)
                    .create();

            demoDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button join = demoDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    join.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            demoDialog.dismiss();

                            // erase user session state (consent acceptance) if the user agrees to join the study
                            SharedPreferences consent = MainActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = consent.edit();
                            editor.putBoolean("consentRead", false);
                            editor.commit();

                            // Open MainActivity
                            Intent consentIntent = new Intent(MainActivity.this, ConsentActivity.class);
                            startActivity(consentIntent);
                            finish();
                        }
                    });

                    Button cancel = demoDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            demoDialog.dismiss();
                        }
                    });

                }
            });
            demoDialog.show();

            return true;
        }

        // quit study option, shown only if consent was accepted; shows a dialog that offers to cancel the study
        if (id == R.id.main_quit_study) {

            // QuitStudy dialog: informs about the applications limited use and offers to join the study
            final AlertDialog quitDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.main_quit_study)
                    .setMessage(R.string.main_quit_details)
                    .setPositiveButton(R.string.confirm, null)
                    .setNegativeButton(R.string.cancel, null)
                    .create();

            quitDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button confirm = quitDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    confirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            quitDialog.dismiss();

                            // erase user session state (consent acceptance) if the user agrees to join the study
                            SharedPreferences consent = MainActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = consent.edit();
                            editor.putBoolean("consentRead", false);
                            editor.putBoolean("consentAccepted", false);
                            editor.commit();

                            // logging that the user want to quit
                            Aware.debug(MainActivity.this, "USER HAS QUIT STUDY");

                            // quitting the study
                            Cursor dbStudy = Aware.getStudy(getApplicationContext(), Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER));
                            if (dbStudy != null && dbStudy.moveToFirst()) {
                                ContentValues complianceEntry = new ContentValues();
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_TIMESTAMP, System.currentTimeMillis());
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_KEY, dbStudy.getInt(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_KEY)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_API, dbStudy.getString(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_API)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_URL, dbStudy.getString(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_URL)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_PI, dbStudy.getString(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_PI)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, dbStudy.getString(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_CONFIG)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_JOINED, dbStudy.getLong(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_JOINED)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_EXIT, System.currentTimeMillis());
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_TITLE, dbStudy.getString(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_TITLE)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION, dbStudy.getString(dbStudy.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION)));
                                complianceEntry.put(Aware_Provider.Aware_Studies.STUDY_COMPLIANCE, "quit study");

                                getContentResolver().insert(Aware_Provider.Aware_Studies.CONTENT_URI, complianceEntry);
                            }
                            if (dbStudy != null && !dbStudy.isClosed()) dbStudy.close();

                            // syncing the data before the quitting
                            sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));

                            // Close the app
                            Toast.makeText(getApplicationContext(), R.string.main_quit_done, Toast.LENGTH_LONG).show();
                            MainActivity.this.finishAffinity();
                        }
                    });

                    Button cancel = quitDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            quitDialog.dismiss();
                        }
                    });

                }
            });
            quitDialog.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Notification scheduler: four times per day
    private void scheduleNotification() {

        String GAME_SCHEDULE_MORNING = getString(R.string.notification_game_morning);
        String GAME_SCHEDULE_NOON = getString(R.string.notification_game_noon);
        String GAME_SCHEDULE_AFTERNOON = getString(R.string.notification_game_afternoon);
        String GAME_SCHEDULE_EVENING = getString(R.string.notification_game_evening);
        String SURVEY_SCHEDULE = getString(R.string.notification_survey);

        try {
            // Morning notification 8:00 - 11:59
            Scheduler.Schedule morning = Scheduler.getSchedule(this, GAME_SCHEDULE_MORNING);
            if (morning == null) {
                morning = new Scheduler.Schedule(GAME_SCHEDULE_MORNING);
                morning.addHour(8).addHour(11)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra(NOTIFICATION_TRIGGER_EVENT, GAME_SCHEDULE_MORNING);

                Scheduler.saveSchedule(getApplicationContext(), morning);
                Aware.startScheduler(getApplicationContext());
            }

            // Noon notification 12:00 - 14:59
            Scheduler.Schedule noon = Scheduler.getSchedule(this, GAME_SCHEDULE_NOON);
            if (noon == null) {
                noon = new Scheduler.Schedule(GAME_SCHEDULE_NOON);
                noon.addHour(12).addHour(14)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra(NOTIFICATION_TRIGGER_EVENT, GAME_SCHEDULE_NOON);

                Scheduler.saveSchedule(getApplicationContext(), noon);
                Aware.startScheduler(getApplicationContext());
            }

            // Afternoon notification 15:00 - 18:59
            Scheduler.Schedule afternoon = Scheduler.getSchedule(this, GAME_SCHEDULE_AFTERNOON);
            if (afternoon == null) {
                afternoon = new Scheduler.Schedule(GAME_SCHEDULE_AFTERNOON);
                afternoon.addHour(15).addHour(18)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra(NOTIFICATION_TRIGGER_EVENT, GAME_SCHEDULE_AFTERNOON);

                Scheduler.saveSchedule(getApplicationContext(), afternoon);
                Aware.startScheduler(getApplicationContext());
            }

            // Evening notification 19:00 - 21:59
            Scheduler.Schedule evening = Scheduler.getSchedule(this, GAME_SCHEDULE_EVENING);
            if (evening == null) {
                evening = new Scheduler.Schedule(GAME_SCHEDULE_EVENING);
                evening.addHour(19).addHour(21)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra(NOTIFICATION_TRIGGER_EVENT, GAME_SCHEDULE_EVENING);

                Scheduler.saveSchedule(getApplicationContext(), evening);
                Aware.startScheduler(getApplicationContext());
            }

            // Survey notification 10:00 - 11:00
            Scheduler.Schedule survey = Scheduler.getSchedule(this, SURVEY_SCHEDULE);
            if (survey == null) {
                survey = new Scheduler.Schedule(SURVEY_SCHEDULE);
                survey.addHour(10)
                        .random(1, 0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_SURVEY)
                        .addActionExtra(NOTIFICATION_TRIGGER_EVENT, SURVEY_SCHEDULE);

                Scheduler.saveSchedule(getApplicationContext(), survey);
                Aware.startScheduler(getApplicationContext());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Notification set up
    private static void notifyShow(Context c, int notifyId, String notifyText, String triggerEvent) {

        // Attach activity for notification onClick
        Intent intent = null;
        if (notifyId == GAME_NOTIFICATION_ID) intent = new Intent(c, MainActivity.class);
        if (notifyId == SURVEY_NOTIFICATION_ID) intent = new Intent(c, HealthActivity.class);
        intent.putExtra(NOTIFICATION_TRIGGER_EVENT, triggerEvent);
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build Notification
        Notification.Builder builder = new Notification.Builder(c)
                .setOngoing(true)
                .setContentTitle(c.getString(R.string.app_name))
                .setContentText(notifyText)
                .setContentIntent(contentIntent);

        // Set icon depending on notification type
        if (notifyId == GAME_NOTIFICATION_ID) builder.setSmallIcon(R.drawable.ic_medication);
        if (notifyId == SURVEY_NOTIFICATION_ID) builder.setSmallIcon(R.drawable.ic_survey);

        // Build the notification and show it.
        if (manager == null) manager = (NotificationManager) c.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        manager.notify(notifyId, builder.build());

        // Vibrate at notification
        Vibrator vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(500);
        }
    }

    // BroadcastReceiver to trigger notifications
    public static class ParkinsonSnapshot extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(MainActivity.ACTION_STOP_FINGERPRINT)) {
                // Randomize game sensitivity in 1-5 range every time when notification is shown
                Random r = new Random();
                int sensitivity = r.nextInt(6 - 1) + 1;
                SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                SharedPreferences.Editor editor = sPref.edit();
                editor.putString(context.getString(R.string.key_sensitivity), String.valueOf(sensitivity));
                editor.commit();

                // Show game notification
                notifyShow(context, GAME_NOTIFICATION_ID,
                        intent.getStringExtra(NOTIFICATION_TRIGGER_EVENT) + context.getString(R.string.notification_game_text),
                        intent.getStringExtra(NOTIFICATION_TRIGGER_EVENT).toLowerCase());
            }

            if (intent.getAction().equalsIgnoreCase(MainActivity.ACTION_STOP_SURVEY)) {
                // Show survey notification
                notifyShow(context, SURVEY_NOTIFICATION_ID,
                        intent.getStringExtra(NOTIFICATION_TRIGGER_EVENT) + context.getString(R.string.notification_survey_text),
                        intent.getStringExtra(NOTIFICATION_TRIGGER_EVENT).toLowerCase());
            }

            // Tracking notification shown state
            // Insert notification shown event to db
            String event = intent.getStringExtra(NOTIFICATION_TRIGGER_EVENT).toLowerCase().replace(" ", "_") + NOTIFICATION_EVENT_SHOWN;
            ContentValues values = new ContentValues();
            values.put(Provider.Notification_Data.TIMESTAMP, System.currentTimeMillis());
            values.put(Provider.Notification_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
            values.put(Provider.Notification_Data.EVENT, event);
            context.getContentResolver().insert(Provider.Notification_Data.CONTENT_URI, values);
        }
    }
}
