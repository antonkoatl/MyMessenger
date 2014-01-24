package com.example.mymessenger;

public interface AsyncTaskCompleteListener<T> {

    public void onTaskComplete(T result);
}