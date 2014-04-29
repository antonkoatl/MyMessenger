package com.example.mymessenger;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {
	public static final String SMS_SENT_ACTION = "mymessenger.SMS_SENT";
	public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.getAction() != null){
			Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
		    SmsMessage[] messages = new SmsMessage[pduArray.length];
		    for (int i = 0; i < pduArray.length; i++) {
		        messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
		    }

		    //Toast.makeText(context, "New SMS!", Toast.LENGTH_LONG).show();
		    
		    SmsMessage sms = messages[0];
		    
		    if (sms.getMessageClass() == SmsMessage.MessageClass.CLASS_0) {
	        //    displayClassZeroMessage(context, sms, format);
	        //    return null;
	        } else if (sms.isReplace()) {
	        //    return replaceMessage(context, msgs, error);
	        } else {
	            //storeMessage(context, messages, intent.getIntExtra("errorCode", 0)); //~~~ Store here
	        }
		    
		    /*
		    
		    ContentValues values = new ContentValues();   
		    values.put("address", messages[0].getDisplayOriginatingAddress());	              
		    if (messages.length == 1) {
	            // There is only one part, so grab the body directly.
	            values.put("body", replaceFormFeeds(messages[0].getDisplayMessageBody()));
	        } else {
	            // Build up the body from the parts.
	            StringBuilder body = new StringBuilder();
	            for (int i = 0; i < messages.length; i++) {
	                body.append(messages[i].getDisplayMessageBody());
	            }
	            values.put("body", replaceFormFeeds(body.toString()));
	        }*/
		    //context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
		    //messages[0].getStatus();
		    
		    //abortBroadcast(); //~~~ Dont abort for now
		    
		    mMessage msg = new mMessage();
		    MyApplication app = (MyApplication) context.getApplicationContext();
		    MessageService sms_service = app.getService(MessageService.SMS);
		    msg.respondent = sms_service.getContact( sms.getDisplayOriginatingAddress() );
		    
		    msg.setFlag(mMessage.OUT, false);			
			msg.text = sms.getDisplayMessageBody();
			msg.sendTime = new Time();
			msg.sendTime.set(sms.getTimestampMillis());
						
			Intent intent2 = new Intent(MsgReceiver.ACTION_RECEIVE);
		    intent2.putExtra("service_type", MessageService.SMS);
		    intent2.putExtra("msg", msg);
		    context.sendBroadcast(intent2);
		    
		    
		}
	}

	/**
     * Extract all the content values except the body from an SMS
     * message.
     */
    private ContentValues extractContentValues(SmsMessage sms) {
        // Store the message in the content provider.
        ContentValues values = new ContentValues();

        values.put("address", sms.getDisplayOriginatingAddress());

        // Use now for the timestamp to avoid confusion with clock
        // drift between the handset and the SMSC.
        // Check to make sure the system is giving us a non-bogus time.
        Calendar buildDate = new GregorianCalendar(2011, 8, 18);    // 18 Sep 2011
        Calendar nowDate = new GregorianCalendar();
        long now = System.currentTimeMillis();
        nowDate.setTimeInMillis(now);

        if (nowDate.before(buildDate)) {
            // It looks like our system clock isn't set yet because the current time right now
            // is before an arbitrary time we made this build. Instead of inserting a bogus
            // receive time in this case, use the timestamp of when the message was sent.
            now = sms.getTimestampMillis();
        }

        values.put("date", new Long(now));
        values.put("date_sent", Long.valueOf(sms.getTimestampMillis()));
        values.put("protocol", sms.getProtocolIdentifier());
        values.put("read", 1);
        values.put("seen", 1);
        if (sms.getPseudoSubject().length() > 0) {
            values.put("subject", sms.getPseudoSubject());
        }
        values.put("reply_path_present", sms.isReplyPathPresent() ? 1 : 0);
        values.put("service_center", sms.getServiceCenterAddress());
        return values;
    }
    
    private void storeMessage(Context context, SmsMessage[] msgs, int error) {
        SmsMessage sms = msgs[0];

        // Store the message in the content provider.
        ContentValues values = extractContentValues(sms);
        values.put("error_code", error);
        int pduCount = msgs.length;

        if (pduCount == 1) {
            // There is only one part, so grab the body directly.
            values.put("body", replaceFormFeeds(sms.getDisplayMessageBody()));
        } else {
            // Build up the body from the parts.
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                body.append(sms.getDisplayMessageBody());
            }
            values.put("body", replaceFormFeeds(body.toString()));
        }

        // Make sure we've got a thread id so after the insert we'll be able to delete
        // excess messages.
        Long threadId = values.getAsLong("thread_id");
        String address = values.getAsString("address");

        // Code for debugging and easy injection of short codes, non email addresses, etc.
        // See Contact.isAlphaNumber() for further comments and results.
//        switch (count++ % 8) {
//            case 0: address = "AB12"; break;
//            case 1: address = "12"; break;
//            case 2: address = "Jello123"; break;
//            case 3: address = "T-Mobile"; break;
//            case 4: address = "Mobile1"; break;
//            case 5: address = "Dogs77"; break;
//            case 6: address = "****1"; break;
//            case 7: address = "#4#5#6#"; break;
//        }

        if (TextUtils.isEmpty(address)) {
        	address = "unknown_sender";  // address = getString(R.string.unknown_sender);
            values.put("address", address);
        }

        /*
        if (((threadId == null) || (threadId == 0)) && (address != null)) {
            threadId = Threads.getOrCreateThreadId(context, address);
            values.put("thread_id", threadId);
        }*/

        ContentResolver resolver = context.getContentResolver();

        //Uri insertedUri = SqliteWrapper.insert(context, resolver, Inbox.CONTENT_URI, values);
        resolver.insert(Uri.parse("content://sms/inbox"), values);

        // Now make sure we're not over the limit in stored messages
        //Recycler.getSmsRecycler().deleteOldMessagesByThreadId(context, threadId);
        //MmsWidgetProvider.notifyDatasetChanged(context);

        //return insertedUri;
    }
    
    public static String replaceFormFeeds(String s) {
        // Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
        return s == null ? "" : s.replace('\f', '\n');
    }
}
