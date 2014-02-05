package com.example.mymessenger;

public class mContact {
	public String address;
	public String name;
	
	public mContact() {

	}
	
	public mContact(String address) {
		this.address = address;
	}
	
	public boolean equals(mContact cnt) {
	    return this.address.equals(cnt.address);
	}
}
