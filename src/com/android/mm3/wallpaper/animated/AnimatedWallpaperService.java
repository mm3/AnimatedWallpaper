package com.android.mm3.wallpaper.animated;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import java.io.File;
import android.widget.Toast;
import android.util.Log;

public class AnimatedWallpaperService extends WallpaperService {

	static final public String SHARED_PREFERENCES_NAME = "AnimatedWallpaperSettings";
	static final public String TAG = "AnimatedWallpaperService";
	
	protected Animation defaultAnimation = new Animation();
	protected Animation animation = defaultAnimation;
	
	@Override
	public Engine onCreateEngine() {		
		return new WallpaperEngine();
	}
	
	public Context getContext() {
		return this;
	}
	
	protected class WallpaperEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener
	{

		protected int delay = 40;
		
		private int user_delay = 0;

		private Handler handler = new Handler();
		private Runnable runnable = new Runnable(){ public void run() { nextFrame(); 	} };
		private boolean visible = false;
		private long time = 0;

		public WallpaperEngine()
		{
			super();
			PreferenceManager.setDefaultValues(AnimatedWallpaperService.this, R.layout.settings, false );
			SharedPreferences p = AnimatedWallpaperService.this.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
			p.registerOnSharedPreferenceChangeListener( this );
			onSharedPreferenceChanged( p, null );
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences p, String k) {
			String fileName = p.getString( "file_name", "none" );
			String style = p.getString("style_anim", "0");
			user_delay = Integer.valueOf(p.getString("delay_anim", "0"));
			Log.w(TAG, "setup file: "+fileName);
			File file = new File(fileName);
			if(file.isDirectory()) {
				Log.w(TAG, "setup directory with walpapers");
				animation = new FolderAnimation(fileName);
			}
			else if(file.isFile()) {
				Log.w(TAG, "file is valide");
				if(file.getName().endsWith(".gif")) {
					//Toast.makeText(getContext(),"this is gif", 7000).show();
					Log.w(TAG, "setup gif wallpaper");
					animation = new GifAnimation(fileName, Integer.valueOf(style));
				} else if(file.getName().endsWith(".png") || file.getName().endsWith(".apng")) {
					//Toast.makeText(getContext(),"this is gif", 7000).show();
					Log.w(TAG, "setup apng wallpaper");
					animation = new ApngAnimation(fileName, Integer.valueOf(style));
				} else if(file.getName().endsWith(".x3d") || file.getName().endsWith(".wrl")) {
					//Toast.makeText(getContext(),"this is gif", 7000).show();
					Log.w(TAG, "setup x3d wallpaper");
					animation = new GLAnimation(getContext(), fileName, Integer.valueOf(style));
				} else if(file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg") || file.getName().endsWith(".bmp")) {
					//Toast.makeText(getContext(),"this is gif", 7000).show();
					Log.w(TAG, "setup jpg wallpaper");
					animation = new StaticAnimation(fileName, Integer.valueOf(style));
				} else if(file.getName().endsWith(".svg")) {
					Log.w(TAG, "setup svg wallpaper");
					int width = 0;
					int height = 0;

					SurfaceHolder holder = null;
					Canvas c = null;
					try {
						holder = getSurfaceHolder();
						if( (c = holder.lockCanvas()) != null ) {
							width = c.getWidth();
							height = c.getHeight();
						}
					} catch(Exception e) {
						Log.w(TAG, "lock Canvas Exception "+ e);
					} finally {
						if( holder != null && c != null )
							holder.unlockCanvasAndPost( c );
					}
					animation = new SvgAnimation(fileName, Integer.valueOf(style), width, height);
				} else {
					Log.w(TAG, "setup default animation");
					animation = defaultAnimation;					
				}
			} else {
				Log.w(TAG, "setup default animation");
				animation = defaultAnimation;
			}
		}
		

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			handler.removeCallbacks( runnable );
			animation.onDestroy();
			SharedPreferences p = AnimatedWallpaperService.this.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
			p.unregisterOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onVisibilityChanged( boolean v )
		{
			visible = v;
			animation.onVisibilityChanged(v);
			if( visible ) {
				time = SystemClock.elapsedRealtime();
				nextFrame();
			} else {
				stopRunnable();
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height )
		{
			super.onSurfaceChanged(holder, format, width, height);
			animation.onSurfaceChanged(holder, format, width, height);
			nextFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder)
		{
			super.onSurfaceCreated(holder);
			animation.onSurfaceCreated(holder);
		}
	
		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder)
		{
			super.onSurfaceDestroyed(holder);
			visible = false;
			animation.onSurfaceDestroyed(holder);
			stopRunnable();
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset )
		{
			super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
			//nextFrame();
		}

		protected void drawFrame( final Canvas c, final float t )
		{
			c.save();
			try {
			animation.draw(c);
			if( user_delay > 0 ) {
				delay = user_delay;
			} 
			else
			{
			    delay = animation.getDelay();
			}
			} catch(Exception e) {
				e.printStackTrace();
				Toast.makeText(getContext(),"Exception in drawFrame: "+e, Toast.LENGTH_LONG).show();
				Log.w(TAG, "Exception: "+e);
				stopRunnable();
			}
			c.restore();
		}

		protected void nextFrame()
		{
			stopRunnable();
			if( visible )
				handler.postDelayed( runnable, delay );
			
			if(animation instanceof GLAnimation) {
				delay = animation.getDelay();
				animation.draw(null);
			} else {
				SurfaceHolder holder = null;
				Canvas c = null;
				try
				{
					holder = getSurfaceHolder();
					if( (c = holder.lockCanvas()) != null )
					{
						final long now = SystemClock.elapsedRealtime();
						drawFrame( c, (float)delay/(now-time) );
						time = now;
					}
				} catch(Exception e) {
					e.printStackTrace();
					stopRunnable();
				}
				finally
				{
					if( holder != null && c != null )
						holder.unlockCanvasAndPost( c );
				}
			}
		}

		private void stopRunnable()
		{
			handler.removeCallbacks( runnable );
		}
	}

}
