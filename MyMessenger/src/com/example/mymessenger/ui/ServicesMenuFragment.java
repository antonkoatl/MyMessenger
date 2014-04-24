package com.example.mymessenger.ui;

import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.R.id;
import com.example.mymessenger.R.layout;
import com.example.mymessenger.services.MessageService;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;


public class ServicesMenuFragment extends Fragment {
	static final int MENU_CON_MOVE = 101;
	static final int MENU_CON_DELETE = 102;
	
	Context context;
	
	@Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.activity_main, container, false);
        Bundle args = getArguments();
        //((TextView) rootView.findViewById(android.R.id.text1)).setText(
        //        Integer.toString(args.getInt(ARG_OBJECT)));
        
        MyApplication app = (MyApplication) getActivity().getApplicationContext();
        
        LinearLayout ll_list = (LinearLayout) rootView.findViewById(R.id.linearlay_mainbuttons);

        for(MessageService ser : app.myMsgServices){
        	Button b = new Button(getActivity());
            b.setText(ser.getServiceName());
            b.setId( getButtonIdMainScreen(ser.getServiceType()) ); 
            b.setOnClickListener((OnClickListener)getActivity());
            ll_list.addView(b);
            registerForContextMenu(b);

            Log.d("myLogs", "Service button added: " + ser.getServiceName());
        }
        //((ViewGroup)rootView).addView(row1);
        return rootView;
    }
	
	private int getButtonIdMainScreen(int type) {
    	int res = 10;
		switch(type){
		case MessageService.SMS :
			res += 0;
			break;
		case MessageService.VK :
			res += 1;
			break;
		}
		return res;
	}
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	/*switch (v.getId()) {
        case 10:
        	menu.add(0, MENU_CON_MOVE, 0, "Move");
        	menu.add(0, MENU_CON_DELETE, 0, "Delete");
        	break;
        }*/
		menu.add(0, MENU_CON_MOVE, 0, "Move");
    	menu.add(0, MENU_CON_DELETE, 0, "Delete");
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

}
