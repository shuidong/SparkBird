/*
 * Copyright (c) 2011 安致创想 工作室. 
 * All rights reserved. http://www.dalian1008.com/
 * 
 * The soft is Non-commercial use only
 * Cannot modify source-code for any purpose (cannot create derivative works)
 */
package SparkBird.SparkBirdWayInfo;

import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * @author dlqingxi
 * 
 */
public class CustomAdapter extends BaseAdapter {

	MainWayInfo_Activity selfContext;

	class customClickListener implements View.OnClickListener {

		public final String weiboMsg;

		public customClickListener(String msg) {
			weiboMsg = msg;
		}

		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(selfContext);
			builder.setTitle("分享");
			builder.setIcon(R.drawable.sb004title);
			builder.setMessage("确定要将此路况信息分享到其它工具吗？");
			builder.setNegativeButton("取消", null);
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent=new Intent(Intent.ACTION_SEND);   
					        intent.setType("text/plain");
					        intent.putExtra(Intent.EXTRA_TEXT, weiboMsg + "--来自#火鸟路况#dalian1008.com");    
					        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   
					        selfContext.startActivity(Intent.createChooser(intent, "选择程序"));   
						}
					});
			builder.show();
			
		}
	}

	public final class ViewHolder {
		public TextView lstHeard1;
		public TextView lstHeard2;
		public TextView wayinfo;
		public TextView wayinfoDetail;
		public ImageButton shareWeibo;
	}

	private LayoutInflater mInflater;
	private List<Map<String, Object>> mListItem;
	private boolean ifFocus;
	
	public CustomAdapter(Context context, List<Map<String, Object>> mListItem,
			int RID, String[] keys, int[] RIDs, boolean ifFocus) {
		selfContext = (MainWayInfo_Activity)context;
		this.mInflater = LayoutInflater.from(context);
		this.mListItem = mListItem;
		this.ifFocus = ifFocus;
	}

	@Override
	public int getCount() {
		return mListItem.size();
	}

	@Override
	public Object getItem(int position) {
		return mListItem.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.listitem, null);
			holder.lstHeard1 = (TextView) convertView
					.findViewById(R.id.listheader1);
			holder.lstHeard2 = (TextView) convertView
					.findViewById(R.id.listheader2);
			holder.wayinfo = (TextView) convertView.findViewById(R.id.wayinfo);
			holder.wayinfoDetail = (TextView) convertView
					.findViewById(R.id.wayinfoDetail);
			holder.shareWeibo = (ImageButton) convertView
					.findViewById(R.id.weiboRt);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.lstHeard1.setText((String) mListItem.get(position).get(
				"listheader1"));
		holder.lstHeard2.setText((String) mListItem.get(position).get(
				"listheader2"));
		holder.wayinfo.setText((String) mListItem.get(position).get("wayinfo"));
		holder.wayinfoDetail.setText((String) mListItem.get(position).get(
				"wayinfoDetail"));
		String weiboMsg = (String) mListItem.get(position).get("wayinfo")
				+ " : " + (String) mListItem.get(position).get("wayinfoDetail");
		holder.shareWeibo.setOnClickListener(new customClickListener(weiboMsg));
		
		if (ifFocus)
		{
			if ("「该路段目前为红色饱和状态」".equals(mListItem.get(position).get("wayinfoDetail")) == true)
			{
				holder.lstHeard1.setTextColor(convertView.getResources().getColor(R.color.Crimson));
				holder.lstHeard2.setTextColor(convertView.getResources().getColor(R.color.Crimson));
			}
			else if ("「该路段目前为黄色缓行状态」".equals(mListItem.get(position).get("wayinfoDetail")) == true)
			{
				holder.lstHeard1.setTextColor(convertView.getResources().getColor(R.color.Gold));
				holder.lstHeard2.setTextColor(convertView.getResources().getColor(R.color.Gold));
			}
			else if ("「该路段目前为绿色畅通状态」".equals(mListItem.get(position).get("wayinfoDetail")) == true)
			{
				holder.lstHeard1.setTextColor(convertView.getResources().getColor(R.color.LightGreen));
				holder.lstHeard2.setTextColor(convertView.getResources().getColor(R.color.LightGreen));
			}
		}
		
		return convertView;
	}

}
