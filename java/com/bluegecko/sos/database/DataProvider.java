package com.bluegecko.sos.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DataProvider extends ContentProvider {
	private static String TAG = "DataProvider";

	public static final String AUTHORITY = "com.bluegecko.sos.dataprovider";
	public static final String RECIPIENTS = "recipients";
	public static final String RECIPIENTS_URL = "content://" + AUTHORITY + "/" + RECIPIENTS;

	private SQLiteDatabase mDb;
	private static final String DATABASE_NAME = "sos";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_RECIPIENTS = "recipients";
	public static final String RECIPIENTS_ID = "_id";
	public static final String RECIPIENTS_RANK = "rank";
	public static final String RECIPIENTS_PHONE = "phone";
	public static final String RECIPIENTS_NAME = "name";
	public static final String RECIPIENTS_MESSAGE = "message";
	public static final String RECIPIENTS_STATUS = "status";
	public static final String RECIPIENTS_POSITION = "position";
	public static final String RECIPIENTS_CALLING = "calling";

	private Context mContext;

	static final int I_RECIPIENTS = 10;
	static final int I_RECIPIENTS_ID = 20;

	static final UriMatcher uriMatcher;
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, RECIPIENTS, I_RECIPIENTS);
		uriMatcher.addURI(AUTHORITY, RECIPIENTS + "/#", I_RECIPIENTS_ID);
	}

	public static Uri Get_URI (String s){
		Uri result;
		if (s.startsWith(RECIPIENTS)) {
			result = Uri.parse("content://" + AUTHORITY + "/" + s);
			return result;
		} else
			return null;
	}

	// Commande sql pour la création de la base de données -> table RECIPIENTS
	private static final String RECIPIENTS_CREATE = "create table "
			+ TABLE_RECIPIENTS + "("
			+ RECIPIENTS_ID + " integer primary key autoincrement, "
			+ RECIPIENTS_RANK + " integer, "
			+ RECIPIENTS_PHONE + " text, "
			+ RECIPIENTS_NAME + " string, "
			+ RECIPIENTS_MESSAGE + " string, "
			+ RECIPIENTS_STATUS + " integer, "              //-1:indéterminé; 0:attente; 1:vu; 2:refus; 3:j'arrive
			+ RECIPIENTS_POSITION + " integer,"
			+ RECIPIENTS_CALLING + ");" ;

	public DataProvider() {
	}

	/**
	 * Helper class that actually creates and manages
	 * the provider's underlying data repository.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * Export data to .csv file
		 * @param tableName : table to be exported
		 */
		private boolean ExportTable(SQLiteDatabase db, String tableName) {
			Cursor c;
			File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			if (!exportDir.exists()) {
				if (!exportDir.mkdirs()){
					return false;
				}
			}
			c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='"+tableName+"'", null);
			if (c.getCount() > 0) {
				String filename = tableName + ".csv";
				c = db.rawQuery("select * from " + tableName, null);
				File saveFile = new File(exportDir, filename);
				try {
					FileWriter fw = new FileWriter(saveFile);
					BufferedWriter bw = new BufferedWriter(fw);
					int rowcount = c.getCount();
					if (rowcount > 0) {
						String[] columnNames = c.getColumnNames();              // all columns headers
						String[] items = new String[columnNames.length-1];      // container for headers except '_id'
						System.arraycopy(columnNames, 1, items, 0, columnNames.length - 1);
						bw.write(TextUtils.join(",",items ));                   // join headers with ; and write in bw
						bw.newLine();                                           // new line
						c.moveToFirst();
						while (!c.isAfterLast()) {                              // for each data line
							for (int i=1; i< columnNames.length; i++){          // for each data (except 'id')
								items[i-1] = c.getString(i);                    // copy
							}
							bw.write(TextUtils.join(",",items ));               // join data with ; and write in bw
							bw.newLine();
							c.moveToNext();
						}
						bw.flush();
						Log.d(TAG, ": " + tableName + " exported Successfully.");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				c.close();
				return true;
			} else {
				return false;
			}

		}

		/**
		 * Import data from .csv file
		 * @param tableName : table to be append with data
		 */
		private void ImportTable(SQLiteDatabase db, String tableName){
			try{
				File importDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				InputStream inStream = new FileInputStream(new File(importDir, tableName + ".csv"));
				BufferedReader buffer = new BufferedReader(new InputStreamReader(inStream));
				db.beginTransaction();
				String line = buffer.readLine();
				if (line!=null) {
					Cursor c = db.query(tableName, null, null,null,null,null,null);
					String[] columsNames = c.getColumnNames();
					int colCount = c.getColumnCount();
					c.close();
					if (columsNames.length==colCount){
						ContentValues cv;
						while ((line = buffer.readLine()) != null) {
							try {
								String[] colums = line.split(",");
								cv = new ContentValues();
								for (int i = 0; i < colums.length; i++) {
									String value = colums[i].trim();
									if (value.equals("null"))
										value = null;
									cv.put(columsNames[i + 1], value);
								}
								db.insert(tableName, null, cv);
							} catch (Exception e) {
								Log.w("CSVParser", "Skipping Bad CSV Row in " + tableName + ": " + line);
							}
						}
						Log.d(TAG, ": " + tableName + " imported Successfully.");
					}
				} else {
					Log.w("CSVParser", " wrong rows count for " + tableName);
				}
				db.setTransactionSuccessful();
				db.endTransaction();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(RECIPIENTS_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			ExportTable(db, TABLE_RECIPIENTS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPIENTS);
			onCreate(db);
			ImportTable(db, TABLE_RECIPIENTS);
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgrade(db, oldVersion, newVersion);
		}
	}


	/**
	 * Create a write able database which will trigger its
	 * creation if it doesn't already exist.
	 */
	@Override
	public boolean onCreate() {
		mContext = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(mContext);
		mDb = dbHelper.getWritableDatabase();
		return mDb != null;
	}

	// Add a new record
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		long rowID;
		switch (uriMatcher.match(uri)){
			case I_RECIPIENTS:
				rowID = mDb.insert(TABLE_RECIPIENTS, "", values);
				if (rowID > 0) {
					Uri _uri = ContentUris.withAppendedId(Get_URI(RECIPIENTS), rowID);
					mContext.getContentResolver().notifyChange(_uri, null);
					return _uri;
				}
				break;
			default:
		}
		return null;
	}

	/**
	 * Query builder
	 */
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (uriMatcher.match(uri)) {
			case I_RECIPIENTS:
				qb.setTables(TABLE_RECIPIENTS);
				qb.setProjectionMap(null);
				if (sortOrder == null || sortOrder.equals("")){
					sortOrder = RECIPIENTS_RANK + " ASC" ;	                    // By default sort asc order
				}
				break;
			case I_RECIPIENTS_ID:
				qb.setTables(TABLE_RECIPIENTS);
				qb.appendWhere( RECIPIENTS_ID + "=" + uri.getPathSegments().get(1));
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}


		Cursor c = qb.query(mDb, projection, selection, selectionArgs, null, null, sortOrder);

		// register to watch a content URI for changes
		c.setNotificationUri(mContext.getContentResolver(), uri);
		return c;
	}

	/**
	 * Delete one or several row(s)
	 * @param uri uri
	 * @param selection selection string
	 * @param selectionArgs selection params
	 * @return number of deleted rows
	 */
	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		int count;
		String id;
		switch (uriMatcher.match(uri)){
			case I_RECIPIENTS:
				count = mDb.delete(TABLE_RECIPIENTS, selection, selectionArgs);
				break;
			case I_RECIPIENTS_ID:
				id = uri.getPathSegments().get(1);
				count = mDb.delete( TABLE_RECIPIENTS, RECIPIENTS_ID +  " = " + id +
						(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		mContext.getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 *  Update one or several row(s)
	 * @param uri uri
	 * @param values values
	 * @param selection selection string
	 * @param selectionArgs selection params
	 * @return updated records count
	 */
	@Override
	public int update(@NonNull  Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
			case I_RECIPIENTS:
				// count = mDb.update(TABLE_RECIPIENTS, values, selection, selectionArgs);                // commenté pour désactiver la mise à jour de masse
				break;
			case I_RECIPIENTS_ID:
				count = mDb.update(TABLE_RECIPIENTS, values, RECIPIENTS_ID + " = " + uri.getPathSegments().get(1) +
						(!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri );
		}
		mContext.getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		return null;
	}
}
