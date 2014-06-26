package com.example.mymessenger;

import com.example.mymessenger.services.MessageService;

import android.text.Spannable;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MsgListItem {
	MyApplication app;
	public mMessage msg;
	public Spannable text_spannable_cache = null;
	
	MsgListItem(mMessage msg, MyApplication app){
		this.msg = msg;
		this.app = app;
	}
	
	public void setupView(View view) {
		
	    MessageService ser = app.getService(msg.msg_service);
	    boolean left = msg.getFlag(mMessage.OUT);
	    
	    RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.msg_container);
	    
	    if(!msg.getFlag(mMessage.READED))rl.setBackgroundColor(app.getResources().getColor(R.color.msg_notreaded));
	    
	    TextView textLabel_date = (TextView) view.findViewById(R.id.msg_date);
	    WrapWidthTextView textLabel_text = (WrapWidthTextView) view.findViewById(R.id.msg_text);	    
	    
	    textLabel_date.setText( msg.sendTime.format("%H:%M\n%d.%m.%Y") );
	    textLabel_date.setGravity(left ? Gravity.LEFT : Gravity.RIGHT);
	    
	    RelativeLayout.LayoutParams lp_date = (RelativeLayout.LayoutParams) textLabel_date.getLayoutParams();
	    //lp_date.addRule(left ? RelativeLayout.ALIGN_PARENT_RIGHT : RelativeLayout.ALIGN_PARENT_LEFT);
	    lp_date.addRule(left ? RelativeLayout.RIGHT_OF : RelativeLayout.LEFT_OF, textLabel_text.getId());
	    textLabel_date.setLayoutParams(lp_date);
	    
	    if(text_spannable_cache == null){
	    	RunnableUpdateMsgItem r = new RunnableUpdateMsgItem(view);
			MyApplication.handler1.post(r);
			textLabel_text.setText( msg.text );
    	} else {
    		textLabel_text.setText( text_spannable_cache );
    	}
    	
	    textLabel_text.setBackgroundResource(left ? R.drawable.bubble_green : R.drawable.bubble_yellow);
	    textLabel_text.setGravity(left ? Gravity.RIGHT : Gravity.LEFT);
    	    	    	
    	RelativeLayout.LayoutParams lp_text = (RelativeLayout.LayoutParams) textLabel_text.getLayoutParams();
    	lp_text.addRule(left ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT);
    	//lp_text.addRule(left ? RelativeLayout.LEFT_OF : RelativeLayout.RIGHT_OF, textLabel_date.getId());

    	textLabel_text.setLayoutParams(lp_text);
    	
    	DisplayMetrics metrics = app.getResources().getDisplayMetrics();
    	textLabel_date.measure(metrics.widthPixels, metrics.heightPixels);
    	//textLabel_text.setMaxWidth((int) convertPixelsToDp( metrics.widthPixels - textLabel_date.getMeasuredWidth() ));
    	textLabel_text.setWidth(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);
    	//textLabel_text.setCustomMax(metrics.widthPixels - textLabel_date.getMeasuredWidth() - 20);
    	
    	
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((msg == null) ? 0 : msg.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.msg == obj)
			return true;
		if (this.msg != null && obj != null && msg.getClass() == obj.getClass()){
			mMessage other = (mMessage) obj;
			return msg.equals(other);
		}

		
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MsgListItem other = (MsgListItem) obj;
		if (msg == null) {
			if (other.msg != null)
				return false;
		} else if (!msg.equals(other.msg))
			return false;
		return true;
	}
	
	

	
}
