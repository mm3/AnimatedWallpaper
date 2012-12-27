package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class Animation
{
	static final public String TAG = "Animation";
	
	static final public int STYLE_CENTRED = 0;
	static final public int STYLE_RESIZED = 1;
	static final public int STYLE_MOSTED  = 2;
	
	public int style = Animation.STYLE_CENTRED;
	protected Paint paint = null;
	
	public Animation(){
		style = Animation.STYLE_CENTRED;
		paint = new Paint();
		paint.setAntiAlias(true);
	}
	
	public Animation(int style) {
		this.style = style;
		paint = new Paint();
		paint.setAntiAlias(true);
	}
	
	public int getImageWidth(Canvas c) {
		return c.getWidth();
	}
	
	public int getImageHeight(Canvas c) {
		return c.getHeight();
	}
	
	public void getNextFrame(Canvas c) {
	}
	
	public void drawImage(Canvas c, int left, int top, int right, int bottom, Paint paint) {
		paint.setColor(Color.RED);
		Log.d(TAG, "draw default animation");
		c.drawColor(Color.BLACK);
		c.drawText("No setup animation.", 0, 0, paint);
	}
	
	public void drawEnd(Canvas c) {
		
	}
	
	public void draw (Canvas c)
	{
		getNextFrame(c);
		c.drawColor(Color.TRANSPARENT);
		
		switch(this.style) {
			case Animation.STYLE_CENTRED:
			{
				int dx = (c.getWidth() - getImageWidth(c)) / 2;
				int dy = (c.getHeight() - getImageHeight(c)) / 2;
				c.drawColor(Color.BLACK);
				drawImage(c, dx, dy, getImageWidth(c), getImageHeight(c), paint);
			}
			break;
			case Animation.STYLE_MOSTED:
			{
				int countX = c.getWidth() / getImageWidth(c);
				int countY = c.getHeight() / getImageHeight(c);
				int dx = (c.getWidth() - countX*getImageWidth(c)) / 2 - getImageWidth(c);
				int dy = (c.getHeight() - countY*getImageHeight(c)) / 2 - getImageHeight(c);
				countX += 2;
				countY += 2;
				
				for(int j = 0; j < countY; j++)
				{
					for(int i = 0; i < countX; i++)
					{
						drawImage(c, dx + i*getImageWidth(c), dy + j*getImageHeight(c), getImageWidth(c), getImageHeight(c), paint);
					}
				}
			}
			break;
			case Animation.STYLE_RESIZED: 
			{
				float scaleb = (float)getImageWidth(c)/(float)getImageHeight(c);
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
						drawImage(c, 0, dd+i*height,c.getWidth(), dd+i*height+height, paint);
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
						drawImage(c, dd+i*width, 0 ,dd+i*width+width, c.getHeight(), paint);
					}
				}
			}
			break;
			default:
			{
				drawImage(c, 0, 0, getImageWidth(c), getImageHeight(c), paint);
			}
			break;
		}
		drawEnd(c);
	}

	public int getDelay() 
	{
		return 60 * 60 * 1000;
	}
}
