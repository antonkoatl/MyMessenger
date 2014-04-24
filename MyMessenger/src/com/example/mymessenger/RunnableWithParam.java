package com.example.mymessenger;

public abstract class RunnableWithParam<T> implements Runnable {
	protected T param;
	
	public RunnableWithParam<T> setParam(T param){
		this.param = param;
		return this;
	}
}
