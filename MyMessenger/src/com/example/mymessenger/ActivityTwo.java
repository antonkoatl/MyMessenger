package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityTwo extends Activity {
	MyApplication app;
	MyAdapter adapter;
	List<mMessage> showing_messages;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.two);
		
		app = (MyApplication) getApplicationContext();
		
		ListView listview_msgs = (ListView) findViewById(R.id.listview_msgs);
		showing_messages = new ArrayList<mMessage>();
		
		adapter = new MyAdapter(this, showing_messages);
		listview_msgs.setAdapter(adapter);
		
		MessageService ms = app.getService( app.active_service );
		
        for(mMessage msg : ms.getMessages(ms.getActiveDialog(), 0, 20)){
        	showing_messages.add(msg);
        }
        
        
        
        adapter.notifyDataSetChanged();
        listview_msgs.invalidateViews();
	}
}
