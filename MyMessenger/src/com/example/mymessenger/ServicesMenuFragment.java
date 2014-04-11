package com.example.mymessenger;

import com.example.mymessenger.services.MessageService;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;


public class ServicesMenuFragment extends Fragment {
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

}
