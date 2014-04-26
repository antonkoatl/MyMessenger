package com.example.mymessenger;

import com.example.mymessenger.services.MessageService;
import com.vk.sdk.VKUIHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

public class SelectServiceActivity extends Activity {
	public static final int REQUEST_CODE = 100;
	public static final int RESULT_ADDED = 100;
	public static final int RESULT_NOT_ADDED = 101;
	
	private MyApplication app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = (MyApplication) getApplicationContext();
		
		setContentView(R.layout.select_service);

		GridView gridview = (GridView) findViewById(R.id.selectservice_grid);
	    gridview.setAdapter(new SelectServiceAdapter(this));
	    gridview.setNumColumns(GridView.AUTO_FIT);
	    gridview.setColumnWidth(128);
	    //gridview.setVerticalSpacing(8);
	    //gridview.setHorizontalSpacing(8);
	    gridview.setStretchMode(GridView.NO_STRETCH);
	    
	    gridview.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	        	Log.d("SelectServiceActivity::OnItemClickListener", "onItemClick");
	        	finish();
	            boolean added = false; 
	            switch(position){
	            case 0:	added = app.newService(MessageService.SMS);
	            case 1:	added = app.newService(MessageService.VK);
	            }
	            if(added)setResult(RESULT_ADDED, new Intent());
	            else setResult(RESULT_NOT_ADDED, new Intent());
	            
	        }
	    });
	}
	
	@Override
	protected void onResume() { 
		super.onResume();
		app.setCurrentActivity(this);
		//VKUIHelper.onResume(this);
	}
	
	@Override
    protected void onPause() {
    	super.onPause();
    	app.setCurrentActivity(null);
    }

	@Override 
	protected void onDestroy() { 
		super.onDestroy();
		//VKUIHelper.onDestroy(this); 
	}
	
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//VKUIHelper.onActivityResult(requestCode, resultCode, data); 
	} 
}
