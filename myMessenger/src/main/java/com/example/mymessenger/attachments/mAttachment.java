package com.example.mymessenger.attachments;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public abstract class mAttachment {
    public static final int PHOTO = 1;
    public abstract View getView(Context context);
    public abstract int getType();

    public static String getDataString(List<mAttachment> attachments) {
        if(attachments == null)return "";
        String data = "";
        for(mAttachment attachment : attachments) {
            if (data.length() > 0) data += ",";
            data += String.valueOf(attachment.getType()) + ">" + attachment.getDataString();
        }

        return data;
    }

    protected abstract String getDataString();

    public static List<mAttachment> getListFromDataString(String data) {
        if(data.length() == 0)return null;
        List<mAttachment> attachments = new ArrayList<mAttachment>();
        for(String data_ : data.split(",")) {
            int type = Integer.valueOf(data_.split(">")[0]);
            String data_at = data_.split(">")[1];
            mAttachment at = null;
            switch(type){
                case PHOTO:
                    at = new PhotoAttachment(data_at);
                    break;
                default:
                    break;
            }
            attachments.add(at);
        }

        return attachments;
    }

    protected abstract void getFromDataString(String data);

}
