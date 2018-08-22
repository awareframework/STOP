package com.aware.app.stop.database;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.R;
import com.aware.utils.Http;
import com.aware.utils.Https;
import com.aware.utils.SSLManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Hashtable;

public class Manual_Sync extends AsyncTask{

    private Context mContext;
    private ProgressDialog dialog;
    private String DEVICE_ID;
    private String WEBSERVER;
    private String protocol;
    private Boolean DEBUG;
    private Boolean syncSuccess = true;
    private String[] DATABASE_TABLES = Provider.DATABASE_TABLES;
    private Uri[] DATABASE_URIS = new Uri[]{
            Provider.Game_Data.CONTENT_URI,
            Provider.Medication_Data.CONTENT_URI,
            Provider.Feedback_Data.CONTENT_URI,
            Provider.Notification_Data.CONTENT_URI,
            Provider.Health_Data.CONTENT_URI,
            Provider.Consent_Data.CONTENT_URI // TODO: No need to sync - remove
    };

    public Manual_Sync(Context context) {
        mContext = context;
        DEVICE_ID = Aware.getSetting(context, Aware_Preferences.DEVICE_ID);
        WEBSERVER = Aware.getSetting(context, Aware_Preferences.WEBSERVICE_SERVER);
        protocol = WEBSERVER.substring(0, WEBSERVER.indexOf(":"));;
        DEBUG = Aware.getSetting(context, Aware_Preferences.DEBUG_FLAG).equals("true");
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        if (DATABASE_TABLES != null && DATABASE_URIS != null) {
            for (int i = 0; i < DATABASE_TABLES.length; i++) {

                String DATABASE_TABLE = Provider.DATABASE_TABLES[i];
                Cursor context_data = mContext.getContentResolver().query(DATABASE_URIS[i], null, null, null, null);

                JSONArray rows = new JSONArray();
                long lastSynced = 0;
                if (context_data != null && context_data.moveToFirst()) {

                    try {
                        do {
                            JSONObject row = new JSONObject();
                            String[] columns = context_data.getColumnNames();
                            for (String c_name : columns) {
                                if (c_name.equals("_id")) continue; //Skip local database ID
                                if (c_name.equals("timestamp") || c_name.contains("double")) {
                                    row.put(c_name, context_data.getDouble(context_data.getColumnIndex(c_name)));
                                } else if (c_name.contains("float")) {
                                    row.put(c_name, context_data.getFloat(context_data.getColumnIndex(c_name)));
                                } else if (c_name.contains("long")) {
                                    row.put(c_name, context_data.getLong(context_data.getColumnIndex(c_name)));
                                } else if (c_name.contains("blob")) {
                                    row.put(c_name, context_data.getBlob(context_data.getColumnIndex(c_name)));
                                } else if (c_name.contains("integer")) {
                                    row.put(c_name, context_data.getInt(context_data.getColumnIndex(c_name)));
                                } else {
                                    String str = "";
                                    if (!context_data.isNull(context_data.getColumnIndex(c_name))) { //fixes nulls and batch inserts not being possible
                                        str = context_data.getString(context_data.getColumnIndex(c_name));
                                    }
                                    row.put(c_name, str);
                                }
                            }
                            rows.put(row);
                        } while (context_data.moveToNext());

                        context_data.close(); //clear phone's memory immediately

                        lastSynced = rows.getJSONObject(rows.length() - 1).getLong("timestamp"); //last record to be synced

//            // For some tables, we must not clear everything.  Leave one row of these tables.
//            if (dontClearSensors.contains(DATABASE_TABLE)) {
//                if (rows.length() >= 2) {
//                    lastSynced = rows.getJSONObject(rows.length() - 2).getLong("timestamp"); //last record to be synced
//                } else {
//                    lastSynced = 0;
//                }
//            }

                        Hashtable<String, String> request = new Hashtable<>();
                        request.put(Aware_Preferences.DEVICE_ID, DEVICE_ID);
                        request.put("data", rows.toString());

                        String success;
                        if (protocol.equals("https")) {
                            try {
                                success = new Https(SSLManager.getHTTPS(mContext, WEBSERVER)).dataPOST(WEBSERVER + "/" + DATABASE_TABLE + "/insert", request, true);
                            } catch (FileNotFoundException e) {
                                success = null;
                            }
                        } else {
                            success = new Http().dataPOST(WEBSERVER + "/" + DATABASE_TABLE + "/insert", request, true);
                        }

                        //Something went wrong, e.g., server is down, lost internet, etc.
                        if (success == null) {
                            if (DEBUG) Log.d(Aware.TAG, DATABASE_TABLE + " FAILED to sync. Server down?");
                            Log.d("STOP_TAG", "ERROR");
                            syncSuccess = false;
                            //return null;
                        } else {

                            try {
                                Aware.debug(mContext, new JSONObject()
                                        .put("table", DATABASE_TABLE)
                                        .put("last_sync_timestamp", lastSynced)
                                        .toString());
                                Log.d("STOP_TAG", "Insert done");

                            } catch (JSONException e) {
                                e.printStackTrace();
                                syncSuccess = false;
                            }

                            if (DEBUG)
                                Log.d(Aware.TAG, "Sync OK into " + DATABASE_TABLE + " [ " + rows.length() + " rows ]");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //return lastSynced;
        return null;
    }

    @Override
    protected void onPreExecute() {
        dialog = ProgressDialog.show(mContext, mContext.getString(R.string.manual_dialog_syncing),
                mContext.getString(R.string.manual_dialog_please_wait),true,false);
    }

    @Override
    protected void onPostExecute(Object o) {
        dialog.dismiss();
        if (syncSuccess) {
            Toast.makeText(mContext, R.string.manual_dialog_sync_done, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, R.string.manual_dialog_sync_error, Toast.LENGTH_LONG).show();
        }
    }
}
