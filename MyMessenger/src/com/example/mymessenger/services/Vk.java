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
	
	boolean accum_cnt_handler_isRunning = false; //??
	
	boolean finished; //??
	boolean handling; //??
	
	private boolean authorization_finished = true;
	
	private AsyncTaskCompleteListener<List<mMessage>> requestMessagesCallback; //??
	private AsyncTaskCompleteListener<List<mDialog>> requestDialogsCallback; //??

	final Handler handler; //Для отложенного запроса данных о пользователях
	
	private List<mContact> accum_cnt;
	
	
	
	
	
	mDialog dl_current_dlg;
	
	
	List<mDialog> loading_msgs = new ArrayList<mDialog>();

	
	
	
	public void requestNewMessagesRunnable(AsyncTaskCompleteListener<Runnable> cb){
		if(!authorised){
			class Runnable_r implements Runnable {
				AsyncTaskCompleteListener<Runnable> cb;
				
				Runnable_r(AsyncTaskCompleteListener<Runnable> cb){
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
		VKRequestListener rl = 	new VKRequestListenerWithCallback<Runnable>(cb, Vk.this) {

					@Override
				    public void onComplete(VKResponse response) {
				    	Log.d("requestNewMessagesRunnable", "onComplete" );
				        try {
				        	JSONObject response_json = response.json.getJSONObject("response");

				    		
			    			String key = response_json.getString( "key" );
			    			String server = response_json.getString( "server" );
			    			Integer ts = response_json.getInt( "ts" );
			    			
			    			Runnable r = new LongPollRunnable(server, key, ts);
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
	
		SharedPreferences sPref = app.getSharedPreferences(service_name, Context.MODE_PRIVATE); //загрузка конфигов
		
		accum_cnt = new ArrayList<mContact>();
		handler = new Handler();
		
		
		
		//Подключение к базе данных, получение количества диалогов
		SQLiteDatabase db = app.dbHelper.getReadableDatabase();		
		String my_table_name = "dlgs_" + String.valueOf(getServiceType());
		Cursor c = db.query(my_table_name, null, null, null, null, null, null);
		dlgs_count = c.getCount();
		c.close();
		//db.close();
		
		//Инициализация VkSdk		
		//VKUIHelper.onResume((Activity) this.context);
		VKSdk.initialize(sdkListener, "4161005", VKAccessToken.tokenFromSharedPreferences(this.app.getApplicationContext(), sTokenKey));
		//VKSdk.authorize(sMyScope, false, true);		
		//VKUIHelper.onDestroy((Activity) this.context);
		
		String my_account = sPref.getString("current_account", "140195103");
		
		//Действия, требующие авторизации
		requestActiveDlg();		
		self_contact = getContact(my_account); //Должно получатся программно
		requestContactData(self_contact);
	}
	
	private void requestAccountInfo() {
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

			        	mContact cnt = new mContact(item.getString("id"));
			        	
			        	String name = item.getString("first_name");
			        	name += " " + item.getString("last_name");
			        	
			        	String photo_100_url = item.getString("photo_100");
			            cnt.icon_100_url = photo_100_url;
			        	cnt.name = name;
			        	
			        	
		        	}
		        	
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }

		    
		    
		};

		request.executeWithListener(rl);
		
	}

	private void requestActiveDlg() {
		AsyncTaskCompleteListener<List<mDialog>> acb = new AsyncTaskCompleteListener<List<mDialog>>(){

			@Override
			public void onTaskComplete(List<mDialog> result) {
				if(result.size() > 0)active_dlg = result.get(0);
			}
			
		};
		
		requestDialogs(0, 1, acb);
		
	}



	@Override
	public void requestContactData(mContact cnt) {
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
			        	for(int i = 0; i < response_json.length(); i++){
				        	JSONObject item = response_json.getJSONObject(i);

				        	mContact cnt = cnt_temp.get(i);
				        	
				        	String name = item.getString("first_name");
				        	name += " " + item.getString("last_name");
				        	
				        	String photo_100_url = item.getString("photo_100");
				        	
				        	//Intent intent = new Intent(context, DownloadService.class);
				            //intent.putExtra("url", photo_100_url);
				            //context.getApplicationContext().startService(intent);
				            
				            cnt.icon_100_url = photo_100_url;
				        	
				            //download_waiter tw = new download_waiter(photo_100_url, "cnt_icon_100", cnt);
				            //((MyApplication) context).dl_waiters.add(tw);
				        			
				        	cnt.name = name;
				        	
				        	//Log.d("requestContactData", "Contact data for " + cnt.address + " received: " + cnt.name);
				        	
			        	}
			        	cnt_temp.clear();
			        	accum_cnt_handler_isRunning = false;
			        	if(accum_cnt.size() > 0)handler.postDelayed(cnts_request_runnable, 500);
			        	
			        	app.triggerCntsUpdaters(); 
			        	
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
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
        	Log.d("VKSdkListener", "onAcceptUserToken" );
        	authorised = true;
        }
    };
    

	
    @Override
	public void requestMessages(mDialog dlg, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb) {
    	Log.d("requestMessages", "requested :: " + String.valueOf(dlg.loading_msgs));
    	
    	if(dl_current_dlg != dlg){
    		dl_current_dlg = dlg;
    		dl_all_new_msgs_downloaded = false;
    		dl_all_msgs_downloaded = false;
    	}
    	
    	Integer lm_count = msgs_thread_count.get(dlg);
    	if(lm_count == null){
    		lm_count = 2;
    		msgs_thread_count.put(dlg, lm_count);
    	}
    	else lm_count += 2;

    	Log.d("requestMessages", "onTaskComplete - bd :: " + String.valueOf(dlg.loading_msgs));
    	
    	List<mMessage> db_data = load_msgs_from_db(dlg, count, offset);
    	lm_count--;
    	cb.onTaskComplete( db_data );
    	
    	if(dl_all_new_msgs_downloaded && db_data.size() == count){
    		lm_count--;
    		return;
    	}
    	
    	
    	
    	
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
				    		String my_table_name = "msgs_" + String.valueOf(getServiceType());
				    		
				    		mDialog dlg = (mDialog) params.get(0);
				    		int dlg_key = app.dbHelper.getDlgId(dlg, getServiceType());
				    		
				    		if(items.length() == 0)dl_all_msgs_downloaded = true;
				    		
				    		for (int i = 0; i < items.length(); i++) {
				    			JSONObject item = items.getJSONObject(i);
				    			
				    			mMessage msg = new mMessage();
				    			msg.out = item.getInt("out") == 1 ?	true : false;
				    			
				    			msg.respondent = getContact( item.getString( "user_id" ) );
								msg.text = item.getString( "body" );
								msg.sendTime.set(item.getLong( "date" )*1000);
								msg.readed = item.getInt( "read_state" ) == 1 ? true : false;
								
				    				
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
				    				
				    				ContentValues cv = new ContentValues();
				    				cv.put(DBHelper.colRespondent, msg.respondent.address);
				    				cv.put(DBHelper.colSendtime, msg.sendTime.toMillis(false));
				    				cv.put(DBHelper.colBody, msg.text);
				    				cv.put(DBHelper.colDlgkey, dlg_key);
				    				
				    				int flags = 0;
				    				if(msg.out) flags += mMessage.OUT;
				    				if(msg.readed) flags += mMessage.READED;
				    				cv.put(DBHelper.colFlags, flags);
				    				
				    				db.insert(my_table_name, null, cv);		    				
				    				msgs.add(msg);
				    			}
				    		}
				    		
				    		
				    		//db.close();
				    		if(all_new){
				    			dl_all_new_msgs_downloaded = false;
				    			int count = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.COUNT) );
				    			int offset = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.OFFSET) );
				    			
				    			int msgs_count = 0;
				    			String selection = DBHelper.colDlgkey + " = ?";
				    			String[] selectionArgs = { String.valueOf(dlg_key) };
				    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
				    			msgs_count = c.getCount();
				    			
				    			if((count + offset) < msgs_count){
				    				requestDialogs(offset + count, count, null);
				    			}
				    			
				    		} else {
				    			dl_all_new_msgs_downloaded = true;
				    		}
				    		
				    		Integer lm_count = msgs_thread_count.get(dlg);
				    		lm_count--;
				    		
				    		Log.d("requestMessages", "onTaskComplete - net :: " + String.valueOf(dlg.loading_msgs));
				    		if(callback != null)callback.onTaskComplete(msgs);
				        	
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
		dlgs_thread_count += 2;
		
		dlgs_thread_count--;
		if(cb != null)cb.onTaskComplete( load_dialogs_from_db(count, offset) );
		
		
		if(!dl_all_dlgs_downloaded || (count + offset) > dlgs_count){
			//if(all_dlgs_downloaded)return;

			VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
					VKApiConst.OFFSET, String.valueOf(offset),
					VKApiConst.FIELDS, "first_name,last_name,photo_50"));
			request.secure = false;
			VKParameters preparedParameters = request.getPreparedParameters();
			
			VKRequestListener rl = new VKRequestListenerWithCallback<List<mDialog>>(cb, Vk.this) {
				
					@Override
				    public void onComplete(VKResponse response) {
						Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
			    	List<mDialog> dlgs = new ArrayList<mDialog>();
			    	boolean all_new = true;
			        try {
			        	JSONObject response_json = response.json.getJSONObject("response");
			        	JSONArray items = response_json.getJSONArray("items");
			        	
			        	SQLiteDatabase db = app.dbHelper.getWritableDatabase();
			    		String my_table_name = "dlgs_" + String.valueOf(getServiceType());	
			    		
			    		if(items.length() == 0)app.dlgs_loading_maxed = true;
			    		
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
				    				
				    				ContentValues cv = new ContentValues();
				    				cv.put(DBHelper.colParticipants, mdl.getParticipantsAddresses());
				    				cv.put(DBHelper.colLastmsgtime, mdl.last_msg_time.toMillis(false));
				    				cv.put(DBHelper.colSnippet, mdl.snippet);
				    				
				    				db.update(my_table_name, cv, "_id=" + id, null);			    				
				    				dlgs.add(mdl);
			    				} else {
			    					//not update
			    					c.close();
			    					all_new = false;
			    					break;
			    				}		    				
			    			} else {
			    				//add
			    				c.close();
			    				
			    				ContentValues cv = new ContentValues();
			    				cv.put(DBHelper.colParticipants, mdl.getParticipantsAddresses());
			    				cv.put(DBHelper.colLastmsgtime, mdl.last_msg_time.toMillis(false));
			    				cv.put(DBHelper.colSnippet, mdl.snippet);
			    				
			    				db.insert(my_table_name, null, cv);		    				
			    				dlgs.add(mdl);
			    				dlgs_count++;
			    			}
			    				
			    			
			    		}
			    		
			    		//db.close();
			    		
			    		if(all_new){
			    			dl_all_dlgs_downloaded = false;
			    			int count = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.COUNT) );
			    			int offset = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.OFFSET) );
			    			
			    			if((count + offset) < dlgs_count){
			    				requestDialogs(offset + count, count, null);
			    			}
			    			
			    		} else {
			    			dl_all_dlgs_downloaded = true;
			    		}
			    		
			    		dlgs_thread_count--;
			    		if(callback != null)callback.onTaskComplete(dlgs);
			        	
					} catch (JSONException e) {
						e.printStackTrace();
					}
			    }
	
			};
			
			request.executeWithListener(rl);
		} else {
			dlgs_thread_count--;
		}
		
	}

	
    
	public void HandleApiError(VKError error){
		if(error.apiError.errorCode == 5){ // User authorization failed.
			if(authorization_finished && error.request.getPreparedParameters().get(VKApiConst.ACCESS_TOKEN).equals(VKSdk.getAccessToken().accessToken) ){
				Log.d("HandleApiError", "VKSdk.authorize: " + error.apiError.errorMessage);
	        	if(app.getCurrentActivity() != null) authorize(app.getCurrentActivity());
				//VKSdk.authorize(sMyScope, false, true);
				authorization_finished = false;
				Log.d("HandleApiError", "error.request.repeat: " + error.apiError.errorMessage);
				error.request.repeat();
			}
		}
		
		if(error.apiError.errorCode == 6){ // Too many requests per second.
			error.request.repeat();
		}
	}


	class LongPollRunnable implements Runnable {
		String server;
		String key;
		Integer ts;
		
		LongPollRunnable(String server, String key, Integer ts) { 
        	this.server = server;
        	this.key = key;
        	this.ts = ts; 
        }

		@Override
		public void run() {
			while(true){
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
						  msg.out = (flags & 2) == 2;
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
		List<mDialog> result = new ArrayList<mDialog>();
		if(offset > dlgs_count)return result;
		
		SQLiteDatabase db = app.dbHelper.getReadableDatabase();
		
		String my_table_name = "dlgs_" + String.valueOf(getServiceType());
		String order_by = DBHelper.colLastmsgtime + " DESC";
		
		Cursor cursor = db.query(my_table_name, null, null, null, null, null, order_by);
		
		if(!cursor.moveToFirst()){return result;}
		boolean cursor_chk = true;
		for (int i = 0; i < offset; i++) cursor_chk = cursor.moveToNext();
				
		// определяем номера столбцов по имени в выборке
        int idColIndex = cursor.getColumnIndex( DBHelper.colId );
        int partColIndex = cursor.getColumnIndex( DBHelper.colParticipants );
        int lastMTColIndex = cursor.getColumnIndex( DBHelper.colLastmsgtime );
        int snipColIndex = cursor.getColumnIndex( DBHelper.colSnippet );
		
		for (int i = 0; i < count; i++) {
			if(cursor_chk){
				mDialog dlg = new mDialog();
	        	dlg.participants.add( getContact( cursor.getString(partColIndex) ) );
	        	dlg.last_msg_time.set( cursor.getLong(lastMTColIndex) );
	        	dlg.snippet = cursor.getString(snipColIndex);
	        	dlg.msg_service = getServiceType();
	        	
	        	result.add(dlg);
	        	cursor_chk = cursor.moveToNext();

	        }
	    }
		
		cursor.close();
		
		return result;
	}
	
	private List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset){
		List<mMessage> result = new ArrayList<mMessage>();
		//if(offset > 100)return result;
		
		int dlg_key = app.dbHelper.getDlgId(dlg, getServiceType());
		
		SQLiteDatabase db = app.dbHelper.getReadableDatabase();
		
		String my_table_name = "msgs_" + String.valueOf(getServiceType());
		String selection = DBHelper.colDlgkey + " = " + String.valueOf(dlg_key);
		String order_by = DBHelper.colSendtime + " DESC";
		
		
		Cursor cursor = db.query(my_table_name, null, selection, null, null, null, order_by);
		
		if(!cursor.moveToFirst()){return result;}
		boolean cursor_chk = true;
		for (int i = 0; i < offset; i++) cursor_chk = cursor.moveToNext();
		
		for (int i = 0; i < count; i++) {
			if(cursor_chk){
	        	mMessage msg = new mMessage();
	        	msg.respondent = getContact( cursor.getString( cursor.getColumnIndex(DBHelper.colRespondent) ) );
	        	msg.sendTime.set( cursor.getLong(cursor.getColumnIndex(DBHelper.colSendtime)) );
	        	msg.text = cursor.getString(cursor.getColumnIndex(DBHelper.colBody));
	        	int flags = cursor.getInt(cursor.getColumnIndex(DBHelper.colFlags));
	        	
	        	msg.out = (flags & mMessage.OUT) == mMessage.OUT ? true : false;
	        	msg.readed = (flags & mMessage.READED) == mMessage.READED ? true : false;

	        	result.add(msg);
	        	cursor_chk = cursor.moveToNext();
	        } 
	    }
		
		cursor.close();
		
		return result;
	}

	@Override
	public void setup() {
		// TODO Auto-generated method stub
		
	}



}
