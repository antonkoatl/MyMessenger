package com.example.mymessenger;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.widget.TextView;

public class WrapWidthTextView extends TextView {
	private float custom_max = 0;

	public WrapWidthTextView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public WrapWidthTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public WrapWidthTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	    Layout layout = getLayout();
	    if (layout != null) {
	        int width = (int)FloatMath.ceil(getMaxLineWidth(layout));
	        //widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
	        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	        //width = getMeasuredWidth();
	        int height = getMeasuredHeight();
	        setMeasuredDimension(width, height);
	        
	        
	    }
	}

	private float getMaxLineWidth(Layout layout) {
	    float max_width = 0.0f;
	    int lines = layout.getLineCount();
	    for (int i = 0; i < lines; i++) {
	        if (layout.getLineWidth(i) > max_width) {
	            max_width = layout.getLineWidth(i);
	        }
	    }
	    
	    max_width = max_width + getCompoundPaddingLeft() + getCompoundPaddingRight();
	    
	    if(custom_max > 0 && max_width > custom_max){
	    	max_width = custom_max;
	    }
	    return max_width;
	}
	
	public void setCustomMax(float dp){
		custom_max = dp;
	}
}
