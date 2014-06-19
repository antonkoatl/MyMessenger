package com.example.mymessenger;

import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
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
    	textLabel.setText( app.getService( dlg.getMsgServiceType() ).getServiceName() );
	    
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgname);
    	textLabel.setText( dlg.getParticipantsNames() );
    	
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgdate);
    	textLabel.setText( dlg.getLastMessageTime().format("%H:%M %d.%m.%Y") );
        
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgtext);
    	textLabel.setText( ChatMessageFormatter.getSmiledText(context, dlg.snippet, dlg.getMsgServiceType(), textLabel.getLineHeight()) );
    	
    	ImageView iv = (ImageView) view.findViewById(R.id.dlgview_iconmain);
    	if(dlg.participants.get(0).icon_100 != null){	    	
	    	iv.setImageBitmap( dlg.participants.get(0).icon_100 );
    	} else if(dlg.participants.get(0).icon_100_url != null){
    		download_waiter tw = new download_waiter(dlg.participants.get(0).icon_100_url){
    			ImageView iv;
    			mContact cnt;
    			
				@Override
				public void onDownloadComplete() {
					BitmapFactory.Options options = new BitmapFactory.Options();
					//options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					cnt.icon_100 = BitmapFactory.decodeFile(filepath);
					iv.setImageBitmap( cnt.icon_100 );			
				}
				
				public download_waiter setParams(ImageView iv, mContact cnt){
					this.iv = iv;
					this.cnt = cnt;
					return this;
				}
				
    			
    		}.setParams(iv, dlg.participants.get(0));
    		
            app.dl_waiters.add(tw);
                       
            Intent intent = new Intent(context, DownloadService.class);
            intent.putExtra("url", dlg.participants.get(0).icon_100_url);
            context.getApplicationContext().startService(intent);
    	}
	
		iv = (ImageView) view.findViewById(R.id.dlgview_dlgtexticon);
	    if(dlg.snippet_out == 1){
	    	mContact my_contact = app.getService(dlg.msg_service_type).getMyContact();
	    	if(my_contact.icon_100 != null){	    	
	    		iv.setImageBitmap( my_contact.icon_100 );
	     	} else if(my_contact.icon_100_url != null){
	     		download_waiter tw = new download_waiter(my_contact.icon_100_url){
	     			ImageView iv;
	     			mContact cnt;
	     			
	 				@Override
	 				public void onDownloadComplete() {
	 					BitmapFactory.Options options = new BitmapFactory.Options();
	 					//options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	 					cnt.icon_100 = BitmapFactory.decodeFile(filepath);
	 					iv.setImageBitmap( cnt.icon_100 );			
	 				}
	 				
	 				public download_waiter setParams(ImageView iv, mContact cnt){
	 					this.iv = iv;
	 					this.cnt = cnt;
	 					return this;
	 				}
	 				
	     			
	     		}.setParams(iv, my_contact);
	     		
	             app.dl_waiters.add(tw);
	             
	             Intent intent = new Intent(context, DownloadService.class);
	             intent.putExtra("url", my_contact.icon_100_url);
	             context.getApplicationContext().startService(intent);
	     	} 
	    } else {
     		iv.setVisibility(View.INVISIBLE);
     		iv.getLayoutParams().width = 0;
     		iv.requestLayout();
        }
    	
    	//Log.d("MyDialogsAdapter", data.size() + " : " + position + " : " + dlg.getParticipantsNames());
		return view;
	}

}
