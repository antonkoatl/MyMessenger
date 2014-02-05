package com.example.mymessenger.services;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.vk.sdk.api.VKRequest.VKRequestListener;

public abstract class VKRequestListenerWithCallback<T> extends VKRequestListener {
	AsyncTaskCompleteListener<T> callback;
	
	public VKRequestListenerWithCallback(AsyncTaskCompleteListener<T> cb) {
		super();
		this.callback = cb;
		
	}
}
