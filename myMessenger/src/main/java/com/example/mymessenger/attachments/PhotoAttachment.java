package com.example.mymessenger.attachments;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import com.example.mymessenger.DownloadService;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.download_waiter;
import com.example.mymessenger.mDialog;

public class PhotoAttachment extends mAttachment {
    String url;
    String id;

    ImageView iv;

    public PhotoAttachment(String data) {
        this.getFromDataString(data);
    }

    public PhotoAttachment() {

    }

    @Override
    public View getView(Context context) {
        if(iv == null){
            iv = new ImageView(context);
            iv.setAdjustViewBounds(true);
            iv.setImageDrawable(context.getResources().getDrawable(R.drawable.placeholder_image));
        }

        download_waiter tw = new download_waiter(url) {
            ImageView iv;

            @Override
            public void onDownloadComplete() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
                options.inScaled = true;
                options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
                iv.setImageBitmap(BitmapFactory.decodeFile(filepath, options));
            }

            public download_waiter setParams(ImageView iv) {
                this.iv = iv;
                return this;
            }


        }.setParams(iv);

        ((MyApplication) context.getApplicationContext()).dl_waiters.add(tw);

        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra("url", url);
        context.getApplicationContext().startService(intent);

        return iv;
    }

    @Override
    public int getType() {
        return mAttachment.PHOTO;
    }

    @Override
    protected String getDataString() {
        return url + ";" + id;
    }

    @Override
    protected void getFromDataString(String data) {
        this.url = data.split(";")[0];
        this.id = data.split(";")[1];
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setId(String id) {
        this.id = id;
    }
}
