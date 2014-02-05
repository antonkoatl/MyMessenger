package com.example.mymessenger;

import android.text.format.Time;

public class mMessage {
	public mContact sender;
	public String text;
	public Time sendTime;
	public String ReadState;
	
	public CharSequence sender_name;
	public CharSequence address_name;
	
	public CharSequence getSenderName() {
		return sender.name == null ? sender.address : sender.name;
	}

	public mMessage() {

	}

}
