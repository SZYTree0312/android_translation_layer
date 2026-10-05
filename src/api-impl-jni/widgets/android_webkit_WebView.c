#include <gio/gunixinputstream.h>
#include <gtk/gtk.h>
#include <webkit/webkit.h>
#include <jsc/jsc.h>

#include <androidfw/androidfw_c_api.h>

#include "../defines.h"
#include "../util.h"

#include "../AssetInputStream.h"
#include "WrapperWidget.h"

#include "../generated_headers/android_view_View.h"
#include "../generated_headers/android_webkit_WebView.h"

/*
 * javascript interfaces registered with addJavascriptInterface are exposed to the page as
 * window.<name>, where every method posts a message back to us instead of calling into java
 * directly. this is one-way and asynchronous: unlike android, we can not return a value
 * from the interface method.
 */
#define ATL_JSI_HANDLER_NAME "atlJavascriptInterface"

static void asset_uri_scheme_request_cb(WebKitURISchemeRequest *request, gpointer user_data)
{
	const gchar *path = webkit_uri_scheme_request_get_path(request);
	if (path == NULL)
		return;
	path += 1; // remove the leading '/'
	JNIEnv *env = get_jni_env();
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(webkit_uri_scheme_request_get_web_view(request))));
	jobject asset_manager_obj = (*env)->CallObjectMethod(env, wrapper->jobj, handle_cache.webview.internalGetAssetManager);
	struct AssetManager *asset_manager = _PTR(_GET_LONG_FIELD(asset_manager_obj, "mObject"));
	struct Asset *asset = AssetManager_openNonAsset(asset_manager, path, ACCESS_STREAMING);
	GInputStream *stream = asset_input_stream_new(asset);
	webkit_uri_scheme_request_finish(request, stream, Asset_getLength(asset), NULL);
	g_object_unref(stream);
}

static void web_view_load_changed(WebKitWebView *web_view, WebKitLoadEvent load_event, gpointer user_data)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(web_view)));
	JNIEnv *env = get_jni_env();
	(*env)->CallVoidMethod(env, wrapper->jobj, handle_cache.webview.internalLoadChanged, load_event, _JSTRING(webkit_web_view_get_uri(web_view)));
}

JNIEXPORT jlong JNICALL Java_android_webkit_WebView_native_1constructor(JNIEnv *env, jobject this, jobject context, jobject attrs)
{
	/*
	 * many apps use webview just for fingerprinting or displaying ads, which seems like
	 * a waste of resources even if we deal with fingerprinting and ads in some other way
	 * in the future.
	 */
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return Java_android_view_View_native_1constructor(env, this, context, attrs);

	GtkWidget *wrapper = g_object_ref(wrapper_widget_new());
	GtkWidget *webview = webkit_web_view_new();
	wrapper_widget_set_child(WRAPPER_WIDGET(wrapper), webview);
	wrapper_widget_set_jobject(WRAPPER_WIDGET(wrapper), env, this);
	webkit_web_context_register_uri_scheme(webkit_web_view_get_context(WEBKIT_WEB_VIEW(webview)), "android-asset", asset_uri_scheme_request_cb, NULL, NULL);
	g_signal_connect(G_OBJECT(webview), "load-changed", G_CALLBACK(web_view_load_changed), NULL);
	return _INTPTR(webview);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1loadUrl(JNIEnv *env, jobject this, jlong widget_ptr, jstring url)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	webkit_web_view_load_uri(webview, _CSTRING(url));
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1loadDataWithBaseURL(JNIEnv *env, jobject this, jlong widget_ptr, jstring base_url, jstring data_jstr, jstring mime_type, jstring encoding)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	jsize data_len = (*env)->GetStringUTFLength(env, data_jstr);
	jsize data_jlen = (*env)->GetStringLength(env, data_jstr);
	char *data = malloc(data_len + 1); // + 1 for NUL
	(*env)->GetStringUTFRegion(env, data_jstr, 0, data_jlen, data);
	webkit_web_view_load_bytes(webview, g_bytes_new(data, data_len), mime_type ? _CSTRING(mime_type) : "text/html", encoding ? _CSTRING(encoding) : NULL, base_url ? _CSTRING(base_url) : NULL);
}

static void evaluate_javascript_finished_cb(GObject *object, GAsyncResult *result, gpointer user_data)
{
	WebKitWebView *web_view = WEBKIT_WEB_VIEW(object);
	WrapperWidget *wrapper = WRAPPER_WIDGET(gtk_widget_get_parent(GTK_WIDGET(web_view)));
	JNIEnv *env = get_jni_env();

	WebKitJavascriptResult *js_result = webkit_web_view_evaluate_javascript_finish(web_view, result, NULL);
	if (js_result == NULL) {
		(*env)->CallVoidMethod(env, wrapper->jobj, handle_cache.webview.internalEvaluateJavascriptResult, NULL);
		return;
	}

	JSCValue *value = webkit_javascript_result_get_js_value(js_result);
	char *value_str = jsc_value_to_string(value);
	(*env)->CallVoidMethod(env, wrapper->jobj, handle_cache.webview.internalEvaluateJavascriptResult, _JSTRING(value_str));
	g_free(value_str);
	webkit_javascript_result_unref(js_result);
}

JNIEXPORT void JNICALL Java_android_webkit_WebView_native_1evaluateJavascript(JNIEnv *env, jobject this, jlong widget_ptr, jstring script)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return;

	WebKitWebView *webview = _PTR(widget_ptr);
	const char *script_str = _CSTRING(script);
	webkit_web_view_evaluate_javascript(webview, script_str, -1, NULL, NULL, NULL, evaluate_javascript_finished_cb, NULL);
	(*env)->ReleaseStringUTFChars(env, script, script_str);
}

static char *js_value_to_string(JSCValue *value)
{
	/* strings and numbers are fine as-is, everything else is passed to java as json */
	if (jsc_value_is_object(value))
		return jsc_value_to_json(value, 0);
	return jsc_value_to_string(value);
}

static jobject js_value_to_java_object(JNIEnv *env, JSCValue *value)
{
	if (value == NULL || jsc_value_is_null(value) || jsc_value_is_undefined(value))
		return NULL;

	if (jsc_value_is_boolean(value)) {
		jclass boolean_class = (*env)->FindClass(env, "java/lang/Boolean");
		jmethodID value_of = _STATIC_METHOD(boolean_class, "valueOf", "(Z)Ljava/lang/Boolean;");
		return (*env)->CallStaticObjectMethod(env, boolean_class, value_of, jsc_value_to_boolean(value) ? JNI_TRUE : JNI_FALSE);
	}

	if (jsc_value_is_number(value)) {
		jclass double_class = (*env)->FindClass(env, "java/lang/Double");
		jmethodID value_of = _STATIC_METHOD(double_class, "valueOf", "(D)Ljava/lang/Double;");
		return (*env)->CallStaticObjectMethod(env, double_class, value_of, (jdouble) jsc_value_to_double(value));
	}

	char *string = js_value_to_string(value);
	jobject result = _JSTRING(string);
	g_free(string);
	return result;
}

static void script_message_received_cb(WebKitUserContentManager *manager, WebKitJavascriptResult *result, gpointer user_data)
{
	WrapperWidget *wrapper = WRAPPER_WIDGET(user_data);
	JNIEnv *env = get_jni_env();
	JSCValue *message = webkit_javascript_result_get_js_value(result);
	if (!jsc_value_is_object(message))
		return;

	JSCValue *name_value = jsc_value_object_get_property(message, "name");
	JSCValue *method_value = jsc_value_object_get_property(message, "method");
	JSCValue *args_value = jsc_value_object_get_property(message, "args");

	char *name = name_value != NULL ? js_value_to_string(name_value) : NULL;
	char *method = method_value != NULL ? js_value_to_string(method_value) : NULL;

	jsize arg_count = 0;
	jobjectArray args = NULL;
	if (args_value != NULL && jsc_value_is_array(args_value)) {
		JSCValue *length_value = jsc_value_object_get_property(args_value, "length");
		if (length_value != NULL && jsc_value_is_number(length_value))
			arg_count = (jsize) jsc_value_to_double(length_value);
		if (length_value != NULL)
			g_object_unref(length_value);

		args = (*env)->NewObjectArray(env, arg_count, (*env)->FindClass(env, "java/lang/Object"), NULL);
		for (jsize i = 0; i < arg_count; i++) {
			JSCValue *arg_value = jsc_value_object_get_property_at_index(args_value, i);
			jobject arg = js_value_to_java_object(env, arg_value);
			(*env)->SetObjectArrayElement(env, args, i, arg);
			if (arg != NULL)
				(*env)->DeleteLocalRef(env, arg);
			if (arg_value != NULL)
				g_object_unref(arg_value);
		}
	}

	if (name != NULL && method != NULL)
		(*env)->CallVoidMethod(env, wrapper->jobj, handle_cache.webview.internalJavascriptInterfaceCall, _JSTRING(name), _JSTRING(method), args);
	if ((*env)->ExceptionCheck(env))
		(*env)->ExceptionClear(env);

	g_free(name);
	g_free(method);
	if (name_value != NULL)
		g_object_unref(name_value);
	if (method_value != NULL)
		g_object_unref(method_value);
	if (args_value != NULL)
		g_object_unref(args_value);
}

static char *escape_js_string_literal(const char *str)
{
	GString *escaped = g_string_new(NULL);
	for (const char *c = str; *c != '\0'; c++) {
		switch (*c) {
			case '\\':
				g_string_append(escaped, "\\\\");
				break;
			case '"':
				g_string_append(escaped, "\\\"");
				break;
			case '\n':
				g_string_append(escaped, "\\n");
				break;
			case '\r':
				g_string_append(escaped, "\\r");
				break;
			case '\t':
				g_string_append(escaped, "\\t");
				break;
			default:
				g_string_append_c(escaped, *c);
		}
	}
	return g_string_free(escaped, FALSE);
}

JNIEXPORT jboolean JNICALL Java_android_webkit_WebView_native_1addJavascriptInterface(JNIEnv *env, jobject this, jlong widget_ptr, jstring name, jobjectArray method_names)
{
	if (!getenv("ATL_UGLY_ENABLE_WEBVIEW"))
		return JNI_FALSE;

	WebKitWebView *webview = _PTR(widget_ptr);
	WebKitUserContentManager *manager = webkit_web_view_get_user_content_manager(webview);
	if (manager == NULL)
		return JNI_FALSE;

	/* the handler must only be registered once per view, no matter how many interfaces are added */
	if (g_object_get_data(G_OBJECT(manager), "atl-jsi-initialized") == NULL) {
		g_object_set_data(G_OBJECT(manager), "atl-jsi-initialized", GINT_TO_POINTER(1));
		webkit_user_content_manager_register_script_message_handler(manager, ATL_JSI_HANDLER_NAME);
		g_signal_connect(G_OBJECT(manager), "script-message-received::" ATL_JSI_HANDLER_NAME, G_CALLBACK(script_message_received_cb), gtk_widget_get_parent(GTK_WIDGET(webview)));
	}

	char *name_str = _CSTRING(name);
	char *escaped_name = escape_js_string_literal(name_str);

	GString *script = g_string_new(NULL);
	g_string_append(script, "(function(){\n");
	g_string_append(script, "\tvar handler = window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers[\"" ATL_JSI_HANDLER_NAME "\"];\n");
	g_string_append(script, "\tif (!handler) return;\n");
	g_string_append(script, "\tvar methods = [");
	jsize method_count = (*env)->GetArrayLength(env, method_names);
	for (jsize i = 0; i < method_count; i++) {
		jstring method_name = (*env)->GetObjectArrayElement(env, method_names, i);
		if (method_name == NULL)
			continue;
		char *method_str = _CSTRING(method_name);
		char *escaped_method = escape_js_string_literal(method_str);
		g_string_append_printf(script, "%s\"%s\"", i == 0 ? "" : ",", escaped_method);
		g_free(escaped_method);
		(*env)->ReleaseStringUTFChars(env, method_name, method_str);
	}
	g_string_append(script, "];\n");
	g_string_append(script, "\tvar iface = {};\n");
	g_string_append(script, "\tfor (var i = 0; i < methods.length; i++) {\n");
	g_string_append(script, "\t\t(function(method) {\n");
	g_string_append(script, "\t\t\tiface[method] = function() {\n");
	g_string_append_printf(script, "\t\t\t\thandler.postMessage({\"name\": \"%s\", \"method\": method, \"args\": Array.prototype.slice.call(arguments)});\n", escaped_name);
	g_string_append(script, "\t\t\t};\n");
	g_string_append(script, "\t\t})(methods[i]);\n");
	g_string_append(script, "\t}\n");
	g_string_append_printf(script, "\twindow[\"%s\"] = iface;\n", escaped_name);
	g_string_append(script, "})();\n");

	WebKitUserScript *user_script = webkit_user_script_new(script->str, WEBKIT_USER_CONTENT_INJECT_ALL_FRAMES, WEBKIT_USER_SCRIPT_INJECT_AT_DOCUMENT_START, NULL, NULL);
	webkit_user_content_manager_add_script(manager, user_script);
	webkit_user_script_unref(user_script);

	g_string_free(script, TRUE);
	g_free(escaped_name);
	(*env)->ReleaseStringUTFChars(env, name, name_str);
	return JNI_TRUE;
}
