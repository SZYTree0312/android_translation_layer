package android.content.pm;

import java.util.Collections;
import java.util.List;

public class ShortcutManager {
	public void removeAllDynamicShortcuts() {
	}

	public List getShortcuts(int matchFlags) {
		return Collections.emptyList();
	}

	// there is no launcher on desktop, so no shortcuts were ever published
	public List<ShortcutInfo> getDynamicShortcuts() {
		return Collections.emptyList();
	}

	public void removeLongLivedShortcuts(List<String> shortcutIds) {
	}

	public void reportShortcutUsed(String shortcutId) {
	}

	public boolean setDynamicShortcuts(List<ShortcutInfo> shortcutInfoList) {
		return true;
	}
}
