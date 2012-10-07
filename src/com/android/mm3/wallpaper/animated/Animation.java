package com.android.mm3.wallpaper.animated;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Animation
{
	public void draw (Canvas c) {
		Paint p = new Paint();
		p.setColor(0xaaaaaa);
		c.drawRect(0,0,10,10,p);
		c.drawText("No setup animation.", c.getWidth()/2, c.getHeight()/2, p);
	}
}
