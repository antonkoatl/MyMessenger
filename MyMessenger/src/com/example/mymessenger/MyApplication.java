package com.example.mymessenger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.services.Sms;
import com.example.mymessenger.services.Vk;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.HandlerThread;

public class MyApplication extends Application {
	public List<MessageService> myMsgServices;
	public int active_service;
	PendingIntent pi;
	private SharedPreferences sPref;
	
	public List<AsyncTaskCompleteListener<Void>> cnts_updaters;
	public List<AsyncTaskCompleteListener<List<mDialog>>> dlgs_updaters;
	public List<AsyncTaskCompleteListener<List<mMessage>>> msgs_updaters;
	
	public List<download_waiter> dl_waiters;
	public DBHelper dbHelper;
	
	public boolean msgs_loading_maxed = false;
	public boolean dlgs_loading_maxed = false;
	
	private Activity mCurrentActivity = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		dbHelper = new DBHelper(this); //Класс для работы с бд
		myMsgServices = new ArrayList<MessageService>(); //Активные сервисы сообщений
		cnts_updaters = new ArrayList<AsyncTaskCompleteListener<Void>>(); //Обработчики обвновлений контактных данных
		dlgs_updaters = new ArrayList<AsyncTaskCompleteListener<List<mDialog>>>(); //Обработчики обвновлений диалогов
		msgs_updaters = new ArrayList<AsyncTaskCompleteListener<List<mMessage>>>(); //Обработчики обвновлений сообщений
		
		dl_waiters = new ArrayList<download_waiter>(); //Обработчики завершения загрузок
		
		sPref = getSharedPreferences("MyPref", MODE_PRIVATE); //загрузка конфигов
		
		//загрузка активных сервисов
        String using_services[] = sPref.getString("usingservices", "10").split(",");
        for(String i : using_services){        	
        	if(i.equals( String.valueOf(MessageService.SMS) ))
        		addMsgService(new Sms(this));
        	if(i.equals( String.valueOf(MessageService.VK) ))
        		addMsgService(new Vk(this));
        }
        
        //Запуск сервиса обновлений        
        Intent intent1 = new Intent(this, UpdateService.class);
		startService(intent1);
	}
	
	public void addMsgService(MessageService mServive){
		myMsgServices.add(mServive);
	}

	public MessageService getService(int typeId) {
		for(MessageService ms : myMsgServices){
			if (ms.getServiceType() == typeId ) return ms;
		}
		return null;
	}

	public MessageService getActiveService() {
		return getService(active_service);
	}

	public boolean isServisesLoaded() {
		return myMsgServices.size() > 0;
	}
	
	
	
    public Activity getCurrentActivity(){
          return mCurrentActivity;
    }
    public void setCurrentActivity(Activity mCurrentActivity){
          this.mCurrentActivity = mCurrentActivity;
    }

	public void requestLastDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
		for(MessageService msg : myMsgServices){
			msg.requestDialogs(offset, count, cb);
		}
	}

	public void setActiveService(int msgService) {
		active_service = msgService;
	}

	
	public void registerCntsUpdater(AsyncTaskCompleteListener<Void> updater){
		if(!cnts_updaters.contains(updater))cnts_updaters.add(updater);
	}
	
	public void registerDlgsUpdater(AsyncTaskCompleteListener<List<mDialog>> updater){
		if(!dlgs_updaters.contains(updater))dlgs_updaters.add(updater);
	}
	
	public void registerMsgsUpdater(AsyncTaskCompleteListener<List<mMessage>> updater){
		if(!msgs_updaters.contains(updater))msgs_updaters.add(updater);
	}
	
	public void unregisterCntsUpdater(AsyncTaskCompleteListener<Void> updater){
		cnts_updaters.remove(updater);
	}
	
	public void unregisterDlgsUpdater(AsyncTaskCompleteListener<List<mDialog>> updater){
		dlgs_updaters.remove(updater);
	}
	
	public void unregisterMsgsUpdater(AsyncTaskCompleteListener<List<mMessage>> updater){
		msgs_updaters.remove(updater);
	}
	
	public void triggerCntsUpdaters(){
		if(getCurrentActivity() != null){
			getCurrentActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 for(AsyncTaskCompleteListener<Void> updater : cnts_updaters)
			 			updater.onTaskComplete(null);
			    }
			});
		}		
	}
	
	public void triggerDlgsUpdaters(final List<mDialog> dlgs){
		if(getCurrentActivity() != null){
			getCurrentActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 for(AsyncTaskCompleteListener<List<mDialog>> updater : dlgs_updaters)
			 			updater.onTaskComplete(dlgs);
			    }
			});
		}		
	}
	
	public void triggerMsgsUpdaters(final List<mMessage> msgs){
		if(getCurrentActivity() != null){
			getCurrentActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 for(AsyncTaskCompleteListener<List<mMessage>> updater : msgs_updaters)
			 			updater.onTaskComplete(msgs);
			    }
			});
		}		
	}

	public List<download_waiter> getDownloadWaiters(String url_path) {
		List<download_waiter> dws = new ArrayList<download_waiter>();

		Iterator<download_waiter> it = dl_waiters.iterator();
		while (it.hasNext()) {
			download_waiter dw = it.next();
			if(dw.url.equals(url_path)){
				dws.add(dw);
				it.remove();
			}
		}
		return dws;
	}

	public boolean isLoadingDlgs() {
		boolean res = false;
		for(MessageService ms : myMsgServices)if(ms.isLoadingDlgs())res = true;
		return res;
	}

	public void refreshServices(AsyncTaskCompleteListener<List<mDialog>> async_complete_listener_dlg) {
		for(MessageService ms : myMsgServices){
			ms.refresh();
			ms.requestDialogs(0, 20, async_complete_listener_dlg);
		}
	}

	public boolean newService(int service_type) {
		boolean isExist = false;
		
		for(MessageService ms : myMsgServices){
			if(ms.getServiceType() == service_type){
				isExist = true;
				break;
			}
		}
		
		if(!isExist){
			MessageService ms = null;
			switch(service_type){
			case MessageService.SMS: ms = new Sms(this);
			case MessageService.VK: ms = new Vk(this);
			}
			
			ms.setup();
			ms.init();
			
			String usingservices = "";
			for(MessageService mst : myMsgServices){
				usingservices += String.valueOf(mst.getServiceType()) + ",";
			}
			usingservices += String.valueOf(service_type);
			
			Editor ed = sPref.edit();
	    	ed.putString("usingservices", usingservices);
	    	ed.commit();
	    	
	    	addMsgService(ms);
			return true;
		} else return false;
	}
	
	public void deleteService(int service_type){
		MessageService ms = null;
		for(MessageService mst : myMsgServices){
			if(mst.getServiceType() == service_type){
				ms = mst;
				break;
			}
		}
		if(ms == null)return;
		
		ms.unsetup();	
		
		String usingservices = "";
		for(MessageService mst : myMsgServices){
			usingservices += String.valueOf(mst.getServiceType()) + ",";
		}
		usingservices = usingservices.replace(String.valueOf(service_type) + ",", "");
		usingservices = usingservices.substring(0, usingservices.length() - 1);
		
		Editor ed = sPref.edit();
    	ed.putString("usingservices", usingservices);
    	ed.commit();
    	
    	myMsgServices.remove(ms);
	}

	public void initServices() {
		for(MessageService ms : myMsgServices){
			ms.init();
		}
	}

	
	
}
