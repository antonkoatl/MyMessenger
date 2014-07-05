package com.example.mymessenger.services;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.mymessenger.ActivityTwo;
import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MsgReceiver;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.SmsReceiver;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

import java.util.ArrayList;
import java.util.List;

public class Sms extends MessageService {
    private PendingIntent mSentIntent;
    private PendingIntent mDeliveredIntent;

    public Sms(MyApplication app) {
        super(app, SMS, R.string.service_name_sms);

        mSentIntent = PendingIntent.getBroadcast(app.getApplicationContext(), 0, new Intent("CTS_SMS_SEND_ACTION"),
                PendingIntent.FLAG_ONE_SHOT);
        mDeliveredIntent = PendingIntent.getBroadcast(app.getApplicationContext(), 0, new Intent("CTS_SMS_DELIVERY_ACTION"),
                PendingIntent.FLAG_ONE_SHOT);

        /*Cursor cursor = app.getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), null, null, null, null);
        dlgs_count = cursor.getCount();
        cursor.close();*/
    }

    @Override
    protected void setupDBHelper(){
        msDBHelper = new MSDBHelper_sms(this);
    }

    @Override
    protected void logout_from_net() {

    }

    @Override
    public void authorize(Context context) {

    }

    @Override
    protected void requestAccountInfoFromNet(AsyncTaskCompleteListener<mContact> cb) {
        mContact cnt = new mContact("");
        cnt.name = "NOWAYTOGETTHIS";
    }










    class mSendReceiver extends BroadcastReceiver {
        mMessage msg;

        mSendReceiver(mMessage msg){
            this.msg = msg;
        }

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

            msg.setFlag(mMessage.OUT, true);

            Intent intent2 = new Intent(MsgReceiver.ACTION_RECEIVE);
            intent2.putExtra("service_type", getServiceType());
            intent2.putExtra("msg", msg);
            context.sendBroadcast(intent2);

            context.unregisterReceiver(this);

        }

    };

    class mDeliveryReceiver extends BroadcastReceiver {
        mMessage msg;

        mDeliveryReceiver(mMessage msg){
            this.msg = msg;
        }

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

            context.unregisterReceiver(this);
        }

    };


    @Override
    public boolean sendMessage(String address, String text) {
        mMessage msg = new mMessage();
        msg.text = text;
        msg.respondent = getContact(address);

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(address, null, text, mSentIntent, mDeliveredIntent);

        msApp.getApplicationContext().registerReceiver(new mSendReceiver(msg), new IntentFilter("CTS_SMS_SEND_ACTION"));
        msApp.getApplicationContext().registerReceiver(new mDeliveryReceiver(msg), new IntentFilter("CTS_SMS_DELIVERY_ACTION"));

        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("body", text);
        msApp.getApplicationContext().getContentResolver().insert(Uri.parse("content://sms/sent"), values);

        return false;

        //ArrayList<String> parts = smsManager.divideMessage(message);
        //smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
    }

    @Override
    public String[] getStringsForMainViewMenu() {
        String data[] = {"---", "New message", "All messages"};
        List<mDialog> t = msDBHelper.load_dialogs_from_db(1, 0);
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
                openActiveDlg();
                break;
            case 1:
                openContacts(con);
                break;
            case 2:
                openDialogs();
                break;
        }
    }

    @Override
    protected void getContactsFromNet(CntsDownloadsRequest req) {

    }

    @Override
    public void requestNewMessagesRunnable(
            AsyncTaskCompleteListener<RunnableAdvanced<?>> cb) {
        BroadcastReceiver br = new SmsReceiver();
        IntentFilter intFilt = new IntentFilter();
        intFilt.addAction(SmsReceiver.SMS_SENT_ACTION);
        intFilt.addAction(SmsReceiver.SMS_RECEIVED_ACTION);
        msApp.getApplicationContext().registerReceiver(br, intFilt);
    }

    @Override
    public void requestContacts(int offset, int count,
                                AsyncTaskCompleteListener<List<mContact>> cb) {
        List<mContact> cnts = new ArrayList<mContact>();

        // define the columns I want the query to return
        String[] name_projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID};

        // query time
        Cursor name_cursor = msApp.getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.PhoneLookup.DISPLAY_NAME + " ASC");

        if(name_cursor.moveToFirst()){
            for (int i = 0; i < offset; i++) name_cursor.moveToNext();
            name_cursor.moveToPrevious();
        }

        for (int i = 0; i < count; i++) {
            if (name_cursor.moveToNext()) {
                //if(name_cursor.getInt( name_cursor.getColumnIndex( ContactsContract.Contacts.HAS_PHONE_NUMBER ) ) == 0)continue;

                mContact cnt = new mContact( name_cursor.getString( name_cursor.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER ) ) );
                cnt.name = name_cursor.getString(name_cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                cnts.add(cnt);
            }
        }
        name_cursor.close();

        cb.onTaskComplete(cnts);

    }


    @Override
    public void unsetup() {
        // TODO Auto-generated method stub

    }

    @Override
    public void requestMarkAsReaded(mMessage msg, mDialog dlg) {
        // TODO Auto-generated method stub

    }


    @Override
    protected void getDialogsFromNet(DlgsDownloadsRequest req) {

    }

    @Override
    protected void getMessagesFromNet(MsgsDownloadsRequest req) {

    }

    @Override
    public int[] getEmojiGroupsIcons() {
        return new int[0];
    }

    @Override
    public String getEmojiUrl(long code) {
        return null;
    }

    @Override
    public long[][] getEmojiCodes(){
        return new long[0][0];
    }


}
