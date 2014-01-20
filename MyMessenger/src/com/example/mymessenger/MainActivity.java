package com.example.mymessenger;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        app = (MyApplication) getApplicationContext();
        app.addService(new SmsService(this.getApplicationContext()));
        
        LinearLayout ll_list = (LinearLayout) findViewById(R.id.linearlay_mainbuttons);
        
        for(MessageService ser : app.myServices){
        	Button b = new Button(this);
            b.setText(ser.getName());
            // TODO dyn id
            b.setId( 10 ); 
            b.setOnClickListener(this);
            ll_list.addView(b);
            registerForContextMenu(b);
            
            Log.d("myLogs", "Service added: " + ser.getName());
        }
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
	public void onClick(View arg0) {
		//Toast.makeText(this, "Нажата кнопка " + ((Button) arg0).getText(), Toast.LENGTH_SHORT).show();
		switch (arg0.getId()) {
		case 10:
			showDialog(DIALOG_SMS);
			break;
		}
		
	}
	
	
	
	
	
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_SMS:
			List<mDialog> t = app.getService( MessageService.SMS ).getDialogs(0, 1);
			app.getService( MessageService.SMS ).setActiveDialog(t.get(0));
			
			String data[] = {t.get(0).getParticipantsNames(), "New message", "All messages"};
			
			adb.setTitle(app.getService( MessageService.SMS ).getName());
			adb.setItems(data, myClickListener);
			break;
		}
		return adb.create();
	}
	
	// обработчик нажатия на пункт списка диалога
	android.content.DialogInterface.OnClickListener myClickListener = new android.content.DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int which) {
			  
			  switch(which) {
			  case 0:
				  app.active_service = MessageService.SMS;
				  Intent intent = new Intent(MainActivity.this, ActivityTwo.class);
				  startActivity(intent);
				  break;
			  }
			  
		  }
	  };
    
}
