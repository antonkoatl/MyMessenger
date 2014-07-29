package com.example.mymessenger;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.example.mymessenger.services.MessageService.msInterfaceMS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class UpdateService extends Service {
	Map<Integer, RunnableAdvanced<?>> runnables;
    Map<Integer, Future<?>> futures;
    private ExecutorService executor;

    class async_complete_listener_runnable implements AsyncTaskCompleteListener<RunnableAdvanced<?>> {
        int service_type;

        @Override
        public void onTaskComplete(RunnableAdvanced<?> result) {
            Log.d("UpdateService", "posted " + String.valueOf(result));
            runnables.put(service_type, result);
            futures.put(service_type, executor.submit(result) );
        }

        public AsyncTaskCompleteListener<RunnableAdvanced<?>> setServiceType(int service_type){
            this.service_type = service_type;
            return this;
        }

    }

    public void onCreate() {
		super.onCreate();
		runnables = new HashMap<Integer, RunnableAdvanced<?>>();
        futures = new HashMap<Integer, Future<?>>();

        executor = Executors.newCachedThreadPool();
	}
  
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int spec_ser = -1;
        boolean remove = false;

        if(intent != null){
            spec_ser = intent.getIntExtra("specific_service", -1);
            remove = intent.getBooleanExtra("remove", false);
        }

		if(remove && spec_ser != -1){
			RunnableAdvanced<?> r = runnables.get(spec_ser);
            Future<?> f = futures.get(spec_ser);
			if(r != null && f != null){
                f.cancel(true);
                futures.remove(spec_ser);
            }
			return START_STICKY;
		} else {
			for(msInterfaceMS i : ( (MyApplication) getApplication() ).msManager.myMsgServices){
                if(runnables.get(i.getServiceType()) == null) {
                    Log.d("UpdateService", "requested");
                    async_complete_listener_runnable async_complete_listener_runnable_t = new async_complete_listener_runnable();
                    async_complete_listener_runnable_t.setServiceType(i.getServiceType());
                    i.requestNewMessagesRunnable(async_complete_listener_runnable_t);
                } else if (futures.get(i.getServiceType()) == null){
                    futures.put(i.getServiceType(), executor.submit(runnables.get(i.getServiceType())) );
                }
			}
		}
		
		return START_STICKY;
	}



	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	
}
