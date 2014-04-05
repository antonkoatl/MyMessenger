package com.example.mymessenger;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

public class mMessage implements Parcelable {
	public mContact respondent; //Собеседник
	
	public String text;
	public Time sendTime;
	public String ReadState;
	public boolean out; 
		
	public mMessage() {

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
		dest.writeString(ReadState);
		dest.writeBooleanArray(new boolean[]{out});
	}

	public mMessage(Parcel sour) {
		respondent = (mContact) sour.readParcelable(mContact.class.getClassLoader());
		text = sour.readString();
		sendTime = new Time();
		sendTime.set(sour.readLong());
		ReadState = sour.readString();
		out = sour.createBooleanArray()[0];
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
