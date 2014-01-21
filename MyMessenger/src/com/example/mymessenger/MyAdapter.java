package com.example.mymessenger;

import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {
	LayoutInflater lInflater;
	List<mMessage> data;
	Context context;
	
	MyAdapter(Context context, List<mMessage> msgs) {
	    lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    data = msgs;
	    this.context = context;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//View view = convertView; // ������������ �������� View, ������ ��� ������ � �������
		View view = null;
		
	    if (view == null) {
	      view = lInflater.inflate(R.layout.list_row_layout, parent, false);
	    }
	    
	    mMessage msg = data.get(position);
	    
	    boolean left = msg.sender == ((MyApplication) context.getApplicationContext()).getService( MessageService.SMS ).getMyName();
	    
    	TextView textLabel = (TextView) view.findViewById(R.id.author_text);
    	textLabel.setText( msg.sender );
        
    	textLabel = (TextView) view.findViewById(R.id.msg_text);
    	textLabel.setText( msg.text );
    	
    	textLabel.setBackgroundResource(left ? R.drawable.bubble_yellow : R.drawable.bubble_green);
    	textLabel.setGravity(left ? Gravity.LEFT : Gravity.RIGHT);
    	
    	RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) textLabel.getLayoutParams();
    	lp.addRule(left ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT);
    	textLabel.setLayoutParams(lp);
    	
    	Log.d("MyAdapter", data.size() + " : " + position + " : " + msg.text);
		return view;
	}

}
