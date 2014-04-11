package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityTwo extends ActionBarActivity implements OnClickListener {
	MyApplication app;
	MyAdapter msg_adapter;
	MyDialogsAdapter dlg_adapter;
	MyContactsAdapter cnt_adapter;
	
	List<mMessage> showing_messages;
	List<mDialog> showing_dialogs;
	List<mContact> showing_contacts;
	private ListView listview;
	
	private boolean dlg_maxed;
	private boolean dlg_isLoading;
	private boolean msg_maxed;
	private boolean msg_isLoading;
	private boolean cnt_maxed;
	private boolean cnt_isLoading;
	
	public int supposedFVI;
	private int async_complete_listener_msg_update_total_offset;
	
	public String mode;
	
	public final static String BROADCAST_ACTION = "ru.mymessage.servicebackbroadcast";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dlg_maxed = false;
		msg_maxed = false;
		msg_isLoading = false;
		
		app = (MyApplication) getApplicationContext();
		
		Intent intent = getIntent();	    
	    mode = intent.getStringExtra("mode");
	    
	    app.getActiveService().setContactDataChangedCallback(contact_data_changed);
	    
	    MessageService ms = app.getActiveService();
	    
	    if (mode.equals("messages")) {
	    	setContentView(R.layout.msg_list);
	    	listview = (ListView) findViewById(R.id.listview_object);
	    	((Button) findViewById(R.id.msg_sendbutton)).setOnClickListener(this);
			showing_messages = new ArrayList<mMessage>();
			
			msg_adapter = new MyAdapter(this, showing_messages);
			listview.setAdapter(msg_adapter);

			ms.requestMessages(ms.getActiveDialog(), 0, 20, async_complete_listener_msg);
			showing_messages.add(0, null);
			msg_isLoading = true;
			msg_adapter.isLoading = true;
			msg_adapter.notifyDataSetChanged();
			
	        listview.setOnItemClickListener(MsgClickListener);
	        listview.setOnScrollListener(MsgScrollListener);
	        
	        
	        supposedFVI = -1;
	        
	        setTitle(ms.getActiveDialog().getParticipantsNames());
	    }
	    
	    if (mode.equals("dialogs")) {
	    	setContentView(R.layout.listview_simple);
	    	listview = (ListView) findViewById(R.id.listview_object);
	    	
	    	showing_dialogs = new ArrayList<mDialog>();
			
			dlg_adapter = new MyDialogsAdapter(this, showing_dialogs);
			listview.setAdapter(dlg_adapter);

			ms.requestDialogs(0, 20, async_complete_listener_dlg);
			
	        listview.setOnItemClickListener(DlgClickListener);
	        listview.setOnScrollListener(DlgScrollListener);
	    }
	    
	    if (mode.equals("contacts")) {
	    	setContentView(R.layout.listview_simple);
	    	listview = (ListView) findViewById(R.id.listview_object);
	    	
	    	showing_contacts = new ArrayList<mContact>();
			
			cnt_adapter = new MyContactsAdapter(this, showing_contacts);
			listview.setAdapter(cnt_adapter);

			ms.requestContacts(0, 20, async_complete_listener_cnt);
			
	        listview.setOnItemClickListener(CntClickListener);
	        listview.setOnScrollListener(CntScrollListener);
	    }
	    
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setHomeButtonEnabled(true);
	}
	
	
	OnItemClickListener DlgClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mDialog dlg = showing_dialogs.get(position);
			((MyApplication) getApplicationContext()).getService( ((MyApplication) getApplicationContext()).active_service ).setActiveDialog(dlg);
			Intent intent = new Intent(ActivityTwo.this, ActivityTwo.class);
			intent.putExtra("mode", "messages");
			startActivity(intent);
		}
		
	};
	
	
	OnScrollListener DlgScrollListener = new OnScrollListener(){

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if ( !dlg_maxed && ( (totalItemCount - (firstVisibleItem + visibleItemCount)) < 5 ) && !dlg_isLoading) {
				MessageService ms = app.getService( app.active_service );
				ms.requestDialogs(showing_dialogs.size(), 20, async_complete_listener_dlg);
				dlg_isLoading = true;
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	OnItemClickListener MsgClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		}
		
	};
	
	
	OnScrollListener MsgScrollListener = new OnScrollListener(){

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if(supposedFVI != -1){
				if(supposedFVI != firstVisibleItem){
					Log.d("MsgScrollListener", "Wrong firstVisibleItem!!");
					firstVisibleItem = supposedFVI;
				}
				supposedFVI = -1;
			}
			
			Log.d("MsgScrollListener", String.valueOf(firstVisibleItem) + ", " + String.valueOf(listview.getFirstVisiblePosition()));
			if (visibleItemCount == 0) return;
			if ( !msg_maxed && ( firstVisibleItem == 0 ) && !msg_isLoading ) {
				
				msg_isLoading = true;

				MessageService ms = app.getActiveService();
				ms.requestMessages(ms.getActiveDialog(), showing_messages.size(), 20, async_complete_listener_msg);
				showing_messages.add(0, null);
				msg_adapter.isLoading = true;
				msg_adapter.notifyDataSetChanged();
				listview.setSelectionFromTop(firstVisibleItem  + 1, listview.getChildAt(firstVisibleItem).getTop());
				//Log.d("MsgScrollListener", String.valueOf(firstVisibleItem + lmsgs.size()) + ", " + String.valueOf(listview.getChildAt(firstVisibleItem).getTop()));
				
				//supposedFVI = firstVisibleItem + lmsgs.size();
				//Log.d("MsgScrollListener", String.valueOf(firstVisibleItem + lmsgs.size()) + ", " + String.valueOf(listview.getChildAt(firstVisibleItem).getTop()));
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			
		}
		
	};

	
	
	OnItemClickListener CntClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mContact cnt = showing_contacts.get(position);
			MessageService ms = ((MyApplication) getApplicationContext()).getService( ((MyApplication) getApplicationContext()).active_service );
			mDialog dlg = new mDialog();
			dlg.participants.add(cnt);
			ms.setActiveDialog(dlg);
			Intent intent = new Intent(ActivityTwo.this, ActivityTwo.class);
			intent.putExtra("mode", "messages");
			startActivity(intent);
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
	
	
	
	
	
	AsyncTaskCompleteListener<List<mMessage>> async_complete_listener_msg = new AsyncTaskCompleteListener<List<mMessage>>(){

		@Override
		public void onTaskComplete(List<mMessage> result) {
			showing_messages.remove(0);
			int s = showing_messages.size();
			for(mMessage msg : result){
	        	showing_messages.add(0, msg);
	        }
			msg_adapter.notifyDataSetChanged();
			if( (showing_messages.size() - s) == 0 )msg_maxed = true;
			
			int firstVisibleItem = listview.getFirstVisiblePosition();

			if(listview.getLastVisiblePosition() > 0)
				listview.setSelectionFromTop(firstVisibleItem  + result.size(), listview.getChildAt(1).getTop()); //listView.getChildAt(i) works where 0 is the very first visible row and (n-1) is the last visible row (where n is the number of visible views you see).

			msg_isLoading = false;
			msg_adapter.isLoading = false;			
		}
		
	};
	
	
	AsyncTaskCompleteListener<List<mDialog>> async_complete_listener_dlg = new AsyncTaskCompleteListener<List<mDialog>>(){

		@Override
		public void onTaskComplete(List<mDialog> result) {

			int s = showing_dialogs.size();
			for(mDialog dlg : result){
	        	showing_dialogs.add(dlg);
	        }
			dlg_adapter.notifyDataSetChanged();
			if( (showing_dialogs.size() - s) == 0 )dlg_maxed = true;
			
			dlg_isLoading = false;
		
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
				app.getActiveService().requestMessages(app.getActiveService().getActiveDialog(), async_complete_listener_msg_update_total_offset, 20, async_complete_listener_msg_update);
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
		switch (view.getId()){
		case R.id.msg_sendbutton :
			EditText textLabel = (EditText) findViewById(R.id.msg_entertext);
			String text = textLabel.getText().toString();
			textLabel.setText("");
			
			InputMethodManager inputManager = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE); 
			inputManager.hideSoftInputFromWindow(
			        this.getCurrentFocus().getWindowToken(),
			        InputMethodManager.HIDE_NOT_ALWAYS); 
			
			Log.d("ActivityTwo.onClick.msg_sendbutton", text);
			MessageService ms = app.getActiveService();
			mDialog dlg = ms.getActiveDialog();
			
			for(mContact cnt : dlg.participants){
				ms.sendMessage(cnt.address, text);
			}
			
			ms.requestMessages(dlg, 0, 1, async_complete_listener_msg);

			break;
		}
			
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
			ms.requestMessages(ms.getActiveDialog(), 0, 20, async_complete_listener_msg_update);
		}

	}

	@Override
	protected void onResume() { 
		super.onResume();
		app.setCurrentActivity(this);
	} 
	
	@Override
    protected void onPause() {
    	super.onPause();
    	app.setCurrentActivity(null);
    }
	
}
