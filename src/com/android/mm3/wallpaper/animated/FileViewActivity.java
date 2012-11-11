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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.*;

public class FileViewActivity extends Activity implements AdapterView.OnItemClickListener {
	
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
		this.listView.setOnItemClickListener(this);
		
		Button btnSave = (Button) findViewById(R.id.button_save);
		btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save(root.getAbsolutePath());
            }
        });
	}
	
	private static boolean isImage(String t) {
		return t.endsWith(".gif") ||
		       t.endsWith(".jpg") ||
			   t.endsWith(".png") ||
			   t.endsWith(".svg");
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
		private Context context = null;
		
		public FileViewAdapret(Context c, File root) {
			this.context = c;
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
				v = inflater.inflate(R.layout.file_item, parent, false);
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
				new LoadIconTask().execute(this.content[position - this.haveParent].getAbsolutePath(), i);
			} else {
				i.setImageDrawable(file_img);
			}
			t.setText(this.content[position - this.haveParent].getName());
			v.setTag(this.content[position - this.haveParent]);
			return v;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		File f = (File) v.getTag();
		if(f == null) {
			this.root = this.root.getParentFile();
			this.listView.setAdapter(new FileViewAdapret(this, this.root));
		} else if(f.isDirectory()) {
			this.root = f;
			this.listView.setAdapter(new FileViewAdapret(this, this.root));
		} else {
			save(f.getAbsolutePath());
		}
		
	}
	
	public static class LoadIconTask extends AsyncTask<Object, Void, View> {
		@Override
		protected View doInBackground(Object... params) {
			try {
				final String file = (String) params[0];
				final View v = (View) params[1];
				if(v.getTag() == null) {
					Drawable d = Drawable.createFromPath(file);
					v.setTag(d);
				}
				return v;
			} catch(Exception e) {
				return null;
			}
		}
		protected void onPostExecute(View v) {
			try {
				Drawable d = (Drawable) v.getTag();
				ImageView view = (ImageView) v;
				Drawable old = view.getDrawable();
				d.setBounds(0,0,old.getIntrinsicWidth(), old.getIntrinsicHeight());
				//d.setBounds(old.getBounds());
				view.setImageDrawable(d);
			} catch(Exception e) {}
		}
	}
}
