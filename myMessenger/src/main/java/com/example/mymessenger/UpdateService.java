package com.example.mymessenger;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.example.mymessenger.services.MessageService.MessageService;

import java.util.HashMap;
import java.util.Map;

public class UpdateService extends Service {
	Handler handler;
	HandlerThread hthread;
	
	Map<Integer, RunnableAdvanced<?>> runnables;
	
	public void onCreate() {
		super.onCreate();
		runnables = new HashMap<Integer, RunnableAdvanced<?>>();
		
		HandlerThread thread = new HandlerThread("UpdateServiceHandlerThread");
		thread.start();
		handler = new Handler(thread.getLooper());
	}
  
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int ss = intent.getIntExtra("specific_service", -1);
		
		boolean remove = intent.getBooleanExtra("remove", false);
		
		if(remove){
			RunnableAdvanced<?> r = runnables.get(ss);
			if(r != null)r.kill();
			return START_REDELIVER_INTENT;
		}
		
		if(ss == -1){
			for(MessageService i : ( (MyApplication) getApplication() ).myMsgServices){
				Log.d("UpdateService", "requested");
				AsyncTaskCompleteListener<RunnableAdvanced<?>> async_complete_listener_runnable_t = new AsyncTaskCompleteListener<RunnableAdvanced<?>>(){
					int service_type;

					@Override
					public void onTaskComplete(RunnableAdvanced<?> result) {
						Log.d("UpdateService", "posted " + String.valueOf(result));
						runnables.put(service_type, result);
						handler.post(result);
					}
					
					public AsyncTaskCompleteListener<RunnableAdvanced<?>> setServiceType(int service_type){
						this.service_type = service_type;
						return this;
					}
					
				}.setServiceType(i.getServiceType());
				i.requestNewMessagesRunnable(async_complete_listener_runnable_t);
			}
		} else {
			AsyncTaskCompleteListener<RunnableAdvanced<?>> async_complete_listener_runnable_t = new AsyncTaskCompleteListener<RunnableAdvanced<?>>(){
				int service_type;

				@Override
				public void onTaskComplete(RunnableAdvanced<?> result) {
					Log.d("UpdateService", "posted " + String.valueOf(result));
					runnables.put(service_type, result);
					handler.post(result);
				}
				
				public AsyncTaskCompleteListener<RunnableAdvanced<?>> setServiceType(int service_type){
					this.service_type = service_type;
					return this;
				}
				
			}.setServiceType(ss);
			
			( (MyApplication) getApplication() ).getService(ss).requestNewMessagesRunnable(async_complete_listener_runnable_t);
		}
		
		return START_REDELIVER_INTENT;
	}



	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	
}
