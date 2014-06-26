package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyDialogsAdapter extends BaseAdapter {
	LayoutInflater lInflater;
	List<DlgListItem> data;
	Context context;
	
	public MyDialogsAdapter(Context context, List<mDialog> showing_dialogs) {
	    lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    data = new ArrayList<DlgListItem>();
	    
	    if(showing_dialogs != null){
		    for(mDialog dlg : showing_dialogs)
		    	data.add( new DlgListItem(dlg, ((MyApplication) context.getApplicationContext())) );
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
			view = lInflater.inflate(R.layout.dialogs_row_layout, parent, false);
		}
		
		DlgListItem dli = data.get(position);
		
		dli.setupView(view);
		
		return view;
	}
	
	public int indexOf(mDialog dlg) {
		return data.indexOf(dlg);
	}

	public void add(int i, mDialog dlg) {
		data.add(i, new DlgListItem(dlg, ((MyApplication) context.getApplicationContext()) ));
		notifyDataSetChanged();	
	}

	public void add(mDialog msg) {
		data.add(new DlgListItem(msg, ((MyApplication) context.getApplicationContext()) ));
		notifyDataSetChanged();	
	}

	public mDialog remove(int tind) {
		mDialog t = data.remove(tind).dlg; 
		notifyDataSetChanged();
		return t;			
	}

}
