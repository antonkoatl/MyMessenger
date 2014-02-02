package com.example.mymessenger.services;

import java.util.List;

import android.content.Context;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public interface MessageService {
	public static final int SMS = 10;
	public static final int VK = 11;
	
	public List<mDialog> getDialogs(int offset, int count);
	public List<mMessage> getMessages(String user_id, int offset, int count);
	public List<mMessage> getMessages(mDialog dlg, int offset, int count);
	public String getName();
	public int getType();
	public void setActiveDialog(mDialog dlg);
	public mDialog getActiveDialog();
	public String getMyName();
	public String getContactName(String address);
	public boolean sendMessage(String address, String text);
	public String[] getStringsForMainViewMenu();
	public void MainViewMenu_click(int which, Context context);
	public void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb);
}
