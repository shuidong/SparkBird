/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LukuangWidgetConfiguration extends Activity {
	private Button btn1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setInflateCancleResult();

		setContentView(R.layout.lukuang_widget_configure);

		btn1 = (Button) findViewById(R.id.button1);
		btn1.setOnClickListener(new btn1ClickListener());
	}
	
	private final class btn1ClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			setInflateFinishResult();
			finish();
		}
	}

	/**
	 * 如果在Activity未完成启动时按下了Back键，则不启动Widget(这也可以不进行设置，只是为了更好的逻辑)
	 */
	private void setInflateCancleResult() {
		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);
	}

	/**
	 * 必须在完成启动后将当前的Widget设置为返回结果，否则Activity退出后将不启动Widget
	 */
	private void setInflateFinishResult() {
		// Find the widget id from the intent.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		int mAppWidgetId = 0;
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}

		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
	}

}