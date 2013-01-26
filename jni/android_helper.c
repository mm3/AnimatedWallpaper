/*
  $Id: android_helper.c,v 1.20 2012/09/17 18:27:42 crc_canada Exp $

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


/* androidHelper.c */

#if defined( _ANDROID )

#include <stddef.h>
/* possibly also stdlib.h, stdio.h...*/

# include <stdbool.h>
#include <stdio.h>
#include <string.h>


#include <libFreeWRL.h>
#include <jni.h>
#include <android/log.h>

#include <pthread.h>

// from orig system

#include <config.h>

#include <libFreeWRL.h>
#include <list.h>
#include <resources.h>

#include "display.h"
#include "vrml_parser/Structs.h"

#include <jni.h>
#include <android/log.h>

#include <pthread.h>
#include <resources.h>
#include <iglobal.h>

// end from orig system



#define TRUE (1==1)
#define FALSE (1==0)
#define  LOG_TAG    "FreeWRL-androidHelper-"

// globals

static JavaVM* g_jvm = NULL;
static jclass fileCallbackClass = NULL;
static jmethodID fileLoadCallback = NULL;
static jmethodID startRenderCallback = NULL;
void DROIDDEBUG( const char*pFmtStr, ...);

static char* currentFile = NULL;

// keep sequence here so that we know if we have a restart, or just a refresh
static int mapTexture = -1;
static int confidenceCone = -1;

static ttglobal *pGlobal=NULL;

/********************************************
 Initializer thread 
*********************************************/

pthread_t loadFileThread = (pthread_t)0;

void fileLoadThread(void* param) {

	DROIDDEBUG("------------------LOAD THREAD-----------------------");
	fwl_OSX_initializeParameters(currentFile);
	DROIDDEBUG("------------------FIN LOAD THREAD-----------------------");
}


/********************************************
*********************************************/


void DROIDDEBUG( const char*pFmtStr, ...)
{
#ifdef DEBUG
	static char zLog[500];
	
	va_list mrk;
	va_start(mrk,pFmtStr);
	vsprintf(zLog,pFmtStr, mrk);
	va_end(mrk);
	__android_log_print(ANDROID_LOG_INFO,LOG_TAG,zLog);
#endif //DEBUG
}

JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM* vm, void* reserved )
{
	DROIDDEBUG("------------------ON LOAD-----------------------");
	g_jvm = vm;
	JNIEnv* ioEnv = NULL;

	DROIDDEBUG("------------------FIN ON LOAD-----------------------");
	return JNI_VERSION_1_6;
}

// explicitly ask to create an instance of the freewrl runtime code. Useful for onDestroy
// to a restart - the library will NOT be reloaded, but we MUST re-initialize our code.
JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_createInstance(JNIEnv * env, jobject obj)
{
	//if (pGlobal != NULL) DROIDDEBUG("createInstance, pGlobal != NULL!!");
	
	// never use pGlobal, but...
	pGlobal = (ttglobal*)fwl_init_instance();
}

JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_initialFile(JNIEnv * env, jobject obj, jstring passedInitialFile)
{
	DROIDDEBUG("------------------INITIAL FILE-----------------------");

	const char *cFilename = (*env)->GetStringUTFChars(env, passedInitialFile, NULL);

	/* save a copy of this filename - we can probably free this after the initial request from the NDK rendering
	   library, as it will cache it as well */

	if (currentFile != NULL) free(currentFile);
	currentFile = strdup(cFilename);


	DROIDDEBUG("cFilename is :%s:",currentFile);
	
	// step 1:
	DROIDDEBUG(" Java_org_freewrl_FreeWRLLib_currentFile starting step 1");
	fwl_initializeRenderSceneUpdateScene();


        // step 2:  create a thread to handle the file load requests from the library
	DROIDDEBUG(" Java_org_freewrl_FreeWRLLib_currentFile starting step 2");
        if( 0 != pthread_create(&loadFileThread, NULL, (void*)fileLoadThread, (void*)currentFile) )
        {
                DROIDDEBUG("!!Error creating fileloadedThread");
                return;
        }


	// step 3:
	DROIDDEBUG(" Java_org_freewrl_FreeWRLLib_currentFile starting step 3");
	int x = fv_display_initialize();


	// do not free this...
	//(*env)->ReleaseStringUTFChars(env, currentFile, cFilename);
	DROIDDEBUG("------------------END INITIAL FILE-----------------------");
}

/* send in the location for FreeWRL tmp files; eg, for making copies of PROTO expansions and such. */
JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_setTmpDir(JNIEnv * env, jobject obj, jstring tmpdir) {
	const char *cFilename = (*env)->GetStringUTFChars(env, tmpdir, NULL);
	fwl_tmpFileLocation(cFilename);
	//(*env)->ReleaseStringUTFChars(env, currentFile, cFilename);
}


JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_init(JNIEnv * env, jobject obj,  jint width, jint height)
{
	DROIDDEBUG("------------------LIB INIT-----------------------");

	fwl_setScreenDim(width, height);

	DROIDDEBUG("------------------FIN LIB INIT-----------------------");
}


/* do we WANT a file? return yes/no */
JNIEXPORT jboolean JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_resourceWanted(JNIEnv * env, jobject obj) {
	return fwg_frontEndWantsFileName()!=NULL;
}

/* return the NAME of the resource we want... */
JNIEXPORT jstring JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_resourceNameWanted(JNIEnv *env, jobject obj) {
	//DROIDDEBUG("------------------RESOURCE NAME WANTED CALLED----------------------");
	DROIDDEBUG(fwg_frontEndWantsFileName());
	return (*env)->NewStringUTF(env,fwg_frontEndWantsFileName());
}




#define SUCCESS			         			0
//public static native int resourceFile(byte[]myBytes, int width, int height, bool hasAlpha);

// For x3d, wrl, textures:
JNIEXPORT jint JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_resourceFile (JNIEnv * env, jclass thiz,
		jbyteArray myBytes,
		jint width,
		jint height,
		jboolean hasAlpha) {

	jbyte *fileData;
	bool ok;
	int length = (*env)->GetArrayLength(env,myBytes);

	// get the bytes over first
	fileData = (*env)->GetByteArrayElements(env,myBytes,0);
	fwg_frontEndReturningData(fileData,length,width,height,hasAlpha);

	(*env)->ReleaseByteArrayElements(env,myBytes,fileData,JNI_ABORT);

return (jint)SUCCESS;
}

//sendFontFile - send only file descriptor, let freewrl open it if wished for.
JNIEXPORT jint JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_sendFontFile (JNIEnv * env, jclass thiz, jint whichFontFile, jobject fd_sys, jint off, jint len) {

	jclass fdClass = (*env)->FindClass(env,"java/io/FileDescriptor");
	if (fdClass != NULL){
		jfieldID fdClassDescriptorFieldID = (*env)->GetFieldID(env,fdClass, "descriptor", "I");
		if (fdClassDescriptorFieldID != NULL && fd_sys != NULL){
			jint fd = (*env)->GetIntField(env,fd_sys, fdClassDescriptorFieldID);
			int myfd = dup(fd);
			FILE* myFile = fdopen(myfd, "rb");
			if (myFile){
				// seek but don't read.
				fseek(myFile, off, SEEK_SET);
				fwg_AndroidFontFile(myFile,len);
				return (jint)SUCCESS;
			}
			else {
				return (jint) !SUCCESS;
			}
		}
		else {
			return (jint)!SUCCESS;
		}
	}
	else {
		return (jint)!SUCCESS;
	}
}


/* do a call of the scenegraph. */
JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_step(JNIEnv * env, jobject obj)
{
    	fwl_RenderSceneUpdateScene();
}


JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_nextViewpoint(JNIEnv * env, jobject obj)
{
    	fwl_Next_ViewPoint();
}

JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_doQuitInstance(JNIEnv * env, jobject obj)
{
    	fwl_doQuitInstance();
}

/* reload assets when onSurfaceCreated, but system already loaded */
JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_reloadAssets(JNIEnv * env, jobject obj)
{
    	fwl_Android_reloadAssets();
}
/* handle touch */
JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_setButDown(JNIEnv *env, jobject obj, int but, int state)
{
        fwl_setButDown(but,state);
}

JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_setLastMouseEvent(JNIEnv *env, jobject obj, int state)
{
        fwl_setLastMouseEvent(state);
}


JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_replaceWorldNeeded(JNIEnv *env, jobject obj)
{
	fwl_Android_replaceWorldNeeded();
}

JNIEXPORT void JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_handleAqua(JNIEnv *env, jobject obj, int but, int state, int x, int y)
{
        fwl_handle_aqua(but,state,x,y);
}

// how many console messages do we have?
JNIEXPORT jint JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_androidGetUnreadMessageCount(JNIEnv *env, jobject obj) {
	//DROIDDEBUG("------------------GET UNREAD MESSAGE COUNT CALLED----------------------");
	return fwg_get_unread_message_count();
}

// get the console message, indicated by the parameter.
JNIEXPORT jstring JNICALL Java_com_android_mm3_wallpaper_animated_FreeWRLLib_androidGetLastMessage(JNIEnv *env, jobject obj, int whichone) {
	//DROIDDEBUG("------------------GET LAST MESSAGE CALLED----------------------");
	return (*env)->NewStringUTF(env,fwg_get_last_message(whichone));
}
#endif
