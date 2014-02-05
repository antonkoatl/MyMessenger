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
	public boolean isLoading;
	
	MyAdapter(Context context, List<mMessage> msgs) {
	    lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    data = msgs;
	    isLoading = false;
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
		//View view = convertView; // Использовать повторно View, решить что делать с шириной
		View view = null;
		
		if(position == 0 && isLoading){
			if (view == null) {
				view = lInflater.inflate(R.layout.list_row_layout, parent, false);
			}
			
			TextView textLabel = (TextView) view.findViewById(R.id.msg_text);
	    	textLabel.setText( "loading..." );
	    	
			
		} else {		
		    if (view == null) {
		    	view = lInflater.inflate(R.layout.list_row_layout, parent, false);
		    }
		    
		    mMessage msg = data.get(position);
		    MessageService ser = ((MyApplication) context.getApplicationContext()).getActiveService();
		    boolean left = msg.sender.equals(ser.getContact( ser.getMyAddress() ));
		    
	    	TextView textLabel = (TextView) view.findViewById(R.id.author_text);
	    	textLabel.setText( msg.getSenderName() );
	        
	    	textLabel = (TextView) view.findViewById(R.id.msg_text);
	    	textLabel.setText( msg.text );
	    	
	    	textLabel.setBackgroundResource(left ? R.drawable.bubble_yellow : R.drawable.bubble_green);
	    	textLabel.setGravity(left ? Gravity.LEFT : Gravity.RIGHT);
	    	    	    	
	    	RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) textLabel.getLayoutParams();
	    	lp.addRule(left ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT);
	    	textLabel.setLayoutParams(lp);
	    	
	    	textLabel = (TextView) view.findViewById(R.id.msg_date);
	    	textLabel.setText( msg.sendTime.format("%d.%m.%Y %H:%M") );
	    	
	    	Log.d("MyAdapter", data.size() + " : " + position + " : " + msg.text);
	    	
		}
		
		return view;
	}

}
