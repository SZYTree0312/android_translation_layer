package com.android.internal.database;

import android.database.AbstractCursor;
import java.util.ArrayList;
import java.util.List;

public class ArrayListCursor extends AbstractCursor {
	private String[] columnNames;
	private ArrayList<Object[]> rows;
	private int rowIndex;

	public ArrayListCursor(String[] columnNames, ArrayList<Object[]> rows) {
		this.columnNames = columnNames;
		this.rows = rows;
		rowIndex = -1;
	}

	private Object getField(int column) {
		Object row = rows.get(rowIndex);
		if (row instanceof Object[]) {
			return ((Object[])row)[column];
		} else if (row instanceof ArrayList) {
			return ((ArrayList)row).get(column);
		}
		return null;
	}

	@Override
	public int getCount() {
		return rows.size();
	}

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public String getString(int column) {
		Object obj = getField(column);
		if (obj == null)
			return null;
		return obj.toString();
	}

	@Override
	public short getShort(int column) {
		Object obj = getField(column);
		if (obj instanceof Number)
			return ((Number)obj).shortValue();
		return 0;
	}

	@Override
	public int getInt(int column) {
		Object obj = getField(column);
		if (obj instanceof Number)
			return ((Number)obj).intValue();
		return 0;
	}

	@Override
	public long getLong(int column) {
		Object obj = getField(column);
		if (obj instanceof Number)
			return ((Number)obj).longValue();
		return 0;
	}

	@Override
	public float getFloat(int column) {
		Object obj = getField(column);
		if (obj instanceof Number)
			return ((Number)obj).floatValue();
		return 0;
	}

	@Override
	public double getDouble(int column) {
		Object obj = getField(column);
		if (obj instanceof Number)
			return ((Number)obj).doubleValue();
		return 0;
	}

	@Override
	public boolean isNull(int column) {
		return getField(column) == null;
	}

	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		rowIndex = newPosition;
		return true;
	}

	@Override
	public byte[] getBlob(int column) {
		Object obj = getField(column);
		if (obj instanceof byte[])
			return (byte[])obj;
		return null;
	}

	@Override
	public int getType(int column) {
		Object obj = getField(column);
		if (obj == null)
			return FIELD_TYPE_NULL;
		else if (obj instanceof byte[])
			return FIELD_TYPE_BLOB;
		else if (obj instanceof Float || obj instanceof Double)
			return FIELD_TYPE_FLOAT;
		else if (obj instanceof Integer || obj instanceof Long || obj instanceof Short || obj instanceof Byte)
			return FIELD_TYPE_INTEGER;
		return FIELD_TYPE_STRING;
	}
}
