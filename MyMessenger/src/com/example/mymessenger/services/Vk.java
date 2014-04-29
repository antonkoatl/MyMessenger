package com.example.mymessenger.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.mymessenger.ActivityTwo;
import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.DBHelper;
import com.example.mymessenger.DownloadService;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MsgReceiver;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.UpdateService;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.Sms.load_msgs_async;
import com.example.mymessenger.download_waiter;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCaptchaDialog;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;

public class Vk extends MessageService {
	private static String sTokenKey = "VK_ACCESS_TOKEN";
	private static String[] sMyScope = new String[]{VKScope.FRIENDS, VKScope.WALL, VKScope.PHOTOS, VKScope.NOHTTPS, VKScope.MESSAGES};
	public static final int MSGS_DOWNLOAD_COUNT = 20;
	public static final int DLGS_DOWNLOAD_COUNT = 20;
	
	boolean accum_cnt_handler_isRunning = false; //??
	
	boolean finished; //??
	boolean handling; //??
	
	private boolean authorization_finished = true;
	
	private AsyncTaskCompleteListener<List<mMessage>> requestMessagesCallback; //??
	private AsyncTaskCompleteListener<List<mDialog>> requestDialogsCallback; //??

	final Handler handler; //Для отложенного запроса данных о пользователях
	
	private List<mContact> accum_cnt;
	
	
	boolean isSetupFinished = true;
	int setup_stage;
	
	mDialog dl_current_dlg;
		
	List<mDialog> loading_msgs = new ArrayList<mDialog>();
		
	public void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb){
		if(!authorised){
			class Runnable_r implements Runnable {
				AsyncTaskCompleteListener<RunnableAdvanced<?>> cb;
				
				Runnable_r(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb){
					this.cb = cb;
				}
				
				@Override
				public void run() {
					requestNewMessagesRunnable(cb);
				}
				
			};
			
			Runnable r = new Runnable_r(cb);
			handler.postDelayed(r, 10000);
			Log.d("requestNewMessagesRunnable", "Not authorised, retrying in 1 sec");
			return;
		}
		
		VKRequest request = new VKRequest("messages.getLongPollServer", VKParameters.from(VKApiConst.COUNT, String.valueOf(1)));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		VKRequestListener rl = 	new VKRequestListenerWithCallback<RunnableAdvanced<?>>(cb, Vk.this) {

					@Override
				    public void onComplete(VKResponse response) {
				    	Log.d("requestNewMessagesRunnable", "onComplete" );
				        try {
				        	JSONObject response_json = response.json.getJSONObject("response");

				    		
			    			String key = response_json.getString( "key" );
			    			String server = response_json.getString( "server" );
			    			Integer ts = response_json.getInt( "ts" );
			    			
			    			RunnableAdvanced<?> r = new LongPollRunnable(server, key, ts);
			    			callback.onTaskComplete(r);
			    			
					    } catch (JSONException e) {
							e.printStackTrace();
						}
					}
				    
				};
				

		request.executeWithListener(rl);
	}
	
	public void authorize(Context acontext){
		if(authorization_finished){
			VKUIHelper.onResume((Activity) acontext);
			VKSdk.authorize(sMyScope, false, true);
			//VKUIHelper.onDestroy((Activity) acontext);
			authorization_finished = false;
		}
	}
	
	public Vk(MyApplication app) {
		super(app);
		service_name = "Vk";
		service_type = VK;
	
		sPref = app.getSharedPreferences(service_name, Context.MODE_PRIVATE); //загрузка конфигов
		
		self_contact = new mContact(sPref.getString("active_account", ""));
		
		accum_cnt = new ArrayList<mContact>();
		handler = new Handler();
		
		//Инициализация VkSdk		
		//VKUIHelper.onResume((Activity) this.context);
		VKSdk.initialize(sdkListener, "4161005", VKAccessToken.tokenFromSharedPreferences(this.app.getApplicationContext(), sTokenKey));
		//VKSdk.authorize(sMyScope, false, true);		
		//VKUIHelper.onDestroy((Activity) this.context);
		
	}
	
	private void requestAccountInfo() { //Только при setup
		VKRequest request = new VKRequest("users.get", VKParameters.from(VKApiConst.FIELDS, "photo_100"));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
				
		VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
		    @Override
		    public void onComplete(VKResponse response) {
		    	Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
		        try {
		        	JSONArray response_json = response.json.getJSONArray("response");
		        	if(response_json.length() > 0){
			        	JSONObject item = response_json.getJSONObject(0);

			        	if(self_contact == null)self_contact = new mContact(item.getString("id"));
			        	else self_contact.address = item.getString("id");
			        	
			        	String name = item.getString("first_name");
			        	name += " " + item.getString("last_name");
			        	
			        	String photo_100_url = item.getString("photo_100");
			        	self_contact.icon_100_url = photo_100_url;
			        	self_contact.name = name;
			        	
			        	SharedPreferences sPref = app.getSharedPreferences(service_name, Context.MODE_PRIVATE); //загрузка конфигов
			        	Editor ed = sPref.edit();
			        	ed.putString("current_account", self_contact.address);
			        	ed.commit();

			        	if(!isSetupFinished){
			        		setup_stage++;
			        		setupStages();
			        	}
		        	}
		        	
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }

		    
		    
		};

		request.executeWithListener(rl);
		
	}

	private void requestActiveDlg() {
		final AsyncTaskCompleteListener<List<mDialog>> acb = new AsyncTaskCompleteListener<List<mDialog>>(){

			@Override
			public void onTaskComplete(List<mDialog> result) {
				if(result.size() > 0)active_dlg = result.get(0);
			}
			
		};
		
		requestDialogs(0, 1, acb);		
	}



	@Override
	public void requestContactData(mContact cnt) {
		
		app.dbHelper.loadContact(cnt, this);
		
		accum_cnt.add(cnt);
		//Log.d("requestContactData", "Requested new contact: " + cnt.address);
		
		if(!accum_cnt_handler_isRunning){
			accum_cnt_handler_isRunning = true;
			
			handler.postDelayed(cnts_request_runnable, 500);
		}
			
	}
	
	Runnable cnts_request_runnable = new Runnable(){

		@Override
		public void run() {
			//Do something after 500ms
			//Log.d("requestContactData", "Starting downloading contact data...");
			final List<mContact> cnt_temp = new ArrayList<mContact>(accum_cnt);
			
			
			if(cnt_temp.size() == 0){
				Log.e("cnts_request_runnable", "error");
			}
			
			accum_cnt.clear();
			
			String uids = cnt_temp.get(0).address;
			for(int i = 1; i < cnt_temp.size(); i++){
				uids += "," + cnt_temp.get(i).address;
			}

			VKRequest request = new VKRequest("users.get", VKParameters.from(VKApiConst.USER_IDS, uids, VKApiConst.FIELDS, "photo_100"));
			request.secure = false;
			VKParameters preparedParameters = request.getPreparedParameters();
			
			
			
			VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
			    @Override
			    public void onComplete(VKResponse response) {
			    	Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
			        try {
			        	JSONArray response_json = response.json.getJSONArray("response");
			        	SQLiteDatabase db = app.dbHelper.getWritableDatabase();
			    		String my_table_name = app.dbHelper.getTableNameCnts(Vk.this);
			    		boolean updated = false;
			    		
			        	for(int i = 0; i < response_json.length(); i++){
				        	JSONObject item = response_json.getJSONObject(i);

				        	mContact cnt = cnt_temp.get(i);
				        	
				        	String name = item.getString("first_name");
				        	name += " " + item.getString("last_name");
				        	
				        	String photo_100_url = item.getString("photo_100");
				            
				            cnt.icon_100_url = photo_100_url;
				        			
				        	cnt.name = name;
				        	
				        	
				    		
				    		String selection = DBHelper.colAddress + " = ?";
			    			String[] selectionArgs = {cnt.address};
			    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
			    			
			    			if(c.moveToFirst()){
			    				//update
			    				if(!cnt.name.equals(c.getString(c.getColumnIndex(DBHelper.colName)))){
			    					app.dbHelper.updateCnt(cnt, Vk.this);
			    					updated = true;
			    				}
			    				if(!cnt.icon_100_url.equals(c.getString(c.getColumnIndex(DBHelper.colIcon100url)))){
			    					app.dbHelper.updateCnt(cnt, Vk.this);
			    					updated = true;
			    				}
			    				
			    			} else {
			    				// add
			    				app.dbHelper.insertCnt(cnt, Vk.this);
			    				updated = true;
			    			}
			    			
			        	}
			        	
			        	
			        	if(accum_cnt.size() > 0)handler.postDelayed(cnts_request_runnable, 500);
			        	else accum_cnt_handler_isRunning = false; 
			        	
			        	if(updated)app.triggerCntsUpdaters(); 
			        	
					} catch (JSONException e) {
						e.printStackTrace();
					}
			    }
	
			    
			    
			};
	
			request.executeWithListener(rl);
			
		}
		
	};


	@Override
	public boolean sendMessage(String address, String text) {
		VKRequest request = new VKRequest("messages.send", VKParameters.from(VKApiConst.USER_ID, address,
				VKApiConst.MESSAGE, text));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		
		VKRequestListener rl = new VKRequestListener() {
		    @Override
		    public void onComplete(VKResponse response) {
		    	Log.d("VKRequestListener", "onComplete" );
		    }

		    @Override
		    public void onError(VKError error) {
		    	Log.w("VKRequestListener.requestMessages", "onError " + error.errorMessage + ", " + error.apiError.errorMessage);
		    	if(error.apiError != null) HandleApiError(error);
		        // Ошибка. Сообщаем пользователю об error.
		    }
		    @Override
		    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
		    	Log.d("VKRequestListener", "attemptFailed" );
		        // Неудачная попытка. В аргументах имеется номер попытки и общее их количество.
		    }
		    
		};

		request.executeWithListener(rl);
		return false;
	}

	@Override
	public String[] getStringsForMainViewMenu() {
		String data[] = {active_dlg.getParticipantsNames(), "New message", "All messages", "Status", "Wall", "Friend Wall", "News"};

		return data;
	}

	@Override
	public void MainViewMenu_click(int which, Context con) {
		Intent intent;
		switch(which) {
		case 0:
			if(getActiveDialog() != null){
				intent = new Intent(con, ActivityTwo.class);
				intent.putExtra("mode", "messages");
				con.startActivity(intent);
			}
			break;
		case 1:
			intent = new Intent(con, ActivityTwo.class);
			intent.putExtra("mode", "contacts");
			con.startActivity(intent);
			break;
		case 2:
			intent = new Intent(con, ActivityTwo.class);
			intent.putExtra("mode", "dialogs");
			con.startActivity(intent);
			break;
		}
	}
	

	
	private VKSdkListener sdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
        	Log.d("VKSdkListener", "onCaptchaError" );
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
        	Log.d("VKSdkListener", "onTokenExpired" );
        	if(app.getCurrentActivity() != null) authorize(app.getCurrentActivity());
            //VKSdk.authorize(sMyScope, false, false);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
        	authorization_finished = true;
        	Log.d("VKSdkListener", "onAccessDenied" );
            new AlertDialog.Builder(app.getApplicationContext())
                    .setMessage(authorizationError.errorMessage)
                    .show();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
            newToken.saveTokenToSharedPreferences(app.getApplicationContext(), sTokenKey);
            authorization_finished = true;
            Log.d("VKSdkListener", "onReceiveNewToken" );
            authorised = true;
            if(!isSetupFinished){
            	setup_stage++;
            	setupStages();
            }
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
        	Log.d("VKSdkListener", "onAcceptUserToken" );
        	authorised = true;
        }
    };
	
    

	
    
    @Override
	public void requestMessages(mDialog dlg, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb) {
    	Log.d("requestMessages", "requested :: " + String.valueOf(isLoadingMsgsForDlg(dlg)));
    	
    	// Обновление информации о количестве потоков загрузки
    	Integer lm_count = msgs_thread_count.get(dlg);
    	if(lm_count == null){
    		lm_count = 2;
    		msgs_thread_count.put(dlg, lm_count);
    	}
    	else lm_count += 2;

    	// Загрузка из БД
    	lm_count--;
    	if(cb != null){
	    	List<mMessage> db_data = load_msgs_from_db(dlg, count, offset);	    	
	    	cb.onTaskComplete( db_data );    	    	
    	}
    	
    	// Скачивание из интернета
		VKRequest request = new VKRequest("messages.getHistory", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
				VKApiConst.OFFSET, String.valueOf(offset), VKApiConst.USER_ID, dlg.getParticipants()));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();

		VKRequestListener rl = new VKRequestListenerWithCallback<List<mMessage>>(cb, Vk.this) {
				    @Override				    
				    public void onComplete(VKResponse response) {				    	
				    	Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
				    	List<mMessage> msgs = new ArrayList<mMessage>();
				    	boolean all_new = true;
				        try {
				        	JSONObject response_json = response.json.getJSONObject("response");
				        	JSONArray items = response_json.getJSONArray("items");
				        	
				        	SQLiteDatabase db = app.dbHelper.getWritableDatabase();
				    		String my_table_name = app.dbHelper.getTableNameMsgs(Vk.this);
				    		
				    		mDialog dlg = (mDialog) params.get(0);
				    		int dlg_key = app.dbHelper.getDlgId(dlg, Vk.this);
				    		
				    		if(items.length() == 0)dl_all_msgs_downloaded = true;
				    		
				    		for (int i = 0; i < items.length(); i++) {
				    			JSONObject item = items.getJSONObject(i);
				    			
				    			mMessage msg = new mMessage();
				    			msg.setFlag(mMessage.OUT, item.getInt("out") == 1 ?	true : false);
				    			
				    			msg.respondent = getContact( item.getString( "user_id" ) );
								msg.text = item.getString( "body" );
								msg.sendTime.set(item.getLong( "date" )*1000);
								msg.setFlag(mMessage.READED, item.getInt( "read_state" ) == 1 ? true : false);
								msg.id = item.getString("id");
								
				    				
								String selection = DBHelper.colDlgkey + " = ? AND " + DBHelper.colSendtime + " = ? AND " + DBHelper.colBody + " = ?";
				    			String[] selectionArgs = { String.valueOf(dlg_key), String.valueOf(msg.sendTime.toMillis(false)), msg.text };
				    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
		
				    			if(c.moveToFirst()){
				    				c.close();
				    				all_new = false;
				    				//break;
				    			} else {
				    				//add
				    				c.close();
				    				
				    				app.dbHelper.insertMsg(msg, my_table_name, dlg_key);
				    				    				
				    				msgs.add(msg);
				    			}
				    		}

				    		if(all_new){
				    			int count = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.COUNT) );
				    			int offset = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.OFFSET) );

			    				int msgs_to_update = app.dbHelper.getMsgsCount(dlg_key, Vk.this) - (offset + count); 
			    				if( msgs_to_update > 0 ){
			    					if(msgs_to_update > MSGS_DOWNLOAD_COUNT){				    					
			    						requestMessages(dlg, offset + count, MSGS_DOWNLOAD_COUNT, null);
			    					} else {
			    						requestMessages(dlg, offset + count, msgs_to_update, null);
			    					}
			    				}				    			
				    		} 
				    		
				    		Integer lm_count = msgs_thread_count.get(dlg);
				    		lm_count--;
				    		
				    		Log.d("requestMessages", "onTaskComplete - net :: " + String.valueOf(isLoadingMsgsForDlg(dlg)));
				    		if(callback == null)app.triggerMsgsUpdaters(msgs);
				    		else callback.onTaskComplete(msgs);
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }
				    
				};

		((VKRequestListenerWithCallback<List<mMessage>>) rl).setParams(dlg);
		request.executeWithListener(rl);

	}

	@Override
	public void requestDialogs(int offset, int count, AsyncTaskCompleteListener<List<mDialog>> cb) {
		
		// Обновление информации о количестве потоков загрузки
		dlgs_thread_count += 2;
		
		// Загрузка из БД
		dlgs_thread_count--;
		if(cb != null)cb.onTaskComplete( load_dialogs_from_db(count, offset) );
		
		// Скачивание из интернета
		VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
				VKApiConst.OFFSET, String.valueOf(offset),
				VKApiConst.FIELDS, "first_name,last_name,photo_50"));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		
		VKRequestListener rl = new VKRequestListenerWithCallback<List<mDialog>>(cb, Vk.this) {
			
				@Override
			    public void onComplete(VKResponse response) {
					Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
					int count = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.COUNT) );
	    			int offset = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.OFFSET) );
					List<mDialog> dlgs = new ArrayList<mDialog>();
					boolean all_new = true;
			        try {
			        	JSONObject response_json = response.json.getJSONObject("response");
			        	JSONArray items = response_json.getJSONArray("items");
			        	
			        	SQLiteDatabase db = app.dbHelper.getReadableDatabase();
			    		String my_table_name = app.dbHelper.getTableNameDlgs(Vk.this);	
			    		
			    		if(items.length() < count)dl_all_dlgs_downloaded = true;
			    		
			    		for (int i = 0; i < items.length(); i++) {
			    			JSONObject item = items.getJSONObject(i);
			    			
		    				mDialog mdl = new mDialog();			    				
		    				String[] recipient_ids = item.getString( "user_id" ).split(",");
	
		    				for(String rid : recipient_ids){
	    						mdl.participants.add( getContact( rid ) );
		    				}
		    				
			    			mdl.snippet = item.getString( "body" );
			    			mdl.last_msg_time.set(item.getLong("date")*1000);
			    			mdl.msg_service = MessageService.VK;
			    			
			    			
			    			String selection = DBHelper.colParticipants + " = ?";
			    			String[] selectionArgs = {mdl.getParticipantsAddresses()};
			    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
	
			    			if(c.moveToFirst()){
			    				Time last_time_in_db = new Time();
			    				last_time_in_db.set( c.getLong( c.getColumnIndex(DBHelper.colLastmsgtime)) );
			    				
			    				if(mdl.last_msg_time.after(last_time_in_db)){
			    					//update
			    					int id = c.getInt(c.getColumnIndex(DBHelper.colId));
			    					c.close();
				    				
			    					app.dbHelper.updateDlg(id, mdl, Vk.this);			    					
				    							    				
				    				dlgs.add(mdl);
			    				} else {
			    					//not update
			    					c.close();
			    					all_new = false;
			    					continue;
			    				}		    				
			    			} else {
			    				//add
			    				c.close();
			    				
			    				app.dbHelper.insertDlg(mdl, Vk.this);
			    					    				
			    				dlgs.add(mdl);
			    				dlgs_count++;
			    			}
			    				
			    			
			    		}

			    		if(all_new){
		    				int dlgs_to_update = app.dbHelper.getDlgsCount(Vk.this) - (offset + count);
		    				
		    				if( dlgs_to_update > 0 ){
		    					if(dlgs_to_update > DLGS_DOWNLOAD_COUNT){				    					
		    						requestDialogs(offset + count, DLGS_DOWNLOAD_COUNT, null);
		    					} else {
		    						requestDialogs(offset + count, dlgs_to_update, null);
		    					}
		    				}
			    		}
			    					    		
			    		dlgs_thread_count--;
			    		if(callback == null) { // Обновление
			    			app.triggerDlgsUpdaters(dlgs);
			    		} else { // Запрос
			    			callback.onTaskComplete(dlgs);
			    		}
		        	
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }

		};
		
		request.executeWithListener(rl);

		
	}

	
    
	public void HandleApiError(VKError error){
		Log.d("HandleApiError", String.valueOf(error.apiError.errorCode) + " :: " + error.apiError.errorMessage);
		if(error.apiError.errorCode == 5){ // User authorization failed.
			if(authorization_finished && check_access_toten(error) ){
				Log.d("HandleApiError", "VKSdk.authorize: " + error.apiError.errorMessage);
	        	if(app.getCurrentActivity() != null) authorize(app.getCurrentActivity());
				//VKSdk.authorize(sMyScope, false, true);
				authorization_finished = false;
			}
			Log.d("HandleApiError", "error.request.repeat: " + error.apiError.errorMessage);
			Runnable r = new RunnableAdvanced<VKError>(){

				@Override
				public void run() {
					VKRequest r = new VKRequest(param.request.methodName, param.request.getMethodParameters());
					r.executeWithListener(param.request.requestListener);
					//param.request.repeat();
				}
				
			}.setParam(error);
			
			handler.postDelayed(r, 2000);
		}
		
		if(error.apiError.errorCode == 6){ // Too many requests per second.
			Runnable r = new RunnableAdvanced<VKError>(){

				@Override
				public void run() {
					param.request.repeat();
				}
				
			}.setParam(error);
			
			handler.postDelayed(r, 5000);
			
		}
	}


	private boolean check_access_toten(VKError error) {
		for(Map<String, String> m : error.apiError.requestParams){
			if(m.get("key").equals(VKApiConst.ACCESS_TOKEN)){
				return m.get("value").equals(VKSdk.getAccessToken().accessToken);
			}
		}
		return false;
	}


	class LongPollRunnable extends RunnableAdvanced<Void> {
		String server;
		String key;
		Integer ts;
		
		LongPollRunnable(String server, String key, Integer ts) { 
        	this.server = server;
        	this.key = key;
        	this.ts = ts; 
        }

		@Override
		public void run_iteration() {
			Log.d("LongPollRunnable", "start");
			try {
			  URL url = new URL("http://"+server+"?act=a_check&key="+key+"&ts="+ts.toString()+"&wait=25&mode=2");
			  HttpURLConnection con = (HttpURLConnection) url.openConnection();
			  //new LongPoll_async().execute(con.getInputStream());
			  BufferedReader reader = null;
			  String line = "";
			  String page = "";
			  try {
				  reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
				  while ((line = reader.readLine()) != null) {
					  page += line;
				  }
			  } catch (IOException e) {
				  e.printStackTrace();
			  } finally {
				  if (reader != null) {
					  try {
						  reader.close();
					  } catch (IOException e) {
						  e.printStackTrace();
					  }
				  }
			  }
  
			  Log.d("LongPollRunnable", page);
			  
			  JSONObject response_json = new JSONObject(page);
			  ts = response_json.getInt( "ts" );
			  
			  JSONArray updates = response_json.getJSONArray("updates");
			  for (int i = 0; i < updates.length(); i++) {
				  JSONArray item = updates.getJSONArray(i);

				  if (item.getInt(0) == 4) {
					  int flags = item.getInt(2);
					  String from_id = item.getString(3);
					  int timestamp = item.getInt(4);
					  String subject = item.getString(5);
					  String text = item.getString(6);
					 
					  mMessage msg = new mMessage();
					  msg.respondent = getContact( from_id );
					  msg.setFlag(mMessage.OUT, (flags & 2) == 2);
		  		 	  msg.text = text;
		 			  msg.sendTime.set(timestamp*1000);
					 
					  Intent intent = new Intent(MsgReceiver.ACTION_RECEIVE);
			    	  intent.putExtra("service_type", getServiceType());
			    	  intent.putExtra("msg", msg);
			    	  app.sendBroadcast(intent);

					  //Log.d("LongPollRunnable", text);
				  }
			  }

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		    			
	}



	@Override
	public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb) {

		VKRequest request = new VKRequest("friends.get", VKParameters.from("order", "hints",
				VKApiConst.OFFSET, String.valueOf(offset), VKApiConst.COUNT, String.valueOf(count), VKApiConst.FIELDS, "photo_100"));
		
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();

		VKRequestListener rl = new VKRequestListenerWithCallback<List<mContact>>(cb, Vk.this) {
				    @Override				    
				    public void onComplete(VKResponse response) {				    	
				    	Log.d("requestContacts", "onComplete" );
				    	List<mContact> cnts = new ArrayList<mContact>();
				        try {
				        	JSONObject response_json = response.json.getJSONObject("response");
				        	JSONArray items = response_json.getJSONArray("items");
				    		
				    		for (int i = 0; i < items.length(); i++) {
				    			JSONObject item = items.getJSONObject(i);
				    			
				    			mContact cnt;
				    			if(contacts.get(item.getString("id")) == null){
				    				cnt = new mContact(item.getString("id"));				    				
				    				
				    				String name = item.getString("first_name");
						        	name += " " + item.getString("last_name");
						        	
						        	cnt.name = name;
						        	
						        	String photo_100_url = item.getString("photo_100");
						        	
						        	Intent intent = new Intent(app.getApplicationContext(), DownloadService.class);
						            intent.putExtra("url", photo_100_url);
						            app.getApplicationContext().startService(intent);
						        	
						            download_waiter tw = new download_waiter(photo_100_url, "cnt_icon_100", cnt);
						            app.dl_waiters.add(tw);
						        	
						        	contacts.put(item.getString("id"), cnt);
				    			}
				    			cnt = contacts.get(item.getString("id"));
					        	
					        	cnt.online = item.getInt("online") == 1 ? true : false;
				    			
								cnts.add(cnt);
				    		}
				    		callback.onTaskComplete(cnts);
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }
				    
				};

		request.executeWithListener(rl);
		
	}



	private List<mDialog> load_dialogs_from_db(int count, int offset){
		return app.dbHelper.loadDlgs(this, count, offset);
	}
	
	private List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset){		
		List<mMessage> result = app.dbHelper.loadMsgs(this, dlg, count, offset);	
		
		return result;
	}

	@Override
	public void setup() {
		isSetupFinished = false;
		setup_stage = 1;
		setupStages();
	}
	
	private void setupStages(){
		switch(setup_stage){
		case 1:
			authorization_finished = false;
			authorised = false;
			VKSdk.logout();
			VKSdk.authorize(sMyScope, false, true);
			break;
		case 2:
			requestAccountInfo();
			break;
		case 3:
			Editor ed = sPref.edit();
	    	ed.putString("active_account", self_contact.address);
	    	ed.commit();
	    	
			app.dbHelper.createTables(this);
			
			// Обновления
			Intent intent = new Intent(app.getApplicationContext(), UpdateService.class);
			intent.putExtra("specific_service", getServiceType());
			app.startService(intent);
			
			setup_stage++;
			setupStages();	
			break;
		case 4:
			isSetupFinished = true;
			break;
		}
	}

	@Override
	public void init() {
		Runnable r = new Runnable(){

			@Override
			public void run() {
				if(isSetupFinished){
					//Подключение к базе данных, получение количества диалогов
					SQLiteDatabase db = app.dbHelper.getReadableDatabase();		
					String my_table_name = app.dbHelper.getTableNameDlgs(Vk.this);
					Cursor c = db.query(my_table_name, null, null, null, null, null, null);
					dlgs_count = c.getCount();
					c.close();
					//db.close();
					
					requestAccountInfo();
					requestActiveDlg();
				} else {
					Log.d("Vk::init", "isSetupFinished = false, delayed for 1000ms");
					handler.postDelayed(this, 1000);
				}
				
			}
			
		};
		
		handler.post(r);
		
	}

	@Override
	public void unsetup() {
		VKSdk.logout();
		
		Intent intent = new Intent(app.getApplicationContext(), UpdateService.class);
		intent.putExtra("specific_service", getServiceType());
		intent.putExtra("remove", true);
		app.startService(intent);	
	}
	
	public void requestMarkAsReaded(mMessage msg){
		VKRequest request = new VKRequest("messages.markAsRead", VKParameters.from("message_ids", msg.id, VKApiConst.USER_ID, msg.respondent.address));
		
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		
		final mMessage tmsg = msg;

		VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
				    @Override				    
				    public void onComplete(VKResponse response) {				    	
				    	Log.d("requestContacts", "onComplete" );
				    	List<mContact> cnts = new ArrayList<mContact>();
				        try {
				        	int resp = response.json.getInt("response");
				        	if(resp == 1)tmsg.setFlag(mMessage.READED, true);
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }
				    
				};

		request.executeWithListener(rl);
	}



}
