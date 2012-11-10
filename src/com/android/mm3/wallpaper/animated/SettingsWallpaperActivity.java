package com.android.mm3.wallpaper.animated;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.app.Activity;

public class SettingsWallpaperActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsWallpaperFragment()).commit();
    }

	public class SettingsWallpaperFragment extends PreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        getPreferenceManager().setSharedPreferencesName(AnimatedWallpaperService.SHARED_PREFERENCES_NAME);
	        addPreferencesFromResource(R.layout.settings);
	    }
	}
}

