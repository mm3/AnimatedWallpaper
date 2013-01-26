/*
  $Id: FreeWRLAssetData.java,v 1.8 2012/09/05 15:06:52 crc_canada Exp $

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

import java.io.BufferedInputStream;

import android.util.Log;
import android.content.Context;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream ;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;

import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;

public class FreeWRLAssetData {
	Integer offset;
	Integer length;
	FileDescriptor fd;
	byte[] myBytes;
	int imageWidth;
	int imageHeight;
	boolean hasAlpha;
	Integer knownType = 0;

	private static String TAG = "FreeWRLAssetData";

        public FreeWRLAssetData (Context myAppContext, String assetName, Integer forceType) {
		Bitmap mybitmap = null;

                this.offset = 0;
		this.length = 0;
                this.fd = null;
                this.myBytes = null; 
                this.imageWidth = -1;
                this.imageHeight = -1;
                this.hasAlpha = false;
                this.knownType = forceType;

                AssetManager am = myAppContext.getAssets();
                if (forceType == 1) {
                        // String. Based on example code from APress Pro Android
                        try {
                                InputStream is = am.open(assetName) ;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();

				byte[] b = new byte[1024];
				int bytesRead ;
				while ( (bytesRead = is.read(b)) != -1) {
					bos.write(b, 0, bytesRead);
					length += bytesRead;
				}
				myBytes = bos.toByteArray();
				mybitmap = BitmapFactory.decodeByteArray(myBytes, 0, length);

				if (mybitmap != null) {
					// convert this bitmap into ARGB_8888 if it is not.
					if (mybitmap.getConfig() != Bitmap.Config.ARGB_8888) {
						//Log.w(TAG, "BITMAP changing");
						mybitmap = mybitmap.copy(Bitmap.Config.ARGB_8888,false);
						//Log.w(TAG,"BITMAP!!! bitmap is a " + mybitmap.getConfig());
					}
				
					// convert this to a char array for us to send to FreeWRL
					imageWidth = mybitmap.getWidth();
					imageHeight = mybitmap.getHeight();
					hasAlpha = mybitmap.hasAlpha();
					int sz = 4 * imageWidth * imageHeight;
					ByteBuffer bb = ByteBuffer.allocate(sz);
					mybitmap.copyPixelsToBuffer(bb);
		
					// convert the ByteBuffer to a byte[] array.
					myBytes = bb.array();
					length = sz ;
				}
			} catch (IOException e) {}
                }
        }

	/** Read the given binary file, and return its contents as a byte array.*/ 
	private byte[] binary_read(Integer len, InputStream myStream) {
		byte[] result = new byte[(int)len];
			// Log.w(TAG,"start binary_read, len " + len);
			try {
				int totalBytesRead = 0;
				BufferedInputStream input = new BufferedInputStream(myStream,8192);

				// Log.w(TAG,"BufferedInputStream available bytes " + input.available());

				while(totalBytesRead < result.length){
					int bytesRemaining = result.length - totalBytesRead;

					//Log.w(TAG,"totalBytesRead " + totalBytesRead + "  bytesRemaining " + bytesRemaining);
					//input.read() returns -1, 0, or more :

					int bytesRead = input.read(result, totalBytesRead, bytesRemaining); 
					if (bytesRead > 0){
						totalBytesRead = totalBytesRead + bytesRead;
					}
				}
		} catch (IOException ex) {
			Log.w(TAG,ex);
		}
		return result;
	}

	public FreeWRLAssetData (Integer of, Integer len, FileDescriptor fd, InputStream myStream) {
		Bitmap mybitmap = null;
	
		this.offset = of;
		this.length = len;
		this.fd = fd;
		this.myBytes = null;
		this.imageWidth = -1;
		this.imageHeight = -1;
		this.hasAlpha = false;


		// any hope of finding anything? if so, lets continue...
		if (myStream != null) {

			// NOTE: Android 2.2 REQUIRES the reset to be called, or else the binary_read can not be
			// read. Android 3.x and above do not require this, not sure about 2.3.x

			try {
				//Log.w(TAG,"FAD constructor, myStream  has available " + myStream.available());
				if (myStream.markSupported()) {
					//Log.w(TAG,"mark supported");
				} else {
					//Log.w(TAG,"mark NOT supported");
				}

				// if the InputStream is null, we had a failure, thus we can not have a bitmap.
				mybitmap = BitmapFactory.decodeStream(myStream);

				myStream.reset();
			} catch (IOException e) {
				Log.w(TAG,"io exception caught - mybitmap is " + mybitmap);
			}
			
			// do we have a valid input stream, that is NOT a bitmap?
			if (mybitmap == null) {
				//Log.w(TAG,"not decoded as a bitmap; Most likely a text file  - of " + of + " len " + len);
				
					myBytes = binary_read(len,myStream);

			} else {

				//Log.w(TAG,"BITMAP!!! bitmap is a " + mybitmap.getConfig());
				// convert this bitmap into ARGB_8888 if it is not.
				if (mybitmap.getConfig() != Bitmap.Config.ARGB_8888) {
					//Log.w(TAG, "BITMAP changing");
					mybitmap = mybitmap.copy(Bitmap.Config.ARGB_8888,false);
					//Log.w(TAG,"BITMAP!!! bitmap is a " + mybitmap.getConfig());
				}
			
				// convert this to a char array for us to send to FreeWRL
				imageWidth = mybitmap.getWidth();
				imageHeight = mybitmap.getHeight();
				hasAlpha = mybitmap.hasAlpha();
				int sz = 4 * imageWidth * imageHeight;
				ByteBuffer bb = ByteBuffer.allocate(sz);
				mybitmap.copyPixelsToBuffer(bb);
	
				// convert the ByteBuffer to a byte[] array.
				myBytes = bb.array();
			}
	
			// we are done with this InputStream now.
			try {
				myStream.close();
			} catch (IOException e) {}

			//Log.w(TAG,"file read in, length " + myBytes.length);
		}
	}
}
