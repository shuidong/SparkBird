/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import org.kobjects.base64.Base64;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WriteLogSendToServer {
	/** Debug Log tag */
	public static final String TAG = "CrashHandler";

	/**
	 * 是否开启日志输出,在Debug状态下开启, 在Release状态下关闭以提示程序性能
	 * */
	public static final boolean DEBUG = true;

	/** 使用Properties来保存设备的信息和错误堆栈信息 */
	private static Properties mDeviceCrashInfo = new Properties();
	private static final String VERSION_NAME = "versionName";
	private static final String VERSION_CODE = "versionCode";
	private static final String STACK_TRACE = "STACK_TRACE";
	/** 错误报告文件的扩展名 */
	private static final String CRASH_REPORTER_EXTENSION = ".cr";

	private static TelephonyManager mTelephonyMgr;

	/**
	 * 保存错误信息到文件中
	 * 
	 * @param ex
	 * @return
	 */
	public static void saveCrashInfoToFile(Context ctx, Throwable ex) {
		Writer info = new StringWriter();
		PrintWriter printWriter = new PrintWriter(info);
		ex.printStackTrace(printWriter);

		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}

		String result = info.toString();
		printWriter.close();

		mDeviceCrashInfo.put(STACK_TRACE, result);

		try {
			long timestamp = System.currentTimeMillis();
			final Calendar mCalendar = Calendar.getInstance();
			mCalendar.setTimeInMillis(timestamp);
			int mYear = mCalendar.get(Calendar.YEAR);
			int mMonth = mCalendar.get(Calendar.MONTH) + 1;
			int mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
			int mHour = mCalendar.get(Calendar.HOUR_OF_DAY);
			int mMinutes = mCalendar.get(Calendar.MINUTE);
			int mSecond = mCalendar.get(Calendar.SECOND);
			int mMillisecond = mCalendar.get(Calendar.MILLISECOND);
			String mTimeStamp = String.valueOf(mYear) + String.valueOf(mMonth)
					+ String.valueOf(mDay) + "_" + String.valueOf(mHour)
					+ String.valueOf(mMinutes) + String.valueOf(mSecond)
					+ String.valueOf(mMillisecond);
			mTelephonyMgr = (TelephonyManager) ctx
					.getSystemService(Context.TELEPHONY_SERVICE);

			String fileName = "crash-" + mTelephonyMgr.getDeviceId() + "-"
					+ mTimeStamp + CRASH_REPORTER_EXTENSION;

			// store方法无法识别转义字符，如\n\t，因此不适用此方法来生成log文件
			/*
			 * FileOutputStream trace = mContext.openFileOutput(fileName,
			 * Context.MODE_PRIVATE); mDeviceCrashInfo.store(trace, "");
			 * trace.flush(); trace.close();
			 */

			// 循环读取mDeviceCrashInfo，一行一行写入文本文件
			Enumeration<?> en = mDeviceCrashInfo.propertyNames();
			while (en.hasMoreElements()) {
				String key = (String) en.nextElement();
				String Property = mDeviceCrashInfo.getProperty(key);
				OutputStream outputStreamO = ctx.getApplicationContext()
						.openFileOutput(fileName, Context.MODE_APPEND);
				Log.e("error-info", Property);
				FileService.save(outputStreamO, key + "=" + Property + "\n");
				outputStreamO.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "an error occured while writing report file...", e);
		}
	}

	/**
	 * 收集程序崩溃的设备信息
	 * 
	 * @param ctx
	 */
	public static void collectCrashDeviceInfo(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
					PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				mDeviceCrashInfo.put(VERSION_NAME,
						pi.versionName == null ? "not set" : pi.versionName);
				mDeviceCrashInfo.put(VERSION_CODE,
						String.valueOf(pi.versionCode));
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Error while collect package info", e);
		}
		// 使用反射来收集设备信息.在Build类中包含各种设备信息,
		// 例如: 系统版本号,设备生产商 等帮助调试程序的有用信息
		// 具体信息请参考后面的截图
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				mDeviceCrashInfo.put(field.getName(), field.get(null)
						.toString());
				if (DEBUG) {
					Log.d(TAG, field.getName() + " : " + field.get(null));
				}
			} catch (Exception e) {
				Log.e(TAG, "Error while collect crash info", e);
			}

		}

	}

	/**
	 * 返回文件字节到字节数组,同时将数组序列化
	 * 
	 */
	public static String getBytesFromFile(Context ctx, File file)
			throws IOException {
		InputStream fin = ctx.getApplicationContext().openFileInput(
				file.getName()); // 获得FileInputStream对象
		int length = fin.available(); // 获取文件长度
		byte[] buffer = new byte[length]; // 创建byte数组用于读入数据
		fin.read(buffer); // 将文件内容读入到byte数组中
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(buffer);
		String uploadBuffer = new String(Base64.encode(baos.toByteArray()));
		fin.close();// 关闭文件输入流
		return uploadBuffer;

	}

	/**
	 * 在程序启动时候, 可以调用该函数来发送以前没有发送的报告
	 */
	public static void sendPreviousReportsToServer(Context ctx) {
		sendCrashReportsToServer(ctx);
	}

	/**
	 * 把错误报告发送给服务器,包含新产生的和以前没发送的.
	 * 
	 * @param ctx
	 */
	public static void sendCrashReportsToServer(Context ctx) {
		String[] crFiles = getCrashReportFiles(ctx);
		if (crFiles != null && crFiles.length > 0) {
			TreeSet<String> sortedFiles = new TreeSet<String>();
			sortedFiles.addAll(Arrays.asList(crFiles));

			for (String fileName : sortedFiles) {
				File cr = new File(ctx.getFilesDir(), fileName);
				if (HttpHelper.postReport(ctx, cr)) {
					cr.delete();// 删除已发送的报告
				}
			}
		}
	}

	/**
	 * 获取错误报告文件名
	 * 
	 * @param ctx
	 * @return
	 */
	private static String[] getCrashReportFiles(Context ctx) {
		File filesDir = ctx.getFilesDir();
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(CRASH_REPORTER_EXTENSION);
			}
		};
		return filesDir.list(filter);
	}

	/**
	 * 正常捕获异常时的log处理
	 * 
	 */
	public static void outputLog(Context ctx, Throwable ex) {
		try {
			// 收集设备信息
			collectCrashDeviceInfo(ctx);
			// 保存错误报告文件
			saveCrashInfoToFile(ctx, ex);
			// 发送错误报告到服务器
			sendCrashReportsToServer(ctx);
		} catch (Exception e) {
			Log.v("正常捕获异常时的log处理出错", e.toString());
		}
	}
}
