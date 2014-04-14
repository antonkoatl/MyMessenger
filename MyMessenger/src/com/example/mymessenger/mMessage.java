package com.example.mymessenger;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

public class mMessage implements Parcelable {
	public static final int OUT = 1;
	public static final int READED = 2;
	
	public mContact respondent; //Собеседник
	
	public String text;
	public Time sendTime;
	
	public boolean out;
	public boolean readed;
		
	public mMessage() {
		sendTime = new Time();
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(respondent, flags);
		dest.writeString(text);
		dest.writeLong(sendTime.toMillis(true));
		dest.writeBooleanArray(new boolean[]{out, readed});
	}

	public mMessage(Parcel sour) {
		sendTime = new Time();
		
		respondent = (mContact) sour.readParcelable(mContact.class.getClassLoader());
		text = sour.readString();
		sendTime.set(sour.readLong());
		boolean ta[] = sour.createBooleanArray(); 
		out = ta[0];
		readed = ta[1];
	}
	
	public static final Parcelable.Creator<mMessage> CREATOR = new Parcelable.Creator<mMessage>() { 
		public mMessage createFromParcel(Parcel in) { 
			return new mMessage(in); 
		}   
		
		public mMessage[] newArray(int size) { 
			return new mMessage[size]; 
		} 
	};
	
	@Override
	public boolean equals(Object o){
		if(o instanceof mMessage){
			mMessage toCompare = (mMessage) o;
		    return this.text.equals(toCompare.text) && Time.compare(this.sendTime, toCompare.sendTime) == 0;
		}
        return false;
	}
}
