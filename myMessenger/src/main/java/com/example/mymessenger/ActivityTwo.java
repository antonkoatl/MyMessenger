package com.example.mymessenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockActivity;
import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.ui.PullToRefreshListView;

import java.util.ArrayList;
import java.util.List;

public class ActivityTwo extends SherlockActivity implements OnClickListener {
	public static final int REQUEST_CODE = 200;
	public static final int RESULT_SELECTED = 100;
	
	MyApplication app;
	MyMsgAdapter msg_adapter;
	MyDialogsAdapter dlg_adapter;
	MyContactsAdapter cnt_adapter;
	
	List<mMessage> showing_messages;
	List<mDialog> showing_dialogs;
	List<mContact> showing_contacts;
	private PullToRefreshListView listview;
	
	private boolean cnt_maxed;
	private boolean cnt_isLoading;
	
	public int supposedFVI;
	private int async_complete_listener_msg_update_total_offset;
	
	public String mode;
	public int msg_service;
	
	public final static String BROADCAST_ACTION = "ru.mymessage.servicebackbroadcast";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		app = (MyApplication) getApplicationContext();
		
		Intent intent = getIntent();	    
	    mode = intent.getStringExtra("mode");
	    msg_service = intent.getIntExtra("msg_service", 0);
	    
	    MessageService ms = app.getActiveService();
	    
	    
	    if (mode.equals("contacts")) {
	    	setContentView(R.layout.listview_simple);
	    	listview = (PullToRefreshListView) findViewById(R.id.listview_object);
	    	
	    	showing_contacts = new ArrayList<mContact>();
			
			cnt_adapter = new MyContactsAdapter(this, showing_contacts);
			listview.setAdapter(cnt_adapter);

			cnt_isLoading = true;
			ms.requestContacts(0, 20, async_complete_listener_cnt);
			
	        listview.setOnItemClickListener(CntClickListener);
	        listview.setOnScrollListener(CntScrollListener);
	    }
	    
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setHomeButtonEnabled(true);
	}
	
	


	
	
	OnItemClickListener CntClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mContact cnt = showing_contacts.get(position);
			Intent intent = new Intent();
			intent.putExtra("msg_service", msg_service);
			intent.putExtra("cnt", cnt.address);
			setResult(RESULT_SELECTED, intent);
			finish();
		}
		
	};
	
	
	OnScrollListener CntScrollListener = new OnScrollListener(){

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if ( !cnt_maxed && ( (totalItemCount - (firstVisibleItem + visibleItemCount)) < 5 ) && !cnt_isLoading) {
				MessageService ms = app.getService( app.active_service );
				ms.requestContacts(showing_contacts.size(), 100, async_complete_listener_cnt);
				cnt_isLoading = true;
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	
	AsyncTaskCompleteListener<List<mContact>> async_complete_listener_cnt = new AsyncTaskCompleteListener<List<mContact>>(){

		@Override
		public void onTaskComplete(List<mContact> result) {

			int s = showing_contacts.size();
			for(mContact cnt : result){
				showing_contacts.add(cnt);
	        }
			cnt_adapter.notifyDataSetChanged();
			if( (showing_contacts.size() - s) == 0 )cnt_maxed = true;
			
			cnt_isLoading = false;
		
		}
		
	};
	
	
	
	AsyncTaskCompleteListener<List<mMessage>> async_complete_listener_msg_update = new AsyncTaskCompleteListener<List<mMessage>>(){
		@Override
		public void onTaskComplete(List<mMessage> result) {
			boolean update = true;

			for(mMessage msg : result){
				if( msg.sendTime.after( showing_messages.get(showing_messages.size()-1).sendTime ) ){
	        		showing_messages.add(msg);
	        		async_complete_listener_msg_update_total_offset++;
	        		msg_adapter.notifyDataSetChanged();
	        	} else {
	        		update = false;
	        		break;
	        	}
	        }
			
			if(update){
				app.getActiveService().requestMessages(app.getActiveService().getActiveDialog(), 20, async_complete_listener_msg_update_total_offset, async_complete_listener_msg_update);
			}
		}
	};

	
	AsyncTaskCompleteListener<Void> contact_data_changed = new AsyncTaskCompleteListener<Void>(){
		@Override
		public void onTaskComplete(Void result) {
			if(msg_adapter != null)msg_adapter.notifyDataSetChanged();
			if(dlg_adapter != null)dlg_adapter.notifyDataSetChanged();
		}
		
	};
	
	@Override
	public void onClick(View view) {
			
	}

	public void NewMessage(mMessage msg){
		boolean toScroll = false;
		int firstVisibleItem = listview.getFirstVisiblePosition();
		if(listview.getChildAt(listview.getChildCount() - 1).getBottom() == listview.getHeight())
			toScroll = true;
		showing_messages.add(msg);
		msg_adapter.notifyDataSetChanged();
		if(toScroll)
			listview.setSelectionFromTop(firstVisibleItem  + 1, 0);

	}
	
	public void MsgUpdate(int service_type){
		MessageService ms = app.getService( service_type );
		
		if(service_type == app.getActiveService().getServiceType()){
			async_complete_listener_msg_update_total_offset = 0;
			ms.requestMessages(ms.getActiveDialog(), 20, 0, async_complete_listener_msg_update);
		}

	}

	@Override
	protected void onResume() { 
		super.onResume();
	} 
	
	@Override
    protected void onPause() {
    	super.onPause();
    }
	
}
