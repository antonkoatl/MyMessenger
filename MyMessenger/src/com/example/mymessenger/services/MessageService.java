package com.example.mymessenger.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public abstract class MessageService {
	public static final int SMS = 10;
	public static final int VK = 11;
	
	protected MyApplication app;
	protected mContact self_contact;
	protected mDialog active_dlg;
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
	protected Map<mDialog, Integer> msgs_thread_count; //Количество потоков, загружающих сообщения для определённого диалога в данных момент
	protected SharedPreferences sPref;
	
	List<mDialog> return_dialogs;
	List<mMessage> return_msgs;
	
	protected MessageService(MyApplication app){
		this.app = app;
		contacts = new HashMap<String, mContact>();
		msgs_thread_count = new HashMap<mDialog, Integer>(); //индикаторы загрузки сообщений для диалогов
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
	public abstract void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb);
	public abstract void requestDialogs(int offset, int count, AsyncTaskCompleteListener<List<mDialog>> cb);
	public abstract void requestContactData(mContact cnt);
	public abstract void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);
	
	public abstract void requestNewMessagesRunnable(AsyncTaskCompleteListener<Runnable> cb); //Запросить алгоритм для отслеживания новых сообщений	
	public abstract void setup(); //Подготовить сервис для работы
	public abstract void init(); //Инициализация, после авторизации
	 
	//Служебные функции
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
	
	public final boolean isLoadingMsgsForDls(mDialog dlg) {
		Integer count = msgs_thread_count.get(dlg); 
		if(count == null)return false;
		if(count == 0){
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
	}
		
	//отправка сообщения
	public abstract boolean sendMessage(String address, String text);
	
	//функции для интерфейса
	public abstract String[] getStringsForMainViewMenu();
	public abstract void MainViewMenu_click(int which, Context context);

}
