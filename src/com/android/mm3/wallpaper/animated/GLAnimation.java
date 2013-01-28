package com.android.mm3.wallpaper.animated;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.EGLContextFactory;
import android.opengl.GLSurfaceView.EGLWindowSurfaceFactory;
import android.util.Log;
import android.view.SurfaceHolder;

public class GLAnimation extends Animation {
	static final public String TAG = "GLAnimation";
	
    static FreeWRLAssets fontAsset_01 = null;
    static FreeWRLAssetData fontAssetDatum_01;
    public static boolean currentlyGettingResource = false;
    private static boolean reloadAssetsRequired = false;
	private static String myNewX3DFile = "blankScreen.wrl.mp3";
	private static boolean loadNewX3DFile = false;

	private GLThread mGLThread = null;
	private EGLConfigChooser mEGLConfigChooser;
	private EGLContextFactory mEGLContextFactory;
	private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;



    private Context context = null;
	public GLAnimation(Context c, String s, int style) {
		super(style);
		Log.d(TAG,"GLAnimation");
		init(c, s);
	}
	
	private void init(Context c, String s) {
		Log.d(TAG,"GLAnimation init");
		
		this.context = c;
		
		FreeWRLLib.createInstance();
		Log.d(TAG,"FreeWRLLib.createInstance();");
		if (fontAsset_01 == null) {
			Log.d(TAG,"creating font assets");
			fontAsset_01 = new FreeWRLAssets();
		}
		Log.d(TAG,"fontAsset_01 = new FreeWRLAssets();");
		fontAssetDatum_01 = fontAsset_01.openAsset(c.getApplicationContext(),"fonts/Vera.ttf.mp3");
		Log.d(TAG,"fontAssetDatum_01 = fontAsset_01.openAsset");
		int res = FreeWRLLib.sendFontFile(01,fontAssetDatum_01.fd, (int) fontAssetDatum_01.offset, fontAssetDatum_01.length);
		Log.d(TAG,"int res = FreeWRLLib.sendFontFile");
		FreeWRLLib.setTmpDir(c.getApplicationContext().getCacheDir().getAbsolutePath());
		Log.d(TAG,"FreeWRLLib.setTmpDir(c.getApplicationContext().getCacheDir().getAbsolutePath());");
		myNewX3DFile = s;
		FreeWRLLib.replaceWorldNeeded();
		Log.d(TAG,"FreeWRLLib.replaceWorldNeeded();");
		loadNewX3DFile = true;
		//FreeWRLLib.initialFile(s);
		//Log.d(TAG,"FreeWRLLib.initialFile(s);");
		
		if (mEGLConfigChooser == null) {
			mEGLConfigChooser = new GLThread.ConfigChooser(8, 8, 8, 8, 16, 0);
			Log.d(TAG,"mEGLConfigChooser = new GLThread.ConfigChooser(8, 8, 8, 8, 16, 0);");
		}
		if (mEGLContextFactory == null) {
			mEGLContextFactory = new ContextFactory();
			Log.d(TAG,"mEGLContextFactory = new ContextFactory();");
		}
		if (mEGLWindowSurfaceFactory == null) {
			mEGLWindowSurfaceFactory = new GLThread.DefaultWindowSurfaceFactory();
			Log.d(TAG,"mEGLWindowSurfaceFactory = new GLThread.DefaultWindowSurfaceFactory();");
		}
		mGLThread = new GLThread(new Renderer(), mEGLConfigChooser, mEGLContextFactory, mEGLWindowSurfaceFactory, null);
		Log.d(TAG,"mGLThread = new GLThread(new Renderer(), ");
		mGLThread.start();
		Log.d(TAG,"mGLThread.start();");
	}
	
	@Override
	public void draw (Canvas c) {
		// Do nothing.
		//Log.d(TAG,"draw");
		new Thread(Timer_Tick).start();
	}

	@Override
	public int getDelay() 	{
		//Log.d(TAG,"getDelay");
		return 100;
	}

	@Override
	public void onSurfaceCreated(SurfaceHolder holder) {
		Log.d(TAG,"onSurfaceCreated");
		mGLThread.surfaceCreated(holder);
	}
	
	@Override
	public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG,"onSurfaceChanged");
		mGLThread.onWindowResize(width, height);
	}
	
	@Override
	public void onSurfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG,"onSurfaceDestroyed");
		mGLThread.surfaceDestroyed();
	}
	
	@Override
	public void onVisibilityChanged(boolean v) {
		Log.d(TAG,"onVisibilityChanged");
		if (v) {
			mGLThread.onResume();
		} else {
			mGLThread.onPause();
		}
	}

	@Override
	public void onDestroy() {
		Log.d(TAG,"onDestroy");
		FreeWRLLib.doQuitInstance();
		mGLThread.requestExitAndWait();
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			//Log.d(TAG,"Timer_Tick run");
			if (FreeWRLLib.resourceWanted()&& (!currentlyGettingResource)) {
				// we are getting a resource...
				currentlyGettingResource = true;

				Log.d(TAG, "FreeWRLLib resource wanted");
				FreeWRLAssetGetter task = new FreeWRLAssetGetter();
				task.sendInContext(context.getApplicationContext());
				task.execute (new String (FreeWRLLib.resourceNameWanted()));
			}
			if (FreeWRLLib.androidGetUnreadMessageCount() > 0) {
				String retString = FreeWRLLib.androidGetLastMessage(0);
				Log.d(TAG, "FreeWRLLib Message" + retString);
			}
		}
	};

    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
        	Log.d(TAG, "creating OpenGL ES 2.0 context");
            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
	    //Log.w(TAG, "after egl.eglCreateContext, we have context " + context);
            checkEglError("After eglCreateContext", egl);

            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
		Log.d(TAG,"destroyContext called");
		egl.eglDestroyContext(display, context);
		reloadAssetsRequired = true;
        }
    }

    private static void checkEglError(String prompt, EGL10 egl) {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
        }
    }

	public static class Renderer implements GLSurfaceView.Renderer {
	
		// keep track of the assets directory
		//static FreeWRLAssets myAsset = null;
	
		// Fonts
		static FreeWRLAssets fontAsset_01 = null; 
		static FreeWRLAssetData fontAssetSize_01;
		
		public void onDrawFrame(GL10 gl) {
			//Log.d(TAG,"Renderer -  onDrawFrame");
			if (loadNewX3DFile) {
				Log.d(TAG,"onDrawFrame, new file - "+myNewX3DFile);
				loadNewX3DFile = false;
				FreeWRLLib.initialFile(myNewX3DFile);
			}
			
			if (reloadAssetsRequired) {
				Log.d(TAG,"onDrawFrame, reloadAssets required");
				FreeWRLLib.reloadAssets();
				reloadAssetsRequired = false;
			}
			FreeWRLLib.step();
			//Log.d(TAG,"Renderer - FreeWRLLib.step();");
		}

        public void onSurfaceChanged(GL10 gl, int width, int height) {
        	Log.d(TAG, "-------------onSurfaceChanged");
            FreeWRLLib.init(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        	Log.d(TAG, "--------------onSurfaceCreated");
        }
	} // end of class Renderer

	
}
