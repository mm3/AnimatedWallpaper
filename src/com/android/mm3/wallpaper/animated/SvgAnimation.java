package com.android.mm3.wallpaper.animated;

import android.util.Log;
import java.io.InputStream;
import java.io.FileInputStream;
import android.graphics.*;

public class SvgAnimation extends Animation
{
	static final public String TAG = "SvgAnimation";
	
	
	protected SvgDecoder decoder = null;
	protected Picture picture = null;
	protected int counter = 0;
	protected int maxCount = 0;
//	protected Drawable[] drawables = null;
	protected Paint paint = null;
	
	public SvgAnimation(String file, int style, int width, int height){
		super(style);
		init(file, width, height);
	}
		
	public void init(String s, int width, int height) {
		Log.w(TAG, "SvgAnimation constructor");

		paint = new Paint();
		paint.setAntiAlias(true);

		InputStream is = null;
        try {
			
            //is = new FileInputStream(s);
			decoder = new SvgDecoder();
			//decoder.read(is, width, height);
			//is.close();
            is = new FileInputStream(s);
			decoder.parse(is);
			maxCount = decoder.getFrameCount();
        }
        catch (Exception e) {
            Log.e(TAG, "SvgAnimation exeption" + e);
        }
		finally {
			try {
			    if(is != null) {
				    is.close();
				    is = null;
			    }
			} catch (Exception e) {}
		}
		Log.w(TAG, "SvgAnimation constructor end");
		
	}
	
	public void draw (Canvas c)
	{
		if(counter >= maxCount) {
			counter = 0;
		}
		c.drawColor(Color.TRANSPARENT);
		//picture = decoder.getFramePicture(counter);
		//picture.draw(c);
		decoder.draw(c);
		counter++;
	}
	
	public int getDelay() 
	{
		return decoder.getDelay(counter);
	}
	
}
