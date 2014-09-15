package com.example.mymessenger;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.example.mymessenger.services.MessageService.MessageService;
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

		if(spec_ser != -1){
            if(remove){
                RunnableAdvanced<?> r = runnables.get(spec_ser);
                Future<?> f = futures.get(spec_ser);
                if(r != null && f != null){
                    f.cancel(true);
                    futures.remove(spec_ser);
                }
                return START_STICKY;
            } else {
                getUpdateRunnable(((MyApplication) getApplication()).msManager.getService(spec_ser));
            }
        } else {
			for(msInterfaceMS ms : ( (MyApplication) getApplication() ).msManager.myMsgServices){
                getUpdateRunnable(ms);
			}
		}
		
		return START_STICKY;
	}

    private void getUpdateRunnable(msInterfaceMS ms){
        if(runnables.get(ms.getServiceType()) == null) {
            Log.d("UpdateService", "requested");
            async_complete_listener_runnable async_complete_listener_runnable_t = new async_complete_listener_runnable();
            async_complete_listener_runnable_t.setServiceType(ms.getServiceType());
            ms.requestNewMessagesRunnable(async_complete_listener_runnable_t);
        } else if (futures.get(ms.getServiceType()) == null){
            futures.put(ms.getServiceType(), executor.submit(runnables.get(ms.getServiceType())) );
            runnables.get(ms.getServiceType()).unstop();
        }
    }



	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	
}
