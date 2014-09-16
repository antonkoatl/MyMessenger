package com.example.mymessenger;

import android.graphics.Bitmap;
import android.text.format.Time;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class mDialog {
	public List<mMessage> messages;
	public List<mContact> participants;
	public int msg_service_type;

    public long chat_id;
    public Bitmap icon_50;
    public String icon_50_url;
	
    public mMessage last_msg;
    public String last_msg_id;
	
	public DlgListItem dlg_ui_helper;
	public String title;
		
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<mContact>();
	}
	

	public mDialog(mContact cnt) {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<mContact>();
		this.participants.add(cnt);
	}

    public mDialog(mDialog dlg) {
        messages = new ArrayList<mMessage>(dlg.messages);
        participants = new ArrayList<mContact>(dlg.participants);

        this.chat_id = dlg.chat_id;
        this.msg_service_type = dlg.msg_service_type;
        this.last_msg = dlg.last_msg;
        this.last_msg_id = dlg.last_msg_id;
        this.title = dlg.title;
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

			for(mMessage m : dlg.messages){
				if(messages.contains(m))break;
				else {
					int pos = 0;
					while(messages.get(pos).sendTime.after(m.sendTime))pos++;
					messages.add(pos, m);
				}
			}
			
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
}
