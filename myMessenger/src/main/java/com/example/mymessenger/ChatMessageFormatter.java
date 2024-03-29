package com.example.mymessenger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;

import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.msInterfaceEM;

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

public class ChatMessageFormatter {
	private static final Factory spannableFactory = Spannable.Factory.getInstance();

	private static final Map<Pattern, Long> emoticons = new HashMap<Pattern, Long>();


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
	
	public static void addPattern(long resource, String smile){
        emoticons.put(Pattern.compile(Pattern.quote(smile)), resource);
	}
		
	public static boolean addSmiles(Context context, Spannable spannable, int service_type, int line_height) throws MalformedURLException, IOException {
	    boolean hasChanges = false;
	    for (Entry<Pattern, Long> entry : emoticons.entrySet()) {
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
	                //String t = getCachedFile(entry.getValue(), context);
	                
	                AsyncTaskCompleteListener<Drawable> cb = new AsyncTaskCompleteListener<Drawable>(){
                		Spannable spannable;
                		int matcher_start, matcher_end;

						@Override
						public void onTaskComplete(Drawable result) {
							if(result == null)return;
							spannable.setSpan(new ImageSpan(result),
		        	                //spannable.setSpan(new ImageSpan(context, R.drawable.smile),
									matcher_start, matcher_end,
		        	                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						
						public AsyncTaskCompleteListener<Drawable> setParams(Spannable spannable, int matcher_start, int matcher_end){
							this.spannable = spannable;
							this.matcher_start = matcher_start;
							this.matcher_end = matcher_end;
							return this;
						}
                		
                	}.setParams(spannable, matcher.start(), matcher.end());

                    msInterfaceEM ms = ((MyApplication) context.getApplicationContext()).msManager.getService(service_type);
                    if(ms != null) {
                        String eurl = ms.getEmojiUrl(entry.getValue());
                        if(eurl != null)getDownload(eurl, cb, line_height, ms);
                    }
	                
	                
	                
	                /*
	                BitmapFactory.Options options = new BitmapFactory.Options();
	                options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
	                options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
	                options.inScaled = true;
	                Bitmap b = BitmapFactory.decodeFile( t, options );
	                Drawable d = new BitmapDrawable(context.getResources(), b);	                
	                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());*/
	                
	                
	            }
	        }
	    }
	    return hasChanges;
	}

	public static Spannable getSmiledText(Context context, CharSequence text, int service_type, int line_height) {
	    Spannable spannable = spannableFactory.newSpannable(text);
	    try {
			addSmiles(context, spannable, service_type, line_height);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return spannable;
	}
	
	public static void getDownload(String url_path, final AsyncTaskCompleteListener<Drawable> cb, int size, msInterfaceEM ms){
		String output = isFileDownloaded(url_path, ms);
		if(output == null){
			new DownloadFilesTask(cb, size, ms).execute(url_path, MyApplication.context.getCacheDir().toString());
		} else {
	    	final Drawable d = getDrawableFromFile(output, size);
	    	if(MyApplication.getMainActivity() != null)MyApplication.getMainActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					cb.onTaskComplete( d ); //On Ui Thread
				}	    		
	    	});
	    	
		}		
	}


	
	public static void getDownload(String url_path, final AsyncTaskCompleteListener<Drawable> cb, MessageService ms){
		getDownload(url_path, cb, 0, ms);
	}

    public static void getDownloadB(String url_path, final AsyncTaskCompleteListener<Bitmap> cb, int size, MessageService ms){
        String output = isFileDownloaded(url_path, ms);
        if(output == null){
            new DownloadFilesTaskB(cb, size, ms).execute(url_path, MyApplication.context.getCacheDir().toString());
        } else {
            final Bitmap b = getBitmapFromFile(output, size);
            if(MyApplication.getMainActivity() != null)MyApplication.getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cb.onTaskComplete( b ); //On Ui Thread
                }
            });

        }
    }
	

	
	public static String isFileDownloaded(String url_path, msInterfaceEM ms){
		String fileName = url_path.substring( url_path.lastIndexOf('/')+1, url_path.length() );
		File output = new File(MyApplication.context.getCacheDir() + File.separator + MessageService.getCacheFolder(ms.getServiceType()), fileName);
		File output_dir = new File(MyApplication.context.getCacheDir() + File.separator + MessageService.getCacheFolder(ms.getServiceType()));
	    if(!output_dir.exists()) output_dir.mkdirs();
	    
	    if (output.exists()) {
	    	return output.getPath();
	    }    

	    return null;
	}
	
	public static Drawable getDrawableFromFile(String file_path){
		return getDrawableFromFile(file_path, 0);
	}

    public static Bitmap getBitmapFromFile(String file_path, int size){
        long tstart = SystemClock.currentThreadTimeMillis();
        long ttemp = SystemClock.currentThreadTimeMillis();
        long t1 = 0;
        long t2 = 0;
        long t3 = 0;
        long t4 = 0;
        long t5 = 0;

        t1 = SystemClock.currentThreadTimeMillis() - ttemp;
        ttemp = SystemClock.currentThreadTimeMillis();

        if(file_path == null)return null;
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
        options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
        options.inScaled = true;

        t2 = SystemClock.currentThreadTimeMillis() - ttemp;
        ttemp = SystemClock.currentThreadTimeMillis();

        Bitmap b = BitmapFactory.decodeFile( file_path, options );
        t3 = SystemClock.currentThreadTimeMillis() - ttemp;
        ttemp = SystemClock.currentThreadTimeMillis();
        if(b == null){
            return null;
        }

        if(size > 0){
            b = Bitmap.createScaledBitmap(b, size, size, true);
        }
        t4 = SystemClock.currentThreadTimeMillis() - ttemp;
        ttemp = SystemClock.currentThreadTimeMillis();

        return b;
    }
	
	public static Drawable getDrawableFromFile(String file_path, int size){
		long tstart = SystemClock.currentThreadTimeMillis();
		long ttemp = SystemClock.currentThreadTimeMillis();
		long t1 = 0;
		long t2 = 0;
		long t3 = 0;
		long t4 = 0;
		long t5 = 0;
		
		t1 = SystemClock.currentThreadTimeMillis() - ttemp;
	    ttemp = SystemClock.currentThreadTimeMillis();
	      
		if(file_path == null)return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        
        options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
        options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
        options.inScaled = true;
        
        t2 = SystemClock.currentThreadTimeMillis() - ttemp;
	    ttemp = SystemClock.currentThreadTimeMillis();
        
        Bitmap b = BitmapFactory.decodeFile( file_path, options );
        t3 = SystemClock.currentThreadTimeMillis() - ttemp;
	    ttemp = SystemClock.currentThreadTimeMillis();
	    if(b == null){
	    	return null;
	    }
	    
        if(size > 0){        	
        	b = Bitmap.createScaledBitmap(b, size, size, true);
        }
        t4 = SystemClock.currentThreadTimeMillis() - ttemp;
	    ttemp = SystemClock.currentThreadTimeMillis();
        
        Drawable d = new BitmapDrawable(MyApplication.context.getResources(), b);
        
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        t5 = SystemClock.currentThreadTimeMillis() - ttemp;
	    ttemp = SystemClock.currentThreadTimeMillis();

	    //Log.d("TIMING", "T1: " + String.valueOf(t1) + ", T2: " + String.valueOf(t2) + ", T3: " + String.valueOf(t3) + ", T4: " + String.valueOf(t4) + ", T5: " + String.valueOf(t5) + ", Total: " + String.valueOf(SystemClock.currentThreadTimeMillis() - tstart));
        return d;
	}
	
	
	public static class DownloadFilesTask extends AsyncTask<String, Void, Drawable>{
		AsyncTaskCompleteListener<Drawable> cb;
		int size;
        msInterfaceEM ms;
		
		DownloadFilesTask(AsyncTaskCompleteListener<Drawable> cb, MessageService ms){
			this.cb = cb;
			this.size = 0;
            this.ms = ms;
		}
		
		DownloadFilesTask(AsyncTaskCompleteListener<Drawable> cb, int size, msInterfaceEM ms){
			this.cb = cb;
			this.size = size;
            this.ms = ms;
		}
		
		@Override
		protected Drawable doInBackground(String... arg0) {
			String url_path = arg0[0];
			String cache_dir = arg0[1];

			String ss = getCachedFile(url_path, cache_dir);
			return getDrawableFromFile(ss, size);
			
			
		}
		
		protected void onPostExecute(Drawable result) {
			cb.onTaskComplete(result);
	    }
		
		public String getCachedFile(String url_path, String cache_dir){
			String fileName = url_path.substring( url_path.lastIndexOf('/')+1, url_path.length() );
			File output = new File(cache_dir + File.separator + MessageService.getCacheFolder(ms.getServiceType()), fileName);
			
			boolean failed = false;
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
		    	failed = true;
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
			if(failed)return null;
			else return output.getPath();
		}
	};

    public static class DownloadFilesTaskB extends AsyncTask<String, Void, Bitmap>{
        AsyncTaskCompleteListener<Bitmap> cb;
        int size;
        MessageService ms;

        DownloadFilesTaskB(AsyncTaskCompleteListener<Bitmap> cb){
            this.cb = cb;
            this.size = 0;
            this.ms = ms;
        }

        DownloadFilesTaskB(AsyncTaskCompleteListener<Bitmap> cb, int size, MessageService ms){
            this.cb = cb;
            this.size = size;
            this.ms = ms;
        }

        @Override
        protected Bitmap doInBackground(String... arg0) {
            String url_path = arg0[0];
            String cache_dir = arg0[1];

            String ss = getCachedFile(url_path, cache_dir, ms);
            return getBitmapFromFile(ss, size);


        }

        protected void onPostExecute(Bitmap result) {
            cb.onTaskComplete(result);
        }

        public String getCachedFile(String url_path, String cache_dir, MessageService ms){
            String fileName = url_path.substring( url_path.lastIndexOf('/')+1, url_path.length() );
            File output = new File(cache_dir + File.separator + MessageService.getCacheFolder(ms.getServiceType()), fileName);

            boolean failed = false;
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
                failed = true;
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
            if(failed)return null;
            else return output.getPath();
        }
    };

    public static String charsFromLong(long code){
        String ccode;
        try {
            ccode = String.valueOf(Character.toChars((int) code));
        } catch (IllegalArgumentException e) { // For 0..9
            ccode = String.valueOf(Character.toChars((int) (code / 0x10000)));
            ccode += String.valueOf(Character.toChars((int) (code % 0x10000)));
        }
        return ccode;
    }
}
