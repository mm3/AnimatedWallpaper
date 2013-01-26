/*
  $Id: FreeWRLActivity.java,v 1.28 2012/09/18 19:39:45 crc_canada Exp $

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

/***** ActionBarSherlock ****/
//import com.actionbarsherlock.app.SherlockActivity;
//import com.actionbarsherlock.app.ActionBar;
//import com.actionbarsherlock.view.Menu;
//import com.actionbarsherlock.view.MenuItem;
//import com.actionbarsherlock.view.MenuInflater;

//import android.view.MenuInflater;
//import android.view.Menu;
//import android.view.MenuItem;
/***** END ActionBarSherlock ****/



import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import java.util.List;
import android.content.IntentFilter;
import android.content.Context;


import java.util.Timer;
import java.util.TimerTask;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

// File Dialog 2
import java.io.File;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.ListView;
import android.os.Environment;
// File Dialog 2

import android.os.Looper;

// logcat stuff
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
// end logcat stuff

// for removing local file and console Views
import android.view.View;

//ActionBarSherlock... public class FreeWRLActivity extends Activity implements IFolderItemListener {
public class FreeWRLActivity extends Activity {
	static FreeWRLView glView = null;
	//static LinearLayout mainView;
	static RelativeLayout mainView;
	

	private static String TAG = "FreeWRLActivity";

	// are we currently getting a resource? if so, we just ignore 
	// front end request for a file, because the requests are synchronous.
	public static boolean currentlyGettingResource = false;

	// timer trials
	private static Timer myTimer = null;

        // Fonts
        static FreeWRLAssets fontAsset_01 = null;
        static FreeWRLAssetData fontAssetDatum_01;

	// Cannot open Folder
	public void OnCannotFileRead(File file) {
		new AlertDialog.Builder(this)
		.setTitle(
		"[" + file.getName()
		+ "] folder can't be read!")
		.setPositiveButton("OK",
		new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog,
				int which) {

				}
			}).show();

	}


private final String[] LOGCAT_CMD = new String[] { "logcat", "" };
public static final String[] LOGCAT_CLEAR_CMD = new String[] { "logcat", "-c" };
private Process mLogcatProc = null;
private BufferedReader reader = null;
private final int BUFFER_SIZE = 1024;

// where do we look for files on the Android device?
private static String lastDirectoryBrowsed = null;

// get the last so many console messages. Message 0 is most recent, 1 is prev, 2, prev
// to that, etc. etc.

private String getLastConsoleMessages() {
	String retString = FreeWRLLib.androidGetLastMessage(0) + 
			"\n(previous):\n" + FreeWRLLib.androidGetLastMessage(1) +
			"\n(previous):\n" + FreeWRLLib.androidGetLastMessage(2) +
			"\n(previous):\n" + FreeWRLLib.androidGetLastMessage(3);
	//Log.w(TAG,"getLastConsoleMessages returns: " + retString);
	return retString;
}

/*

@Override
public boolean onCreateOptionsMenu(Menu menu) {

	if (glView != null) {

	// ActionBarSherlock...MenuInflater inflater = getMenuInflater();
	MenuInflater inflater = getSupportMenuInflater(); //ActionBarSherlock


	inflater.inflate(R.menu.main_activity, menu);
	//Log.w(TAG,"onCreateOptionsMenu called");
	}

	return true;
}
*/

/*

@Override
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case R.id.NEW_WORLD:
			Context origContext = getApplication();

			// Actions in case that Edid Contacts is pressed 
			//Log.w(TAG,"NEW_WORLD");
			// File Dialog 2
			if (localFolders != null) localFolders.setVisibility(View.INVISIBLE);
			localFolders = new FolderLayout(getApplication(),null);

			//Log.w(TAG, "2 going to findViewById");
			localFolders.setIFolderItemListener(this);

			//Log.w(TAG, "3 going to findViewById");
			if (lastDirectoryBrowsed == null) 
				lastDirectoryBrowsed = Environment.getExternalStorageDirectory().getPath();

			//localFolders.setDir(Environment.getExternalStorageDirectory().getPath());
			localFolders.setDir(lastDirectoryBrowsed);

			// set the background colour - let FreeWRL show through sometimes.
			localFolders.setBackgroundColor(0xAF000000 );

			// display it
			viewStack.push(localFolders);
			//Log.w(TAG, "onOpetionsIntemSelected, pushing " + viewStack.peek());
			getWindow().addContentView(localFolders, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

			break;
		case R.id.VIEWPOINT : {
			//Log.w(TAG,"VIEWPOINT_CHANGE");
			// Actions in case that Delete Contact is pressed 
			FreeWRLLib.nextViewpoint();
			break;
		}

		case R.id.PREFERENCES: {
			Log.w(TAG,"PREFERENCES");
			break;
		}

		case R.id.SETTINGS: {
			Log.w(TAG,"SETTINGS");
			displaySettings();
			break;
		}

		case R.id.LOG_LOOK : {
			displayConsole();
			break;

		}
    }
	return true;
}
*/

//// user hit the menu button - display our main selections.


    @Override protected void onCreate(Bundle icicle) {
	//Log.w(TAG,"onCreate");
        super.onCreate(icicle);

	setContentView(R.layout.activity_settings_wallpaper);

	//mainView = (LinearLayout)findViewById(R.id.MainView);
	
	mainView = (RelativeLayout)findViewById(R.id.view_picture);
	//Log.w(TAG, "onCreate pushing " + viewStack.peek());


	// Check at init whether we can really do OpenGL ES2.0
	final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
	final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

	/*
	if (supportsEs2) {
		Log.w(TAG, "supportsEs2 TRUE");
	} else {
		Log.w(TAG,"supportsEs2 FALSE");
	}
	*/

	if (supportsEs2) {
	        glView = new FreeWRLView(getApplication());
	
		//Log.w(TAG,"glView is " + glView);
	
		// tell the library to (re)create it's internal databases
		FreeWRLLib.createInstance();
	
		// for gestures
		//	glView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	
		// add the glView here.
	mainView.addView((View)glView,0,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	
		//Log.w(TAG,"setContentView on glView");
		// now we have a main view, and glView is a sub-view...	setContentView(glView);
	
		// send in font directory pointers.
		if (fontAsset_01 == null) {
			//Log.w(TAG,"creating font assets");
			fontAsset_01 = new FreeWRLAssets();
		}
	
		// send in the font file descriptor on create.
		fontAssetDatum_01 = fontAsset_01.openAsset(getApplicationContext(),"fonts/Vera.ttf.mp3");
		int res = FreeWRLLib.sendFontFile(01,fontAssetDatum_01.fd,
			(int) fontAssetDatum_01.offset, fontAssetDatum_01.length);
		
		//Log.w(TAG,"---- assets for Vera.ttf; " + fontAssetDatum_01.length);
	
		// send in the temp file, used by FreeWRL for creating tmp files, what else?
		FreeWRLLib.setTmpDir(getApplicationContext().getCacheDir().getAbsolutePath());

		//Log.w(TAG,"cache dir is " + getApplicationContext().getCacheDir().getAbsolutePath());
	
		glView.setLoadNewX3DFile();
	
		//Log.w(TAG,"starting timer task");
		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}
	
		// do it 30 times per second (1/30)
		}, 0, 33);
	} else {
		 // no GLES2
		glView = null;
		Log.w (TAG, "no OpenGL ES 2.0 config available");

//		TextView myPath = (TextView) findViewById(R.id.rowtext);
//		       myPath.setText("CAN NOT DO OPENGL ES 2 ON THIS PHONE");
	}
}



    @Override protected void onRestart() {
	//Log.w (TAG,"onRestart");
        super.onRestart();
    }

    @Override protected void onStop() {
	//Log.w (TAG,"onStop");
        super.onStop();
    }

    @Override protected void onStart() {
	//Log.w (TAG,"onStart");
        super.onStart();
    }

    @Override protected void onDestroy() {
	//Log.w (TAG,"onDestroy");
        super.onDestroy();
	FreeWRLLib.doQuitInstance();
	//Log.w (TAG,"FreeWRL onDestroyed");
    }

    @Override protected void onPause() {
	//Log.w (TAG,"onPause");
        super.onPause();
        if (glView != null) glView.onPause();
    }

    @Override protected void onResume() {
	//Log.w (TAG,"onResume");
        super.onResume();
        if (glView != null) glView.onResume();
    }




	// timer stuff 
	private void TimerMethod()
	{
		//This method is called directly by the timer
		//and runs in the same thread as the timer.

		//We call the method that will work with the UI
		//through the runOnUiThread method.
		this.runOnUiThread(Timer_Tick);
	}

	private Runnable Timer_Tick = new Runnable() {
		public void run() {

			// This method runs in the same thread as the UI.       
			// it checks if a resource is requested by the back end, and if one is,
			// it checks to see that we have not already started a thread to 
			// satisfy the request. The back end will send only work on one file
			// at a time, so in essence, we just need to keep track of this
			// synchronous stream and thus we'll be getting only one resource at
			// a time, too.

			// Log.w(TAG,"timer tick");
			// do we want a new resource? are we currently NOT getting a resource??
			if (FreeWRLLib.resourceWanted()&& (!currentlyGettingResource)) {
				// we are getting a resource...
				currentlyGettingResource = true;

				FreeWRLAssetGetter task = new FreeWRLAssetGetter();
				task.sendInContext(getApplication());

				// execute the task, and then the ending of the task will 
				// set the currentlyGettingResource to false.
				task.execute (new String (FreeWRLLib.resourceNameWanted()));
			}

			// Do we have any console messages not shown? 
			if (FreeWRLLib.androidGetUnreadMessageCount() > 0) {
//				displayConsole();
			}

		}
	};
}
