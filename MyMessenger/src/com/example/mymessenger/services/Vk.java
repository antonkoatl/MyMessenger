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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.mymessenger.ActivityTwo;
import com.example.mymessenger.AsyncTaskCompleteListener;
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

public class Vk implements MessageService {
	private Context context;
	private static String sTokenKey = "VK_ACCESS_TOKEN";
	private static String[] sMyScope = new String[]{VKScope.FRIENDS, VKScope.WALL, VKScope.PHOTOS, VKScope.NOHTTPS, VKScope.MESSAGES};
	public mDialog active_dlg;
	
	List<mDialog> return_dialogs;
	List<mMessage> return_msgs;
	
	List<mContact> accum_cnt;
	
	boolean accum_cnt_handler_isRunning;
	
	Map<String, mContact> contacts;
	
	boolean finished;
	boolean handling;
	private boolean authorization_finished;
	private AsyncTaskCompleteListener<List<mMessage>> requestMessagesCallback;
	private AsyncTaskCompleteListener<List<mDialog>> requestDialogsCallback;

	final Handler handler;
	
	boolean authorised;
	
	mContact self_contact;
	
	
	
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
			handler.postDelayed(r, 1000);
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
			VKSdk.authorize(sMyScope, false, false);
			VKUIHelper.onDestroy((Activity) acontext);
			authorization_finished = false;
		}
	}
	
	public Vk(Context context) {
		this.context = context;
		authorization_finished = true;
		authorised = false;
		
		//VKUIHelper.onResume((Activity) this.context);
		VKSdk.initialize(sdkListener, "4161005", VKAccessToken.tokenFromSharedPreferences(this.context, sTokenKey));
		//VKSdk.authorize(sMyScope, false, true);
		
		active_dlg = new mDialog();
		requestActiveDlg();
		//VKUIHelper.onDestroy((Activity) this.context);

		contacts = new HashMap<String, mContact>();
		
		accum_cnt = new ArrayList<mContact>();
		
		accum_cnt_handler_isRunning = false;
		handler = new Handler();
				
		self_contact = new mContact("140195103");
		requestContactData(self_contact);
	}

	private void requestActiveDlg() {
		VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(1)));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();

		request.executeWithListener(
				new VKRequestListener() {

					@Override
				    public void onComplete(VKResponse response) {
				    	Log.d("requestActiveDlg", "onComplete" );
				        try {
				        	JSONObject response_json = response.json.getJSONObject("response");
				        	JSONArray items = response_json.getJSONArray("items");
				    		

			    			JSONObject item = items.getJSONObject(0);
				    			
		    				mDialog mdl = new mDialog();			    				
		    				String[] recipient_ids = item.getString( "user_id" ).split(",");

		    				for(String rid : recipient_ids){
	    						mdl.participants.add( getContact(rid) );
		    				}
		    				
			    			mdl.snippet = item.getString( "body" );
			    				
			    			active_dlg = mdl;

				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }

				    @Override
				    public void onError(VKError error) {
				    	Log.w("requestActiveDlg", "onError " + error.errorCode + " : " + error.errorMessage + " : " + error.apiError + " : " + String.valueOf(authorization_finished));
				    	if(error.apiError != null) HandleApiError(error);
				        // Ошибка. Сообщаем пользователю об error.
				    }
				    @Override
				    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
				    	Log.d("requestActiveDlg", "attemptFailed" );
				        // Неудачная попытка. В аргументах имеется номер попытки и общее их количество.
				    }
				    
				}
				
				);
		
	}


	@Override
	public String getServiceName() {
		return "Vk";
	}

	@Override
	public int getServiceType() {
		return MessageService.VK;
	}

	@Override
	public void setActiveDialog(mDialog dlg) {
		active_dlg = dlg;
	}

	@Override
	public mDialog getActiveDialog() {
		return active_dlg;
	}

	@Override
	public mContact getMyContact() {
		return self_contact;
	}

	@Override
	public void requestContactData(mContact cnt) {
		accum_cnt.add(cnt);
		//Log.d("requestContactData", "Requested new contact: " + cnt.address);
		
		if(!accum_cnt_handler_isRunning){
			accum_cnt_handler_isRunning = true;
			
			handler.postDelayed(new Runnable() {

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
					    	Log.d("VKRequestListener", "onComplete" );
					        try {
					        	JSONArray response_json = response.json.getJSONArray("response");
					        	for(int i = 0; i < response_json.length(); i++){
						        	JSONObject item = response_json.getJSONObject(i);

						        	mContact cnt = cnt_temp.get(i);
						        	
						        	String name = item.getString("first_name");
						        	name += " " + item.getString("last_name");
						        	
						        	String photo_100_url = item.getString("photo_100");
						        	
						        	Intent intent = new Intent(context, DownloadService.class);
						            intent.putExtra("url", photo_100_url);
						            context.getApplicationContext().startService(intent);
						        	
						            download_waiter tw = new download_waiter(photo_100_url, "cnt_icon_100", cnt);
						            ((MyApplication) context).dl_waiters.add(tw);
						        			
						        	cnt.name = name;
						        	
						        	//Log.d("requestContactData", "Contact data for " + cnt.address + " received: " + cnt.name);
						        	
					        	}
					        	cnt_temp.clear();
					        	accum_cnt_handler_isRunning = false;
					        	
					        	((MyApplication) context).triggerCntsUpdaters(); 
					        	
							} catch (JSONException e) {
								e.printStackTrace();
							}
					    }
			
					    
					    
					};
			
					request.executeWithListener(rl);
					
				}
			}, 500);
		}
			
	}


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
        	MyApplication app = (MyApplication) context;
        	if(app.getCurrentActivity() != null) authorize(app.getCurrentActivity());
            //VKSdk.authorize(sMyScope, false, false);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
        	authorization_finished = true;
        	Log.d("VKSdkListener", "onAccessDenied" );
            new AlertDialog.Builder(Vk.this.context)
                    .setMessage(authorizationError.errorMessage)
                    .show();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
            newToken.saveTokenToSharedPreferences(Vk.this.context, sTokenKey);
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
	public void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb) {


		VKRequest request = new VKRequest("messages.getHistory", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
				VKApiConst.OFFSET, String.valueOf(offset), VKApiConst.USER_ID, activeDialog.getParticipants()));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();

		VKRequestListener rl = new VKRequestListenerWithCallback<List<mMessage>>(cb, Vk.this) {
				    @Override				    
				    public void onComplete(VKResponse response) {				    	
				    	Log.d("VKRequestListener", "onComplete" );
				    	List<mMessage> msgs = new ArrayList<mMessage>();
				        try {
				        	JSONObject response_json = response.json.getJSONObject("response");
				        	JSONArray items = response_json.getJSONArray("items");
				    		
				    		for (int i = 0; i < items.length(); i++) {
				    			JSONObject item = items.getJSONObject(i);
				    			
				    			mMessage msg = new mMessage();
				    			msg.out = item.getInt("out") == 1 ?	true : false;
				    			
				    			msg.respondent = getContact( item.getString( "user_id" ) );
								msg.text = item.getString( "body" );
								msg.sendTime.set(item.getLong( "date" )*1000);
								msg.ReadState = item.getString( "read_state" );
								
				    				
								msgs.add(msg);
				    		}
				    		callback.onTaskComplete(msgs);
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
				    }
				    
				};

		request.executeWithListener(rl);
	}

	@Override
	public void requestDialogs(int offset, int count,
			AsyncTaskCompleteListener<List<mDialog>> cb) {

		VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
				VKApiConst.OFFSET, String.valueOf(offset),
				VKApiConst.FIELDS, "first_name,last_name,photo_50"));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		
		VKRequestListener rl = new VKRequestListenerWithCallback<List<mDialog>>(cb, Vk.this) {
			
		    @Override
			    public void onComplete(VKResponse response) {
		    	Log.d("VKRequestListener", "onComplete" );
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
		    			mdl.last_msg_time.set(item.getLong("date")*1000);
		    			mdl.msg_service = MessageService.VK;
		    				
		    			dlgs.add(mdl);

		    		}
		    		callback.onTaskComplete(dlgs);
		        	
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }

		};
		
		request.executeWithListener(rl);
		
	}

	



	@Override
	public mContact getContact(String address) {
		mContact cnt = contacts.get(address);
		
		if(cnt == null){
			cnt = new mContact(address);
			
			requestContactData(cnt);
			
			contacts.put(address, cnt);
		}
		
		return cnt;
	}
    
    
	public void HandleApiError(VKError error){
		if(error.apiError.errorCode == 5){ // User authorization failed.
			if(authorization_finished && error.request.getPreparedParameters().get(VKApiConst.ACCESS_TOKEN).equals(VKSdk.getAccessToken().accessToken) ){
				Log.d("HandleApiError", "VKSdk.authorize: " + error.apiError.errorMessage);
				MyApplication app = (MyApplication) context;
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
				    	  context.sendBroadcast(intent);

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
						        	
						        	Intent intent = new Intent(context, DownloadService.class);
						            intent.putExtra("url", photo_100_url);
						            context.getApplicationContext().startService(intent);
						        	
						            download_waiter tw = new download_waiter(photo_100_url, "cnt_icon_100", cnt);
						            ((MyApplication) context).dl_waiters.add(tw);
						        	
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



	
	
}
