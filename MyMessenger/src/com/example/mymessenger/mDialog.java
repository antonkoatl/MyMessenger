package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

public class mDialog {
	public List<mMessage> messages;
	public List<String> participants;
	
	public mDialog() {
		messages = new ArrayList<mMessage>();
		participants = new ArrayList<String>();
	}
}
