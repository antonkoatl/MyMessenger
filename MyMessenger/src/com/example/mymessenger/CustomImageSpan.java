package com.example.mymessenger;

import com.example.mymessenger.ui.ListViewSimpleFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.Log;

public class CustomImageSpan extends DynamicDrawableSpan {
	Drawable mDrawable = null;
	boolean downloaded = false;
	private Context mContext;
	private int mResourceId;

	
	public CustomImageSpan(Context context, int resourceId, String url) {
		super(ALIGN_BOTTOM);
		mContext = context;
		mResourceId = resourceId;

		Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("url", url);
        context.getApplicationContext().startService(intent);
        
        MyApplication app = (MyApplication) ((Activity) context).getApplication();
        
        download_waiter dw = new download_waiter(url){

			@Override
			public void onDownloadComplete() {
				Bitmap bitmap  = BitmapFactory.decodeFile(filepath);
				mDrawable = new BitmapDrawable(bitmap);
				mDrawable.setBounds(0, 0, mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
				//ListViewSimpleFragment fr = (ListViewSimpleFragment) ((MainActivity) app.getCurrentActivity()).pagerAdapter.getRegisteredFragment(2);
			}
        	
        };
        
        app.dl_waiters.add(dw);
	}

	@Override
    public Drawable getDrawable() {
        Drawable drawable = null;
        
        if (mDrawable != null) {
            drawable = mDrawable;
        } else {
            try {
                drawable = mContext.getResources().getDrawable(mResourceId);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight());
            } catch (Exception e) {
                Log.e("sms", "Unable to find resource: " + mResourceId);
            }                
        }

        return drawable;
    }
	
	
}
