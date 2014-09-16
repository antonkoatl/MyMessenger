package com.example.mymessenger;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mymessenger.services.MessageService.MessageService;

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
        textLabel.setText(app.msManager.getService(dlg.getMsgServiceType()).getServiceName());

        textLabel = (TextView) view.findViewById(R.id.dlgview_dlgname);
        textLabel.setText(dlg.isChat() ? dlg.getDialogTitle() : dlg.getParticipantsNames());

        textLabel = (TextView) view.findViewById(R.id.dlgview_dlgdate);
        textLabel.setText(dlg.getLastMessageTime().format("%H:%M %d.%m.%Y"));


        textLabel = (TextView) view.findViewById(R.id.dlgview_dlgtext);

        if (snippet_spannable_cache == null) {
            RunnableUpdateDlgItem r = new RunnableUpdateDlgItem(view);
            MyApplication.handler1.post(r);
            textLabel.setText(dlg.last_msg.text);
        } else {
            textLabel.setText(snippet_spannable_cache);
        }

        ImageView iv = (ImageView) view.findViewById(R.id.dlgview_iconmain);
        if (dlg.isChat()){
            if(dlg.icon_50 != null)
                iv.setImageBitmap(dlg.icon_50);
            else {
                if(dlg.icon_50_url == null)
                    dlg.icon_50_url = dlg.participants.get(0).icon_50_url;

                download_waiter tw = new download_waiter(dlg.icon_50_url) {
                    ImageView iv;
                    mDialog dlg;

                    @Override
                    public void onDownloadComplete() {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inDensity = DisplayMetrics.DENSITY_LOW;
                        options.inScaled = true;
                        options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
                        dlg.icon_50 = BitmapFactory.decodeFile(filepath, options);
                        iv.setImageBitmap(dlg.icon_50);
                    }

                    public download_waiter setParams(ImageView iv, mDialog dlg) {
                        this.iv = iv;
                        this.dlg = dlg;
                        return this;
                    }


                }.setParams(iv, dlg);

                app.dl_waiters.add(tw);

                Intent intent = new Intent(iv.getContext(), DownloadService.class);
                intent.putExtra("url", dlg.icon_50_url);
                iv.getContext().getApplicationContext().startService(intent);
            }
        } else {
            showContactImage(dlg.participants.get(0), iv);
        }

        iv = (ImageView) view.findViewById(R.id.dlgview_dlgtexticon);
        if (dlg.last_msg.isOut()) {
            mContact my_contact = app.msManager.getService(dlg.msg_service_type).getMyContact();
            showContactImageSmall(my_contact, iv);
        } else if(dlg.isChat()){
            mContact contact = dlg.last_msg.respondent;
            showContactImageSmall(contact, iv);

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
			snippet_spannable_cache = ChatMessageFormatter.getSmiledText(view.getContext(), dlg.last_msg.text, dlg.getMsgServiceType(), textLabel.getLineHeight());

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
	
	private void showContactImage(mContact cnt, ImageView iv){
        if (cnt.icon_50 != null) {
            iv.setImageBitmap(cnt.icon_50);
        } else if (cnt.icon_50_url != null) {
            download_waiter tw = new download_waiter(cnt.icon_50_url) {
                ImageView iv;
                mContact cnt;

                @Override
                public void onDownloadComplete() {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inDensity = DisplayMetrics.DENSITY_LOW;
                    options.inScaled = true;
                    options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
                    cnt.icon_50 = BitmapFactory.decodeFile(filepath, options);
                    iv.setImageBitmap(cnt.icon_50);
                }

                public download_waiter setParams(ImageView iv, mContact cnt) {
                    this.iv = iv;
                    this.cnt = cnt;
                    return this;
                }


            }.setParams(iv, cnt);

            app.dl_waiters.add(tw);

            Intent intent = new Intent(iv.getContext(), DownloadService.class);
            intent.putExtra("url", cnt.icon_50_url);
            iv.getContext().getApplicationContext().startService(intent);
        }
    }

    private void showContactImageSmall(mContact cnt, ImageView iv){
        if (cnt.icon_50 != null) {
            iv.setImageBitmap(cnt.icon_50);
        } else if (cnt.icon_50_url != null) {
            download_waiter tw = new download_waiter(cnt.icon_50_url) {
                ImageView iv;
                mContact cnt;

                @Override
                public void onDownloadComplete() {
                    //BitmapFactory.Options options = new BitmapFactory.Options();
                    //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    cnt.icon_50 = BitmapFactory.decodeFile(filepath);
                    iv.setImageBitmap(cnt.icon_50);
                }

                public download_waiter setParams(ImageView iv, mContact cnt) {
                    this.iv = iv;
                    this.cnt = cnt;
                    return this;
                }


            }.setParams(iv, cnt);

            app.dl_waiters.add(tw);

            Intent intent = new Intent(iv.getContext(), DownloadService.class);
            intent.putExtra("url", cnt.icon_50_url);
            iv.getContext().getApplicationContext().startService(intent);
        }
    }
}
