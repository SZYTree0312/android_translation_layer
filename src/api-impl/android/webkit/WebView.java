package android.webkit;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.ViewGroup;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebView extends ViewGroup {

	private WebViewClient webViewClient;
	private ValueCallback evaluateJavascriptCallback;
	private final Map<String, Object> javascriptInterfaces = new HashMap<String, Object>();

	public WebView(Context context) {
		this(context, null);
	}

	public WebView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public WebSettings getSettings() {
		return new WebSettings();
	}

	public void setDownloadListener(DownloadListener downloadListener) {}

	public void setScrollBarStyle(int scrollBarStyle) {}

	public void setWebViewClient(WebViewClient webViewClient) {
		this.webViewClient = webViewClient;
	}

	// to be used by native code
	void internalLoadChanged(int loadState, String url) {
		if (loadState == /*WEBKIT_LOAD_STARTED*/ 0 && webViewClient != null) {
			webViewClient.onPageStarted(this, url);
		} else if (loadState == /*WEBKIT_LOAD_FINISHED*/ 3 && webViewClient != null) {
			webViewClient.onPageFinished(this, url);
		}
	}

	public void setVerticalScrollBarEnabled(boolean enabled) {}
	public void setVerticalScrollbarOverlay(boolean overlay) {}
	public void setInitialScale(int scaleInPercent) {}

	public void addJavascriptInterface(Object object, String name) {
		javascriptInterfaces.put(name, object);

		List<String> methodNames = new ArrayList<String>();
		for (Method method : object.getClass().getMethods()) {
			/*
			 * android only exposes methods annotated with @JavascriptInterface, but a lot of apps
			 * out there never got the memo, so we expose every public method instead. methods
			 * inherited from Object are skipped, they are of no use to a page and would allow it
			 * to get at the class of the injected object.
			 */
			if (method.getDeclaringClass() == Object.class)
				continue;
			if (!methodNames.contains(method.getName()))
				methodNames.add(method.getName());
		}

		if (native_addJavascriptInterface(widget, name, methodNames.toArray(new String[methodNames.size()])))
			return;

		/*
		 * HACK: the webview backend is disabled, so nothing will ever load the page and the
		 * interface can not be called. directly call onRenderingDone for OctoDroid, which
		 * otherwise waits for a callback that will never come.
		 */
		if (object.getClass().getName().startsWith("com.gh4a") && "NativeClient".equals(name)) {
			try {
				object.getClass().getMethod("onRenderingDone").invoke(object);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}

	public void setWebChromeClient(WebChromeClient dummy) {}

	public void removeAllViews() {}

	public void destroy() {}

	public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
		if ("base64".equals(encoding)) {
			data = new String(Base64.decode(data, 0));
		}
		if (mimeType != null && mimeType.contains(";")) {
			mimeType = mimeType.substring(0, mimeType.indexOf(";"));
		}
		// webkit doesn't allow overwriting the file:// uri scheme. So we replace it with the android-asset:// scheme
		data = data.replace("file:///android_asset/", "android-asset:///assets/");
		native_loadDataWithBaseURL(widget, baseUrl, data, mimeType, encoding);
	}

	public void loadUrl(String url) {
		if (url.startsWith("javascript:")) {
			System.out.println("loadUrl: " + url + " - not implemented yet");
			return;
		}
		native_loadUrl(widget, url);
	}

	public void stopLoading() {}

	// to be used by native code
	AssetManager internalGetAssetManager() {
		return getContext().getResources().getAssets();
	}

	public void onPause() {}

	public void onResume() {}

	public boolean isPaused() {
		return false;
	}

	public void resumeTimers() {}

	public void loadData(String data, String mimeType, String encoding) {
		loadDataWithBaseURL("about:blank", data, mimeType, encoding, "about:blank");
	}

	public void evaluateJavascript(String script, ValueCallback resultCallback) {
		this.evaluateJavascriptCallback = resultCallback;
		native_evaluateJavascript(widget, script);
	}

	// to be used by native code
	void internalEvaluateJavascriptResult(String result) {
		if (evaluateJavascriptCallback != null) {
			ValueCallback callback = evaluateJavascriptCallback;
			evaluateJavascriptCallback = null;
			callback.onReceiveValue(result);
		}
	}

	// to be used by native code
	void internalJavascriptInterfaceCall(String name, String methodName, Object[] args) {
		Object object = javascriptInterfaces.get(name);
		if (object == null)
			return;

		Method target = null;
		for (Method method : object.getClass().getMethods()) {
			if (!method.getName().equals(methodName))
				continue;
			if (method.getParameterTypes().length != args.length)
				continue;
			target = method;
			break;
		}
		if (target == null) {
			System.err.println("WebView: javascript interface " + name + " has no " + methodName + "/" + args.length);
			return;
		}

		Class<?>[] parameterTypes = target.getParameterTypes();
		Object[] convertedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++)
			convertedArgs[i] = convertJavascriptValue(args[i], parameterTypes[i]);

		try {
			target.invoke(object, convertedArgs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* javascript has a single number type, so narrow it down to whatever the method expects */
	private static Object convertJavascriptValue(Object value, Class<?> target) {
		if (value == null)
			return null;
		if (target == String.class)
			return value.toString();
		if (value instanceof Double) {
			double number = (Double) value;
			if (target == int.class || target == Integer.class)
				return (int) number;
			if (target == long.class || target == Long.class)
				return (long) number;
			if (target == float.class || target == Float.class)
				return (float) number;
			if (target == short.class || target == Short.class)
				return (short) number;
			if (target == byte.class || target == Byte.class)
				return (byte) number;
			if (target == boolean.class || target == Boolean.class)
				return number != 0;
		}
		return value;
	}

	public static void setWebContentsDebuggingEnabled(boolean enabled) {}

	public void clearFormData() {}

	public void clearHistory() {}

	public void clearMatches() {}

	public void clearSslPreferences() {}

	public void clearCache(boolean includeDiskFiles) {}

	// directly accessed by androidx WebViewGlueCommunicator to get ClassLoader
	private static Object getFactory() {
		return new Object();
	}

	@Override
	protected native long native_constructor(Context context, AttributeSet attrs);
	private native void native_loadDataWithBaseURL(long widget, String baseUrl, String data, String mimeType, String encoding);
	private native void native_loadUrl(long widget, String url);
	private native void native_evaluateJavascript(long widget, String script);
	private native boolean native_addJavascriptInterface(long widget, String name, String[] methodNames);
}
