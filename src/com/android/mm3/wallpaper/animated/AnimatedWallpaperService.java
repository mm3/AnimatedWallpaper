package com.android.mm3.wallpaper.animated;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class AnimatedWallpaperService extends WallpaperService {

	static final public String SHARED_PREFERENCES_NAME = "AnimatedWallpaperSettings";
	
	protected Animation animation = new Animation();
	
	@Override
	public Engine onCreateEngine() {
		return new WallpaperEngine();
	}
	
	protected class WallpaperEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener
	{

		protected int delay = 40;

		private Handler handler = new Handler();
		private Runnable runnable = new Runnable(){ public void run() { nextFrame(); 	} };
		private boolean visible = false;
		private long time = 0;

		public WallpaperEngine()
		{
			super();
			PreferenceManager.setDefaultValues(AnimatedWallpaperService.this, R.xml.settings, false );
			SharedPreferences p = AnimatedWallpaperService.this.getSharedPreferences(	SHARED_PREFERENCES_NAME, 0 );
			p.registerOnSharedPreferenceChangeListener( this );
			onSharedPreferenceChanged( p, null );
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences p, String k) {
			String fileName = p.getString( "file_name", "none" );
			if(!fileName.equalsIgnoreCase("none"))
			{
				animation = new GifAnimation(fileName);
			}
		}
		

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			handler.removeCallbacks( runnable );
		}

		@Override
		public void onVisibilityChanged( boolean v )
		{
			visible = v;
			if( visible )
			{
				time = SystemClock.elapsedRealtime();
				nextFrame();
			}
			else
				stopRunnable();
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height )
		{
			nextFrame();
		}

		@Override
		public void onSurfaceDestroyed( SurfaceHolder holder )
		{
			super.onSurfaceDestroyed( holder );
			visible = false;
			stopRunnable();
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset )
		{
			nextFrame();
		}

		protected void drawFrame( final Canvas c, final float t )
		{
			c.save();
			animation.draw(c);
			c.restore();
		}

		protected void nextFrame()
		{
			stopRunnable();
			if( visible )
				handler.postDelayed( runnable, delay );

			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try
			{
				if( (c = holder.lockCanvas()) != null )
				{
					final long now = SystemClock.elapsedRealtime();
					drawFrame( c, (float)delay/(now-time) );
					time = now;
				}
			}
			finally
			{
				if( c != null )
					holder.unlockCanvasAndPost( c );
			}
		}

		private void stopRunnable()
		{
			handler.removeCallbacks( runnable );
		}
	}

}
