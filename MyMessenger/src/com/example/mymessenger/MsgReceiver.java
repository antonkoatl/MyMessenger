package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.Vk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class MsgReceiver extends BroadcastReceiver {
	public static final String ACTION_RECEIVE = "android.mymessenger.MSG_RECEIVED";
	public static final String ACTION_UPDATE = "android.mymessenger.MSG_UPDATED";
	
	public static final int UPDATE_REPLACE = 1;
	public static final int UPDATE_INSTALL = 2;
	public static final int UPDATE_RESET = 3;
	NotificationManager manager;
	MyApplication app;

	@Override
	public void onReceive(Context context, Intent intent) {
		app = (MyApplication) context.getApplicationContext();
		
		if(intent.getAction().equals(ACTION_RECEIVE)){
			mMessage msg = (mMessage) intent.getParcelableExtra("msg");			

			
			long chat_id = intent.getLongExtra("chat_id", 0);
			
			mDialog dlg;
			if(chat_id != 0)
				dlg = app.update_db_dlg(msg, chat_id);
			else
				dlg = app.update_db_dlg(msg);
			
			app.update_db_msg(msg, dlg);
			List<mDialog> dlgs = new ArrayList<mDialog>();
			dlgs.add(dlg);
			app.triggerDlgsUpdaters(dlgs);
			app.triggerMsgUpdaters(msg, dlg);
			if(!msg.getFlag(mMessage.OUT)){
				if(app.getUA() != app.UA_MSGS_LIST && app.getUA() != app.UA_DLGS_LIST)
					createInfoNotification(context, msg);
				else
					createSimpleNotification(context, msg);
			}
		}

		if(intent.getAction().equals(ACTION_UPDATE)){
			int msg_id = intent.getIntExtra("msg_id", -1);
			int flags = intent.getIntExtra("msg_flags", 0);
			int mode = intent.getIntExtra("msg_mode", 0);
			int service_type = intent.getIntExtra("service_type", 0);
			
			mMessage msg = app.dbHelper.getMsgByMsgId(msg_id, app.getService(service_type));
			
			if(msg != null){
				if(mode == UPDATE_REPLACE){
					msg.setFlag(mMessage.READED, (flags & 1) != 1); //�������� �������� ��� READED
					msg.setFlag(mMessage.OUT, (flags & 2) == 2);
				}
				if(mode == UPDATE_INSTALL){
					if( (flags & 1) == 1 )msg.setFlag(mMessage.READED, false);
					if( (flags & 2) == 2 )msg.setFlag(mMessage.OUT, true);
				}
				if(mode == UPDATE_RESET){
					if( (flags & 1) == 1 )msg.setFlag(mMessage.READED, true);
					if( (flags & 2) == 2 )msg.setFlag(mMessage.OUT, false);
				}
				

				//int dlg_key = app.dbHelper.getDlgId(msg.respondent.address, app.getService(msg.msg_service));
				mDialog dlg = app.update_db_dlg(msg);
				app.update_db_msg(msg, dlg);
				app.triggerMsgUpdaters(msg, dlg);
				Log.d("MsgReceiver", "Msg updated: " + msg.text);
			}
			
		}
		
	}
	
	public void createInfoNotification(Context context, mMessage msg){
		manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
	    Intent notificationIntent = new Intent(context, MainActivity.class); // �� ����� �� ����������� ��������� MainActivity
	    notificationIntent.putExtra("notification_clicked_msg", true);
	    notificationIntent.putExtra("msg", msg);
	    
	    NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
	        .setSmallIcon(R.drawable.ic_launcher) //������ �����������
	        .setAutoCancel(true) //����������� ��������� �� ����� �� ����
	        .setTicker(msg.text) //�����, ������� ����������� ������ ������-���� ��� �������� �����������
	        .setContentText(msg.text) // �������� ����� �����������
	        .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT))
	        .setWhen(msg.sendTime.toMillis(false)) //������������ ����� �����������
	        .setContentTitle("MyMessenger - " + app.getService(msg.msg_service).getServiceName()) //��������� �����������
	        .setDefaults(Notification.DEFAULT_ALL); // ����, ����� � ������� ��������� ������������ �� ���������

	        Notification notification = nb.build(); //���������� �����������
	        manager.notify(0 , notification); // ���������� ��� ������������.
	      //notifications.put(lastId, notification); //������ �� ����� ���������� � ���� �� id
	    //return lastId++;
	  }
	
	public void createSimpleNotification(Context context, mMessage msg){
		manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
	    Intent notificationIntent = new Intent(context, MainActivity.class); // �� ����� �� ����������� ��������� MainActivity
	    notificationIntent.putExtra("notification_clicked_msg", true);
	    notificationIntent.putExtra("msg", msg);
	    
	    NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
	        .setDefaults(Notification.DEFAULT_ALL); // ����, ����� � ������� ��������� ������������ �� ���������

	        Notification notification = nb.build(); //���������� �����������
	        manager.notify(1, notification); // ���������� ��� ������������.
	      //notifications.put(lastId, notification); //������ �� ����� ���������� � ���� �� id
	    //return lastId++;
	  }


}

