/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import SparkBird.Utility.CrashHandler;
import SparkBird.Utility.HttpHelper;
import SparkBird.Utility.UtilityConst;
import SparkBird.Utility.WriteLogSendToServer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.mobclick.android.MobclickAgent;

public class Welcome_Activity extends Activity {
	private SharedPreferences preferences;
	private TelephonyManager mTelephonyMgr;
	private String ClientVersion = null;
	private String Rid = null;
	private final String DATABASE_PATH = "/data/data/SparkBird.SparkBirdWayInfo/databases/";
	private final String DATABASE_NAME = "sparkbird.db";
	private String shortCutFlag = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// the process bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// set the background
		setContentView(R.layout.welcome);

		CrashHandler crashHandler = CrashHandler.getInstance();
		// 注册crashHandler
		crashHandler.init(this);
		// 发送以前没发送的报告(可选)
		WriteLogSendToServer.sendPreviousReportsToServer(this);

		// the process bar
		setProgressBarIndeterminateVisibility(true);
		try {
			// Init the soft Information
			Init();
			// Init Soft Setting
			new Init().execute("");

			// 在欢迎画面显示版本号
			TextView wel_version = (TextView) findViewById(R.id.wel_version);
			wel_version.setText("版本号：" + getVersionName());
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}

	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	public void onBackPressed() {
		//在欢迎画面禁止用户点击返回按钮
	    //super.onBackPressed();
	}
	
	private void MoveToNext(String Flag) {
		Intent intent = new Intent();
		intent.setClass(Welcome_Activity.this, MainWayInfo_Activity.class);
		Bundle bundle = new Bundle();
		bundle.putString("Flag", Flag);
		bundle.putString("shortCutFlag", shortCutFlag);
		intent.putExtras(bundle);
		startActivity(intent);
		finish();
	}

	private void Init() {
		// client version
		ClientVersion = getVersionName();
		// Init SparkBird Info
		preferences = getSharedPreferences(UtilityConst.SHAREDPREFERENCES,
				MODE_PRIVATE);
		Rid = preferences.getString(UtilityConst.SP_KEY_RID,
				UtilityConst.NULL_VALUE);

		// get ClientVersion
		mTelephonyMgr = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
	}

	public class Init extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {

			InitDB();
			String result = UtilityConst.RESPONSE_NORMAL;
			if (!HttpHelper.isNetworkAvailable(Welcome_Activity.this)) {
				return UtilityConst.RESPONSE_NETWORK_NOAVILABLE;
			}
			if (Rid.equals(UtilityConst.NULL_VALUE)) {
				result = register();
				// 用户第一次使用时，创建桌面快捷方式。
				shortCutFlag = UtilityConst.RESPONSE_NEW_USER;
			} else {
				result = checkRegisterInfo();
			}
			return result;
		}

		private String register() {
			String sdkVersion = android.os.Build.VERSION.SDK;
			String model = android.os.Build.MODEL;
			String os_version = android.os.Build.VERSION.RELEASE;
			String imsi = mTelephonyMgr.getSubscriberId();
			String imei = mTelephonyMgr.getDeviceId();

			String result = HttpHelper.Register(imsi, imei, os_version, model,
					sdkVersion, ClientVersion);
			if (result.startsWith(UtilityConst.RESPONSE_NORMAL)) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(UtilityConst.SP_KEY_RID,
						result.substring(2, result.length()));
				editor.commit();
				return UtilityConst.RESPONSE_NORMAL;
			}
			return result;
		}

		private String checkRegisterInfo() {
			String result = HttpHelper.CheckRegisterInfo(Rid, ClientVersion);
			return result;
		}

		protected void onPostExecute(String result) {

			Log.v("Result", result);
			MoveToNext(result);
		}

	}

	/**
	 * 取得应用程序当前的版本号
	 * 
	 */
	public String getVersionName() {
		try {
			String versionName = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName;
			return versionName;
		} 
		catch (android.content.pm.PackageManager.NameNotFoundException e) 
		{
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
			return null;
		}
	}

	private void InitDB() {

		String outFileName = DATABASE_PATH + DATABASE_NAME;
		File dir = new File(outFileName);
		if (dir.exists())
			return;

		dir = new File(DATABASE_PATH);
		if (!dir.exists())
			dir.mkdir();
		InputStream input = null;
		OutputStream output = null;
		input = getResources().openRawResource(R.raw.sparkbird);
		try {
			output = new FileOutputStream(outFileName);

			byte[] buffer = new byte[2048];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		} catch (IOException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		} finally {
			try {
				output.flush();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(this, e);
			}
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(this, e);
			}
		}

	}
}
