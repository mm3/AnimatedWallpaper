package com.android.mm3.wallpaper.animated;


import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.InputStream;
import java.io.FileInputStream;


public class GifAnimation extends Animation
{
	static final public String TAG = "GifAnimation";
	

	protected GifDecoder gifDecoder = null;
	private Bitmap bitmap = null;
	private int counter = 0;
	private int maxCount = 0;
	private InputStream is = null;
	
	public GifAnimation(String s) 
	{
		Log.w(TAG, "GifAnimation constructor");
		
        try {
            is = new FileInputStream(s);
			gifDecoder = new GifDecoder();
			gifDecoder.read(is);
			maxCount = gifDecoder.getFrameCount();
        }
        catch (Exception e) {
            Log.e(TAG, "GifAnimation exeption" + e);
        }
		Log.w(TAG, "GifAnimation constructor end");
	}
	
	public void draw (Canvas c)
	{
		//Log.w(TAG, "draw");
		if(counter >= maxCount) {
			counter = 0;
		}
		bitmap = gifDecoder.getFrame(counter);
		c.drawBitmap(bitmap, 0, 0, null);
		counter++;
	}
	
	public int getDelay() 
	{
		return gifDecoder.getDelay(counter);
	}
}
