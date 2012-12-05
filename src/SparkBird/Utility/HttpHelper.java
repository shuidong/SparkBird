/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.Utility;

import java.io.File;
import java.io.IOException;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class HttpHelper {
	
	private static String serviceUrl="http://sb.dalian1008.com/sparkbird/";
	private static String nameSpace = "http://dalian1008.com/";
	
	public static String CheckRegisterInfo(String rid, String clientversion)
	{
		String serviceURL = serviceUrl + "RegisterWS.asmx";
        String methodName = "CheckRegisterInfo";
        String soapAction = nameSpace + methodName;
        SoapObject request = new SoapObject(nameSpace, methodName);
        
        request.addProperty("rid", rid);
        request.addProperty("clientversion", clientversion);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.bodyOut = request;
        envelope.dotNet=true;
        HttpTransportSE ht = new HttpTransportSE(serviceURL, 12000);
        try {
            ht.call(soapAction, envelope);
            if (envelope.getResponse() != null) {
                return envelope.getResponse().toString();
            } else {
            	return "1";
            }
        } catch (Exception e) {
        	//todo zhangzl log for exception
            Log.v("HttpRestonseErorr", e.toString());
            return "1";
        }
	}
	
	public static String Register(String imsi, String imei, String os_version, String model, String sdkVersion, String clientversion)
    {
        String serviceURL = serviceUrl + "RegisterWS.asmx";
        String methodName = "Register";
        String soapAction = nameSpace + methodName;
        SoapObject request = new SoapObject(nameSpace, methodName);
        
        request.addProperty("imsi", imsi);
        request.addProperty("imei", imei);
        request.addProperty("os_version", os_version);
        request.addProperty("model", model);
        request.addProperty("sdkVersion", sdkVersion);
        request.addProperty("clientversion", clientversion);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.bodyOut = request;
        envelope.dotNet=true;
        Log.v("HttpRestonseErorr","Start");
        HttpTransportSE ht = new HttpTransportSE(serviceURL, 12000);
        try {
            ht.call(soapAction, envelope);
            if (envelope.getResponse() != null) {
                return envelope.getResponse().toString();
            } else {
            	return "1";
            }
        } catch (Exception e) {
        	//todo zhangzl log for exception
            Log.v("HttpRestonseErorr", e.toString());
            return "1";
        }
    	
    }

	/**
	 * 取得路況
	 * 
	 */
	public static String GetWayStatus(String rid, String cityName) {
		String serviceURL = serviceUrl + "RequestWayInfo.asmx";
		String methodName = "getWayInfo";
		String soapAction = nameSpace + methodName;
		SoapObject request = new SoapObject(nameSpace, methodName);

		request.addProperty("rid", rid);
		request.addProperty("strCityName", cityName);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.bodyOut = request;
		envelope.dotNet = true;
		HttpTransportSE ht = new HttpTransportSE(serviceURL, 12000);
		try {
			ht.call(soapAction, envelope);
			if (envelope.getResponse() != null) {
				return envelope.getResponse().toString();
			} else {
				return "1";
			}
		} catch (Exception e) {
			// todo zhangzl log for exception
			Log.v("HttpRestonseErorr", e.toString());
			return "1";
		}
	}
	
	/**
	 * 判断手机的网络连接是否可用
	 * 
	 */
	public static boolean isNetworkAvailable(Activity mActivity) {
		Context context = mActivity.getApplicationContext();
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			return false;
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 发送反馈
	 * 
	 */
	public static boolean SendSuggestion(String suggestion, String contact) {
		boolean rt = false;
		String serviceURL = serviceUrl + "Suggestion.asmx";
		//String serviceURL = "http://153.65.84.29:12345/Service1.asmx";
		//String serviceURL = "http://192.168.0.6:12345/Service1.asmx";
		
		String methodName = "SaveSuggestion";
		String soapAction = nameSpace + methodName;
		SoapObject request = new SoapObject(nameSpace, methodName);

		request.addProperty("strSuggestion", suggestion);
		request.addProperty("strContact",contact);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.bodyOut = request;
		envelope.dotNet = true;
		HttpTransportSE ht = new HttpTransportSE(serviceURL, 12000);
		try {
			ht.call(soapAction, envelope);
			if (envelope.getResponse() != null) {
				String StrRt = envelope.getResponse().toString();
				Log.v("发送反馈成功，服务器返回：", StrRt);
				if (StrRt.equals("true") == true) {
					rt = true;
				}
			}
		} catch (Exception e) {
			Log.v("发送反馈出错", e.toString());
		}
		return rt;
	}
	
	/**
	 * 发送log文件
	 * 
	 */
	public static boolean postReport(Context ctx, File file) {
		boolean rt = false;
		String serviceURL = serviceUrl + "ExLog.asmx";
		//String serviceURL = "http://153.65.84.29:12345/Service1.asmx";
		//String serviceURL = "http://192.168.0.6:12345/Service1.asmx";
		
		String methodName = "SaveFile";
		String soapAction = nameSpace + methodName;
		SoapObject request = new SoapObject(nameSpace, methodName);

		String updata = "";
		try {
			updata = WriteLogSendToServer.getBytesFromFile(ctx, file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		request.addProperty("updata", updata);
		request.addProperty("fileName", file.getName());
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.bodyOut = request;
		envelope.dotNet = true;
		HttpTransportSE ht = new HttpTransportSE(serviceURL, 12000);
		try {
			ht.call(soapAction, envelope);
			if (envelope.getResponse() != null) {
				String StrRt = envelope.getResponse().toString();
				Log.v("上传文件成功，服务器返回：", StrRt);
				if (StrRt.equals("true") == true) {
					rt = true;
				}
			} else {

			}
		} catch (Exception e) {
			Log.v("上传文件出错", e.toString());
		}
		return rt;
	}
	
}