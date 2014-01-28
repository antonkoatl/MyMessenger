package com.example.mymessenger;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.example.mymessenger.services.SmsService;

public class MainActivity extends Activity implements OnClickListener {
	final int MENU_CON_MOVE = 101;
	final int MENU_CON_DELETE = 102;
	final int DIALOG_SMS = 1;
	MyApplication app;
	private SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (MyApplication) getApplicationContext();
        
        sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        String using_services[] = sPref.getString("usingservices", "sms").split(";");
        
        for(String i : using_services){        	
        	if(i.equals("sms"))
        		app.addService(new SmsService(this.getApplicationContext()));
        }
        
        LinearLayout ll_list = (LinearLayout) findViewById(R.id.linearlay_mainbuttons);
        
        for(MessageService ser : app.myServices){
        	Button b = new Button(this);
            b.setText(ser.getName());
            b.setId( getButtonIdMainScreen(ser.getType()) ); 
            b.setOnClickListener(this);
            ll_list.addView(b);
            registerForContextMenu(b);
            
            Log.d("myLogs", "Service added: " + ser.getName());
        }
    }


    private int getButtonIdMainScreen(int type) {
    	int res = 10;
		switch(type){
		case MessageService.SMS :
			res += 0;
			break;
		}
		return res;
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
		if( isServicesButton(view.getId()) )
			showDialog(view.getId());
		
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
		app.active_service = ser.getType();
		
		String data[] = ser.getStringsForMainViewMenu();

		adb.setTitle(ser.getName());
		adb.setItems(data, myClickListener);

		return adb.create();
	}
	
	private MessageService getServiceFromButtonId(int id) {
		switch(id){
			case 10+0 :
				return app.getService( MessageService.SMS );
			default :
				return null;
		}
	}

	// обработчик нажатия на пункт списка диалога
	android.content.DialogInterface.OnClickListener myClickListener = new android.content.DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int which) {
			  MessageService ser = app.getService( app.active_service );
			  ser.MainViewMenu_click(which, MainActivity.this);			  
		  }
	  };
    
}
