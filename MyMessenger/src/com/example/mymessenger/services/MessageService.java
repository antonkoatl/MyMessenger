package com.example.mymessenger.services;

import java.util.List;

import android.content.Context;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public interface MessageService {
	public static final int SMS = 10;
	public static final int VK = 11;

	//Запросить данные - может быть возвращено 2 раза (из бд, затем из интернета)	
	/* Если данные загружаются из интернета, то алгоритм таков:
	 * 1. Возвращаются данные из бд
	 * 2. Загружаются данные из интернета
	 * 3. Проверка являются ли загруженные данные новыми
	 * 4. Возвращение новых данных, обновление бд
	 * 5. Если все загруженные данные - новые, то продолжить обновление пока количество обновлённых данных не достигнет количества данных в бд
	 *  * При дополнительной загрузке данных для обновления данные не возвращаются, только обновляется бд
	 */	
	public void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb);
	public void requestDialogs(int offset, int count, AsyncTaskCompleteListener<List<mDialog>> cb);
	public void requestContactData(mContact cnt);
	public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);
	
	//Запросить алгоритм для отслеживания новых сообщений	
	public void requestNewMessagesRunnable(AsyncTaskCompleteListener<Runnable> cb);
	
	//Служебные функции
	public String getServiceName();
	public int getServiceType();
	public mDialog getActiveDialog();
	public mContact getContact(String address);
	public mContact getMyContact();
	
	public void setActiveDialog(mDialog dlg);
	
	//отправка сообщения
	public boolean sendMessage(String address, String text);
	
	//функции для интерфейса
	public String[] getStringsForMainViewMenu();
	public void MainViewMenu_click(int which, Context context);

}
