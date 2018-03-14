package com.aware.app.stop;

import android.os.AsyncTask;
import android.util.Log;

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

// Class to convert voice recognized string to timestamp format
public class TimestampParser extends AsyncTask<String, String, Long> {

    // Wit.ai client access token
    private final static String ACCESS_TOKEN = "TJCEIBT7526M4F2TXIMK3U5GQJBCTISX";

    protected Long doInBackground(String... text) {

        long timestamp = 0;

        try {
            // opening http connection to send string to wit.ai
            String getUrl = String.format("%s%s", "https://api.wit.ai/message?q=", URLEncoder.encode(text[0], "utf-8"));
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
                Date date = df.parse(value);
                timestamp = date.getTime()/1000;

            } finally {
                urlConnection.disconnect();
            }

        } catch (Exception e) {
            Log.d(MainActivity.MYO_TAG, String.valueOf(e));
        }

        return timestamp;
    }

    protected void onPostExecute(Long result) {
    }

}
