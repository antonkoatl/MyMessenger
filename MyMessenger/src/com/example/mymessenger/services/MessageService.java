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
	protected boolean dl_all_dlgs_downloaded = false; //��� ������� ��������� �� ����
	protected mDialog dl_current_dlg; //��� �������� ��������� ��� ������ �������, ���������� ��� ��� ���������
	protected boolean dl_all_msgs_downloaded = false;
	protected boolean dl_all_new_msgs_downloaded = false;
	protected int dlgs_thread_count = 0; //���������� �������, ����������� ������� � ������ ������
	protected Map<mDialog, Integer> msgs_thread_count; //���������� �������, ����������� ��������� ��� ������������ ������� � ������ ������
	protected SharedPreferences sPref;
	
	List<mDialog> return_dialogs;
	List<mMessage> return_msgs;
	
	protected MessageService(MyApplication app){
		this.app = app;
		contacts = new HashMap<String, mContact>();
		msgs_thread_count = new HashMap<mDialog, Integer>(); //���������� �������� ��������� ��� ��������
	}
	

	//��������� ������ - ����� ���� ���������� 2 ���� (�� ��, ����� �� ���������)	
	/* ���� ������ ����������� �� ���������, �� �������� �����:
	 * 1. ������������ ������ �� ��
	 * 2. ����������� ������ �� ���������
	 * 3. �������� �������� �� ����������� ������ ������
	 * 4. ����������� ����� ������, ���������� ��
	 * 5. ���� ��� ����������� ������ - �����, �� ���������� ���������� ���� ���������� ���������� ������ �� ��������� ���������� ������ � ��
	 *  * ��� �������������� �������� ������ ��� ���������� ������ �� ������������, ������ ����������� ��
	 */	
	public abstract void requestMessages(mDialog activeDialog, int offset, int count, AsyncTaskCompleteListener<List<mMessage>> cb);
	public abstract void requestDialogs(int offset, int count, AsyncTaskCompleteListener<List<mDialog>> cb);
	public abstract void requestContactData(mContact cnt);
	public abstract void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);
	
	public abstract void requestNewMessagesRunnable(AsyncTaskCompleteListener<Runnable> cb); //��������� �������� ��� ������������ ����� ���������	
	public abstract void setup(); //����������� ������ ��� ������
	public abstract void init(); //�������������, ����� �����������
	 
	//��������� �������
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
	
	public final void refresh() { //�������� ��� ���������� ���������� ��������
		dl_all_dlgs_downloaded = false;		
	}
	
	public final void setActiveDialog(mDialog dlg) {
		active_dlg = dlg;
	}
		
	//�������� ���������
	public abstract boolean sendMessage(String address, String text);
	
	//������� ��� ����������
	public abstract String[] getStringsForMainViewMenu();
	public abstract void MainViewMenu_click(int which, Context context);

}
