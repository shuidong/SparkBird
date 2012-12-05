/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.Utility;

public class UtilityConst {

	public final static String SHAREDPREFERENCES = "compass";
	public final static String SP_KEY_RID = "rid";
	public final static String SP_KEY_AT_REFRESH = "autorefresh";
	public final static String SP_KEY_DB = "DBVersion";
	public final static String DB_VERSION = "1";
	public final static String NULL_VALUE = "NULL";
	public final static int INT_99_VALUE = 99;
	
	public final static String RESPONSE_NETWORK_NOAVILABLE = "-1";
	public final static String RESPONSE_NORMAL = "0";
	public final static String RESPONSE_SERVER_ERROR = "1";
	public final static String RESPONSE_NO_RID = "2";
	public final static String RESPONSE_OLD_CLIENT_VERSION = "9";
	public final static String RESPONSE_NEW_USER = "66";

	public final static int DIALOG_NETWORK_NOAVILABLE = -1;
	public final static int DIALOG_SERVER_ERROR = 1;
	public final static int DIALOG_NO_RID = 2;
	public final static int DIALOG_OLD_CLIENT_VERSION = 9;
	public final static int DIALOG_NEW_CLIENT_VERSION = 90;
	public final static int DIALOG_NO_SHOUSHUO_TTS = 10;
	public final static int DIALOG_NEW_USER = 66;

	public final static int MUTI_CHOICE_DIALOG = 11;

	public final static String downloadDir = "SparkBird";
	
	public static final int POISEARCH = 1000;
	public static final int ERROR = 1001;
	public static final int FIRST_LOCATION = 1002;
	
	public static final int ROUTE_START_SEARCH=2000;//路径规划起点搜索
	public static final int ROUTE_END_SEARCH=2001;//路径规划起点搜索
	public static final int ROUTE_SEARCH_RESULT=2002;//路径规划结果
	public static final int ROUTE_SEARCH_ERROR=2004;//路径规划起起始点搜索异常
	
	
	public static final int GEOCODER_RESULT=3000;//地理编码结果
	public static final int DIALOG_LAYER=4000;

}
