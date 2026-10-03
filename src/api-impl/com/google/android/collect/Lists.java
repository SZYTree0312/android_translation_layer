package com.google.android.collect;

import java.util.ArrayList;
import java.util.Collections;

public class Lists {
	public static <E> ArrayList<E> newArrayList() {
		return new ArrayList<E>();
	}

	public static <E> ArrayList<E> newArrayList(E... elements) {
		ArrayList<E> list = new ArrayList<E>(elements.length);
		Collections.addAll(list, elements);
		return list;
	}
}
