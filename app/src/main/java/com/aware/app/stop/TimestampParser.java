package com.aware.app.stop;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

// Class to convert voice recognized string to timestamp GMT+0 format
public class TimestampParser extends AsyncTask<String, String, Long> {

    private ProgressDialog dialog;

    // Wit.ai client access token
    private final static String ACCESS_TOKEN = "TJCEIBT7526M4F2TXIMK3U5GQJBCTISX";

    // Parser constructor
    TimestampParser(Context context) {
        dialog = new ProgressDialog(context);
        dialog.setMessage("Please wait...");
    }

    protected Long doInBackground(String... text) {

        long timestamp = 0;

        try {
            // opening http connection to send string to wit.ai
            String timezone = "&context={\"timezone\":\"" + TimeZone.getDefault().getID() + "\"}";
            String getUrl = String.format("%s%s", "https://api.wit.ai/message?q=", URLEncoder.encode(text[0], "utf-8")) + timezone;
            URL url = new URL(getUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Authorization", String.format("Bearer %s", ACCESS_TOKEN));

            try {
                // reading answer from server
                InputStream is = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine())!=null) {
                    sb.append(line);
                }
                String response = sb.toString();
                br.close();
                is.close();

                // converting response to timestamp:
                // Response string -> JSONObject of time value -> String -> Date -> long timestamp
                JSONObject object = new JSONObject(response).getJSONObject("entities")
                        .getJSONArray("datetime").getJSONObject(0);

                String value = object.getString("value");
                value = value.substring(0, value.length()-6);
                value = value.substring(0, 10) + " " + value.substring(11);

                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                df.setTimeZone(TimeZone.getDefault());
                Date date = df.parse(value);
                timestamp = date.getTime();

            } finally {
                urlConnection.disconnect();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.show();
    }

    protected void onPostExecute(Long result) {
        dialog.dismiss();
    }

}
