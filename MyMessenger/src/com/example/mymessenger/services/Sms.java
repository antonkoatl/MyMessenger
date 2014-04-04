package com.example.mymessenger.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.mymessenger.ActivityTwo;
import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.SmsReceiver;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public class Sms implements MessageService {
	private Context context;
	private List<mDialog> dialogs;
	public String self_name;
	public String self_address;
	public mDialog active_dialog;
	private MyApplication app;
	BroadcastReceiver br;

	Map<String, mContact> contacts;
	private AsyncTaskCompleteListener<Void> contact_data_changed;
	
	public Sms(Context context) {
		this.context = context;
		self_name = "Me";
		dialogs = new ArrayList<mDialog>();
		app = (MyApplication) context.getApplicationContext();
		
		mSentIntent = PendingIntent.getBroadcast(context, 0, new Intent("CTS_SMS_SEND_ACTION"),
                PendingIntent.FLAG_ONE_SHOT);
        mDeliveredIntent = PendingIntent.getBroadcast(context, 0, new Intent("CTS_SMS_DELIVERY_ACTION"),
                PendingIntent.FLAG_ONE_SHOT);
        
        contacts = new HashMap<String, mContact>();
	}
	
	public List<mDialog> getDialogs(int offset, int count) {
		/*
		 * Inbox = "content://sms/inbox"
		 * Failed = "content://sms/failed"
		 * Queued = "content://sms/queued"
		 * Sent = "content://sms/sent"
		 * Draft = "content://sms/draft"
		 * Outbox = "content://sms/outbox"
		 * Undelivered = "content://sms/undelivered"
		 * All = "content://sms/all"
		 * Conversations = "content://sms/conversations".
		 */
		
		/* column names for provider:
		 *  0: _id
		 *  1: thread_id
		 *  2: address
		 *  3: person
		 *  4: date
		 *  5: protocol
		 *  6: read
		 *  7: status
		 *  8: type
		 *  9: reply_path_present
		 *  10: subject
		 *  11: body
		 *  12: service_center
		 *  13: locked
		 */
		
		String[] projection = null; // A list of which columns to return. Passing null will return all columns, which is inefficient.
		String selection = null; // A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI
		String[] selectionArgs = null; // You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings
		String sortOrder = null; // How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered
		Cursor cursor = context.getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), projection, selection, selectionArgs, sortOrder);
		
		int total = cursor.getCount();
		List<mDialog> return_dialogs = new ArrayList<mDialog>();
		
		if(cursor.moveToFirst()){}
		for (int i = 0; i < offset; i++) cursor.moveToNext();
		cursor.moveToPrevious();
		
		for (int i = 0; i < count; i++) {
			if(cursor.moveToNext()){
				mDialog mdl = new mDialog();
				String[] recipient_ids = cursor.getString( cursor.getColumnIndex("recipient_ids") ).split(" ");

				for(String rid : recipient_ids){
					Cursor c = context.getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = ?", new String[]{rid}, null);
					if(c.moveToNext()){
						mdl.participants.add( getContact( c.getString( c.getColumnIndex("address") ) ) );
					} else {
						for(String cn : cursor.getColumnNames()){
							Log.d("getDialogs", cn + " : " + cursor.getString(cursor.getColumnIndex(cn)) );
						}
						mdl.participants.add( getContact("DRAFT") ); //??
					}
					c.close();
				}
				
				mdl.snippet = cursor.getString( cursor.getColumnIndex("snippet") );
				
				return_dialogs.add(mdl);
			} else break;
		}
		
		cursor.close();
		return return_dialogs;
	}

	@Override
	public String getServiceName() {
		return "Sms";
	}


	@Override
	public int getServiceType() {
		return MessageService.SMS;
	}

	@Override
	public void setActiveDialog(mDialog mdl) {
		active_dialog = mdl;
	}

	@Override
	public mDialog getActiveDialog() {
		return active_dialog;
	}

	public List<mMessage> getMessages(mDialog dlg, int offset, int count) {
		/* MESSAGE_TYPE_ALL    = 0;
		 * MESSAGE_TYPE_INBOX  = 1;
		 * MESSAGE_TYPE_SENT   = 2;
		 * MESSAGE_TYPE_DRAFT  = 3;
		 * MESSAGE_TYPE_OUTBOX = 4;
		 * MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
		 * MESSAGE_TYPE_QUEUED = 6; // for messages to send later
		 */
		
		List<mMessage> return_msgs = new ArrayList<mMessage>();
		
		String[] projection = null; // A list of which columns to return. Passing null will return all columns, which is inefficient.
		String selection = "address=?"; // A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI
		String[] selectionArgs = {dlg.participants.get(0).address}; // You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings
		String sortOrder = null; // How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered
		Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms"), projection, selection, selectionArgs, sortOrder);
		
		int total = cursor.getCount();
		
		if(cursor.moveToFirst()){}
		for (int i = 0; i < offset; i++) cursor.moveToNext();
		cursor.moveToPrevious();
		
		for (int i = 0; i < count; i++) {
			if(cursor.moveToNext()){
				mMessage msg = new mMessage();
				String address = cursor.getString( cursor.getColumnIndex("address") );
				msg.text = cursor.getString( cursor.getColumnIndex("body") );
				msg.sendTime = new Time();
				msg.sendTime.set(cursor.getLong( cursor.getColumnIndex("date") ) );
				msg.ReadState = cursor.getString( cursor.getColumnIndex("read") );
				
				if (cursor.getString(cursor.getColumnIndex("type")).contains("1")) { //Inbox
					msg.sender = getContact(address);
	            } else if (cursor.getString(cursor.getColumnIndex("type")).contains("2")) { //Sent
	            	msg.sender = getContact(self_name);
	            } else {
	            	cursor.moveToNext();
	            	continue;
	            }
				return_msgs.add(msg);
			} else break;
		}
		
		cursor.close();
		return return_msgs;
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
	public String getMyAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void requestContactData(mContact cnt) {
		String name = "";

		// define the columns I want the query to return
	    String[] name_projection = new String[] {
	            ContactsContract.PhoneLookup.DISPLAY_NAME,
	            ContactsContract.PhoneLookup._ID};

	    // encode the phone number and build the filter URI
	    Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cnt.address));

	    // query time
	    Cursor name_cursor = context.getContentResolver().query(contactUri, name_projection, null, null, null);

	    if(name_cursor != null) {
	        if (name_cursor.moveToFirst()) {
	        	name =      name_cursor.getString(name_cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
	            Log.v("SmsService.getContactName", "Contact Found @ " + cnt.address);            
	            Log.v("SmsService.getContactName", "Contact name  = " + name);
	        } else {
	        	name = cnt.address;
	            Log.v("SmsService.getContactName", "Contact Not Found @ " + cnt.address);
	        }
	        name_cursor.close();
	    }
		    

		cnt.name = name;		
	}
		
	private PendingIntent mSentIntent;
    private PendingIntent mDeliveredIntent;
    
	@Override
	public boolean sendMessage(String address, String text) {
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage(address, null, text, mSentIntent, mDeliveredIntent);
		context.registerReceiver(mSendReceiver, new IntentFilter("CTS_SMS_SEND_ACTION"));
		context.registerReceiver(mDeliveryReceiver, new IntentFilter("CTS_SMS_DELIVERY_ACTION"));
		
		ContentValues values = new ContentValues();   
	    values.put("address", address);	              
	    values.put("body", text);
	    context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		
		return false;
		
		//ArrayList<String> parts = smsManager.divideMessage(message); 
	    //smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
	}
	
	private BroadcastReceiver mSendReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	switch (getResultCode()){
        	case Activity.RESULT_OK:
        		Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();
                break;

        	case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
        		Toast.makeText(context, "SMS: Generic failure", Toast.LENGTH_SHORT).show();
        		break;

            case SmsManager.RESULT_ERROR_NO_SERVICE:
            	Toast.makeText(context, "SMS: No service", Toast.LENGTH_SHORT).show();
            	break;

            case SmsManager.RESULT_ERROR_NULL_PDU:
            	Toast.makeText(context, "SMS: Null PDU", Toast.LENGTH_SHORT).show();
            	break;

            case SmsManager.RESULT_ERROR_RADIO_OFF:
            	Toast.makeText(context, "SMS: Radio off", Toast.LENGTH_SHORT).show();
            	break;
            }
        	 
        	context.unregisterReceiver(mSendReceiver);

        }

    };
    
    private BroadcastReceiver mDeliveryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			switch(getResultCode()) {
            case Activity.RESULT_OK:
                Toast.makeText(context, "SMS Delivered", Toast.LENGTH_SHORT).show();
                break;
            case Activity.RESULT_CANCELED:
                Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show();
                break;
            }
			
			context.unregisterReceiver(mDeliveryReceiver);
		}
    	
    };

	@Override
	public String[] getStringsForMainViewMenu() {
		String data[] = {"---", "New message", "All messages"};
		List<mDialog> t = getDialogs(0, 1);
		if(t.size() > 0){
			setActiveDialog(t.get(0));
			data[0] = t.get(0).getParticipantsNames();
		}
		else
			setActiveDialog(null);
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

	@Override
	public void requestMessages(mDialog dlg, int offset, int count,
			AsyncTaskCompleteListener<List<mMessage>> cb) {

		new load_msgs_async(this.context, cb, dlg).execute(offset, count);
		
	}
	
	@Override
	public void requestDialogs(int offset, int count,
			AsyncTaskCompleteListener<List<mDialog>> cb) {
		
		new load_dlgs_async(this.context, cb).execute(offset, count);		
	}
	
	class load_msgs_async extends AsyncTask<Integer, Void, List<mMessage>> {
	    private AsyncTaskCompleteListener<List<mMessage>> callback;
		private Context context;
		private mDialog dlg;

	    public load_msgs_async(Context context, AsyncTaskCompleteListener<List<mMessage>> cb, mDialog dialog) {
	        this.context = context;
	        this.callback = cb;
	        this.dlg = dialog;
	    }

	    protected void onPostExecute(List<mMessage> result) {
	       callback.onTaskComplete(result);
	   }

		@Override
		protected List<mMessage> doInBackground(Integer... params) {
			return getMessages(dlg, params[0], params[1]);
		}  
	}

	
	class load_dlgs_async extends AsyncTask<Integer, Void, List<mDialog>> {
	    private AsyncTaskCompleteListener<List<mDialog>> callback;
		private Context context;

	    public load_dlgs_async(Context context, AsyncTaskCompleteListener<List<mDialog>> cb) {
	        this.context = context;
	        this.callback = cb;
	    }

	    protected void onPostExecute(List<mDialog> result) {
	       callback.onTaskComplete(result);
	   }

		@Override
		protected List<mDialog> doInBackground(Integer... params) {
			return getDialogs(params[0], params[1]);
		}  
	}


	@Override
	public void setContactDataChangedCallback(
			AsyncTaskCompleteListener<Void> contact_data_changed) {
		this.contact_data_changed = contact_data_changed;		
	}

	@Override
	public void requestNewMessagesRunnable(
			AsyncTaskCompleteListener<Runnable> cb) {
		br = new SmsReceiver();	    
	    IntentFilter intFilt = new IntentFilter(SmsReceiver.ACTION);
	    context.registerReceiver(br, intFilt);
	}


	

	
	

	

}
