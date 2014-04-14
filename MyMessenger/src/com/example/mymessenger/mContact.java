package com.example.mymessenger;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class mContact implements Parcelable, Comparable<mContact> {
	public String address;
	public String name;
	public boolean online;
	public Bitmap icon_100;
	
	public mContact(String address) {
		this.address = address;
	}
	
	public boolean equals(mContact cnt) {
	    return this.address.equals(cnt.address);
	}
	
	@Override
	public boolean equals(Object o){
		  if(o instanceof mContact){
			  mContact toCompare = (mContact) o;
		    return this.address.equals(toCompare.address);
		  }
		  return false;
		}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(address);
		dest.writeString(name);
		dest.writeBooleanArray(new boolean[]{online});
		dest.writeParcelable(icon_100, flags);
	}
	
	public mContact(Parcel sour){
		address = sour.readString();
		name = sour.readString();
		online = sour.createBooleanArray()[0];
		icon_100 = (Bitmap) sour.readParcelable(Bitmap.class.getClassLoader());
	}
	
	public static final Parcelable.Creator<mContact> CREATOR = new Parcelable.Creator<mContact>() { 
		public mContact createFromParcel(Parcel in) { 
			return new mContact(in); 
		}   
		
		public mContact[] newArray(int size) { 
			return new mContact[size]; 
		} 
	};

	public String getName() {
		return name == null ? address : name;
	}

	@Override
	public int compareTo(mContact another) {
		return address.compareTo(another.address);
	}
}
