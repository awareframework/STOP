package com.aware.app.stop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

//import com.plumillonforge.android.chipview.Chip;
//import com.plumillonforge.android.chipview.ChipView;

import com.github.florent37.viewtooltip.ViewTooltip;

import java.util.ArrayList;
import java.util.List;


public class ConsentActivity extends AppCompatActivity {

    // UI elements
    private CheckBox checkBox;
    private Spinner spinnerWhen;
    private Button consentSubmit;
    private NonScrollListView symptomsList;
    private RelativeLayout detailsPD;

    private SymptomAdapter symptomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);

        // Consent form dialog. Has to be accepted for further app use
        final AlertDialog consentDialog = new AlertDialog.Builder(this)
                .setTitle("App consent form")
                .setMessage("1text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n text \n ")
                .setPositiveButton("Accept", null)
                .setNegativeButton("Decline", null)
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
                        Toast.makeText(getApplicationContext(), "You cannot use the application until you accept the consent form", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        //consentDialog.show();

        detailsPD = findViewById(R.id.detailsPD);

        // PD
        checkBox = findViewById(R.id.checkboxPD);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkBox.setText("Yes");
                    detailsPD.setVisibility(View.VISIBLE);
                } else {
                    checkBox.setText("No");
                    detailsPD.setVisibility(View.GONE);
                }
            }
        });

        spinnerWhen = findViewById(R.id.spinnerWhen);
        symptomsList = findViewById(R.id.symptomsList);



        // retrieve symptoms data from arrays.xml and parse it to Symptom.class array
        final String[] listSymptoms = getResources().getStringArray(R.array.listSymptoms);
        String[] listSymptomsDescription = getResources().getStringArray(R.array.listSymptomsDescription);
        final ArrayList<Symptom> symptomList = new ArrayList<>();
        for (int i=0; i<listSymptoms.length; i++) {
            symptomList.add(new Symptom(listSymptoms[i], listSymptomsDescription[i], false));
        }

        // initialize custom adapter and apply it to ListView
        symptomAdapter = new SymptomAdapter(this, symptomList);
        symptomsList.setAdapter(symptomAdapter);


        consentSubmit = findViewById(R.id.consentSubmit);
        consentSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                for (int i=0; i<symptomAdapter.getCount(); i++) {
                    CheckedTextView checkedTextView = symptomsList.getChildAt(i).findViewById(R.id.symptomName);
                    if (checkedTextView.isChecked()) sb.append(symptomAdapter.getItem(i).getName()).append(", ");
                }

                String result = sb.toString();
                result = result.substring(0, result.length()-2);
                Toast.makeText(ConsentActivity.this, result, Toast.LENGTH_LONG).show();
            }
        });
    }

    ArrayList<View> viewsList = new ArrayList<>();

    // custom ListView adapted for Symptoms
    private class SymptomAdapter extends ArrayAdapter<Symptom> {

        private Context mContext;
        private List<Symptom> symptomList;

        public SymptomAdapter(@NonNull Context context, ArrayList<Symptom> list) {
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

            final Symptom currentSymptom = symptomList.get(position);

            final CheckedTextView symptomText = listItem.findViewById(R.id.symptomName);
            symptomText.setText(currentSymptom.getName());

            symptomText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (symptomText.isChecked()) {
                        symptomText.setChecked(false);
                    } else {
                        symptomText.setChecked(true);
                    }
                }
            });

            // Show tooltip about the selected symptom
            final ImageView symptomInfo = listItem.findViewById(R.id.symptomInfo);

            final ViewTooltip viewTooltip = ViewTooltip.on((Activity) getContext(), symptomInfo)
                    .autoHide(true, 10000)
                    .clickToHide(true)
                    .corner(30)
                    .color(Color.parseColor("#2196F3"))
                    .position(ViewTooltip.Position.LEFT)
                    .text(currentSymptom.getDescription())
                    .onDisplay(new ViewTooltip.ListenerDisplay() {
                        @Override
                        public void onDisplay(View view) {
                            currentSymptom.setDescriptionShown(true);
                            symptomInfo.setClickable(true);

                        }
                    })
                    .onHide(new ViewTooltip.ListenerHide() {
                        @Override
                        public void onHide(View view) {
                            currentSymptom.setDescriptionShown(false);
                            symptomInfo.setClickable(true);
                        }
                    });

            symptomInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    if (!currentSymptom.isDescriptionShown()) {
                        viewTooltip.show();
                        symptomInfo.setClickable(false);
                        //currentSymptom.setDescriptionShown(true);
                    } else {
                        viewTooltip.close();
                        symptomInfo.setClickable(false);
                        //currentSymptom.setDescriptionShown(false);
                    }
                }
            });

            return listItem;
        }
    }
}
