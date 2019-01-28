package com.aware.app.stop;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrScannerActivity extends Activity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                      // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        Log.d(MainActivity.STOP_TAG, "Code: " + rawResult.getText()); // Prints scan results
        Log.v(MainActivity.STOP_TAG, rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        if (rawResult.getBarcodeFormat() == BarcodeFormat.QR_CODE) {
            Toast.makeText(getApplicationContext(), "Code found: " + rawResult.getText(), Toast.LENGTH_SHORT).show();
            this.finish();
        } else {
            Toast.makeText(getApplicationContext(), "Wrong barcode format", Toast.LENGTH_SHORT).show();
            this.finish();
        }

    }
}