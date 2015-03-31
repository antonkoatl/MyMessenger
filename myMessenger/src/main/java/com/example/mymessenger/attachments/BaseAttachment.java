package com.example.mymessenger.attachments;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BaseAttachment extends mAttachment {

    public BaseAttachment(String data_at) {
        super(data_at);
    }

    public BaseAttachment() {
        super();
    }

    @Override
    public View getView(Context context) {
        TextView textView = new TextView(context);
        textView.setText("<attachment>"+name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(lp);
        return textView;
    }

    @Override
    public int getType() {
        return BASE;
    }

}
