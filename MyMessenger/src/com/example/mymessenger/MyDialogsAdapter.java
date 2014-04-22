package com.example.mymessenger;

import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
	List<mDialog> data;
	Context context;
	
	public MyDialogsAdapter(Context context, List<mDialog> showing_dialogs) {
	    lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    data = showing_dialogs;
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
		
	    if (view == null) {
	      view = lInflater.inflate(R.layout.dialogs_row_layout, parent, false);
	    }
	    
	    mDialog dlg = data.get(position);
	    
	    //boolean left = msg.sender == ((MyApplication) context.getApplicationContext()).getService( MessageService.SMS ).getMyName();
	    
	    MyApplication app = (MyApplication) ((Activity) context).getApplication();
	    
	    TextView textLabel = (TextView) view.findViewById(R.id.dlgview_dlgsername);
    	textLabel.setText( app.getService( dlg.getMsgService() ).getServiceName() );
	    
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgname);
    	textLabel.setText( dlg.getParticipantsNames() );
    	
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgdate);
    	textLabel.setText( dlg.getLastMessageTime().format("%H:%M %d.%m.%Y") );
        
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgtext);
    	textLabel.setText( dlg.snippet );
    	
    	ImageView iv = (ImageView) view.findViewById(R.id.dlgview_iconmain);
    	if(dlg.participants.get(0).icon_100 != null){	    	
	    	iv.setImageBitmap( dlg.participants.get(0).icon_100 );
    	} else if(dlg.participants.get(0).icon_100_url != null){
    		download_waiter tw = new download_waiter(dlg.participants.get(0).icon_100_url, "iv_cnt_icon_100", iv);
            app.dl_waiters.add(tw);
            
            tw = new download_waiter(dlg.participants.get(0).icon_100_url, "cnt_icon_100", dlg.participants.get(0));
            app.dl_waiters.add(tw);
            
            Intent intent = new Intent(context, DownloadService.class);
            intent.putExtra("url", dlg.participants.get(0).icon_100_url);
            context.getApplicationContext().startService(intent);
    	}
    	
    	//Log.d("MyDialogsAdapter", data.size() + " : " + position + " : " + dlg.getParticipantsNames());
		return view;
	}

}
