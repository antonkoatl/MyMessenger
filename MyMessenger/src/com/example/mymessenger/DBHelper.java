package com.example.mymessenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.services.VKRequestListenerWithCallback;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	public static final String dbName = "myDB";
	
	//message
	public static final String colId = "_id";
	public static final String colRespondent = "respondent";
	public static final String colSendtime = "send_time";
	public static final String colBody = "body";
	public static final String colFlags = "flags";
	public static final String colDlgkey = "dlg_key";
	public static final String colMsgId = "msg_id";
	
	//dialog
	public static final String colParticipants = "participants";
	public static final String colLastmsgtime = "last_msg_time";
	public static final String colSnippet = "snippet";
	public static final String colSnippetOut = "snippet_out";
	public static final String colChatId = "chat_id";
	public static final String colTitle = "title";
	
	//contact
	public static final String colAddress = "address";
	public static final String colName = "name";
	public static final String colIcon100url = "icon_100_url";
	
	
	MyApplication app;

	public DBHelper(Context context) {
		super(context, "myDB", null, 1);
		app = (MyApplication) context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for(MessageService ms : app.myMsgServices){
			createTablesDb(ms, db);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	
	public int getDlgId(mDialog dlg, MessageService ms){
		SQLiteDatabase db = getReadableDatabase();
		
		String my_table_name = getTableNameDlgs(ms);
		
		Cursor c;
		if(dlg.chat_id == 0){
			String selection = DBHelper.colParticipants + " = ?";
			String selection_args[] = {dlg.participants.get(0).address};
			c = db.query(my_table_name, null, selection, selection_args, null, null, null);
		} else {
			String selection = DBHelper.colChatId + " = ?";
			String selection_args[] = {String.valueOf(dlg.chat_id)};
			c = db.query(my_table_name, null, selection, selection_args, null, null, null);
		}
		
		
		int dlg_key = 0;
		
		if(c.moveToFirst()){
			dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
		}
		c.close();
		
		return dlg_key;
	}
	
	public int getDlgIdOrCreate(String from_id, MessageService ms) {
		SQLiteDatabase db = getReadableDatabase();
		
		String my_table_name = getTableNameDlgs(ms);
		String selection = DBHelper.colParticipants + " = ?";
		String selection_args[] = {from_id};
		Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);
		
		int dlg_key = 0;
		
		if(c.moveToFirst()){
			dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
		} else {
			ContentValues cv = new ContentValues();
			cv.put(colParticipants, from_id);
			SQLiteDatabase dbw = getWritableDatabase();
			dbw.insert(my_table_name, null, cv);
			
			if(c.moveToFirst()){
				dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
			} else {
				Log.d("DBHelper", "Dlg not created!");
			}
		}
		c.close();
		
		return dlg_key;
	}
	
	public void loadDlgFromCursor(mDialog dlg, Cursor cursor, MessageService ms){
		if(cursor.isNull( cursor.getColumnIndex(colChatId) )){
			dlg.participants.add( ms.getContact( cursor.getString( cursor.getColumnIndex(colParticipants) ) ) );
		} else {
			dlg.chat_id = cursor.getLong( cursor.getColumnIndex(colChatId) );
			String[] ps = cursor.getString( cursor.getColumnIndex(colParticipants) ).split(",");
			for(String part : ps) dlg.participants.add( ms.getContact( part ) );
		}
    	
    	if(!cursor.isNull( cursor.getColumnIndex(colLastmsgtime) )) dlg.last_msg_time.set( cursor.getLong( cursor.getColumnIndex(colLastmsgtime) ) );
    	if(!cursor.isNull( cursor.getColumnIndex(colSnippet) )) dlg.snippet = cursor.getString( cursor.getColumnIndex(colSnippet) );
    	if(!cursor.isNull( cursor.getColumnIndex(colSnippetOut) )) dlg.snippet_out = cursor.getInt( cursor.getColumnIndex(colSnippetOut) );	        	
    	if(!cursor.isNull( cursor.getColumnIndex(colTitle) )) dlg.title = cursor.getString( cursor.getColumnIndex(colTitle) );
    	
    	dlg.msg_service_type = ms.getServiceType();
	}
	
	public mDialog getDlg(String from_id, MessageService ms) {
		SQLiteDatabase db = getReadableDatabase();
		
		String my_table_name = getTableNameDlgs(ms);
		String selection = colParticipants + " = ? AND " + colChatId + " IS NULL";
		String selection_args[] = {from_id};
		Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);
		
		int dlg_key = 0;
		mDialog dlg;
		
		if(c.moveToFirst()){
			dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
			dlg = new mDialog();
			loadDlgFromCursor(dlg, c, ms);
		} else {
			dlg = null;
			/*dlg = new mDialog();
			dlg.participants.add( ms.getContact(from_id) );			
			insertDlg(dlg, ms);
			
			if(c.moveToFirst()){
				dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
			} else {
				Log.d("DBHelper", "Dlg not created!");
			}*/
		}
		c.close();
		
		return dlg;
	}
	
	public int getDlgIdOrCreate(long chat_id, MessageService ms) {
		SQLiteDatabase db = getReadableDatabase();
		
		String my_table_name = getTableNameDlgs(ms);
		String selection = DBHelper.colChatId + " = ?";
		String selection_args[] = {String.valueOf(chat_id)};
		Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);
		
		int dlg_key = 0;
		
		if(c.moveToFirst()){
			dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
		} else {
			ContentValues cv = new ContentValues();
			cv.put(colChatId, chat_id);
			SQLiteDatabase dbw = getWritableDatabase();
			dbw.insert(my_table_name, null, cv);
			
			if(c.moveToFirst()){
				dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
			} else {
				Log.d("DBHelper", "Dlg not created!");
			}
		}
		c.close();
		
		return dlg_key;
	}

	private void createTablesDb(MessageService ms, SQLiteDatabase db){
		String tn_msgs = getTableNameMsgs(ms);
		String tn_dlgs = getTableNameDlgs(ms);
		String tn_cnts = getTableNameCnts(ms);
		
		db.execSQL("create table IF NOT EXISTS " + tn_msgs + " ("
		          + colId + " integer primary key autoincrement," 
		          + colRespondent + " text,"
		          + colSendtime + " integer,"
		          + colBody + " text,"
		          + colFlags + " integer,"
		          + colMsgId + " text,"
		          + colDlgkey + " integer" + ");");
		
		db.execSQL("create table IF NOT EXISTS " + tn_dlgs + " ("
		          + colId + " integer primary key autoincrement," 
		          + colParticipants + " text unique,"
		          + colLastmsgtime + " integer,"
		          + colSnippet + " text,"
		          + colSnippetOut + " integer,"
		          + colChatId + " integer,"
		          + colTitle + " text"
		          + ");");
		
		db.execSQL("create table IF NOT EXISTS " + tn_cnts + " ("
		          + colId + " integer primary key autoincrement," 
		          + colAddress + " text unique,"
		          + colName + " text,"
		          + colIcon100url + " text" + ");");
		
		db.execSQL("CREATE TRIGGER IF NOT EXISTS tg_dlg_" + tn_msgs
				  + " Before INSERT ON " + tn_msgs
				  + " FOR EACH ROW BEGIN"
				  + " SELECT CASE WHEN ((SELECT " + colId + " FROM " + tn_dlgs + " WHERE " + colId + " =new." + colDlgkey + " ) IS NULL)"
				  + " THEN RAISE (ABORT,'Foreign Key Violation') END;"
				  + " END");
	}
	
	public void createTables(MessageService ms){
		SQLiteDatabase db = getWritableDatabase();
		createTablesDb(ms, db);
	}
	
	
	public String getTableNameDlgs(MessageService ms){
		return "dlgs_" + String.valueOf(ms.getServiceType()) + "_" + ms.getMyContact().address;
	}
	
	public String getTableNameMsgs(MessageService ms){
		return "msgs_" + String.valueOf(ms.getServiceType()) + "_" + ms.getMyContact().address;
	}
	
	public String getTableNameCnts(MessageService ms){
		return "cnts_" + String.valueOf(ms.getServiceType()) + "_" + ms.getMyContact().address;
	}
	
	public void insertMsg(mMessage msg, String table, int dlg_key){
		ContentValues cv = new ContentValues();
		cv.put(colRespondent, msg.respondent.address);
		cv.put(colSendtime, msg.sendTime.toMillis(false));
		cv.put(colBody, msg.text);
		cv.put(colDlgkey, dlg_key);
		cv.put(colMsgId, msg.id);
		cv.put(colFlags, msg.flags);
		
		SQLiteDatabase db = getWritableDatabase();
		db.insert(table, null, cv);		
	}
	
	public void insertDlg(mDialog dlg, MessageService ms){
		String table_name = getTableNameDlgs(ms);
		
		ContentValues cv = new ContentValues();
		cv.put(colParticipants, dlg.getParticipantsAddresses());
		cv.put(colLastmsgtime, dlg.last_msg_time.toMillis(false));
		cv.put(colSnippet, dlg.snippet);
		cv.put(colSnippetOut, dlg.snippet_out);
		if(dlg.chat_id != 0)cv.put(colChatId, dlg.chat_id);
		cv.put(colTitle, dlg.title);
		
		SQLiteDatabase db = getWritableDatabase();
		db.insert(table_name, null, cv);	
	}

	public int getMsgsCount(int dlg_key, MessageService ms) {
		String table_name = getTableNameMsgs(ms);
		SQLiteDatabase db = getReadableDatabase();
		
		String selection = colDlgkey + " = " + dlg_key;
		Cursor c = db.query(table_name, null, selection, null, null, null, null);
		int count = c.getCount();
		c.close();
		return count;
	}
	
	public int getMsgsCount(mDialog dlg, MessageService ms) {
		return getMsgsCount(getDlgId(dlg, ms), ms);
	}
	
	public List<mMessage> loadMsgs(MessageService ms, mDialog dlg, int count, int offset){
		List<mMessage> result = new ArrayList<mMessage>();
		
		int dlg_key = getDlgId(dlg, ms);
		
		SQLiteDatabase db = getReadableDatabase();
		
		String table_name = getTableNameMsgs(ms);
		String selection = colDlgkey + " = " + String.valueOf(dlg_key);
		String order_by = colSendtime + " DESC";		
		
		Cursor cursor = db.query(table_name, null, selection, null, null, null, order_by);
		
		if(!cursor.moveToFirst()){return result;}
		
		boolean cursor_chk = true;
		for (int i = 0; i < offset; i++) cursor_chk = cursor.moveToNext();
		
		for (int i = 0; i < count; i++) {
			if(cursor_chk){
	        	mMessage msg = new mMessage();
	        	msg.respondent = ms.getContact( cursor.getString( cursor.getColumnIndex(colRespondent) ) );
	        	msg.sendTime.set( cursor.getLong(cursor.getColumnIndex(colSendtime)) );
	        	msg.text = cursor.getString(cursor.getColumnIndex(colBody));
	        	msg.flags = cursor.getInt(cursor.getColumnIndex(colFlags));
	        	msg.id = cursor.getString(cursor.getColumnIndex(colMsgId));
	        	msg.msg_service = ms.getServiceType();
	        	
	        	result.add(msg);
	        	cursor_chk = cursor.moveToNext();
	        } 
	    }
		
		cursor.close();
		
		return result;
		
	}

	public int getDlgsCount(MessageService ms) {
		int count = 0;
		if(ms.getServiceType() == MessageService.SMS){
			Cursor cursor = app.getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), null, null, null, null);
			count = cursor.getCount();
			cursor.close();
		} else {
			String table_name = getTableNameDlgs(ms);
			SQLiteDatabase db = getReadableDatabase();
			
			Cursor c = db.query(table_name, null, null, null, null, null, null);
			count = c.getCount();
			c.close();
		}
		
		return count;
	}

	public void updateDlg(int id, mDialog dlg, MessageService ms) {
		String table_name = getTableNameDlgs(ms);
		
		ContentValues cv = new ContentValues();
		cv.put(colParticipants, dlg.getParticipantsAddresses());
		cv.put(colLastmsgtime, dlg.last_msg_time.toMillis(false));
		cv.put(colSnippet, dlg.snippet);
		cv.put(colSnippetOut, dlg.snippet_out);
		if(dlg.chat_id != 0)cv.put(colChatId, dlg.chat_id);
		cv.put(colTitle, dlg.title);
		
		getWritableDatabase().update(table_name, cv, "_id=" + id, null);
		
	}
	
	public List<mDialog> loadDlgs(MessageService ms, int count, int offset){
		List<mDialog> result = new ArrayList<mDialog>();
		
		SQLiteDatabase db = getReadableDatabase();
		
		String my_table_name = getTableNameDlgs(ms);
		String order_by = colLastmsgtime + " DESC";
		
		Cursor cursor = db.query(my_table_name, null, null, null, null, null, order_by);
		
		if(!cursor.moveToFirst()){return result;}
		
		boolean cursor_chk = true;
		for (int i = 0; i < offset; i++) cursor_chk = cursor.moveToNext();
						
		for (int i = 0; i < count; i++) {
			if(cursor_chk){
				mDialog dlg = new mDialog();
				loadDlgFromCursor(dlg, cursor, ms);
	        	
	        	result.add(dlg);
	        	cursor_chk = cursor.moveToNext();

	        }
	    }
		
		cursor.close();
		
		return result;
	}
	
	public void insertCnt(mContact cnt,	MessageService ms) {
		String table_name = getTableNameCnts(ms);
		
		ContentValues cv = new ContentValues();
		cv.put(colAddress, cnt.address);
		cv.put(colName, cnt.name);
		cv.put(colIcon100url, cnt.icon_100_url);
		
		SQLiteDatabase db = getWritableDatabase();
		db.insert(table_name, null, cv);		
	}
	
	public void updateCnt(mContact cnt, MessageService ms) {
		String table_name = getTableNameCnts(ms);
		
		ContentValues cv = new ContentValues();
		//cv.put(colParticipants, dlg.getParticipantsAddresses());
		cv.put(colName, cnt.name);
		cv.put(colIcon100url, cnt.icon_100_url);
		
		getWritableDatabase().update(table_name, cv, colAddress + " = " + cnt.address, null);
		
	}

	public void loadContact(mContact cnt, MessageService ms) {
		String table_name = getTableNameCnts(ms);
		
		String selection = colAddress + " = ?";
		String selection_args[] = {cnt.address};
		Cursor cursor = getReadableDatabase().query(table_name, null, selection, selection_args, null, null, null);
		
		if(cursor.moveToFirst()){
			cnt.name = cursor.getString( cursor.getColumnIndex(colName) );
			cnt.icon_100_url = cursor.getString( cursor.getColumnIndex(colIcon100url) );
		}
		cursor.close();
	}

	public void updateMsg(int id, mMessage msg, MessageService ms) {
		String table_name = getTableNameMsgs(ms);
		
		ContentValues cv = new ContentValues();
		//cv.put(colParticipants, dlg.getParticipantsAddresses());
		cv.put(colFlags, msg.flags);
		
		getWritableDatabase().update(table_name, cv, "_id=" + id, null);
	}
	
	public mMessage getMsgByMsgId(int message_id, MessageService ms){
		mMessage msg = null;
		SQLiteDatabase db = getReadableDatabase();
		
		String table_name = getTableNameMsgs(ms);
		String selection = colMsgId + " = " + String.valueOf(message_id);
		
		Cursor cursor = db.query(table_name, null, selection, null, null, null, null);
		
		if(cursor.moveToFirst()){
			msg = new mMessage();
			msg.respondent = ms.getContact( cursor.getString( cursor.getColumnIndex(colRespondent) ) );
			msg.sendTime.set( cursor.getLong( cursor.getColumnIndex(colSendtime) ) );
			msg.text = cursor.getString(cursor.getColumnIndex(colBody));
        	msg.flags = cursor.getInt(cursor.getColumnIndex(colFlags));
        	msg.id = cursor.getString(cursor.getColumnIndex(colMsgId));
        	msg.msg_service = ms.getServiceType();
		}
		cursor.close();
		
		return msg;
	}
	
	public int getDlgIdByMsgId(int message_id, MessageService ms){
		SQLiteDatabase db = getReadableDatabase();
		
		String table_name = getTableNameMsgs(ms);
		String selection = colMsgId + " = " + String.valueOf(message_id);
		
		Cursor cursor = db.query(table_name, null, selection, null, null, null, null);
		
		int dlg_key = -1;
		
		if(cursor.moveToFirst()){
			dlg_key = cursor.getInt( cursor.getColumnIndex(colDlgkey));
		}
		cursor.close();
		
		return dlg_key;		
	}

	
	public mDialog getDlgById(int dlg_key, MessageService ms) {
		mDialog dlg = null;
		SQLiteDatabase db = getReadableDatabase();
		
		String table_name = getTableNameDlgs(ms);
		String selection = colId + " = " + String.valueOf(dlg_key);
		
		Cursor cursor = db.query(table_name, null, selection, null, null, null, null);
		
		if(cursor.moveToFirst()){
			dlg = new mDialog();
			if(cursor.isNull(cursor.getColumnIndex(colChatId))){
				dlg.participants.add( ms.getContact( cursor.getString( cursor.getColumnIndex(colParticipants) ) ) );
			} else {
				dlg.chat_id = cursor.getLong( cursor.getColumnIndex(colChatId) );
				String[] ps = cursor.getString( cursor.getColumnIndex(colParticipants) ).split(",");
				for(String part : ps) dlg.participants.add( ms.getContact( part ) );
			}
        	if(!cursor.isNull( cursor.getColumnIndex(colLastmsgtime) )) dlg.last_msg_time.set( cursor.getLong(cursor.getColumnIndex(colLastmsgtime)) );
        	if(!cursor.isNull( cursor.getColumnIndex(colSnippet) )) dlg.snippet = cursor.getString(cursor.getColumnIndex(colSnippet));
        	if(!cursor.isNull( cursor.getColumnIndex(colSnippetOut) )) dlg.snippet_out = cursor.getInt(cursor.getColumnIndex(colSnippetOut));
        	if(!cursor.isNull( cursor.getColumnIndex(colTitle) )) dlg.title = cursor.getString(cursor.getColumnIndex(colTitle));
        	
        	dlg.msg_service_type = ms.getServiceType();
		}
		cursor.close();
		
		return dlg;
	}

	
}
