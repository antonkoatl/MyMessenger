package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityTwo extends Activity implements AsyncTaskCompleteListener<List<mMessage>>, OnClickListener {
	MyApplication app;
	MyAdapter msg_adapter;
	MyDialogsAdapter dlg_adapter;
	List<mMessage> showing_messages;
	List<mDialog> showing_dialogs;
	private ListView listview;
	
	private boolean dlg_maxed;
	private boolean msg_maxed;
	private boolean msg_isLoading;
	
	public int supposedFVI;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dlg_maxed = false;
		msg_maxed = false;
		msg_isLoading = false;
		
		app = (MyApplication) getApplicationContext();
		
		Intent intent = getIntent();	    
	    String mode = intent.getStringExtra("mode");
	    
	    
	    if (mode.equals("messages")) {
	    	setContentView(R.layout.msg_list);
	    	listview = (ListView) findViewById(R.id.msg_listview);
	    	((Button) findViewById(R.id.msg_sendbutton)).setOnClickListener(this);
			showing_messages = new ArrayList<mMessage>();
			
			msg_adapter = new MyAdapter(this, showing_messages);
			listview.setAdapter(msg_adapter);
			
			MessageService ms = app.getService( app.active_service );
			
	        for(mMessage msg : ms.getMessages(ms.getActiveDialog(), 0, 20)){
	        	showing_messages.add(0, msg);
	        }
	        
	        msg_adapter.notifyDataSetChanged();
	        listview.invalidateViews();

	        listview.setOnItemClickListener(MsgClickListener);
	        listview.setOnScrollListener(MsgScrollListener);
	        
	        
	        supposedFVI = -1;
	    }
	    
	    if (mode.equals("dialogs")) {
	    	setContentView(R.layout.two);
	    	listview = (ListView) findViewById(R.id.msg_listview);
	    	
			showing_dialogs = new ArrayList<mDialog>();
			
			dlg_adapter = new MyDialogsAdapter(this, showing_dialogs);
			listview.setAdapter(dlg_adapter);
			
			MessageService ms = app.getService( app.active_service );
			
	        for(mDialog dlg : ms.getDialogs(0, 20)){
	        	showing_dialogs.add(dlg);
	        }
	        
	        dlg_adapter.notifyDataSetChanged();
	        listview.invalidateViews();
	        
	        listview.setOnItemClickListener(DlgClickListener);
	        listview.setOnScrollListener(DlgScrollListener);
	    }
	    
	    
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
			if ( !dlg_maxed && ( (totalItemCount - (firstVisibleItem + visibleItemCount)) < 5 ) ) {
				MessageService ms = app.getService( app.active_service );
				int s = showing_dialogs.size();
				for(mDialog dlg : ms.getDialogs(showing_dialogs.size(), 20)){
		        	showing_dialogs.add(dlg);
		        }
				if( (showing_dialogs.size() - s) == 0 )dlg_maxed = true;
				dlg_adapter.notifyDataSetChanged();
				//listview.invalidateViews();
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
				new load_msgs_async(ActivityTwo.this, ActivityTwo.this).execute(null);
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
		listview.setSelectionFromTop(firstVisibleItem  + result.size(), listview.getChildAt(firstVisibleItem + 1).getTop());
		
		msg_isLoading = false;
		msg_adapter.isLoading = false;
	}
	
	
	class load_msgs_async extends AsyncTask<String, Void, List<mMessage>> {
	    private AsyncTaskCompleteListener<List<mMessage>> callback;
		private Context context;

	    public load_msgs_async(Context context, AsyncTaskCompleteListener<List<mMessage>> cb) {
	        this.context = context;
	        this.callback = cb;
	    }

	    protected void onPostExecute(List<mMessage> result) {
	       callback.onTaskComplete(result);
	   }

		@Override
		protected List<mMessage> doInBackground(String... params) {
			MessageService ms = app.getService( app.active_service );
			return ms.getMessages(ms.getActiveDialog(), showing_messages.size(), 20);
		}  
	}


	@Override
	public void onClick(View view) {
		switch (view.getId()){
		case R.id.msg_sendbutton :
			EditText textLabel = (EditText) findViewById(R.id.msg_entertext);
			String text = textLabel.getText().toString();
			Log.d("ActivityTwo.onClick.msg_sendbutton", text);
			MessageService ms = app.getService( app.active_service );
			mDialog dlg = ms.getActiveDialog();
			
			for(String addr : dlg.participants){
				ms.sendMessage(addr, text);
			}
			break;
		}
			
	}
}
