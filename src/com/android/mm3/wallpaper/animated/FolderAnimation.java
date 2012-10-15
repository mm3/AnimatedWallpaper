package com.android.mm3.wallpaper.animated;

import java.io.*;
import android.graphics.Canvas;

public class FolderAnimation extends Animation
{
	static final public String TAG = "FolderAnimation";
	
	protected File rootFolder= null;

	public FolderAnimation (String folder){
		rootFolder = new File(folder);
	}
	
	public void draw (Canvas c)
	{
		if(rootFolder != null) 
		{
			//animation.draw(c,0,0);
		}
	}
	

}
