package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.graphics.*;

public class Animation
{
	static final public String TAG = "Animation";
	
	static final public int STYLE_CENTRED = 0;
	static final public int STYLE_RESIZED = 1;
	static final public int STYLE_MOSTED  = 2;
	
	public int style = Animation.STYLE_CENTRED;
	
	public Animation(){
		style = Animation.STYLE_CENTRED;
	}
	
	public Animation(int style) {
		this.style = style;
	}
	
	
	public void draw (Canvas c) {
		Paint p = new Paint();
		Log.w(TAG, "draw");
		p.setColor(Color.RED);
		c.drawColor(Color.BLACK);
		c.drawText("No setup animation.", 0, 0, p);
	}
	
	public int getDelay() 
	{
		return 100;
	}
}
