package com.aware.app.stop;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;

import java.util.ArrayList;
import java.util.List;


public class ConsentActivity extends AppCompatActivity {

    // UI elements
    private CheckBox checkBox;
    private Spinner spinnerWhen;
    private Button consentSubmit;
    private NonScrollListView symptomsList;
    private RelativeLayout detailsPD;
    private EditText etAge, etWhen, etMedication;
    private AlertDialog consentDialog;

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

        consentSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String age = etAge.getText().toString();

                if (checkBox.isChecked()) {

                    // retrieve data from entries
                    String whenSpinner = spinnerWhen.getSelectedItem().toString();
                    String whenPD = etWhen.getText().toString();
                    String medications = etMedication.getText().toString();
                    String when = whenPD + " " + whenSpinner;

                    // retrieve symptoms from the list
                    StringBuilder sb = new StringBuilder();
                    for (int i=0; i<symptomAdapter.getCount(); i++) {
                        CheckedTextView checkedTextView = symptomsList.getChildAt(i).findViewById(R.id.symptomName);
                        if (checkedTextView.isChecked()) sb.append(symptomAdapter.getItem(i).getName()).append(", ");
                    }
                    String symptoms = sb.toString();
                    if (symptoms.length()!=0) symptoms = symptoms.substring(0, symptoms.length()-2);

                    // insert data to db
                    if (!age.equals("") && !whenPD.equals("") && !medications.equals("")) {

                        ContentValues values = new ContentValues();
                        values.put(Provider.Consent_Data.TIMESTAMP, System.currentTimeMillis());
                        values.put(Provider.Consent_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        values.put(Provider.Consent_Data.AGE, age);
                        values.put(Provider.Consent_Data.PD_DIAGNOSED, "diagnosed-pd");
                        values.put(Provider.Consent_Data.PD_DIAGNOSED_DATE, when);
                        values.put(Provider.Consent_Data.PD_SYMPTOMS, symptoms);
                        values.put(Provider.Consent_Data.PD_MEDICATIONS, medications);
                        getContentResolver().insert(Provider.Consent_Data.CONTENT_URI, values);

                        // save consent state as true
                        SharedPreferences consent = ConsentActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor = consent.edit();
                        editor.putBoolean("consent", true);
                        editor.commit();

                        // Open MainActivity when all conditions are ok
                        Intent main = new Intent(ConsentActivity.this, MainActivity.class);
                        startActivity(main);
                        finish();

                    } else {
                        Toast.makeText(getApplicationContext(), R.string.consent_empty_entries, Toast.LENGTH_SHORT).show();
                    }

                } else {

                    if (!age.equals("")) {
                        ContentValues values = new ContentValues();
                        values.put(Provider.Consent_Data.TIMESTAMP, System.currentTimeMillis());
                        values.put(Provider.Consent_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        values.put(Provider.Consent_Data.AGE, age);
                        values.put(Provider.Consent_Data.PD_DIAGNOSED, "non-pd");
                        values.put(Provider.Consent_Data.PD_DIAGNOSED_DATE, "null");
                        values.put(Provider.Consent_Data.PD_SYMPTOMS, "null");
                        values.put(Provider.Consent_Data.PD_MEDICATIONS, "null");
                        getContentResolver().insert(Provider.Consent_Data.CONTENT_URI, values);

                        // save consent state as true
                        SharedPreferences consent = ConsentActivity.this.getSharedPreferences("consentPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor = consent.edit();
                        editor.putBoolean("consent", true);
                        editor.commit();

                        // Open MainActivity when all conditions are ok
                        Intent main = new Intent(ConsentActivity.this, MainActivity.class);
                        startActivity(main);
                        finish();

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

}
