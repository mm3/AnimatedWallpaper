package com.android.mm3.wallpaper.animated;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Log;

public class StaticAnimation extends Animation
{
	static final public String TAG = "StaticAnimation";
	protected File nextFile= null;
	protected Bitmap bitmap = null;
	private boolean canUpdate = true;
	private int width = 0;
	private int height = 0;

	public StaticAnimation (String file, int style){
		super(style);
		nextFile = new File(file);
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
			return Integer.MAX_VALUE-1;
		}
	}
}
