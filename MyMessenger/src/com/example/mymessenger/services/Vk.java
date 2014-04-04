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
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MsgReceiver;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.Sms.load_msgs_async;
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
	
	private AsyncTaskCompleteListener<Void> contact_data_changed;
	final Handler handler;
	final Handler nwhandler;
	
	boolean authorised;
	
	public void requestNewMessagesRunnable(AsyncTaskCompleteListener<Runnable> cb){
		VKRequest request = new VKRequest("messages.getLongPollServer", VKParameters.from(VKApiConst.COUNT, String.valueOf(1)));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		VKRequestListener rl = 	new VKRequestListenerWithCallback<Runnable>(cb) {

					@Override
				    public void onComplete(VKResponse response) {
				    	Log.d("getLastDlg", "onComplete" );
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

				    @Override
				    public void onError(VKError error) {
				    	Log.w("requestActiveDlg", "onError " + error.errorCode + " : " + error.errorMessage + String.valueOf(authorization_finished));
				    	if(error.apiError != null) HandleApiError(error);
				        // Ошибка. Сообщаем пользователю об error.
				    }
				    @Override
				    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
				    	Log.d("getLastDlg", "attemptFailed" );
				        // Неудачная попытка. В аргументах имеется номер попытки и общее их количество.
				    }
				    
				};
				

		request.executeWithListener(rl);
	}
	
	public void authorize(Context acontext){
		VKUIHelper.onResume((Activity) acontext);
		VKSdk.authorize(sMyScope, false, false);
		VKUIHelper.onDestroy((Activity) acontext);
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
		
		nwhandler = new Handler( ((MyApplication) context).netthread.getLooper() );
	}

	private void requestActiveDlg() {
		VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(1)));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();

		request.executeWithListener(
				new VKRequestListener() {

					@Override
				    public void onComplete(VKResponse response) {
				    	Log.d("getLastDlg", "onComplete" );
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
				    	Log.w("requestActiveDlg", "onError " + error.errorCode + " : " + error.errorMessage + String.valueOf(authorization_finished));
				    	if(error.apiError != null) HandleApiError(error);
				        // Ошибка. Сообщаем пользователю об error.
				    }
				    @Override
				    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
				    	Log.d("getLastDlg", "attemptFailed" );
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
	public String getMyAddress() {
		// TODO Auto-generated method stub
		return "140195103";
	}

	@Override
	public void requestContactData(mContact cnt) {
		accum_cnt.add(cnt);
		
		if(!accum_cnt_handler_isRunning){
			accum_cnt_handler_isRunning = true;
			
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					//Do something after 500ms
					String uids = accum_cnt.get(0).address;
					for(int i = 1; i < accum_cnt.size(); i++){
						uids += "," + accum_cnt.get(i).address;
					}
		
					VKRequest request = new VKRequest("users.get", VKParameters.from(VKApiConst.USER_IDS, uids));
					request.secure = false;
					VKParameters preparedParameters = request.getPreparedParameters();
					
					class change_sender_name_callback implements AsyncTaskCompleteListener<Void>{
						public List<mContact> cnt;
						
						public change_sender_name_callback(List<mContact> cnt) {
							this.cnt = new ArrayList<mContact>(cnt);
							cnt.clear();
						}
			
						@Override
						public void onTaskComplete(Void result) {
							accum_cnt_handler_isRunning = false;
						}
						
					};
					
					change_sender_name_callback cb = new change_sender_name_callback(accum_cnt);
					
					VKRequestListener rl = new VKRequestListenerWithCallback<Void>(cb) {
					    @Override
					    public void onComplete(VKResponse response) {
					    	Log.d("VKRequestListener", "onComplete" );
					        try {
					        	JSONArray response_json = response.json.getJSONArray("response");
					        	for(int i = 0; i < response_json.length(); i++){
						        	JSONObject item = response_json.getJSONObject(i);
						        	change_sender_name_callback data_cb = (change_sender_name_callback) callback;
						        	mContact cnt = data_cb.cnt.get(i);
						        	
						        	String name = item.getString("first_name");
						        	name += " " + item.getString("last_name");
						        	
						        	cnt.name = name;
						        	
						        	callback.onTaskComplete(null);
					        	}
					        	if(contact_data_changed != null)contact_data_changed.onTaskComplete(null);
					        	
							} catch (JSONException e) {
								e.printStackTrace();
							}
					    }
			
					    @Override
					    public void onError(VKError error) {
					    	Log.w("VKRequestListener.requestContactData", "onError " + error.errorMessage + ", " + error.apiError.errorMessage);
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
            authorization_finished = false;
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
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
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
        	Log.d("VKSdkListener", "onAcceptUserToken" );
        }
    };
    

	
    @Override
	public void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb) {


		VKRequest request = new VKRequest("messages.getHistory", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
				VKApiConst.OFFSET, String.valueOf(offset), VKApiConst.USER_ID, activeDialog.getParticipants()));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();

		VKRequestListener rl = new VKRequestListenerWithCallback<List<mMessage>>(cb) {
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
				    			msg.sender = getContact( item.getString( "from_id" ) );
								
								msg.text = item.getString( "body" );
								msg.sendTime = new Time();
								msg.sendTime.set(item.getLong( "date" )*1000);
								msg.ReadState = item.getString( "read_state" );
								
				    				
								msgs.add(msg);
				    		}
				    		callback.onTaskComplete(msgs);
				        	
						} catch (JSONException e) {
							e.printStackTrace();
						}
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
	}

	@Override
	public void requestDialogs(int offset, int count,
			AsyncTaskCompleteListener<List<mDialog>> cb) {

		VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(count),
				VKApiConst.OFFSET, String.valueOf(offset),
				VKApiConst.FIELDS, "first_name,last_name,photo_50"));
		request.secure = false;
		VKParameters preparedParameters = request.getPreparedParameters();
		
		VKRequestListener rl = new VKRequestListenerWithCallback<List<mDialog>>(cb) {
			
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
		    				
		    			dlgs.add(mdl);

		    		}
		    		callback.onTaskComplete(dlgs);
		        	
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }

		    @Override
		    public void onError(VKError error) {
		    	Log.w("VKRequestListener.requestDialogs", "onError " + error.errorMessage + ", " + error.apiError.errorMessage);
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

	@Override
	public void setContactDataChangedCallback(
			AsyncTaskCompleteListener<Void> contact_data_changed) {
		this.contact_data_changed = contact_data_changed;
		
	}
    
    
	private void HandleApiError(VKError error){
		if(error.apiError.errorCode == 5){ // User authorization failed.
			if(authorization_finished && error.request.getPreparedParameters().get(VKApiConst.ACCESS_TOKEN).equals(VKSdk.getAccessToken().accessToken) ){
				Log.d("HandleApiError", "VKSdk.authorize: " + error.apiError.errorMessage);
				VKSdk.authorize(sMyScope, false, true);
				authorization_finished = false;
			}
			
			if(authorization_finished){
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
				//Log.d("LongPollRunnable", "start");
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
	  
				  //Log.d("LongPollRunnable", page);
				  
				  JSONObject response_json = new JSONObject(page);
				  ts = response_json.getInt( "ts" );
				  
				  JSONArray updates = response_json.getJSONArray("updates");
				  for (int i = 0; i < updates.length(); i++) {
					  JSONArray item = updates.getJSONArray(i);

					  if (item.getInt(0) == 4) {
						 String from_id = item.getString(3);
						 int timestamp = item.getInt(4);
						 String subject = item.getString(5);
						 String text = item.getString(6);
						 
						 mMessage msg = new mMessage();
						 msg.sender = getContact( from_id );
						
						 msg.text = text;
						 msg.sendTime = new Time();
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
	
}
