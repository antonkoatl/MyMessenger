package com.example.mymessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MsgReceiver extends BroadcastReceiver {
	public static final String ACTION_RECEIVE = "android.mymessenger.MSG_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {
		int service_type = intent.getIntExtra("service_type", 0);
		mMessage msg = (mMessage) intent.getParcelableExtra("msg");
		MyApplication app = (MyApplication) context.getApplicationContext();
		Toast.makeText(context, "New MSG! " + app.getService(service_type).getServiceName() + " " + msg.text, Toast.LENGTH_LONG).show();

		if(app.getCurrentActivity() != null && app.getCurrentActivity().getClass() == ActivityTwo.class){
			ActivityTwo ac = (ActivityTwo) app.getCurrentActivity(); 
			
			if(ac.mode.equals("dialogs")){
				
			}
			
			if(ac.mode.equals("messages")){
				
				if(app.getActiveService().getActiveDialog().participants.contains(msg.respondent)){
					ac.NewMessage(msg);
				}
			}
		}
	}

}
