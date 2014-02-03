package com.example.mymessenger.services;

import java.util.List;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.vk.sdk.api.VKRequest.VKRequestListener;

public abstract class VKRequestListenerWithCallback<T> extends VKRequestListener {
	AsyncTaskCompleteListener<List<T>> callback;
	
	public VKRequestListenerWithCallback(AsyncTaskCompleteListener<List<T>> cb) {
		super();
		this.callback = cb;
		
	}
}
