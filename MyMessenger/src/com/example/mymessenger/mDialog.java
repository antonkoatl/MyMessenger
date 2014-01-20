package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

public class mDialog {
	public List<mMessage> messages;
	public List<String> participants;
	public int mservice;
	
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<String>();
	}
	
	public String getParticipantsNames(){
		String res = participants.get(0);
		
		if(participants.size() > 1) {
			int i = 1;
			do {
				res += ", " + participants.get(i);
			} while (i < participants.size());
		}
		
		return res;
	}
}
