package com.android.mm3.wallpaper.animated;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import java.io.InputStream;


public class ApngAnimation extends GifAnimation
{
	static final public String TAG = "ApngAnimation";
	
	protected Bitmap bitmap = null;
	
	public ApngAnimation(String s, int style) {
		super(s, style);
	}
	
	@Override
	protected Decoder newDecoder(InputStream is) {
        try {
			ApngDecoder decoder = new ApngDecoder(is);
        	return decoder;
        }
        catch (Exception e) {
            Log.e(TAG, "ApngAnimation exeption" + e);
        }
        return null;
	}
	
	@Override
	public int getImageWidth(Canvas c) {
		return (this.decoder != null) ? this.decoder.getWidth() : c.getWidth();
	}
	
	@Override
	public int getImageHeight(Canvas c) {
		return (this.decoder != null) ? this.decoder.getHeight() : c.getHeight();
	}
	
	

}
