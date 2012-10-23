package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.InputStream;
import java.io.FileInputStream;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Color;
import android.graphics.Paint;
import java.nio.*;


public class ApngAnimation extends Animation
{
	static final public String TAG = "ApngAnimation";
	
	protected Paint paint;
	protected Bitmap bitmap = null;
	
	public ApngAnimation(String s, int style) {
		super(style);
		init(s);
	}
	
	public void init(String s) {
		Log.w(TAG, "ApngAnimation constructor");

		paint = new Paint();
		paint.setAntiAlias(true);

		InputStream is = null;

        try {
            is = new FileInputStream(s);
			Log.e(TAG, "init 1");
			PngDecoder decoder = new PngDecoder(is);
			Log.e(TAG, "init width = " +decoder.getWidth()+" height = " + decoder.getHeight());
			ByteBuffer bb = ByteBuffer.allocate(decoder.getWidth()*decoder.getHeight()*3);
			decoder.decode(bb, 0, PngDecoder.Format.RGB);
			bitmap = Bitmap.createBitmap(decoder.getWidth(), decoder.getHeight(), Bitmap.Config.RGB_565 );
			Log.e(TAG, "init position = "+bb.position());
			bb.position(0);
			bitmap.copyPixelsFromBuffer(bb);
			//maxCount = gifDecoder.getFrameCount();
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
	
	public void draw (Canvas c) {
		Log.w(TAG, "draw");
		c.drawColor(Color.BLACK);
		c.drawBitmap(bitmap, 0, 0, paint);
	}

	public int getDelay() 
	{
		return Integer.MAX_VALUE;
	}
	
}
