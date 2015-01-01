package com.example.mymessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.example.mymessenger.attachments.mAttachment;
import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.msInterfaceMS;

import java.util.ArrayList;
import java.util.List;

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
    public static final String colAttachments = "attachments";
	
	//dialog
	public static final String colParticipants = "participants";
	public static final String colLastmsgid = "last_msg_id";
	public static final String colChatId = "chat_id";
	public static final String colTitle = "title";
	
	//contact
	public static final String colAddress = "address";
	public static final String colName = "name";
	public static final String colIcon50url = "icon_50_url";
	
	
	MyApplication app;

	public DBHelper(Context context) {
		super(context, "myDB", null, 1);
		app = (MyApplication) context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for(MessageService ms : app.msManager.myMsgServices){
            if(ms.isSetupFinished())
			    createTablesDb(ms, db);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	


	private void createTablesDb(msInterfaceMS ms, SQLiteDatabase db){
		String tn_msgs = getTableNameMsgs(ms);
		String tn_dlgs = getTableNameDlgs(ms);
		String tn_cnts = getTableNameCnts(ms);
		
		db.execSQL("create table IF NOT EXISTS " + tn_msgs + " ("
		          + colId + " integer primary key autoincrement," 
		          + colRespondent + " text,"
		          + colSendtime + " integer,"
		          + colBody + " text,"
		          + colFlags + " integer,"
		          + colMsgId + " text unique,"
                  + colAttachments + " text,"
		          + colDlgkey + " integer" + ");");
		
		db.execSQL("create table IF NOT EXISTS " + tn_dlgs + " ("
		          + colId + " integer primary key autoincrement," 
		          + colParticipants + " text,"
		          + colLastmsgid + " text,"
		          + colChatId + " integer,"
		          + colTitle + " text,"
                  + colIcon50url + " text"
		          + ");");
		
		db.execSQL("create table IF NOT EXISTS " + tn_cnts + " ("
		          + colId + " integer primary key autoincrement," 
		          + colAddress + " text unique,"
		          + colName + " text,"
		          + colIcon50url + " text" + ");");
		
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
	
	
	public String getTableNameDlgs(msInterfaceMS ms){
		return "dlgs_" + String.valueOf(ms.getServiceType()) + "_" + ms.getMyContact().address;
	}
	
	public String getTableNameMsgs(msInterfaceMS ms){
		return "msgs_" + String.valueOf(ms.getServiceType()) + "_" + ms.getMyContact().address;
	}
	
	public String getTableNameCnts(msInterfaceMS ms){
		return "cnts_" + String.valueOf(ms.getServiceType()) + "_" + ms.getMyContact().address;
	}
	
	public void insertMsg(mMessage msg, String table, long dlg_key){
		ContentValues cv = new ContentValues();
		cv.put(colRespondent, msg.respondent.address);
		cv.put(colSendtime, msg.sendTime.toMillis(false));
		cv.put(colBody, msg.text);
		cv.put(colDlgkey, dlg_key);
		cv.put(colMsgId, msg.id);
		cv.put(colFlags, msg.flags);
        cv.put(colAttachments, mAttachment.getDataString(msg.attachments));
		
		SQLiteDatabase db = getWritableDatabase();
		db.insert(table, null, cv);		
	}
	


	public int getMsgsCount(long dlg_key, MessageService ms) {
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
		
		long dlg_key = getDlgId(dlg, ms);
		
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
                msg.attachments = mAttachment.getListFromDataString(cursor.getString(cursor.getColumnIndex(colAttachments)));
	        	
	        	result.add(msg);
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
		cv.put(colIcon50url, cnt.icon_50_url);
		
		SQLiteDatabase db = getWritableDatabase();
		db.insert(table_name, null, cv);		
	}
	
	public void updateCnt(mContact cnt, MessageService ms) {
		String table_name = getTableNameCnts(ms);
		
		ContentValues cv = new ContentValues();
		//cv.put(colParticipants, dlg.getParticipantsAddresses());
		cv.put(colName, cnt.name);
		cv.put(colIcon50url, cnt.icon_50_url);
		
		getWritableDatabase().update(table_name, cv, colAddress + " = " + cnt.address, null);
		
	}

	public void loadContact(mContact cnt, MessageService ms) {
		String table_name = getTableNameCnts(ms);
		
		String selection = colAddress + " = ?";
		String selection_args[] = {cnt.address};
		Cursor cursor = getReadableDatabase().query(table_name, null, selection, selection_args, null, null, null);
		
		if(cursor.moveToFirst()){
			cnt.name = cursor.getString( cursor.getColumnIndex(colName) );
			cnt.icon_50_url = cursor.getString( cursor.getColumnIndex(colIcon50url) );
		}
		cursor.close();
	}

    public void loadCnts(List<mContact> cnts, MessageService ms) {
        String table_name = getTableNameCnts(ms);

        for(mContact cnt : cnts){
            loadCnt(cnt, ms);
        }
    }

    public boolean loadCnt(mContact cnt, MessageService ms){
        String table_name = getTableNameCnts(ms);
        String selection = colAddress + " = ?";
        String selection_args[] = {cnt.address};
        Cursor cursor = getReadableDatabase().query(table_name, null, selection, selection_args, null, null, null);
        boolean isFound = false;

        if(cursor.moveToFirst()){
            cnt.name = cursor.getString( cursor.getColumnIndex(colName) );
            cnt.icon_50_url = cursor.getString( cursor.getColumnIndex(colIcon50url) );
            isFound = true;
        }
        cursor.close();
        return isFound;
    }

	public void updateMsg(int id, mMessage msg, MessageService ms) {
		String table_name = getTableNameMsgs(ms);
		
		ContentValues cv = new ContentValues();
		//cv.put(colParticipants, dlg.getParticipantsAddresses());
		cv.put(colFlags, msg.flags);
		
		getWritableDatabase().update(table_name, cv, "_id=" + id, null);
	}

    public void updateOrInsertMsgById(String msg_id, mMessage msg, mDialog dlg, MessageService ms){
        if(getMsgByMsgId(msg_id, ms) == null){
            insertMsg(msg, getTableNameMsgs(ms), getDlgIdOrCreate(dlg, ms));
        } else {
            updateMsgById(msg_id, msg, ms);
        }
    }

    public void updateMsgById(String msg_id, mMessage msg, MessageService ms) {
        String table_name = getTableNameMsgs(ms);

        ContentValues cv = new ContentValues();
        //cv.put(colParticipants, dlg.getParticipantsAddresses());
        cv.put(colFlags, msg.flags);

        String args[] = {msg_id};
        getWritableDatabase().update(table_name, cv, colMsgId + "=?", args);
    }
	
	public mMessage getMsgByMsgId(String message_id, MessageService ms){
		mMessage msg = null;
		SQLiteDatabase db = getReadableDatabase();
		
		String table_name = getTableNameMsgs(ms);
		String selection = colMsgId + "=?";
		String sel_args[] = {message_id};

		Cursor cursor = db.query(table_name, null, selection, sel_args, null, null, null);
		
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
	



    public mContact getCnt(String address, MessageService ms) {
        String selection = colAddress + " = ?";
        String[] selectionArgs = {address};
        SQLiteDatabase db = getWritableDatabase();
        String my_table_name = getTableNameCnts(ms);

        Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);

        mContact cnt = null;

        if(c.moveToFirst()){
            cnt = loadCntFromCursor(c);
        }

        return cnt;
    }

    private mContact loadCntFromCursor(Cursor cursor) {
        mContact cnt = new mContact(cursor.getString( cursor.getColumnIndex(colAddress) ) );

        cnt.name = cursor.getString( cursor.getColumnIndex(colName) );
        cnt.icon_50_url = cursor.getString( cursor.getColumnIndex(colIcon50url) );

        return cnt;
    }



    public List<mMessage> update_db_msgs(List<mMessage> result, MessageService ms, mDialog dlg) {
        SQLiteDatabase db = getWritableDatabase();
        String my_table_name = getTableNameMsgs(ms);
        List<mMessage> msgs = new ArrayList<mMessage>();

        for (mMessage msg : result) {
            if( update_db_msg(msg, dlg, ms) ) msgs.add(msg);
        }

        return msgs;
    }

    public boolean update_db_msg(mMessage msg, mDialog dlg, MessageService ms) {
        SQLiteDatabase db = getWritableDatabase();
        long dlg_key = getDlgId(dlg, ms);
        String my_table_name = getTableNameMsgs(ms);
        String selection = DBHelper.colDlgkey + " = ? AND " + DBHelper.colSendtime + " = ? AND " + DBHelper.colBody + " = ?";
        String[] selectionArgs = { String.valueOf(dlg_key), String.valueOf(msg.sendTime.toMillis(false)), msg.text };
        Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);

        if(c.moveToFirst()){
            int  flags_in_db = c.getInt( c.getColumnIndex(DBHelper.colFlags) );

            if(msg.flags != flags_in_db){
                //update
                int id = c.getInt(c.getColumnIndex(DBHelper.colId));
                c.close();
                updateMsg(id, msg, ms);
                return true;
            } else {
                //not update
                c.close();
                return false;
            }
        } else {
            //add
            c.close();
            insertMsg(msg, my_table_name, dlg_key);
            return true;
        }
    }

    public mDialog update_db_dlg(mMessage msg, long dlg_key, MessageService ms){
        mDialog dlg = getDlgById(dlg_key, ms);
        if(dlg.getLastMessageTime().before(msg.sendTime)){
            dlg.setLastMsg(msg);
            updateDlg(dlg_key, dlg, ms);
        }
        return dlg;
    }













    // DLG
    public long getDlgId(mDialog dlg, MessageService ms){
        SQLiteDatabase db = getReadableDatabase();

        String my_table_name = getTableNameDlgs(ms);

        Cursor c;
        if(!dlg.isChat()){
            String selection = DBHelper.colParticipants + " = ?";
            String selection_args[] = {dlg.participants.get(0).address};
            c = db.query(my_table_name, null, selection, selection_args, null, null, null);
        } else {
            String selection = DBHelper.colChatId + " = ?";
            String selection_args[] = {String.valueOf(dlg.chat_id)};
            c = db.query(my_table_name, null, selection, selection_args, null, null, null);
        }


        long dlg_key = -1;

        if(c.moveToFirst()){
            dlg_key = c.getLong( c.getColumnIndex(DBHelper.colId) );
        }
        c.close();

        return dlg_key;
    }

    public long getDlgIdOrCreate(String from_id, MessageService ms) {
        SQLiteDatabase db = getReadableDatabase();

        String my_table_name = getTableNameDlgs(ms);
        String selection = DBHelper.colParticipants + " = ?";
        String selection_args[] = {from_id};
        Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);

        long dlg_key = -1;

        if(c.moveToFirst()){
            dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
        } else {
            ContentValues cv = new ContentValues();
            cv.put(colParticipants, from_id);
            SQLiteDatabase dbw = getWritableDatabase();
            dlg_key = dbw.insert(my_table_name, null, cv);

            if(dlg_key == -1){
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
            dlg.parseParticipants(cursor.getString( cursor.getColumnIndex(colParticipants) ), ms);
        }

        if(!cursor.isNull( cursor.getColumnIndex(colLastmsgid) )) {
            dlg.last_msg_id = cursor.getString( cursor.getColumnIndex(colLastmsgid) );
            dlg.last_msg = getMsgByMsgId(dlg.last_msg_id, ms);
        }

        if(!cursor.isNull( cursor.getColumnIndex(colIcon50url) )) dlg.icon_50_url = cursor.getString( cursor.getColumnIndex(colIcon50url) );

        if(!cursor.isNull( cursor.getColumnIndex(colTitle) )) dlg.title = cursor.getString( cursor.getColumnIndex(colTitle) );

        dlg.msg_service_type = ms.getServiceType();
    }

    public mDialog getDlg(mContact from, MessageService ms) {
        SQLiteDatabase db = getReadableDatabase();

        String my_table_name = getTableNameDlgs(ms);
        String selection = colParticipants + " = ? AND " + colChatId + " IS NULL";
        String selection_args[] = {from.address};
        Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);

        mDialog dlg;

        if(c.moveToFirst()){
            dlg = new mDialog();
            loadDlgFromCursor(dlg, c, ms);
        } else {
            dlg = null;
        }
        c.close();

        return dlg;
    }

    public mDialog getDlg(long chat_id, MessageService ms) {
        SQLiteDatabase db = getReadableDatabase();

        String my_table_name = getTableNameDlgs(ms);
        String selection = colChatId + " = ?";
        String selection_args[] = {String.valueOf(chat_id)};
        Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);

        mDialog dlg;

        if(c.moveToFirst()){
            dlg = new mDialog();
            loadDlgFromCursor(dlg, c, ms);
        } else {
            dlg = null;
        }
        c.close();

        return dlg;
    }

    private long getDlgIdOrCreate(mDialog dlg, MessageService ms) {
        SQLiteDatabase db = getReadableDatabase();

        String my_table_name = getTableNameDlgs(ms);
        String selection = dlg.isChat() ? DBHelper.colChatId + "=?" : DBHelper.colParticipants + "=?";
        String selection_args[] = dlg.isChat() ? new String[]{String.valueOf(dlg.chat_id)} : new String[]{dlg.participants.get(0).address};
        Cursor c = db.query(my_table_name, null, selection, selection_args, null, null, null);

        int dlg_key = -1;

        if(c.moveToFirst()){
            dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
        } else {
            insertDlg(dlg, ms);

            c.close();
            c = db.query(my_table_name, null, selection, selection_args, null, null, null);

            if(c.moveToFirst()){
                dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
            } else {
                Log.d("DBHelper", "Dlg not created!");
            }
        }
        c.close();

        return dlg_key;
    }

    public void insertDlg(mDialog dlg, MessageService ms){
        String table_name = getTableNameDlgs(ms);

        ContentValues cv = new ContentValues();
        if(!dlg.isChat() && (dlg.getParticipantsAddresses() == null || dlg.getParticipantsAddresses().length() == 0))
            Log.d("DB", "DLG ERROR");
        cv.put(colParticipants, dlg.getParticipantsAddresses());
        cv.put(colLastmsgid, dlg.last_msg_id);
        if(dlg.isChat())cv.put(colChatId, dlg.chat_id);
        cv.put(colTitle, dlg.title);
        cv.put(colIcon50url, dlg.icon_50_url);

        SQLiteDatabase db = getWritableDatabase();
        db.insert(table_name, null, cv);
    }

    public int getDlgsCount(MessageService ms) {
        int count = 0;
        /*if(ms.getServiceType() == MessageService.SMS){
            Cursor cursor = app.getApplicationContext().getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), null, null, null, null);
            count = cursor.getCount();
            cursor.close();
        } else {*/
        String table_name = getTableNameDlgs(ms);
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(table_name, null, null, null, null, null, null);
        count = c.getCount();
        c.close();

        return count;
    }

    public void updateDlg(long id, mDialog dlg, MessageService ms) {
        String table_name = getTableNameDlgs(ms);

        ContentValues cv = new ContentValues();
        cv.put(colParticipants, dlg.getParticipantsAddresses());
        cv.put(colLastmsgid, dlg.last_msg_id);
        if(dlg.isChat())cv.put(colChatId, dlg.chat_id);
        cv.put(colTitle, dlg.title);
        cv.put(colIcon50url, dlg.icon_50_url);

        getWritableDatabase().update(table_name, cv, "_id=" + id, null);

    }

    public List<mDialog> loadDlgs(MessageService ms, int count, int offset){
        List<mDialog> result = new ArrayList<mDialog>();

        SQLiteDatabase db = getReadableDatabase();

        String my_table_name = getTableNameDlgs(ms);

        Cursor cursor = db.query(my_table_name, null, null, null, null, null, null);

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

    public int getDlgIdByMsgId(String message_id, MessageService ms){
        SQLiteDatabase db = getReadableDatabase();

        String table_name = getTableNameMsgs(ms);
        String selection = colMsgId + " = " + message_id;

        Cursor cursor = db.query(table_name, null, selection, null, null, null, null);

        int dlg_key = -1;

        if(cursor.moveToFirst()){
            dlg_key = cursor.getInt( cursor.getColumnIndex(colDlgkey));
        }
        cursor.close();

        return dlg_key;
    }


    public mDialog getDlgById(long dlg_key, MessageService ms) {
        mDialog dlg = null;
        SQLiteDatabase db = getReadableDatabase();

        String table_name = getTableNameDlgs(ms);
        String selection = colId + " = " + String.valueOf(dlg_key);

        Cursor cursor = db.query(table_name, null, selection, null, null, null, null);

        if(cursor.moveToFirst()){
            dlg = new mDialog();
            loadDlgFromCursor(dlg, cursor, ms);
        }
        cursor.close();

        return dlg;
    }

    public List<mDialog> updateDlgs(List<mDialog> dlgs, MessageService ms) {
        List<mDialog> dlgs_updated = new ArrayList<mDialog>();
        for (mDialog dlg : dlgs) {
            long dlg_key = getDlgId(dlg, ms);

            if (dlg_key != 0) {
                mDialog dlg_in_db = getDlgById(dlg_key, ms);

                if (dlg.getLastMessageTime().after(dlg_in_db.getLastMessageTime())) {
                    //update
                    updateDlg(dlg_key, dlg, ms);
                    dlgs_updated.add(dlg);
                } else {
                    continue;
                }
            } else {
                //add
                insertDlg(dlg, ms);
                dlgs_updated.add(dlg);
            }
        }
        return dlgs_updated;
    }
}
