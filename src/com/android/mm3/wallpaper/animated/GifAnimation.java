package com.android.mm3.wallpaper.animated;

import android.graphics.Movie;
import android.graphics.Canvas;

public class GifAnimation extends Animation
{
	protected Movie animation = null;
	
	public GifAnimation(String s) 
	{
		animation = Movie.decodeFile(s);
	}
	
	public void draw (Canvas c)
	{
		if(animation != null) 
		{
			animation.draw(c,0,0);
		}
	}
}
