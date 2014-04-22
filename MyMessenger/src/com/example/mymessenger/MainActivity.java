package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.services.Sms;
import com.example.mymessenger.services.Vk;
import com.vk.sdk.VKUIHelper;

public class MainActivity extends ActionBarActivity implements OnClickListener {
	final int MENU_CON_MOVE = 101;
	final int MENU_CON_DELETE = 102;
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
        
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);

        
        sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        
        //((Vk) app.getService(MessageService.VK)).authorize(this);

        
        
        
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
    	Log.d("MainActivity", "onPause");
    }
    
    @Override 
	protected void onDestroy() { 
		super.onDestroy();
		Log.d("MainActivity", "onDestroy");
		//VKUIHelper.onDestroy(this); 
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
    	Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
    	return super.onOptionsItemSelected(item);
    }
    
    
    
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	switch (v.getId()) {
        case 10:
        	menu.add(0, MENU_CON_MOVE, 0, "Move");
        	menu.add(0, MENU_CON_DELETE, 0, "Delete");
        	break;
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case MENU_CON_MOVE:    		
    		break;

    	case MENU_CON_DELETE:
    		break;
    	}
    	return super.onContextItemSelected(item);
    }

    
    
    

	@Override
	public void onClick(View view) {
		//Toast.makeText(this, "Нажата кнопка " + ((Button) arg0).getText(), Toast.LENGTH_SHORT).show();
		if( isServicesButton(view.getId()) ){
			MessageService ser = getServiceFromButtonId(view.getId());
			app.active_service = ser.getServiceType();
			removeDialog(view.getId());
			showDialog(view.getId());
		}
		
	}
	
	
	
	
	
	private boolean isServicesButton(int id) {
		if (id >= 10 && id < 20)
			return true;
		else 
			return false;
	}


	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		
		MessageService ser = getServiceFromButtonId(id);
		
		String data[] = ser.getStringsForMainViewMenu();

		adb.setTitle(ser.getServiceName());
		adb.setItems(data, myClickListener);

		return adb.create();
	}
			
	private MessageService getServiceFromButtonId(int id) {
		switch(id){
			case 10+0 :
				return app.getService( MessageService.SMS );
			case 10+1 :
				return app.getService( MessageService.VK );
			default :
				return null;
		}
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
	} 
}
