package com.android.mm3.wallpaper.animated;


import android.content.res.Resources;
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
	

	protected Decoder decoder = null;
	protected Bitmap bitmap = null;
	protected int counter = 0;
	protected int maxCount = 0;
	protected Drawable[] drawables = null;
	
	public GifAnimation(String s)
	{
		super(Animation.STYLE_MOSTED);
		init(s);
	}
	
	public GifAnimation(String s, int style)
	{
		super(style);
		init(s);
	}
	
	public void init(final String s) 
	{
		Thread t = new Thread() {
			@Override
			public void run() {
				Log.d(TAG, "GifAnimation constructor");
				InputStream is = null;
		        try {
		            is = new FileInputStream(s);
					setDecoder(newDecoder(is));
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
				Log.d(TAG, "GifAnimation constructor end");
			}
		};
		t.start();
	}
	
	protected Decoder newDecoder(InputStream is) {
        GifDecoder decoder = new GifDecoder();
		decoder.read(is);
		return decoder;
	}

	
	private void setDecoder(Decoder decoder) {
		this.maxCount = decoder.getFrameCount();
		this.decoder = decoder;
		this.counter = 0;
	}

	public void getNextFrame(Canvas c) {
		if(this.decoder == null) {
			return;
		}
		
		if(this.counter >= this.maxCount) {
			this.counter = 0;
		}
		
		this.bitmap = this.decoder.getFrame(this.counter);
		
		if (this.style == Animation.STYLE_RESIZED) {
			if(this.drawables == null) 
			{
				this.drawables = new Drawable[this.maxCount];
			}
			if(this.drawables[this.counter] == null)
			{
				this.drawables[this.counter] = new BitmapDrawable(Resources.getSystem(), this.bitmap);
			}
		}
		
	}
	
	public int getImageWidth(Canvas c) {
		return (this.bitmap != null) ? this.bitmap.getWidth() : c.getWidth();
	}
	
	public int getImageHeight(Canvas c) {
		return (this.bitmap != null) ? this.bitmap.getHeight() : c.getHeight();
	}

	
	public void drawImage(Canvas c, int left, int top, int right, int bottom, Paint paint) {
		if(this.decoder == null) {
			c.drawColor(Color.BLACK);
			return;
		}
		switch(this.style) {
			case Animation.STYLE_RESIZED:
				if(this.drawables != null && this.drawables[this.counter] != null) {
					this.drawables[this.counter].setBounds(left,top,right,bottom);
					this.drawables[this.counter].draw(c);
				}
				break;
			case Animation.STYLE_CENTRED:
			case Animation.STYLE_MOSTED:
			default:
				c.drawColor(Color.BLACK);
				if(this.bitmap != null) {
					c.drawBitmap(this.bitmap, left, top, paint);
				}
				break;
		}
	}

	public void drawEnd(Canvas c) {
		this.counter++;
	}
	
	public int getDelay() 
	{
		if(this.decoder != null ) {
			return this.decoder.getDelay(this.counter);
		}
		else {
			return 100;
		}
	}
}
