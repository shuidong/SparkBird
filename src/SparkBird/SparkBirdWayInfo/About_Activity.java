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
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class About_Activity extends Activity {
	static Context ctx = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		ctx = this;

		// logo按钮
		ImageButton btnlogo = (ImageButton) findViewById(R.id.btnlogo);
		btnlogo.setOnClickListener(new btnlogoClickListener());

		TextView aboutTitle1 = (TextView) findViewById(R.id.aboutTitle1);
		TextView aboutTitle2 = (TextView) findViewById(R.id.aboutTitle2);
		TextView about1 = (TextView) findViewById(R.id.about1);
		TextView about2 = (TextView) findViewById(R.id.about2);
		TextView about3 = (TextView) findViewById(R.id.about3);
		TextView about4 = (TextView) findViewById(R.id.about4);
		TextView about5 = (TextView) findViewById(R.id.about5);
/*		TextView about6 = (TextView) findViewById(R.id.about6);
		TextView about7 = (TextView) findViewById(R.id.about7);*/
		TextView about8 = (TextView) findViewById(R.id.about8);
		TextView about9 = (TextView) findViewById(R.id.about9);
		TextView about10 = (TextView) findViewById(R.id.about10);
		
		aboutTitle1.setText("火鸟路况");
		aboutTitle2.setText("版本号：" + getVersionName());

		about1.setText("本软件的下载、安装完全免费，使用过程中产生的数据流量费用，由运营商收取。");
		about2.setText("安致创想工作室致力于创造卓越的手机应用。");

		about3.setText("版本发布时间：" + getString(R.string.VersionTime));

		about4.setText(Html
				.fromHtml("<b>官方网站：</b> "
						+ "<a href=\"http://www.dalian1008.com\">http://www.dalian1008.com</a> "));
		
		about5.setText(Html.fromHtml("<b>开发团队：</b> "
				+ "<a href=mailto:dalian1008@gmail.com>安致创想工作室</a> "));
		
/*		about5.setText(Html.fromHtml("<b>手机客户端开发：</b> "
				+ "<a href=mailto:weibin.td@gmail.com>Bill Wei</a> "));
		about6.setText(Html.fromHtml("<b>服务器端开发：</b> "
				+ "<a href=mailto:wszzl2001@gmail.com>Michael Zhang</a> "));
		about7.setText(Html.fromHtml("<b>UI设计：</b> "
				+ "<a href=mailto:chaosofminds@gmail.com>Cage Liang</a> "));*/
		
		about8.setText(Html
				.fromHtml("<b>官方微博：</b> "
						+ "<a href=\"http://weibo.cn/sparkbird\">http://weibo.cn/sparkbird</a> "));
		
		about9.setText("官方QQ群： 3287476");
		about10.setText(getString(R.string.copyright));
		
		about4.setMovementMethod(LinkMovementMethod.getInstance());
		about5.setMovementMethod(LinkMovementMethod.getInstance());
		/*about6.setMovementMethod(LinkMovementMethod.getInstance());
		about7.setMovementMethod(LinkMovementMethod.getInstance());*/
		about8.setMovementMethod(LinkMovementMethod.getInstance());
		// 捕获超链接的点击事件
		/*
		 * CharSequence text = about3.getText(); if (text instanceof Spannable)
		 * { int end = text.length(); Spannable sp = (Spannable)
		 * about3.getText(); URLSpan[] urls = sp.getSpans(0, end,
		 * URLSpan.class); SpannableStringBuilder style = new
		 * SpannableStringBuilder(text); style.clearSpans();// should clear old
		 * spans for (URLSpan url : urls) { MyURLSpan myURLSpan = new
		 * MyURLSpan(url.getURL()); style.setSpan(myURLSpan,
		 * sp.getSpanStart(url), sp.getSpanEnd(url),
		 * Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); } about3.setText(style); }
		 */
	}

	// 捕获超链接的点击事件
	/*
	 * private static class MyURLSpan extends ClickableSpan {
	 * 
	 * private String mUrl;
	 * 
	 * MyURLSpan(String url) { mUrl = url; }
	 * 
	 * @Override public void onClick(View widget) { // TODO Auto-generated
	 * method stub Toast.makeText(ctx, mUrl, Toast.LENGTH_LONG).show(); } }
	 */

	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	
	/**
	 * logo按钮事件
	 * 
	 */
	private final class btnlogoClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			finish();
		}
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
}