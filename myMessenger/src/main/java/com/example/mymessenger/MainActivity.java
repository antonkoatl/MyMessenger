package com.example.mymessenger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.msInterfaceMS;
import com.example.mymessenger.services.MessageService.msInterfaceUI;
import com.example.mymessenger.services.Twitter.mTwitter;
import com.example.mymessenger.ui.ListViewSimpleFragment;
import com.example.mymessenger.ui.ServicesMenuFragment;
import com.example.mymessenger.ui.ViewPagerAdvanced;
import com.vk.sdk.VKUIHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {
	final int DIALOG_SMS = 1;
	MyApplication app;
	private SharedPreferences sPref;
	private List<Button> buttons;
	public ViewPagerAdvanced mViewPager;
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
        

        
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPagerAdvanced) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPagerAdvanced.OnPageChangeListener() {
            boolean position_changed = false;
            boolean scroll_finished = false;

            @Override
            public void onPageSelected(int position) {
                pagerAdapter.setCurrentPosition(position);
                position_changed = true;
                if(scroll_finished)change_width();

                //pagerAdapter.notifyDataSetChanged();

                //pagerAdapter.notifyDataSetChanged();

                /*if(position == 0) {
                    mViewPager.infoForPosition(0).widthFactor = 0.3f;
                    mViewPager.infoForPosition(1).widthFactor = 0.7f;
                } else {
                    mViewPager.infoForPosition(1).widthFactor = 0.3f;
                    mViewPager.infoForPosition(2).widthFactor = 0.7f;
                }*/
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //pagerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //pagerAdapter.notifyDataSetChanged();
                if(state == ViewPagerAdvanced.SCROLL_STATE_IDLE) {
                    scroll_finished = true;
                    if(position_changed)change_width();
                }
            }


            private void change_width(){
                pagerAdapter.notifyDataSetChanged();
                scroll_finished = false;
                position_changed = false;
                return;
                /*
                if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
                    return;
                }

                if (pagerAdapter.current_position == 0) {
                    ViewPagerAdvanced.ItemInfo ii = mViewPager.infoForPosition(0);
                    ii.widthFactor = 0.3f;
                    ii = mViewPager.infoForPosition(1);
                    ii.widthFactor = 0.7f;
                    ii = mViewPager.infoForPosition(2);
                    ii.widthFactor = 0.3f;
                    View child = mViewPager.getChildAt(0);
                    ViewPagerAdvanced.LayoutParams lp = (ViewPagerAdvanced.LayoutParams) child.getLayoutParams();
                    lp.widthFactor = 0.3f;
                    child = mViewPager.getChildAt(1);
                    lp = (ViewPagerAdvanced.LayoutParams) child.getLayoutParams();
                    lp.widthFactor = 0.7f;
                    child = mViewPager.getChildAt(2);
                    lp = (ViewPagerAdvanced.LayoutParams) child.getLayoutParams();
                    lp.widthFactor = 0.3f;
                } else {
                    ViewPagerAdvanced.ItemInfo ii = mViewPager.infoForPosition(0);
                    ii.widthFactor = 0.3f;
                    ii = mViewPager.infoForPosition(1);
                    ii.widthFactor = 0.3f;
                    ii = mViewPager.infoForPosition(2);
                    ii.widthFactor = 0.7f;
                    View child = mViewPager.getChildAt(0);
                    ViewPagerAdvanced.LayoutParams lp = (ViewPagerAdvanced.LayoutParams) child.getLayoutParams();
                    lp.widthFactor = 0.3f;
                    child = mViewPager.getChildAt(1);
                    lp = (ViewPagerAdvanced.LayoutParams) child.getLayoutParams();
                    lp.widthFactor = 0.3f;
                    child = mViewPager.getChildAt(2);
                    lp = (ViewPagerAdvanced.LayoutParams) child.getLayoutParams();
                    lp.widthFactor = 0.7f;

                }
                //mViewPager.populate();
                mViewPager.requestLayout();
                mViewPager.invalidate();

                scroll_finished = false;
                position_changed = false;*/
            }
        });

        if(notification_clicked_msg){
        	mMessage msg = (mMessage) intent.getParcelableExtra("msg");
        	app.msManager.setActiveService(msg.msg_service);
            msInterfaceMS ms = app.msManager.getActiveService();

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
		//app.setMainActivity(null);
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
        if(mViewPager.getCurrentItem() == 1){
            mViewPager.setCurrentItem(0);
            return;
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
        // TODO: Создаётся два раза подряд при перелистывании фрагметов (?)
        Log.d("MainActiviy", "onCreateOptionsMenu");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.item_selection1);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(searchItem);

        List<String> choices = new ArrayList<String>();

        int selected_service_for_dialogs = app.sPref.getInt("selected_service_for_dialogs", 0);
        String saved = null;
        if(selected_service_for_dialogs == 0){
            saved = getString(R.string.service_name_all);
        } else if(app.msManager.getService(selected_service_for_dialogs) != null){
            saved = app.msManager.getService(selected_service_for_dialogs).getServiceName();
        }

        choices.add(getString(R.string.service_name_all));
        for(msInterfaceMS ms : app.msManager.myMsgServices){
            choices.add(ms.getServiceName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, choices);
// Specify the layout to use when the list of choices appears
        if(saved != null){
            spinner.setSelection( adapter.getPosition(saved) );
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                String ser_name = (String) adapterView.getItemAtPosition(pos);
                int ser_type = 0;
                if(ser_name.equals(getString(R.string.service_name_all))) ser_type = 0;
                if(ser_name.equals(getString(R.string.service_name_sms))) ser_type = MessageService.SMS;
                if(ser_name.equals(getString(R.string.service_name_vk))) ser_type = MessageService.VK;
                if(ser_name.equals(getString(R.string.service_name_tw))) ser_type = MessageService.TW;
                ListViewSimpleFragment fr = (ListViewSimpleFragment) pagerAdapter.getRegisteredFragment(1);
                fr.setSelectedService(ser_type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return true;
    }
    
    // ���������� ����
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("MainActivity", "onPrepareOptionsMenu");
        if(mViewPager.getCurrentItem() == 0){
            menu.findItem(R.id.item_selection1).setVisible(false);
            //menu.getItem(0).setVisible(true);
        }
        if(mViewPager.getCurrentItem() == 1) {
            menu.findItem(R.id.item_selection1).setVisible(true);

            MenuItem searchItem = menu.findItem(R.id.item_selection1);
            Spinner spinner = (Spinner) MenuItemCompat.getActionView(searchItem);

            // TODO: не пересоздавать каждый раз, обновлять по мере надобности
            List<String> choices = new ArrayList<String>();
            choices.add(getString(R.string.service_name_all));
            for(msInterfaceMS ms : app.msManager.myMsgServices){
                choices.add(ms.getServiceName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item, choices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            int selected_service_for_dialogs = app.sPref.getInt("selected_service_for_dialogs", 0);
            String saved = null;
            if(selected_service_for_dialogs == 0){
                saved = getString(R.string.service_name_all);
            } else if(app.msManager.getService(selected_service_for_dialogs) != null){
                saved = app.msManager.getService(selected_service_for_dialogs).getServiceName();
            }

            if(saved != null){
                spinner.setSelection( adapter.getPosition(saved) );
            }

            //menu.getItem(0).setVisible(false);
        }
        if(mViewPager.getCurrentItem() == 2) {
            menu.findItem(R.id.item_selection1).setVisible(false);
            //menu.getItem(0).setVisible(false);
        }

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
            mViewPager.setCurrentItem(0);
    		ServicesMenuFragment fr = (ServicesMenuFragment) pagerAdapter.getRegisteredFragment(0);
    		fr.setForDeleteService();
    		break;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    
    


    private MessageService getServiceFromButtonId(int id) {
		switch(id){
			case 1000+0 :
				return app.msManager.getService( MessageService.SMS );
			case 1000+1 :
				return app.msManager.getService( MessageService.VK );
            case 1000+2 :
                return app.msManager.getService( MessageService.TW );
			default :
				return null;
		}
	}




	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		
		msInterfaceUI ser = getServiceFromButtonId(id);
		
		String data[] = ser.getStringsForMainViewMenu();

		adb.setTitle(ser.getServiceName());
		adb.setItems(data, myClickListener);

		return adb.create();
	}
			

	android.content.DialogInterface.OnClickListener myClickListener = new android.content.DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
            msInterfaceUI ser = app.msManager.getActiveService();
			  ser.MainViewMenu_click(which, MainActivity.this);
		  }
	};
	  
    
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
        if(app.msManager.getService(MessageService.TW) != null)
            ((mTwitter) app.msManager.getService(MessageService.TW)).authOnActivityResult(this, requestCode, resultCode, data);
		
		if(requestCode == SelectServiceActivity.REQUEST_CODE){
			if(resultCode == SelectServiceActivity.RESULT_ADDED){
				int service_type = data.getIntExtra("service_type", 0);
				app.msManager.newService(service_type);
			}
			if(resultCode == SelectServiceActivity.RESULT_NOT_ADDED){
				Toast.makeText(this, "Service not added", Toast.LENGTH_SHORT).show();
			}
		}
		
		if(requestCode == ActivityTwo.REQUEST_CODE){
			if(resultCode == ActivityTwo.RESULT_SELECTED){
				msInterfaceMS ms = app.msManager.getService( data.getIntExtra("msg_service", 0) );
				mContact cnt = ms.getContact( data.getStringExtra("cnt") );
				mDialog dlg = ms.getDialog(cnt);
				ms.setActiveDialog(dlg);
				mViewPager.setCurrentItem(2);
			}
		}
	}
	


	

	
}
