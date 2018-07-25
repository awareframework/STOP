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
                String timestamp = String.valueOf(System.currentTimeMillis());
                values.put(Provider.Medication_Data.TIMESTAMP, timestamp);
                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getContext(), Aware_Preferences.DEVICE_ID));
                values.put(Provider.Medication_Data.MEDICATION_TIMESTAMP, timestamp);
                getContext().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);
                Toast.makeText(getContext(), R.string.medication_recorded, Toast.LENGTH_SHORT).show();

                updateList();
            }
        });

        // Mic button listens for users voice
        micBtn = view.findViewById(R.id.micBtn);
        micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm.getActiveNetworkInfo() != null) {
                    listenVoice();

                } else {
                    Toast.makeText(getContext(), R.string.medication_no_internet, Toast.LENGTH_LONG).show();
                }
            }
        });
        // Disable recognition feature to Finnish language devices due to Wit.ai
        if (Locale.getDefault().getLanguage().equals("fi")) micBtn.setVisibility(View.INVISIBLE);

        // Specify button allows user to select date via TimePicker and DatePicker (when internet is disabled)
        specifyBtn = view.findViewById(R.id.specifyBtn);
        specifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Specifying date manually
                TimePickerDialog timeSpecify = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, final int hourPicked, final int minutePicked) {

                        DatePickerDialog dateSpecify = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {

                                Calendar specified = Calendar.getInstance();
                                specified.set(yearPicked, monthPicked, dayPicked, hourPicked, minutePicked);

                                // check if the entry is not in the future
                                if (specified.getTimeInMillis() <= System.currentTimeMillis()) {

                                    // Date is specified, write it to db
                                    ContentValues values = new ContentValues();
                                    values.put(Provider.Medication_Data.TIMESTAMP, System.currentTimeMillis());
                                    values.put(Provider.Medication_Data.MEDICATION_TIMESTAMP, specified.getTimeInMillis());
                                    values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getContext(), Aware_Preferences.DEVICE_ID));
                                    getContext().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);
                                    Toast.makeText(getContext(), R.string.medication_recorded, Toast.LENGTH_SHORT).show();

                                    updateList();

                                } else {
                                    Toast.makeText(getContext(), R.string.medication_future, Toast.LENGTH_SHORT).show();
                                }

                            }
                        }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DATE));
                        dateSpecify.setTitle(R.string.medication_specify_date);
                        dateSpecify.setCancelable(false);
                        dateSpecify.show();

                    }
                }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true);
                timeSpecify.setTitle(R.string.medication_specify_time);
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

                // parsing timestamp from users response
                TimestampParser tp = new TimestampParser(getContext());
                try {
                    verifyTime(tp.execute(results.get(0)).get());

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), R.string.medication_error + e.getMessage(), Toast.LENGTH_LONG).show();
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
        listenToUser.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_when_taken));
        startActivityForResult(listenToUser, RC_SPEECH_INPUT);
    }

    // offer user to verify date received from voice
    private void verifyTime(final long time) {

        if (time != 0) {

            final Date date = new Date(time);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // transforming timestamp to readable date format to show in dialog
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd.MM.yyyy");
            sdf.setTimeZone(TimeZone.getDefault());
            String formattedDate = sdf.format(date);

            // AlertDialog to verify the date
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getContext());
            }
            builder.setTitle(R.string.medication_check_title)
                    .setMessage(formattedDate)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // check if the entry is not in the future
                            if (time <= System.currentTimeMillis()) {

                                // Date is OK, write it to db
                                ContentValues values = new ContentValues();
                                values.put(Provider.Medication_Data.TIMESTAMP, System.currentTimeMillis());
                                values.put(Provider.Medication_Data.MEDICATION_TIMESTAMP, time);
                                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getContext(), Aware_Preferences.DEVICE_ID));
                                getContext().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);

                                Toast.makeText(getContext(), R.string.medication_recorded, Toast.LENGTH_SHORT).show();
                                updateList();

                            } else {
                                Toast.makeText(getContext(), R.string.medication_future, Toast.LENGTH_SHORT).show();
                            }

                        }

                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                            dialog.dismiss();
                        }

                    })
                    .setNeutralButton(R.string.edit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // Date is wrong, editing it manually
                            TimePickerDialog timeEdit = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, final int hourPicked, final int minutePicked) {

                                    DatePickerDialog dateEdit = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {

                                            calendar.set(yearPicked, monthPicked, dayPicked, hourPicked, minutePicked);

                                            // check if the entry is not in the future
                                            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {

                                                // Date is fixed, write it to db
                                                ContentValues values = new ContentValues();
                                                values.put(Provider.Medication_Data.TIMESTAMP, System.currentTimeMillis());
                                                values.put(Provider.Medication_Data.MEDICATION_TIMESTAMP, calendar.getTimeInMillis());
                                                values.put(Provider.Medication_Data.DEVICE_ID, Aware.getSetting(getContext(), Aware_Preferences.DEVICE_ID));
                                                getContext().getContentResolver().insert(Provider.Medication_Data.CONTENT_URI, values);

                                                Toast.makeText(getContext(), R.string.medication_recorded, Toast.LENGTH_SHORT).show();
                                                updateList();

                                            } else {
                                                Toast.makeText(getContext(), R.string.medication_future, Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
                                    dateEdit.setTitle(R.string.medication_check_date);
                                    dateEdit.setCancelable(false);
                                    dateEdit.show();

                                }
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                            timeEdit.setTitle(R.string.medication_check_time);
                            timeEdit.setCancelable(false);
                            timeEdit.show();
                        }
                    })
                    .setIcon(R.drawable.ic_medication_light)
                    .setCancelable(false)
                    .show();

        } else {
            Toast.makeText(getContext(), R.string.medication_cannot_recognize, Toast.LENGTH_SHORT).show();
        }
    }

    // Update ListView with the new cursor query
    private void updateList() {

        medicationList.setAdapter(null);

        String[] columns = new String[]{Provider.Medication_Data._ID, Provider.Medication_Data.MEDICATION_TIMESTAMP};
        String order = Provider.Medication_Data.MEDICATION_TIMESTAMP + " DESC";
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
        String[] columns = new String[]{Provider.Medication_Data._ID, Provider.Medication_Data.MEDICATION_TIMESTAMP};
        String selection = Provider.Medication_Data._ID + " = " + id;
        Cursor modify = getContext().getContentResolver().query(Provider.Medication_Data.CONTENT_URI, columns, selection , null, null);
        modify.moveToFirst();

        final long time = modify.getLong(modify.getColumnIndexOrThrow("double_medication"));
        final Date date = new Date(time);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // transforming timestamp to readable date format to show in dialog
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        String formattedDate = sdf.format(date);

        // AlertDialog to modify/delete the date
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }
        builder.setTitle(R.string.medication_modify_title)
                .setMessage(formattedDate)
                .setPositiveButton(R.string.edit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // Modify timestamp
                        TimePickerDialog timeEdit = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, final int hourPicked, final int minutePicked) {

                                DatePickerDialog dateEdit = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view, int yearPicked, int monthPicked, int dayPicked) {

                                        calendar.set(yearPicked, monthPicked, dayPicked, hourPicked, minutePicked);

                                        // check if the entry is not in the future
                                        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {

                                            // Date is fixed, write it to db
                                            ContentValues update_values = new ContentValues();
                                            update_values.put(Provider.Medication_Data.MEDICATION_TIMESTAMP, calendar.getTimeInMillis());
                                            getContext().getContentResolver().update(Provider.Medication_Data.CONTENT_URI, update_values, Provider.Medication_Data._ID + "=" + id, null);

                                            Toast.makeText(getContext(), R.string.medication_edited, Toast.LENGTH_SHORT).show();
                                            updateList();

                                        } else {
                                            Toast.makeText(getContext(), R.string.medication_future, Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
                                dateEdit.setTitle(R.string.medication_edit_date);
                                dateEdit.show();

                            }
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                        timeEdit.setTitle(R.string.medication_edit_time);
                        timeEdit.show();
                    }

                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        dialog.dismiss();
                    }

                })
                .setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Remove timestamp from db
                        getContext().getContentResolver().delete(Provider.Medication_Data.CONTENT_URI, Provider.Medication_Data._ID + "=" + id, null);
                        Toast.makeText(getContext(), R.string.medication_deleted, Toast.LENGTH_SHORT).show();
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
            return LayoutInflater.from(context).inflate(R.layout.view_list_item_journal_medication, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            TextView tvTimestamp = view.findViewById(R.id.timestamp);
            long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("double_medication"));
            Date date = new Date(timestamp);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // transforming timestamp to readable date format to show in dialog
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm,  dd MMMM yyyy");
            sdf.setTimeZone(TimeZone.getDefault());
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
