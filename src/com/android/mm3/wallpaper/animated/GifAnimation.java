package com.android.mm3.wallpaper.animated;


import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.InputStream;
import java.io.FileInputStream;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Color;


public class GifAnimation extends Animation
{
	static final public String TAG = "GifAnimation";
	

	protected GifDecoder gifDecoder = null;
	private Bitmap bitmap = null;
	private int counter = 0;
	private int maxCount = 0;
	private InputStream is = null;
	private Drawable[] drawables = null;
	
	public GifAnimation(String s)
	{
		init(s, Animation.STYLE_RESIZED);
	}
	
	public GifAnimation(String s, int style)
	{
		init(s, style);
	}
	
	public void init(String s, int style) 
	{
		Log.w(TAG, "GifAnimation constructor");
		
		this.style = style;
		
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
		if(counter >= maxCount) {
			counter = 0;
		}
		c.drawColor(Color.TRANSPARENT);
		bitmap = gifDecoder.getFrame(counter);
		
		if(style == Animation.STYLE_CENTRED)
		{
			int dx = (c.getWidth() - bitmap.getWidth()) / 2;
			int dy = (c.getHeight() - bitmap.getHeight()) / 2;
			c.drawBitmap(bitmap, dx, dy, null);
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
					c.drawBitmap(bitmap, dx + i*bitmap.getWidth(), dy + j*bitmap.getHeight(), null);
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
			float scaleb = bitmap.getWidth()/bitmap.getHeight();
			float scalec = c.getWidth()/c.getHeight();
			if(scalec <= scaleb)
			{
				int height = bitmap.getHeight()*c.getWidth()/bitmap.getWidth();
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
				int width = bitmap.getWidth()*c.getHeight()/bitmap.getHeight();
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
		counter++;
	}
	
	public int getDelay() 
	{
		return gifDecoder.getDelay(counter);
	}
}
