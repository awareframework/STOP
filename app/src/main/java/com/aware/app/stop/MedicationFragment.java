package com.aware.app.stop;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class MedicationFragment extends Fragment{

    // UI components
    private Button nowBtn, specifyBtn;
    private ImageButton micBtn;
    private ListView medicationList;
    private TextView noRecords;

    // Cursor to access the internal database
    Cursor cursor;

    private final static int RC_SPEECH_INPUT = 1;

    public MedicationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_medication, container, false);

        medicationList = view.findViewById(R.id.medicationList);
        noRecords = view.findViewById(R.id.noRecords);
        medicationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                modifyRecord(id);
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
                Log.d(MainActivity.STOP_TAG, "Now timestamp inserted");

                updateList();
            }
        });

        // Mic button listens for users voice
        micBtn = view.findViewById(R.id.micBtn);
        micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm.getActiveNetworkInfo() != null) {
                    listenVoice();

                } else {
                    Toast.makeText(getActivity(), "Internet connection is disabled. Please enable it or use \"Specify time\" option ",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Specify button allows user to select date via TimePicker and DatePicker (when internet is disabled)
        specifyBtn = view.findViewById(R.id.specifyBtn);
        specifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Specifying date manually
                TimePickerDialog timeSpecify = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, final int hourPicked, final int minutePicked) {

                        DatePickerDialog dateSpecify = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {

                                Calendar specified = Calendar.getInstance();
                                specified.set(yearPicked, monthPicked, dayPicked, hourPicked, minutePicked);

                                // Date is specified, write it to db
                                ContentValues values = new ContentValues();
                                values.put(Provider.Medication_Data.TIMESTAMP, specified.getTimeInMillis());
                                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);
                                Toast.makeText(getActivity(), "Medication recorded", Toast.LENGTH_SHORT).show();
                                Log.d(MainActivity.STOP_TAG, "Specified: " + String.valueOf(specified.getTimeInMillis()));

                                updateList();

                            }
                        }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DATE));
                        dateSpecify.setTitle("Medication date");
                        dateSpecify.setCancelable(false);
                        dateSpecify.show();

                    }
                }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true);
                timeSpecify.setTitle("Medication time");
                timeSpecify.setCancelable(false);
                timeSpecify.show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Log.d(MainActivity.STOP_TAG, "Voice: " + results.get(0));

                // parsing timestamp from users response
                TimestampParser tp = new TimestampParser();
                try {
                    Log.d(MainActivity.STOP_TAG, "Parse started");
                    verifyTime(tp.execute(results.get(0)).get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Log.d(MainActivity.STOP_TAG, "UI:Parser error: " + e.getMessage());
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
        listenToUser.putExtra(RecognizerIntent.EXTRA_PROMPT, "When have you taken medication last time?");
        startActivityForResult(listenToUser, RC_SPEECH_INPUT);
    }

    // offer user to verify date received from voice
    private void verifyTime(final long time) {

        if (time != 0) {

            final Date date = new Date(time);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            Log.d(MainActivity.STOP_TAG, "Parsed: " + String.valueOf(time));

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
                            Log.d(MainActivity.STOP_TAG, "Confirmed: " + time);

                            updateList();

                        }

                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                            dialog.dismiss();
                            Log.d(MainActivity.STOP_TAG, "Cancelled: " + time);
                        }

                    })
                    .setNeutralButton("Edit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // Date is wrong, editing it manually
                            TimePickerDialog timeEdit = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, final int hourPicked, final int minutePicked) {

                                    DatePickerDialog dateEdit = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {

                                            calendar.set(yearPicked, monthPicked, dayPicked, hourPicked, minutePicked);

                                            // Date is fixed, write it to db
                                            ContentValues values = new ContentValues();
                                            values.put(Provider.Medication_Data.TIMESTAMP, calendar.getTimeInMillis());
                                            values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getActivity().getApplicationContext(), Aware_Preferences.DEVICE_ID));
                                            getActivity().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);
                                            Toast.makeText(getActivity(), "Medication recorded", Toast.LENGTH_SHORT).show();
                                            Log.d(MainActivity.STOP_TAG, "Updated: " + String.valueOf(calendar.getTimeInMillis()));

                                            updateList();

                                        }
                                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
                                    dateEdit.setTitle("Check date");
                                    dateEdit.setCancelable(false);
                                    dateEdit.show();

                                }
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                            timeEdit.setTitle("Check time");
                            timeEdit.setCancelable(false);
                            timeEdit.show();
                        }
                    })
                    .setIcon(R.drawable.ic_medication_light)
                    .setCancelable(false)
                    .show();

        } else {
            Toast.makeText(getActivity(), "Cannot recognize date, please try again", Toast.LENGTH_SHORT).show();
        }
    }

    // Update ListView with the new cursor query
    private void updateList() {

        medicationList.setAdapter(null);

        String[] columns = new String[]{Provider.Medication_Data._ID, Provider.Medication_Data.TIMESTAMP};
        String order = Provider.Medication_Data.TIMESTAMP + " DESC";
        cursor = getContext().getContentResolver().query(Provider.Medication_Data.CONTENT_URI, columns, null, null, order);

        if (cursor != null) {
            if (cursor.getCount() >0) {
                if (noRecords.getVisibility() == View.VISIBLE) noRecords.setVisibility(View.INVISIBLE);

                MedicationCursorAdapter mcs = new MedicationCursorAdapter(getContext(), cursor, 0);
                medicationList.setAdapter(mcs);

            } else {
                noRecords.setVisibility(View.VISIBLE);
            }
        }
    }

    // Modifying timestamp on item click
    private void modifyRecord(final long id) {

        // query to db to retrieve timestamp for selected id
        String[] columns = new String[]{Provider.Medication_Data._ID, Provider.Medication_Data.TIMESTAMP};
        String selection = Provider.Medication_Data._ID + " = " + id;
        Cursor modify = getContext().getContentResolver().query(Provider.Medication_Data.CONTENT_URI, columns, selection , null, null);
        modify.moveToFirst();

        final long time = modify.getLong(modify.getColumnIndexOrThrow("timestamp"));
        final Date date = new Date(time);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Log.d(MainActivity.STOP_TAG, "onClicked: " + String.valueOf(time));

        // transforming timestamp to readable date format to show in dialog
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        String formattedDate = sdf.format(date);

        // AlertDialog to modify/delete the date
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }
        builder.setTitle("Modify medication record")
                .setMessage(formattedDate)
                .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // Modify timestamp
                        TimePickerDialog timeEdit = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, final int hourPicked, final int minutePicked) {

                                DatePickerDialog dateEdit = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {

                                        calendar.set(yearPicked, monthPicked, dayPicked, hourPicked, minutePicked);

                                        // Date is fixed, write it to db
                                        ContentValues update_values = new ContentValues();
                                        update_values.put(Provider.Medication_Data.TIMESTAMP, calendar.getTimeInMillis());
                                        getActivity().getContentResolver().update(Provider.Medication_Data.CONTENT_URI, update_values, Provider.Medication_Data._ID + "=" + id, null);
                                        Toast.makeText(getActivity(), "Medication edited", Toast.LENGTH_SHORT).show();
                                        Log.d(MainActivity.STOP_TAG, "Edited: " + String.valueOf(calendar.getTimeInMillis()));

                                        updateList();

                                    }
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
                                dateEdit.setTitle("Edit date");
                                dateEdit.show();

                            }
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                        timeEdit.setTitle("Edit time");
                        timeEdit.show();
                    }

                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        dialog.dismiss();
                        Log.d(MainActivity.STOP_TAG, "Cancelled: " + time);
                    }

                })
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Remove timestamp from db
                        getActivity().getContentResolver().delete(Provider.Medication_Data.CONTENT_URI, Provider.Medication_Data._ID + "=" + id, null);

                        Toast.makeText(getActivity(), "Medication deleted", Toast.LENGTH_SHORT).show();
                        Log.d(MainActivity.STOP_TAG, "Deleted: " + String.valueOf(time));

                        updateList();

                    }
                })
                .setIcon(R.drawable.ic_medication_light)
                .show();

        modify.close();
    }

    // Custom cursor adapter
    private class MedicationCursorAdapter extends CursorAdapter {

        int total = -1;

        private MedicationCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            total = c.getCount();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.medication_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            TextView tvTimestamp = view.findViewById(R.id.timestamp);

            long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            Date date = new Date(timestamp);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // transforming timestamp to readable date format to show in dialog
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm,  dd MMMM yyyy");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            String formattedDate = sdf.format(date);

            tvTimestamp.setText(formattedDate);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = super.getView(position, convertView, parent);
            TextView number = view.findViewById(R.id.number);

            String order = String.valueOf(total - position) + ")";
            number.setText(order);

            return view;
        }
    }

}
