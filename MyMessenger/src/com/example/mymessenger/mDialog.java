package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import android.text.Spannable;
import android.text.format.Time;
import android.view.View;

public class mDialog {
	public List<mMessage> messages;
	public List<mContact> participants;
	public int msg_service_type;
	public long chat_id;
	
	public Time last_msg_time;
	public String snippet;
	public int snippet_out;
	
	public DlgListItem dlg_ui_helper;
	public String title;
		
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<mContact>();
		last_msg_time = new Time();
	}
	

	public mDialog(mContact cnt) {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<mContact>();
		last_msg_time = new Time();
		this.participants.add(cnt);
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
		return last_msg_time;
	}


	public int getMsgServiceType() {
		return msg_service_type;
	}
		
	@Override
	public boolean equals(Object o){
		if(o instanceof mDialog){
			mDialog toCompare = (mDialog) o;
		    return this.participants.equals(toCompare.participants);
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
		if(last_msg_time.before(dlg.last_msg_time) && participants.equals(dlg.participants)){
			last_msg_time.set(dlg.last_msg_time);
			snippet = dlg.snippet;
			
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

}
