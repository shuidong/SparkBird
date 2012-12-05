/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import SparkBird.Utility.FileService;
import SparkBird.Utility.Utility;
import SparkBird.Utility.WriteLogSendToServer;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

public class LukuangUpdateService extends Service {
	private String strWayStatus;
	private String strWayInfoRed = "";
	private String[] aryWayInfoRed;
	private String[] strWayMaster;
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private Map<String, Object> map = new HashMap<String, Object>();
	private int i;

	@Override
	public void onCreate() {
		super.onCreate();
		getOldWayInfo();
		i = 0;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		try {
			super.onStart(intent, startId);

			if (i == list.size() - 4) {
				i = 0;
			} else {
				i++;
			}

			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			RemoteViews updateViews = buildUpdate(this);

			if (updateViews != null) {
				manager.updateAppWidget(manager
						.getAppWidgetIds(new ComponentName(this,
								LukuangWidgetProvider.class)), updateViews);
			}

			long now = System.currentTimeMillis();
			// 设置刷新时间间隔
			long updateMilis = 4 * 1000;

			PendingIntent pendingIntent = PendingIntent.getService(this, 0,
					intent, 0);

			// Schedule alarm, and force the device awake for this update
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, now + updateMilis,
					pendingIntent);

			// No updates remaining, so stop service
			// stopSelf();
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}

	private RemoteViews buildUpdate(Context context) {
		Date date = new Date();
		RemoteViews updateViews = null;

		updateViews = new RemoteViews(context.getPackageName(),
				R.layout.lukuang_appwidget);

		updateViews.setTextViewText(R.id.widget_TextView_time,
				String.format("%tr", date));
		updateViews.setTextViewText(R.id.widget_TextView_title,
				"红色饱和路段，每五秒自动翻页");
		updateViews.setTextViewText(R.id.widget_TextView_lukuang1, list.get(i)
				.get("wayinfo").toString());
		updateViews.setTextViewText(R.id.widget_TextView_lukuang2,
				list.get(i + 1).get("wayinfo").toString());
		updateViews.setTextViewText(R.id.widget_TextView_lukuang3,
				list.get(i + 2).get("wayinfo").toString());
		updateViews.setTextViewText(R.id.widget_TextView_lukuang4,
				list.get(i + 3).get("wayinfo").toString());

		// 添加按键响应，跳转到新的Activity
		Intent detailIntent = new Intent(context, Welcome_Activity.class);
		PendingIntent pending = PendingIntent.getActivity(context, 0,
				detailIntent, 0);
		updateViews.setOnClickPendingIntent(R.id.app_widget_title, pending);
		updateViews.setOnClickPendingIntent(R.id.app_widget_body_01, pending);
		updateViews.setOnClickPendingIntent(R.id.app_widget_body_02, pending);
		updateViews.setOnClickPendingIntent(R.id.app_widget_body_03, pending);
		updateViews.setOnClickPendingIntent(R.id.app_widget_body_04, pending);
		updateViews.setOnClickPendingIntent(R.id.widget_TextView_lukuang1, pending);
		updateViews.setOnClickPendingIntent(R.id.widget_TextView_lukuang2, pending);
		updateViews.setOnClickPendingIntent(R.id.widget_TextView_lukuang3, pending);
		updateViews.setOnClickPendingIntent(R.id.widget_TextView_lukuang4, pending);
		
		return updateViews;
	}

	/**
	 * 与服务器交互失败时，取得上次成功在本地保存的路况数据 本方法为测试用,桌面组件暂时使用本地保存的路况数据
	 * 
	 */
	public void getOldWayInfo() {
		try {
			InputStream inputStream = this.getBaseContext().openFileInput(
					"wayInfo.sb");
			strWayStatus = FileService.read(inputStream);
			inputStream.close();

			String[] aryWayStatus;
			aryWayStatus = strWayStatus.split(";");
			strWayInfoRed = "";

			for (int i = 0; i < aryWayStatus.length; i++) {
				if (aryWayStatus[i].equals("1") == true) {
					strWayInfoRed = strWayInfoRed + i + ",";
				}
			}

			if (strWayInfoRed.length() != 0) {
				aryWayInfoRed = strWayInfoRed.split(",");
			}

			if (aryWayInfoRed != null) {
				strWayMaster = Utility.getWayInfoFromCityMaster(this, "Dalian");

				for (int i = 0; i < aryWayInfoRed.length; i++) {
					String strtmp = strWayMaster[Integer
							.parseInt(aryWayInfoRed[i])];
					map = new HashMap<String, Object>();
					map.put("wayinfo", strtmp);
					list.add(map);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}

}