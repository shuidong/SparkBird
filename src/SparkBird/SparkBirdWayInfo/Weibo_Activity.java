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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mobclick.android.MobclickAgent;

import weibo4android.Paging;
import weibo4android.Status;
import weibo4android.Weibo;
import weibo4android.WeiboException;

import SparkBird.Utility.CrashHandler;
import SparkBird.Utility.CustomViewBinder;
import SparkBird.Utility.WriteLogSendToServer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class Weibo_Activity extends Activity {

	static {
		Weibo.CONSUMER_KEY = "2939480918";
		Weibo.CONSUMER_SECRET = "85bfb53273df949270d1d9dc2cf5bb5f";
	}
	
	private SimpleAdapter adapterWeibo;
	private Handler messageHandler;
	private ListView mContactList;
	private ImageButton btnRefresh;
	private ImageButton btnReturn;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.weibo_wayinfo);

		btnReturn = (ImageButton) findViewById(R.id.imageButton1);
		btnReturn.setOnClickListener(new btnReturnClickListener());
		
		CrashHandler crashHandler = CrashHandler.getInstance();
		// 注册crashHandler
		crashHandler.init(this);

		// 刷新按钮
		btnRefresh = (ImageButton) findViewById(R.id.imageButton2);
		btnRefresh.setOnClickListener(new btnRefreshClickListener());
		
		mContactList = (ListView) findViewById(R.id.waystatus);
		
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

					if (processGetWeiboWayInfo() == true) {
						message.obj = "1";
					} else {
						message.obj = "0";
					}

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
				// 把处理的结果显示在ListView上
				adapterWeibo.setViewBinder(new CustomViewBinder());
				mContactList.setAdapter(adapterWeibo);
				mContactList.setOnItemClickListener(new onItemClickListener());
				Log.v("数据显示结果", "微博路况");
			} else if (msg.obj.toString().equals("0") == true) {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						Weibo_Activity.this);
				builder.setTitle("火鸟路况");
				builder.setIcon(R.drawable.sb004title);
				builder.setMessage("数据获取失败，");
				builder.setPositiveButton("确认",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
										
							}
						});
				builder.show();
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
	 * 刷新按钮事件
	 * 
	 */
	private final class btnRefreshClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			final ProgressDialog dialog = ProgressDialog.show(Weibo_Activity.this, null,
					"数据获取中，请稍等", false);
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

						if (processGetWeiboWayInfo() == true) {
							message.obj = "1";
						} else {
							message.obj = "0";
						}

						// 通过Handler发布携带有adapter的消息
						messageHandler.sendMessage(message);
						handler.sendEmptyMessage(0);
					}
				}.start();
			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(Weibo_Activity.this, e);
			}
		}
	}

	/**
	 * 从微博取得路况，生成listview使用的adapter
	 * 
	 */
	private boolean processGetWeiboWayInfo() {
		try {
			// 长时间处理的任务开始
			List<Map<String, Object>> dataWeibo = getDataWeibo();

			if (dataWeibo != null) {
				adapterWeibo = new SimpleAdapter(this, dataWeibo,
						R.layout.weibo_listitem, new String[] { "wayinfo",
								"wayinfoDetail", "listheader", "picDetail" },
						new int[] { R.id.wayinfo, R.id.wayinfoDetail,
								R.id.listheader, R.id.picDetail });

				return true;
			} else {
				return false;
			}

			// 长时间处理的任务结束
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
			return false;
		}
	}

	private List<Map<String, Object>> getDataWeibo() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret", Weibo.CONSUMER_SECRET);

		try {
			Map<String, Object> map = new HashMap<String, Object>();
			
			// 获取前20条关注用户的微博信息
			Weibo weibo = new Weibo();
			weibo.setToken(getString(R.string.token),
					getString(R.string.tokenSecret));
			Paging page = new Paging(1);
			List<Status> statuses = weibo.getFriendsTimeline(page);
			SimpleDateFormat formatter;
		    formatter = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss a");
		    
			for (Status status : statuses) {
				if (status.getRetweeted_status() != null) {
					if (status.getRetweeted_status().getText()
							.contains("此微博已被删除") == false) {
						map = new HashMap<String, Object>();
						map.put("wayinfo",
								trimLeft(status.getRetweeted_status().getText()
										.replace("@大连电台交通广播", "")));
						map.put("wayinfoDetail", formatter.format(status
								.getRetweeted_status().getCreatedAt()));
						if (status.getRetweeted_status().getThumbnail_pic() != "") {
							map.put("listheader", getBitmap(status
									.getRetweeted_status().getThumbnail_pic()));
						}
						else
						{
							map.put("listheader", R.drawable.listheader);
						}
						map.put("picDetail", status.getRetweeted_status().getOriginal_pic());
						list.add(map);
					}

				} else {
					if (status.getText().contains("的哥的姐晚上好") == false
							&& status.getText().contains("节目话题") == false
							&& status.getText().contains("此微博已被删除") == false) {
						map = new HashMap<String, Object>();
						map.put("wayinfo", trimLeft(status.getText()));
						map.put("wayinfoDetail", formatter.format(status.getCreatedAt()));
						if (status.getThumbnail_pic() != "") {
							map.put("listheader",
									getBitmap(status.getThumbnail_pic()));
						}
						else
						{
							map.put("listheader", R.drawable.listheader);
						}
						map.put("picDetail", status.getOriginal_pic());
						list.add(map);
					}
				}
			}
		} catch (WeiboException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}

		return list;
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
	
	/**
	 * 列表项目点击事件
	 * 
	 */
	private final class onItemClickListener implements
			AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			ListView listView = (ListView) parent;
			@SuppressWarnings("unchecked")
			HashMap<String, String> itemData = (HashMap<String, String>) listView
					.getItemAtPosition(position);
			String wayinfo = itemData.get("wayinfo");
			String wayinfoDetail = itemData.get("wayinfoDetail");
			String picDetail = itemData.get("picDetail");
			
			Intent intent = new Intent();
			intent.setClass(Weibo_Activity.this, WeiboDetail_Activity.class);
			Bundle bundle = new Bundle();
			bundle.putString("wayinfo", wayinfo);
			bundle.putString("wayinfoDetail", wayinfoDetail);
			bundle.putString("imageUrl", picDetail);
			intent.putExtras(bundle);
			startActivity(intent);
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

	/**
	 * 去掉String字符串左边的空格
	 * 
	 * @param src
	 *            要处理的String字符串
	 * @return 处理好的String字符串
	 */
	public static String trimLeft(final String src) {
		StringBuffer sb = new StringBuffer();
		int len = src.length();
		char c;
		int i = 0;
		for (i = 0; i < len; i++) {
			c = src.charAt(i);
			if (c != ' ' && c != '\t') {
				break;
			}
		}
		sb.append(src.substring(i, len));
		return sb.toString();
	}
}