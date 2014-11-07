package com.example.mymessenger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;
import android.view.View;

import com.example.mymessenger.services.MessageService.MessageService;

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
                    icon_50_drawable = participants.get(0).getIconDrawable(context);
                } else {
                    icon_50_drawable = new DrawableDL(icon_50_url, mGlobal.scale(50), mGlobal.scale(50), context);
                }
            } else {
                icon_50_drawable = participants.get(0).getIconDrawable(context);
            }
        }
        return icon_50_drawable;
    }
}
