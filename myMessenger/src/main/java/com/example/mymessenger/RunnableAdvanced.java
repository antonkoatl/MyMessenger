package com.example.mymessenger;

public abstract class RunnableAdvanced<T> implements Runnable {
	private boolean stop = false;
	protected T param;

	@Override
	public void run() {
		while(!stop && !Thread.currentThread().isInterrupted())run_iteration();
	}
	
	public void run_iteration(){
		
	}
	
	public void stop(){
        stop=true;
	}

    public void unstop(){
        stop=false;
    }
	
	public RunnableAdvanced<T> setParam(T param){
		this.param = param;
		return this;
	}

}
