package com.example.mymessenger;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

public class mMessage implements Parcelable {
	public mContact sender;
	public String text;
	public Time sendTime;
	public String ReadState;
	
	public String sender_name;
	public String address_name;
	
	public String getSenderName() {
		return sender.name == null ? sender.address : sender.name;
	}

	public mMessage() {

	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(sender, flags);
		dest.writeString(text);
		dest.writeLong(sendTime.toMillis(true));
		dest.writeString(ReadState);
		dest.writeString(sender_name);
		dest.writeString(address_name);
	}

	public mMessage(Parcel sour) {
		sender = (mContact) sour.readParcelable(mContact.class.getClassLoader());
		text = sour.readString();
		sendTime = new Time();
		sendTime.set(sour.readLong());
		ReadState = sour.readString();
		sender_name = sour.readString();
		address_name = sour.readString();
	}
	
	public static final Parcelable.Creator<mMessage> CREATOR = new Parcelable.Creator<mMessage>() { 
		public mMessage createFromParcel(Parcel in) { 
			return new mMessage(in); 
		}   
		
		public mMessage[] newArray(int size) { 
			return new mMessage[size]; 
		} 
	};
}
