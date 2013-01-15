package com.android.mm3.wallpaper.animated;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.*;

public class FileViewActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
	
	private GridView listView = null;
	private File root = null;
	
	protected Drawable folder_img;
	protected Drawable file_img;
	protected Drawable image_img;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid_file_view);
		
		init((GridView) findViewById(R.id.file_list));
		
		Button btnSave = (Button) findViewById(R.id.button_save);
		btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save(root.getAbsolutePath());
            }
        });
	}
	
	protected void init(GridView listView) {
		folder_img = this.getResources().getDrawable(R.drawable.ic_folder_item);
		file_img = this.getResources().getDrawable(R.drawable.ic_file_icon);
		image_img = this.getResources().getDrawable(R.drawable.ic_image_item);
		
		this.root = Environment.getExternalStorageDirectory();
		if(this.root == null) {
			this.root = Environment.getDataDirectory();
		}
		this.listView = listView;
		this.listView.setAdapter(new FileViewAdapret(this, this.root));
		this.listView.setOnItemClickListener(this);
		this.listView.setOnItemLongClickListener(this);
	}
	
	private static boolean isImage(String t) {
		return t.endsWith(".gif") ||
		       t.endsWith(".jpg") ||
			   t.endsWith(".png") ||
			   t.endsWith(".svg");
	}
	
	protected void save(String path) {
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
			this.inflater = LayoutInflater.from(this.context);
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
					BitmapFactory.Options op = new BitmapFactory.Options();
					int scale = 1;
					InputStream in = null;
					try {
						in = new BufferedInputStream(new FileInputStream(file));
						op.inJustDecodeBounds = true;
						BitmapFactory.decodeStream(in, null, op);
						if(op.outWidth > op.outHeight) {
							scale = Math.round((float) op.outHeight / (float) v.getHeight());
						} else {
							scale = Math.round((float) op.outWidth / (float) v.getWidth());
						}
					} catch (Exception e) {						
					} finally {
						try {
							if(in != null) {
								in.close();
								in = null;
							}
						} catch (Exception e) {}
					}
					op = new BitmapFactory.Options();
					op.inSampleSize = scale;
					op.inPreferredConfig = Config.RGB_565;
					Bitmap b = BitmapFactory.decodeFile(file, op);
					b = Bitmap.createScaledBitmap(b, v.getWidth(), v.getHeight(), false);
					v.setTag(b);
				}
				return v;
			} catch(Exception e) {
				return null;
			}
		}
		protected void onPostExecute(View v) {
			try {
				Bitmap b = (Bitmap) v.getTag();
				ImageView i = (ImageView) v;
				i.setImageBitmap(b);
			} catch(Exception e) {}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		File f = (File) view.getTag();
		if(f != null && f.isFile()) {
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			Uri uri = Uri.fromFile(f);
			String name = f.getName();
			MimeTypeMap mime = MimeTypeMap.getSingleton();
            String ext=name.substring(name.indexOf(".")+1).toLowerCase();
            String type = mime.getMimeTypeFromExtension(ext);
			intent.setDataAndType(uri, type);
			try {
				startActivity(intent);
			} catch(Exception e) {
				intent.setDataAndType(uri, "*/*");
				startActivity(intent);
			}
			return true;
		}
		return false;
	}
}
