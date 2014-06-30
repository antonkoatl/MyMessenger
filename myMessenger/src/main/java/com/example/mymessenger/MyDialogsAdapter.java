package com.example.mymessenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyDialogsAdapter extends BaseAdapter {
    LayoutInflater lInflater;
    List<mDialog> data;
    Context context;

    public MyDialogsAdapter(Context context, List<mDialog> showing_dialogs) {
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        data = new ArrayList<mDialog>();

        if(showing_dialogs != null){
            for(mDialog dlg : showing_dialogs)
                data.add( dlg );
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
            view = lInflater.inflate(R.layout.dialogs_row_layout, parent, false);
        }

        mDialog dlg = data.get(position);

        dlg.setupView(view);

        return view;
    }

    public int indexOf(mDialog dlg) {
        return data.indexOf(dlg);
    }

    public void add(int i, mDialog dlg) {
        data.add(i, dlg);
        notifyDataSetChanged();
    }

    public void add(mDialog dlg) {
        data.add(dlg);
        notifyDataSetChanged();
    }

    public mDialog remove(int tind) {
        mDialog t = data.remove(tind);
        notifyDataSetChanged();
        return t;
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }
}
