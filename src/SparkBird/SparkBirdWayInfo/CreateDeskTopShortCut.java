/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import SparkBird.Utility.WriteLogSendToServer;
import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;

public class CreateDeskTopShortCut extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			final Intent intent = getIntent();
			final String action = intent.getAction();
			if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
				createShortCut();
				finish();
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}

	void createShortCut() {
/*		Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
		shortcutIntent.setClass(this, Welcome_Activity.class);

		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
		Parcelable shortIcon = Intent.ShortcutIconResource.fromContext(this,
				R.drawable.sb004);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortIcon);
		setResult(RESULT_OK, intent);*/
		
		Intent shortcut = new Intent(
		"com.android.launcher.action.INSTALL_SHORTCUT");

		// 快捷方式的名称
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.app_name));
		// 不允许重复创建
		shortcut.putExtra("duplicate", false);

		// 指定快捷方式启动的Activity
		// 这里必须为Intent设置一个action，可以任意(但安装和卸载时该参数必须一致)
		String action = "SparkBird.SparkBirdWayInfo";
		Intent respondIntent = new Intent(this, Welcome_Activity.class);
		respondIntent.setAction(action);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, respondIntent);
		// 下面的方法与上面的效果是一样的,另一种构建形式而已
		// 注意: ComponentName的第二个参数必须加上点号(.)，否则快捷方式无法启动相应程序
		/*
		 * String appClass = this.getPackageName() + "." +
		 * this.getLocalClassName();
		 */
		/*
		 * String appClass = Welcome_Activity.class.getName();
		 * 
		 * ComponentName comp = new ComponentName(this.getPackageName(),
		 * appClass); shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new
		 * Intent(action).setComponent(comp));
		 */

		// 快捷方式的图标
		ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(
				this, R.drawable.sb004);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);

		sendBroadcast(shortcut);
	}
}
