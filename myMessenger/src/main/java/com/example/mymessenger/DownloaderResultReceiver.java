package com.example.mymessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class DownloaderResultReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		MyApplication app = (MyApplication) context.getApplicationContext();
		
		String url_path = intent.getStringExtra("url");
		String file_path = intent.getStringExtra("file_path");
		
		boolean cnt_updated = false;

        List<download_waiter> dws = app.getDownloadWaiters(url_path);
		if(dws != null) {
            for (download_waiter dw : dws) {
                dw.setFilePath(file_path);
                dw.onDownloadComplete();
            }
        }
		
		if(cnt_updated)app.triggerCntsUpdaters();
	}

}
