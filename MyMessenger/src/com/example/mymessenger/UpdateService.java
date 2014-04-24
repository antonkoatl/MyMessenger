package com.example.mymessenger;

import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class UpdateService extends Service {
	Handler handler;
	HandlerThread hthread;
	
	public void onCreate() {
		super.onCreate();
		
		HandlerThread thread = new HandlerThread("UpdateServiceHandlerThread");
		thread.start();
		handler = new Handler(thread.getLooper());
	}
  
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int ss = intent.getIntExtra("specific_service", -1);
		
		if(ss == -1){
			for(MessageService i : ( (MyApplication) getApplication() ).myMsgServices){
				Log.d("UpdateService", "requested");
				i.requestNewMessagesRunnable(async_complete_listener_runnable);
			}
		} else {
			( (MyApplication) getApplication() ).getService(ss).requestNewMessagesRunnable(async_complete_listener_runnable);
		}
		
		return START_STICKY;
	}

	AsyncTaskCompleteListener<Runnable> async_complete_listener_runnable = new AsyncTaskCompleteListener<Runnable>(){

		@Override
		public void onTaskComplete(Runnable result) {
			Log.d("UpdateService", "posted " + result.toString());
			handler.post(result);
		}
		
	};

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
}
