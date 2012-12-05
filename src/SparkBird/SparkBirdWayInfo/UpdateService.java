/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import SparkBird.Utility.UtilityConst;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class UpdateService extends Service {
	// 标题
	private int titleId = 0;
	// 文件存储
	private File updateDir = null;
	private File updateFile = null;
	// 通知栏
	private NotificationManager updateNotificationManager = null;
	private Notification updateNotification = null;
	// 通知栏跳转Intent
	private Intent updateIntent = null;
	private PendingIntent updatePendingIntent = null;

	// 下载状态
	private final static int DOWNLOAD_COMPLETE = 0;
	private final static int DOWNLOAD_FAIL = 1;

	//TODO
	private final static String updateFileFromServer = "http://www.dalian1008.com/files/sparkbird.apk";

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// 获取传值
		titleId = intent.getIntExtra("titleId", 0);

		Log.v("升級Service", "开始");

		// 创建文件
		// 如果用户安装了SD卡，将文件创建在SD卡上
		if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment
				.getExternalStorageState())) {
			updateDir = new File(Environment.getExternalStorageDirectory(),
					UtilityConst.downloadDir);

		} else {
			// files目录
			updateDir = getFilesDir();
		}
		updateFile = new File(updateDir.getPath(), getResources().getString(
				titleId)
				+ ".apk");

		this.updateNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		this.updateNotification = new Notification();

		// 设置下载过程中，点击通知栏，回到主界面
		// 此处会导致启动多个主界面的activity
		//updateIntent = new Intent(this, MainWayInfo_Activity.class);
		updateIntent = new Intent();
		updatePendingIntent = PendingIntent.getActivity(this, 0, updateIntent,
				0);

		// 设置通知栏显示内容
		updateNotification.icon = R.drawable.sb004title;
		updateNotification.tickerText = "火鸟路况升级程序开始下载";

		// 下载进度以百分比显示
		updateNotification.setLatestEventInfo(this, "火鸟路况升级程序下载", "0%",
				updatePendingIntent);

		// 下载进度以进度条显示
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.update_notification);
        contentView.setProgressBar(SparkBird.SparkBirdWayInfo.R.id.update_notification_progressbar, 100, 0, false);
        contentView.setTextViewText(SparkBird.SparkBirdWayInfo.R.id.update_notification_progresstext, "0%");
        updateNotification.contentView = contentView;
        
		// 发出通知
		updateNotificationManager.notify(0, updateNotification);

		// 开启一个新的线程下载，如果使用Service同步下载，会导致ANR问题，Service本身也会阻塞
		new Thread(new updateRunnable()).start();// 这个是下载的重点，是下载的过程
		return super.onStartCommand(intent, flags, startId);
	}

	private Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DOWNLOAD_COMPLETE:
				// 点击安装PendingIntent
				
				//下载进度以百分比显示
				/*Uri uri = Uri.fromFile(updateFile);
				Intent installIntent = new Intent(Intent.ACTION_VIEW);
				installIntent.setDataAndType(uri,
						"application/vnd.android.package-archive");
				updatePendingIntent = PendingIntent.getActivity(
						UpdateService.this, 0, installIntent, 0);
				updateNotification.defaults = Notification.DEFAULT_SOUND;// 铃声提醒
				updateNotification.setLatestEventInfo(UpdateService.this,
						"火鸟路况", "下载完成,点击安装。", updatePendingIntent);
				updateNotificationManager.notify(0, updateNotification);*/

				//下载进度以进度条显示
				Uri uri = Uri.fromFile(updateFile);
				Intent installIntent = new Intent(Intent.ACTION_VIEW);
				installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
				updatePendingIntent = PendingIntent.getActivity(UpdateService.this, 0, installIntent, 0);
				updateNotification.defaults = Notification.DEFAULT_SOUND;//铃声提醒
				updateNotification.contentIntent = updatePendingIntent;//安装界面
				updateNotification.contentView.setViewVisibility(SparkBird.SparkBirdWayInfo.R.id.update_notification_progressblock, View.GONE);
				updateNotification.contentView.setTextViewText(SparkBird.SparkBirdWayInfo.R.id.update_notification_progresstext, "下载完成，点击安装！");
				updateNotificationManager.notify(0, updateNotification);
				
				// 停止服务
				stopService(updateIntent);
			case DOWNLOAD_FAIL:
				// 下载失败
				updateNotification.setLatestEventInfo(UpdateService.this,
						"火鸟路况", "下载完成,点击安装。", updatePendingIntent);
				updateNotificationManager.notify(0, updateNotification);
			default:
				stopService(updateIntent);
			}
		}
	};

	class updateRunnable implements Runnable {
		Message message = updateHandler.obtainMessage();

		public void run() {
			message.what = DOWNLOAD_COMPLETE;

			try {
				// 增加权限<USES-PERMISSION
				// android:name="android.permission.WRITE_EXTERNAL_STORAGE">;
				if (!updateDir.exists()) {
					updateDir.mkdirs();
				}

/*				if (!updateFile.exists()) {
					updateFile.createNewFile();
				}*/
				
				if (updateFile.exists()) {
					updateFile.delete();
				}
				updateFile.createNewFile();

				// 给文件赋权限
				// String cmd = "chmod +x " + updateFile();
				String cmd = "chmod 777 " + updateFile.getPath();
				try {
					Runtime.getRuntime().exec(cmd);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// 下载函数
				// 增加权限<USES-PERMISSION
				// android:name="android.permission.INTERNET">;
				long downloadSize = downloadUpdateFile(updateFileFromServer,
						updateFile);
				if (downloadSize > 0) {
					// 下载成功
					updateHandler.sendMessage(message);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				message.what = DOWNLOAD_FAIL;

				// 下载失败
				updateHandler.sendMessage(message);
			}
		}
	}

	public long downloadUpdateFile(String downloadUrl, File saveFile)
			throws Exception {
		//int downloadCount = 0;
		int currentSize = 0;
		long totalSize = 0;
		int updateTotalSize = 0;

		HttpURLConnection httpConnection = null;
		InputStream is = null;
		FileOutputStream fos = null;

		try {
			URL url = new URL(downloadUrl);
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection
					.setRequestProperty("User-Agent", "PacificHttpClient");

			if (currentSize > 0) {
				httpConnection.setRequestProperty("RANGE", "bytes="
						+ currentSize + "-");
			}

			httpConnection.setConnectTimeout(10000);
			httpConnection.setReadTimeout(20000);
			
			//for Android 2.2 only
			//updateTotalSize = httpConnection.getContentLength();
			//for Android 2.2 only
			
			//for Android 2.2 and 2.3.3
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(downloadUrl);
			HttpResponse response;
			response = client.execute(get);
			HttpEntity entity = response.getEntity();
			updateTotalSize = (int) entity.getContentLength();
			//for Android 2.2 and 2.3.3

			if (httpConnection.getResponseCode() == 404) {
				throw new Exception("fail!");
			}

			is = httpConnection.getInputStream();
			fos = new FileOutputStream(saveFile, false);
			byte buffer[] = new byte[4096];
			int readsize = 0;

			while ((readsize = is.read(buffer)) > 0) {
				fos.write(buffer, 0, readsize);
				totalSize += readsize;
				
/*				// 下载进度以百分比显示
				// 为了防止频繁的通知导致应用吃紧，百分比增加10才通知一次
				if ((downloadCount == 0)
						|| (int) (totalSize * 100 / updateTotalSize) - 10 > downloadCount) {
					downloadCount += 10;
					
					updateNotification.setLatestEventInfo(UpdateService.this,
							"火鸟路况升级程序正在下载", (int) totalSize * 100
									/ updateTotalSize + "%",
							updatePendingIntent);
					updateNotificationManager.notify(0, updateNotification);
				}*/
				
				// 下载进度以进度条显示
				updateNotification.setLatestEventInfo(UpdateService.this,
						"火鸟路况升级程序正在下载", (int) totalSize * 100 / updateTotalSize
								+ "%", updatePendingIntent);

				RemoteViews contentView = new RemoteViews(getPackageName(),
						R.layout.update_notification);
				contentView
						.setProgressBar(
								SparkBird.SparkBirdWayInfo.R.id.update_notification_progressbar,
								100, (int) (totalSize * 100 / updateTotalSize),
								false);
				contentView
						.setTextViewText(
								SparkBird.SparkBirdWayInfo.R.id.update_notification_progresstext,
								(int) (totalSize * 100 / updateTotalSize) + "%");
				updateNotification.contentView = contentView;

				updateNotificationManager.notify(0, updateNotification);
			}

			// 给文件赋权限
			// String cmd = "chmod +x " + saveFile.getPath();
			String cmd = "chmod 777 " + saveFile.getPath();
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
			if (is != null) {
				is.close();
			}

			if (fos != null) {
				fos.close();
			}
		}

		return totalSize;
	}
	
}
