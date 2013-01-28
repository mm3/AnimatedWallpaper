package com.android.mm3.wallpaper.animated;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class PictureViewerActivity extends FileViewActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Button btnSave = (Button)findViewById(R.id.button_save);
		btnSave.setVisibility(View.GONE);
	}
	
	protected void testX3D(String path) {
		Intent i = new Intent(this, FreeWRLActivity.class);
		i.putExtra("view_picture", path);
		startActivity(i);
	}
	
	@Override
	protected void save(String path) {
		if(path.endsWith(".x3d") || path.endsWith(".wrl")) {
			testX3D(path);
			return;
		}
		Intent i = new Intent(this, ViewPictureActivity.class);
		i.putExtra("view_picture", path);
		startActivity(i);
	}
}
