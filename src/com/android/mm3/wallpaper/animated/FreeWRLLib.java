/*
  $Id: FreeWRLLib.java,v 1.9 2012/08/28 15:34:52 crc_canada Exp $

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

import java.io.FileDescriptor;
public class FreeWRLLib {

     static {
         System.loadLibrary("FreeWRL");
     }

     public static native void init(int width, int height);
     public static native void step();
     public static native void initialFile(String initFile);
	public static native boolean resourceWanted();
	public static native String resourceNameWanted();
	public static native void resourceFile(byte[]myBytes, int width, int height, boolean hasAlpha);
	public static native int sendFontFile(int which, FileDescriptor ad, int offset, int length);
	public static native void setButDown(int but, int state);
	public static native void setLastMouseEvent(int state);
	public static native void handleAqua(int but, int state, int x, int y);
	public static native void reloadAssets();
	public static native void nextViewpoint();
	public static native void doQuitInstance();
	public static native void createInstance();
	public static native void replaceWorldNeeded();
	public static native void setTmpDir(String tmpDirectory);

	public static native String androidGetLastMessage(int whichone);
	public static native int androidGetUnreadMessageCount();
}
