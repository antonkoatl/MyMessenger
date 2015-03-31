package com.example.mymessenger.attachments;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public abstract class mAttachment {
    public static final int BASE = 0;
    public static final int PHOTO = 1;
    public static final int FWD = 2;

    String name;

    public mAttachment(String data) {
        this.getFromDataString(data);
    }

    public mAttachment() {

    }

    public abstract View getView(Context context);

    public abstract int getType();

    public void setName(String name){
        this.name = name;
    }

    public static String getDataString(List<mAttachment> attachments) {
        if(attachments == null)return "";
        String data = "";
        for(mAttachment attachment : attachments) {
            if (data.length() > 0) data += "<,>";
            data += String.valueOf(attachment.getType()) + "<:>" + attachment.getDataString();
        }

        return data;
    }

    protected String getDataString() {
        return name;
    };

    public static List<mAttachment> getListFromDataString(String data) {
        if(data.length() == 0)return null;
        List<mAttachment> attachments = new ArrayList<mAttachment>();
        for(String data_ : data.split("<,>")) {
            int type = Integer.valueOf(data_.split("<:>")[0]);
            String data_at = data_.split("<:>")[1];
            mAttachment at = null;
            switch(type){
                case PHOTO:
                    at = new PhotoAttachment(data_at);
                    break;
                case FWD:
                    at = new FwdAttachment(data_at);
                    break;
                default:
                    at = new BaseAttachment(data_at);
                    break;
            }
            attachments.add(at);
        }

        return attachments;
    }

    protected void getFromDataString(String data){
        this.name = data;
    };

}
