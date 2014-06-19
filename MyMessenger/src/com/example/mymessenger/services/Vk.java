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
import com.example.mymessenger.R;
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
		
		for(long[] group : emoji){
			for(long code : group){
				String scode = ChatMessageFormatter.long_to_hex_string(code);
				String res_url = "http://vk.com/images/emoji/" + scode + ".png";
				String ccode = ChatMessageFormatter.string_from_hex_string(scode);
				ChatMessageFormatter.addPattern(getServiceType(), res_url, ccode);
			}
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
			//authorization_finished = false;
			authorised = false;
			try{
				VKSdk.logout();
			} catch (Exception ex) {
				Toast toast = Toast.makeText(MyApplication.context, Log.getStackTraceString(ex), Toast.LENGTH_LONG);
				toast.show();
			}
			//VKSdk.authorize(sMyScope, false, true);
			authorize(MyApplication.getCurrentActivity());
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
			    			mdl.msg_service_type = MessageService.VK;
			    			
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

	public static long[][] emoji;
	public static int[] emoji_group_icons = new int[]{R.drawable.ic_emoji_smile, R.drawable.ic_emoji_flower, R.drawable.ic_emoji_bell, R.drawable.ic_emoji_car, R.drawable.ic_emoji_symbol};
	static {
        emoji = new long[5][];
        emoji[0] = new long[]{3627933188L, 3627933187L, 3627933184L, 3627933194L, 9786, 3627933193L, 3627933197L, 3627933208L, 3627933210L, 3627933207L, 3627933209L, 3627933212L, 3627933213L, 3627933211L, 3627933235L, 3627933185L, 3627933204L, 3627933196L, 3627933202L, 3627933214L, 3627933219L, 3627933218L, 3627933186L, 3627933229L, 3627933226L, 3627933221L, 3627933232L, 3627933189L, 3627933203L, 3627933225L, 3627933227L, 3627933224L, 3627933233L, 3627933216L, 3627933217L, 3627933220L, 3627933206L, 3627933190L, 3627933195L, 3627933239L, 3627933198L, 3627933236L, 3627933237L, 3627933234L, 3627933215L, 3627933222L, 3627933223L, 3627933192L, 3627932799L, 3627933230L, 3627933228L, 3627933200L, 3627933205L, 3627933231L, 3627933238L, 3627933191L, 3627933199L, 3627933201L, 3627932786L, 3627932787L, 3627932782L, 3627932791L, 3627932802L, 3627932790L, 3627932774L, 3627932775L, 3627932776L, 3627932777L, 3627932788L, 3627932789L, 3627932785L, 3627932796L, 3627932792L, 3627933242L, 3627933240L, 3627933243L, 3627933245L, 3627933244L, 3627933248L, 3627933247L, 3627933241L, 3627933246L, 3627932793L, 3627932794L, 3627933256L, 3627933257L, 3627933258L, 3627932800L, 3627932797L, 3627932841L, 3627932965L, 10024, 3627867935L, 3627932843L, 3627932837L, 3627932834L, 3627932838L, 3627932839L, 3627932836L, 3627932840L, 3627932738L, 3627932736L, 3627932739L, 3627932741L, 3627932740L, 3627932749L, 3627932750L, 3627932748L, 3627932746L, 9994, 9996, 3627932747L, 9995, 3627932752L, 3627932742L, 3627932743L, 3627932745L, 3627932744L, 3627933260L, 3627933263L, 9757, 3627932751L, 3627932842L, 3627933366L, 3627868099L, 3627932803L, 3627932779L, 3627932778L, 3627932780L, 3627932781L, 3627932815L, 3627932817L, 3627932783L, 3627933254L, 3627933253L, 3627932801L, 3627933259L, 3627932806L, 3627932807L, 3627932805L, 3627932784L, 3627933262L, 3627933261L, 3627933255L, 3627868073L, 3627932753L, 3627932754L, 3627932767L, 3627932766L, 3627932769L, 3627932768L, 3627932770L, 3627932757L, 3627932756L, 3627932762L, 3627932759L, 3627868093L, 3627932758L, 3627932760L, 3627932761L, 3627932860L, 3627932764L, 3627932765L, 3627932763L, 3627932755L, 3627868032L, 3627867906L, 3627932804L, 3627932827L, 3627932825L, 3627932828L, 3627932826L, 10084, 3627932820L, 3627932823L, 3627932819L, 3627932821L, 3627932822L, 3627932830L, 3627932824L, 3627932812L, 3627932811L, 3627932813L, 3627932814L, 3627932772L, 3627932773L, 3627932844L, 3627932771L, 3627932845L};
        emoji[1] = new long[]{3627932726L, 3627932730L, 3627932721L, 3627932717L, 3627932729L, 3627932720L, 3627932728L, 3627932719L, 3627932712L, 3627932731L, 3627932727L, 3627932733L, 3627932718L, 3627932695L, 3627932725L, 3627932690L, 3627932724L, 3627932689L, 3627932696L, 3627932732L, 3627932711L, 3627932710L, 3627932708L, 3627932709L, 3627932707L, 3627932692L, 3627932685L, 3627932706L, 3627932699L, 3627932701L, 3627932700L, 3627932702L, 3627932684L, 3627932697L, 3627932698L, 3627932704L, 3627932703L, 3627932716L, 3627932723L, 3627932683L, 3627932676L, 3627932687L, 3627932672L, 3627932675L, 3627932677L, 3627932679L, 3627932681L, 3627932686L, 3627932688L, 3627932691L, 3627932693L, 3627932694L, 3627932673L, 3627932674L, 3627932722L, 3627932705L, 3627932682L, 3627932715L, 3627932714L, 3627932678L, 3627932680L, 3627932713L, 3627932734L, 3627932816L, 3627867960L, 3627867959L, 3627867968L, 3627867961L, 3627867963L, 3627867962L, 3627867969L, 3627867971L, 3627867970L, 3627867967L, 3627867966L, 3627867972L, 3627867957L, 3627867956L, 3627867954L, 3627867955L, 3627867952L, 3627867953L, 3627867964L, 3627867920L, 3627867934L, 3627867933L, 3627867930L, 3627867921L, 3627867922L, 3627867923L, 3627867924L, 3627867925L, 3627867926L, 3627867927L, 3627867928L, 3627867932L, 3627867931L, 3627867929L, 3627867917L, 3627867918L, 3627867919L, 3627867915L, 3627867916L, 3627867936L, 11088, 9728, 9925, 9729, 9889, 9748, 10052, 9924, 3627867904L, 3627867905L, 3627867912L, 3627867914L};
        emoji[2] = new long[]{3627868045L, 3627932829L, 3627868046L, 3627868050L, 3627868051L, 3627868047L, 3627868038L, 3627868039L, 3627868048L, 3627868049L, 3627868035L, 3627932795L, 3627868037L, 3627868036L, 3627868033L, 3627868043L, 3627868041L, 3627868042L, 3627868040L, 3627868044L, 3627932974L, 3627868069L, 3627932919L, 3627932921L, 3627932924L, 3627932863L, 3627932864L, 3627932861L, 3627932862L, 3627932859L, 3627932913L, 9742, 3627932894L, 3627932895L, 3627932896L, 3627932897L, 3627932922L, 3627932923L, 3627932938L, 3627932937L, 3627932936L, 3627932935L, 3627932948L, 3627932948L, 3627932898L, 3627932899L, 9203, 8987, 9200, 8986, 3627932947L, 3627932946L, 3627932943L, 3627932944L, 3627932945L, 3627932942L, 3627932833L, 3627932966L, 3627932934L, 3627932933L, 3627932940L, 3627932939L, 3627932941L, 3627933376L, 3627933375L, 3627933373L, 3627932967L, 3627932969L, 3627932968L, 3627933354L, 3627933356L, 3627932835L, 3627932971L, 3627932970L, 3627932810L, 3627932809L, 3627932848L, 3627932852L, 3627932853L, 3627932855L, 3627932854L, 3627932851L, 3627932856L, 3627932914L, 3627932903L, 3627932901L, 3627932900L, 9993, 3627932905L, 3627932904L, 3627932911L, 3627932907L, 3627932906L, 3627932908L, 3627932909L, 3627932910L, 3627932902L, 3627932893L, 3627932868L, 3627932867L, 3627932881L, 3627932874L, 3627932872L, 3627932873L, 3627932892L, 3627932875L, 3627932869L, 3627932870L, 3627932871L, 3627932865L, 3627932866L, 9986, 3627932876L, 3627932878L, 10002, 9999, 3627932879L, 3627932880L, 3627932885L, 3627932887L, 3627932888L, 3627932889L, 3627932883L, 3627932884L, 3627932882L, 3627932890L, 3627932886L, 3627932950L, 3627932891L, 3627932972L, 3627932973L, 3627932912L, 3627868072L, 3627868076L, 3627868068L, 3627868071L, 3627868092L, 3627868085L, 3627868086L, 3627868089L, 3627868091L, 3627868090L, 3627868087L, 3627868088L, 3627932798L, 3627868078L, 3627867343L, 3627868084L, 3627867140L, 3627868082L, 3627868079L, 3627868104L, 3627868096L, 9917, 9918, 3627868094L, 3627868081L, 3627868105L, 3627868083L, 9971, 3627933365L, 3627933364L, 3627868097L, 3627868103L, 3627868102L, 3627868095L, 3627868098L, 3627868106L, 3627868100L, 3627868067L, 9749, 3627868021L, 3627868022L, 3627868028L, 3627868026L, 3627868027L, 3627868024L, 3627868025L, 3627868023L, 3627868020L, 3627867989L, 3627867988L, 3627867999L, 3627867991L, 3627867990L, 3627867997L, 3627867995L, 3627868004L, 3627868017L, 3627868003L, 3627868005L, 3627867993L, 3627867992L, 3627867994L, 3627867996L, 3627868018L, 3627868002L, 3627868001L, 3627868019L, 3627867998L, 3627868009L, 3627868014L, 3627868006L, 3627868008L, 3627868007L, 3627868034L, 3627868016L, 3627868010L, 3627868011L, 3627868012L, 3627868013L, 3627868015L, 3627867982L, 3627867983L, 3627867978L, 3627867979L, 3627867986L, 3627867975L, 3627867977L, 3627867987L, 3627867985L, 3627867976L, 3627867980L, 3627867984L, 3627867981L, 3627868000L, 3627867974L, 3627867973L, 3627867965L};
        emoji[3] = new long[]{3627868128L, 3627868129L, 3627868139L, 3627868130L, 3627868131L, 3627868133L, 3627868134L, 3627868138L, 3627868137L, 3627868136L, 3627932818L, 9962, 3627868140L, 3627868132L, 3627867911L, 3627867910L, 3627868143L, 3627868144L, 9978, 3627868141L, 3627933180L, 3627933182L, 3627933179L, 3627867908L, 3627867909L, 3627867907L, 3627933181L, 3627867913L, 3627868064L, 3627868065L, 9970, 3627868066L, 3627933346L, 9973, 3627933348L, 3627933347L, 9875, 3627933312L, 9992, 3627932858L, 3627933313L, 3627933314L, 3627933322L, 3627933321L, 3627933342L, 3627933318L, 3627933316L, 3627933317L, 3627933320L, 3627933319L, 3627933341L, 3627933323L, 3627933315L, 3627933326L, 3627933324L, 3627933325L, 3627933337L, 3627933336L, 3627933335L, 3627933333L, 3627933334L, 3627933339L, 3627933338L, 3627933352L, 3627933331L, 3627933332L, 3627933330L, 3627933329L, 3627933328L, 3627933362L, 3627933345L, 3627933343L, 3627933344L, 3627933340L, 3627932808L, 3627933327L, 3627868075L, 3627933350L, 3627933349L, 9888, 3627933351L, 3627932976L, 9981, 3627868142L, 3627868080L, 9832, 3627933183L, 3627868074L, 3627868077L, 3627932877L, 3627933353L, -2865171240719688203L, -2865171236424720905L, -2865171266489491990L, -2865171270784459277L, -2865171193475047944L, -2865171257899557385L, -2865171262194524680L, -2865171245014655495L, -2865171206359949830L, -2865171253604590105L};
        emoji[4] = new long[]{3219683, 3285219, 3350755, 3416291, 3481827, 3547363, 3612899, 3678435, 3743971, 3154147, 3627932959L, 3627932962L, 2302179, 3627932963L, 11014, 11015, 11013, 10145, 3627932960L, 3627932961L, 3627932964L, 8599, 8598, 8600, 8601, 8596, 8597, 3627932932L, 9664, 9654, 3627932988L, 3627932989L, 8617, 8618, 8505, 9194, 9193, 9195, 9196, 10549, 10548, 3627867543L, 3627932928L, 3627932929L, 3627932930L, 3627867541L, 3627867545L, 3627867538L, 3627867539L, 3627867542L, 3627932918L, 3627868070L, 3627867649L, 3627867695L, 3627867699L, 3627867701L, 3627867698L, 3627867700L, 3627867698L, 3627867728L, 3627867705L, 3627867706L, 3627867702L, 3627867674L, 3627933371L, 3627933369L, 3627933370L, 3627933372L, 3627933374L, 3627933360L, 3627933358L, 3627867519L, 9855, 3627933357L, 3627867703L, 3627867704L, 3627867650L, 9410, 3627867729L, 12953, 12951, 3627867537L, 3627867544L, 3627867540L, 3627933355L, 3627932958L, 3627932917L, 3627933359L, 3627933361L, 3627933363L, 3627933367L, 3627933368L, 9940, 10035, 10055, 10062, 9989, 10036, 3627932831L, 3627867546L, 3627932915L, 3627932916L, 3627867504L, 3627867505L, 3627867534L, 3627867518L, 3627932832L, 10175, 9851, 9800, 9801, 9802, 9803, 9804, 9805, 9806, 9807, 9808, 9809, 9810, 9811, 9934, 3627932975L, 3627868135L, 3627932857L, 3627932850L, 3627932849L, 169, 174, 8482, 12349, 12336, 3627932957L, 3627932954L, 3627932953L, 3627932955L, 3627932956L, 10060, 11093, 10071, 10067, 10069, 10068, 3627932931L, 3627933019L, 3627933031L, 3627933008L, 3627933020L, 3627933009L, 3627933021L, 3627933010L, 3627933022L, 3627933011L, 3627933023L, 3627933012L, 3627933024L, 3627933013L, 3627933014L, 3627933015L, 3627933016L, 3627933017L, 3627933018L, 3627933025L, 3627933026L, 3627933027L, 3627933028L, 3627933029L, 3627933030L, 10006, 10133, 10134, 10135, 9824, 9829, 9827, 9830, 3627932846L, 3627932847L, 10004, 9745, 3627932952L, 3627932951L, 10160, 3627932977L, 3627932978L, 3627932979L, 9724, 9723, 9726, 9725, 9642, 9643, 3627932986L, 11036, 11035, 9899, 9898, 3627932980L, 3627932981L, 3627932987L, 3627932982L, 3627932983L, 3627932984L, 3627932985L};
    }
	/*
	public static final String[] emoji = ("D83DDE04, " +
			"D83DDE0A, D83DDE03, D83DDE09, D83DDE06, D83DDE1C, D83DDE0B, D83DDE0D, D83DDE0E, D83DDE12, D83DDE0F, D83DDE14, D83DDE22, D83DDE2D, D83DDE29, D83DDE28, D83DDE10, D83DDE0C, D83DDE20, D83DDE21, D83DDE07, D83DDE30, D83DDE32, D83DDE33, D83DDE37, D83DDE1A, D83DDE08, 2764, D83DDC4D, D83DDC4E, 261D, 270C, D83DDC4C, 26BD, 26C5, D83CDF1F, D83CDF4C, D83CDF7A, D83CDF7B, D83CDF39, D83CDF45, D83CDF52, D83CDF81, D83CDF82, D83CDF84, D83CDFC1, D83CDFC6, D83DDC0E, D83DDC0F, D83DDC1C, D83DDC2B, D83DDC2E, D83DDC03, D83DDC3B, D83DDC3C, D83DDC05, D83DDC13, D83DDC18, D83DDC94, D83DDCAD, D83DDC36, D83DDC31, D83DDC37, D83DDC11, 23F3, 26BE, 26C4, 2600, D83CDF3A, D83CDF3B, D83CDF3C, D83CDF3D, D83CDF4A, D83CDF4B, D83CDF4D, D83CDF4E, D83CDF4F, D83CDF6D, D83CDF37, D83CDF38, D83CDF46, D83CDF49, D83CDF50, D83CDF51, D83CDF53, D83CDF54, D83CDF55, D83CDF56, D83CDF57, D83CDF69, D83CDF83, D83CDFAA, D83CDFB1, D83CDFB2, D83CDFB7, D83CDFB8, D83CDFBE, D83CDFC0, D83CDFE6, D83DDC00, D83DDC0C, D83DDC1B, D83DDC1D, D83DDC1F, D83DDC2A, D83DDC2C, D83DDC2D, D83DDC3A, D83DDC3D, D83DDC2F, D83DDC5C, D83DDC7B, D83DDC14, D83DDC23, D83DDC24, D83DDC40, D83DDC42, D83DDC43, D83DDC46, D83DDC47, D83DDC48, D83DDC51, D83DDC60, D83DDCA1, D83DDCA3, D83DDCAA, D83DDCAC, D83DDD14, D83DDD25").split(", ");
*/
	@Override
	public long[][] getEmojiCodes() {
		return emoji;
	}
			
	public static String getEmojiUrl(String code){
		return "http://vk.com/images/emoji/" + code + ".png";
	}

	@Override
	public int[] getEmojiGroupsIcons() {
		return emoji_group_icons;
	}


}
