package com.example.mymessenger.attachments;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.mymessenger.DownloadService;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.download_waiter;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mGlobal;

public class PhotoAttachment extends mAttachment {
    String url;
    String id;
    int width, height;

    ImageView iv;

    public PhotoAttachment() {
        super();
    }

    public PhotoAttachment(String data) {
        super(data);
    }

    @Override
    public View getView(Context context) {
        if(iv == null){
            iv = new ImageView(context);
            iv.setAdjustViewBounds(true);
            int mwidth, mheight;
            if(width > mGlobal.msg_max_width){
                mwidth = mGlobal.msg_max_width;
                mheight = height * mGlobal.msg_max_width / width;
            } else {
                mwidth = width;
                mheight = height;
            }
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    mwidth,
                    mheight);
            iv.setLayoutParams(params);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            iv.setImageDrawable(context.getResources().getDrawable(R.drawable.placeholder_image));

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
        }



        return iv;
    }

    @Override
    public int getType() {
        return mAttachment.PHOTO;
    }

    @Override
    protected String getDataString() {
        return url + ";" + id + ";" + String.valueOf(width) + ";" + String.valueOf(height);
    }

    @Override
    protected void getFromDataString(String data) {
        this.url = data.split(";")[0];
        this.id = data.split(";")[1];
        this.width = Integer.valueOf(data.split(";")[2]);
        this.height = Integer.valueOf(data.split(";")[3]);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSize(int width, int height){
        this.width = width;
        this.height = height;
    }
}
