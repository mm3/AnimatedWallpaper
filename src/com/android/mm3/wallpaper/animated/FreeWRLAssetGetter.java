/*
  $Id: FreeWRLAssetGetter.java,v 1.11 2012/08/28 15:34:52 crc_canada Exp $

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


/* Notes:

An Asset, in FreeWRL terms, is a resource (vrml file, jpg file, etc) that resides
*SOMEWHERE*.

It can reside within the FreeWRL apk (eg, font files reside there), or it can
reside on the SD card.

We ALWAYS look in the assets in the apk file first; if not there, then we go elsewhere.

*/

package com.android.mm3.wallpaper.animated;

import android.util.Log;
import android.content.Context;
import android.os.Looper;

import android.os.AsyncTask; //<Params, Progress, Result>;

public class FreeWRLAssetGetter extends AsyncTask<String, String, String> {

private static String TAG="FreeWRLAssetGetter";
private static Context myContext= null;
private static FreeWRLAssets myAsset = null;
FreeWRLAssetData myAssetDatum;

	public void sendInContext(Context c) {
		myContext = c;
	}

	@Override
	protected String doInBackground(String... wantedName) {
		FreeWRLAssetData myAssetDatum;

		// local munged copy of the requested file name.
		String myName  = wantedName[0];

		if (Looper.myLooper () == null) {
			//Log.w(TAG,"FreeWRLAssetGetter, no looper yet");
			Looper.prepare();
		}

/*
		Log.w(TAG,"now, RESOURCE Wanted name is " + myName);

                Log.w(TAG, "file " + new Throwable().getStackTrace()[0].getFileName() +
                        " class " + new Throwable().getStackTrace()[0].getClassName() +
                        " method " + new Throwable().getStackTrace()[0].getMethodName() +
                        " line " + new Throwable().getStackTrace()[0].getLineNumber());
                Log.w(TAG, "From : " +
                        " class " + new Throwable().getStackTrace()[1].getClassName() +
                        " method " + new Throwable().getStackTrace()[1].getMethodName() +
                        " line " + new Throwable().getStackTrace()[1].getLineNumber());
*/

		myAsset = new FreeWRLAssets();
		myAssetDatum = myAsset.openAsset(myContext,myName);
		//Log.w(TAG,"-------------and, the getStartOffset, getLength is " + myAssetDatum.offset + "  " + myAssetDatum.length);

		// send this to FreeWRL
		FreeWRLLib.resourceFile(
			myAssetDatum.myBytes, 
			myAssetDatum.imageWidth, 
			myAssetDatum.imageHeight, 
			myAssetDatum.hasAlpha);
		//Log.w(TAG,"------------resourceFile NDK call returns " + res);

		return myName;
	}

	@Override
	protected void onPostExecute(String result) {
		//Log.w(TAG, "FreeWRLAssetGetter onPostExecute done - string "+ result);
		FreeWRLActivity.currentlyGettingResource=false;

	}
}

