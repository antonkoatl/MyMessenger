package com.example.mymessenger.ui;

import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.MyMsgAdapter;
import com.example.mymessenger.R;
import com.example.mymessenger.R.id;
import com.example.mymessenger.R.layout;
import com.example.mymessenger.services.MessageService;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
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
import android.widget.RelativeLayout;
import android.widget.TextView;


public class ServicesMenuFragment extends Fragment implements OnClickListener {
	static final int MENU_CON_MOVE = 101;
	static final int MENU_CON_DELETE = 102;
	
	boolean isForDelete = false;
	
	@Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
		isForDelete = false;
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
        	View button_view = inflater.inflate(R.layout.main_servicerow, container, false);
        	
        	Button service_button = (Button) button_view.findViewById(R.id.service_button);
        	service_button.setText(ser.getServiceName());
        	service_button.setId( getButtonIdMainScreen(ser.getServiceType()) );
        	service_button.setOnClickListener(this);
        	registerForContextMenu(service_button);
        	
        	TextView service_delete = (TextView) button_view.findViewById(R.id.service_delete);
        	service_delete.setVisibility(View.INVISIBLE);
        	service_delete.setId( getTextviewDelIdMainScreen(ser.getServiceType()) );
        	
        	/*
        	
        	RelativeLayout relativeLayout = new RelativeLayout(getActivity());
        	
        	// Defining the RelativeLayout layout parameters.
            // In this case I want to fill its parent
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT);
            
            relativeLayout.setLayoutParams(rlp);
            
        	Button b = new Button(getActivity());
            b.setText(ser.getServiceName());
            b.setId( getButtonIdMainScreen(ser.getServiceType()) );
            
            RelativeLayout.LayoutParams lpb = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            b.setLayoutParams(lpb);
            
            b.setOnClickListener(this);
            //ll_list.addView(b);
            registerForContextMenu(b);
            
            relativeLayout.addView(b);
            
            TextView tv = new TextView(getActivity());
            tv.setText("Delete");
            
         // Defining the layout parameters of the TextView
            RelativeLayout.LayoutParams lpt = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            lpt.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lpt.addRule(RelativeLayout.ALIGN_BASELINE, b.getId());
            lpt.setMargins(0, 0, (int) convertDpToPixel(8), 0);
            
            
         // Setting the parameters on the TextView
            tv.setLayoutParams(lpt);
            tv.setVisibility(View.INVISIBLE);
            tv.setId( getTextviewDelIdMainScreen(ser.getServiceType()) );
            
            relativeLayout.addView(tv);
            
            ll_list.addView(relativeLayout);
            */
        	ll_list.addView(button_view);
            Log.d("myLogs", "Service button added: " + ser.getServiceName());
        }
        //((ViewGroup)rootView).addView(row1);
        return rootView;
    }
	
	private int getButtonIdMainScreen(int type) {
    	int res = 1000;
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
	
	private int getTextviewDelIdMainScreen(int type) {
    	int res = 2000;
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
	
	private MessageService getServiceFromButtonId(int id) {
		switch(id){
			case 1000+0 :
				return ((MyApplication) getActivity().getApplication()).getService( MessageService.SMS );
			case 1000+1 :
				return ((MyApplication) getActivity().getApplication()).getService( MessageService.VK );
			default :
				return null;
		}
	}
	

	
	private boolean isServicesButton(int id) {
		if (id >= 1000 && id < 1020)
			return true;
		else 
			return false;
	}
	
	public float convertPixelsToDp(float px){
	    Resources resources = getActivity().getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	}
	
	public float convertDpToPixel(float dp){
	    Resources resources = getActivity().getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float px = dp * (metrics.densityDpi / 160f);
	    return px;
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

	public void setForDeleteService() {
		isForDelete = true;
		View view = getView();
		for(MessageService ms : ((MyApplication) getActivity().getApplication()).myMsgServices ){
			TextView tv = (TextView) view.findViewById( getTextviewDelIdMainScreen(ms.getServiceType()) );
			tv.setVisibility(View.VISIBLE);
		}
	}
	
	public boolean isForDeleteService(){
		return isForDelete;
	}
	
	public void setForNormal(){
		isForDelete = false;
		View view = getView();
		for(MessageService ms : ((MyApplication) getActivity().getApplication()).myMsgServices ){
			TextView tv = (TextView) view.findViewById( getTextviewDelIdMainScreen(ms.getServiceType()) );
			tv.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onClick(View view) {
		if( isServicesButton(view.getId()) ){
			if(isForDelete){
				((MyApplication) getActivity().getApplication()).deleteService( getServiceFromButtonId( view.getId() ).getServiceType() );
				//setForNormal();
				((MainActivity) getActivity()).pagerAdapter.notifyDataSetChanged();				
			} else {
				MessageService ser = getServiceFromButtonId(view.getId()); //TODO: replace for tag?
				((MyApplication) getActivity().getApplication()).active_service = ser.getServiceType();
				getActivity().removeDialog(view.getId());
				getActivity().showDialog(view.getId());
			}
		}		
	}

}
