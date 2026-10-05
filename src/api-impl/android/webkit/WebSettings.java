package android.webkit;

public class WebSettings {

	public static enum LayoutAlgorithm {
		NORMAL,
		NARROW_COLUMNS,
	}

	public static enum RenderPriority {
		HIGH,
	}

	private final long widget;
	private String userAgentString;

	WebSettings(long widget) {
		this.widget = widget;
	}

	public static String getDefaultUserAgent(android.content.Context context) {
		return "GDPR VIOLATION";
	}

	public String getUserAgentString() {
		return userAgentString != null ? userAgentString : "GDPR VIOLATION";
	}

	public void setUserAgentString(String userAgentString) {
		this.userAgentString = userAgentString;
		native_setString(widget, "user-agent", userAgentString);
	}

	public void setSupportMultipleWindows(boolean supportMultipleWindows) {}

	public void setJavaScriptEnabled(boolean javaScriptEnabled) {
		native_setBoolean(widget, "enable-javascript", javaScriptEnabled);
	}

	public void setSavePassword(boolean savePassword) {}

	public void setGeolocationEnabled(boolean enabled) {}

	public void setCacheMode(int dummy) {}

	public void setLayoutAlgorithm(LayoutAlgorithm layoutAlgorithm) {}

	public void setAllowFileAccess(boolean allowFileAccess) {
		native_setBoolean(widget, "allow-file-access-from-file-urls", allowFileAccess);
	}

	public void setBuiltInZoomControls(boolean builtInZoomControls) {}

	public void setDisplayZoomControls(boolean displayZoomControls) {}

	public void setLoadsImagesAutomatically(boolean loadsImagesAutomatically) {
		native_setBoolean(widget, "auto-load-images", loadsImagesAutomatically);
	}

	public void setSupportZoom(boolean supportZoom) {}

	public void setUseWideViewPort(boolean useWideViewPort) {}

	public void setTextZoom(int textZoom) {}

	public void setDefaultFontSize(int size) {
		native_setInt(widget, "default-font-size", size);
	}

	public void setMinimumFontSize(int size) {
		native_setInt(widget, "minimum-font-size", size);
	}

	public void setAppCacheEnabled(boolean enabled) {}

	public void setAppCachePath(String path) {}

	public void setLoadWithOverviewMode(boolean overview) {}

	public void setRenderPriority(RenderPriority priority) {}

	public void setBlockNetworkLoads(boolean block) {}

	public void setMixedContentMode(int mode) {}

	public void setBlockNetworkImage(boolean block) {}

	/*
	 * dom storage, the application cache and the web sql database used to be opt-in
	 * settings with matching properties, but webkit dropped those: dom storage is
	 * always available now, while appcache and websql are gone entirely. so there is
	 * nothing left to set here.
	 */
	public void setDomStorageEnabled(boolean flag) {}

	public void setMediaPlaybackRequiresUserGesture(boolean require) {
		native_setBoolean(widget, "media-playback-requires-user-gesture", require);
	}

	public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
		native_setBoolean(widget, "javascript-can-open-windows-automatically", flag);
	}

	public void setAppCacheMaxSize(long size) {}

	public void setDatabaseEnabled(boolean enabled) {}

	public void setDatabasePath(String path) {}

	public void setAllowUniversalAccessFromFileURLs(boolean allow) {
		native_setBoolean(widget, "allow-universal-access-from-file-urls", allow);
	}

	public void setGeolocationDatabasePath(String path) {}

	public void setSaveFormData(boolean save) {}

	public void setAllowContentAccess(boolean allow) {}

	private native void native_setBoolean(long widget, String property, boolean value);
	private native void native_setString(long widget, String property, String value);
	private native void native_setInt(long widget, String property, int value);
}
