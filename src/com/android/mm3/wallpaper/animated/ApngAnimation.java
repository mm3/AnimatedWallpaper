package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.InputStream;
import java.io.FileInputStream;
import android.graphics.Color;
import android.graphics.Paint;


public class ApngAnimation extends Animation
{
	static final public String TAG = "ApngAnimation";
	
	protected Bitmap bitmap = null;
	
	public ApngAnimation(String s, int style) {
		super(style);
		init(s);
	}
	
	public void init(String s) {
		Log.w(TAG, "ApngAnimation constructor");

		InputStream is = null;

        try {
            is = new FileInputStream(s);
			Log.e(TAG, "init 1");
			PngDecoder decoder = new PngDecoder(is);
			Log.e(TAG, "init width = " +decoder.getWidth()+" height = " + decoder.getHeight());
			bitmap = decoder.decodeBitmap();
			Log.e(TAG, "init bitmap_w="+bitmap.getWidth()+" bitmap_h="+bitmap.getHeight());
        }
        catch (Exception e) {
            Log.e(TAG, "ApngAnimation exeption: " + e);
			e.printStackTrace();
        }
		finally {
			try {
			    if(is != null) {
				    is.close();
				    is = null;
			    }
			} catch (Exception e) {}
		}
		Log.w(TAG, "ApngAnimation constructor end");
		
	}
	
	public void getNextFrame(Canvas c) {
		Log.w(TAG, "draw");
	}
	
	public int getImageWidth(Canvas c) {
		return (this.bitmap != null) ? this.bitmap.getWidth() : c.getWidth();
	}
	
	public int getImageHeight(Canvas c) {
		return (this.bitmap != null) ? this.bitmap.getHeight() : c.getHeight();
	}

	public void drawImage(Canvas c, int left, int top, int right, int bottom, Paint paint) {
		c.drawColor(Color.BLACK);
		c.drawBitmap(this.bitmap, left, top, paint);
	}
	
	public void drawEnd(Canvas c) {
	}
	
	public int getDelay() 
	{
		return super.getDelay();
	}
	
}
