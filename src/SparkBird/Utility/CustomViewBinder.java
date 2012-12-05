/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.Utility;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter.ViewBinder;

public class CustomViewBinder implements ViewBinder {
	public boolean setViewValue(View view, Object data,
			String textRepresentation) {
		if ((view instanceof ImageView) & (data instanceof Bitmap)) {
			ImageView iv = (ImageView) view;
			Bitmap bm = (Bitmap) data;
			iv.setImageBitmap(bm);
			return true;
		}
		return false;
	}

}
