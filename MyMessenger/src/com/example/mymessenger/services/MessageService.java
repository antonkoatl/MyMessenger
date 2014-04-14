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

	public void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb);
	public void requestDialogs(int offset, int count, AsyncTaskCompleteListener<List<mDialog>> cb);
	public void requestNewMessagesRunnable(AsyncTaskCompleteListener<Runnable> cb);
	
	public String getServiceName();
	public int getServiceType();
	
	public void setActiveDialog(mDialog dlg);
	public mDialog getActiveDialog();

	public boolean sendMessage(String address, String text);
	
	public String[] getStringsForMainViewMenu();
	public void MainViewMenu_click(int which, Context context);
	
	public void requestContactData(mContact cnt);
	public mContact getContact(String address);

	public mContact getMyContact();
	
	public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);

}
