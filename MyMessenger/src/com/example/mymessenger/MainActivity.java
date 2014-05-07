package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.services.Sms;
import com.example.mymessenger.services.Vk;
import com.example.mymessenger.ui.ServicesMenuFragment;
import com.vk.sdk.VKUIHelper;

public class MainActivity extends ActionBarActivity {
	final int DIALOG_SMS = 1;
	MyApplication app;
	private SharedPreferences sPref;
	private List<Button> buttons;
	public ViewPager mViewPager;
	public MyPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate^" + String.valueOf(savedInstanceState == null));
        
        setContentView(R.layout.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        app = (MyApplication) getApplicationContext();
        
        app.initServices();
        
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        
        
        
        sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        
    }

    @Override
    protected void onStart(){
    	super.onStart();
    	Log.d("MainActivity", "onStart");
    }

    @Override
	protected void onResume() { 
		super.onResume();
		Log.d("MainActivity", "onResume");
		VKUIHelper.onResume(this);
		app.setCurrentActivity(this);
	} 

    @Override
    protected void onPause() {
    	super.onPause();
    	Log.d("MainActivity", "onPause");
    	app.setCurrentActivity(null);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d("MainActivity", "onStop");
    	
    	Editor ed = app.sPref.edit();
    	if(app.getActiveService() != null){
    		ed.putInt("active_service", app.getActiveService().getServiceType());
    		ed.putString("active_dialog", app.getActiveService().getActiveDialog().getParticipantsAddresses());
    	}
    	ed.commit();
    }
    
    @Override 
	protected void onDestroy() { 
		super.onDestroy();
		Log.d("MainActivity", "onDestroy");
		VKUIHelper.onDestroy(this); 
	} 
    
    @Override
    public void onBackPressed() {
    	if(mViewPager.getCurrentItem() == 0){
    		ServicesMenuFragment fr = (ServicesMenuFragment) pagerAdapter.getRegisteredFragment(0);
            if(fr.isForDeleteService()){
            	fr.setForNormal();
            	return;
            }
    	}
    	if(mViewPager.getCurrentItem() == 2){
    		mViewPager.setCurrentItem(1);
    		return;
    	}
    	
        super.onBackPressed();
    }
    
    

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    // обновление меню
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menuitem_addservice:
    		Intent intent = new Intent(this, SelectServiceActivity.class);
    		startActivityForResult(intent, SelectServiceActivity.REQUEST_CODE);
    		break;
    	case R.id.menuitem_removeservice:
    		ServicesMenuFragment fr = (ServicesMenuFragment) pagerAdapter.getRegisteredFragment(0);
    		fr.setForDeleteService();
    		break;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    
    
    
   	
    private MessageService getServiceFromButtonId(int id) {
		switch(id){
			case 1000+0 :
				return app.getService( MessageService.SMS );
			case 1000+1 :
				return app.getService( MessageService.VK );
			default :
				return null;
		}
	}
	
	


	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		
		MessageService ser = getServiceFromButtonId(id);
		
		String data[] = ser.getStringsForMainViewMenu();

		adb.setTitle(ser.getServiceName());
		adb.setItems(data, myClickListener);

		return adb.create();
	}
			
	

	// обработчик нажатия на пункт списка диалога
	android.content.DialogInterface.OnClickListener myClickListener = new android.content.DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			  MessageService ser = app.getActiveService();
			  ser.MainViewMenu_click(which, MainActivity.this);
		  }
	};
	  
    
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		VKUIHelper.onActivityResult(requestCode, resultCode, data); 
		
		if(requestCode == SelectServiceActivity.REQUEST_CODE){
			if(resultCode == SelectServiceActivity.RESULT_ADDED){
				pagerAdapter.notifyDataSetChanged();
				//getSupportFragmentManager().beginTransaction().remove(pagerAdapter.getRegisteredFragment(0)).commit();				
			}
			if(resultCode == SelectServiceActivity.RESULT_NOT_ADDED){
				Toast.makeText(this, "Service not added", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
}
