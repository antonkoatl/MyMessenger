package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

public class mDialog {
	public List<mMessage> messages;
	public List<mContact> participants;
	public int mservice;
	public String snippet;
	
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<mContact>();
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
}
