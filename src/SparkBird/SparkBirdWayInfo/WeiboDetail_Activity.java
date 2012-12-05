/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.mobclick.android.MobclickAgent;

import SparkBird.Utility.MulitPointTouchListener;
import SparkBird.Utility.WriteLogSendToServer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class WeiboDetail_Activity extends Activity {
	private ImageView weiboDetail;
	private Bitmap picDetail;
	private Handler messageHandler;
	private ImageButton btnReturn;
	private TextView txwayinfo;
	private TextView txwayinfoDetail;
	
	private String imageUrl = null;
	private String wayinfo = null;
	private String wayinfoDetail = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.weibo_detail);
		
		btnReturn = (ImageButton) findViewById(R.id.imageButton2);
		btnReturn.setOnClickListener(new btnReturnClickListener());
		
		weiboDetail = (ImageView) findViewById(R.id.weibodetail);
		weiboDetail.setOnTouchListener(new MulitPointTouchListener());

		txwayinfo = (TextView) findViewById(R.id.wayinfo);
		txwayinfoDetail = (TextView) findViewById(R.id.wayinfoDetail);
		
		final ProgressDialog dialog = ProgressDialog.show(this, null,
				"数据载入中，请稍等", false);
		dialog.setCancelable(true);
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
					
					Bundle bundle = WeiboDetail_Activity.this.getIntent().getExtras();
					if (bundle != null) {
						imageUrl = bundle.getString("imageUrl");
						wayinfo = bundle.getString("wayinfo");
						wayinfoDetail = bundle.getString("wayinfoDetail");
					}
					picDetail=getBitmap(imageUrl);
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
				weiboDetail.setImageBitmap(picDetail);
				txwayinfo.setText(wayinfo);
				txwayinfoDetail.setText(wayinfoDetail);
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
	
	public Bitmap getBitmap(String imageUrl) {
		Bitmap mBitmap = null;
		try {
			URL url = new URL(imageUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
			mBitmap = BitmapFactory.decodeStream(is);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		} catch (IOException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}

		return mBitmap;
	}
}