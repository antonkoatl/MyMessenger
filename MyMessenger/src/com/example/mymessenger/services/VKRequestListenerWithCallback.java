package com.example.mymessenger.services;

import android.util.Log;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MyApplication;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;

public abstract class VKRequestListenerWithCallback<T> extends VKRequestListener {
	AsyncTaskCompleteListener<T> callback;
	Vk vk;
	
	public VKRequestListenerWithCallback(AsyncTaskCompleteListener<T> cb, Vk vk) {
		super();
		this.callback = cb;
		this.vk = vk;
	}
	
	@Override
    public void onError(VKError error) {
    	Log.w("VKRequestListener", error.request.methodName +  " :: onError " + error.errorCode + ", " + error.errorMessage);
    	if(error.apiError != null) vk.HandleApiError(error);
        // Ошибка. Сообщаем пользователю об error.
    }
	
    @Override
    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
    	Log.d("VKRequestListener", "attemptFailed" );
        // Неудачная попытка. В аргументах имеется номер попытки и общее их количество.
    }
}
