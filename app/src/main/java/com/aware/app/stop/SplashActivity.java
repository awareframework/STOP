package com.aware.app.stop;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;

import com.aware.Aware;
import com.aware.ui.PermissionsHandler;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends AppCompatActivity {

    public static final String STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/1868/g5Jxp2f9IJwb";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // List of required permission
        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.INTERNET);
        REQUIRED_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_SETTINGS);

        // flag to check permissions
        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        // flag to check if the consent has been read by the user
        boolean consentRead = getSharedPreferences("consentPref", MODE_PRIVATE).getBoolean("consentRead", false);

        // 1st: Check for permissions
        if (permissions_ok) {

            Intent aware = new Intent(getApplicationContext(), Aware.class);
            startService(aware);

            // 2nd: Check for consent was read
            if (consentRead) {

                // Open MainActivity when all conditions are ok
                Intent main = new Intent(this, MainActivity.class);
                startActivity(main);
                finish();

            } else {

                // Open ConsentActivity if consent was not accepted yet
                Intent consentIntent = new Intent(this, ConsentActivity.class);
                startActivity(consentIntent);
                finish();
            }

        } else {

            // Request permissions if not granted yet
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }
    }
}
