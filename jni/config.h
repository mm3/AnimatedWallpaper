
/*
	Android compatible config.h
*/


//#define DEBUG_MALLOC 1
//#define FW_DEBUG 1


//  try stereo rendering
#define FREEWRL_STEREO_RENDERING

#define MAX_MULTITEXTURE 4

#define GLES2 1

#define EXCLUDE_EAI 1

#define TARGET_ANDROID

#define FREEWRL_MESSAGE_WRAPPER "time"

/* do we want the frontend to take snappies? */
#define FRONTEND_DOES_SNAPSHOTS 1

/* Path to internet browser */
#define BROWSER "C:/Program Files/Mozilla Firefox/firefox.exe"

/* Define to 1 if you have the <ctype.h> header file. */
#define HAVE_CTYPE_H 1

/* Define to 1 if you have the <dirent.h> header file. */
#define HAVE_DIRENT_H 1

/* Define to 1 if you have the <dlfcn.h> header file. */
#define HAVE_DLFCN_H 1

/* Define to 1 if you have the <errno.h> header file. */
#define HAVE_ERRNO_H 1

#define HAVE_LIBXML_PARSER_H 1

/* Define to 1 if you have the `fork' function. */
#define HAVE_FORK 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPE_H 1


/* Define to 1 if your system has a GNU libc compatible `malloc' function, and
   to 0 otherwise. */
#define HAVE_MALLOC 1

/* Define to 1 if you have the <math.h> header file. */
#define HAVE_MATH_H 1

/* Define to 1 if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1


/* Define if you have POSIX threads libraries and header files. */
#define HAVE_PTHREAD 1

/* Define to 1 if you have the <sched.h> header file. */
#define HAVE_SCHED 1

/* Define to 1 if you have the <signal.h> header file. */
#define HAVE_SIGNAL_H 1

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdio.h> header file. */
#define HAVE_STDIO_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the `strchr' function. */
#define HAVE_STRCHR 1

/* Define to 1 if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the `strrchr' function. */
#define HAVE_STRRCHR 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <sys/wait.h> header file. */
#define HAVE_SYS_WAIT_H 1

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define to 1 if you have the `vfork' function. */
#define HAVE_VFORK 1

/* Define to 1 if you have the <vfork.h> header file. */

/* Name of package */

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT " freewrl "

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Define to 1 if the X Window System is missing or not being used. */
#define X_DISPLAY_MISSING 1

/* enable debugging (default=off) */

#define ushort unsigned short

/* Define to rpl_malloc if the replacement function should be used. */

/* Define to `int' if <sys/types.h> does not define. */

/* Define as `fork' if `vfork' does not work. */
#define vfork fork

#define HAVE_DIRECT_H 1

#define HAVE_GETTIMEOFDAY 1

#define HAVE_STDARG_H 1

#define GL_ES_VERSION_2_0 1

#define FRONTEND_GETS_FILES 1
#define FRONTEND_HANDLES_DISPLAY_THREAD 1

#ifdef _ANDROID
void DROIDDEBUG( const char*pFmtStr, ...);
#endif

#undef INCLUDE_STL_FILES
