package com.android.mm3.wallpaper.animated;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsWallpaperActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource( R.xml.settings );
        setContentView(R.layout.activity_settings_wallpaper);
    }

}
