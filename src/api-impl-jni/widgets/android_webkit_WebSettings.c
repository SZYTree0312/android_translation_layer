#include <gtk/gtk.h>
#include <webkit/webkit.h>

#include "../defines.h"
#include "../util.h"

#include "../generated_headers/android_webkit_WebSettings.h"

/*
 * the settings are applied by property name instead of through the generated setters.
 * webkit has been dropping settings over the years (dom storage, offline web application
 * cache, web sql, ...), and going through g_object_set means we keep building when a
 * property disappears - an unknown property is only a warning at runtime, while a missing
 * setter is a build failure.
 */

static WebKitSettings *get_settings(jlong widget_ptr)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return NULL;
	/* borrowed, the web view keeps its own reference */
	return webkit_web_view_get_settings(WEBKIT_WEB_VIEW(_PTR(widget_ptr)));
}

JNIEXPORT void JNICALL Java_android_webkit_WebSettings_native_1setBoolean(JNIEnv *env, jobject this, jlong widget_ptr, jstring property, jboolean value)
{
	WebKitSettings *settings = get_settings(widget_ptr);
	if (settings == NULL)
		return;

	const char *property_str = _CSTRING(property);
	g_object_set(G_OBJECT(settings), property_str, value == JNI_TRUE ? TRUE : FALSE, NULL);
	(*env)->ReleaseStringUTFChars(env, property, property_str);
}

JNIEXPORT void JNICALL Java_android_webkit_WebSettings_native_1setInt(JNIEnv *env, jobject this, jlong widget_ptr, jstring property, jint value)
{
	WebKitSettings *settings = get_settings(widget_ptr);
	if (settings == NULL)
		return;

	const char *property_str = _CSTRING(property);
	g_object_set(G_OBJECT(settings), property_str, (guint) value, NULL);
	(*env)->ReleaseStringUTFChars(env, property, property_str);
}

JNIEXPORT void JNICALL Java_android_webkit_WebSettings_native_1setString(JNIEnv *env, jobject this, jlong widget_ptr, jstring property, jstring value)
{
	WebKitSettings *settings = get_settings(widget_ptr);
	if (settings == NULL)
		return;

	const char *property_str = _CSTRING(property);
	const char *value_str = value != NULL ? _CSTRING(value) : NULL;
	g_object_set(G_OBJECT(settings), property_str, value_str, NULL);

	if (value_str != NULL)
		(*env)->ReleaseStringUTFChars(env, value, value_str);
	(*env)->ReleaseStringUTFChars(env, property, property_str);
}
