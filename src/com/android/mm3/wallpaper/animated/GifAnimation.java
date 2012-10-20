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


public class GifAnimation extends Animation
{
	static final public String TAG = "GifAnimation";
	

	protected Decoder gifDecoder = null;
	protected Bitmap bitmap = null;
	protected int counter = 0;
	protected int maxCount = 0;
	protected Drawable[] drawables = null;
	protected Paint paint = null;
	
	public GifAnimation(String s)
	{
		init(s, Animation.STYLE_MOSTED);
	}
	
	public GifAnimation(String s, int style)
	{
		init(s, style);
	}
	
	public void init(String s, int style) 
	{
		Log.w(TAG, "GifAnimation constructor");
		
		this.style = style;
		
		paint = new Paint();
		paint.setAntiAlias(true);
		
		InputStream is = null;
		
        try {
            is = new FileInputStream(s);
			gifDecoder = new GifDecoder();
			gifDecoder.read(is);
			maxCount = gifDecoder.getFrameCount();
        }
        catch (Exception e) {
            Log.e(TAG, "GifAnimation exeption" + e);
        }
		finally {
			try {
			    if(is != null) {
				    is.close();
				    is = null;
			    }
			} catch (Exception e) {}
		}
		Log.w(TAG, "GifAnimation constructor end");
	}
	
	public void draw (Canvas c)
	{
		if(counter >= maxCount) {
			counter = 0;
		}
		c.drawColor(Color.TRANSPARENT);
		bitmap = gifDecoder.getFrame(counter);
		
		if(style == Animation.STYLE_CENTRED)
		{
			int dx = (c.getWidth() - bitmap.getWidth()) / 2;
			int dy = (c.getHeight() - bitmap.getHeight()) / 2;
			//c.setBitmap(bitmap);
			c.drawBitmap(bitmap, dx, dy, paint);
		}
		else if (style == Animation.STYLE_MOSTED)
		{
			int countX = c.getWidth() / bitmap.getWidth();
			int countY = c.getHeight() / bitmap.getHeight();
			int dx = (c.getWidth() - countX*bitmap.getWidth()) / 2 - bitmap.getWidth();
			int dy = (c.getHeight() - bitmap.getHeight()) / 2 - bitmap.getHeight();
			countX += 2;
			countY += 2;
			
			for(int j = 0; j < countY; j++)
			{
				for(int i = 0; i < countX; i++)
				{
					c.drawBitmap(bitmap, dx + i*bitmap.getWidth(), dy + j*bitmap.getHeight(), paint);
				}
			}
		}
		else if (style == Animation.STYLE_RESIZED) 
		{
			if(drawables == null) 
			{
				drawables = new Drawable[maxCount];
			}
			if(drawables[counter] == null)
			{
				drawables[counter] = new BitmapDrawable(bitmap);
			}
			float scaleb = (float)bitmap.getWidth()/(float)bitmap.getHeight();
			float scalec = (float)c.getWidth()/(float)c.getHeight();
			//Log.w(TAG, "canvas width="+c.getWidth()+" height="+c.getHeight());
			//Log.w(TAG, "bitmap width="+bitmap.getWidth()+" height="+bitmap.getHeight());
			if(scalec < scaleb)
			{
				int height = (int)(((float)c.getWidth())/scaleb);
				int countd = c.getHeight() / height;
				int dd = (c.getHeight() - countd*height) / 2 - height;
				countd += 2;

				for(int i = 0; i < countd; i++)
				{
					drawables[counter].setBounds(0,dd+i*height,c.getWidth(),dd+i*height+height);
					drawables[counter].draw(c);
				}
			}
			else 
			{
				int width = (int)(((float)c.getHeight())*scaleb);
				int countd = c.getWidth() / width;
				int dd = (c.getWidth() - countd*width) / 2 - width;
				countd += 2;

				for(int i = 0; i < countd; i++)
				{
					drawables[counter].setBounds(dd+i*width,0,dd+i*width+width,c.getHeight());
					drawables[counter].draw(c);
				}
			}
		}
		else
		{
			c.drawBitmap(bitmap, 0, 0, paint);
		}
		counter++;
	}
	
	public int getDelay() 
	{
		return gifDecoder.getDelay(counter);
	}
}
