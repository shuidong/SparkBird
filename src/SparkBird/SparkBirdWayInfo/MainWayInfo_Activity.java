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
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import SparkBird.Utility.CrashHandler;
import SparkBird.Utility.FileService;
import SparkBird.Utility.HttpHelper;
import SparkBird.Utility.Utility;
import SparkBird.Utility.UtilityConst;
import SparkBird.Utility.WriteLogSendToServer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobclick.android.MobclickAgent;
import com.shoushuo.android.tts.ITts;

public class MainWayInfo_Activity extends Activity {
	private TelephonyManager mTelephonyMgr;
	private SharedPreferences preferences;
	private CustomAdapter adapterRed;
	private CustomAdapter adapterYellow;
	private CustomAdapter adapterGreen;
	private CustomAdapter adapterFocus;
	private String[] aryWayInfoRed;
	private String[] aryWayInfoYellow;
	private String[] aryWayInfoGreen;
	private String[] aryWayInfoFocus;
	private String strWayInfoRed = "";
	private String strWayInfoYellow = "";
	private String strWayInfoGreen = "";
	private String[] aryWayStatusDetail;
	private String strFinalTime = "";
	private String[] strWayMaster;
	private Handler messageHandler;
	private Handler messageHandlerVoice;
	// 保存当前页面显示的路段颜色
	private String strWayInfoFlagForRefresh = "3";
	private boolean[] selected;
	private int auto_refresh_selected;
	private int auto_refresh_time = 0;
	private String strWayStatus;
	private ListView mContactList;
	private ImageButton btn1;
	private ImageButton btn2;
	private ImageButton btn3;
	private ImageButton btn4;
	private ImageButton btnRefresh;
	private ImageButton btnAdd;
	private ImageButton btnWeibo;
	private ImageButton btnVoice;

	private String Rid = null;
	private String ClientVersion = null;
	private String imei = null;
	private String WeiboBtnReportName;
	private int auto_refresh_index;

	private Thread refreshThread;

	private ITts ttsService;
	private boolean ttsBound;
	private ServiceConnection connection;
	private int itemID;
	
	private boolean stopFlag = false;
	//private View updateview;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wayinfo);

		CrashHandler crashHandler = CrashHandler.getInstance();
		// 注册crashHandler
		crashHandler.init(this);
		
		// client version
		ClientVersion = getVersionName();
		preferences = getSharedPreferences(UtilityConst.SHAREDPREFERENCES,
				MODE_PRIVATE);
		Rid = preferences.getString(UtilityConst.SP_KEY_RID,
				UtilityConst.NULL_VALUE);
		
		mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		imei = mTelephonyMgr.getDeviceId();

		createWeiboBtnReport();
		
		// 自动更新设定
		auto_refresh();
		 
		String Flag = null;
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			Flag = bundle.getString("Flag");
		}

		if (Flag.startsWith(UtilityConst.RESPONSE_NETWORK_NOAVILABLE)) {
			showDialog(UtilityConst.DIALOG_NETWORK_NOAVILABLE);
			stopFlag = true;
		}

		if (Flag.startsWith(UtilityConst.RESPONSE_SERVER_ERROR)) {
			showDialog(UtilityConst.DIALOG_SERVER_ERROR);
			stopFlag = true;
		}

		if (Flag.startsWith(UtilityConst.RESPONSE_NO_RID)) {
			showDialog(UtilityConst.DIALOG_NO_RID);
			stopFlag = true;
		}

		if (Flag.startsWith(UtilityConst.RESPONSE_OLD_CLIENT_VERSION)) {
			showDialog(UtilityConst.DIALOG_OLD_CLIENT_VERSION);
		}

		// 用户第一次使用时，询问用户是否创建快捷方式
		/*
		 * String shortCutFlag = null; Bundle bundle =
		 * MainWayInfo_Activity.this.getIntent().getExtras(); if (bundle !=
		 * null) { shortCutFlag = bundle.getString("shortCutFlag"); }
		 * 
		 * if (shortCutFlag.startsWith(UtilityConst.RESPONSE_NEW_USER)) {
		 * showDialog(UtilityConst.DIALOG_NEW_USER); }
		 */
		// 用户第一次使用时，不询问，直接创建快捷方式
		String shortCutFlag = null;
		Bundle bundle2 = this.getIntent().getExtras();
		if (bundle2 != null) {
			shortCutFlag = bundle.getString("shortCutFlag");
		}

		if (shortCutFlag.startsWith(UtilityConst.RESPONSE_NEW_USER)) {
			create_shortcut("0");
		}

		if (stopFlag == false) {
			final ProgressDialog dialog = ProgressDialog.show(this, null,
					"数据获取中，请稍等", false);
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

						if (processGetWayInfo("Dalian") == true) {
							saveWayInfo();
							message.obj = "3";
						} else {
							getOldWayInfo();
							message.obj = "0";
						}

						// 通过Handler发布携带有adapter的消息
						messageHandler.sendMessage(message);
						handler.sendEmptyMessage(0);
					}
				}.start();

				strWayInfoFlagForRefresh = "3";
			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(this, e);
			}

			mContactList = (ListView) findViewById(R.id.waystatus);

			// 刷新按钮
			btnRefresh = (ImageButton) findViewById(R.id.imageButton2);
			btnRefresh.setOnClickListener(new btnRefreshClickListener());
			// 添加关注按钮
			btnAdd = (ImageButton) findViewById(R.id.imageButton1);
			btnAdd.setOnClickListener(new btnAddClickListener());
			// 绿色畅通按钮
			btn3 = (ImageButton) findViewById(R.id.imageBtn3);
			btn3.setOnClickListener(new btn3ClickListener());
			// 黄色缓行按钮
			btn2 = (ImageButton) findViewById(R.id.imageBtn2);
			btn2.setOnClickListener(new btn2ClickListener());
			// 红色饱和按钮
			btn1 = (ImageButton) findViewById(R.id.imageBtn1);
			btn1.setOnClickListener(new btn1ClickListener());
			// 关注路段按钮
			btn4 = (ImageButton) findViewById(R.id.imageBtn4);
			btn4.setOnClickListener(new btn4ClickListener());

			// 语音播报按钮
			btnVoice = (ImageButton) findViewById(R.id.imageButtonVoice);
			btnVoice.setOnClickListener(new btnVoiceClickListener());
		}
		
		connection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				ttsService = ITts.Stub.asInterface(service);
				ttsBound = true;
				// 在应用第一个使用TTS的地方，调用下面的initialize方法，比如如果有
				// 两个Activity都使用手说TTS，则第二个Activity在此不需要再调用。
				try {
					ttsService.initialize();
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				ttsService = null;
				ttsBound = false;
			}
		};

		// weibo按钮
		btnWeibo = (ImageButton) findViewById(R.id.imageButtonMenu);
		btnWeibo.setOnClickListener(new btnWeiboClickListener());

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
	protected void onStart() {
		super.onStart();
		if (!ttsBound) {
			String actionName = "com.shoushuo.android.tts.intent.action.InvokeTts";
			Intent intent = new Intent(actionName);
			this.bindService(intent, connection, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onDestroy() {
		//if (ttsBound) {
			ttsBound = false;
			this.unbindService(connection);
		//}
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { // 按下的如果是BACK，同时没有重复
			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("火鸟路况");
			builder.setIcon(R.drawable.sb004title);
			builder.setMessage("确定要退出火鸟路况么？");
			builder.setNegativeButton("取消", null);
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (auto_refresh_index != UtilityConst.INT_99_VALUE) {
								handlerRefresh.removeCallbacks(runnableRefresh);
							}
							
							//提示用户等待语音播报结束
							final ProgressDialog dialogVoice = ProgressDialog.show(MainWayInfo_Activity.this, null,
									"语音播报停止中，请稍等", false);
							final Handler handlerVoice = new Handler() {
								public void handleMessage(Message msg) {
									dialogVoice.dismiss();
								}
							};

							Looper looperVoice = Looper.myLooper();
							messageHandlerVoice = new MessageHandlerVoice(looperVoice);

							try {
								new Thread() {
									@Override
									public void run() {
										// 创建一个Message对象，并把当前显示的路况颜色赋值给Message对象
										Message messageVoice = Message.obtain();

										if (ttsService != null) {
											try {
												do  {
													messageVoice.obj = "000";
												}while (ttsService.isSpeaking() == true);
													
												// 通过Handler发布携带有adapter的消息
												messageHandlerVoice.sendMessage(messageVoice);
												handlerVoice.sendEmptyMessage(0);
											} catch (RemoteException e) {
												e.printStackTrace();
												WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
											}
										}
										else
										{
											messageVoice.obj = "000";
											// 通过Handler发布携带有adapter的消息
											messageHandlerVoice.sendMessage(messageVoice);
											handlerVoice.sendEmptyMessage(0);
										}
									}
								}.start();
							} catch (Exception e) {
								e.printStackTrace();
								WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
							}
							
							//finish();// 结束Activity
						}
					});
			builder.show();

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
		case UtilityConst.MUTI_CHOICE_DIALOG:
			Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
					this, R.style.AlertDialogCustom));
			// Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("请选择您关注的道路");
			builder.setIcon(R.drawable.sb004title);

			

			DialogInterface.OnMultiChoiceClickListener mutiListener = new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int which,
						boolean isChecked) {
					selected[which] = isChecked;
				}
			};

			builder.setMultiChoiceItems(strWayMaster, selected, mutiListener);

			DialogInterface.OnClickListener btnOKListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int which) {
					String strOldWayId = "";
					OutputStream outputStreamO;

					if (ttsService != null) {
						try {
							if (ttsService.isSpeaking() == true) {
								ttsService.stop();
							}
						} catch (RemoteException e) {
							e.printStackTrace();
							WriteLogSendToServer.outputLog(
									MainWayInfo_Activity.this, e);
						}
					}

					try {
						for (int i = 0; i < selected.length; i++) {
							if (selected[i] == true) {
								strOldWayId = strOldWayId + i + ",";
							}
						}

						outputStreamO = MainWayInfo_Activity.this
								.getBaseContext()
								.openFileOutput("wayInfoFocus.sb",
										Context.MODE_PRIVATE);

						if (strOldWayId.equals("") == false) {
							FileService.save(
									outputStreamO,
									strOldWayId.substring(0, strOldWayId.length() - 1));
							outputStreamO.close();
						} else {
							FileService.save(outputStreamO, strOldWayId);
							outputStreamO.close();
						}

						if (strWayInfoFlagForRefresh.equals("4") == true) {
							if (strOldWayId.equals("") == true) {
								mContactList.setAdapter(null);
							} else {
								aryWayInfoFocus = strOldWayId.substring(0,
										strOldWayId.length() - 1).split(",");

								adapterFocus = new CustomAdapter(
										MainWayInfo_Activity.this,
										getDataFocus(), R.layout.listitem,
										new String[] { "wayinfo",
												"wayinfoDetail", "listheader1",
												"listheader2" }, new int[] {
												R.id.wayinfo,
												R.id.wayinfoDetail,
												R.id.listheader1,
												R.id.listheader2 }, true);

								mContactList.setAdapter(adapterFocus);
								mContactList.setOnItemClickListener(new onItemClickListener());
							}
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
					} catch (Exception e) {
						e.printStackTrace();
						WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
					}
				}
			};

			/*
			 * DialogInterface.OnClickListener btnCancleListener = new
			 * DialogInterface.OnClickListener() {
			 * 
			 * @Override public void onClick(DialogInterface dialogInterface,
			 * int which) { // 用于记录被选中的道路ID selected = new
			 * boolean[strWayMaster.length];
			 * 
			 * try { for (int i = 0; i < aryWayInfoFocus.length; i++) { Integer
			 * intIndex = Integer .parseInt(aryWayInfoFocus[i]) - 1;
			 * selected[intIndex] = true; } } catch (Exception e) {
			 * e.printStackTrace(); } } };
			 */

			builder.setPositiveButton("确定", btnOKListener);
			builder.setNegativeButton("取消", null);
			dialog = builder.create();
			break;
		case UtilityConst.DIALOG_NEW_USER:
			// 用户第一次使用时，询问是否创建桌面快捷方式。
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
					.setTitle(R.string.msg_welcome)
					.setIcon(R.drawable.sb004title)
					.setMessage(R.string.msg_shortcut)
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									create_shortcut("1");
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).create();
			
		case UtilityConst.DIALOG_NETWORK_NOAVILABLE:
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
					.setTitle(R.string.msg_title)
					.setIcon(R.drawable.sb004title)
					.setMessage(R.string.msg_network_error)
					.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog,
								int which) {
							startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS), 0);
						}
					}).create();

		case UtilityConst.DIALOG_SERVER_ERROR:
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
					.setTitle(R.string.msg_title)
					.setIcon(R.drawable.sb004title)
					.setMessage(R.string.msg_server_error)
					.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog,
								int which) {
							startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS), 0);
						}
					}).create();

		case UtilityConst.DIALOG_NO_RID:
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
					.setTitle(R.string.msg_title)
					.setIcon(R.drawable.sb004title)
					.setMessage(R.string.msg_init_error)
					.setPositiveButton(R.string.ok_btn, null).create();

		case UtilityConst.DIALOG_NO_SHOUSHUO_TTS:
			// 发现用户没有安装shoushuoTTS.apk，提示用户下载安装
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
					.setTitle(R.string.msg_title)
					.setIcon(R.drawable.sb004title)
					.setMessage(R.string.msg_no_shoushuotts)
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									// 开启更新服务UpdateService
									// 这里为了把update更好模块化，可以传一些updateService依赖的值
									// 如布局ID，资源ID，动态获取的标题,这里以app_name为例
									Intent updateIntent = new Intent(
											MainWayInfo_Activity.this,
											GetshoushuoTTSService.class);
									updateIntent.putExtra("titleId",
											R.string.shoushuotts);
									startService(updateIntent);

								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).create();

/*		case UtilityConst.DIALOG_OLD_CLIENT_VERSION:
			// 发现新版本，提示用户更新
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
					.setTitle(R.string.msg_title)
					.setIcon(R.drawable.sb004title)
					.setMessage(R.string.msg_old_client)
					.setPositiveButton("更新",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									// 开启更新服务UpdateService
									// 这里为了把update更好模块化，可以传一些updateService依赖的值
									// 如布局ID，资源ID，动态获取的标题,这里以app_name为例
									Intent updateIntent = new Intent(
											MainWayInfo_Activity.this,
											UpdateService.class);
									updateIntent.putExtra("titleId",
											R.string.app_name);
									startService(updateIntent);

								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).create();*/
		case UtilityConst.DIALOG_OLD_CLIENT_VERSION:
			//弹出式升级画面
			LayoutInflater factory = LayoutInflater.from(this);
			View updateview = factory.inflate(R.layout.alertdialogupdate, null);
			TextView update1 = (TextView) updateview.findViewById(R.id.update1);
			TextView update2 = (TextView) updateview.findViewById(R.id.update2);
			TextView update3 = (TextView) updateview.findViewById(R.id.update3);
			
			update1.setText(R.string.msg_old_client);
			update2.setText("下载地址：");
			update3.setText(Html
					.fromHtml("<a href=\"http://www.dalian1008.com/client/download.html\">http://www.dalian1008.com/client/download.html</a> "));
			update3.setMovementMethod(LinkMovementMethod.getInstance());
			
			Builder builderupdate = new AlertDialog.Builder(this);
			builderupdate.setView(updateview);
			builderupdate.setTitle(getResources().getString(R.string.msg_title));
			builderupdate.setIcon(R.drawable.sb004title);
			//builderupdate.setMessage(R.string.msg_old_client);
			builderupdate.setPositiveButton(R.string.ok_btn, null);
			dialog = builderupdate.create();
			break;
		case UtilityConst.DIALOG_NEW_CLIENT_VERSION:
			return new AlertDialog.Builder(MainWayInfo_Activity.this)
			.setTitle(R.string.msg_title)
			.setIcon(R.drawable.sb004title)
			.setMessage(R.string.msg_new_client)
			.setPositiveButton(R.string.ok_btn, null).create();
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) // 点击菜单按钮时执行，创建一个菜单
	{
		if (ttsService != null) {
			try {
				if (ttsService.isSpeaking() == true) {
					ttsService.stop();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
				WriteLogSendToServer
						.outputLog(MainWayInfo_Activity.this, e);
			}
		}
		
		// 通过menu.add()方法添加菜单项，add(groupId, itemId, order, title)方法的四个参数分别为
		// 菜单项所在组的id，菜单项的id，菜单项在这个组中排列的序号，菜单项显示的标题
		menu.add(0, 1, 1, "定时刷新").setIcon(R.drawable.menu_refreshtimer);
		menu.add(0, 2, 2, "检查更新").setIcon(R.drawable.menu_checkupdate);
		menu.add(0, 3, 3, "建议反馈").setIcon(R.drawable.menu_attr);
		menu.add(0, 4, 4, "关于火鸟").setIcon(R.drawable.menu_about);
		//menu.add(0, 5, 5, "使用帮助").setIcon(R.drawable.menu_help);
		menu.add(0, 5, 5, "地图路况").setIcon(R.drawable.menu_view_image);
		menu.add(0, 6, 6, "退出火鸟路况").setIcon(R.drawable.menu_quit);
		menu.add(0, 7, 7, "创建桌面快捷方式");
		//menu.add(0, 8, 8, "桌面显示设置");
		menu.add(0, 8, 8, "语音播报");
		//menu.add(0, 9, 9, "最新图文路情");
		menu.add(0, 9, 9, "使用帮助");
		menu.add(0, 10, 10, "图文路情");
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {// 当选择菜单项时执行这个方法		
		if (item.getItemId() == 5) {
			Intent intent = new Intent();
			intent.setClass(MainWayInfo_Activity.this, Traffic_Activity.class);
			Bundle bundle = new Bundle();
			intent.putExtras(bundle);
			startActivity(intent);
		}
		if (item.getItemId() == 10) {
			recordWeibotn("menu_btn");
			
			Intent intent = new Intent();
			intent.setClass(MainWayInfo_Activity.this, Weibo_Activity.class);
			Bundle bundle = new Bundle();
			intent.putExtras(bundle);
			startActivity(intent);
		}
		
		if (item.getItemId() == 9) {
			Intent intent = new Intent();
			intent.setClass(MainWayInfo_Activity.this, Help_Activity.class);
			Bundle bundle = new Bundle();
			intent.putExtras(bundle);
			startActivity(intent);
		}

/*		if (item.getItemId() == 8) {
			Intent intent = new Intent();
			intent.setClass(this, LukuangConfiguration.class);
			startActivity(intent);
		}*/
		
		if (item.getItemId() == 8) {
			try {
				// 语音播报
				if (ttsService == null) {
					// 提示用户安装shoushuoTTS.apk
					showDialog(UtilityConst.DIALOG_NO_SHOUSHUO_TTS);
				}

				if (ttsService != null) {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					} else {
						if ("1".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报红色饱和的路段", 1);
							if (aryWayInfoRed != null) {
								for (int i = 0; i < aryWayInfoRed.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoRed[i])];
									ttsService.speak(strtmp, 1);
								}
							}
						} else if ("2".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报黄色缓行的路段", 1);
							if (aryWayInfoYellow != null) {
								for (int i = 0; i < aryWayInfoYellow.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoYellow[i])];
									ttsService.speak(strtmp, 1);
								}
							}
						} else if ("3".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报绿色畅通的路段", 1);
							if (aryWayInfoGreen != null) {
								for (int i = 0; i < aryWayInfoGreen.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoGreen[i])];
									ttsService.speak(strtmp, 1);
								}
							}
						} else if ("4".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报您关注的路段", 1);
							if (aryWayInfoFocus != null) {
								for (int i = 0; i < aryWayInfoFocus.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoFocus[i]) - 1];
									ttsService.speak(strtmp, 1);

									if (wayInfoOld_getColor(aryWayInfoFocus[i])
											.equals("1") == true) {
										ttsService.speak("该路段目前为红色饱和状态", 1);
									} else if (wayInfoOld_getColor(
											aryWayInfoFocus[i]).equals("2") == true) {
										ttsService.speak("该路段目前为黄色缓行状态", 1);
									} else {
										ttsService.speak("该路段目前为绿色畅通状态", 1);
									}
								}
							}
						}
					}
				}

			} catch (RemoteException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			}
		}

		if (item.getItemId() == 7) {
			// 创建桌面快捷方式
			create_shortcut("1");
		}

		if (item.getItemId() == 3) {
			LayoutInflater factory = LayoutInflater
					.from(MainWayInfo_Activity.this);
			final View suggestionview = factory.inflate(R.layout.suggestion,
					null);
			final EditText suggestion = (EditText) suggestionview
					.findViewById(R.id.suggestion);
			final EditText contact = (EditText) suggestionview
					.findViewById(R.id.contact);
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					MainWayInfo_Activity.this);
			dialog.setTitle("发送反馈");
			dialog.setIcon(R.drawable.sb004title);
			dialog.setPositiveButton("发送",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String strSuggestion = suggestion.getText()
									.toString();
							String strContact = contact.getText().toString();
							//判断内容是否为空
							if (strSuggestion.trim().equals("") == true
									&& strContact.trim().equals("") == true) {
								Toast.makeText(MainWayInfo_Activity.this.getBaseContext(),
										"您还没有填写反馈内容", Toast.LENGTH_LONG).show();
								return;
							}
							if (HttpHelper.SendSuggestion(strSuggestion,
									strContact) == true) {
								AlertDialog.Builder d = new AlertDialog.Builder(
										MainWayInfo_Activity.this);
								d.setMessage("发送成功，非常感谢您的反馈。");
								d.setPositiveButton("确定", null);
								d.show();
							} else {
								AlertDialog.Builder d = new AlertDialog.Builder(
										MainWayInfo_Activity.this);
								d.setMessage("发送失败，请检查您的网络连接。");
								d.setPositiveButton("确定", null);
								d.show();
							}
						}
					});
			dialog.setNegativeButton("取消", null);
			dialog.setView(suggestionview);
			dialog.show();
		}

		if (item.getItemId() == 2) {
			final ProgressDialog dialog = ProgressDialog.show(this, null,
					"正在检查是否有新版本", false);
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
						
						// 判断是否有新版本，如果有返回88
						String result;
						if (Rid.equals(UtilityConst.NULL_VALUE)) {
							result = register();
						} else {
							result = checkRegisterInfo();
						}
						
						if (result.startsWith(UtilityConst.RESPONSE_OLD_CLIENT_VERSION)) {
							message.obj = "88";
						}
						else
						{
							message.obj = "088";
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
		// 如果选择的菜单项是“退出”
		if (item.getItemId() == 6) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("火鸟路况");
			builder.setIcon(R.drawable.sb004title);
			builder.setMessage("确定要退出火鸟路况么？");
			builder.setNegativeButton("取消", null);
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//提示用户等待语音播报结束
							final ProgressDialog dialogVoice = ProgressDialog.show(MainWayInfo_Activity.this, null,
									"语音播报停止中，请稍等", false);
							final Handler handlerVoice = new Handler() {
								public void handleMessage(Message msg) {
									dialogVoice.dismiss();
								}
							};

							Looper looperVoice = Looper.myLooper();
							messageHandlerVoice = new MessageHandlerVoice(looperVoice);

							try {
								new Thread() {
									@Override
									public void run() {
										// 创建一个Message对象，并把当前显示的路况颜色赋值给Message对象
										Message messageVoice = Message.obtain();

										if (ttsService != null) {
											try {
												do  {
													messageVoice.obj = "000";
												}while (ttsService.isSpeaking() == true);
													
												// 通过Handler发布携带有adapter的消息
												messageHandlerVoice.sendMessage(messageVoice);
												handlerVoice.sendEmptyMessage(0);
											} catch (RemoteException e) {
												e.printStackTrace();
												WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
											}
										}
										else
										{
											messageVoice.obj = "000";
											// 通过Handler发布携带有adapter的消息
											messageHandlerVoice.sendMessage(messageVoice);
											handlerVoice.sendEmptyMessage(0);
										}
									}
								}.start();
							} catch (Exception e) {
								e.printStackTrace();
								WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
							}
							dialog.dismiss();
							//finish();// 结束Activity
						}
					});
			builder.show();
		}
		if (item.getItemId() == 4) {
			//弹出式关于画面，废止
			/*LayoutInflater factory = LayoutInflater
					.from(MainWayInfo_Activity.this);
			final View aboutview = factory.inflate(R.layout.alertdialogabout,
					null);
			TextView about1 = (TextView) aboutview.findViewById(R.id.about1);
			TextView about2 = (TextView) aboutview.findViewById(R.id.about2);
			TextView about3 = (TextView) aboutview.findViewById(R.id.about3);
			TextView about4 = (TextView) aboutview.findViewById(R.id.about4);
			TextView about5 = (TextView) aboutview.findViewById(R.id.about5);
			about1.setText("实时路况查询软件，火鸟科技出品。");
			about2.setText("版本号：" + getVersionName());
			about3.setText("作者: Bill Wei, Michael Zhang, Cage Liang");
			about4.setText("  ");
			about5.setText("Copyright 2010-2011 火鸟科技 Corporation, All Rights Reserved");
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					MainWayInfo_Activity.this);
			dialog.setTitle("火鸟路况");
			dialog.setIcon(R.drawable.sb004title);
			dialog.setNegativeButton("确定", null);
			dialog.setView(aboutview);
			dialog.show();*/
			
			Intent intent = new Intent();
			intent.setClass(MainWayInfo_Activity.this, About_Activity.class);
			Bundle bundle = new Bundle();
			intent.putExtras(bundle);
			startActivity(intent);
		}
		if (item.getItemId() == 1) {
			Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
					this, R.style.AlertDialogCustom));
			builder.setTitle("设定自动更新时间间隔");
			builder.setIcon(R.drawable.sb004title);

			final String[] array_auto_refresh = getResources().getStringArray(
					R.array.autorefresh_dialog_items);

			// 从未设定过自动更新时，默认选中手动更新
			if (auto_refresh_index == UtilityConst.INT_99_VALUE) {
				auto_refresh_index = 5;
			}

			builder.setSingleChoiceItems(array_auto_refresh,
					auto_refresh_index, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							auto_refresh_selected = whichButton;
						}
					});
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							SharedPreferences.Editor editor = preferences
									.edit();
							editor.putInt(UtilityConst.SP_KEY_AT_REFRESH,
									auto_refresh_selected);
							editor.commit();

							// 自动更新设定
							auto_refresh();
						}
					});
			builder.setNegativeButton("取消", null);
			builder.show();
		}

		return super.onOptionsItemSelected(item);
	}

	// 子类化一个Handler
	class MessageHandler extends Handler {
		public MessageHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			TextView txtTime = (TextView) findViewById(R.id.time);
			
			if (msg.obj.toString().equals("1") == true) {
				// 把处理的结果显示在ListView上
				mContactList.setAdapter(adapterRed);
				Log.v("数据显示结果", "红色");
			} else if (msg.obj.toString().equals("2") == true) {
				// 把处理的结果显示在ListView上
				mContactList.setAdapter(adapterYellow);
				Log.v("数据显示结果", "黄色");
			} else if (msg.obj.toString().equals("3") == true) {
				// 把处理的结果显示在ListView上
				mContactList.setAdapter(adapterGreen);
				Log.v("数据显示结果", "绿色");
			} else if (msg.obj.toString().equals("4") == true) {
				InputStream inputStreamO;
				try {
					inputStreamO = MainWayInfo_Activity.this.getBaseContext()
							.openFileInput("wayInfoFocus.sb");
					String strWayInfoFocus = FileService.read(inputStreamO);
					inputStreamO.close();

					Log.v("已添加关注的路段", strWayInfoFocus);

					if (strWayInfoFocus.length() != 0) {
						aryWayInfoFocus = strWayInfoFocus.split(",");
					}

					adapterFocus = new CustomAdapter(MainWayInfo_Activity.this,
							getDataFocus(), R.layout.listitem, new String[] {
									"wayinfo", "wayinfoDetail", "listheader1",
									"listheader2" }, new int[] { R.id.wayinfo,
									R.id.wayinfoDetail, R.id.listheader1,
									R.id.listheader2 }, true);

					mContactList.setAdapter(adapterFocus);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
					mContactList.setAdapter(null);
				} catch (Exception e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
				Log.v("数据显示结果", "关注的路段");
			} else if (msg.obj.toString().equals("0") == true) {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainWayInfo_Activity.this);
				builder.setTitle("火鸟路况");
				builder.setIcon(R.drawable.sb004title);
				builder.setMessage("数据获取失败，将显示上一次成功获取的数据");
				// a.setNegativeButton("确认", null);
				builder.setPositiveButton("确认",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mContactList.setAdapter(adapterGreen);
								Log.v("数据显示结果", "旧数据");
							}
						});
				builder.show();
			} else if (msg.obj.toString().equals("00") == true) {
				// 数据刷新失败
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainWayInfo_Activity.this);
				builder.setTitle("火鸟路况");
				builder.setIcon(R.drawable.sb004title);
				builder.setMessage("数据刷新失败，将显示上一次成功获取的数据");
				builder.setNegativeButton("确认", null);
				builder.show();
			} else if (msg.obj.toString().equals("88") == true) {
				// 发现新版本，提示用户更新
				showDialog(UtilityConst.DIALOG_OLD_CLIENT_VERSION);
			}
			else if (msg.obj.toString().equals("088") == true) {
				// 发现新版本，提示用户更新
				showDialog(UtilityConst.DIALOG_NEW_CLIENT_VERSION);
			}			
			
			if (strFinalTime.equals("") == false) {
				txtTime.setText(strFinalTime);
			}
			
			mContactList.setOnItemClickListener(new onItemClickListener());
		}
	}
	
	// 子类化一个Handler用于处理程序退出时停止语音播报
	class MessageHandlerVoice extends Handler {
		public MessageHandlerVoice(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj.toString().equals("000") == true) {
				// 退出系统
				finish();
			}
		}
	}
	
	/**
	 * 刷新按钮事件
	 * 
	 */
	private final class btnRefreshClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}

			final ProgressDialog dialog = ProgressDialog.show(
					MainWayInfo_Activity.this, null, "数据处理中，请稍等。", false);
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

						if (processGetWayInfo("Dalian") == true) {
							saveWayInfo();
							if ("1".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "1";
							} else if ("2".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "2";
							} else if ("3".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "3";
							} else if ("4".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "4";
							}
						} else {
							getOldWayInfo();
							message.obj = "00";
						}

						// 通过Handler发布携带有adapter的消息
						messageHandler.sendMessage(message);
						handler.sendEmptyMessage(0);
					}
				}.start();

			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			}
		}
	}

	/**
	 * 添加关注按钮事件
	 * 
	 */
	private final class btnAddClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Log.v("数据显示结果", "选择关注的道路");

			// ------------------
			// 取出本地都保存了哪些关注的路段
			// 用于记录被选中的道路ID
			selected = new boolean[strWayMaster.length];
			String strWayInfoFocus = "";
			InputStream inputStreamO;
			try {
				File filesDir = MainWayInfo_Activity.this.getFilesDir();
				File wayInfoFocus = new File(filesDir + "/wayInfoFocus.sb");

				if (wayInfoFocus.exists()) {
					inputStreamO = MainWayInfo_Activity.this.getBaseContext()
							.openFileInput("wayInfoFocus.sb");

					strWayInfoFocus = FileService.read(inputStreamO);
					inputStreamO.close();
				}

				if (strWayInfoFocus.length() != 0) {
					aryWayInfoFocus = strWayInfoFocus.split(",");

					for (int i = 0; i < aryWayInfoFocus.length; i++) {
						Integer intIndex = Integer.parseInt(aryWayInfoFocus[i]);
						selected[intIndex] = true;
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			}
			// ------------------
				
			Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
					MainWayInfo_Activity.this, R.style.AlertDialogCustom));
			// Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("请选择您关注的道路");
			builder.setIcon(R.drawable.sb004title);

			

			DialogInterface.OnMultiChoiceClickListener mutiListener = new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int which,
						boolean isChecked) {
					selected[which] = isChecked;
				}
			};

			builder.setMultiChoiceItems(strWayMaster, selected, mutiListener);

			DialogInterface.OnClickListener btnOKListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int which) {				
					String strOldWayId = "";
					OutputStream outputStreamO;

					if (ttsService != null) {
						try {
							if (ttsService.isSpeaking() == true) {
								ttsService.stop();
							}
						} catch (RemoteException e) {
							e.printStackTrace();
							WriteLogSendToServer.outputLog(
									MainWayInfo_Activity.this, e);
						}
					}

					try {
						for (int i = 0; i < selected.length; i++) {
							if (selected[i] == true) {
								strOldWayId = strOldWayId + i + ",";
							}
						}

						outputStreamO = MainWayInfo_Activity.this
								.getBaseContext()
								.openFileOutput("wayInfoFocus.sb",
										Context.MODE_PRIVATE);

						if (strOldWayId.equals("") == false) {
							FileService.save(
									outputStreamO,
									strOldWayId.substring(0, strOldWayId.length() - 1));
							outputStreamO.close();
						} else {
							FileService.save(outputStreamO, strOldWayId);
							outputStreamO.close();
						}

						if (strOldWayId.equals("") == true) {
							aryWayInfoFocus = null;
						} else {
							aryWayInfoFocus = strOldWayId.substring(0, strOldWayId.length() - 1).split(",");
						}
						if (strWayInfoFlagForRefresh.equals("1") == true) {
							adapterRed = new CustomAdapter(
									MainWayInfo_Activity.this,
									getDataRed(),
									R.layout.listitem,
									new String[] { "wayinfo",
											"wayinfoDetail",
											"listheader1",
											"listheader2" },
									new int[] { R.id.wayinfo,
											R.id.wayinfoDetail,
											R.id.listheader1,
											R.id.listheader2 }, false);

							mContactList.setAdapter(adapterRed);
						} else if (strWayInfoFlagForRefresh.equals("2") == true) {
							adapterYellow = new CustomAdapter(
									MainWayInfo_Activity.this,
									getDataYellow(),
									R.layout.listitem,
									new String[] { "wayinfo",
											"wayinfoDetail",
											"listheader1",
											"listheader2" },
									new int[] { R.id.wayinfo,
											R.id.wayinfoDetail,
											R.id.listheader1,
											R.id.listheader2 }, false);

							mContactList.setAdapter(adapterYellow);
						} else if (strWayInfoFlagForRefresh.equals("3") == true) {
							adapterGreen = new CustomAdapter(
									MainWayInfo_Activity.this,
									getDataGreen(),
									R.layout.listitem,
									new String[] { "wayinfo",
											"wayinfoDetail",
											"listheader1",
											"listheader2" },
									new int[] { R.id.wayinfo,
											R.id.wayinfoDetail,
											R.id.listheader1,
											R.id.listheader2 }, false);

							mContactList.setAdapter(adapterGreen);
						} else if (strWayInfoFlagForRefresh.equals("4") == true) {
							adapterFocus = new CustomAdapter(
									MainWayInfo_Activity.this,
									getDataFocus(),
									R.layout.listitem,
									new String[] { "wayinfo",
											"wayinfoDetail",
											"listheader1",
											"listheader2" },
									new int[] { R.id.wayinfo,
											R.id.wayinfoDetail,
											R.id.listheader1,
											R.id.listheader2 }, true);

							mContactList.setAdapter(adapterFocus);
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
					} catch (Exception e) {
						e.printStackTrace();
						WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
					}
				}
			};

			/*
			 * DialogInterface.OnClickListener btnCancleListener = new
			 * DialogInterface.OnClickListener() {
			 * 
			 * @Override public void onClick(DialogInterface dialogInterface,
			 * int which) { // 用于记录被选中的道路ID selected = new
			 * boolean[strWayMaster.length];
			 * 
			 * try { for (int i = 0; i < aryWayInfoFocus.length; i++) { Integer
			 * intIndex = Integer .parseInt(aryWayInfoFocus[i]) - 1;
			 * selected[intIndex] = true; } } catch (Exception e) {
			 * e.printStackTrace(); } } };
			 */

			builder.setPositiveButton("确定", btnOKListener);
			builder.setNegativeButton("取消", null);
			builder.show();

			
			//showDialog(UtilityConst.MUTI_CHOICE_DIALOG);
		}
	}

	/**
	 * 红色饱和路段按钮事件
	 * 
	 */
	private final class btn1ClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			strWayInfoFlagForRefresh = "1";
			
			btn3.setBackgroundResource(R.drawable.green);
			btn1.setBackgroundResource(R.drawable.bcrowed);
			btn2.setBackgroundResource(R.drawable.slow);
			btn4.setBackgroundResource(R.drawable.focus);

			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}

			adapterRed = new CustomAdapter(
					MainWayInfo_Activity.this,
					getDataRed(),
					R.layout.listitem,
					new String[] { "wayinfo",
							"wayinfoDetail",
							"listheader1",
							"listheader2" },
					new int[] { R.id.wayinfo,
							R.id.wayinfoDetail,
							R.id.listheader1,
							R.id.listheader2 }, false);
			
			mContactList.setAdapter(adapterRed);
			mContactList.setOnItemClickListener(new onItemClickListener());
			Log.v("数据显示结果", "红色");
		}
	}

	/**
	 * 黄色缓行路段按钮事件
	 * 
	 */
	private final class btn2ClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			strWayInfoFlagForRefresh = "2";
			
			// ListView切换的动画效果
			/*
			 * AnimationSet set = new AnimationSet(true); Animation animation =
			 * new AlphaAnimation(0.0f, 1.0f); animation.setDuration(50);
			 * set.addAnimation(animation); animation = new
			 * TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
			 * Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
			 * -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
			 * animation.setDuration(100); set.addAnimation(animation);
			 * LayoutAnimationController controller = new
			 * LayoutAnimationController( set, 0.5f);
			 * mContactList.setLayoutAnimation(controller);
			 */
			
			btn3.setBackgroundResource(R.drawable.green);
			btn1.setBackgroundResource(R.drawable.crowed);
			btn2.setBackgroundResource(R.drawable.bslow);
			btn4.setBackgroundResource(R.drawable.focus);

			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}

			adapterYellow = new CustomAdapter(
					MainWayInfo_Activity.this,
					getDataYellow(),
					R.layout.listitem,
					new String[] { "wayinfo",
							"wayinfoDetail",
							"listheader1",
							"listheader2" },
					new int[] { R.id.wayinfo,
							R.id.wayinfoDetail,
							R.id.listheader1,
							R.id.listheader2 }, false);
			mContactList.setAdapter(adapterYellow);
			mContactList.setOnItemClickListener(new onItemClickListener());
			Log.v("数据显示结果", "黄色");
		}
	}

	/**
	 * 绿色畅通路段按钮事件
	 * 
	 */
	private final class btn3ClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			strWayInfoFlagForRefresh = "3";
			
			btn3.setBackgroundResource(R.drawable.bgreen);
			btn1.setBackgroundResource(R.drawable.crowed);
			btn2.setBackgroundResource(R.drawable.slow);
			btn4.setBackgroundResource(R.drawable.focus);

			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}

			adapterGreen = new CustomAdapter(
					MainWayInfo_Activity.this,
					getDataGreen(),
					R.layout.listitem,
					new String[] { "wayinfo",
							"wayinfoDetail",
							"listheader1",
							"listheader2" },
					new int[] { R.id.wayinfo,
							R.id.wayinfoDetail,
							R.id.listheader1,
							R.id.listheader2 }, false);
			mContactList.setAdapter(adapterGreen);
			mContactList.setOnItemClickListener(new onItemClickListener());
			Log.v("数据显示结果", "绿色");
		}
	}

	/**
	 * 关注路段按钮事件
	 * 
	 */
	private final class btn4ClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			strWayInfoFlagForRefresh = "4";
			
			btn3.setBackgroundResource(R.drawable.green);
			btn1.setBackgroundResource(R.drawable.crowed);
			btn2.setBackgroundResource(R.drawable.slow);
			btn4.setBackgroundResource(R.drawable.bfocus);

			String strWayInfoFocus = "";
			InputStream inputStreamO;

			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}

			try {
				File filesDir = MainWayInfo_Activity.this.getFilesDir();
				File wayInfoFocus = new File(filesDir + "/wayInfoFocus.sb");

				if (wayInfoFocus.exists()) {
					inputStreamO = MainWayInfo_Activity.this.getBaseContext()
							.openFileInput("wayInfoFocus.sb");
					strWayInfoFocus = FileService.read(inputStreamO);
					inputStreamO.close();
				}

				Log.v("已添加关注的路段", strWayInfoFocus);

				if (strWayInfoFocus.length() != 0) {
					aryWayInfoFocus = strWayInfoFocus.split(",");
				}

				adapterFocus = new CustomAdapter(MainWayInfo_Activity.this,
						getDataFocus(), R.layout.listitem, new String[] {
								"wayinfo", "wayinfoDetail", "listheader1",
								"listheader2" }, new int[] { R.id.wayinfo,
								R.id.wayinfoDetail, R.id.listheader1,
								R.id.listheader2 }, true);
				mContactList.setAdapter(adapterFocus);
				mContactList.setOnItemClickListener(new onItemClickListener());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
				mContactList.setAdapter(null);
			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			}
		}
	}

	/**
	 * weibo按钮事件
	 * 
	 */
	private final class btnWeiboClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			new Thread() {
				@Override
				public void run() {
					/*Instrumentation inst = new Instrumentation();
					inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);*/
					
					if (ttsService != null) {
						try {
							if (ttsService.isSpeaking() == true) {
								ttsService.stop();
							}
						} catch (RemoteException e) {
							e.printStackTrace();
							WriteLogSendToServer
									.outputLog(MainWayInfo_Activity.this, e);
						}
					}
					
					// 在本地生成报告，记录用户点击微博按钮的点击率
					recordWeibotn("top_btn");
					Intent intent = new Intent();
					intent.setClass(MainWayInfo_Activity.this, Weibo_Activity.class);
					Bundle bundle = new Bundle();
					intent.putExtras(bundle);
					startActivity(intent);
				}
			}.start();
		}
	}

	/**
	 * 语音播报按钮事件
	 * 
	 */
	private final class btnVoiceClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			try {
				// 语音播报
				if (ttsService == null) {
					// 提示用户安装shoushuoTTS.apk
					showDialog(UtilityConst.DIALOG_NO_SHOUSHUO_TTS);
				}

				if (ttsService != null) {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					} else {
						if ("1".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报红色饱和的路段", 1);
							if (aryWayInfoRed != null) {
								for (int i = 0; i < aryWayInfoRed.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoRed[i])];
									ttsService.speak(strtmp, 1);
								}
							}
						} else if ("2".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报黄色缓行的路段", 1);
							if (aryWayInfoYellow != null) {
								for (int i = 0; i < aryWayInfoYellow.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoYellow[i])];
									ttsService.speak(strtmp, 1);
								}
							}
						} else if ("3".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报绿色畅通的路段", 1);
							if (aryWayInfoGreen != null) {
								for (int i = 0; i < aryWayInfoGreen.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoGreen[i])];
									ttsService.speak(strtmp, 1);
								}
							}
						} else if ("4".equals(strWayInfoFlagForRefresh) == true) {
							ttsService.speak("以下播报您关注的路段", 1);
							if (aryWayInfoFocus != null) {
								for (int i = 0; i < aryWayInfoFocus.length; i++) {
									String strtmp = strWayMaster[Integer
											.parseInt(aryWayInfoFocus[i])];
									ttsService.speak(strtmp, 1);

									if (wayInfoOld_getColor(aryWayInfoFocus[i])
											.equals("1") == true) {
										ttsService.speak("该路段目前为红色饱和状态", 1);
									} else if (wayInfoOld_getColor(
											aryWayInfoFocus[i]).equals("2") == true) {
										ttsService.speak("该路段目前为黄色缓行状态", 1);
									} else {
										ttsService.speak("该路段目前为绿色畅通状态", 1);
									}
								}
							}
						}
					}
				}

			} catch (RemoteException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			}
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
			String listheader2 = itemData.get("listheader2");
			itemID = (int)id;

			if (ttsService != null) {
				try {
					if (ttsService.isSpeaking() == true) {
						ttsService.stop();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					WriteLogSendToServer
							.outputLog(MainWayInfo_Activity.this, e);
				}
			}
			
			if (listheader2 == null) {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainWayInfo_Activity.this);
				builder.setTitle("添加关注");
				builder.setIcon(R.drawable.sb004title);
				builder.setMessage("确定要将此路段添加到关注么？" + "\n" + wayinfo);
				builder.setNegativeButton("取消", null);
				builder.setPositiveButton("确定",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String strWayInfoFocus = "";
								InputStream inputStreamO;
								try {
									File filesDir = MainWayInfo_Activity.this
											.getFilesDir();
									File wayInfoFocus = new File(filesDir
											+ "/wayInfoFocus.sb");

									if (wayInfoFocus.exists()) {
										inputStreamO = MainWayInfo_Activity.this
												.getBaseContext()
												.openFileInput(
														"wayInfoFocus.sb");

										strWayInfoFocus = FileService
												.read(inputStreamO);
										inputStreamO.close();
									}

								} catch (FileNotFoundException e) {
									e.printStackTrace();
									WriteLogSendToServer.outputLog(
											MainWayInfo_Activity.this, e);
								} catch (Exception e) {
									e.printStackTrace();
									WriteLogSendToServer.outputLog(
											MainWayInfo_Activity.this, e);
								}

								String strOldWayId = "";
								OutputStream outputStreamO;
								try {
									if (strWayInfoFlagForRefresh.equals("1") == true) {
										if (strWayInfoFocus.equals("") == true) {
											strOldWayId = aryWayInfoRed[itemID] + ",";
										} else {
											strOldWayId = strWayInfoFocus + ","
													+ (aryWayInfoRed[itemID]) + ",";
										}
									} else if (strWayInfoFlagForRefresh.equals("2") == true) {
										if (strWayInfoFocus.equals("") == true) {
											strOldWayId = aryWayInfoYellow[itemID] + ",";
										} else {
											strOldWayId = strWayInfoFocus + ","
													+ (aryWayInfoYellow[itemID]) + ",";
										}
									} else if (strWayInfoFlagForRefresh.equals("3") == true) {
										if (strWayInfoFocus.equals("") == true) {
											strOldWayId = aryWayInfoGreen[itemID] + ",";
										} else {
											strOldWayId = strWayInfoFocus + ","
													+ (aryWayInfoGreen[itemID]) + ",";
										}
									}

									outputStreamO = MainWayInfo_Activity.this
											.getBaseContext().openFileOutput(
													"wayInfoFocus.sb",
													Context.MODE_PRIVATE);

									if (strOldWayId.equals("") == false) {
										aryWayInfoFocus = strOldWayId.split(",");
										
										FileService.save(outputStreamO, strOldWayId.substring(0, strOldWayId.length() - 1));
										outputStreamO.close();
									} else {
										FileService.save(outputStreamO,
												strOldWayId);
										outputStreamO.close();
									}

									if (strWayInfoFlagForRefresh.equals("1") == true) {
										adapterRed = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataRed(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, false);

										mContactList.setAdapter(adapterRed);
									} else if (strWayInfoFlagForRefresh.equals("2") == true) {
										adapterYellow = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataYellow(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, false);

										mContactList.setAdapter(adapterYellow);
									} else if (strWayInfoFlagForRefresh.equals("3") == true) {
										adapterGreen = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataGreen(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, false);

										mContactList.setAdapter(adapterGreen);
									} else if (strWayInfoFlagForRefresh.equals("4") == true) {
										adapterFocus = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataFocus(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, true);

										mContactList.setAdapter(adapterFocus);
									}
								} catch (FileNotFoundException e) {
									e.printStackTrace();
									WriteLogSendToServer.outputLog(
											MainWayInfo_Activity.this, e);
								} catch (Exception e) {
									e.printStackTrace();
									WriteLogSendToServer.outputLog(
											MainWayInfo_Activity.this, e);
								}

							}
						});
				builder.show();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainWayInfo_Activity.this);
				builder.setTitle("取消关注");
				builder.setIcon(R.drawable.sb004title);
				builder.setMessage("确定要将此路段取消关注么？" + "\n" + wayinfo);
				builder.setNegativeButton("返回", null);
				builder.setPositiveButton("确定",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String strWayInfoFocus = "";
								InputStream inputStreamO;
								try {
									File filesDir = MainWayInfo_Activity.this.getFilesDir();
									File wayInfoFocus = new File(filesDir + "/wayInfoFocus.sb");

									if (wayInfoFocus.exists()) {
										inputStreamO = MainWayInfo_Activity.this
												.getBaseContext().openFileInput(
														"wayInfoFocus.sb");

										strWayInfoFocus = FileService.read(inputStreamO);
										inputStreamO.close();
									}

									if (strWayInfoFocus.length() != 0) {
										aryWayInfoFocus = strWayInfoFocus.split(",");
									}
								} catch (FileNotFoundException e) {
									e.printStackTrace();
									WriteLogSendToServer
											.outputLog(MainWayInfo_Activity.this, e);
								} catch (Exception e) {
									e.printStackTrace();
									WriteLogSendToServer
											.outputLog(MainWayInfo_Activity.this, e);
								}
								
								String strOldWayId = "";
								OutputStream outputStreamO;
								try {

									for (int i = 0; i < aryWayInfoFocus.length; i++) {
										if (strWayInfoFlagForRefresh.equals("1") == true) {
											if (aryWayInfoFocus[i].equals(aryWayInfoRed[itemID]) == false) {
												if (strOldWayId.equals("") == true) {
													strOldWayId = aryWayInfoFocus[i] + ",";
												} else {
													strOldWayId = strOldWayId + aryWayInfoFocus[i] + ",";
												}
											}
										}
										else if (strWayInfoFlagForRefresh.equals("2") == true) {
											if (aryWayInfoFocus[i].equals(aryWayInfoYellow[itemID]) == false) {
												if (strOldWayId.equals("") == true) {
													strOldWayId = aryWayInfoFocus[i] + "," ;
												} else {
													strOldWayId = strOldWayId+ aryWayInfoFocus[i] + ",";
												}
											}
										}
										else if (strWayInfoFlagForRefresh.equals("3") == true) {
											if (aryWayInfoFocus[i].equals(aryWayInfoGreen[itemID]) == false) {
												if (strOldWayId.equals("") == true) {
													strOldWayId = aryWayInfoFocus[i] + ",";
												} else {
													strOldWayId = strOldWayId + aryWayInfoFocus[i] + ",";
												}
											}
										}
										else if (strWayInfoFlagForRefresh.equals("4") == true) {
											if (aryWayInfoFocus[i].equals(aryWayInfoFocus[itemID]) == false) {
												if (strOldWayId.equals("") == true) {
													strOldWayId = aryWayInfoFocus[i] + ",";
												} else {
													strOldWayId = strOldWayId + aryWayInfoFocus[i] + ",";
												}
											}
										}
										
									}

									outputStreamO = MainWayInfo_Activity.this
											.getBaseContext().openFileOutput(
													"wayInfoFocus.sb",
													Context.MODE_PRIVATE);

									if (strOldWayId != "") {									
										FileService.save(outputStreamO,strOldWayId.substring(0, strOldWayId.length() - 1));
										outputStreamO.close();
									} else {								
										FileService.save(outputStreamO,
												strOldWayId);
										outputStreamO.close();
									}

									if (strOldWayId.equals("") == true) {
										aryWayInfoFocus = null;
									} else {
										aryWayInfoFocus = strOldWayId.substring(0, strOldWayId.length() - 1).split(",");
									}
									if (strWayInfoFlagForRefresh.equals("1") == true) {
										adapterRed = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataRed(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, false);

										mContactList.setAdapter(adapterRed);
									} else if (strWayInfoFlagForRefresh.equals("2") == true) {
										adapterYellow = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataYellow(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, false);

										mContactList.setAdapter(adapterYellow);
									} else if (strWayInfoFlagForRefresh.equals("3") == true) {
										adapterGreen = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataGreen(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, false);

										mContactList.setAdapter(adapterGreen);
									} else if (strWayInfoFlagForRefresh.equals("4") == true) {
										adapterFocus = new CustomAdapter(
												MainWayInfo_Activity.this,
												getDataFocus(),
												R.layout.listitem,
												new String[] { "wayinfo",
														"wayinfoDetail",
														"listheader1",
														"listheader2" },
												new int[] { R.id.wayinfo,
														R.id.wayinfoDetail,
														R.id.listheader1,
														R.id.listheader2 }, true);

										mContactList.setAdapter(adapterFocus);
									}
								} catch (FileNotFoundException e) {
									e.printStackTrace();
									WriteLogSendToServer.outputLog(
											MainWayInfo_Activity.this, e);
								} catch (Exception e) {
									e.printStackTrace();
									WriteLogSendToServer.outputLog(
											MainWayInfo_Activity.this, e);
								}

							}
						});
				builder.show();
			}

		}
	}
	
	/**
	 * 从服务器取得路况
	 * 
	 */
	private boolean getWayStatus() {
		String strTempTime;
		strWayStatus = HttpHelper.GetWayStatus(Rid, "Dalian");
		//strWayStatus = "0201111152006,3;3;3;3;1;1;1;3;3;3;3;3;3;3;3;3;3;2;2;2;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;";
		//strWayStatus = "0201106071234,1事故占用双向车道;1;1;1;3;1;3;3;3;1;1;3;2;3;2;3;3;3;3;1;3;1;2;2;2;2;3;3;3;3;3;3;2;3;1;3;2;3;3;2;1;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;3;1;3;";
		String[] aryWayStatus;
		if (strWayStatus.startsWith("0")) {
			strTempTime = strWayStatus.substring(1, 13);
			strFinalTime = "  路况取得时间：" + strTempTime.substring(0, 4) + "/"
					+ strTempTime.substring(4, 6) + "/"
					+ strTempTime.substring(6, 8) + " "
					+ strTempTime.substring(8, 10) + ":"
					+ strTempTime.substring(10, 12);

			strWayStatus = strWayStatus.substring(14, strWayStatus.length());
			strWayInfoRed = "";
			strWayInfoYellow = "";
			strWayInfoGreen = "";

			aryWayStatus = strWayStatus.split(";");
			aryWayStatusDetail = strWayStatus.split(";");

			Log.v("wayStatus", strWayStatus);

			for (int i = 0; i < aryWayStatus.length; i++) {
				aryWayStatusDetail[i] = aryWayStatusDetail[i].substring(1,
						aryWayStatusDetail[i].length());

				if (aryWayStatus[i].substring(0, 1).equals("1") == true) {
					strWayInfoRed = strWayInfoRed + i + ",";
				} else if (aryWayStatus[i].substring(0, 1).equals("2") == true) {
					strWayInfoYellow = strWayInfoYellow + i + ",";
				} else if (aryWayStatus[i].substring(0, 1).equals("3") == true) {
					strWayInfoGreen = strWayInfoGreen + i + ",";
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 与服务器交互失败时，取得上次成功在本地保存的路况数据
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
			aryWayStatusDetail = strWayStatus.split(";");
			strWayInfoRed = "";
			strWayInfoYellow = "";
			strWayInfoGreen = "";
			
			for (int i = 0; i < aryWayStatus.length; i++) {
				aryWayStatusDetail[i] = aryWayStatusDetail[i].substring(1,
						aryWayStatusDetail[i].length());

				if (aryWayStatus[i].substring(0, 1).equals("1") == true) {
					strWayInfoRed = strWayInfoRed + i + ",";
				} else if (aryWayStatus[i].substring(0, 1).equals("2") == true) {
					strWayInfoYellow = strWayInfoYellow + i + ",";
				} else if (aryWayStatus[i].substring(0, 1).equals("3") == true) {
					strWayInfoGreen = strWayInfoGreen + i + ",";
				}
			}

			if (strWayInfoRed.length() != 0) {
				aryWayInfoRed = strWayInfoRed.split(",");
			}
			if (strWayInfoYellow.length() != 0) {
				aryWayInfoYellow = strWayInfoYellow.split(",");
			}
			if (strWayInfoGreen.length() != 0) {
				aryWayInfoGreen = strWayInfoGreen.split(",");
			}

			adapterRed = new CustomAdapter(this, getDataRed(),
					R.layout.listitem, new String[] { "wayinfo",
							"wayinfoDetail", "listheader1", "listheader2" },
					new int[] { R.id.wayinfo, R.id.wayinfoDetail,
							R.id.listheader1, R.id.listheader2 }, false);

			adapterYellow = new CustomAdapter(this, getDataYellow(),
					R.layout.listitem, new String[] { "wayinfo",
							"wayinfoDetail", "listheader1", "listheader2" },
					new int[] { R.id.wayinfo, R.id.wayinfoDetail,
							R.id.listheader1, R.id.listheader2 }, false);

			adapterGreen = new CustomAdapter(this, getDataGreen(),
					R.layout.listitem, new String[] { "wayinfo",
							"wayinfoDetail", "listheader1", "listheader2" },
					new int[] { R.id.wayinfo, R.id.wayinfoDetail,
							R.id.listheader1, R.id.listheader2 }, false);
			
			strFinalTime = "  数据获取失败，显示上一次成功获取的数据";
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}

	/**
	 * 与服务器交互成功后，在本地保存路况数据
	 * 
	 */
	private void saveWayInfo() {
		try {
			OutputStream outputStreamR = this.getBaseContext().openFileOutput(
					"wayInfo.sb", Context.MODE_PRIVATE);
			FileService.save(outputStreamR, strWayStatus);
			outputStreamR.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}

	/**
	 * 从服务器取得路况，按照不同颜色生成listview使用的adapter
	 * 
	 */
	private boolean processGetWayInfo(String strCityName) {
		try {
			// 长时间处理的任务开始
			strWayMaster = Utility.getWayInfoFromCityMaster(this, strCityName);

			String strWayInfoFocus = "";
			InputStream inputStreamO;
			try {
				File filesDir = MainWayInfo_Activity.this.getFilesDir();
				File wayInfoFocus = new File(filesDir + "/wayInfoFocus.sb");

				if (wayInfoFocus.exists()) {
					inputStreamO = MainWayInfo_Activity.this.getBaseContext()
							.openFileInput("wayInfoFocus.sb");

					strWayInfoFocus = FileService.read(inputStreamO);
					inputStreamO.close();
				}

				if (strWayInfoFocus.length() != 0) {
					aryWayInfoFocus = strWayInfoFocus.split(",");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(this, e);
			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(this, e);
			}
			
			if (getWayStatus() == true) {
				if (strWayInfoRed.length() != 0) {
					aryWayInfoRed = strWayInfoRed.split(",");
				}
				if (strWayInfoYellow.length() != 0) {
					aryWayInfoYellow = strWayInfoYellow.split(",");
				}
				if (strWayInfoGreen.length() != 0) {
					aryWayInfoGreen = strWayInfoGreen.split(",");
				}

				adapterRed = new CustomAdapter(this, getDataRed(),
						R.layout.listitem,
						new String[] { "wayinfo", "wayinfoDetail",
								"listheader1", "listheader2" }, new int[] {
								R.id.wayinfo, R.id.wayinfoDetail,
								R.id.listheader1, R.id.listheader2 }, false);

				adapterYellow = new CustomAdapter(this, getDataYellow(),
						R.layout.listitem,
						new String[] { "wayinfo", "wayinfoDetail",
								"listheader1", "listheader2" }, new int[] {
								R.id.wayinfo, R.id.wayinfoDetail,
								R.id.listheader1, R.id.listheader2 }, false);

				adapterGreen = new CustomAdapter(this, getDataGreen(),
						R.layout.listitem,
						new String[] { "wayinfo", "wayinfoDetail",
								"listheader1", "listheader2" }, new int[] {
								R.id.wayinfo, R.id.wayinfoDetail,
								R.id.listheader1, R.id.listheader2 }, false);

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

/*	*//**
	 * 从CityMaster表中取出所有道路名称存入数组中
	 * 
	 *//*
	private String[] getWayInfoFromCityMaster(String strCityName) {
		String[] strWayMaster = null;
		Cursor cursor = null;
		DataBaseHelper dbhelper = null;
		try {
			int i = 0;
			dbhelper = new DataBaseHelper(MainWayInfo_Activity.this,
					"sparkbird.db", null, 1);
			SQLiteDatabase getReadableDatabase = dbhelper.getReadableDatabase();
			
			 * Cursor cursor = getReadableDatabase.query("Dalian", new String[]
			 * { "WayID", "WayName" }, "WayID=?", new String[] {"1"}, null,
			 * null, null);
			 
			cursor = getReadableDatabase.rawQuery("select WayID,WayName from "
					+ strCityName + " order by 1", null);

			strWayMaster = new String[cursor.getCount()];

			while (cursor.moveToNext()) {
				String wayrecord = cursor.getString(cursor
						.getColumnIndex("WayName"));

				
				 * Log.v("DB***********",
				 * cursor.getString(cursor.getColumnIndex("WayName")));
				 

				Log.v("master表中的路段名", wayrecord);

				strWayMaster[i] = wayrecord;
				i++;
			}
			return strWayMaster;

		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		} finally {
			cursor.close();
			dbhelper.close();
		}

		return null;

	}*/

	/**
	 * 取得关注的路段的颜色
	 * 
	 */
	private String wayInfoOld_getColor(String wayID) {
		if (aryWayInfoRed != null) {
			for (int i = 0; i < aryWayInfoRed.length; i++) {
				if (aryWayInfoRed[i].equals(wayID) == true) {
					return "1";
				}
			}
		}

		if (aryWayInfoYellow != null) {
			for (int i = 0; i < aryWayInfoYellow.length; i++) {
				if (aryWayInfoYellow[i].equals(wayID) == true) {
					return "2";
				}
			}
		}

		return "3";
	}

	private List<Map<String, Object>> getDataFocus() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		String strTemp;
		try {
			Map<String, Object> map = new HashMap<String, Object>();

			if (aryWayInfoFocus != null) {
				for (int i = 0; i < aryWayInfoFocus.length; i++) {
					String strtmp = strWayMaster[Integer
							.parseInt(aryWayInfoFocus[i])];
					map = new HashMap<String, Object>();
					map.put("wayinfo", strtmp);
					if (wayInfoOld_getColor(aryWayInfoFocus[i]).equals("1") == true) {
						if (aryWayStatusDetail[Integer
								.parseInt(aryWayInfoFocus[i])].equals("") == false) {
							map.put("wayinfoDetail", aryWayStatusDetail[Integer
									.parseInt(aryWayInfoFocus[i])]);
						} else {
							map.put("wayinfoDetail", "「该路段目前为红色饱和状态」");
						}
					} else if (wayInfoOld_getColor(aryWayInfoFocus[i]).equals(
							"2") == true) {
						if (aryWayStatusDetail[Integer
								.parseInt(aryWayInfoFocus[i])].equals("") == false) {
							map.put("wayinfoDetail", aryWayStatusDetail[Integer
									.parseInt(aryWayInfoFocus[i])]);
						} else {
							map.put("wayinfoDetail", "「该路段目前为黄色缓行状态」");
						}
					} else {
						if (aryWayStatusDetail[Integer
								.parseInt(aryWayInfoFocus[i])].equals("") == false) {
							map.put("wayinfoDetail", aryWayStatusDetail[Integer
									.parseInt(aryWayInfoFocus[i])]);
						} else {
							map.put("wayinfoDetail", "「该路段目前为绿色畅通状态」");
						}
					}
					strTemp = String.format("%02d", i + 1);
					map.put("listheader1", strTemp);
					map.put("listheader2", "已关注");
					list.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
		return list;
	}

	private List<Map<String, Object>> getDataRed() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		String strTemp;
		try {
			Map<String, Object> map = new HashMap<String, Object>();

			if (aryWayInfoRed != null) {
				for (int i = 0; i < aryWayInfoRed.length; i++) {
					String strtmp = strWayMaster[Integer
							.parseInt(aryWayInfoRed[i])];
					map = new HashMap<String, Object>();
					map.put("wayinfo", strtmp);
					if (aryWayStatusDetail[Integer.parseInt(aryWayInfoRed[i])]
							.equals("") == false) {
						map.put("wayinfoDetail", aryWayStatusDetail[Integer
								.parseInt(aryWayInfoRed[i])]);
					} else {
						map.put("wayinfoDetail", "「该路段目前为红色饱和状态」");
					}
					
					strTemp = String.format("%02d", i + 1);
					map.put("listheader1", strTemp);
					
					if (aryWayInfoFocus != null) {
						for (int j = 0; j < aryWayInfoFocus.length; j++) {
							if (aryWayInfoFocus[j].equals(aryWayInfoRed[i]) == true) {
								map.put("listheader2", "已关注");
							}
						}
					}
					
					list.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
		return list;
	}

	private List<Map<String, Object>> getDataYellow() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		String strTemp;
		try {
			Map<String, Object> map = new HashMap<String, Object>();

			if (aryWayInfoYellow != null) {
				for (int i = 0; i < aryWayInfoYellow.length; i++) {
					String strtmp = strWayMaster[Integer
							.parseInt(aryWayInfoYellow[i])];
					map = new HashMap<String, Object>();
					map.put("wayinfo", strtmp);
					if (aryWayStatusDetail[Integer.parseInt(aryWayInfoYellow[i])]
							.equals("") == false) {
						map.put("wayinfoDetail", aryWayStatusDetail[Integer
								.parseInt(aryWayInfoYellow[i])]);
					} else {
						map.put("wayinfoDetail", "「该路段目前为黄色缓行状态」");
					}
					strTemp = String.format("%02d", i + 1);
					map.put("listheader1", strTemp);
					
					if (aryWayInfoFocus != null) {
						for (int j = 0; j < aryWayInfoFocus.length; j++) {
							if (aryWayInfoFocus[j].equals(aryWayInfoYellow[i]) == true) {
								map.put("listheader2", "已关注");
							}
						}
					}
					
					list.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
		return list;
	}

	private List<Map<String, Object>> getDataGreen() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		String strTemp;
		try {
			Map<String, Object> map = new HashMap<String, Object>();

			if (aryWayInfoGreen != null) {
				for (int i = 0; i < aryWayInfoGreen.length; i++) {
					String strtmp = strWayMaster[Integer
							.parseInt(aryWayInfoGreen[i])];
					map = new HashMap<String, Object>();
					map.put("wayinfo", strtmp);
					if (aryWayStatusDetail[Integer.parseInt(aryWayInfoGreen[i])]
							.equals("") == false) {
						map.put("wayinfoDetail", aryWayStatusDetail[Integer
								.parseInt(aryWayInfoGreen[i])]);
					} else {
						map.put("wayinfoDetail", "「该路段目前为绿色畅通状态」");
					}
					strTemp = String.format("%02d", i + 1);
					map.put("listheader1", strTemp);
					
					if (aryWayInfoFocus != null) {
						for (int j = 0; j < aryWayInfoFocus.length; j++) {
							if (aryWayInfoFocus[j].equals(aryWayInfoGreen[i]) == true) {
								map.put("listheader2", "已关注");
							}
						}
					}
					
					list.add(map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
		return list;
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
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			WriteLogSendToServer.outputLog(this, e);
			return null;
		}
	}

	// ----------For update check Start
	/**
	 * 初始化全局变量 实际工作中这个方法中serverVersion从服务器端获取，最好在启动画面的activity中执行
	 */
	/*
	 * public void initGlobal() { try { Global.localVersion =
	 * getPackageManager().getPackageInfo( getPackageName(), 0).versionCode; //
	 * 设置本地版本号 // serverVersion应该从服务器端获取 Global.serverVersion = 2;//
	 * 假定服务器版本为2，本地版本默认是1 } catch (Exception ex) { ex.printStackTrace(); } }
	 */

	/**
	 * 检查更新版本
	 */
	/*
	 * public void checkVersion() { if (Global.localVersion <
	 * Global.serverVersion) { // 发现新版本，提示用户更新 AlertDialog.Builder alert = new
	 * AlertDialog.Builder(this); alert.setTitle("软件升级")
	 * .setIcon(R.drawable.sb004title) .setMessage("发现新版本,建议立即更新使用.")
	 * .setPositiveButton("更新", new DialogInterface.OnClickListener() {
	 * 
	 * public void onClick(DialogInterface dialog, int which) { //
	 * 开启更新服务UpdateService // 这里为了把update更好模块化，可以传一些updateService依赖的值 //
	 * 如布局ID，资源ID，动态获取的标题,这里以app_name为例 Intent updateIntent = new Intent(
	 * MainWayInfo_Activity.this, UpdateService.class);
	 * updateIntent.putExtra("titleId", R.string.app_name);
	 * startService(updateIntent); } })
	 * 
	 * .setNegativeButton("取消", new DialogInterface.OnClickListener() { public
	 * void onClick(DialogInterface dialog, int which) { dialog.dismiss(); } });
	 * alert.create().show(); } else { // 清理工作 cheanUpdateFile(); } }
	 */

	/**
	 * 清理工作
	 */
	/*
	 * private void cheanUpdateFile() { File updateFile = new
	 * File(Global.downloadDir, getResources() .getString(R.string.app_name) +
	 * ".apk");
	 * 
	 * if (updateFile.exists()) { // 当不需要的时候，清除之前的下载文件，避免浪费用户空间
	 * updateFile.delete(); } }
	 */
	// ----------For update check End

	/**
	 * 定时器
	 */
	Handler handlerRefresh = new Handler();
	Runnable runnableRefresh = new Runnable() {
		@Override
		public void run() {
			final ProgressDialog dialog = ProgressDialog.show(
					MainWayInfo_Activity.this, null, "数据自动刷新中，请稍等。", false);
			final Handler handler = new Handler() {
				public void handleMessage(Message msg) {
					dialog.dismiss();
				}
			};

			Looper looper = Looper.myLooper();
			messageHandler = new MessageHandler(looper);

			try {
				refreshThread = new Thread() {
					@Override
					public void run() {
						if (ttsService != null) {
							try {
								if (ttsService.isSpeaking() == true) {
									ttsService.stop();
								}
							} catch (RemoteException e) {
								e.printStackTrace();
								WriteLogSendToServer.outputLog(
										MainWayInfo_Activity.this, e);
							}
						}

						// 创建一个Message对象，并把当前显示的路况颜色赋值给Message对象
						Message message = Message.obtain();

						if (processGetWayInfo("Dalian") == true) {
							saveWayInfo();
							if ("1".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "1";
							} else if ("2".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "2";
							} else if ("3".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "3";
							} else if ("4".equals(strWayInfoFlagForRefresh) == true) {
								// 创建一个Message对象，赋值给Message对象
								message.obj = "4";
							}
						} else {
							getOldWayInfo();
							message.obj = "00";
						}

						// 通过Handler发布携带有adapter的消息
						messageHandler.sendMessage(message);
						handler.sendEmptyMessage(0);
					}
				};

				refreshThread.start();

			} catch (Exception e) {
				e.printStackTrace();
				WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
			}

			handlerRefresh.postDelayed(this, auto_refresh_time);
		}
	};

	/**
	 * 自动更新设定
	 */
	private void auto_refresh() {
		auto_refresh_index = preferences.getInt(UtilityConst.SP_KEY_AT_REFRESH,
				UtilityConst.INT_99_VALUE);

		if (auto_refresh_index != UtilityConst.INT_99_VALUE) {
			switch (auto_refresh_index) {
			case 0:
				auto_refresh_time = 60000;
				break;
			case 1:
				auto_refresh_time = 180000;
				break;
			case 2:
				auto_refresh_time = 300000;
				break;
			case 3:
				auto_refresh_time = 600000;
				break;
			case 4:
				auto_refresh_time = 1200000;
				break;
			case 5:
				auto_refresh_time = 0;
				break;
			}

			if (auto_refresh_time != 0) {
				handlerRefresh.postDelayed(runnableRefresh, auto_refresh_time);
			}
		}
	}
	
	/**
	 * 创建桌面快捷方式
	 */
	private void create_shortcut(String popupFlag) {
		try {
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
			Intent respondIntent = new Intent(MainWayInfo_Activity.this,
					Welcome_Activity.class);
			respondIntent.setAction(action);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, respondIntent);
			// 下面的方法与上面的效果是一样的,另一种构建形式而已
			// 注意:
			// ComponentName的第二个参数必须加上点号(.)，否则快捷方式无法启动相应程序
			/*
			 * String appClass = this.getPackageName() + "." +
			 * this.getLocalClassName();
			 */
			/*
			 * String appClass = Welcome_Activity.class.getName();
			 * 
			 * ComponentName comp = new ComponentName(this.getPackageName(),
			 * appClass); shortcut.putExtra(Intent. EXTRA_SHORTCUT_INTENT, new
			 * Intent(action).setComponent(comp));
			 */

			// 快捷方式的图标
			ShortcutIconResource iconRes = Intent.ShortcutIconResource
					.fromContext(MainWayInfo_Activity.this, R.drawable.sb004);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);

			sendBroadcast(shortcut);
			
			if (popupFlag.equals("1") == true) {
				Toast.makeText(MainWayInfo_Activity.this.getBaseContext(),
						"快捷方式创建成功", Toast.LENGTH_LONG).show();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
	}
	
	/**
	 * 创建微博按钮点击率报告
	 */
	private void createWeiboBtnReport() {
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

			if (Rid != null) {
				WeiboBtnReportName = "btnWeiboClick-" + Rid + "-" + mTimeStamp
						+ ".cr";
			} else {
				WeiboBtnReportName = "btnWeiboClick-" + imei + "-" + mTimeStamp
						+ ".cr";
			}
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
		}
	}

	/**
	 * 记录微博按钮点击率
	 */
	private void recordWeibotn(String fromwhere) {
		try {
			String FileContent;
			OutputStream outputStreamO;
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss");
			Date d = new Date();
			
			outputStreamO = MainWayInfo_Activity.this.getBaseContext()
					.openFileOutput(WeiboBtnReportName, Context.MODE_APPEND);
			FileContent = fromwhere + " " + formatter.format(d) + "\r" + "\n";
			FileService.save(outputStreamO, FileContent);
			outputStreamO.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(MainWayInfo_Activity.this, e);
		}
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
}