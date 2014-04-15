package com.example.mymessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class DownloaderResultReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		MyApplication app = (MyApplication) context.getApplicationContext();
		
		String url_path = intent.getStringExtra("url");
		String file_path = intent.getStringExtra("file_path");
		
		boolean cnt_updated = false;
		
		for(download_waiter dw : app.getDownloadWaiters(url_path)){
			if(dw.type.equals("cnt_icon_100")){
				mContact cnt = (mContact) dw.obj;
				BitmapFactory.Options options = new BitmapFactory.Options();
				//options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				cnt.icon_100 = BitmapFactory.decodeFile(file_path);
				//cnt_updated = true;
			}
			
			if(dw.type.equals("iv_cnt_icon_100")){
				ImageView iv = (ImageView) dw.obj;
				iv.setImageBitmap( BitmapFactory.decodeFile(file_path) );
			}
		}
		
		if(cnt_updated)app.triggerCntsUpdaters();
	}

}
