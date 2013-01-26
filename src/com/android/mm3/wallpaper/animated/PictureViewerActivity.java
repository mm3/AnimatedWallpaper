package com.android.mm3.wallpaper.animated;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;


public class PictureViewerActivity extends FileViewActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid_file_view);
		
		init((GridView) findViewById(R.id.file_list));
		
		Button btnSave = (Button)findViewById(R.id.button_save);
		btnSave.setText(R.string.button_test);
		btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                test();
            }
        });

		//btnSave.setVisibility(View.GONE);
	}
	
	protected void test() {
		Intent i = new Intent(this, FreeWRLActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void save(String path) {
		Intent i = new Intent(this, ViewPictureActivity.class);
		i.putExtra("view_picture", path);
		startActivity(i);
	}
}
