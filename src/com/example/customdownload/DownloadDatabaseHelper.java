package com.example.customdownload;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class DownloadDatabaseHelper {
	private static final String TAG = DownloadDatabaseHelper.class.getSimpleName();
	
	/** Database filename */
    public static final String DB_NAME = "downloads.db";
    /** Current database version */
    public static final int DB_VERSION = 1;
    /** Name of table in the database */
    public static final String DB_TABLE = "downloads";

    private static DownloadDatabaseHelper mInstance = null;
    
    private DBOpenHelper mDBOpenHelper;
    private SQLiteDatabase mSQLiteDatabase;
	
	/**
     * Creates and updated database on demand when opening it. Helper class to
     * create database the first time the provider is initialized and upgrade it
     * when a new version of the provider needs an updated version of the
     * database.
     */
	private final class DBOpenHelper extends SQLiteOpenHelper {
		
		public DBOpenHelper(final Context context) {
		    super(context, DB_NAME, null, DB_VERSION);
		}
		
		/**
		 * Creates database the first time we try to open it.
		 */
		@Override
		public void onCreate(final SQLiteDatabase db) {
		    Log.d(TAG, "DBOpenHelper:onCreate: Enter");
		    
		    createDownloadsTable(db);
		}

		/**
		 * Updates the database format when a content provider is used with a
		 * database that was created with a different format.
		 * 
		 * Note: to support downgrades, creating a table should always drop it
		 * first if it already exists.
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, int oldVer, final int newVer) {
			Log.d(TAG, "DBOpenHelper:onUpgrade: oldVer = " + oldVer + ", newVer = " + newVer);
		}

		/**
		 * Creates the table that'll hold the download information.
		 */
		private void createDownloadsTable(SQLiteDatabase db) {
			Log.d(TAG, "DBOpenHelper:createDownloadsTable: Enter");
		    try {
				db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
				db.execSQL("CREATE TABLE " + DB_TABLE + "(" 
					+ Downloads._ID + " INTEGER PRIMARY KEY,"
					+ Downloads.COLUMN_URI + " TEXT, "
					+ Downloads.COLUMN_APP_DATA + " TEXT, "
					+ Downloads.COLUMN_FILE_NAME + " TEXT, "
					+ Downloads.COLUMN_FILE_PATH + " TEXT, "
					+ Downloads.COLUMN_MIME_TYPE + " TEXT, "
					+ Downloads.COLUMN_VISIBILITY + " INTEGER, "
					+ Downloads.COLUMN_STATUS + " INTEGER, "
					+ Downloads.COLUMN_LAST_MODIFICATION + " BIGINT, "
					+ Downloads.COLUMN_COOKIE_DATA + " TEXT, "
					+ Downloads.COLUMN_USER_AGENT + " TEXT, "
					+ Downloads.COLUMN_REFERER + " TEXT, "
					+ Downloads.COLUMN_TOTAL_BYTES + " INTEGER, "
					+ Downloads.COLUMN_CURRENT_BYTES + " INTEGER, "
					+ Downloads.COLUMN_ETAG + " TEXT, " 
					+ Downloads.COLUMN_DESCRIPTION + " TEXT); ");
		    } catch (SQLException ex) {
				Log.e(TAG, "DBOpenHelper:createDownloadsTable: couldn't create table in downloads database");
				throw ex;
		    }
		}
	}
	
	/**
	 * 获取DownloadDatabaseHelper实例
	 * @return
	 */
	public static DownloadDatabaseHelper getInstance() {
		if (null == mInstance) {
			mInstance = new DownloadDatabaseHelper();
		}
		return mInstance;
	}
	
	private DownloadDatabaseHelper() {
		
	}
	
	/**
	 * 初始化DownloadDatabaseHelper
	 * @param context
	 */
	public void init(Context context) {
		mDBOpenHelper = new DBOpenHelper(context);
		mSQLiteDatabase = mDBOpenHelper.getWritableDatabase();
	}
	
	/**
	 * 获取所有下载任务的ID列表
	 * @return
	 */
	public ArrayList<Long> getTaskIdList() {
		Log.d(TAG, "getTaskIdList: Enter");
		
		String[] column = {Downloads._ID};
		Cursor cursor = query(false, DB_TABLE, column, null, null, null, null, null, null);
		if (null == cursor) {
			Log.d(TAG, "getTaskIdList: cursor = null");
			return null;
		}
		
		int count = cursor.getCount();
		Log.d(TAG, "getTaskIdList: cursor's count = " + count);
		if (0 == count) {
			return null;
		}
		
		int columnCount = cursor.getColumnCount();
		Log.d(TAG, "getTaskIdList: columnCount = " + columnCount);
		ArrayList<Long> taskIdList = new ArrayList<Long>();
		String columnName;
		long id;
		while(cursor.moveToNext()) {
			for (int i = 0; i < columnCount; i++) {
				columnName = cursor.getColumnName(i);
				id = cursor.getLong(i);
				Log.d(TAG, "getTaskIdList: i = " + i + ", columnName = " + columnName + ", id = " + id);
				taskIdList.add(id);
			}
		}
		
		return taskIdList;
	}
	
	/**
	 * 获取所有下载任务的任务信息
	 * @return
	 */
	public ArrayList<TaskInfo> getTaskInfoList() {
		Log.d(TAG, "getTaskInfoList: Enter");
		
		Cursor cursor = query(false, DB_TABLE, null, null, null, null, null, null, null);
		if (null == cursor) {
			Log.d(TAG, "getTaskInfoList: cursor = null");
			return null;
		}
		
		int count = cursor.getCount();
		Log.d(TAG, "getTaskInfoList: cursor's count = " + count);
		if (0 == count) {
			return null;
		}
		
		int columnCount = cursor.getColumnCount();
		Log.d(TAG, "getTaskInfoList: columnCount = " + columnCount);
		ArrayList<TaskInfo> taskInfoList = new ArrayList<TaskInfo>();
		String columnName;
		while(cursor.moveToNext()) {
			TaskInfo taskInfo = new TaskInfo();
			for (int i = 0; i < columnCount; i++) {
				columnName = cursor.getColumnName(i);
				Log.d(TAG, "getTaskInfoList: i = " + i + ", columnName = " + columnName);
				if (columnName.equalsIgnoreCase(Downloads._ID)) {
					taskInfo.setId(cursor.getLong(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_FILE_NAME)) {
					taskInfo.setFileName(cursor.getString(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_STATUS)) {
					taskInfo.setTaskStatus(cursor.getInt(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_URI)) {
					taskInfo.setTaskUri(cursor.getString(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_TOTAL_BYTES)) {
					taskInfo.setTotalBytes(cursor.getLong(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_CURRENT_BYTES)) {
					taskInfo.setCurrentBytes(cursor.getLong(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_FILE_PATH)) {
					taskInfo.setSavePath(cursor.getString(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_ETAG)) {
					taskInfo.setEtag(cursor.getString(i));
				}
			}
			taskInfo.print();
			taskInfoList.add(taskInfo);
		}
		
		return taskInfoList;
	}
	
	/**
	 * 根据任务ID获取任务信息
	 * @param taskId
	 * @return
	 */
	public TaskInfo getTaskInfoById(long taskId) {
		Log.d(TAG, "getTaskInfoById: Enter");
		
		String whereClause = Downloads._ID + "=?";
		String[] whereArgs = {String.valueOf(taskId)};
		Cursor cursor = query(false, DB_TABLE, null, whereClause, whereArgs, null, null, null, null);
		if (null == cursor) {
			Log.d(TAG, "getTaskInfoById: cursor = null");
			return null;
		}
		
		int count = cursor.getCount();
		Log.d(TAG, "getTaskInfoById: cursor's count = " + count);
		if (0 == count) {
			return null;
		}
		
		int columnCount = cursor.getColumnCount();
		Log.d(TAG, "getTaskInfoById: columnCount = " + columnCount);
		TaskInfo taskInfo = new TaskInfo();
		String columnName;
		while(cursor.moveToNext()) {
			for (int i = 0; i < columnCount; i++) {
				columnName = cursor.getColumnName(i);
				Log.d(TAG, "getTaskInfoList: i = " + i + ", columnName = " + columnName);
				if (columnName.equalsIgnoreCase(Downloads._ID)) {
					taskInfo.setId(cursor.getLong(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_FILE_NAME)) {
					taskInfo.setFileName(cursor.getString(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_STATUS)) {
					taskInfo.setTaskStatus(cursor.getInt(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_URI)) {
					taskInfo.setTaskUri(cursor.getString(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_TOTAL_BYTES)) {
					taskInfo.setTotalBytes(cursor.getLong(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_CURRENT_BYTES)) {
					taskInfo.setCurrentBytes(cursor.getLong(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_FILE_PATH)) {
					taskInfo.setSavePath(cursor.getString(i));
				} else if (columnName.equalsIgnoreCase(Downloads.COLUMN_ETAG)) {
					taskInfo.setEtag(cursor.getString(i));
				}
			}
			taskInfo.print();
		}
		
		return taskInfo;
	}
	
	/**
	 * 插入一项数据到数据库
	 * @param values 指定行对应的列的值，这个类型很类似Map，key表示列的名称，values表示列的值
	 * @return 返回新插入的行的ID，如果存在错误默认返回 -1
	 */
	public long insert(ContentValues values) {
		Log.d(TAG, "insert: Enter");
		
		return insert(DB_TABLE, null, values);
	}
	
	/**
	 * 更新一项数据
	 * @param id 任务ID
	 * @param values 指定行对应的列的值，这个类型很类似Map，key表示列的名称，values表示列的值
	 * @return
	 */
	public long update(long id, ContentValues values) {
		Log.d(TAG, "update: id = " + id);
		
		String whereClause = Downloads._ID + "=?";
		String[] whereArgs = {String.valueOf(id)};
		
		return update(DB_TABLE, values, whereClause, whereArgs);
	}
	
	/**
	 * 插入一项数据到数据库
	 * @param table 需要插入行的表的名称
	 * @param nullColumnHack 这个参数是可选的，可以为null, SQL 不允许插入一个至少一列都不命名的完全空的行。如果你提供的 values 是空的，而且没有已知的的列名，这就是一个空行，这是不能被插入的。如果设置非空，nullColumnHack 参数提供一个可为空的列的名称，当插入的 values 是空的时候，就将这个列名置为NULL，然后values值插入。
	 * @param values 指定行对应的列的值，这个类型很类似Map，key表示列的名称，values表示列的值
	 * @return 返回新插入的行的ID，如果存在错误默认返回 -1
	 */
	public long insert(String table, String nullColumnHack, ContentValues values) {
		Log.d(TAG, "insert: table = " + table + ", nullColumnHack = " + nullColumnHack);
		
		return mSQLiteDatabase.insert(table, nullColumnHack, values);
	}
	
	/**
	 * 删除一项数据
	 * @param table 需要插入行的表的名称
	 * @param whereClause 可选项，是可以通过 SQL语句中where语句来过滤条件删除的条目，如果是null 表示删除所有行
	 * @param whereArgs 紧跟第二个参数，作为删除过滤条件的占位符，详情请看下面程序 PersonDao2 的 deletePerson() 方法的操作。
	 * @return 如果是 0 表示未删除任何行，如果已经有删除行的操作 会得到 count > 0的数，表示删除的行数
	 */
	public long delete(String table, String whereClause, String[] whereArgs) {
		Log.d(TAG, "delete: table = " + table + ", whereClause = " + whereClause);
		
		return mSQLiteDatabase.delete(table, whereClause, whereArgs);
	}
	
	/**
	 * 更新一项数据
	 * @param table 需要插入行的表的名称
	 * @param values 指定行对应的列的值，这个类型很类似Map，key表示列的名称，values表示列的值
	 * @param whereClause 可选项，是可以通过 SQL语句中where语句来过滤条件删除的条目，如果是null 表示删除所有行
	 * @param whereArgs 紧跟第二个参数，作为删除过滤条件的占位符，详情请看下面程序 PersonDao2 的 deletePerson() 方法的操作。
	 * @return
	 */
	public long update(String table, ContentValues values, String whereClause, String[] whereArgs) {
		Log.d(TAG, "update: table = " + table + ", values = " + values + ", whereClause = " + whereClause);
		
		return mSQLiteDatabase.update(table, values, whereClause, whereArgs);
	}
	
	/**
	 * 查询
	 * @param distinct 判断是否返回的行是唯一值，如果想要返回唯一的行，则为true，否则为false
	 * @param table 需要查询的表的名称
	 * @param columns 需要返回的列，如果要返回所有列则置为null
	 * @param selection 过滤需要返回的行，格式遵从SQL中 SQL WHERE 语句(除了Where关键字以外).如果返回给定表的所有行，则置为Null
	 * @param selectionArgs 过滤条件的占位符，这里的值会代替过滤语句中 "?"
	 * @param groupBy 过滤条件对行进行分组，格式遵从SQL中 SQL GROUP BY 语句 (除了关键字GROUP BY之外)，如果不分组，置为Null
	 * @param having 对分组过滤条件的占位符操作
	 * @param orderBy 如何进行排序，遵从SQL中的SQL ORDER BY 语句, 如果是null表示使用默认的排序顺序
	 * @param limit 是否对数据库进行分页的查询
	 * @return 返回的是一个游标
	 */
	public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		Log.d(TAG, "query: distinct = " + distinct + ", table = " + table + ", selection = " + selection + ", groupBy = " + groupBy + ", having = " + having + ", orderBy = " + orderBy + ", limit = " + limit);
		
		return mSQLiteDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}
}
