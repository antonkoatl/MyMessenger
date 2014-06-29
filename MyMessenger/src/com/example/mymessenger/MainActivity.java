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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
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
import com.example.mymessenger.ui.ListViewSimpleFragment;
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
        app.setMainActivity(this);
        
        Intent intent = getIntent(); 
        boolean notification_clicked_msg = intent.getBooleanExtra("notification_clicked_msg", false);
        
        Log.d("MainActivity", "onCreate^" + String.valueOf(savedInstanceState == null));
        
        setContentView(R.layout.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        app = (MyApplication) getApplicationContext();
        
        app.initServices();
        
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        
        if(notification_clicked_msg){
        	mMessage msg = (mMessage) intent.getParcelableExtra("msg");
        	app.setActiveService(msg.msg_service);
        	MessageService ms = app.getActiveService();
        	
        	mDialog dlg = new mDialog();        	
        	dlg.participants.add( msg.respondent );			
        	dlg.snippet = msg.text;
			dlg.snippet_out = msg.getFlag(mMessage.OUT) ? 1 : 0;
			dlg.last_msg_time.set(msg.sendTime);
			dlg.msg_service_type = msg.msg_service;
			
        	ms.setActiveDialog(dlg);
        	List<mDialog> dlgs = new ArrayList<mDialog>();
        	dlgs.add(dlg);
        	app.triggerDlgsUpdaters(dlgs);
        	
        	pagerAdapter.recreateFragment(2);
        	mViewPager.setCurrentItem(2, false);
        }
        
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
	} 

    @Override
    protected void onPause() {
    	super.onPause();
    	Log.d("MainActivity", "onPause");
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d("MainActivity", "onStop");
    }
    
    @Override 
	protected void onDestroy() { 
		super.onDestroy();
		app.setMainActivity(null);
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
    		ListViewSimpleFragment fr = (ListViewSimpleFragment) pagerAdapter.getRegisteredFragment(2);
    		if (fr.emojiPopup.isShowing()) {
    			fr.emojiPopup.hide();
                return;
            }
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
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data); 
		
		if(requestCode == SelectServiceActivity.REQUEST_CODE){
			if(resultCode == SelectServiceActivity.RESULT_ADDED){
				int service_type = data.getIntExtra("service_type", 0);
				app.newService(service_type);
			}
			if(resultCode == SelectServiceActivity.RESULT_NOT_ADDED){
				Toast.makeText(this, "Service not added", Toast.LENGTH_SHORT).show();
			}
		}
		
		if(requestCode == ActivityTwo.REQUEST_CODE){
			if(resultCode == ActivityTwo.RESULT_SELECTED){
				MessageService ms = app.getService( data.getIntExtra("msg_service", 0) );
				mContact cnt = ms.getContact( data.getStringExtra("cnt") );
				mDialog dlg = ms.getDialog(cnt);
				ms.setActiveDialog(dlg);
				mViewPager.setCurrentItem(2);
			}
		}
	}
	


	

	
}
