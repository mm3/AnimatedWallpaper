package com.android.mm3.wallpaper.animated;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		final int height = dm.heightPixels;
		final int width = dm.widthPixels;
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
        lp.addRule(RelativeLayout.ABOVE, R.id.button_layout);
        this.listView.setLayoutParams(lp);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		SharedPreferences p = getSharedPreferences(AnimatedWallpaperService.SHARED_PREFERENCES_NAME, 0);
		SharedPreferences.Editor editor = p.edit();
		if(this.root.getParent() != null) {
			editor.putString("root_folder", this.root.getAbsolutePath());
		} else {
			editor.remove("root_folder");
		}
		editor.commit();
	}

	
	protected void init(GridView listView) {
		folder_img = this.getResources().getDrawable(R.drawable.ic_folder_item);
		file_img = this.getResources().getDrawable(R.drawable.ic_file_icon);
		image_img = this.getResources().getDrawable(R.drawable.ic_image_item);
		
		SharedPreferences p = getSharedPreferences(AnimatedWallpaperService.SHARED_PREFERENCES_NAME, 0);
		String folder = p.getString( "root_folder", null );
		if(folder != null) {
			this.root = new File(folder);
		} else {
			this.root = Environment.getExternalStorageDirectory();
			if(this.root == null) {
				this.root = Environment.getDataDirectory();
			}
		}
		this.listView = listView;
		this.listView.setAdapter(new FileViewAdapret(this, this.root));
		this.listView.setOnItemClickListener(this);
		this.listView.setOnItemLongClickListener(this);
	}
	
	private static boolean isImage(String t) {
		return t.endsWith(".gif") ||
			    t.endsWith(".png") ||
			    t.endsWith(".x3d") ||
			    t.endsWith(".wrl") ||
		        t.endsWith(".bmp") ||
		        t.endsWith(".jpg") ||
		        t.endsWith(".jpeg") ||
		        t.endsWith(".svg");
	}
	
	protected void save(String path) {
		SharedPreferences p = getSharedPreferences(AnimatedWallpaperService.SHARED_PREFERENCES_NAME, 0);
		SharedPreferences.Editor editor = p.edit();
		editor.putString("file_name", path);
		editor.commit();
		finish();
	}
	
    public class FileComparator implements Comparator<File> {
		public int compare(File file1, File file2) {
			if(file1.isDirectory() != file2.isDirectory()) {
				return file1.isDirectory() ? -1 : 1;
			}
			return file1.compareTo( file2 );
		}
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
    		if(this.content != null) {
    			Arrays.sort(this.content, new FileComparator());
    		}
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

	public static class LoadIconTask extends AsyncTask<Object, Void, View> {
		@Override
		protected View doInBackground(Object... params) {
			try {
				final String file = (String) params[0];
				final View v = (View) params[1];
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

	private static boolean isSupportFile(String t) {
		String tt = t.toLowerCase();
		return tt.endsWith(".gif") ||
			    tt.endsWith(".png") ||
			    tt.endsWith(".x3d") ||
			    tt.endsWith(".wrl") ||
		        tt.endsWith(".bmp") ||
		        tt.endsWith(".jpg") ||
		        tt.endsWith(".jpeg") ||
			    tt.endsWith(".svg");
	}

	private void startStandartViewer(File f) {
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
			if(isSupportFile(f.getName())) {
				save(f.getAbsolutePath());
			} else {
				startStandartViewer(f);
			}
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		File f = (File) view.getTag();
		if(f != null && f.isFile()) {
			startStandartViewer(f);
			return true;
		}
		return false;
	}
	
	@Override
    public void onBackPressed() {
		File parent = this.root.getParentFile();
		if(parent != null) {
			this.root = parent;
			this.listView.setAdapter(new FileViewAdapret(this, this.root));
		} else {
			super.onBackPressed();
		}
    }
}
