package com.example.mymessenger;

import android.text.Spannable;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.mymessenger.attachments.mAttachment;
import com.example.mymessenger.services.MessageService.msInterfaceMS;
import com.example.mymessenger.ui.WrapWidthTextView;

public class MsgListItem {
	MyApplication app;
	public mMessage msg;
    public mDialog dlg;
	public Spannable text_spannable_cache = null;
	
	MsgListItem(mMessage msg, mDialog dlg, MyApplication app){
		this.msg = msg;
        this.dlg = dlg;
		this.app = app;
	}
	
	public void setupView(View view) {
		
	    msInterfaceMS ser = app.msManager.getService(msg.msg_service);

        boolean right = msg.isOut();
        boolean chat = dlg.isChat();

        RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.msg_container);
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.msg_linearLayout);
        WrapWidthTextView textLabel_text = (WrapWidthTextView) view.findViewById(R.id.msg_text);
        TextView textLabel_date = (TextView) view.findViewById(R.id.msg_date);
        ImageView user_icon = (ImageView) view.findViewById(R.id.msg_user_icon);

        DisplayMetrics metrics = app.getResources().getDisplayMetrics();

        // Color for new messages
	    if(msg.getFlag(mMessage.READED)) {
            relativeLayout.setBackgroundColor(app.getResources().getColor(R.color.msg_readed));
        } else {
            relativeLayout.setBackgroundColor(app.getResources().getColor(R.color.msg_notreaded));
        }

        // Msg data layout
        linearLayout.setBackgroundResource(right ? R.drawable.bg_msg_out_full : R.drawable.bg_msg_in_full);
        RelativeLayout.LayoutParams lp_lay = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp_lay.addRule(right ? RelativeLayout.ALIGN_PARENT_RIGHT : RelativeLayout.ALIGN_PARENT_LEFT);
        lp_lay.setMargins(right ? mGlobal.scale(50) : chat ? mGlobal.scale(50) : 0, 0, right ? chat ? mGlobal.scale(50) : 0 : mGlobal.scale(50), 0);
        linearLayout.setLayoutParams(lp_lay);

        // Msg text
        if(text_spannable_cache == null){
            RunnableUpdateMsgItem r = new RunnableUpdateMsgItem(view);
            MyApplication.handler1.post(r);
            textLabel_text.setText( msg.text );
        } else {
            textLabel_text.setText( text_spannable_cache );
        }


        //textLabel_text.setGravity(right ? Gravity.RIGHT : Gravity.LEFT);

        // Msg time
        String time = msg.sendTime.format("%H:%M\n");
        if( DateUtils.isToday(msg.sendTime.toMillis(false)) ){
            time += "today";
        } else {
            time += msg.sendTime.format("%d.%m.%Y");
        }
        textLabel_date.setText( time );
        textLabel_date.setGravity(right ? Gravity.RIGHT : Gravity.LEFT);
        RelativeLayout.LayoutParams lp_date = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp_date.addRule(right ? RelativeLayout.LEFT_OF : RelativeLayout.RIGHT_OF, linearLayout.getId());
        lp_date.setMargins(right ? 0 : -mGlobal.scale(50), 0, right ? -mGlobal.scale(50) : 0, 0);
        textLabel_date.setLayoutParams(lp_date);

    	//*textLabel_date.measure(metrics.widthPixels, metrics.heightPixels);
    	//textLabel_text.setMaxWidth((int) convertPixelsToDp( metrics.widthPixels - textLabel_date.getMeasuredWidth() ));
    	//textLabel_text.setWidth(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);
        //*textLabel_text.setCustomMaxWidth(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 40);
    	//textLabel_text.setCustomMax(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);

        // User icon
        if(chat){
            user_icon.setVisibility(View.VISIBLE);
            user_icon.setImageDrawable(msg.isOut() ? app.msManager.getService(dlg.msg_service_type).getMyContact().getIconDrawable(view.getContext()) :msg.respondent.getIconDrawable(view.getContext()));
            RelativeLayout.LayoutParams lp_icon = new RelativeLayout.LayoutParams(mGlobal.scale(50), mGlobal.scale(50));
            lp_icon.addRule(right ? RelativeLayout.RIGHT_OF : RelativeLayout.LEFT_OF, linearLayout.getId());
            lp_icon.setMargins(right ? -mGlobal.scale(50) : 0, 0, right ? 0 : -mGlobal.scale(50), 0);
            user_icon.setLayoutParams(lp_icon);

        } else {
            user_icon.setVisibility(View.INVISIBLE);
        }

        // Attachments
        if(msg.attachments != null) {
            LinearLayout ll = (LinearLayout) relativeLayout.findViewById(R.id.msg_attachments);
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
    	
    	//Log.d("MyAdapter", data.size() + " : " + position + " : " + msg.text);

	}
	
	class RunnableUpdateMsgItem implements Runnable{
		View view;
		
		RunnableUpdateMsgItem(View view){
			this.view = view;
		}

		@Override
		public void run() {
			final TextView textLabel_text = (TextView) view.findViewById(R.id.msg_text);
			text_spannable_cache = ChatMessageFormatter.getSmiledText(view.getContext(), msg.text, msg.msg_service, textLabel_text.getLineHeight());
			app.getMainActivity().runOnUiThread(new Runnable() {
			     @Override
			     public void run() {
			    	 textLabel_text.setText( text_spannable_cache );
			    	 DisplayMetrics metrics = app.getResources().getDisplayMetrics();
			    	 TextView textLabel_date = (TextView) view.findViewById(R.id.msg_date);
			     	 textLabel_date.measure(metrics.widthPixels, metrics.heightPixels);
			     	 //textLabel_text.setMaxWidth((int) convertPixelsToDp( metrics.widthPixels - textLabel_date.getMeasuredWidth() ));
			     	 //textLabel_text.setWidth(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);
			    }
			});
		}		
	}

}
