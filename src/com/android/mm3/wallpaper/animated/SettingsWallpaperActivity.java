package com.android.mm3.wallpaper.animated;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;



public class SettingsWallpaperActivity extends PreferenceActivity {
// public class SettingsWallpaperActivity extends Activity{
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(AnimatedWallpaperService.SHARED_PREFERENCES_NAME);
        addPreferencesFromResource( R.xml.settings );
        //setContentView(R.layout.activity_settings_wallpaper);
    }

}

