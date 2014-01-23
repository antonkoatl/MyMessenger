package com.example.mymessenger;

import android.text.format.Time;

public class mMessage {
	public String sender;
	public String address;
	public String text;
	public Time sendTime;
	public String ReadState;
	
	public CharSequence sender_name;
	public CharSequence address_name;
	
	public CharSequence getSenderName() {
		return (sender_name.length() > 0 ? sender_name : sender);
	}
	
	public CharSequence getAddressName() {
		return (address_name.length() > 0 ? address_name : address);
	}
}
