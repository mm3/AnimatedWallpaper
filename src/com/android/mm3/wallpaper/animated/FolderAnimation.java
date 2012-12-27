package com.android.mm3.wallpaper.animated;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Log;

public class FolderAnimation extends Animation
{
	static final public String TAG = "FolderAnimation";
	
	protected File rootFolder= null;
	protected File nextFile= null;
	protected Bitmap bitmap = null;
	private boolean canUpdate = true;
	private int width = 0;
	private int height = 0;

	public FolderAnimation (String folder){
		rootFolder = new File(folder);
	}
	
	public void getNextFrame(Canvas c) {
		if(this.canUpdate) {
			this.width = c.getWidth();
			this.height = c.getHeight();
			
			Thread t = new Thread() {
				@Override
				public void run() {
					File file = nextFile;
					Bitmap b = null;
					try {
						File[] list = rootFolder.listFiles(filter);
						Arrays.sort(list, comp);
						if(list != null && list.length > 0 ) {
							if(file == null) {
								file = list[0];
							} else {
								for(int i = 0; i < list.length; i++) {
									if(file.getName().equalsIgnoreCase(list[i].getName())) {
										file = list[((i+1 == list.length) ? 0 : i+1)];
										break;
									}
								}
							}
						}
						
						if(file != null) {
							BitmapFactory.Options op = new BitmapFactory.Options();
							int scale = 1;
							InputStream in = null;
							try {
								in = new BufferedInputStream(new FileInputStream(file));
								op.inJustDecodeBounds = true;
								BitmapFactory.decodeStream(in, null, op);
								if(op.outWidth > op.outHeight) {
									scale = Math.round((float) op.outHeight / (float) height);
								} else {
									scale = Math.round((float) op.outWidth / (float) width);
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
							b = BitmapFactory.decodeFile(file.getAbsolutePath(), op);	
						}
					} catch (Exception e) {	
						Log.d(TAG, "Exception: " + e);
					}
					setBitmap(file, b);
				}
			};
			t.start();
			this.canUpdate = false;
		}
	}
	
	private void setBitmap(File file, Bitmap b) {
		this.canUpdate = true;
		this.nextFile = file;
		this.bitmap = b;
	}
	
	protected Comparator<File> comp = new Comparator<File>() {
		public int compare(File file1, File file2) {
			return file1.compareTo( file2 );
		}
    };
	
	protected FileFilter filter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if(pathname.isFile()) {
				int i = pathname.getName().lastIndexOf('.');
				if(i > 0) {
					String s = pathname.getName().substring(i+1).toLowerCase();
					return s.equals("jpg") || s.equals("jpeg") || s.equals("png") ||s.equals("gif") || s.equals("bmp");
				}
			}
			return false;
		}
		
	};

	
	public int getImageWidth(Canvas c) {
		return (this.bitmap != null) ? this.bitmap.getWidth() : c.getWidth();
	}
	
	public int getImageHeight(Canvas c) {
		return (this.bitmap != null) ? this.bitmap.getHeight() : c.getHeight();
	}

	
	public void drawImage(Canvas c, int left, int top, int right, int bottom, Paint paint) {
		c.drawColor(Color.BLACK);
		if(this.bitmap != null) {
			c.drawBitmap(this.bitmap, left, top, paint);
		}
	}

	public void drawEnd(Canvas c) {
		
	}
	
	public int getDelay() 
	{
		if(this.bitmap == null && !this.canUpdate) {
			return 100;
		} else {
			return 5 * 60 * 1000;
		}
	}

}
