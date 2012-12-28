package com.android.mm3.wallpaper.animated;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class ViewPictureActivity extends Activity {
	static final public String TAG = "ViewPictureActivity";
	
	private Handler handler = new Handler();
	private Runnable runnable = new Runnable(){ 
			@Override
			public void run() { 
				nextFrame(); 	
			} 
		};
	private PictureView pview = null;
	protected int delay = 100;
	private int user_delay = 0;


	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings_wallpaper);
		Intent i = getIntent();
		String path = i.getStringExtra("view_picture");
		if(path != null) {
			pview = new PictureView(this, path);
			RelativeLayout view = (RelativeLayout)findViewById(R.id.view_picture);
			view.addView(pview);
		} else {
			finish();
		}
	}
	
	protected void nextFrame()
	{
		stopRunnable();
		handler.postDelayed( runnable, delay );

		if(pview != null) {
			pview.invalidate();
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		stopRunnable();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stopRunnable();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		nextFrame();
	}

	private void stopRunnable()
	{
		handler.removeCallbacks( runnable );
	}

	
	public class PictureView extends View {

		protected Animation defaultAnimation = new Animation();
		protected Animation animation = defaultAnimation;

		public PictureView(Context context, String path) {
			super(context);
			File file = new File(path);
			if(file.isDirectory()) {
				Log.w(TAG, "setup directory with walpapers");
				animation = new FolderAnimation(path);
			}
			else if(file.isFile()) {
				Log.w(TAG, "file is valide");
				if(file.getName().endsWith(".gif"))
				{
					animation = new GifAnimation(path, Animation.STYLE_CENTRED);
				}
				else if(file.getName().endsWith(".png") || file.getName().endsWith(".apng"))
				{
					animation = new ApngAnimation(path, Animation.STYLE_CENTRED);
				}
				else if(file.getName().endsWith(".svg"))
				{
					DisplayMetrics dm = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(dm);
					final int height = dm.heightPixels;
					final int width = dm.widthPixels;
					animation = new SvgAnimation(path, Animation.STYLE_CENTRED, width, height);
				}
				else {
					animation = defaultAnimation;					
				}
			}
			else {
				animation = defaultAnimation;
			}
		}
		
		@Override
		public	void draw(Canvas c) {
			c.save();
			animation.draw(c);
			if( user_delay > 0 ) {
				delay = user_delay;
			} 
			else
			{
			    delay = animation.getDelay();
			}
			c.restore();
		}
	}
}
