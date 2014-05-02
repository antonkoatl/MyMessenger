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

	public static void addPatternVk(String code){
		if(code.length() == 4)addPattern(emoticons, String.valueOf( Character.toChars( Integer.parseInt(code, 16) ) ), "http://vk.com/images/emoji/" + code + ".png");
		if(code.length() == 8)addPattern(emoticons, String.valueOf( Character.toChars( Integer.parseInt(code.substring(0, 4), 16) ) ) + String.valueOf( Character.toChars( Integer.parseInt(code.substring(4, 8), 16) ) ), "http://vk.com/images/emoji/" + code + ".png");
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
