package com.android.mm3.wallpaper.animated;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class FileViewActivity extends Activity implements View.OnClickListener {
	
	private GridView listView = null;
	private File root = null;
	
	private Drawable folder_img;
	private Drawable file_img;
	private Drawable image_img;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid_file_view);
		
		folder_img = this.getResources().getDrawable(R.drawable.ic_folder_item);
		file_img = this.getResources().getDrawable(R.drawable.ic_file_icon);
		image_img = this.getResources().getDrawable(R.drawable.ic_image_item);
		
		this.root = Environment.getExternalStorageDirectory();
		if(this.root == null) {
			this.root = Environment.getDataDirectory();
		}
		this.listView = (GridView) findViewById(R.id.file_list);
		this.listView.setAdapter(new FileViewAdapret(this, this.root));
		this.listView.setOnClickListener(this);
		
		Button btnSave = (Button) findViewById(R.id.button_save);
		btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save(root.getAbsolutePath());
            }
        });
	}
	
	private static boolean isImage(String t) {
		return false;
	}
	
	private void save(String path) {
		SharedPreferences p = getSharedPreferences(AnimatedWallpaperService.SHARED_PREFERENCES_NAME, 0);
		SharedPreferences.Editor editor = p.edit();
		editor.putString("file_name", path);
		editor.commit();
		finish();
	}
	
	public class FileViewAdapret extends BaseAdapter {

		private File[] content = null;
		private LayoutInflater inflater = null;
		private int haveParent = 0;
		public FileViewAdapret(Context c, File root) {
			this.inflater = LayoutInflater.from(c);
			this.content = root.listFiles();
			this.haveParent = (root.getParent() != null) ? 1 : 0;
		}
		
		@Override
		public int getCount() {
			return this.content == null ? 0 : this.content.length + this.haveParent;
		}

		@Override
		public Object getItem(int position) {
			return this.content[position - this.haveParent];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if(v == null) {
				v = inflater.inflate(R.layout.file_item, parent);
			}
			ImageView i = (ImageView) v.findViewById(R.id.image);
			TextView t = (TextView) v.findViewById(R.id.text);
			if(position == 0 && this.haveParent != 0) {
				i.setImageDrawable(folder_img);
				t.setText("..");
				v.setTag(null);
				return v;
			}
			if(this.content[position - this.haveParent].isDirectory()) {
				i.setImageDrawable(folder_img);
			} else if(isImage(this.content[position - this.haveParent].getName())) {
				i.setImageDrawable(image_img);
			} else {
				i.setImageDrawable(file_img);
			}
			t.setText(this.content[position - this.haveParent].getName());
			v.setTag(this.content[position - this.haveParent]);
			return v;
		}
	}

	@Override
	public void onClick(View v) {
		File f = (File) v.getTag();
		if(f == null) {
			this.root = this.root.getParentFile();
		} else if(f.isDirectory()) {
			this.root = f;
			this.listView.setAdapter(new FileViewAdapret(this, this.root));
		} else {
			save(f.getAbsolutePath());
		}
		
	}
}
