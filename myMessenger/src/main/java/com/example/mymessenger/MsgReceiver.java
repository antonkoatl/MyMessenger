package com.example.mymessenger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.mymessenger.services.MessageService.MessageService;

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
            MessageService ms = (MessageService) app.msManager.getService(msg.msg_service);
			long chat_id = intent.getLongExtra("chat_id", 0);

            ms.msDBHelper.updateMsgInDB(msg, chat_id, ms);

			if(!msg.getFlag(mMessage.OUT)){
				if(app.getUA() != app.UA_MSGS_LIST && app.getUA() != app.UA_DLGS_LIST)
					createInfoNotification(context, msg, chat_id);
				else
					createSimpleNotification(context, msg);
			}
		}

		if(intent.getAction().equals(ACTION_UPDATE)){
			int msg_id = intent.getIntExtra("msg_id", -1);
			int flags = intent.getIntExtra("msg_flags", 0);
			int mode = intent.getIntExtra("msg_mode", 0);
			int service_type = intent.getIntExtra("service_type", 0);

            MessageService ms = (MessageService) app.msManager.getService(service_type);
            mMessage msg = ms.msDBHelper.getMsgByIdFromDB(msg_id, ms);
			
			if(msg != null){
				if(mode == UPDATE_REPLACE){
                    msg.setReaded((flags & 1) != 1); //Обратное значение для READED
					msg.setOut((flags & 2) == 2);
				}
				if(mode == UPDATE_INSTALL){
				    if( (flags & 1) == 1 )msg.setReaded(false);
					if( (flags & 2) == 2 )msg.setOut(true);
				}
				if(mode == UPDATE_RESET){
					if( (flags & 1) == 1 )msg.setReaded(true);
					if( (flags & 2) == 2 )msg.setOut(false);
				}
				
                ms.msDBHelper.updateMsgInDBById(msg, msg_id, ms);
				Log.d("MsgReceiver", "Msg updated: " + msg.text);
			}
			
		}
		
	}

    public void createInfoNotification(Context context, mMessage msg, long chat_id){
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(context, MainActivity.class); // по клику на уведомлении откроется MainActivity
        notificationIntent.putExtra("notification_clicked_msg", true);
        notificationIntent.putExtra("msg", msg);
        notificationIntent.putExtra("chat_id", chat_id);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher) //иконка уведомления
                .setAutoCancel(true) //уведомление закроется по клику на него
                .setTicker(msg.text) //текст, который отобразится вверху статус-бара при создании уведомления
                .setContentText(msg.text) // Основной текст уведомления
                .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                .setWhen(msg.sendTime.toMillis(false)) //отображаемое время уведомления
                .setContentTitle(app.msManager.getService(msg.msg_service).getServiceName() + " - " + msg.respondent.getName() + (chat_id == 0 ? "" : " (" + getChatName(msg, chat_id) + ")")) //заголовок уведомления
                .setDefaults(Notification.DEFAULT_ALL); // звук, вибро и диодный индикатор выставляются по умолчанию

        Notification notification = nb.build(); //генерируем уведомление
        manager.notify(0 , notification); // отображаем его пользователю.
        //notifications.put(lastId, notification); //теперь мы можем обращаться к нему по id
        //return lastId++;
    }

    private CharSequence getChatName(mMessage msg, long chat_id) {
        MessageService ms = (MessageService) app.msManager.getService(msg.msg_service);
        mDialog dlg = ms.msDBHelper.getDlgFromDBOrCreate(msg, chat_id, ms);
        return dlg.getDialogTitle();
    }

    public void createSimpleNotification(Context context, mMessage msg){
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(context, MainActivity.class); // по клику на уведомлении откроется MainActivity
        notificationIntent.putExtra("notification_clicked_msg", true);
        notificationIntent.putExtra("msg", msg);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setDefaults(Notification.DEFAULT_ALL); // звук, вибро и диодный индикатор выставляются по умолчанию

        Notification notification = nb.build(); //генерируем уведомление
        manager.notify(1, notification); // отображаем его пользователю.
        //notifications.put(lastId, notification); //теперь мы можем обращаться к нему по id
        //return lastId++;
    }


}

