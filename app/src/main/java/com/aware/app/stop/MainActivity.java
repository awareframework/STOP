package com.aware.app.stop;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.aware.Aware;
import com.aware.plugin.myo.ContextCard;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;
    private DashboardFragment dashboardFragment;
    private WearFragment wearFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent aware = new Intent(getApplicationContext(), Aware.class);
        startService(aware);
        Aware.startPlugin(this, "com.aware.plugin.myo");

        ContextCard card = new ContextCard();
        //setContentView(card.getContextCard(getApplicationContext()));

        View view = card.getContextCard(getApplicationContext());

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
