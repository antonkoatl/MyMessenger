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
	
	public int loading_msgs = 0;
	
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

}
