/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import com.mobclick.android.MobclickAgent;

import SparkBird.Utility.WriteLogSendToServer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;

public class Help_Activity extends Activity {
	private ImageButton btnReturn;
	private Handler messageHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);

		btnReturn = (ImageButton) findViewById(R.id.imageButton2);
		btnReturn.setOnClickListener(new btnReturnClickListener());

		final ProgressDialog dialog = ProgressDialog.show(this, null,
				"数据载入中，请稍等", false);
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				dialog.dismiss();
			}
		};

		Looper looper = Looper.myLooper();
		messageHandler = new MessageHandler(looper);

		try {
			new Thread() {
				@Override
				public void run() {
					// 创建一个Message对象，并把当前显示的路况颜色赋值给Message对象
					Message message = Message.obtain();

					Bundle bundle = Help_Activity.this.getIntent().getExtras();
					if (bundle != null) {
						WebView webview = (WebView) findViewById(R.id.webView_help);
						WebSettings webSettings = webview.getSettings();
						webSettings.setSavePassword(false);
						webSettings.setSaveFormData(false);
						webSettings.setJavaScriptEnabled(true);
						webSettings.setSupportZoom(false);
						//webview.loadUrl("file:///android_asset/help.html");// 本地
						webview.loadUrl("http://www.dalian1008.com/client/help.html");// 远程

						//另一种方法
						//String htmldata = "";// 网页代码
						//String targeturl = "http://www.baidu.com";// 目标网址（具体）
						//String baseurl = "";// 连接目标网址失败进入的默认网址
						//webview.getSettings().setDefaultTextEncodingName("GB2312");
						//webview.loadData(htmldata, "text/html", "utf-8");
						//webview.loadDataWithBaseURL(targeturl, htmldata, "text/html", "utf-8", baseurl);
					}

					message.obj = "1";

					// 通过Handler发布携带有adapter的消息
					messageHandler.sendMessage(message);
					handler.sendEmptyMessage(0);
				}
			}.start();
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}

	}

	// 子类化一个Handler
	class MessageHandler extends Handler {
		public MessageHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj.toString().equals("1") == true) {

			}
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
	
	/**
	 * 返回按钮事件
	 * 
	 */
	private final class btnReturnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			finish();
		}
	}
}