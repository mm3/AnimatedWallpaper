/*
  $Id: FreeWRLView.java,v 1.13 2012/09/05 16:01:10 crc_canada Exp $

*/

/****************************************************************************
    This file is part of the FreeWRL/FreeX3D Distribution.

    Copyright 2012 CRC Canada. (http://www.crc.gc.ca)

    FreeWRL/FreeX3D is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FreeWRL/FreeX3D is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FreeWRL/FreeX3D.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************/

package com.android.mm3.wallpaper.animated;


import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileDescriptor;
import android.util.Log;

import android.view.ScaleGestureDetector;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.os.AsyncTask; //<Params, Progress, Result>;


/*
// for assets/fonts
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
*/


/**
 * A simple GLSurfaceView sub-class that demonstrate how to perform
 * OpenGL ES 2.0 rendering into a GL Surface. Note the following important
 * details:
 *
 * - The class must use a custom context factory to enable 2.0 rendering.
 *   See ContextFactory class definition below.
 *
 * - The class must use a custom EGLConfigChooser to be able to select
 *   an EGLConfig that supports 2.0. This is done by providing a config
 *   specification to eglChooseConfig() that has the attribute
 *   EGL10.ELG_RENDERABLE_TYPE containing the EGL_OPENGL_ES2_BIT flag
 *   set. See ConfigChooser class definition below.
 *
 * - The class must select the surface's format, then choose an EGLConfig
 *   that matches it exactly (with regards to red/green/blue/alpha channels
 *   bit depths). Failure to do so would result in an EGL_BAD_MATCH error.
 */
class FreeWRLView extends GLSurfaceView {
    private static String TAG = "FreeWRLView";
    private static final boolean DEBUG = false;
    private static Context myContext;

    // if we loose the context, we will have to reload OpenGL ES resources
    // within FreeWRL (shaders, buffers, etc) on restart.
    private static boolean reloadAssetsRequired = false;

//gesture stuff
    private static final int INVALID_POINTER_ID = -1;
    private float mPosX;
    private float mPosY;
   
    private float mLastTouchX;
    private float mLastTouchY;

    private float gestureInitPosX;
    private float gestureInitPosY;
    private float gestureInitSpan;

    private int mActivePointerId = INVALID_POINTER_ID;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
	private static final int KeyPress        =2;
	private static final int KeyRelease      =3;
	private static final int ButtonPress     =4;
	private static final int ButtonRelease   =5;
	private static final int MotionNotify    =6;
	private static final int MapNotify       =19;

	public static boolean poisedForAction = false;
// end gesture stuff

	// starting file - we default to a "splash x3d file" in the assets dir.
	private static String myNewX3DFile = "blankScreen.wrl.mp3";
	//private static String myNewX3DFile = "/mnt/sdcard/1.wrl";
	private static String possibleNewX3DFile = null;
	private static boolean loadNewX3DFile = false;


    public FreeWRLView(Context context) {
        super(context);
        // original - init(false, 0, 0);
        //works - init(true, 8, 0);
        //works - init(true, 16, 0);
        // fails - init(true, 24, 0);

        init(true, 16, 0);
	myContext = context;
	mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public FreeWRLView(Context context, boolean translucent, int depth, int stencil) {
        super(context);
        init(translucent, depth, stencil);
	myContext = context;
	mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    private void init(boolean translucent, int depth, int stencil) {
	//Log.w(TAG, "----------------- init method called");
        /* By default, GLSurfaceView() creates a RGB_565 opaque surface.
         * If we want a translucent one, we should change the surface's
         * format here, using PixelFormat.TRANSLUCENT for GL Surfaces
         * is interpreted as any 32-bit surface with alpha by SurfaceFlinger.
         */
        if (translucent) {
            this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }

        /* Setup the context factory for 2.0 rendering.
         * See ContextFactory class definition below
         */
        setEGLContextFactory(new ContextFactory());

        /* We need to choose an EGLConfig that matches the format of
         * our surface exactly. This is going to be done in our
         * custom config chooser. See ConfigChooser class definition
         * below.
         */
        setEGLConfigChooser( translucent ?
                             new ConfigChooser(8, 8, 8, 8, depth, stencil) :
                             new ConfigChooser(5, 6, 5, 0, depth, stencil) );

        /* Set the renderer responsible for frame rendering */
        setRenderer(new Renderer());
    }

    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
	    //Log.w(TAG, "creating OpenGL ES 2.0 context");
            checkEglError("Before eglCreateContext", egl);
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
	    //Log.w(TAG, "after egl.eglCreateContext, we have context " + context);
            checkEglError("After eglCreateContext", egl);

            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
		//Log.w(TAG,"destroyContext called");
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

	//onCreate called - we need to initialize FreeWRL, but can't do it until the context is
	//stable, so we just store the file name here.
	public void setPossibleNewFileName(String fn) {
		possibleNewX3DFile = fn;
		//Log.w(TAG,"setNewFileName - setting file name to " + fn);
	}

	public void discardPossibleNewFileName() {
		possibleNewX3DFile = null;
	}

	// Ok - initiate a new load here.
	public void setLoadNewX3DFile() {
		// did we have a new file name here?
		if (possibleNewX3DFile != null) {
			myNewX3DFile = possibleNewX3DFile;
			FreeWRLLib.replaceWorldNeeded();
			possibleNewX3DFile = null;
		}
		loadNewX3DFile = true;
	}



	// touch events (gestures)
	// note the "poisedForAction" - we delay initial sending of "ButtonDown" messages
	// so that we can see if it is a one or two finger salute given to us by the user.

public boolean onTouchEvent(final MotionEvent ev) {
		//Log.w(TAG,"-------------glScreen-onTouchEvent");

	// get the touch event. 
        mScaleDetector.onTouchEvent(ev);
        
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {

	// single or double finger action/gesture start
        case MotionEvent.ACTION_DOWN: {
		mLastTouchX = ev.getX();
		mLastTouchY = ev.getY();
		mActivePointerId = ev.getPointerId(0);
		//Log.w(TAG,".... ACTION_DOWN, x " + mLastTouchX + " y " + mLastTouchY + " mActivePointerId " + mActivePointerId);
		poisedForAction = true;
            	break;
        }
            
	// finger movement - double or single finger moves.
        case MotionEvent.ACTION_MOVE: {
		final int pointerIndex = ev.findPointerIndex(mActivePointerId);
		final float x = ev.getX(pointerIndex);
		final float y = ev.getY(pointerIndex);

		// Only move if the ScaleGestureDetector isn't processing a gesture.
		if (!mScaleDetector.isInProgress()) {
			//Log.w(TAG,".... ACTION_MOVE, ScaleDetector NOT InProgress");
			final float dx = x - mLastTouchX;
			final float dy = y - mLastTouchY;

			mPosX += dx;
			mPosY += dy;

			// IS the first move since we started the ACTON?
			if (poisedForAction) {
				FreeWRLLib.setButDown(1, 1);
				FreeWRLLib.setLastMouseEvent(ButtonPress);
				FreeWRLLib.handleAqua(ButtonPress,1,(int)mLastTouchX,(int)mLastTouchY);
				poisedForAction = false;
			} else {
				FreeWRLLib.setLastMouseEvent(MotionNotify);
				FreeWRLLib.handleAqua(MotionNotify,1,(int)mLastTouchX,(int)mLastTouchY);
				//Log.w(TAG,".... But 1 ACTION_MOVE, x " + mLastTouchX + " y " + mLastTouchY + " mActivePointerId " + mActivePointerId);
			}

        	        invalidate();
		} else {
		}

	
		// record where we left this touch event.
		mLastTouchX = x;
		mLastTouchY = y;

		break;
	}
            
	// single finger release
	case MotionEvent.ACTION_UP: {
		mActivePointerId = INVALID_POINTER_ID;
		FreeWRLLib.setButDown(1, 0);
		FreeWRLLib.setLastMouseEvent(ButtonRelease);
		FreeWRLLib.handleAqua(ButtonRelease,1,(int)mLastTouchX,(int)mLastTouchY);
		poisedForAction = false;
		//Log.w(TAG,".... But 1 ACTION_UP  x " + mLastTouchX + " y " + mLastTouchY + " mActivePointerId " + mActivePointerId);
		break;
	}
            
	// never seen this - but set everything back to "null" state.
	case MotionEvent.ACTION_CANCEL: {
		mActivePointerId = INVALID_POINTER_ID;
		poisedForAction = false;
		FreeWRLLib.setButDown(1, 0);
		FreeWRLLib.setButDown(3, 0);
		FreeWRLLib.setLastMouseEvent(ButtonRelease);
		FreeWRLLib.handleAqua(ButtonRelease,1,(int)mLastTouchX,(int)mLastTouchY);
		FreeWRLLib.handleAqua(ButtonRelease,3,(int)mLastTouchX,(int)mLastTouchY);
		//Log.w(TAG,".... But 1 But 3 ACTION_CANCEL, x " + mLastTouchX + " y " + mLastTouchY + " mActivePointerId " + mActivePointerId);
		break;
	}

	case MotionEvent.ACTION_POINTER_UP: {
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
				>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);

		poisedForAction = false;
		if (pointerId == mActivePointerId) {
			//Log.w (TAG,".... ACTION_POINTER_UP, pointerDI eq mActivePointerID");

			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastTouchX = ev.getX(newPointerIndex);
			mLastTouchY = ev.getY(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
		} else {
			//Log.w(TAG,"but 3 ACTION_POINTER_UP, not eq pointerID");
		}
		FreeWRLLib.setButDown(3, 0);
		FreeWRLLib.setLastMouseEvent(ButtonRelease);
		FreeWRLLib.handleAqua(ButtonRelease,3,(int)mLastTouchX,(int)mLastTouchY);
		//Log.w(TAG,".... ACTION_POINTER_UP, x " + mLastTouchX + " y " + mLastTouchY + " mActivePointerId " + mActivePointerId);

		break;
	}
	}
	return true;
}


private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scaledX, scaledY;
		float ANDROID_TOUCH_DIV = 100.0f;

		if (poisedForAction) {
			poisedForAction = false;
			gestureInitPosX = mLastTouchX;
			gestureInitPosY = mLastTouchY;
			gestureInitSpan = detector.getCurrentSpan();
			FreeWRLLib.setButDown(3, 1);
			FreeWRLLib.setLastMouseEvent(ButtonPress);
			scaledX = gestureInitPosX * ((detector.getCurrentSpan() - gestureInitSpan)/ANDROID_TOUCH_DIV);
			scaledY = gestureInitPosY * ((detector.getCurrentSpan() - gestureInitSpan)/ANDROID_TOUCH_DIV);
			FreeWRLLib.handleAqua(ButtonPress,3,(int)scaledX,(int)scaledY);

			//Log.w(TAG,"scaleFactor start - x " + mLastTouchX + " y " + mLastTouchY + " span " + gestureInitSpan);
		} else {
			scaledX = gestureInitPosX * ((detector.getCurrentSpan() - gestureInitSpan)/ANDROID_TOUCH_DIV);
			scaledY = gestureInitPosY * ((detector.getCurrentSpan() - gestureInitSpan)/ANDROID_TOUCH_DIV);
			FreeWRLLib.handleAqua(MotionNotify,3,(int)scaledX,(int)scaledY);
			//Log.w(TAG,"scaleFactor " + mScaleFactor + " detector.getsCurrentSpan " + detector.getCurrentSpan() + " x,y" + scaledX + " " + scaledY);

			// keep the updated position for future moves, etc
			mLastTouchX = scaledX;
			mLastTouchY = scaledY;
			invalidate();
		}
		return true;
	}
} // end of class ScaleListener


private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

        public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
            mRedSize = r;
            mGreenSize = g;
            mBlueSize = b;
            mAlphaSize = a;
            mDepthSize = depth;
            mStencilSize = stencil;
        }

        /* This EGL config specification is used to specify 2.0 rendering.
         * We use a minimum size of 4 bits for red/green/blue, but will
         * perform actual matching in chooseConfig() below.
         */
        private static int EGL_OPENGL_ES2_BIT = 4;
        private static int[] s_configAttribs2 =
        {
            EGL10.EGL_RED_SIZE, 4,
            EGL10.EGL_GREEN_SIZE, 4,
            EGL10.EGL_BLUE_SIZE, 4,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_NONE
        };

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

            /* Get the number of minimally matching EGL configurations
             */
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                //throw new IllegalArgumentException("No configs match configSpec");
		Log.e(TAG,"Need OpenGL ES 2.0 - not found");
		return null;
            }

            /* Allocate then read the array of minimally matching EGL configs
             */
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config);

            if (DEBUG) {
                 printConfigs(egl, display, configs);
            }
            /* Now return the "best" one
             */
            return chooseConfig(egl, display, configs);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs) {
            for(EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);

                // We need at least mDepthSize and mStencilSize bits
                if (d < mDepthSize || s < mStencilSize)
                    continue;

                // We want an *exact* match for red/green/blue/alpha
                int r = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);

                if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
                    return config;
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private void printConfigs(EGL10 egl, EGLDisplay display,
            EGLConfig[] configs) {
            int numConfigs = configs.length;
            Log.w(TAG, String.format("%d configurations", numConfigs));
            for (int i = 0; i < numConfigs; i++) {
                Log.w(TAG, String.format("Configuration %d:\n", i));
                printConfig(egl, display, configs[i]);
            }
        }

        private void printConfig(EGL10 egl, EGLDisplay display,
                EGLConfig config) {
            int[] attributes = {
                    EGL10.EGL_BUFFER_SIZE,
                    EGL10.EGL_ALPHA_SIZE,
                    EGL10.EGL_BLUE_SIZE,
                    EGL10.EGL_GREEN_SIZE,
                    EGL10.EGL_RED_SIZE,
                    EGL10.EGL_DEPTH_SIZE,
                    EGL10.EGL_STENCIL_SIZE,
                    EGL10.EGL_CONFIG_CAVEAT,
                    EGL10.EGL_CONFIG_ID,
                    EGL10.EGL_LEVEL,
                    EGL10.EGL_MAX_PBUFFER_HEIGHT,
                    EGL10.EGL_MAX_PBUFFER_PIXELS,
                    EGL10.EGL_MAX_PBUFFER_WIDTH,
                    EGL10.EGL_NATIVE_RENDERABLE,
                    EGL10.EGL_NATIVE_VISUAL_ID,
                    EGL10.EGL_NATIVE_VISUAL_TYPE,
                    0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
                    EGL10.EGL_SAMPLES,
                    EGL10.EGL_SAMPLE_BUFFERS,
                    EGL10.EGL_SURFACE_TYPE,
                    EGL10.EGL_TRANSPARENT_TYPE,
                    EGL10.EGL_TRANSPARENT_RED_VALUE,
                    EGL10.EGL_TRANSPARENT_GREEN_VALUE,
                    EGL10.EGL_TRANSPARENT_BLUE_VALUE,
                    0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
                    0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                    0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
                    0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
                    EGL10.EGL_LUMINANCE_SIZE,
                    EGL10.EGL_ALPHA_MASK_SIZE,
                    EGL10.EGL_COLOR_BUFFER_TYPE,
                    EGL10.EGL_RENDERABLE_TYPE,
                    0x3042 // EGL10.EGL_CONFORMANT
            };
            String[] names = {
                    "EGL_BUFFER_SIZE",
                    "EGL_ALPHA_SIZE",
                    "EGL_BLUE_SIZE",
                    "EGL_GREEN_SIZE",
                    "EGL_RED_SIZE",
                    "EGL_DEPTH_SIZE",
                    "EGL_STENCIL_SIZE",
                    "EGL_CONFIG_CAVEAT",
                    "EGL_CONFIG_ID",
                    "EGL_LEVEL",
                    "EGL_MAX_PBUFFER_HEIGHT",
                    "EGL_MAX_PBUFFER_PIXELS",
                    "EGL_MAX_PBUFFER_WIDTH",
                    "EGL_NATIVE_RENDERABLE",
                    "EGL_NATIVE_VISUAL_ID",
                    "EGL_NATIVE_VISUAL_TYPE",
                    "EGL_PRESERVED_RESOURCES",
                    "EGL_SAMPLES",
                    "EGL_SAMPLE_BUFFERS",
                    "EGL_SURFACE_TYPE",
                    "EGL_TRANSPARENT_TYPE",
                    "EGL_TRANSPARENT_RED_VALUE",
                    "EGL_TRANSPARENT_GREEN_VALUE",
                    "EGL_TRANSPARENT_BLUE_VALUE",
                    "EGL_BIND_TO_TEXTURE_RGB",
                    "EGL_BIND_TO_TEXTURE_RGBA",
                    "EGL_MIN_SWAP_INTERVAL",
                    "EGL_MAX_SWAP_INTERVAL",
                    "EGL_LUMINANCE_SIZE",
                    "EGL_ALPHA_MASK_SIZE",
                    "EGL_COLOR_BUFFER_TYPE",
                    "EGL_RENDERABLE_TYPE",
                    "EGL_CONFORMANT"
            };
            int[] value = new int[1];
            for (int i = 0; i < attributes.length; i++) {
                int attribute = attributes[i];
                String name = names[i];
                if ( egl.eglGetConfigAttrib(display, config, attribute, value)) {
                    Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
                } else {
                    Log.w(TAG, String.format("  %s: failed\n", name));
                    while (egl.eglGetError() != EGL10.EGL_SUCCESS);
                }
            }
        }

        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];
} // end of class ConfigChooser

private static class Renderer implements GLSurfaceView.Renderer {

	// keep track of the assets directory
	//static FreeWRLAssets myAsset = null;

	// Fonts
	static FreeWRLAssets fontAsset_01 = null; 
	static FreeWRLAssetData fontAssetSize_01;


	
	public void onDrawFrame(GL10 gl) {
		// do the draw
		if (loadNewX3DFile) {
			//Log.w(TAG,"onDrawFrame, new file");
			//Log.w(TAG,myNewX3DFile);
			loadNewX3DFile = false;
			FreeWRLLib.initialFile(myNewX3DFile);
		}

		if (reloadAssetsRequired) {
			//Log.w(TAG,"onDrawFrame, reloadAssets required");
			FreeWRLLib.reloadAssets();
			reloadAssetsRequired = false;
		}
		FreeWRLLib.step();
	}

        public void onSurfaceChanged(GL10 gl, int width, int height) {
		//Log.w(TAG, "-------------onSurfaceChanged");
            FreeWRLLib.init(width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//Log.w(TAG, "--------------onSurfaceCreated");
        }
} // end of class Renderer
}
