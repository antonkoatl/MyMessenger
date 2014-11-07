package com.example.mymessenger;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyMsgAdapter extends BaseAdapter {
    LayoutInflater lInflater;
    List<mMessage> data;
    mDialog dlg;
    Context context;



    public MyMsgAdapter(Context context, List<mMessage> msgs) {
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        data = new ArrayList<mMessage>();
        if(msgs != null){
            for(mMessage msg : msgs)
                data.add( msg );
        }
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //View view = convertView; // Использовать повторно View, решить что делать с шириной
        View view = null;


        if (view == null) {
            view = lInflater.inflate(R.layout.msg_row_layout, parent, false);
        }

        mMessage msg = data.get(position);
        msg.setupUIView(view, dlg);

        return view;
    }

    public float convertPixelsToDp(float px){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public float convertDpToPixel(float dp){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public int indexOf(mMessage msg) {
        return data.indexOf(msg);
    }

    public void add(int i, mMessage msg) {
        data.add(i, msg);
        notifyDataSetChanged();
    }

    public void add(mMessage msg) {
        data.add(msg);
        notifyDataSetChanged();
    }

    public mMessage remove(int tind) {
        mMessage t = data.remove(tind);
        notifyDataSetChanged();
        return t;
    }

    public void setDlg(mDialog dlg){
        this.dlg = dlg;
    }

}
