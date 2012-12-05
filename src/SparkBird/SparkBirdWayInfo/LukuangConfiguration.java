/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LukuangConfiguration extends Activity {
	private Button btn1;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_list);

		btn1 = (Button) findViewById(R.id.button1);
		btn1.setOnClickListener(new btn1ClickListener());
	}

	private final class btn1ClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			finish();
		}
	}
}