package com.example.mymessenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.services.Sms;
import com.example.mymessenger.services.Vk;
import com.example.mymessenger.ui.ListViewSimpleFragment;
import com.example.mymessenger.ui.ServicesMenuFragment;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

public class MyApplication extends Application {
	public static Context context;
	public List<MessageService> myMsgServices;
	public int active_service;
	PendingIntent pi;
	public SharedPreferences sPref;
	
	public List<AsyncTaskCompleteListener<Void>> cnts_updaters;
	public List<AsyncTaskCompleteListener<List<mDialog>>> dlgs_updaters;
	public List<AsyncTaskCompleteListener<List<mMessage>>> msgs_updaters;
	
	public List<download_waiter> dl_waiters;
	public DBHelper dbHelper;
	
	public boolean msgs_loading_maxed = false;
	public boolean dlgs_loading_maxed = false;
	
	private static Activity mMainActivity = null;
	
	static HandlerThread thread1 = new HandlerThread("MsgListHandlerThread");
	static Handler handler1 = null;
	
	public int active_user_action = 0;
	public static final int UA_SERVICES_MENU = 1;
	public static final int UA_DLGS_LIST = 2;
	public static final int UA_MSGS_LIST = 3;
	
	public void setUA(int action){
		active_user_action = action;
	}
	
	public int getUA(){
		return active_user_action;
	}
	
	static{
		thread1.start();
	    handler1 = new Handler(thread1.getLooper());
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (context == null) {
            context = getApplicationContext();
		}
		
		dbHelper = new DBHelper(this); //Класс для работы с бд
		myMsgServices = new ArrayList<MessageService>(); //Активные сервисы сообщений
		cnts_updaters = new ArrayList<AsyncTaskCompleteListener<Void>>(); //Обработчики обвновлений контактных данных
		dlgs_updaters = new ArrayList<AsyncTaskCompleteListener<List<mDialog>>>(); //Обработчики обвновлений диалогов
		msgs_updaters = new ArrayList<AsyncTaskCompleteListener<List<mMessage>>>(); //Обработчики обвновлений сообщений
		
		dl_waiters = new ArrayList<download_waiter>(); //Обработчики завершения загрузок
		
		sPref = getSharedPreferences("MyPref", MODE_PRIVATE); //загрузка конфигов
		
		//загрузка сервисов
        String using_services[] = sPref.getString("usingservices", "10").split(",");
        for(String i : using_services){        	
        	if(i.equals( String.valueOf(MessageService.SMS) ))
        		addMsgService(new Sms(this));
        	if(i.equals( String.valueOf(MessageService.VK) ))
        		addMsgService(new Vk(this));
        }
        
        active_service = sPref.getInt("active_service", 0);
        if(active_service != 0 && getService(active_service) != null){
        	MessageService ms = getService(active_service);
        	String dialog_addr = sPref.getString("active_dialog", "");
        	if(dialog_addr.length() > 0){
        		mDialog dlg = new mDialog();
        		dlg.participants.add(ms.getContact(dialog_addr));
        		ms.setActiveDialog(dlg);
        	}
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
	
	
	
    public static Activity getMainActivity(){
          return mMainActivity;
    }
    
    public static void setMainActivity(Activity mActivity){
    	mMainActivity = mActivity;
    }


	public void requestLastDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
		for(MessageService msg : myMsgServices){
			msg.requestDialogs(count, offset, cb);
		}
	}

	public void setActiveService(int msgService) {
		active_service = msgService;
		sPref.edit().putInt("active_service", active_service).commit();		
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
		if(getMainActivity() != null){
			getMainActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 for(AsyncTaskCompleteListener<Void> updater : cnts_updaters)
			 			updater.onTaskComplete(null);
			    }
			});
		}		
	}
	
	public void triggerDlgsUpdaters(final List<mDialog> dlgs){
		if(getMainActivity() != null){
			getMainActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 for(AsyncTaskCompleteListener<List<mDialog>> updater : dlgs_updaters)
			 			updater.onTaskComplete(dlgs);
			    }
			});
		}		
	}
		
	public void triggerMsgsUpdaters(final List<mMessage> msgs){
		
		if(getMainActivity() != null){
			getMainActivity().runOnUiThread(new Runnable() {
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
			ms.requestDialogs(20, 0, async_complete_listener_dlg);
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
			
			AsyncTaskCompleteListener<MessageService> asms = new AsyncTaskCompleteListener<MessageService>(){

				@Override
				public void onTaskComplete(MessageService ms) {
					ms.init();
					
					String usingservices = "";
					for(MessageService mst : myMsgServices){
						usingservices += String.valueOf(mst.getServiceType()) + ",";
					}
					usingservices += String.valueOf(ms.getServiceType());
					
					Editor ed = sPref.edit();
			    	ed.putString("usingservices", usingservices);
			    	ed.commit();
			    	
			    	addMsgService(ms);
			    	
			    	ServicesMenuFragment fr = (ServicesMenuFragment) ((MainActivity) getMainActivity()).pagerAdapter.getRegisteredFragment(0);				
					fr.POSITION = FragmentPagerAdapter.POSITION_NONE;
					
					ListViewSimpleFragment fr2 = (ListViewSimpleFragment) ((MainActivity) getMainActivity()).pagerAdapter.getRegisteredFragment(1);			
					fr2.POSITION = FragmentPagerAdapter.POSITION_NONE;
					
					((MainActivity) getMainActivity()).pagerAdapter.notifyDataSetChanged();
				}
				
			};
			
			ms.setup(asms);
			
			
			
			
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

	public void refreshtDialogsFromNet(AsyncTaskCompleteListener<List<mDialog>> cb, int count) {
		for(MessageService msg : myMsgServices){
			msg.refreshDialogsFromNet(cb, count);
		}
	}

	public List<mMessage> update_db_msgs(List<mMessage> result, MessageService ms, mDialog dlg) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		String my_table_name = dbHelper.getTableNameMsgs(ms);
		List<mMessage> msgs = new ArrayList<mMessage>();
		
		for (mMessage msg : result) {
			if( update_db_msg(msg, dlg) ) msgs.add(msg);
		}
		
		return msgs;		
	}
	
	public mDialog update_db_dlg(mMessage msg){
		int dlg_key = dbHelper.getDlgId(msg.respondent.address, getService(msg.msg_service));
		mDialog dlg = dbHelper.getDlgById(dlg_key, getService(msg.msg_service));
		if(dlg.last_msg_time.before(msg.sendTime)){
			dlg.last_msg_time.set(msg.sendTime);
			dlg.snippet = msg.text;
			dlg.snippet_out = msg.getFlag(mMessage.OUT) ? 1 : 0;
			dbHelper.updateDlg(dlg_key, dlg, getService(msg.msg_service));
		}
		return dlg;
	}
	
	public boolean update_db_msg(mMessage msg, mDialog dlg) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		MessageService ms = getService(msg.msg_service);
		int dlg_key = dbHelper.getDlgId(dlg, ms);
		String my_table_name = dbHelper.getTableNameMsgs(ms);
		String selection = DBHelper.colDlgkey + " = ? AND " + DBHelper.colSendtime + " = ? AND " + DBHelper.colBody + " = ?";
		String[] selectionArgs = { String.valueOf(dlg_key), String.valueOf(msg.sendTime.toMillis(false)), msg.text };
		Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);

		if(c.moveToFirst()){
			int  flags_in_db = c.getInt( c.getColumnIndex(DBHelper.colFlags) );
			
			if(msg.flags != flags_in_db){
				//update
				int id = c.getInt(c.getColumnIndex(DBHelper.colId));
				c.close();
				dbHelper.updateMsg(id, msg, ms);							    				
				return true;
			} else {
				//not update
				c.close();
				return false;
			}
		} else {
			//add
			c.close();			
			dbHelper.insertMsg(msg, my_table_name, dlg_key);			    				
			return true;
		}
	}
	
	
}
