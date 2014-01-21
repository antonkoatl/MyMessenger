package com.example.mymessenger.services;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public class SmsService implements MessageService {
	private Context context;
	private List<mDialog> dialogs;
	public String self_name;
	public mDialog active_dialog;
	
	public SmsService(Context context) {
		this.context = context;
		self_name = "Me";
		dialogs = new ArrayList<mDialog>();
	}
	
	@Override
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
						mdl.participants.add( c.getString( c.getColumnIndex("address") ) );
					} else {
						for(String cn : cursor.getColumnNames()){
							Log.d("getDialogs", cn + " : " + cursor.getString(cursor.getColumnIndex(cn)) );
						}
						mdl.participants.add( "DRAFT" ); //??
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
	public String getName() {
		return "Sms";
	}

	@Override
	public List<mMessage> getMessages(String user_id, int offset, int count) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getType() {
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

	@Override
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
		String[] selectionArgs = {dlg.participants.get(0)}; // You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings
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
					msg.sender = address;
					msg.address = self_name;
	            } else if (cursor.getString(cursor.getColumnIndex("type")).contains("2")) { //Sent
	            	msg.sender = self_name;
					msg.address = address;
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
	public String getMyName() {
		return self_name;
	}

}
