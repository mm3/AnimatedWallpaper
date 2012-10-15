package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class Animation
{
	static final public String TAG = "Animation";
	public void draw (Canvas c) {
		Paint p = new Paint();
		Log.w(TAG, "draw");
		p.setColor(0xaaaaaa);
		c.drawRect(0,0,10,10,p);
		c.drawText("No setup animation.", c.getWidth()/2, c.getHeight()/2, p);
	}
	
	public int getDelay() 
	{
		return 100;
	}
}
