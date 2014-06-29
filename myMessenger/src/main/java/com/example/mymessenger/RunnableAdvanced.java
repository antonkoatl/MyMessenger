package com.example.mymessenger;

public abstract class RunnableAdvanced<T> implements Runnable {
	private boolean kill = false;
	protected T param;

	@Override
	public void run() {
		while(!kill)run_iteration();
	}
	
	public void run_iteration(){
		
	}
	
	public void kill(){
		kill=true;
	}
	
	public RunnableAdvanced<T> setParam(T param){
		this.param = param;
		return this;
	}

}
