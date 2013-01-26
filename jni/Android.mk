
LOCAL_PATH := $(call my-dir)

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES:=freex3d/src/lib/vrml_parser/CProto.c \
	freex3d/src/lib/vrml_parser/CParse.c \
	freex3d/src/lib/vrml_parser/CParseLexer.c \
	freex3d/src/lib/vrml_parser/CRoutes.c \
	freex3d/src/lib/vrml_parser/CFieldDecls.c \
	freex3d/src/lib/vrml_parser/CParseParser.c \
	freex3d/src/lib/x3d_parser/Bindable.c \
	freex3d/src/lib/x3d_parser/X3DParser.c \
	freex3d/src/lib/x3d_parser/X3DProtoScript.c \
	freex3d/src/lib/x3d_parser/capabilitiesHandler.c \
	freex3d/src/lib/non_web3d_formats/ColladaParser.c \
	android_helper.c \
	freex3d/src/lib/main/ConsoleMessage.c \
	freex3d/src/lib/main/MainLoop.c \
	freex3d/src/lib/main/ProdCon.c \
	freex3d/src/lib/main/Snapshot.c \
	freex3d/src/lib/main/SoundEngineClient.c \
	freex3d/src/lib/main/utils.c \
	freex3d/src/lib/scenegraph/Children.c \
	freex3d/src/lib/scenegraph/Collision.c \
	freex3d/src/lib/scenegraph/Component_Core.c \
	freex3d/src/lib/scenegraph/Component_CubeMapTexturing.c \
	freex3d/src/lib/scenegraph/Component_DIS.c \
	freex3d/src/lib/scenegraph/Component_EnvironEffects.c \
	freex3d/src/lib/scenegraph/Component_EnvironSensor.c \
	freex3d/src/lib/scenegraph/Component_EventUtils.c \
	freex3d/src/lib/scenegraph/Component_Geometry2D.c \
	freex3d/src/lib/scenegraph/Component_Geometry3D.c \
	freex3d/src/lib/scenegraph/Component_Geospatial.c \
	freex3d/src/lib/scenegraph/Component_Grouping.c \
	freex3d/src/lib/scenegraph/Component_HAnim.c \
	freex3d/src/lib/scenegraph/Component_Interpolation.c \
	freex3d/src/lib/scenegraph/Component_KeyDevice.c \
	freex3d/src/lib/scenegraph/Component_Lighting.c \
	freex3d/src/lib/scenegraph/Component_Navigation.c \
	freex3d/src/lib/scenegraph/Component_Networking.c \
	freex3d/src/lib/scenegraph/Component_NURBS.c \
	freex3d/src/lib/scenegraph/Component_Picking.c \
	freex3d/src/lib/scenegraph/Component_PointingDevice.c \
	freex3d/src/lib/scenegraph/Component_ProgrammableShaders.c \
	freex3d/src/lib/scenegraph/Component_Rendering.c \
	freex3d/src/lib/scenegraph/Component_Scripting.c \
	freex3d/src/lib/scenegraph/Component_Shape.c \
	freex3d/src/lib/scenegraph/Component_Sound.c \
	freex3d/src/lib/scenegraph/Component_Text.c \
	freex3d/src/lib/scenegraph/Component_Texturing.c \
	freex3d/src/lib/scenegraph/Component_Time.c \
	freex3d/src/lib/scenegraph/Component_VRML1.c \
	freex3d/src/lib/scenegraph/GeneratedCode.c \
	freex3d/src/lib/scenegraph/GenPolyRep.c \
	freex3d/src/lib/scenegraph/LinearAlgebra.c \
	freex3d/src/lib/scenegraph/MPEG_Utils.c \
	freex3d/src/lib/scenegraph/NormalCalcs.c \
	freex3d/src/lib/scenegraph/OSCcallbacks.c \
	freex3d/src/lib/scenegraph/Polyrep.c \
	freex3d/src/lib/scenegraph/quaternion.c \
	freex3d/src/lib/scenegraph/readpng.c \
	freex3d/src/lib/scenegraph/RenderFuncs.c \
	freex3d/src/lib/scenegraph/ringbuf.c \
	freex3d/src/lib/scenegraph/statics.c \
	freex3d/src/lib/scenegraph/StreamPoly.c \
	freex3d/src/lib/scenegraph/Tess.c \
	freex3d/src/lib/scenegraph/Vector.c \
	freex3d/src/lib/scenegraph/Viewer.c \
	freex3d/src/lib/input/convert1To2.c \
	freex3d/src/lib/input/EAIEventsIn.c \
	freex3d/src/lib/input/EAIEventsOut.c \
	freex3d/src/lib/input/EAIHelpers.c \
	freex3d/src/lib/input/EAIServ.c \
	freex3d/src/lib/input/EAI_C_CommonFunctions.c \
	freex3d/src/lib/input/InputFunctions.c \
	freex3d/src/lib/input/SensInterps.c	\
	freex3d/src/lib/world_script/CScripts.c \
	freex3d/src/lib/world_script/fieldGet.c \
	freex3d/src/lib/world_script/fieldSet.c \
	freex3d/src/lib/world_script/JScript.c \
	freex3d/src/lib/world_script/jsUtils.c \
	freex3d/src/lib/world_script/jsVRMLBrowser.c \
	freex3d/src/lib/world_script/jsVRMLClasses.c \
	freex3d/src/lib/world_script/jsVRML_MFClasses.c \
	freex3d/src/lib/world_script/jsVRML_SFClasses.c	\
	freex3d/src/lib/display.c \
	freex3d/src/lib/iglobal.c \
	freex3d/src/lib/internal.c \
	freex3d/src/lib/io_files.c \
	freex3d/src/lib/io_http.c \
	freex3d/src/lib/list.c \
	freex3d/src/lib/main.c \
	freex3d/src/lib/resources.c \
	freex3d/src/lib/threads.c \
	freex3d/src/lib/opengl/Frustum.c \
	freex3d/src/lib/opengl/LoadTextures.c \
	freex3d/src/lib/opengl/Material.c \
	freex3d/src/lib/opengl/OpenGL_Utils.c \
	freex3d/src/lib/opengl/OSX_miniglew.c \
	freex3d/src/lib/opengl/RasterFont.c \
	freex3d/src/lib/opengl/RenderTextures.c \
	freex3d/src/lib/opengl/Textures.c \
	freex3d/src/lib/ui/common.c \
	freex3d/src/lib/ui/fwCommonX11.c \
	freex3d/src/lib/ui/CursorDraw.c \
	freex3d/src/lib/ui/GLwDrawA.c \
	freex3d/src/lib/ui/statusbarConsole.c \
	freex3d/src/lib/ui/statusbarHud.c \
	freex3d/src/lib/ui/statusbarStub.c \
	freex3d/src/lib/plugin/PluginSocket.c \
	freex3d/src/lib/plugin/pluginUtils.c \
	freex3d/src/libtess/dict.c \
	freex3d/src/libtess/geom.c \
	freex3d/src/libtess/memalloc.c \
	freex3d/src/libtess/mesh.c \
	freex3d/src/libtess/normal.c \
	freex3d/src/libtess/priorityq.c \
	freex3d/src/libtess/render.c \
	freex3d/src/libtess/sweep.c \
	freex3d/src/libtess/tess.c \
	freex3d/src/libtess/tessmono.c

# For the device
# =====================================================

include $(CLEAR_VARS)

APP_OPTIM=debug

LOCAL_CFLAGS:=-D_ANDROID -O0 -g -ggdb

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/freetype2-android/include/ \
	$(LOCAL_PATH)/ \
	$(LOCAL_PATH)/freex3d/src/lib/  \
	$(LOCAL_PATH)/freex3d/src/libtess/ \
	$(LOCAL_PATH)/libxml2/include/ \


LOCAL_SRC_FILES := $(common_SRC_FILES)

LOCAL_MODULE:= FreeWRL

LOCAL_STATIC_LIBRARIES := xml2 freetype2-static

LOCAL_LDLIBS    := -lGLESv2 -lGLESv1_CM -lc -llog

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/libxml2/Android.mk \
        $(LOCAL_PATH)/freetype2-android/Android.mk
