package com.example.mymessenger;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.Spannable.Factory;


public class Global {
	public static float displayDensity;
	public static Resources res;
	public static boolean inited;
	public static boolean isTablet = false;


	static {
		try {
            res = MyApplication.context.getResources();
            displayDensity = MyApplication.context.getResources().getDisplayMetrics().density;
            SharedPreferences prefs = MyApplication.context.getApplicationContext().getSharedPreferences(null, 0);
           
            inited = true;
        } catch (Exception e) {
        }

		
	}


	public static int scale(float dip) {
        return Math.round(displayDensity * dip);
    }
	

}