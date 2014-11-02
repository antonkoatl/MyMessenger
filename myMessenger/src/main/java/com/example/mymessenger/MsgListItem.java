package com.example.mymessenger;

import android.text.Spannable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.mymessenger.attachments.mAttachment;
import com.example.mymessenger.services.MessageService.msInterfaceMS;

public class MsgListItem {
	MyApplication app;
	public mMessage msg;
	public Spannable text_spannable_cache = null;
	
	MsgListItem(mMessage msg, MyApplication app){
		this.msg = msg;
		this.app = app;
	}
	
	public void setupView(View view) {
		
	    msInterfaceMS ser = app.msManager.getService(msg.msg_service);

        boolean right = msg.isOut();
	    
	    RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.msg_container);

        // Color for new messages
	    if(!msg.getFlag(mMessage.READED))rl.setBackgroundColor(app.getResources().getColor(R.color.msg_notreaded));

        // Msg data layout
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.msg_linearLayout);
        linearLayout.setBackgroundResource(right ? R.drawable.bubble_green : R.drawable.bubble_yellow);
        RelativeLayout.LayoutParams lp_lay = (RelativeLayout.LayoutParams) linearLayout.getLayoutParams();
        lp_lay.addRule(right ? RelativeLayout.ALIGN_PARENT_RIGHT : RelativeLayout.ALIGN_PARENT_LEFT);
        linearLayout.setLayoutParams(lp_lay);

        // Msg text
        WrapWidthTextView textLabel_text = (WrapWidthTextView) view.findViewById(R.id.msg_text);

        if(text_spannable_cache == null){
            RunnableUpdateMsgItem r = new RunnableUpdateMsgItem(view);
            MyApplication.handler1.post(r);
            textLabel_text.setText( msg.text );
        } else {
            textLabel_text.setText( text_spannable_cache );
        }


        textLabel_text.setGravity(right ? Gravity.RIGHT : Gravity.LEFT);

        // Msg time
	    TextView textLabel_date = (TextView) view.findViewById(R.id.msg_date);
        textLabel_date.setText( msg.sendTime.format("%H:%M\n%d.%m.%Y") );
        textLabel_date.setGravity(right ? Gravity.RIGHT : Gravity.LEFT);
        RelativeLayout.LayoutParams lp_date = (RelativeLayout.LayoutParams) textLabel_date.getLayoutParams();
        lp_date.addRule(right ? RelativeLayout.LEFT_OF : RelativeLayout.RIGHT_OF, linearLayout.getId());
        textLabel_date.setLayoutParams(lp_date);

    	DisplayMetrics metrics = app.getResources().getDisplayMetrics();
    	textLabel_date.measure(metrics.widthPixels, metrics.heightPixels);
    	//textLabel_text.setMaxWidth((int) convertPixelsToDp( metrics.widthPixels - textLabel_date.getMeasuredWidth() ));
    	textLabel_text.setWidth(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);
    	//textLabel_text.setCustomMax(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);


        // Attachments
        if(msg.attachments != null) {
            for (mAttachment attachment : msg.attachments) {
                LinearLayout ll = (LinearLayout) rl.findViewById(R.id.msg_linearLayout);
                while(ll.getChildCount() > 1)
                    ll.removeViewAt(1);
                View view_at = attachment.getView(view.getContext());
                ll.addView(view_at);
            }
        }
    	
    	//Log.d("MyAdapter", data.size() + " : " + position + " : " + msg.text);

	}
	
	class RunnableUpdateMsgItem implements Runnable{
		View view;
		
		RunnableUpdateMsgItem(View view){
			this.view = view;
		}

		@Override
		public void run() {
			final WrapWidthTextView textLabel_text = (WrapWidthTextView) view.findViewById(R.id.msg_text);
			text_spannable_cache = ChatMessageFormatter.getSmiledText(view.getContext(), msg.text, msg.msg_service, textLabel_text.getLineHeight());
			app.getMainActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 textLabel_text.setText( text_spannable_cache );
			    	 DisplayMetrics metrics = app.getResources().getDisplayMetrics();
			    	 TextView textLabel_date = (TextView) view.findViewById(R.id.msg_date);
			     	 textLabel_date.measure(metrics.widthPixels, metrics.heightPixels);
			     	 //textLabel_text.setMaxWidth((int) convertPixelsToDp( metrics.widthPixels - textLabel_date.getMeasuredWidth() ));
			     	 textLabel_text.setWidth(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);
			    }
			});
		}		
	}

}
