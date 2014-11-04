package com.example.mymessenger;

import android.app.IntentService;
import android.content.Intent;

import com.example.mymessenger.services.MessageService.MessageService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DownloadService extends IntentService {
	public static final String NOTIFICATION_FINISHED = "com.mymessenger.downloader.finished";

	public DownloadService() {
		super("Downloader");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String url_path = intent.getStringExtra("url");
        int ser_type = intent.getIntExtra("ser_type", 0);

		String fileName = url_path.substring( url_path.lastIndexOf('/')+1, url_path.length() );
		
		
		
		File output = new File(getCacheDir() + File.separator + MessageService.getCacheFolder(ser_type), fileName);

        // проверяем доступность SD
	    /*if (!Environment.getExternalStorageState().equals(
	        Environment.MEDIA_MOUNTED)) {
	      //Log.d("DownloadService", "SD-карта не доступна: " + Environment.getExternalStorageState());
	    	output = new File(Environment.getDownloadCacheDirectory().getExternalStorageDirectory() + "/" + DIR_SD, fileName);
	    } else {
	    	output = new File(Environment.getExternalStorageDirectory() + "/" + DIR_SD, fileName);
	    } */
		
	    File output_dir = new File(getCacheDir() + File.separator + MessageService.getCacheFolder(ser_type));
	    if(!output_dir.exists()) output_dir.mkdirs();
	    
	    if (output.exists()) {
	    	//output.delete();
	    	//Log.d("DownloadService", "File in cache: " + url_path);
	    	sendResult(url_path, output.getPath());
	    	return;	    	
	    }

	    //Log.d("DownloadService", "Starting file download: " + url_path);
	    
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
	    	
	    	sendResult(url_path, output.getPath());
	    }
	    
	    
	}

	private void sendResult(String url_path, String file_path) {
		Intent intent = new Intent(NOTIFICATION_FINISHED);
	    intent.putExtra("url", url_path);
	    intent.putExtra("file_path", file_path);
	    sendBroadcast(intent);
	}

    /*
	public String getCachedFile(String url_path){
		String fileName = url_path.substring( url_path.lastIndexOf('/')+1, url_path.length() );
		File output = new File(getCacheDir() + File.separator + DIR_SD, fileName);
		File output_dir = new File(getCacheDir() + File.separator + DIR_SD);
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
	}*/
}
