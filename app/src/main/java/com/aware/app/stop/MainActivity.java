package com.aware.app.stop;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.aware.Aware;

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

    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }
}
