package com.example.mymessenger.ui;

import android.util.Log;
import android.view.View;

public class PageTransformer implements ViewPagerAdvanced.PageTransformer {
    private static final float MIN_SCALE = 0.85f;
    private static final float MIN_ALPHA = 0.5f;

    public void transformPage(View view, float position, ViewPagerAdvanced.ItemInfo ii) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            //view.setAlpha(0);

        } else if (position < 0) {
            //float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
            //view.setScaleX(scaleFactor);
            //view.setScaleY(scaleFactor);

            float tWidth = 0.3f;
            ViewPagerAdvanced.LayoutParams lp = (ViewPagerAdvanced.LayoutParams) view.getLayoutParams();
            lp.widthFactor = tWidth;
            view.setLayoutParams(lp);
            //view.setScaleX(0.42f + 0.58f*k);
            ii.widthFactor = tWidth;
            Log.d("slider", "tWidth = " + String.valueOf(tWidth));

        } else if (position < 0.3f) {
            //view.setScaleX(1);
            //view.setScaleY(1);

            float k = (position/0.3f);
            float tWidth = 0.3f + k*0.4f;
            ViewPagerAdvanced.LayoutParams lp = (ViewPagerAdvanced.LayoutParams) view.getLayoutParams();
            lp.widthFactor = tWidth;
            view.setLayoutParams(lp);
            //view.setScaleX(0.42f + 0.58f*k);
            ii.widthFactor = tWidth;
            Log.d("slider", "tWidth = " + String.valueOf(tWidth));


        } else if (position <= 1) { // [0.3,1]
            //view.setScaleX(1);
            //view.setScaleY(1);


            float tWidth = 0.7f;
            ViewPagerAdvanced.LayoutParams lp = (ViewPagerAdvanced.LayoutParams) view.getLayoutParams();
            lp.widthFactor = tWidth;
            view.setLayoutParams(lp);
            //view.setScaleX(0.42f + 0.58f*k);
            ii.widthFactor = tWidth;
            Log.d("slider", "tWidth = " + String.valueOf(tWidth));

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            //view.setAlpha(0);
        }

        Log.d("transformPage", String.valueOf(position));
    }
}
