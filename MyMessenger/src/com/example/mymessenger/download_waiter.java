package com.example.mymessenger;

public class download_waiter{
	String url;
	String type;
	Object obj;
	
	public download_waiter(String url, String type, Object obj){
		this.url = url;
		this.type = type;
		this.obj = obj;
	}
}
