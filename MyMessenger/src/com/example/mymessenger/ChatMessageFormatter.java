package com.example.mymessenger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.mymessenger.services.Vk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;

public class ChatMessageFormatter {
	private static final Factory spannableFactory = Spannable.Factory
	        .getInstance();

	private static final Map<Pattern, String> emoticons = new HashMap<Pattern, String>();

	public static void addPatternVk(long code){
		String scode = long_to_hex_string(code);
		addPattern(emoticons, string_from_hex_string(scode), Vk.getEmojiUrl(scode));
	}
	
	public int substring_hex_to_int(String code, int start, int end){
		return Integer.parseInt(code.substring(start, end), 16);
	}
	
	public static String long_to_hex_string(long code) {
        int i = 0; 
        while (i < 4) {
            int cc = (int) ((code >> ((i) * 16)) & 65535);
            if (cc == 0) {
                break;
            }
            i++;
        }

        if(i == 1)return String.format("%04X", code);
        if(i == 2)return String.format("%08X", code);
        //if(i == 3)return String.format("%12X", code);
        if(i == 4)return String.format("%16X", code);
        return "";
    }
	
	public static String string_from_hex_string(String val){		
		if(val.length() == 4)return String.valueOf( Character.toChars( Integer.parseInt(val, 16) ) );
		if(val.length() == 8)return String.valueOf( Character.toChars( Integer.parseInt(val.substring(0, 4), 16) ) ) + String.valueOf( Character.toChars( Integer.parseInt(val.substring(4, 8), 16) ) );
		if(val.length() == 16)return String.valueOf( Character.toChars( Integer.parseInt(val.substring(0, 4), 16) ) ) 
										+ String.valueOf( Character.toChars( Integer.parseInt(val.substring(4, 8), 16) ) )
										+ String.valueOf( Character.toChars( Integer.parseInt(val.substring(8, 12), 16) ) )
										+ String.valueOf( Character.toChars( Integer.parseInt(val.substring(12, 16), 16) ) );
		return "";
	}
	
	private static void addPattern(Map<Pattern, String> map, String smile,
	        String resource) {
	    map.put(Pattern.compile(Pattern.quote(smile)), resource);
	}
	
	public static boolean addSmiles(Context context, Spannable spannable, int line_height) throws MalformedURLException, IOException {
	    boolean hasChanges = false;
	    for (Entry<Pattern, String> entry : emoticons.entrySet()) {
	        Matcher matcher = entry.getKey().matcher(spannable);
	        while (matcher.find()) {
	            boolean set = true;
	            for (ImageSpan span : spannable.getSpans(matcher.start(), matcher.end(), ImageSpan.class))
	                if (spannable.getSpanStart(span) >= matcher.start() && spannable.getSpanEnd(span) <= matcher.end())
	                    spannable.removeSpan(span);
	                else {
	                    set = false;
	                    break;
	                }
	            if (set) {
	                hasChanges = true;
	                //spannable.setSpan(new CustomImageSpan(context, R.drawable.smile, entry.getValue()),
	                String t = getCachedFile(entry.getValue(), context);
	                BitmapFactory.Options options = new BitmapFactory.Options();
	                options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
	                options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
	                options.inScaled = true;
	                Bitmap b = BitmapFactory.decodeFile( t, options );
	                Drawable d = new BitmapDrawable(context.getResources(), b);
	                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
	                spannable.setSpan(new ImageSpan(d),
	                //spannable.setSpan(new ImageSpan(context, R.drawable.smile),
	                        matcher.start(), matcher.end(),
	                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            }
	        }
	    }
	    return hasChanges;
	}

	public static Spannable getSmiledText(Context context, CharSequence text, int line_height) {
	    Spannable spannable = spannableFactory.newSpannable(text);
	    try {
			addSmiles(context, spannable, line_height);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return spannable;
	}
	
	public static Drawable getEmojiDrawableVk(String code, int size){
		String t = getCachedFile(Vk.getEmojiUrl(code), MyApplication.context);
        BitmapFactory.Options options = new BitmapFactory.Options();
        
        options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
        options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
        options.inScaled = true;
        
        Bitmap b = BitmapFactory.decodeFile( t, options );
        if(size > 0){        	
        	b = Bitmap.createScaledBitmap(b, size, size, true);
        }
        
        Drawable d = new BitmapDrawable(MyApplication.context.getResources(), b);
        
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        
        if(size > 0){        	
        	b = Bitmap.createScaledBitmap(b, size, size, true);
        }
        
        return d;
	}
	
	public static String getCachedFile(String url_path, Context context){
		String fileName = url_path.substring( url_path.lastIndexOf('/')+1, url_path.length() );
		File output = new File(context.getCacheDir() + File.separator + DownloadService.DIR_SD, fileName);
		File output_dir = new File(context.getCacheDir() + File.separator + DownloadService.DIR_SD);
	    if(!output_dir.exists()) output_dir.mkdirs();
	    
	    if (output.exists()) {
	    	return output.getPath();	    	
	    }
	    
	    InputStream stream = null;
	    FileOutputStream fos = null;
	    try {
	    	URL url = new URL(url_path);
	    	stream = url.openConnection().getInputStream();
	    	//InputStreamReader reader = new InputStreamReader(stream);
	    	fos = new FileOutputStream(output.getPath());
	    	final byte[] buffer = new byte[1024];
	    	int next = -1;
	    	while ((next = stream.read(buffer)) != -1) {
	    		fos.write(buffer, 0, next);
	    	}
	    	// successfully finished
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
	    	if (stream != null) {
	    		try {
	    			stream.close();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	if (fos != null) {
	    		try {
	    			fos.close();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
		}
		    
		return output.getPath();
	}
}
