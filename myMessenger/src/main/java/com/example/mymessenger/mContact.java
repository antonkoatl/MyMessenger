package com.example.mymessenger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class mContact implements Parcelable, Comparable<mContact> {
	public String address;
	public String name;
	public boolean online;
	public Bitmap icon_50;
	public String icon_50_url;
    Drawable icon_50_drawable;
	
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
		dest.writeParcelable(icon_50, flags);
	}
	
	public mContact(Parcel sour){
		address = sour.readString();
		name = sour.readString();
		online = sour.createBooleanArray()[0];
		icon_50 = (Bitmap) sour.readParcelable(Bitmap.class.getClassLoader());
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

    public void update(mContact result) {
        this.address = result.address;
        this.name = result.name;
        this.icon_50_url = result.icon_50_url;

        clearCached();
    }

    public void clearCached(){
        icon_50 = null;
        icon_50_drawable = null;
    }

    public Drawable getIconDrawable(Context context) {
        if (icon_50_drawable == null) {
            if(icon_50_url == null){
                icon_50_drawable = context.getResources().getDrawable( R.drawable.sample_image );
            } else {
                icon_50_drawable = new DrawableDL(icon_50_url, mGlobal.scale(50), mGlobal.scale(50), context);
            }
        }
        return icon_50_drawable;
    }
}
