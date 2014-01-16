package com.example.mymessenger.services;

import java.util.List;

import com.example.mymessenger.mDialog;

public interface MessageService {
	public List<mDialog> getDialogs();
	public String getName();
}
