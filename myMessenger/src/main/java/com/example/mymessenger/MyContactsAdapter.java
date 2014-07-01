package com.example.mymessenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MyContactsAdapter extends BaseAdapter {
    LayoutInflater lInflater;
    List<mContact> data;
    Context context;

    MyContactsAdapter(Context context, List<mContact> showing_contacts) {
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        data = showing_contacts;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
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
            view = lInflater.inflate(R.layout.contactlist_row_layout, parent, false);
        }

        mContact cnt = data.get(position);

        //boolean left = msg.sender == ((MyApplication) context.getApplicationContext()).getService( MessageService.SMS ).getMyName();

        TextView textLabel = (TextView) view.findViewById(R.id.cntview_cntname);
        textLabel.setText( cnt.getName() );

        if(cnt.icon_50 != null){
            ImageView iv = (ImageView) view.findViewById(R.id.cntview_iconmain);
            iv.setImageBitmap( cnt.icon_50);
        }

        //Log.d("MyDialogsAdapter", data.size() + " : " + position + " : " + dlg.getParticipantsNames());
        return view;
    }

}
