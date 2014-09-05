package com.example.mymessenger.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.msInterfaceMS;
import com.example.mymessenger.services.MessageService.msInterfaceUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ServicesMenuFragment extends Fragment implements OnClickListener, OnTouchListener {
	static final int MENU_CON_MOVE = 101;
	static final int MENU_CON_DELETE = 102;

	boolean isForDelete = false;
	
	public int POSITION = FragmentPagerAdapter.POSITION_UNCHANGED;

    Map<Integer, View> service_button_views = new HashMap<Integer, View>();
	
	@Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
		isForDelete = false;

        POSITION = FragmentPagerAdapter.POSITION_UNCHANGED;
		
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.activity_main, container, false);

        rootView.setOnTouchListener(this);
        
        Bundle args = getArguments();
        //((TextView) rootView.findViewById(android.R.id.text1)).setText(
        //        Integer.toString(args.getInt(ARG_OBJECT)));
        
        MyApplication app = (MyApplication) getActivity().getApplicationContext();
        
        LinearLayout ll_list = (LinearLayout) rootView.findViewById(R.id.linearlay_mainbuttons);

        for(msInterfaceUI ser : app.msManager.myMsgServices){
        	View button_view = inflater.inflate(R.layout.main_servicerow, container, false);
        	
        	Button service_button = (Button) button_view.findViewById(R.id.service_button);
        	service_button.setText(ser.getServiceName());
            service_button.setTag(R.id.SERVICE_BUTTON_SERVICE_KEY, ser.getServiceType());
        	service_button.setOnClickListener(this);
        	registerForContextMenu(service_button);
        	
        	TextView service_delete = (TextView) button_view.findViewById(R.id.service_delete);
        	service_delete.setVisibility(View.INVISIBLE);

            ProgressBar prog_bar = (ProgressBar) button_view.findViewById(R.id.service_loading);
            if(ser.isLoading()) prog_bar.setVisibility(View.VISIBLE);
            else prog_bar.setVisibility(View.INVISIBLE);

        	ll_list.addView(button_view);
            service_button_views.put(ser.getServiceType(), button_view);
            Log.d("myLogs", "Service button added: " + ser.getServiceName());
        }
        //((ViewGroup)rootView).addView(row1);
        return rootView;
    }

    private boolean isServicesButton(View view) {
        if (view.getTag(R.id.SERVICE_BUTTON_SERVICE_KEY) == null)
            return false;
        else
            return true;
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
        for(View view : service_button_views.values()){
            TextView tv = (TextView) view.findViewById( R.id.service_delete );
            tv.setVisibility(View.VISIBLE);
        }
	}
	
	public boolean isForDeleteService(){
		return isForDelete;
	}
	
	public void setForNormal(){
		isForDelete = false;
        for(View view : service_button_views.values()){
            TextView tv = (TextView) view.findViewById( R.id.service_delete );
            tv.setVisibility(View.INVISIBLE);
        }
	}

	@Override
	public void onClick(View view) {
		if( isServicesButton(view) ){
			if(isForDelete){
                ((MyApplication) getActivity().getApplication()).msManager.deleteService((Integer) view.getTag());
				//setForNormal();
				ServicesMenuFragment fr = (ServicesMenuFragment) ((MainActivity) getActivity()).pagerAdapter.getRegisteredFragment(0);		
				fr.POSITION = FragmentPagerAdapter.POSITION_NONE;
				
				ListViewSimpleFragment fr2 = (ListViewSimpleFragment) ((MainActivity) getActivity()).pagerAdapter.getRegisteredFragment(1);				
				fr2.POSITION = FragmentPagerAdapter.POSITION_NONE;
				
				((MainActivity) getActivity()).pagerAdapter.notifyDataSetChanged();				
			} else {
				MessageService ser = ((MyApplication) getActivity().getApplication()).msManager.getService((Integer) view.getTag(R.id.SERVICE_BUTTON_SERVICE_KEY));
				((MyApplication) getActivity().getApplication()).msManager.active_service = ser.getServiceType();
				getActivity().removeDialog(view.getId());
				getActivity().showDialog(view.getId());
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(isForDelete){
			setForNormal();	
		}
		return false;
	}

    public void setServiceLoading(MessageService ms, boolean fl) {
        View view = getView();
        ProgressBar prog_bar = (ProgressBar) service_button_views.get(ms.getServiceType()).findViewById( R.id.service_loading );
        if(fl)
            prog_bar.setVisibility(View.VISIBLE);
        else
            prog_bar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d("ServicesMenuFragment", "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);
    }
}
