package com.example.mymessenger.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.DBHelper;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public abstract class MessageService {
	public static final int SMS = 10;
	public static final int VK = 11;
	
	public static final int MSGS_DOWNLOAD_COUNT = 20;
	public static final int DLGS_DOWNLOAD_COUNT = 20;
	
	protected MyApplication msApp;
	protected mContact self_contact;
	private mDialog active_dlg;
	protected Map<String, mContact> contacts;
	protected boolean authorised = false;
	protected String service_name = "service_name";
	protected int service_type;
	
	protected int dlgs_count;
	protected boolean dl_all_dlgs_downloaded = false; //Все диалоги загружены из сети
	protected mDialog dl_current_dlg; //При загрузке сообщений для одного диалога, показывает что все загружены
	protected boolean dl_all_msgs_downloaded = false;
	protected boolean dl_all_new_msgs_downloaded = false;
	protected int dlgs_thread_count = 0; //Количество потоков, загружающих диалоги в данных момент
	protected Map<mDialog, IntegerMutable> msgs_thread_count; //Количество потоков, загружающих сообщения для определённого диалога в данных момент
	protected SharedPreferences sPref;
	
	List<mDialog> return_dialogs;
	List<mMessage> return_msgs;
		
	class IntegerMutable {
	    public int value;
	    
	    public IntegerMutable(int i) {
	    	this.value = i;
		}

		@Override
	    public boolean equals(Object that){
	    	if(that instanceof IntegerMutable){
	    		IntegerMutable toCompare = (IntegerMutable) that;
	    		return this.value == toCompare.value;
			}
			return false;
	    }
	}
	
	protected MessageService(MyApplication app){
		this.msApp = app;
		contacts = new HashMap<String, mContact>();
		msgs_thread_count = new HashMap<mDialog, IntegerMutable>(); //индикаторы загрузки сообщений для диалогов
	}
	

	//Запросить данные - может быть возвращено 2 раза (из бд, затем из интернета)	
	/* Если данные загружаются из интернета, то алгоритм таков:
	 * 1. Возвращаются данные из бд
	 * 2. Загружаются данные из интернета
	 * 3. Проверка являются ли загруженные данные новыми
	 * 4. Возвращение новых данных, обновление бд
	 * 5. Если все загруженные данные - новые, то продолжить обновление пока количество обновлённых данных не достигнет количества данных в бд
	 *  * При дополнительной загрузке данных для обновления данные не возвращаются, только обновляется бд
	 */
	protected abstract void getMessagesFromDB(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb);
	protected abstract void getMessagesFromNet(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb);
	
	protected abstract void getDialogsFromDB(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb);
	protected abstract void getDialogsFromNet(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb);
	
	public abstract void requestContactData(mContact cnt);
	public abstract void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);
	public abstract void requestMarkAsReaded(mMessage msg);
	
	public abstract void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb); //Запросить алгоритм для отслеживания новых сообщений	
	public abstract void setup(AsyncTaskCompleteListener<MessageService> asms); //Подготовить сервис для работы
	public abstract void init(); //Инициализация, после авторизации
	public abstract void unsetup(); //Удалить сервис
	public abstract long[][] getEmojiCodes();
	public abstract int[] getEmojiGroupsIcons();
	
	//Служебные функции
	public final void requestDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
		int dlgs_in_db = msApp.dbHelper.getDlgsCount(MessageService.this);
		
		if(offset + count < dlgs_in_db){
			getDialogsFromDB(count, offset, cb);
			refreshDialogsFromNet(cb, 0);
		} else {
			getDialogsFromDB(count, offset, cb);
			msUpdateDlgsDB_cb up_cb = new msUpdateDlgsDB_cb(cb);
			getDialogsFromNet(count, offset, up_cb);
		}		
	}

	public final void refreshDialogsFromNet(AsyncTaskCompleteListener<List<mDialog>> cb, int count) {		
		msRefreshDlgsCb.addRefresh(cb, count);		
	}
	
	public final void requestMessages(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb){
		int msgs_in_db = msApp.dbHelper.getMsgsCount(dlg, MessageService.this);
		
		if(offset + count < msgs_in_db){
			getMessagesFromDB(dlg, count, offset, cb);
			refreshMessagesFromNet(dlg, cb, 0);
		} else {
			getMessagesFromDB(dlg, count, offset, cb);
			msUpdateMsgsDB_cb up_cb = new msUpdateMsgsDB_cb(dlg, cb);
			getMessagesFromNet(dlg, count, offset, up_cb);
		}		
	}
	
	public final void refreshMessagesFromNet(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb, int count) {		
		msRefreshMsgsCb.addRefresh(dlg, cb, count);		
	}
	
	
	public final String getServiceName() {
		return service_name;
	}
	
	public final int getServiceType() {
		return service_type;
	}
		
	public final mDialog getActiveDialog() {
		return active_dlg;
	}
	
	public final mContact getMyContact() {
		return self_contact;
	}
	
	public final mContact getContact(String address) {
		mContact cnt = contacts.get(address);
		
		if(cnt == null){
			cnt = new mContact(address);
			
			requestContactData(cnt);
			
			contacts.put(address, cnt);
		}
		
		return cnt;
	}
	
	public final boolean isAllMsgsDownloaded() {
		return dl_all_msgs_downloaded;
	}
	
	public final boolean isLoadingDlgs() {
		return dlgs_thread_count > 0;
	}
	
	public final boolean isLoadingMsgsForDlg(mDialog dlg) {
		IntegerMutable count = msgs_thread_count.get(dlg); 
		if(count == null)return false;
		if(count.value == 0){
			msgs_thread_count.remove(dlg);
			return false;
		}
		return true;
	}
	
	public final void refresh() { //сбросить все индикаторы завершения загрузок
		dl_all_dlgs_downloaded = false;		
	}
	
	
	public final void setActiveDialog(mDialog dlg) {
		active_dlg = dlg;
		msApp.sPref.edit().putString("active_dialog", dlg.getParticipantsAddresses()).commit();
	}
			
	//отправка сообщения
	public abstract boolean sendMessage(String address, String text);
	
	//функции для интерфейса
	public abstract String[] getStringsForMainViewMenu();
	public abstract void MainViewMenu_click(int which, Context context);

	

	protected void updateMsgsThreadCount(mDialog dlg, int count){
		IntegerMutable lm_count = msgs_thread_count.get(dlg);
    	if(lm_count == null){
    		lm_count = new IntegerMutable(count);
    		msgs_thread_count.put(dlg, lm_count);
    	}
    	else lm_count.value += count;
	}
	
	protected class msUpdateDlgsDB_cb implements AsyncTaskCompleteListener<List<mDialog>>{
    	AsyncTaskCompleteListener<List<mDialog>> cb;

		public msUpdateDlgsDB_cb(AsyncTaskCompleteListener<List<mDialog>> cb) {
			this.cb = cb;
		}

		@Override
		public void onTaskComplete(List<mDialog> result) {
			SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();
    		String my_table_name = msApp.dbHelper.getTableNameDlgs(MessageService.this);
    		List<mDialog> dlgs = new ArrayList<mDialog>();
    		
    		for (mDialog mdl : result) {
    			String selection = DBHelper.colParticipants + " = ?";
    			String[] selectionArgs = {mdl.getParticipantsAddresses()};
    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
    			
    			if(c.moveToFirst()){
    				Time last_time_in_db = new Time();
    				last_time_in_db.set( c.getLong( c.getColumnIndex(DBHelper.colLastmsgtime)) );
    				
    				if(mdl.last_msg_time.after(last_time_in_db)){
    					//update
    					int id = c.getInt(c.getColumnIndex(DBHelper.colId));
    					c.close();
	    				
    					msApp.dbHelper.updateDlg(id, mdl, MessageService.this);			    					
	    							    				
    					dlgs.add(mdl);
    				} else {
    					//not update
    					c.close();
    					continue;
    				}		    				
    			} else {
    				//add
    				c.close();
    				
    				msApp.dbHelper.insertDlg(mdl, MessageService.this);
    					    				
    				dlgs.add(mdl);
    				dlgs_count++;
    			}
    			
    			if(cb != null){    				
    				cb.onTaskComplete(dlgs);
	    		}
    		}
		}
    	
    }
	
	protected class msUpdateMsgsDB_cb implements AsyncTaskCompleteListener<List<mMessage>>{
    	AsyncTaskCompleteListener<List<mMessage>> cb;
    	mDialog dlg;

		public msUpdateMsgsDB_cb(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb) {
			this.cb = cb;
			this.dlg = dlg;
		}

		@Override
		public void onTaskComplete(List<mMessage> result) {
			SQLiteDatabase db = msApp.dbHelper.getWritableDatabase();
    		String my_table_name = msApp.dbHelper.getTableNameMsgs(MessageService.this);
    		int dlg_key = msApp.dbHelper.getDlgId(dlg, MessageService.this);
    		List<mMessage> msgs = new ArrayList<mMessage>();
    		
    		for (mMessage msg : result) {
				String selection = DBHelper.colDlgkey + " = ? AND " + DBHelper.colSendtime + " = ? AND " + DBHelper.colBody + " = ?";
    			String[] selectionArgs = { String.valueOf(dlg_key), String.valueOf(msg.sendTime.toMillis(false)), msg.text };
    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);

    			if(c.moveToFirst()){
    				int  flags_in_db = c.getInt( c.getColumnIndex(DBHelper.colFlags) );
    				
    				if(msg.flags != flags_in_db){
    					//update
    					int id = c.getInt(c.getColumnIndex(DBHelper.colId));
    					c.close();
	    				
    					msApp.dbHelper.updateMsg(id, msg, MessageService.this);			    					
	    							    				
	    				msgs.add(msg);
    				} else {
    					//not update
    					c.close();
    					continue;
    				}
    			} else {
    				//add
    				c.close();
    				
    				msApp.dbHelper.insertMsg(msg, my_table_name, dlg_key);
    				    				
    				msgs.add(msg);
    			}
    			
    			
    		}
    		
    		if(cb != null){    				
				cb.onTaskComplete(msgs);
    		}
    		
		}
    	
    }
	
	
	protected class msRefreshDlgs_cb implements AsyncTaskCompleteListener<List<mDialog>>{
		int count;
		int max_count;
		int offset;
		boolean running = false;
		List<mDialog> update_dlgs = new ArrayList<mDialog>();
		List<AsyncTaskCompleteListener<List<mDialog>>> update_cbs = new ArrayList<AsyncTaskCompleteListener<List<mDialog>>>(0);

		@Override
		public void onTaskComplete(List<mDialog> result) {
			SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();
    		String my_table_name = msApp.dbHelper.getTableNameDlgs(MessageService.this);
    		boolean all_new = true;
    		
    		if(result.size() < count)dl_all_dlgs_downloaded = true;	    		
    		
    		for (mDialog mdl : result) {
    			String selection = DBHelper.colParticipants + " = ?";
    			String[] selectionArgs = {mdl.getParticipantsAddresses()};
    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
    			
    			if(c.moveToFirst()){
    				Time last_time_in_db = new Time();
    				last_time_in_db.set( c.getLong( c.getColumnIndex(DBHelper.colLastmsgtime)) );
    				
    				if(mdl.last_msg_time.after(last_time_in_db)){
    					//update
    					int id = c.getInt(c.getColumnIndex(DBHelper.colId));
    					c.close();
	    				
    					msApp.dbHelper.updateDlg(id, mdl, MessageService.this);			    					
	    							    				
    					update_dlgs.add(mdl);
    				} else {
    					//not update
    					c.close();
    					all_new = false;
    					continue;
    				}		    				
    			} else {
    				//add
    				c.close();
    				
    				msApp.dbHelper.insertDlg(mdl, MessageService.this);
    					    				
    				update_dlgs.add(mdl);
    				dlgs_count++;
    			}
    		}
    		
    		if(all_new && result.size() > 0){
				int dlgs_to_update = msApp.dbHelper.getDlgsCount(MessageService.this) - (offset + count);
				if( max_count > 0 && (offset + dlgs_to_update) > max_count){
					dlgs_to_update = max_count - offset; 
				}
				
				if( dlgs_to_update > 0 ){
					offset = offset + count;
					
					if(dlgs_to_update > DLGS_DOWNLOAD_COUNT){	    						
						count = DLGS_DOWNLOAD_COUNT;	    						
					} else {
						count = dlgs_to_update;
					}
					
					getDialogsFromNet(count, offset, this);
				} else {
					run_cbs();
				}
    		} else {
    			run_cbs();
    		}
    		
		}
				
		private void run_cbs(){
			for(AsyncTaskCompleteListener<List<mDialog>> cb : update_cbs){
				if(cb != null)cb.onTaskComplete(update_dlgs);
			}
			update_cbs.clear();
			update_dlgs = new ArrayList<mDialog>();
			running = false;
		}

		public void addRefresh(AsyncTaskCompleteListener<List<mDialog>> cb,	int count) {
			update_cbs.add(cb);
			if(!running){
				running = true;
				max_count = count;
				this.count = DLGS_DOWNLOAD_COUNT;
				offset = 0;
				getDialogsFromNet(this.count, this.offset, this);
			}			
		}
		
	};
	
	protected class msRefreshMsgs_cb implements AsyncTaskCompleteListener<List<mMessage>>{
		class Params{
			int count;
			int max_count;
			int offset;
			List<mMessage> update_msgs = new ArrayList<mMessage>();
			List<AsyncTaskCompleteListener<List<mMessage>>> update_cbs = new ArrayList<AsyncTaskCompleteListener<List<mMessage>>>();
			mDialog dlg;
		}
		
		boolean running = false;
		List<Params> Psets = new ArrayList<Params>();
		

		@Override
		public void onTaskComplete(List<mMessage> result) {
			SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();
    		String my_table_name = msApp.dbHelper.getTableNameMsgs(MessageService.this);
    		boolean all_new = true;
    		
    		Params cp = Psets.get(0);
    		
    		if(result.size() < cp.count)dl_all_msgs_downloaded = true;	    		
    		int dlg_key = msApp.dbHelper.getDlgId(cp.dlg, MessageService.this);
    		
    		for (mMessage msg : result) {
    			String selection = DBHelper.colDlgkey + " = ? AND " + DBHelper.colSendtime + " = ? AND " + DBHelper.colBody + " = ?";
    			String[] selectionArgs = { String.valueOf(dlg_key), String.valueOf(msg.sendTime.toMillis(false)), msg.text };
    			Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);
    			
    			if(c.moveToFirst()){
    				int  flags_in_db = c.getInt( c.getColumnIndex(DBHelper.colFlags) );
    				
    				if(msg.flags != flags_in_db){
    					//update
    					int id = c.getInt(c.getColumnIndex(DBHelper.colId));
    					c.close();
	    				
    					msApp.dbHelper.updateMsg(id, msg, MessageService.this);			    					
	    							    				
	    				cp.update_msgs.add(msg);
    				} else {
    					//not update
    					c.close();
    					all_new = false;
    					continue;
    				}		    				
    			} else {
    				//add
    				c.close();
    				
    				msApp.dbHelper.insertMsg(msg, my_table_name, dlg_key);
    				
    				cp.update_msgs.add(msg);
    			}
    		}
    		
    		if(all_new && result.size() > 0){
    			int msgs_to_update = msApp.dbHelper.getMsgsCount(dlg_key, MessageService.this) - (cp.offset + cp.count);
    			if( cp.max_count > 0 && (cp.offset + msgs_to_update) > cp.max_count){
    				msgs_to_update = cp.max_count - cp.offset; 
				}
    			
    			cp.offset =  cp.offset + cp.count;
				if( msgs_to_update > 0 ){
					if(msgs_to_update > MSGS_DOWNLOAD_COUNT){				    		
						cp.count = MSGS_DOWNLOAD_COUNT;
					} else {
						cp.count = msgs_to_update;
					}
					
					getMessagesFromNet(cp.dlg, cp.count, cp.offset, this);
				} else {
					run_cbs();
				}
    		} else {
    			run_cbs();
    		}
    		
		}
				
		private void run_cbs(){
			Params cp = Psets.remove(0);
			for(AsyncTaskCompleteListener<List<mMessage>> cb : cp.update_cbs){
				if(cb != null)cb.onTaskComplete(cp.update_msgs);
			}
			
			if(Psets.size() == 0){
				running = false;
			} else {
				cp = Psets.get(0);
				getMessagesFromNet(cp.dlg, cp.count, cp.offset, this);
			}
		}

		public void addRefresh(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb, int count) {
			Params np = null;
			for(Params pp : Psets){
				if(pp.dlg.equals(dlg)){
					np = pp;
					break;
				}
			}
			if(np == null){
				np = new Params();
				np.dlg = dlg;
				np.max_count = count;
				np.update_cbs.add(cb);
				np.count = MSGS_DOWNLOAD_COUNT;
				np.offset = 0;
				Psets.add(np);				
			} else {
				np.update_cbs.add(cb);
			}
			
			if(!running){
				running = true;
				getMessagesFromNet(np.dlg, np.count, np.offset, this);
			}			
		}
		
	};
	
	msRefreshDlgs_cb msRefreshDlgsCb = new msRefreshDlgs_cb();
	msRefreshMsgs_cb msRefreshMsgsCb = new msRefreshMsgs_cb();
}
