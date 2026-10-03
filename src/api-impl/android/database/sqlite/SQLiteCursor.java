/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
** Modified to support SQLite extensions by the SQLite developers:
** sqlite-dev@sqlite.org.
*/

package android.database.sqlite;

import android.atl.ATLLoadedApp;
import android.content.ContentValues;
import android.database.AbstractWindowedCursor;
import android.database.CursorWindow;
import android.database.DatabaseUtils;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A Cursor implementation that exposes results from a query on a
 * {@link SQLiteDatabase}.
 *
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 */
public class SQLiteCursor extends AbstractWindowedCursor {
	static final String TAG = "SQLiteCursor";
	static final int NO_COUNT = -1;

	/** The name of the table to edit */
	private final String mEditTable;

	/** The names of the columns in the rows */
	private final String[] mColumns;

	/** The query object for the cursor */
	private final SQLiteQuery mQuery;

	/** The compiled query this cursor came from */
	private final SQLiteCursorDriver mDriver;

	/** The number of rows in the cursor */
	private int mCount = NO_COUNT;

	/** The number of rows that can fit in the cursor window, 0 if unknown */
	private int mCursorWindowCapacity;

	/** A mapping of column names to column indices, to speed up lookups */
	private Map<String, Integer> mColumnNameMap;

	/**
	 * Execute a query and provide access to its result set through a Cursor
	 * interface. For a query such as: {@code SELECT name, birth, phone FROM
	 * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
	 * phone) would be in the projection argument and everything from
	 * {@code FROM} onward would be in the params argument.
	 *
	 * @param db a reference to a Database object that is already constructed
	 *     and opened. This param is not used any longer
	 * @param editTable the name of the table used for this query
	 * @param query the rest of the query terms
	 *     cursor is finalized
	 * @deprecated use {@link #SQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)} instead
	 */
	@Deprecated
	public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
	                    String editTable, SQLiteQuery query) {
		this(driver, editTable, query);
	}

	/**
	 * Execute a query and provide access to its result set through a Cursor
	 * interface. For a query such as: {@code SELECT name, birth, phone FROM
	 * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
	 * phone) would be in the projection argument and everything from
	 * {@code FROM} onward would be in the params argument.
	 *
	 * @param editTable the name of the table used for this query
	 * @param query the {@link SQLiteQuery} object associated with this cursor object.
	 */
	public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
		if (query == null) {
			if (ATLLoadedApp.getPrimaryApplication().pkg.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				throw new IllegalArgumentException("query object cannot be null");
			else
				throw new NullPointerException("query object cannot be null");
		}
		mDriver = driver;
		mEditTable = editTable;
		mColumnNameMap = null;
		mQuery = query;

		mColumns = query.getColumnNames();
	}

	/**
	 * Get the database that this cursor is associated with.
	 * @return the SQLiteDatabase that this cursor is associated with.
	 */
	public SQLiteDatabase getDatabase() {
		return mQuery.getDatabase();
	}

	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		// Make sure the row at newPosition is present in the window
		if (mWindow == null || newPosition < mWindow.getStartPosition()
		    || newPosition >= (mWindow.getStartPosition() + mWindow.getNumRows())) {
			fillWindow(newPosition);
		}

		return true;
	}

	@Override
	public int getCount() {
		if (mCount == NO_COUNT) {
			fillWindow(0);
		}
		return mCount;
	}

	/*
	** The AbstractWindowClass contains protected methods clearOrCreateWindow() and
	** closeWindow(), which are used by the android.database.sqlite.* version of this
	** class. But, since they are marked with "@hide", the following replacement
	** versions are required.
	*/
	private void awc_clearOrCreateWindow(String name) {
		CursorWindow win = getWindow();
		if (win == null) {
			win = new CursorWindow(name);
			setWindow(win);
		} else {
			win.clear();
		}
	}
	private void awc_closeWindow() {
		setWindow(null);
	}

	private void fillWindow(int requiredPos) {
		awc_clearOrCreateWindow(getDatabase().getPath());

		try {
			if (mCount == NO_COUNT) {
				int startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos, 0);
				mCount = mQuery.fillWindow(mWindow, startPos, requiredPos, true);
				mCursorWindowCapacity = mWindow.getNumRows();
				if (Log.isLoggable(TAG, Log.DEBUG)) {
					Log.d(TAG, "received count(*) from native_fill_window: " + mCount);
				}
			} else {
				int startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos,
				                                                               mCursorWindowCapacity);
				mQuery.fillWindow(mWindow, startPos, requiredPos, false);
			}
		} catch (RuntimeException ex) {
			// Close the cursor window if the query failed and therefore will
			// not produce any results.  This helps to avoid accidentally leaking
			// the cursor window if the client does not correctly handle exceptions
			// and fails to close the cursor.
			awc_closeWindow();
			throw ex;
		}
		// fillWindow was async pre Android 4.0, so we need to notify observers to not hang the old CTS tests
		if (ATLLoadedApp.getPrimaryApplication().pkg.applicationInfo.targetSdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			final Looper looper = Looper.myLooper();
			if (looper != null) {
				new Handler(looper).post(new Runnable() {
					@Override
					public void run() {
						if (!isClosed())
							notifyDataSetObservers();
					}
				});
			}
		}
	}

	@Override
	public int getColumnIndex(String columnName) {
		// Create mColumnNameMap on demand
		if (mColumnNameMap == null) {
			String[] columns = mColumns;
			int columnCount = columns.length;
			HashMap<String, Integer> map = new HashMap<String, Integer>(columnCount, 1);
			for (int i = 0; i < columnCount; i++) {
				map.put(columns[i], i);
			}
			mColumnNameMap = map;
		}

		// Hack according to bug 903852
		final int periodIndex = columnName.lastIndexOf('.');
		if (periodIndex != -1) {
			Exception e = new Exception();
			Log.e(TAG, "requesting column name with table name -- " + columnName, e);
			columnName = columnName.substring(periodIndex + 1);
		}

		Integer i = mColumnNameMap.get(columnName);
		if (i != null) {
			return i.intValue();
		} else {
			return -1;
		}
	}

	@Override
	public String[] getColumnNames() {
		return mColumns;
	}

	@Override
	public void deactivate() {
		super.deactivate();
		mDriver.cursorDeactivated();
	}

	@Override
	public void close() {
		super.close();
		synchronized (this) {
			mQuery.close();
			mDriver.cursorClosed();
		}
	}

	@Override
	public boolean requery() {
		if (isClosed()) {
			return false;
		}

		synchronized (this) {
			if (!mQuery.getDatabase().isOpen()) {
				return false;
			}

			if (mWindow != null) {
				mWindow.clear();
			}
			mPos = -1;
			mCount = NO_COUNT;

			// Pending updates are keyed by row id.  The data set is about
			// to change, so those ids may no longer refer to the same rows.
			// Drop them to avoid applying updates to the wrong rows later.
			if (mUpdatedRows != null)
				mUpdatedRows.clear();

			mDriver.cursorRequeried(this);
		}

		try {
			return super.requery();
		} catch (IllegalStateException e) {
			// for backwards compatibility, just return false
			Log.w(TAG, "requery() failed " + e.getMessage(), e);
			return false;
		}
	}

	@Override
	public void setWindow(CursorWindow window) {
		super.setWindow(window);
		mCount = NO_COUNT;
	}

	// --- Row update / delete support ---
	private boolean mRowIdColumnInitialized = false;

	// Tracks updates per row id, using the inherited mUpdatedRows field.
	// Each entry maps column index (as a string) -> value.
	private Map<String, Object> getOrCreateRowUpdates(long rowId) {
		if (mUpdatedRows == null)
			mUpdatedRows = new HashMap<Long, Map<String, Object>>();
		Map<String, Object> updates = mUpdatedRows.get(rowId);
		if (updates == null) {
			updates = new HashMap<String, Object>();
			mUpdatedRows.put(rowId, updates);
		}
		return updates;
	}

	private long getRowIdAtPosition(int position) {
		if (!mRowIdColumnInitialized) {
			// Find the _id column index from the column names.
			String[] cols = mColumns;
			mRowIdColumnIndex = -1;
			for (int i = 0; i < cols.length; i++) {
				if ("_id".equals(cols[i])) {
					mRowIdColumnIndex = i;
					break;
				}
			}
			mRowIdColumnInitialized = true;
		}
		if (mRowIdColumnIndex < 0)
			throw new IllegalStateException(
			    "Cannot determine the row id because the cursor has no _id column.");
		return mWindow.getLong(position, mRowIdColumnIndex);
	}

	@Override
	public boolean updateString(int columnIndex, String value) {
		checkPosition();
		if (mWindow.putString(value, mPos, columnIndex)) {
			getOrCreateRowUpdates(getRowIdAtPosition(mPos)).put(Integer.toString(columnIndex), value);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateInt(int columnIndex, int value) {
		checkPosition();
		if (mWindow.putLong((long)value, mPos, columnIndex)) {
			getOrCreateRowUpdates(getRowIdAtPosition(mPos)).put(Integer.toString(columnIndex), (Long)(long)value);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateBlob(int columnIndex, byte[] value) {
		checkPosition();
		if (mWindow.putBlob(value, mPos, columnIndex)) {
			getOrCreateRowUpdates(getRowIdAtPosition(mPos)).put(Integer.toString(columnIndex), value);
			return true;
		}
		return false;
	}

	public boolean commitUpdates() {
		if (mUpdatedRows != null && !mUpdatedRows.isEmpty()) {
			for (Map.Entry<Long, Map<String, Object>> rowEntry : mUpdatedRows.entrySet()) {
				long rowId = rowEntry.getKey();
				Map<String, Object> updates = rowEntry.getValue();
				ContentValues values = new ContentValues();
				for (Map.Entry<String, Object> colEntry : updates.entrySet()) {
					int colIndex = Integer.parseInt(colEntry.getKey());
					Object val = colEntry.getValue();
					if (val instanceof Long)
						values.put(mColumns[colIndex], (Long)val);
					else if (val instanceof String)
						values.put(mColumns[colIndex], (String)val);
					else if (val instanceof byte[])
						values.put(mColumns[colIndex], (byte[])val);
				}
				getDatabase().update(mEditTable, values, "_id=" + rowId, null);
				// Legacy sync support: flag (or propagate the flag to) the
				// _sync_dirty column of the affected row.
				getDatabase().rowUpdated(mEditTable, rowId);
			}
			mUpdatedRows.clear();
		}
		return true;
	}

	public boolean deleteRow() {
		checkPosition();
		long rowId = getRowIdAtPosition(mPos);
		int numDeleted = getDatabase().delete(mEditTable, "_id=" + rowId, null);
		if (numDeleted > 0) {
			boolean success = requery();
			if (mUpdatedRows != null)
				mUpdatedRows.clear();
			return success;
		}
		return false;
	}

	public boolean hasUpdates() {
		return mUpdatedRows != null && !mUpdatedRows.isEmpty();
	}

	public boolean supportsUpdates() {
		return true;
	}

	/**
	 * Changes the selection arguments. The new values take effect after a call to requery().
	 */
	public void setSelectionArguments(String[] selectionArgs) {
		mDriver.setBindArguments(selectionArgs);
	}

	/**
	 * Release the native resources, if they haven't been released yet.
	 */
	@Override
	protected void finalize() {
		try {
			// if the cursor hasn't been closed yet, close it first
			if (mWindow != null) {
				close();
			}
		} finally {
			super.finalize();
		}
	}
}
