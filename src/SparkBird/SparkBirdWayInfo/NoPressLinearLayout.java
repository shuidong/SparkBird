/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * @author dlqingxi
 *
 */
public class NoPressLinearLayout extends LinearLayout {

	public NoPressLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void setPressed(boolean pressed) {
		super.setPressed(false);
	}
}
