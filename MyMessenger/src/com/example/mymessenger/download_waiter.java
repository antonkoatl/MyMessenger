package com.example.mymessenger;

public abstract class download_waiter{
	String url;
	protected String filepath;
	
	public download_waiter(String url){
		this.url = url;
	}
	
	public abstract void onDownloadComplete();

	public void setFilePath(String filepath) {
		this.filepath = filepath;
	}
}
