package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;
import com.example.mymessenger.services.MessageService;
import android.app.Application;

public class MyApplication extends Application {
	public List<MessageService> myServices;
	public int active_service;
	
	@Override
	public void onCreate() {
		super.onCreate();
		myServices = new ArrayList<MessageService>();
	}
	
	public void addService(MessageService mServive){
		myServices.add(mServive);
	}

	public MessageService getService(int typeId) {
		for(MessageService ms : myServices){
			if (ms.getType() == typeId ) return ms;
		}
		return null;
	}

	public MessageService getActiveService() {
		return getService(active_service);
	}
}
