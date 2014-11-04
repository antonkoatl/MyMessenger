package com.example.mymessenger;

import android.content.SharedPreferences;
import android.content.res.Resources;


public class mGlobal {
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

    public static int msg_max_width = 368;


    public static int scale(float dip) {
        return Math.round(displayDensity * dip);
    }
	
	
	public static String LongToHexStr(long code) {
        int i = 0; 
        while (i <= 4) {
            int cc = (int) ((code >> ((i) * 16)) & 65535);
            if (cc == 0) {
                break;
            }
            i++;
        }

        if(i == 1)return String.format("%04X", code);
        if(i == 2)return String.format("%08X", code);
        if(i == 3)return String.format("%12X", code);
        if(i == 4)return String.format("%16X", code);
        return "";
    }

    public static String LongToHexStr32nozero(long code) {
        return String.format("%x", code);
    }

    public static class IntegerMutable {
        public int value;

        public IntegerMutable(int i) {
            this.value = i;
        }

        @Override
        public boolean equals(Object that){
            if(that instanceof IntegerMutable){
                IntegerMutable toCompare = (IntegerMutable) that;
                return this.value == toCompare.value;
            }
            return false;
        }
    }

}