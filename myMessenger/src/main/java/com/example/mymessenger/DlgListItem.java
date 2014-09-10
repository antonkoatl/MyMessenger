package com.example.mymessenger;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DlgListItem {
	MyApplication app;
	public mDialog dlg;
	public Spannable snippet_spannable_cache = null;
	
	DlgListItem(mDialog dlg, MyApplication app){
		this.dlg = dlg;
		this.app = app;
	}
	
	public void setupView(View view) {

	    TextView textLabel = (TextView) view.findViewById(R.id.dlgview_dlgsername);
    	textLabel.setText( app.msManager.getService( dlg.getMsgServiceType() ).getServiceName() );
	    
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgname);
        textLabel.setText( dlg.isChat() ? dlg.getDialogTitle() : dlg.getParticipantsNames() );
    	
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgdate);
    	textLabel.setText( dlg.getLastMessageTime().format("%H:%M %d.%m.%Y") );
    	
        
    	textLabel = (TextView) view.findViewById(R.id.dlgview_dlgtext);
    	
    	if(snippet_spannable_cache == null){
    		RunnableUpdateDlgItem r = new RunnableUpdateDlgItem(view);
			MyApplication.handler1.post(r);
			textLabel.setText( dlg.snippet );
    	} else {
    		textLabel.setText( snippet_spannable_cache );
    	}
    	
    	ImageView iv = (ImageView) view.findViewById(R.id.dlgview_iconmain);
    	if(dlg.participants.get(0).icon_50 != null){
	    	iv.setImageBitmap( dlg.participants.get(0).icon_50);
    	} else if(dlg.participants.get(0).icon_50_url != null){
    		download_waiter tw = new download_waiter(dlg.participants.get(0).icon_50_url){
    			ImageView iv;
    			mContact cnt;
    			
				@Override
				public void onDownloadComplete() {
					BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inDensity = DisplayMetrics.DENSITY_LOW;
                    options.inScaled = true;
                    options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
					cnt.icon_50 = BitmapFactory.decodeFile(filepath, options);
					iv.setImageBitmap( cnt.icon_50);
				}
				
				public download_waiter setParams(ImageView iv, mContact cnt){
					this.iv = iv;
					this.cnt = cnt;
					return this;
				}
				
    			
    		}.setParams(iv, dlg.participants.get(0));
    		
            app.dl_waiters.add(tw);
                       
            Intent intent = new Intent(view.getContext(), DownloadService.class);
            intent.putExtra("url", dlg.participants.get(0).icon_50_url);
            view.getContext().getApplicationContext().startService(intent);
    	}
    		
		iv = (ImageView) view.findViewById(R.id.dlgview_dlgtexticon);
	    if(dlg.snippet_out == 1){
	    	mContact my_contact = app.msManager.getService(dlg.msg_service_type).getMyContact();
	    	if(my_contact.icon_50 != null){
	    		iv.setImageBitmap( my_contact.icon_50);
	     	} else if(my_contact.icon_50_url != null){
	     		download_waiter tw = new download_waiter(my_contact.icon_50_url){
	     			ImageView iv;
	     			mContact cnt;
	     			
	 				@Override
	 				public void onDownloadComplete() {
	 					//BitmapFactory.Options options = new BitmapFactory.Options();
	 					//options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	 					cnt.icon_50 = BitmapFactory.decodeFile(filepath);
	 					iv.setImageBitmap( cnt.icon_50);
	 				}
	 				
	 				public download_waiter setParams(ImageView iv, mContact cnt){
	 					this.iv = iv;
	 					this.cnt = cnt;
	 					return this;
	 				}
	 				
	     			
	     		}.setParams(iv, my_contact);
	     		
	             app.dl_waiters.add(tw);
	             
	             Intent intent = new Intent(view.getContext(), DownloadService.class);
	             intent.putExtra("url", my_contact.icon_50_url);
	             view.getContext().getApplicationContext().startService(intent);
	     	} 
	    } else {
     		iv.setVisibility(View.INVISIBLE);
     		iv.getLayoutParams().width = 0;
     		iv.requestLayout();
        }

    	//Log.d("MyDialogsAdapter", data.size() + " : " + position + " : " + dlg.getParticipantsNames());
	}
	
	class RunnableUpdateDlgItem implements Runnable{
		View view;
		
		RunnableUpdateDlgItem(View view){
			this.view = view;
		}

		@Override
		public void run() {
			final TextView textLabel = (TextView) view.findViewById(R.id.dlgview_dlgtext);
			snippet_spannable_cache = ChatMessageFormatter.getSmiledText(view.getContext(), dlg.snippet, dlg.getMsgServiceType(), textLabel.getLineHeight());

            view.post(new Runnable() {
			     @Override
			     public void run() {
			    	 textLabel.setText( snippet_spannable_cache );
			    }
			});
		}		
	}


	public void update() {
		snippet_spannable_cache = null;
	}
	
	
}
