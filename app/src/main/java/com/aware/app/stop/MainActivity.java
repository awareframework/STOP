package com.aware.app.stop;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncRequest;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop.database.Provider;
import com.aware.plugin.myo.ContextCard;

import static com.aware.app.stop.database.Provider.AUTHORITY;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;
    private DashboardFragment dashboardFragment;
    private WearFragment wearFragment;
    private ProfileFragment profileFragment;

    public static final String MYO_TAG = "MYO_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Aware.startPlugin(getApplicationContext(), "com.aware.plugin.myo");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Aware.ACTION_AWARE_SYNC_DATA);
        registerReceiver(contextBroadcaster, filter);


        mMainFrame = findViewById(R.id.main_frame);
        mMainNav = findViewById(R.id.main_nav);
        mMainNav.setSelectedItemId(R.id.nav_wear);

        dashboardFragment = new DashboardFragment();
        wearFragment = new WearFragment();
        profileFragment = new ProfileFragment();

        setFragment(wearFragment);

        mMainNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_dashboard:
                        setFragment(dashboardFragment);
                        return true;
                    case R.id.nav_wear:
                        setFragment(wearFragment);
                        return true;
                    case R.id.nav_profile:
                        setFragment(profileFragment);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (contextBroadcaster != null) unregisterReceiver(contextBroadcaster);
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    public class ContextBroadcaster extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Aware.ACTION_AWARE_SYNC_DATA) && AUTHORITY.length() > 0) {
                Log.d(MainActivity.MYO_TAG, "Context broadcater");
                Bundle sync = new Bundle();
                sync.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                sync.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(Aware.getAWAREAccount(context), AUTHORITY, sync);
            }
        }
    }

    private ContextBroadcaster contextBroadcaster = new ContextBroadcaster();

}
