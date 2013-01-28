/*
$Id: FreeWRLAssets.java,v 1.9 2012/08/28 15:34:52 crc_canada Exp $

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
import android.content.res.AssetFileDescriptor;
import android.content.Context;

import java.io.IOException;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class FreeWRLAssets {

	private static String TAG = "FreeWRLAssets";
	
	// open a file. 
	// first, see if it is within the FreeWRL apk "assets" directory.
	// second, go through the file system and open it.
	// the FileDescriptor is ONLY used for getting font files from the
	// apk assets directory; libfreetype wants the file compressed
	// and un touched.

	public FreeWRLAssetData openAsset(Context context, String path )
	{
		Integer offset = 0;
		Integer length = 0;
		FileDescriptor fd = null;
		InputStream imgFile = null;
		long f_length = 0;

		// Step 1 - is this in the FreeWRL Assets folder??
		if (path.indexOf('/') == 0) {
			String tryInAppAssets = path.substring(1);

			try {
				AssetFileDescriptor ad = context.getResources().getAssets().openFd(tryInAppAssets);
				if (ad != null) {
					ad.close();
					FreeWRLAssetData rv = new FreeWRLAssetData(context, tryInAppAssets, 1);
					if(null != rv) {
						return rv;
					}
				}
			} catch( IOException e ) {
				Log.e( TAG, "openAsset - not an Asset: " + e.toString() );
			}
		}


		try {
			AssetFileDescriptor ad = null;
			String tryInAppAssets = path;
			if (path.indexOf('/') == 0) tryInAppAssets = path.substring(1);
			ad = context.getResources().getAssets().openFd(tryInAppAssets);
			imgFile = context.getAssets().open(tryInAppAssets);
			fd = ad.getFileDescriptor();
		  	Integer off = (int) ad.getStartOffset();
		  	Integer len = (int) ad.getLength();
			FreeWRLAssetData rv = new FreeWRLAssetData(off,len,fd,imgFile);
			return rv;
		} catch( IOException e ) {
			Log.e( TAG, "openAsset: " + e.toString() );
		}

		InputStream in = null;
		try {
			File f = new File(path);
			InputStream r=new FileInputStream(f);
			in = new BufferedInputStream(r);
			f_length = f.length();
			return new FreeWRLAssetData(0,(int)f_length,fd,in);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Couldn't find or open this file " + path);
			return new FreeWRLAssetData(0,0,null,null);
		}
	}
}
