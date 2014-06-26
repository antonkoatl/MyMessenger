package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyMsgAdapter extends BaseAdapter {
	LayoutInflater lInflater;
	List<MsgListItem> data;
	Context context;
	
	
	
	public MyMsgAdapter(Context context, List<mMessage> msgs) {
	    lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    data = new ArrayList<MsgListItem>();
	    if(msgs != null){
		    for(mMessage msg : msgs)
		    	data.add( new MsgListItem(msg, ((MyApplication) context.getApplicationContext())) );
	    }
	    this.context = context;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//View view = convertView; // Использовать повторно View, решить что делать с шириной
		View view = null;
		
				
	    if (view == null) {
	    	view = lInflater.inflate(R.layout.list_row_layout, parent, false);
	    }
	    
	    
	    MsgListItem mli = data.get(position);
	    
	    mli.setupView(view);
	    	
		return view;
	}
	
	public float convertPixelsToDp(float px){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	}
	
	public float convertDpToPixel(float dp){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float px = dp * (metrics.densityDpi / 160f);
	    return px;
	}

	public int indexOf(mMessage msg) {
		return data.indexOf(msg);
	}

	public void add(int i, mMessage msg) {
		data.add(i, new MsgListItem(msg, ((MyApplication) context.getApplicationContext()) ));
		notifyDataSetChanged();	
	}

	public void add(mMessage msg) {
		data.add(new MsgListItem(msg, ((MyApplication) context.getApplicationContext()) ));
		notifyDataSetChanged();	
	}

	public mMessage remove(int tind) {
		mMessage t = data.remove(tind).msg; 
		notifyDataSetChanged();
		return t;			
	}

}
