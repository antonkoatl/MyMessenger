package com.example.mymessenger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.View;

import com.example.mymessenger.services.MessageService.MessageService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class mDialog implements Parcelable {
	public List<mContact> participants;
	public int msg_service_type;

    public long chat_id;
    public Bitmap icon_50;
    public String icon_50_url;
    Drawable icon_50_drawable;

    public mMessage last_msg;
    public String last_msg_id;
	
	public DlgListItem dlg_ui_helper;
	public String title;
		
	public mDialog() {
		participants = new ArrayList<mContact>();
	}
	

	public mDialog(mContact cnt) {
		participants = new ArrayList<mContact>();
		this.participants.add(cnt);
	}

    public mDialog(mDialog dlg) {
        participants = new ArrayList<mContact>(dlg.participants);

        this.chat_id = dlg.chat_id;
        this.msg_service_type = dlg.msg_service_type;
        this.last_msg = dlg.last_msg;
        this.last_msg_id = dlg.last_msg_id;
        this.title = dlg.title;
    }

    public mDialog(Parcel sour){
        this.chat_id = sour.readLong();
        sour.readList(this.participants, mContact.class.getClassLoader());
        this.title = sour.readString();
        this.last_msg = sour.readParcelable(mMessage.class.getClassLoader());
        this.last_msg_id = sour.readString();
        this.msg_service_type = sour.readInt();
    }


    public String getParticipantsNames(){
		String res;
		if(participants.size() > 0){
			res = participants.get(0).name == null ? participants.get(0).address : participants.get(0).name;
			
			if(participants.size() > 1) {
				int i = 1;
				do {
					res += ", " + (participants.get(i).name == null ? participants.get(i).address : participants.get(i).name);
					i++;
				} while (i < participants.size());
			}
		} else {
			res = "---";
		}
		
		return res;
	}


	public String getParticipants() {
		String res;
		if(participants.size() > 0){
			res = participants.get(0).address;
			
			if(participants.size() > 1) {
				int i = 1;
				do {
					res += ", " + participants.get(i).address;
					i++;
				} while (i < participants.size());
			}
		} else {
			res = "---";
		}
		
		return res;
	}


	public Time getLastMessageTime() {
		return last_msg == null ? null : last_msg.sendTime;
	}


	public int getMsgServiceType() {
		return msg_service_type;
	}
		
	@Override
	public boolean equals(Object o){
		if(o instanceof mDialog){
			mDialog toCompare = (mDialog) o;
            if(chat_id != 0){
                return this.chat_id == toCompare.chat_id;
            } else {
                return this.participants.equals(toCompare.participants) && this.chat_id == toCompare.chat_id;
            }
		}
		return false;
	}


	public String getParticipantsAddresses() {
		String res = "";
		for(mContact cnt : participants){
			if(res.length() == 0)
				res += cnt.address;
			else
				res += "," + cnt.address;
		}		
		return res;
	}


	public void update(mDialog dlg) {
		if(getLastMessageTime().before(dlg.getLastMessageTime()) && participants.equals(dlg.participants)){
			last_msg = dlg.last_msg;



			
			if(dlg_ui_helper != null)dlg_ui_helper.update();
		}		
	}
	
	public boolean compareParticipants(mDialog dlg){ //??
		if(participants.size() == dlg.participants.size()){
			for(mContact cnt : participants){
				boolean fl = false;
				for(mContact cnt2 : dlg.participants){
					if(cnt.equals(cnt2)){
						fl = true;
						break;
					}
				}
				if(!fl)return false;
			}
			return true;
		} else return false;
	}


	public void setupView(View view) {
		if(dlg_ui_helper == null)dlg_ui_helper = new DlgListItem(this, (MyApplication) view.getContext().getApplicationContext() );
		
		dlg_ui_helper.setupView(view);
	}


	public CharSequence getDialogTitle() {
		if(chat_id != 0){
			return title;
		} else 
			return participants.get(0).name == null ? participants.get(0).address : participants.get(0).name;
	}

    public boolean isChat() {
        return chat_id != 0;
    }

    public void setLastMsg(mMessage msg) {
        this.last_msg = msg;
        this.last_msg_id = msg.id;
    }

    public void parseParticipants(String participants_str, MessageService ms) {
        if(participants_str != null && participants_str.length() > 0){
            for(String address : participants_str.split(",")){
                participants.add( ms.getContact(address) );
            }
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(chat_id);
        dest.writeList(participants);
        dest.writeString(title);
        dest.writeParcelable(last_msg, flags);
        dest.writeString(last_msg_id);
        dest.writeInt(msg_service_type);
    }

    public Drawable getIconDrawable(Context context) {
        if (icon_50_drawable == null) {
            if(isChat()) {
                if (icon_50_url == null) {
                    if(participants.size() < 2){
                        icon_50_drawable = context.getResources().getDrawable( R.drawable.ic_place_users_big );
                        return icon_50_drawable;
                    }

                    icon_50_drawable = new DrawableDL(null, mGlobal.scale(50), mGlobal.scale(50), context);

                    MyApplication app = (MyApplication) context.getApplicationContext();

                    download_waiter tw = new download_waiter(participants.get(0).icon_50_url) {
                        mDialog dlg;
                        MyApplication app;
                        String[] filepaths = new String[4];
                        int n = 0;
                        int i = 0;

                        @Override
                        public void onDownloadComplete() {
                            filepaths[i] = filepath;
                            i++;
                            while(i < n){
                                url = dlg.participants.get(i).icon_50_url;
                                app.dl_waiters.add(this);

                                Intent intent = new Intent(app.getApplicationContext(), DownloadService.class);
                                intent.putExtra("url", url);
                                app.startService(intent);
                                return;
                            }

                            Bitmap result = Bitmap.createBitmap( mGlobal.scale(50), mGlobal.scale(50), Bitmap.Config.ARGB_8888);
                            Canvas cv = new Canvas(result);
                            Paint paint = new Paint();

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inDensity = DisplayMetrics.DENSITY_LOW;
                            options.inScaled = true;
                            options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;

                            Bitmap bitmap;
                            Rect r;

                            if(n == 4) {
                                bitmap = BitmapFactory.decodeFile(filepaths[0], options);
                                r = new Rect(0, 0, mGlobal.scale(50) / 2, mGlobal.scale(50) / 2);
                                cv.drawBitmap(bitmap, null, r, paint);

                                bitmap = BitmapFactory.decodeFile(filepaths[1], options);
                                r.set(mGlobal.scale(50) / 2, 0, mGlobal.scale(50), mGlobal.scale(50) / 2);
                                cv.drawBitmap(bitmap, null, r, paint);

                                bitmap = BitmapFactory.decodeFile(filepaths[2], options);
                                r.set(0, mGlobal.scale(50) / 2, mGlobal.scale(50) / 2, mGlobal.scale(50));
                                cv.drawBitmap(bitmap, null, r, paint);

                                bitmap = BitmapFactory.decodeFile(filepaths[3], options);
                                r.set(mGlobal.scale(50) / 2, mGlobal.scale(50) / 2, mGlobal.scale(50), mGlobal.scale(50));
                                cv.drawBitmap(bitmap, null, r, paint);
                            }

                            if(n == 2){
                                bitmap = BitmapFactory.decodeFile(filepaths[0], options);
                                Rect r2 = new Rect((int) (bitmap.getWidth() * 0.25), 0, (int) (bitmap.getWidth() * 0.75), bitmap.getHeight());
                                r = new Rect(0, 0, mGlobal.scale(50) / 2, mGlobal.scale(50));
                                cv.drawBitmap(bitmap, r2, r, paint);

                                bitmap = BitmapFactory.decodeFile(filepaths[1], options);
                                r.set(mGlobal.scale(50) / 2, 0, mGlobal.scale(50), mGlobal.scale(50));
                                cv.drawBitmap(bitmap, r2, r, paint);
                            }

                            ((DrawableDL) icon_50_drawable).setBitmap(result);
                            //DrawableDL.this.invalidateSelf();
                        }

                        public download_waiter setParams(mDialog dlg, MyApplication app) {
                            this.dlg = dlg;
                            this.app = app;
                            if(dlg.participants.size() > 3){
                                n = 4;
                            } else if (dlg.participants.size() > 1){
                                n = 2;
                            }
                            return this;
                        }


                    }.setParams(this, app);



                    app.dl_waiters.add(tw);

                    Intent intent = new Intent(context, DownloadService.class);
                    intent.putExtra("url", participants.get(0).icon_50_url);
                    app.startService(intent);
                } else {
                    icon_50_drawable = new DrawableDL(icon_50_url, mGlobal.scale(50), mGlobal.scale(50), context);
                }
            } else {
                return participants.get(0).getIconDrawable(context);
            }
        }
        return icon_50_drawable;
    }
}
