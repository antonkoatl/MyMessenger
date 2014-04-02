package com.example.mymessenger;

import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

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
			i.requestNewMessagesRunnable(async_complete_listener_runnable);
		}
		
	}

	AsyncTaskCompleteListener<Runnable> async_complete_listener_runnable = new AsyncTaskCompleteListener<Runnable>(){

		@Override
		public void onTaskComplete(Runnable result) {
			handler.post(result);
		}
		
	};
	
}
