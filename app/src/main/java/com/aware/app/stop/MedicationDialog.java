package com.aware.app.stop;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MedicationDialog extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.view_dialog_medication, null);
        final TextInputEditText dialogMedication = view.findViewById(R.id.dialogMedication);
        final TextInputEditText dialogTimes = view.findViewById(R.id.dialogTimes);
        final TextInputEditText dialogPillsEachTime = view.findViewById(R.id.dialogPillsEachTime);
        final TextInputEditText dialogHowOften = view.findViewById(R.id.dialogHowOften);
        final TextInputEditText dialogFirstMedication = view.findViewById(R.id.dialogFirstMedication);
        final TextInputEditText dialogComments = view.findViewById(R.id.dialogComments);

        final AlertDialog medicationDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.consent_add_medication)
                .setView(view)
                .setPositiveButton(R.string.consent_button_add, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        medicationDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button add = medicationDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button cancel = medicationDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String medication = dialogMedication.getText().toString();
                        String times = dialogTimes.getText().toString();
                        String pillsEachTime = dialogPillsEachTime.getText().toString();
                        String howOften = dialogHowOften.getText().toString();
                        String firstMedication = dialogFirstMedication.getText().toString();
                        String comments = dialogComments.getText().toString();

                        if (!medication.equals("") && !times.equals("") && !pillsEachTime.equals("")
                                && !howOften.equals("") && !firstMedication.equals("")) {

                            JSONObject jsonMedication = new JSONObject();
                            try {
                                jsonMedication.put("medication", medication);
                                jsonMedication.put("times_per_day", times);
                                jsonMedication.put("pills_per_time", pillsEachTime);
                                jsonMedication.put("how_often", howOften);
                                jsonMedication.put("first_med_daily", firstMedication);
                                jsonMedication.put("comments", comments);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Activity activity = getActivity();
                            if (activity instanceof ConsentActivity)
                                ((ConsentActivity) activity).addMedication(jsonMedication, medication);

                            dialog.dismiss();

                        } else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    R.string.consent_fill_entries, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        medicationDialog.dismiss();
                    }
                });

            }
        });

        return medicationDialog;
    }
}
