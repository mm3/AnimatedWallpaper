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
	protected int width = 0;
	protected int height = 0;
//	protected Drawable[] drawables = null;
	protected Paint paint = null;
	
	public SvgAnimation(String file, int style, int width, int height){
		super(style);
		this.width = width;
		this.height = height;
		init(file);
	}
		
	public void init(final String s) 
	{
		Thread t = new Thread() {
			@Override
			public void run() {
				Log.d(TAG, "SvgAnimation constructor");
				InputStream is = null;
		        try {
		            is = new FileInputStream(s);
					setDecoder(newDecoder(is));
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
				Log.d(TAG, "SvgAnimation constructor end");
			}
		};
		t.start();
	}
	
	protected SvgDecoder newDecoder(InputStream is) {
		SvgDecoder decoder = new SvgDecoder();
		decoder.setWidthHeight(width, height);
		decoder.parse(is);
		decoder.setScaling(true);
		return decoder;
	}

	
	private void setDecoder(SvgDecoder decoder) {
		this.maxCount = decoder.getFrameCount();
		this.decoder = decoder;
		this.counter = 0;
	}

	
	public void draw (Canvas c)
	{
		if(decoder == null) {
			return;
		}
		
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
		return (decoder == null) ? 100 : decoder.getDelay(counter);
	}
	
}
