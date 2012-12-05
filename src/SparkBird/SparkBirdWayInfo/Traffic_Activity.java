/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import mapABC.MyLocationOverlayProxy;
import SparkBird.Utility.CrashHandler;
import SparkBird.Utility.UtilityConst;
import SparkBird.Utility.WriteLogSendToServer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.mapabc.mapapi.GeoPoint;
import com.mapabc.mapapi.MapActivity;
import com.mapabc.mapapi.MapController;
import com.mapabc.mapapi.MapView;

import com.mobclick.android.MobclickAgent;

/**
 * 目前支持实时路况的城市有：北京 上海 广州 深圳 成都 南京 沈阳 武汉 宁波 重庆 青岛 杭州
 * 实时路况的图符块不会被缓存，且每5分钟左右更新一次。
 * 矢量地图暂时还不支持矢量地图，请期待。
 */
public class Traffic_Activity extends MapActivity{

	private MapView mMapView;
	private MapController mMapController;
	private GeoPoint point;
	private ImageButton trafficLayer;
	private static boolean isTraffic = true;//处理实时路况
	private MyLocationOverlayProxy mLocationOverlay;
	
	private ImageButton btnReturn;

	@Override
	/**
	*显示栅格地图，启用内置缩放控件，并用MapController控制地图的中心点及Zoom级别
	*/
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.traffic);

		CrashHandler crashHandler = CrashHandler.getInstance();
		// 注册crashHandler
		crashHandler.init(this);

		try {
			mMapView = (MapView) findViewById(R.id.traffic_mapView);
			mMapView.setBuiltInZoomControls(true); // 设置启用内置的缩放控件
			mMapController = mMapView.getController(); // 得到mMapView的控制权,可以用它控制和驱动平移和缩放

			// 北京
			/*
			 * point = new GeoPoint((int) (39.90923 * 1E6), (int) (116.397428 *
			 * 1E6)); //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
			 */

			// 大连
			point = new GeoPoint((int) (38.900001 * 1E6),
					(int) (121.629997 * 1E6)); // 用给定的经纬度构造一个GeoPoint，单位是微度 (度 *
												// 1E6)

			mMapController.setCenter(point); // 设置地图中心点
			mMapController.setZoom(12); // 设置地图zoom级别
			mLocationOverlay = new MyLocationOverlayProxy(this, mMapView);
			mMapView.getOverlays().add(mLocationOverlay);
			// 实现初次定位使定位结果居中显示
			mLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					handler.sendMessage(Message.obtain(handler,
							UtilityConst.FIRST_LOCATION));
				}
			});

			mMapView.setTraffic(true);
			isTraffic = true;

			trafficLayer = (ImageButton) findViewById(R.id.ImageButtonTraffic);

			trafficLayer.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showDialog(UtilityConst.DIALOG_LAYER);
				}
			});

			btnReturn = (ImageButton) findViewById(R.id.imageButton1);
			btnReturn.setOnClickListener(new btnReturnClickListener());

		} catch (Exception e) {
			e.printStackTrace();
			WriteLogSendToServer.outputLog(this, e);
		}
		
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case UtilityConst.DIALOG_LAYER:
		String[] traffic = {getResources().getString(
					R.string.real_time_traffic)};
	    boolean[] traffic_falg = new boolean[] { isTraffic};
        return new AlertDialog.Builder(Traffic_Activity.this).setTitle(
			R.string.choose_layer).setMultiChoiceItems(traffic,
					traffic_falg, new DialogInterface.OnMultiChoiceClickListener() {

				public void onClick(DialogInterface dialog, int which,
						boolean isChecked) {

					if (which == 0) {
                         if (isChecked) {
							mMapView.setTraffic(true);//显示实时路况
                        } else {
							mMapView.setTraffic(false);//关闭实时路况
                        }
						isTraffic = isChecked;
					}
				
					mMapView.postInvalidate();
					dismissDialog(UtilityConst.DIALOG_LAYER);

				}

			}).setPositiveButton(R.string.alert_dialog_cancel,
			new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(UtilityConst.DIALOG_LAYER);
				}
			}).create();
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPause() {
    	this.mLocationOverlay.disableMyLocation();
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onResume() {
		this.mLocationOverlay.enableMyLocation();
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == UtilityConst.FIRST_LOCATION) {
				mMapController.animateTo(mLocationOverlay.getMyLocation());
			}
		}
    };
	
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
