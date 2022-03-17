package com.sap.appodatav4.mdui;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.sap.appodatav4.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SettingsFragment settingFragment = new SettingsFragment();
        settingFragment.setRetainInstance(true);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, settingFragment).commit();
    }
}
