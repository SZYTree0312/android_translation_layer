package android.transition;

import android.animation.TimeInterpolator;
import android.view.View;

public class Transition {

	public interface TransitionListener {
		void onTransitionStart(Transition transition);
		void onTransitionEnd(Transition transition);
		void onTransitionCancel(Transition transition);
		void onTransitionPause(Transition transition);
		void onTransitionResume(Transition transition);
	}

	public Transition clone() {
		return new Transition();
	}

	// There is no real animation engine backing transitions here, so treat every
	// transition as completing instantly. Callers (including WhatsApp's own
	// shared-element image viewer) commonly gate follow-up work on
	// onTransitionEnd(); never firing it leaves them waiting forever with no
	// visible error, which looks like the app hanging on click.
	public Transition addListener(TransitionListener listener) {
		listener.onTransitionStart(this);
		listener.onTransitionEnd(this);
		return this;
	}

	public Transition excludeTarget(int targetId, boolean exclude) {
		return this;
	}

	public Transition addTarget(View target) {
		return this;
	}

	public Transition addTarget(String targetName) {
		return this;
	}

	public Transition setDuration(long duration) {
		return this;
	}

	public Transition setInterpolator(TimeInterpolator interpolator) {
		return this;
	}
}
