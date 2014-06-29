package com.example.mymessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloaderResultReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		MyApplication app = (MyApplication) context.getApplicationContext();
		
		String url_path = intent.getStringExtra("url");
		String file_path = intent.getStringExtra("file_path");
		
		boolean cnt_updated = false;
		
		
		for(download_waiter dw : app.getDownloadWaiters(url_path)){
			dw.setFilePath(file_path);
			dw.onDownloadComplete();
		}
		
		if(cnt_updated)app.triggerCntsUpdaters();
	}

}
