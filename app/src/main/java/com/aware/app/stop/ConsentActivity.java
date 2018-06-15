package com.aware.app.stop;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class ConsentActivity extends AppCompatActivity {

    // UI elements
    private CheckBox checkBox;
    private Spinner spinnerWhen;
    private Button consentSubmit;
    private NonScrollListView symptomsList;
    private RelativeLayout detailsPD;
    private EditText etUsername, etAge, etWhen, etMedication;
    private AlertDialog consentDialog;
    private ProgressDialog progressDialog;

    private SymptomAdapter symptomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);

        detailsPD = findViewById(R.id.detailsPD);
        checkBox = findViewById(R.id.checkboxPD);
        spinnerWhen = findViewById(R.id.spinnerWhen);
        symptomsList = findViewById(R.id.symptomsList);
        consentSubmit = findViewById(R.id.consentSubmit);
        etUsername = findViewById(R.id.etUsername);
        etAge = findViewById(R.id.etAge);
        etWhen = findViewById(R.id.etWhen);
        etMedication = findViewById(R.id.etMedication);

        // Consent form dialog. Has to be accepted for further app use
        consentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.consent_app_consent)
                .setMessage(R.string.consent_app_consent_details)
                .setPositiveButton(R.string.consent_accept, null)
                .setNegativeButton(R.string.consent_decline, null)
                .setCancelable(false)
                .create();

        consentDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button accept = consentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        consentDialog.dismiss();
                    }
                });

                Button decline = consentDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                decline.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ConsentActivity.this.finishAffinity();
                        //Toast.makeText(getApplicationContext(), R.string.consent_cannot_use, Toast.LENGTH_LONG).show();
                    }
                });

            }
        });
        consentDialog.show();


        // "Do you have PD" checkbox, dismiss PD details entries for non-PD user
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkBox.setText(getString(R.string.consent_yes));
                    detailsPD.setVisibility(View.VISIBLE);
                } else {
                    checkBox.setText(R.string.consent_no);
                    detailsPD.setVisibility(View.GONE);
                }
            }
        });

        // retrieve symptoms data from arrays.xml and parse it to Symptom.class array
        TypedArray arrayOfArrays = getResources().obtainTypedArray(R.array.symptomsList);
        ArrayList<Symptom> symptomList = new ArrayList<>();

        for (int i=0; i<arrayOfArrays.length(); i++) {
            int resId = arrayOfArrays.getResourceId(i, -1);
            if (resId < 0) {
                continue;
            }

            String[] symptomArray = getResources().getStringArray(resId);
            symptomList.add(new Symptom(symptomArray[0], symptomArray[1],
                    symptomArray[2], symptomArray[3], symptomArray[4], symptomArray[5]));
        }

        //initialize custom adapter and apply it to ListView
        symptomAdapter = new SymptomAdapter(this, symptomList);
        symptomsList.setAdapter(symptomAdapter);

        // "Submit" button
        consentSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // retrieve data from entries
                String username = etUsername.getText().toString();
                String age = etAge.getText().toString();

                if (checkBox.isChecked()) {

                    // retrieve data from entries
                    String whenPdStr = etWhen.getText().toString();
                    String medications = etMedication.getText().toString();

                    // retrieve symptoms values from the list
                    boolean allChecked = true;
                    JSONObject symptoms = new JSONObject();
                    for (int i=0; i<symptomAdapter.getCount(); i++) {

                        String symptomName = symptomAdapter.getItem(i).getName().replaceAll(" ", "_").toLowerCase();

                        RadioGroup group = symptomsList.getChildAt(i).findViewById(R.id.symptomRate);
                        int radioButtonId = group.getCheckedRadioButtonId();
                        View radioButton = group.findViewById(radioButtonId);
                        int value = group.indexOfChild(radioButton);
                        if (value==-1) allChecked = false;

                        try {
                            symptoms.put(symptomName, value);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // insert data to db
                    if (!username.equals("") && !age.equals("") && !whenPdStr.equals("") && !medications.equals("") && allChecked) {

                        int whenPD = Integer.valueOf(whenPdStr);
                        long time = System.currentTimeMillis();
                        long monthMillis = 2952000000L;
                        long yearMillis = 31104000000L;
                        if (spinnerWhen.getSelectedItemPosition() == 0) {
                            time = time - monthMillis*whenPD;
                        } else {
                            time = time - yearMillis*whenPD;
                        }

                        // Transfer entries to JSON object
                        JSONObject userdata = new JSONObject();
                        try {
                            userdata.put("username", username);
                            userdata.put("age", Integer.valueOf(age));
                            userdata.put("diagnosed_pd", checkBox.isChecked());
                            userdata.put("diagnosed_time", time);
                            userdata.put("medications", medications);
                            userdata.put("symptoms", symptoms);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // insert
                        ContentValues values = new ContentValues();
                        values.put(Provider.Consent_Data.TIMESTAMP, System.currentTimeMillis());
                        values.put(Provider.Consent_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        values.put(Provider.Consent_Data.USER_DATA, userdata.toString());
                        getContentResolver().insert(Provider.Consent_Data.CONTENT_URI, values);

                        // save consent state as true
                        SharedPreferences consent = ConsentActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor = consent.edit();
                        editor.putBoolean("consent", true);
                        editor.commit();

                        // Join AWARE study if not joined yed
                        joinObserver = new JoinObserver();
                        Aware.joinStudy(ConsentActivity.this, SplashActivity.STUDY_URL);
                        IntentFilter joinFilter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
                        registerReceiver(joinObserver, joinFilter);

                        // Progress dialog while joining the study
                        progressDialog = new ProgressDialog(ConsentActivity.this);
                        progressDialog.setMessage(getString(R.string.consent_loading));
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.show();

                    } else {
                        Toast.makeText(getApplicationContext(), R.string.consent_empty_entries, Toast.LENGTH_SHORT).show();
                    }

                } else {

                    if (!username.equals("") && !age.equals("")) {

                        // Transfer entries to JSON object
                        JSONObject userdata = new JSONObject();
                        try {
                            userdata.put("username", username);
                            userdata.put("age", Integer.valueOf(age));
                            userdata.put("diagnosed_pd", checkBox.isChecked());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // insert
                        ContentValues values = new ContentValues();
                        values.put(Provider.Consent_Data.TIMESTAMP, System.currentTimeMillis());
                        values.put(Provider.Consent_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        values.put(Provider.Consent_Data.USER_DATA, userdata.toString());
                        getContentResolver().insert(Provider.Consent_Data.CONTENT_URI, values);

                        // save consent state as true
                        SharedPreferences consent = ConsentActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor = consent.edit();
                        editor.putBoolean("consent", true);
                        editor.commit();

                        // Join AWARE study if not joined yed
                        joinObserver = new JoinObserver();
                        Aware.joinStudy(ConsentActivity.this, SplashActivity.STUDY_URL);
                        IntentFilter joinFilter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
                        registerReceiver(joinObserver, joinFilter);

                        // Progress dialog while joining the study
                        progressDialog = new ProgressDialog(ConsentActivity.this);
                        progressDialog.setMessage(getString(R.string.consent_loading));
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.show();

                    } else {
                        Toast.makeText(getApplicationContext(), R.string.consent_specify_age, Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }

    // custom ListView adapted for Symptoms
    private class SymptomAdapter extends ArrayAdapter<Symptom> {

        private Context mContext;
        private List<Symptom> symptomList;

        private SymptomAdapter(@NonNull Context context, ArrayList<Symptom> list) {
            super(context,0, list);
            mContext = context;
            symptomList = list;
        }

        @Nullable
        @Override
        public Symptom getItem(int position) {
            return super.getItem(position);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.symptom_list_item, parent, false);
            }

            Symptom currentSymptom = symptomList.get(position);

            TextView symptomText = listItem.findViewById(R.id.symptomName);
            RadioButton rate0 = listItem.findViewById(R.id.rate0);
            RadioButton rate1 = listItem.findViewById(R.id.rate1);
            RadioButton rate2 = listItem.findViewById(R.id.rate2);
            RadioButton rate3 = listItem.findViewById(R.id.rate3);
            RadioButton rate4 = listItem.findViewById(R.id.rate4);

            symptomText.setText(currentSymptom.getName());
            rate0.setText(currentSymptom.getRate0());
            rate1.setText(currentSymptom.getRate1());
            rate2.setText(currentSymptom.getRate2());
            rate3.setText(currentSymptom.getRate3());
            rate4.setText(currentSymptom.getRate4());

            return listItem;
        }
    }


    // Reciever waiting for study joined state
    private JoinObserver joinObserver;
    private class JoinObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Aware.ACTION_JOINED_STUDY)) {

                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Provider.getAuthority(getApplicationContext());
                long frequency = Long.parseLong(Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;
                ContentResolver.setIsSyncable(aware_account, authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, authority, true);
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);

                // Open MainActivity when all conditions are ok
                Intent main = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(main);
                unregisterReceiver(joinObserver);
                progressDialog.dismiss();
                Toast.makeText(context, getString(R.string.consent_thank_you), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

}
