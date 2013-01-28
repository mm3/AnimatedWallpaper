package com.android.mm3.wallpaper.animated;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.EGLContextFactory;
import android.opengl.GLSurfaceView.EGLWindowSurfaceFactory;
import android.opengl.GLSurfaceView.GLWrapper;
import android.util.Log;
import android.view.SurfaceHolder;

public class GLThread extends Thread {
	static final public String TAG = "GLAnimation";

	
	private final static boolean LOG_THREADS = false;
	public final static int DEBUG_CHECK_GL_ERROR = 1;
	public final static int DEBUG_LOG_GL_CALLS = 2;

	private final GLThreadManager sGLThreadManager = new GLThreadManager();
	private GLThread mEglOwner;

	private EGLConfigChooser mEGLConfigChooser;
	private EGLContextFactory mEGLContextFactory;
	private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
	private GLWrapper mGLWrapper;

	public SurfaceHolder mHolder;
	private boolean mSizeChanged = true;

	// Once the thread is started, all accesses to the following member
	// variables are protected by the sGLThreadManager monitor
	public boolean mDone;
	private boolean mPaused;
	private boolean mHasSurface;
	private boolean mWaitingForSurface;
	private boolean mHaveEgl;
	private int mWidth;
	private int mHeight;
	private int mRenderMode;
	private boolean mRequestRender;
	private boolean mEventsWaiting;
	// End of member variables protected by the sGLThreadManager monitor.

	private GLAnimation.Renderer mRenderer;
	private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
	private EglHelper mEglHelper;

	GLThread(GLAnimation.Renderer renderer, EGLConfigChooser chooser, EGLContextFactory contextFactory,
			EGLWindowSurfaceFactory surfaceFactory, GLWrapper wrapper) {
		super();
		mDone = false;
		mWidth = 0;
		mHeight = 0;
		mRequestRender = true;
		//mRenderMode = GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY;
		mRenderer = renderer;
		this.mEGLConfigChooser = chooser;
		this.mEGLContextFactory = contextFactory;
		this.mEGLWindowSurfaceFactory = surfaceFactory;
		this.mGLWrapper = wrapper;
	}

	@Override
	public void run() {
		setName("GLThread " + getId());
		if (LOG_THREADS) {
			Log.i("GLThread", "starting tid=" + getId());
		}

		try {
			guardedRun();
		} catch (InterruptedException e) {
			// fall thru and exit normally
		} finally {
			sGLThreadManager.threadExiting(this);
		}
	}

	/*
	 * This private method should only be called inside a synchronized(sGLThreadManager) block.
	 */
	private void stopEglLocked() {
		if (mHaveEgl) {
			mHaveEgl = false;
			mEglHelper.destroySurface();
			sGLThreadManager.releaseEglSurface(this);
		}
	}

	private void guardedRun() throws InterruptedException {
		mEglHelper = new EglHelper(mEGLConfigChooser, mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
		try {
			GL10 gl = null;
			boolean tellRendererSurfaceCreated = true;
			boolean tellRendererSurfaceChanged = true;

			/*
			 * This is our main activity thread's loop, we go until asked to quit.
			 */
			while (!isDone()) {
				/*
				 * Update the asynchronous state (window size)
				 */
				int w = 0;
				int h = 0;
				boolean changed = false;
				boolean needStart = false;
				boolean eventsWaiting = false;

				synchronized (sGLThreadManager) {
					while (true) {
						// Manage acquiring and releasing the SurfaceView
						// surface and the EGL surface.
						if (mPaused) {
							stopEglLocked();
						}
						if (!mHasSurface) {
							if (!mWaitingForSurface) {
								stopEglLocked();
								mWaitingForSurface = true;
								sGLThreadManager.notifyAll();
							}
						} else {
							if (!mHaveEgl) {
								if (sGLThreadManager.tryAcquireEglSurface(this)) {
									mHaveEgl = true;
									mEglHelper.start();
									mRequestRender = true;
									needStart = true;
								}
							}
						}

						// Check if we need to wait. If not, update any state
						// that needs to be updated, copy any state that
						// needs to be copied, and use "break" to exit the
						// wait loop.

						if (mDone) {
							return;
						}

						if (mEventsWaiting) {
							eventsWaiting = true;
							mEventsWaiting = false;
							break;
						}

						if ((!mPaused) && mHasSurface && mHaveEgl && (mWidth > 0) && (mHeight > 0)
								&& (mRequestRender )) {
							changed = mSizeChanged;
							w = mWidth;
							h = mHeight;
							mSizeChanged = false;
							mRequestRender = false;
							if (mHasSurface && mWaitingForSurface) {
								changed = true;
								mWaitingForSurface = false;
								sGLThreadManager.notifyAll();
							}
							break;
						}

						// By design, this is the only place where we wait().

						if (LOG_THREADS) {
							Log.i("GLThread", "waiting tid=" + getId());
						}
						sGLThreadManager.wait();
					}
				} // end of synchronized(sGLThreadManager)

				/*
				 * Handle queued events
				 */
				if (eventsWaiting) {
					Runnable r;
					while ((r = getEvent()) != null) {
						r.run();
						if (isDone()) {
							return;
						}
					}
					// Go back and see if we need to wait to render.
					continue;
				}

				if (needStart) {
					tellRendererSurfaceCreated = true;
					changed = true;
				}
				if (changed) {
					gl = (GL10) mEglHelper.createSurface(mHolder);
					tellRendererSurfaceChanged = true;
				}
				if (tellRendererSurfaceCreated) {
					mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
					tellRendererSurfaceCreated = false;
				}
				if (tellRendererSurfaceChanged) {
					mRenderer.onSurfaceChanged(gl, w, h);
					tellRendererSurfaceChanged = false;
				}
				if ((w > 0) && (h > 0)) {
					/* draw a frame here */
					mRenderer.onDrawFrame(gl);

					/*
					 * Once we're done with GL, we need to call swapBuffers() to instruct the system to display the
					 * rendered frame
					 */
					mEglHelper.swap();
					Thread.sleep(10);
				}
			}
		} finally {
			/*
			 * clean-up everything...
			 */
			synchronized (sGLThreadManager) {
				stopEglLocked();
				mEglHelper.finish();
			}
		}
	}

	private boolean isDone() {
		synchronized (sGLThreadManager) {
			return mDone;
		}
	}

	public void setRenderMode(int renderMode) {
		synchronized (sGLThreadManager) {
			mRenderMode = renderMode;
			sGLThreadManager.notifyAll();
		}
	}

	public int getRenderMode() {
		synchronized (sGLThreadManager) {
			return mRenderMode;
		}
	}

	public void requestRender() {
		synchronized (sGLThreadManager) {
			mRequestRender = true;
			sGLThreadManager.notifyAll();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		synchronized (sGLThreadManager) {
			if (LOG_THREADS) {
				Log.i("GLThread", "surfaceCreated tid=" + getId());
			}
			mHasSurface = true;
			sGLThreadManager.notifyAll();
		}
	}

	public void surfaceDestroyed() {
		synchronized (sGLThreadManager) {
			if (LOG_THREADS) {
				Log.i("GLThread", "surfaceDestroyed tid=" + getId());
			}
			mHasSurface = false;
			sGLThreadManager.notifyAll();
			while (!mWaitingForSurface && isAlive() && !mDone) {
				try {
					sGLThreadManager.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public void onPause() {
		synchronized (sGLThreadManager) {
			mPaused = true;
			sGLThreadManager.notifyAll();
		}
	}

	public void onResume() {
		synchronized (sGLThreadManager) {
			mPaused = false;
			mRequestRender = true;
			sGLThreadManager.notifyAll();
		}
	}

	public void onWindowResize(int w, int h) {
		synchronized (sGLThreadManager) {
			mWidth = w;
			mHeight = h;
			mSizeChanged = true;
			sGLThreadManager.notifyAll();
		}
	}

	public void requestExitAndWait() {
		// don't call this from GLThread thread or it is a guaranteed
		// deadlock!
		synchronized (sGLThreadManager) {
			mDone = true;
			sGLThreadManager.notifyAll();
		}
		try {
			join();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Queue an "event" to be run on the GL rendering thread.
	 *
	 * @param r
	 * the runnable to be run on the GL rendering thread.
	 */
	public void queueEvent(Runnable r) {
		synchronized (this) {
			mEventQueue.add(r);
			synchronized (sGLThreadManager) {
				mEventsWaiting = true;
				sGLThreadManager.notifyAll();
			}
		}
	}

	private Runnable getEvent() {
		synchronized (this) {
			if (mEventQueue.size() > 0) {
				return mEventQueue.remove(0);
			}

		}
		return null;
	}

	private class GLThreadManager {

		public synchronized void threadExiting(GLThread thread) {
			if (LOG_THREADS) {
				Log.i("GLThread", "exiting tid=" + thread.getId());
			}
			thread.mDone = true;
			if (mEglOwner == thread) {
				mEglOwner = null;
			}
			notifyAll();
		}

		/*
		 * Tries once to acquire the right to use an EGL surface. Does not block.
		 *
		 * @return true if the right to use an EGL surface was acquired.
		 */
		public synchronized boolean tryAcquireEglSurface(GLThread thread) {
			if (mEglOwner == thread || mEglOwner == null) {
				mEglOwner = thread;
				notifyAll();
				return true;
			}
			return false;
		}

		public synchronized void releaseEglSurface(GLThread thread) {
			if (mEglOwner == thread) {
				mEglOwner = null;
			}
			notifyAll();
		}
	}
	
    
    public static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

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

	
	public static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

		public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay
				display, EGLConfig config, Object nativeWindow) {
			// this is a bit of a hack to work around Droid init problems - if you don't have this, it'll get hung up on orientation changes
			EGLSurface eglSurface = null;
			while (eglSurface == null) {
				try {
					eglSurface = egl.eglCreateWindowSurface(display,
							config, nativeWindow, null);
				} catch (Throwable t) {
				} finally {
					if (eglSurface == null) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException t) {
						}
					}
				}
			}
			return eglSurface;
		}

		public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
			egl.eglDestroySurface(display, surface);
		}
	}

	
	class EglHelper {

		private EGL10 mEgl;
		private EGLDisplay mEglDisplay;
		private EGLSurface mEglSurface;
		private EGLContext mEglContext;
		EGLConfig mEglConfig;

		private EGLConfigChooser mEGLConfigChooser;
		private EGLContextFactory mEGLContextFactory;
		private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
		private GLWrapper mGLWrapper;

		public EglHelper(EGLConfigChooser chooser, EGLContextFactory contextFactory,
				EGLWindowSurfaceFactory surfaceFactory, GLWrapper wrapper) {
			this.mEGLConfigChooser = chooser;
			this.mEGLContextFactory = contextFactory;
			this.mEGLWindowSurfaceFactory = surfaceFactory;
			this.mGLWrapper = wrapper;
		}

		/**
		 * Initialize EGL for a given configuration spec.
		 *
		 * @param configSpec
		 */
		public void start() {
			// Log.d("EglHelper" + instanceId, "start()");
			if (mEgl == null) {
				// Log.d("EglHelper" + instanceId, "getting new EGL");
				/*
				 * Get an EGL instance
				 */
				mEgl = (EGL10) EGLContext.getEGL();
			} else {
				// Log.d("EglHelper" + instanceId, "reusing EGL");
			}

			if (mEglDisplay == null) {
				// Log.d("EglHelper" + instanceId, "getting new display");
				/*
				 * Get to the default display.
				 */
				mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
			} else {
				// Log.d("EglHelper" + instanceId, "reusing display");
			}

			if (mEglConfig == null) {
				// Log.d("EglHelper" + instanceId, "getting new config");
				/*
				 * We can now initialize EGL for that display
				 */
				int[] version = new int[2];
				mEgl.eglInitialize(mEglDisplay, version);
				mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
			} else {
				// Log.d("EglHelper" + instanceId, "reusing config");
			}

			if (mEglContext == null) {
				// Log.d("EglHelper" + instanceId, "creating new context");
				/*
				 * Create an OpenGL ES context. This must be done only once, an OpenGL context is a somewhat heavy object.
				 */
				mEglContext = mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
				if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
					throw new RuntimeException("createContext failed");
				}
			} else {
				// Log.d("EglHelper" + instanceId, "reusing context");
			}

			mEglSurface = null;
		}

		/*
		 * React to the creation of a new surface by creating and returning an OpenGL interface that renders to that
		 * surface.
		 */
		public GL createSurface(SurfaceHolder holder) {
			/*
			 * The window size has changed, so we need to create a new surface.
			 */
			if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {

				/*
				 * Unbind and destroy the old EGL surface, if there is one.
				 */
				mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
				mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
			}

			/*
			 * Create an EGL surface we can render into.
			 */
			mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl, mEglDisplay, mEglConfig, holder);

			if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
				throw new RuntimeException("createWindowSurface failed");
			}

			/*
			 * Before we can issue GL commands, we need to make sure the context is current and bound to a surface.
			 */
			if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
				throw new RuntimeException("eglMakeCurrent failed.");
			}

			GL gl = mEglContext.getGL();
			if (mGLWrapper != null) {
				gl = mGLWrapper.wrap(gl);
			}

			/*
			 * if ((mDebugFlags & (DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS))!= 0) { int configFlags = 0; Writer log =
			 * null; if ((mDebugFlags & DEBUG_CHECK_GL_ERROR) != 0) { configFlags |= GLDebugHelper.CONFIG_CHECK_GL_ERROR; }
			 * if ((mDebugFlags & DEBUG_LOG_GL_CALLS) != 0) { log = new LogWriter(); } gl = GLDebugHelper.wrap(gl,
			 * configFlags, log); }
			 */
			return gl;
		}

		/**
		 * Display the current render surface.
		 *
		 * @return false if the context has been lost.
		 */
		public boolean swap() {
			mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

			/*
			 * Always check for EGL_CONTEXT_LOST, which means the context and all associated data were lost (For instance
			 * because the device went to sleep). We need to sleep until we get a new surface.
			 */
			return mEgl.eglGetError() != EGL11.EGL_CONTEXT_LOST;
		}

		public void destroySurface() {
			if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
				mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
				mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
				mEglSurface = null;
			}
		}

		public void finish() {
			if (mEglContext != null) {
				mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
				mEglContext = null;
			}
			if (mEglDisplay != null) {
				mEgl.eglTerminate(mEglDisplay);
				mEglDisplay = null;
			}
		}
	}
}
