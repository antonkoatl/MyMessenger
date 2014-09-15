package com.example.mymessenger.services.Sms;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.format.Time;
import android.util.Log;

import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MSDBHelper;
import com.example.mymessenger.services.MessageService.MessageService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Azteki on 05.07.2014.
 */
public class MSDBHelper_sms extends MSDBHelper {
    private static MSDBHelper instance = new MSDBHelper_sms();

    public static MSDBHelper getInstance(){
        return instance;
    }

    @Override
    protected List<mDialog> load_dialogs_from_db(int count, int offset, MessageService ms) {
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
        Cursor cursor = ms.getMsApp().getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), projection, selection, selectionArgs, sortOrder);


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
                    Cursor c = ms.getMsApp().getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null, "_id = ?", new String[]{rid}, null);
                    if(c.moveToNext()){
                        mdl.participants.add( ms.getContact( check_address( c.getString(c.getColumnIndex("address")) ) ) );
                    } else {
                        for(String cn : cursor.getColumnNames()){
                            Log.d("getDialogs", cn + " : " + cursor.getString(cursor.getColumnIndex(cn)));
                        }
                        mdl.participants.add( ms.getContact("DRAFT") ); //??
                    }
                    c.close();
                }

                Cursor c = ms.getMsApp().getApplicationContext().getContentResolver().query(Uri.parse("content://sms"), null, "address=?", new String[]{mdl.participants.get(0).address}, null);
                if(c.moveToNext()){
                    mMessage msg = new mMessage();
                    String address = check_address(c.getString(c.getColumnIndex("address")));
                    msg.text = c.getString( c.getColumnIndex("body") );
                    msg.sendTime = new Time();
                    msg.sendTime.set(c.getLong( c.getColumnIndex("date") ) );
                    msg.setFlag(mMessage.READED, c.getInt( c.getColumnIndex("read") ) == 1 ? true : false);
                    msg.respondent = ms.getContact(address);
                    if (cursor.getString(c.getColumnIndex("type")).contains("1")) { //Inbox
                        msg.setFlag(mMessage.OUT, false);
                    } else if (cursor.getString(c.getColumnIndex("type")).contains("2")) { //Sent
                        msg.setFlag(mMessage.OUT, true);
                    }
                    mdl.setLastMsg(msg);
                }
                c.close();

                mdl.msg_service_type = MessageService.SMS;

                return_dialogs.add(mdl);
            } else break;
        }

        cursor.close();
        return return_dialogs;
    }

    private String check_address(String address){
        String address2 = address.replaceAll("[-() ]", "");
        if(address2.matches("\\+?\\d+(\\.\\d+)?")) {
            if (address2.charAt(0) == '8')
                address2 = "+7" + address2.substring(1);
            return address2;
        }
        else
            return address;
    }

    @Override
    protected List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset, MessageService ms) {
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
        Cursor cursor = ms.getMsApp().getApplicationContext().getContentResolver().query(Uri.parse("content://sms"), projection, selection, selectionArgs, sortOrder);

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
                msg.setFlag(mMessage.READED, cursor.getInt( cursor.getColumnIndex("read") ) == 1 ? true : false);
                msg.respondent = ms.getContact(address);
                if (cursor.getString(cursor.getColumnIndex("type")).contains("1")) { //Inbox
                    msg.setFlag(mMessage.OUT, false);
                } else if (cursor.getString(cursor.getColumnIndex("type")).contains("2")) { //Sent
                    msg.setFlag(mMessage.OUT, true);
                } else {
                    cursor.moveToNext();
                    continue;
                }
                return_msgs.add(msg);
            } else break;
        }

        //if(return_msgs.size() == 0)dl_all_msgs_downloaded = true;

        cursor.close();
        return return_msgs;
    }

    @Override
    protected void load_cnts_from_db(List<mContact> cnts, MessageService ms) {
        for(mContact cnt : cnts) {
            String name = "";

            // define the columns I want the query to return
            String[] name_projection = new String[]{
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup._ID};

            // encode the phone number and build the filter URI
            Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cnt.address));

            // query time
            Cursor name_cursor = ms.getMsApp().getApplicationContext().getContentResolver().query(contactUri, name_projection, null, null, null);

            if (name_cursor != null) {
                if (name_cursor.moveToFirst()) {
                    name = name_cursor.getString(name_cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    //Log.v("SmsService.getContactName", "Contact Found @ " + cnt.address);
                    //Log.v("SmsService.getContactName", "Contact name  = " + name);
                } else {
                    name = cnt.address;
                    //Log.v("SmsService.getContactName", "Contact Not Found @ " + cnt.address);
                }
                name_cursor.close();
            }


            cnt.name = name;
        }
    }

    @Override
    public void updateMsgInDB(mMessage msg, mDialog dlg, MessageService ms) {

    }

    @Override
    public void updateMsgInDBById(mMessage msg, String msg_id, MessageService ms) {

    }

    @Override
    public mDialog updateDlgInDB(mMessage msg, long chat_id, MessageService ms) {
        Cursor c1 = ms.getMsApp().getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), null, "address = ?", new String[]{msg.respondent.address}, null);
        String rid = null;
        if(c1.moveToNext()) {
            rid = c1.getString(c1.getColumnIndex("_id"));
        }

        c1.close();

        if(rid == null)return null;

        String[] projection = null; // A list of which columns to return. Passing null will return all columns, which is inefficient.
        String selection = "recipient_ids=?"; // A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given URI
        String[] selectionArgs = {rid}; // You may include ?s in selection, which will be replaced by the values from selectionArgs, in the order that they appear in the selection. The values will be bound as Strings
        String sortOrder = null; // How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered
        Cursor cursor = ms.getMsApp().getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), projection, selection, selectionArgs, sortOrder);

        mDialog dlg = null;

        if(cursor.moveToNext()) {
            dlg = new mDialog();

            dlg.participants.add(ms.getContactCheckDB(msg.respondent.address));
            // TODO: get last msg
            dlg.msg_service_type = MessageService.SMS;
        }

        cursor.close();

        return dlg;
    }
}
