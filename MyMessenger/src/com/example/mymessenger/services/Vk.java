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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.mymessenger.ActivityTwo;
import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.ChatMessageFormatter;
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
import com.example.mymessenger.services.Sms.load_dlgs_async;
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
	
	
	boolean isSetupFinished = true;
	int setup_stage;
	
	mDialog dl_current_dlg;
		
	List<mDialog> loading_msgs = new ArrayList<mDialog>();
	
	List<VKRequest> requests_waiting_for_auth = new ArrayList<VKRequest>();
	boolean requests_waiting_for_auth_runnable_active = false;
		
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
		VKSdk.initialize(sdkListener, "4161005", VKAccessToken.tokenFromSharedPreferences(this.msApp.getApplicationContext(), sTokenKey));
		//VKSdk.authorize(sMyScope, false, true);		
		//VKUIHelper.onDestroy((Activity) this.context);
		
		for(String code : emoji){
			ChatMessageFormatter.addPatternVk(code);
		}
		
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
			        	
			        	SharedPreferences sPref = msApp.getSharedPreferences(service_name, Context.MODE_PRIVATE); //загрузка конфигов
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
		if(getActiveDialog() != null)return;
		final AsyncTaskCompleteListener<List<mDialog>> acb = new AsyncTaskCompleteListener<List<mDialog>>(){

			@Override
			public void onTaskComplete(List<mDialog> result) {
				if(result.size() > 0)setActiveDialog(result.get(0));
			}
			
		};
		
		requestDialogs(1, 0, acb);		
	}



	@Override
	public void requestContactData(mContact cnt) {
		
		msApp.dbHelper.loadContact(cnt, this);
		
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
			        	SQLiteDatabase db = msApp.dbHelper.getWritableDatabase();
			    		String my_table_name = msApp.dbHelper.getTableNameCnts(Vk.this);
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
			    					msApp.dbHelper.updateCnt(cnt, Vk.this);
			    					updated = true;
			    				}
			    				if(!cnt.icon_100_url.equals(c.getString(c.getColumnIndex(DBHelper.colIcon100url)))){
			    					msApp.dbHelper.updateCnt(cnt, Vk.this);
			    					updated = true;
			    				}
			    				
			    			} else {
			    				// add
			    				msApp.dbHelper.insertCnt(cnt, Vk.this);
			    				updated = true;
			    			}
			    			
			        	}
			        	
			        	
			        	if(accum_cnt.size() > 0)handler.postDelayed(cnts_request_runnable, 500);
			        	else accum_cnt_handler_isRunning = false; 
			        	
			        	if(updated)msApp.triggerCntsUpdaters(); 
			        	
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
		String data[] = {getActiveDialog().getParticipantsNames(), "New message", "All messages", "Status", "Wall", "Friend Wall", "News"};

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
        	if(msApp.getCurrentActivity() != null) authorize(msApp.getCurrentActivity());
            //VKSdk.authorize(sMyScope, false, false);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
        	authorization_finished = true;
        	Log.d("VKSdkListener", "onAccessDenied" );
            new AlertDialog.Builder(msApp.getApplicationContext())
                    .setMessage(authorizationError.errorMessage)
                    .show();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
        	Log.d("VKSdkListener", "onReceiveNewToken " + newToken.accessToken + " :: " + sTokenKey + " :: " + VKSdk.getAccessToken().accessToken);
            newToken.saveTokenToSharedPreferences(msApp.getApplicationContext(), sTokenKey);
            authorization_finished = true;
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
	
        
	

	
    
	public void HandleApiError(VKError error){
		Log.d("HandleApiError", String.valueOf(error.apiError.errorCode) + " :: " + error.apiError.errorMessage);
		if(error.apiError.errorCode == 5){ // User authorization failed.
			if(authorization_finished && check_access_toten(error) ){
				//Log.d("HandleApiError", "VKSdk.authorize: " + error.apiError.errorMessage);
	        	if(msApp.getCurrentActivity() != null) authorize(msApp.getCurrentActivity());
			}
			
			if(requests_waiting_for_auth.size() < 10){
				requests_waiting_for_auth.add(error.request);
				Log.d("HandleApiError", "request added to requests_waiting_for_auth");
			}
        				
			
			if(!requests_waiting_for_auth_runnable_active){
				requests_waiting_for_auth_runnable_active = true;
				
				Runnable r = new RunnableAdvanced<Void>(){

					@Override
					public void run() {
						if(authorization_finished){
							Log.d("+++", VKSdk.getAccessToken().accessToken);
							for(VKRequest r : requests_waiting_for_auth){
								Log.d("HandleApiError", "Executing request from requests_waiting_for_auth: " + r.methodName);
								Log.d("HandleApiError", "Executing request from requests_waiting_for_auth: " + r.methodName);
								VKRequest r_new = new VKRequest(r.methodName, r.getMethodParameters());
								r_new.executeWithListener(r.requestListener);
							}
							requests_waiting_for_auth.clear();
							requests_waiting_for_auth_runnable_active = false;
						} else {
							Log.d("HandleApiError", "Auth not finished, waiting: " + String.valueOf(requests_waiting_for_auth.size()));
							handler.postDelayed(this, 10000);
						}
					}
					
				};
				
				handler.postDelayed(r, 20000);
			}
			
			
			
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
				Log.d("check_access_toten", m.get("value") + " :: " + VKSdk.getAccessToken().accessToken);
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
			  
			  if(response_json.has("failed")){
				  kill();
			  }
			  
			  ts = response_json.getInt( "ts" );
			  
			  JSONArray updates = response_json.getJSONArray("updates");
			  for (int i = 0; i < updates.length(); i++) {				  
				  JSONArray item = updates.getJSONArray(i);
				  
				  if(item.getInt(0) == 1) { // 1,$message_id,$flags -- замена флагов сообщения (FLAGS:=$flags)
					  int message_id = item.getInt(1);
					  mMessage msg = msApp.dbHelper.getMsgByMsgId(message_id, Vk.this);
					  if(msg != null){
						  int flags = item.getInt(2);
						  
						  msg.setFlag(mMessage.READED, (flags & 1) != 1);
						  msg.setFlag(mMessage.OUT, (flags & 2) == 2);
						  
						  Intent intent = new Intent(MsgReceiver.ACTION_UPDATE);
				    	  intent.putExtra("service_type", getServiceType());
				    	  intent.putExtra("msg", msg);
				    	  msApp.sendBroadcast(intent);
					  }
				  }

				  if(item.getInt(0) == 2) { // 2,$message_id,$mask[,$user_id] -- установка флагов сообщения (FLAGS|=$mask)
					  int message_id = item.getInt(1);
					  mMessage msg = msApp.dbHelper.getMsgByMsgId(message_id, Vk.this);
					  if(msg != null){
						  int flags = item.getInt(2);
						  
						  msg.setFlag(mMessage.READED, (flags & 1) != 1);
						  msg.setFlag(mMessage.OUT, (flags & 2) == 2);
						  
						  Intent intent = new Intent(MsgReceiver.ACTION_UPDATE);
				    	  intent.putExtra("service_type", getServiceType());
				    	  intent.putExtra("msg", msg);
				    	  msApp.sendBroadcast(intent);
					  }
				  }

				  if(item.getInt(0) == 3) { // 3,$message_id,$mask[,$user_id] -- сброс флагов сообщения (FLAGS&=~$mask)
					  int message_id = item.getInt(1);
					  mMessage msg = msApp.dbHelper.getMsgByMsgId(message_id, Vk.this);
					  if(msg != null){
						  int flags = item.getInt(2);
						  
						  msg.setFlag(mMessage.READED, (flags & 1) == 1);
						  msg.setFlag(mMessage.OUT, (flags & 2) != 2);
						  
						  Intent intent = new Intent(MsgReceiver.ACTION_UPDATE);
				    	  intent.putExtra("service_type", getServiceType());
				    	  intent.putExtra("msg", msg);
				    	  msApp.sendBroadcast(intent);
					  }
				  }

				  if (item.getInt(0) == 4) { // 4,$message_id,$flags,$from_id,$timestamp,$subject,$text,$attachments -- добавление нового сообщения
					  String msg_id = item.getString(1);
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
		 			  msg.id = msg_id;
		 			  msg.msg_service = getServiceType();
					 
					  Intent intent = new Intent(MsgReceiver.ACTION_RECEIVE);
			    	  intent.putExtra("service_type", getServiceType());
			    	  intent.putExtra("msg", msg);
			    	  msApp.sendBroadcast(intent);

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
						        	
						        	Intent intent = new Intent(msApp.getApplicationContext(), DownloadService.class);
						            intent.putExtra("url", photo_100_url);
						            msApp.getApplicationContext().startService(intent);
						        	
						            download_waiter tw = new download_waiter(photo_100_url){
						            	mContact cnt;
						            	
										@Override
										public void onDownloadComplete() {
											cnt.icon_100 = BitmapFactory.decodeFile(filepath);
										}
										
										public download_waiter setParams(mContact cnt){
											this.cnt = cnt;
											return this;
										}
						            	
						            }.setParams(cnt);
						            msApp.dl_waiters.add(tw);
						        	
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
		return msApp.dbHelper.loadDlgs(this, count, offset);
	}
	
	private List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset){		
		List<mMessage> result = msApp.dbHelper.loadMsgs(this, dlg, count, offset);	
		
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
	    	
			msApp.dbHelper.createTables(this);
			
			// Обновления
			Intent intent = new Intent(msApp.getApplicationContext(), UpdateService.class);
			intent.putExtra("specific_service", getServiceType());
			msApp.startService(intent);
			
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
					SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();		
					String my_table_name = msApp.dbHelper.getTableNameDlgs(Vk.this);
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
		
		Intent intent = new Intent(msApp.getApplicationContext(), UpdateService.class);
		intent.putExtra("specific_service", getServiceType());
		intent.putExtra("remove", true);
		msApp.startService(intent);	
	}
	
	public void requestMarkAsReaded(mMessage msg){
		VKRequest request = new VKRequest("messages.markAsRead", VKParameters.from("message_ids", msg.id, VKApiConst.USER_ID, msg.respondent.address));
		
		msg.setFlag(mMessage.LOADING, true);
		
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		
		final mMessage tmsg = msg;

		VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
				    @Override				    
				    public void onComplete(VKResponse response) {				    	
				    	Log.d("requestContacts", "onComplete" );
				    	List<mMessage> msgs = new ArrayList<mMessage>();
				        try {
				        	int resp = response.json.getInt("response");
				        	if(resp == 1)tmsg.setFlag(mMessage.READED, true);
				        	tmsg.setFlag(mMessage.LOADING, false);
				        	msgs.add(tmsg);
				        	msApp.triggerMsgsUpdaters(msgs);
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }
				    
				};

		request.executeWithListener(rl);
	}

	class load_dlgs_async extends AsyncTask<Integer, Void, List<mDialog>> {
	    private AsyncTaskCompleteListener<List<mDialog>> callback;

	    public load_dlgs_async(AsyncTaskCompleteListener<List<mDialog>> cb) {
	        this.callback = cb;
	    }

	    protected void onPostExecute(List<mDialog> result) {
	    	dlgs_thread_count--;
	        if(callback != null)callback.onTaskComplete(result);
	    }

		@Override
		protected List<mDialog> doInBackground(Integer... params) {
			return load_dialogs_from_db(params[0], params[1]);
		}  
	}
	
	class load_msgs_async extends AsyncTask<Integer, Void, List<mMessage>> {
	    private AsyncTaskCompleteListener<List<mMessage>> callback;
		private mDialog dlg;

	    public load_msgs_async(AsyncTaskCompleteListener<List<mMessage>> cb, mDialog dialog) {
	        this.callback = cb;
	        this.dlg = dialog;
	    }

	    protected void onPostExecute(List<mMessage> result) {
	    	updateMsgsThreadCount(dlg, -1);
	    	if(callback != null)callback.onTaskComplete(result);
	   }

		@Override
		protected List<mMessage> doInBackground(Integer... params) {
			return load_msgs_from_db(dlg, params[0], params[1]);
		}  
	}

	public static final String[] emoji = ("D83DDE04, " +
			"D83DDE0A, D83DDE03, D83DDE09, D83DDE06, D83DDE1C, D83DDE0B, D83DDE0D, D83DDE0E, D83DDE12, D83DDE0F, D83DDE14, D83DDE22, D83DDE2D, D83DDE29, D83DDE28, D83DDE10, D83DDE0C, D83DDE20, D83DDE21, D83DDE07, D83DDE30, D83DDE32, D83DDE33, D83DDE37, D83DDE1A, D83DDE08, 2764, D83DDC4D, D83DDC4E, 261D, 270C, D83DDC4C, 26BD, 26C5, D83CDF1F, D83CDF4C, D83CDF7A, D83CDF7B, D83CDF39, D83CDF45, D83CDF52, D83CDF81, D83CDF82, D83CDF84, D83CDFC1, D83CDFC6, D83DDC0E, D83DDC0F, D83DDC1C, D83DDC2B, D83DDC2E, D83DDC03, D83DDC3B, D83DDC3C, D83DDC05, D83DDC13, D83DDC18, D83DDC94, D83DDCAD, D83DDC36, D83DDC31, D83DDC37, D83DDC11, 23F3, 26BE, 26C4, 2600, D83CDF3A, D83CDF3B, D83CDF3C, D83CDF3D, D83CDF4A, D83CDF4B, D83CDF4D, D83CDF4E, D83CDF4F, D83CDF6D, D83CDF37, D83CDF38, D83CDF46, D83CDF49, D83CDF50, D83CDF51, D83CDF53, D83CDF54, D83CDF55, D83CDF56, D83CDF57, D83CDF69, D83CDF83, D83CDFAA, D83CDFB1, D83CDFB2, D83CDFB7, D83CDFB8, D83CDFBE, D83CDFC0, D83CDFE6, D83DDC00, D83DDC0C, D83DDC1B, D83DDC1D, D83DDC1F, D83DDC2A, D83DDC2C, D83DDC2D, D83DDC3A, D83DDC3D, D83DDC2F, D83DDC5C, D83DDC7B, D83DDC14, D83DDC23, D83DDC24, D83DDC40, D83DDC42, D83DDC43, D83DDC46, D83DDC47, D83DDC48, D83DDC51, D83DDC60, D83DDCA1, D83DDCA3, D83DDCAA, D83DDCAC, D83DDD14, D83DDD25").split(", ");

	@Override
	public String[] getEmojiCodes() {
		return emoji;
	}
			
	public static String getEmojiUrl(String code){
		return "http://vk.com/images/emoji/" + code + ".png";
	}

	@Override
	protected void getDialogsFromDB(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
		// Обновление информации о количестве потоков загрузки
		dlgs_thread_count += 1;
		
		new load_dlgs_async(cb).execute(count, offset);		
	}

	@Override
	protected void getDialogsFromNet(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
		// Обновление информации о количестве потоков загрузки
		dlgs_thread_count += 1;
		
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
					
			        try {
			        	JSONObject response_json = response.json.getJSONObject("response");
			        	JSONArray items = response_json.getJSONArray("items");
			        	
			        	for (int i = 0; i < items.length(); i++) {
			    			JSONObject item = items.getJSONObject(i);
			    			mDialog mdl = new mDialog();			    				
		    				String[] recipient_ids = item.getString( "user_id" ).split(",");
		    				
		    				for(String rid : recipient_ids){
	    						mdl.participants.add( getContact( rid ) );
		    				}
		    				
			    			mdl.snippet = item.getString( "body" );
			    			mdl.snippet_out = item.getInt( "out" );
			    			mdl.last_msg_time.set(item.getLong("date")*1000);
			    			mdl.msg_service = MessageService.VK;
			    			
			    			dlgs.add(mdl);
			        	}
			        	
			        	dlgs_thread_count--;
			        	if(callback != null) { 
			        		callback.onTaskComplete(dlgs);
			    		}
			    			
		        	
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }

		};
		
		request.executeWithListener(rl);
	}

	@Override
	protected void getMessagesFromDB(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
		// Обновление информации о количестве потоков загрузки
		updateMsgsThreadCount(dlg, 1);

    	new load_msgs_async(cb, dlg).execute(count, offset);   
	}

	@Override
	protected void getMessagesFromNet(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
		// Обновление информации о количестве потоков загрузки
		updateMsgsThreadCount(dlg, 1);
		
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
				    		
				    		mDialog dlg = (mDialog) params.get(0);
				    						    		
				    		for (int i = 0; i < items.length(); i++) {
				    			JSONObject item = items.getJSONObject(i);
				    			
				    			mMessage msg = new mMessage();
				    			msg.setFlag(mMessage.OUT, item.getInt("out") == 1 ?	true : false);
				    			
				    			msg.respondent = getContact( item.getString( "user_id" ) );
								msg.text = item.getString( "body" );
								msg.sendTime.set(item.getLong( "date" )*1000);
								msg.setFlag(mMessage.READED, item.getInt( "read_state" ) == 1 ? true : false);
								msg.id = item.getString("id");
								msg.msg_service = getServiceType();
				    		
				    
				    			msgs.add(msg);
				    		}

				    		
				    		updateMsgsThreadCount(dlg, -1);
				    		
				    		Log.d("requestMessages", "onTaskComplete - net :: " + String.valueOf(isLoadingMsgsForDlg(dlg)));
				    		if(callback != null){
				    			callback.onTaskComplete(msgs);
				    		}
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }
				    
				};

		((VKRequestListenerWithCallback<List<mMessage>>) rl).setParams(dlg);
		request.executeWithListener(rl);
		
	}

	
	

}
