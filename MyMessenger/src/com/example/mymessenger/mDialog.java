package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import android.text.format.Time;

public class mDialog {
	public List<mMessage> messages;
	public List<mContact> participants;
	public int msg_service;
	
	public Time last_msg_time;
	public String snippet;
	
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<mContact>();
		last_msg_time = new Time();
	}
	

	public String getParticipantsNames(){
		String res;
		if(participants.size() > 0){
			res = participants.get(0).name == null ? participants.get(0).address : participants.get(0).name;
			
			if(participants.size() > 1) {
				int i = 1;
				do {
					res += ", " + (participants.get(0).name == null ? participants.get(0).address : participants.get(0).name);
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


	public int getMsgService() {
		return msg_service;
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
		for(mContact cnt : participants)res += cnt.address;
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

}
