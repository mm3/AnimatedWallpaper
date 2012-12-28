package com.android.mm3.wallpaper.animated;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;


public class PictureViewerActivity extends FileViewActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid_file_view);
		
		init((GridView) findViewById(R.id.file_list));
		
		View btnSave = findViewById(R.id.button_save);
		btnSave.setVisibility(View.GONE);
	}
	
	@Override
	protected void save(String path) {
		Intent i = new Intent(this, ViewPictureActivity.class);
		i.putExtra("view_picture", path);
		startActivity(i);
	}
}
