/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.Utility;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Utility {

	/**
	 * 从CityMaster表中取出所有道路名称存入数组中
	 * 
	 */
	public static String[] getWayInfoFromCityMaster(Context ctx, String strCityName) {
		String[] strWayMaster = null;
		Cursor cursor = null;
		DataBaseHelper dbhelper = null;
		try {
			int i = 0;
			dbhelper = new DataBaseHelper(ctx, "sparkbird.db", null, 1);
			SQLiteDatabase getReadableDatabase = dbhelper.getReadableDatabase();
			/*
			 * Cursor cursor = getReadableDatabase.query("Dalian", new String[]
			 * { "WayID", "WayName" }, "WayID=?", new String[] {"1"}, null,
			 * null, null);
			 */
			cursor = getReadableDatabase.rawQuery("select WayID,WayName from "
					+ strCityName + " order by 1", null);

			strWayMaster = new String[cursor.getCount()];

			while (cursor.moveToNext()) {
				String wayrecord = cursor.getString(cursor
						.getColumnIndex("WayName"));

				/*
				 * Log.v("DB***********",
				 * cursor.getString(cursor.getColumnIndex("WayName")));
				 */

				Log.v("master表中的路段名", wayrecord);

				strWayMaster[i] = wayrecord;
				i++;
			}
			return strWayMaster;

		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(ctx, e);
		} finally {
			cursor.close();
			dbhelper.close();
		}

		return null;

	}

}
