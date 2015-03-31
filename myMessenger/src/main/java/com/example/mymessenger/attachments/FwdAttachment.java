package com.example.mymessenger.attachments;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mymessenger.R;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.ui.WrapWidthTextView;
import com.google.gson.Gson;

public class FwdAttachment extends mAttachment {
    View view;
    mMessage msg;

    public FwdAttachment(mMessage message) {
        this.msg = message;
    }

    public FwdAttachment(String data_at) {
        super(data_at);
    }

    @Override
    public View getView(Context context) {
        if (view == null) {
            LayoutInflater lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = lInflater.inflate(R.layout.msg_fwd_view, null);

            setupView();
        }



        return view;
    }

    private void setupView(){
        WrapWidthTextView textLabel_text = (WrapWidthTextView) view.findViewById(R.id.msg_fwd_text);
        textLabel_text.setText( msg.text );

        String time = msg.sendTime.format("%H:%M ");
        if( DateUtils.isToday(msg.sendTime.toMillis(false)) ){
            time += "today";
        } else {
            time += msg.sendTime.format("%d.%m.%Y");
        }

        TextView textLabel_name = (TextView) view.findViewById(R.id.msg_fwd_user_name);
        textLabel_name.setText( msg.respondent.getName() );
        TextView textLabel_date = (TextView) view.findViewById(R.id.msg_fwd_send_time);
        textLabel_date.setText( time );

        ImageView user_icon = (ImageView) view.findViewById(R.id.msg_fwd_user_icon);
        user_icon.setImageDrawable(msg.respondent.getIconDrawable(view.getContext()));

        if(msg.attachments != null) {
            LinearLayout ll = (LinearLayout) view.findViewById(R.id.msg_fwd_attachments);
            ll.removeAllViews();

            for (mAttachment attachment : msg.attachments) {
                View view_at = attachment.getView(view.getContext());
                ViewParent parent = view_at.getParent();
                if (parent != null && parent instanceof View) {
                    ((ViewGroup)parent).removeView(view_at);
                }

                ll.addView(view_at);
            }
        }
    }

    @Override
    public int getType() {
        return FWD;
    }

    @Override
    protected String getDataString() {
        return super.getDataString() + ";" + (new Gson()).toJson(msg);
    }

    @Override
    protected void getFromDataString(String data) {
        super.getFromDataString(data);
        this.msg = new Gson().fromJson(data.split(";")[1], mMessage.class);
    }

    public void setMsg(mMessage msg) {
        this.msg = msg;
    }
}
