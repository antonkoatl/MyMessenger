package com.example.mymessenger;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadReceiv extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		MyApplication app = (MyApplication) context.getApplicationContext();
		//Intent intent1 = new Intent(context, UpdateService.class);
		//app.pi = PendingIntent.getBroadcast(context, 0, intent1, 0);
		
		//context.startService(intent1);
	}

}
