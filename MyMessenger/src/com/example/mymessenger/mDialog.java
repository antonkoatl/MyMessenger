package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

public class mDialog {
	public List<mMessage> messages;
	public List<String> participants;
	public List<String> participants_names;
	public int mservice;
	public String snippet;
	
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<String>();
		participants_names = new ArrayList<String>();
	}
	
	public String getParticipants(){
		String res = participants.get(0);
		
		if(participants.size() > 1) {
			int i = 1;
			do {
				res += ", " + participants.get(i);
			} while (i < participants.size());
		}
		
		return res;
	}
	
	public String getParticipantsNames(){
		String res;
		if(participants_names.size() > 0){
			res = participants_names.get(0);
			
			if(participants_names.size() > 1) {
				int i = 1;
				do {
					res += ", " + participants_names.get(i);
				} while (i < participants_names.size());
			}
		} else {
			res = "---";
		}
		
		return res;
	}
}
