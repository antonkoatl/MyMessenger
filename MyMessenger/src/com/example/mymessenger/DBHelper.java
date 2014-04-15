package com.example.mymessenger;

import java.util.Collections;

import com.example.mymessenger.services.MessageService;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	public static final String dbName = "myDB";
	public static final String colId = "_id";
	public static final String colRespondent = "respondent";
	public static final String colSendtime = "send_time";
	public static final String colBody = "body";
	public static final String colFlags = "flags";
	public static final String colDlgkey = "dlg_key";
	
	public static final String colParticipants = "participants";
	public static final String colLastmsgtime = "last_msg_time";
	public static final String colSnippet = "snippet";
	
	
	MyApplication app;

	public DBHelper(Context context) {
		super(context, "myDB", null, 1);
		app = (MyApplication) context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		int ms_types[] = {MessageService.SMS, MessageService.VK };
		for(int ms_type : ms_types){
			String tn_msgs = "msgs_" + String.valueOf(ms_type);
			String tn_dlgs = "dlgs_" + String.valueOf(ms_type);
			
			db.execSQL("create table " + tn_msgs + " ("
			          + colId + " integer primary key autoincrement," 
			          + colRespondent + " text,"
			          + colSendtime + " integer,"
			          + colBody + " text,"
			          + colFlags + " integer,"
			          + colDlgkey + " integer" + ");");
			
			db.execSQL("create table " + tn_dlgs + " ("
			          + colId + " integer primary key autoincrement," 
			          + colParticipants + " text unique,"
			          + colLastmsgtime + " integer,"
			          + colSnippet + " text" + ");");
			
			db.execSQL("CREATE TRIGGER tg_dlg_" + tn_msgs
					  + " Before INSERT ON " + tn_msgs
					  + " FOR EACH ROW BEGIN"
					  + " SELECT CASE WHEN ((SELECT " + colId + " FROM " + tn_dlgs + " WHERE " + colId + " =new." + colDlgkey + " ) IS NULL)"
					  + " THEN RAISE (ABORT,'Foreign Key Violation') END;"
					  + " END");
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	
	public int getDlgId(mDialog dlg, int ms_type){
		SQLiteDatabase db = getReadableDatabase();
		
		String my_table_name = "dlgs_" + String.valueOf(ms_type);
		Collections.sort(dlg.participants);
		String selection = DBHelper.colParticipants + " = " + dlg.getParticipantsAddresses();
		Cursor c = db.query(my_table_name, null, selection, null, null, null, null);
		
		int dlg_key = 0;
		
		if(c.moveToFirst()){
			dlg_key = c.getInt( c.getColumnIndex(DBHelper.colId) );
		}
		c.close();
		
		return dlg_key;
	}

}
