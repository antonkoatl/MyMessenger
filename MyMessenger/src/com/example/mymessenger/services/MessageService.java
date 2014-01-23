package com.example.mymessenger.services;

import java.util.List;

import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

public interface MessageService {
	public static final int SMS = 10;
	
	public List<mDialog> getDialogs(int offset, int count);
	public List<mMessage> getMessages(String user_id, int offset, int count);
	public List<mMessage> getMessages(mDialog dlg, int offset, int count);
	public String getName();
	public int getType();
	public void setActiveDialog(mDialog dlg);
	public mDialog getActiveDialog();
	public String getMyName();
	public String getContactName(String address);
}
