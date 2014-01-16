package com.example.mymessenger.services;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public class SmsService implements MessageService {
	private Context context;
	private List<mDialog> dialogs;
	public String self_name;
	
	public SmsService(Context context) {
		this.context = context;
		self_name = "Me";
		dialogs = new ArrayList<mDialog>();
	}
	
	@Override
	public List<mDialog> getDialogs() {
		Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
		
		int totalSms = cursor.getCount();
		
		if(cursor.moveToFirst()){
			for (int i = 0; i < totalSms; i++) {
				mMessage msg = new mMessage();

				String address = cursor.getString( cursor.getColumnIndex("address") );
				
				msg.text = cursor.getString( cursor.getColumnIndex("body") );
				msg.sendTime = new Time();
				msg.sendTime.set(cursor.getLong( cursor.getColumnIndex("date") ) );
				msg.ReadState = cursor.getString( cursor.getColumnIndex("read") );
				if (cursor.getString(cursor.getColumnIndex("type")).contains("1")) { //Inbox
					msg.sender = address;
					msg.address = self_name;
	            } else if (cursor.getString(cursor.getColumnIndex("type")).contains("4")) { //Sent
	            	msg.sender = self_name;
					msg.address = address;
	            } else {
	            	cursor.moveToNext();
	            	continue;
	            }
				
				boolean fl = false;
				for(int j = 0; j < dialogs.size(); j++){					
					for(int k = 0; k < dialogs.get(j).participants.size(); k++){
						if(dialogs.get(j).participants.get(k).equals(address)){
							dialogs.get(j).messages.add(msg);
							fl = true;
						}
						if(fl)break;
					}
					if(fl)break;
				}
				
				if(!fl){
					mDialog t = new mDialog();
					t.participants.add(address);
					t.messages.add(msg);
					dialogs.add(t);
				}
				
				cursor.moveToNext();
			} 
		}
		
		return dialogs;
	}

	@Override
	public String getName() {
		return "Sms";
	}

}
