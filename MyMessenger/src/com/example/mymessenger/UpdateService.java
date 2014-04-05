package com.example.mymessenger;

import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class UpdateService extends IntentService {
	Handler handler;
	
	public UpdateService() {
		super("UpdateService");
	}

	public void onCreate() {
		super.onCreate();
	}
  
	@Override
	protected void onHandleIntent(Intent intent) {
		handler = new Handler();
		
		for(MessageService i : ( (MyApplication) getApplication() ).myMsgServices){
			Log.d("UpdateService", "requested");
			i.requestNewMessagesRunnable(async_complete_listener_runnable);
		}
		
	}

	AsyncTaskCompleteListener<Runnable> async_complete_listener_runnable = new AsyncTaskCompleteListener<Runnable>(){

		@Override
		public void onTaskComplete(Runnable result) {
			Log.d("UpdateService", "posted");
			handler.post(result);
		}
		
	};
	
}
