package com.example.mymessenger;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;


public class ListViewSimpleFragment extends Fragment implements OnClickListener {
	Context context;
	String mode;
	private boolean dlg_maxed;
	private boolean msg_maxed;
	private boolean msg_isLoading;
	private boolean dlg_isLoading;
	int async_complete_listener_msg_update_total_offset;
	MyMsgAdapter msg_adapter;
	MyDialogsAdapter dlg_adapter;
	private ListView listview;
	MyApplication app;
	List<mMessage> showing_messages;
	List<mDialog> showing_dialogs;
	private int supposedFVI;
	
	int loaded_dlgs_from_each;
	
	// newInstance constructor for creating fragment with arguments
    public static ListViewSimpleFragment newInstance(String mode) {
    	ListViewSimpleFragment fragmentFirst = new ListViewSimpleFragment();
    	fragmentFirst.mode = mode;
        return fragmentFirst;
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
		dlg_maxed = false;
		msg_maxed = false;
		msg_isLoading = false;
		View rootView = null;

	    //app.active_service = MessageService.VK;
	    
	    
	    if (mode.equals("messages")) {
	    	rootView = inflater.inflate(R.layout.msg_list, container, false);

	    	listview = (ListView) rootView.findViewById(R.id.listview_object);
	    	((Button) rootView.findViewById(R.id.msg_sendbutton)).setOnClickListener(this);
			showing_messages = new ArrayList<mMessage>();
			
			msg_adapter = new MyMsgAdapter(getActivity(), showing_messages);
			listview.setAdapter(msg_adapter);
			
			MessageService ms = app.getActiveService();
			if(ms != null)ms.requestMessages(ms.getActiveDialog(), 0, 20, async_complete_listener_msg);
			
			showing_messages.add(0, null);
			msg_isLoading = true;
			msg_adapter.isLoading = true;
			msg_adapter.notifyDataSetChanged();
			
	        listview.setOnItemClickListener(MsgClickListener);
	        listview.setOnScrollListener(MsgScrollListener);
	        
	        
	        supposedFVI = -1;
	        
	        //setTitle(ms.getActiveDialog().getParticipantsNames());
	    }
	    
	    if (mode.equals("dialogs")) {
	    	rootView = inflater.inflate(R.layout.listview_simple, container, false);
	    	listview = (ListView) rootView.findViewById(R.id.listview_object);
	    	
	    	showing_dialogs = new ArrayList<mDialog>();
			
			dlg_adapter = new MyDialogsAdapter(getActivity(), showing_dialogs);
			listview.setAdapter(dlg_adapter);

			app.requestLastDialogs(0, 20, async_complete_listener_dlg);
			loaded_dlgs_from_each = 20;
			
	        listview.setOnItemClickListener(DlgClickListener);
	        listview.setOnScrollListener(DlgScrollListener);
	    }
	  
        return rootView;
    }
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (MyApplication) activity.getApplication();
	}
	
	@Override
	public void onStart(){
		super.onStart();
		Log.d("ListViewSimpleFragment", "onStart");
		// Apply any required UI change now that the Fragment is visible.
	}
	
	@Override
	public void onResume(){
		super.onResume();
		app.registerCntsUpdater(updater);
		Log.d("ListViewSimpleFragment", "onResume");
		// Apply any required UI change now that the Fragment is visible.
	}
	
	@Override
	public void onPause(){
		super.onPause();
		app.unregisterCntsUpdater(updater);
		Log.d("ListViewSimpleFragment", "onPause");
		// Apply any required UI change now that the Fragment is visible.
	}
	
	@Override
	public void onStop(){
		super.onStop();
		Log.d("ListViewSimpleFragment", "onStop");
		// Apply any required UI change now that the Fragment is visible.
	}
	
	public int getItemPosition(Object object) {
	    return PagerAdapter.POSITION_NONE;
	}
	
	protected void refresh_data() {
		if (mode.equals("messages")) {
			showing_messages.clear();
			msg_maxed = false;
			msg_isLoading = false;
			
			msg_adapter.notifyDataSetChanged();
			
			MessageService ms = app.getActiveService();
			if(ms != null){
				ms.requestMessages(ms.getActiveDialog(), 0, 20, async_complete_listener_msg);
				showing_messages.add(0, null);
				msg_isLoading = true;
				msg_adapter.isLoading = true;
				msg_adapter.notifyDataSetChanged();
			}
			
		}
		
		if (mode.equals("dialogs")) {
			showing_dialogs.clear();		
			
			dlg_maxed = false;
			
			dlg_adapter.notifyDataSetChanged();
			
			app.requestLastDialogs(0, 20, async_complete_listener_dlg);
			loaded_dlgs_from_each = 20;
		}
			
	}
	
	
	AsyncTaskCompleteListener<Void> updater = new AsyncTaskCompleteListener<Void>(){
		@Override
		public void onTaskComplete(Void result) {
			if(msg_adapter != null)msg_adapter.notifyDataSetChanged();
			if(dlg_adapter != null)dlg_adapter.notifyDataSetChanged();
		}
		
	};

	AsyncTaskCompleteListener<List<mMessage>> async_complete_listener_msg = new AsyncTaskCompleteListener<List<mMessage>>(){

		@Override
		public void onTaskComplete(List<mMessage> result) {
			showing_messages.remove(0);
			int s = showing_messages.size();
			for(mMessage msg : result){
	        	showing_messages.add(0, msg);
	        }
			msg_adapter.notifyDataSetChanged();
			if( (showing_messages.size() - s) == 0 )msg_maxed = true;
			
			int firstVisibleItem = listview.getFirstVisiblePosition();

			if(listview.getLastVisiblePosition() > 0)
				listview.setSelectionFromTop(firstVisibleItem  + result.size(), listview.getChildAt(1).getTop()); //listView.getChildAt(i) works where 0 is the very first visible row and (n-1) is the last visible row (where n is the number of visible views you see).

			msg_isLoading = false;
			msg_adapter.isLoading = false;			
		}
		
	};
	
	
	AsyncTaskCompleteListener<List<mDialog>> async_complete_listener_dlg = new AsyncTaskCompleteListener<List<mDialog>>(){

		@Override
		public void onTaskComplete(List<mDialog> result) {
			int s = showing_dialogs.size();
			for(mDialog dlg : result){
				boolean added = false;
				for(int i = 0; i < showing_dialogs.size(); i++){
					if(showing_dialogs.get(i).getLastMessageTime() == null || dlg.getLastMessageTime() == null){
						Log.d("smth", "wrong");
					}
					if( dlg.getLastMessageTime().after( showing_dialogs.get(i).getLastMessageTime() ) ){
						showing_dialogs.add(i, dlg);
						added = true;
						break;
					}	
				}
				if(!added)showing_dialogs.add(dlg);
	        }
			dlg_adapter.notifyDataSetChanged();
			if( (showing_dialogs.size() - s) == 0 )dlg_maxed = true;
			
			dlg_isLoading = false;
		
		}
		
	};
		
	
	AsyncTaskCompleteListener<List<mMessage>> async_complete_listener_msg_update = new AsyncTaskCompleteListener<List<mMessage>>(){
		@Override
		public void onTaskComplete(List<mMessage> result) {
			boolean update = true;

			for(mMessage msg : result){
				if( msg.sendTime.after( showing_messages.get(showing_messages.size()-1).sendTime ) ){
	        		showing_messages.add(msg);
	        		async_complete_listener_msg_update_total_offset++;
	        		msg_adapter.notifyDataSetChanged();
	        	} else {
	        		update = false;
	        		break;
	        	}
	        }
			
			if(update){
				app.getActiveService().requestMessages(app.getActiveService().getActiveDialog(), async_complete_listener_msg_update_total_offset, 20, async_complete_listener_msg_update);
			}
		}
	};

	
	OnItemClickListener DlgClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mDialog dlg = showing_dialogs.get(position);
			app.setActiveService( dlg.getMsgService() );
			app.getService( dlg.getMsgService() ).setActiveDialog(dlg);
			ListViewSimpleFragment fr = (ListViewSimpleFragment) ((MainActivity) getActivity()).pagerAdapter.getRegisteredFragment(2);
			fr.refresh_data();
			((MainActivity) getActivity()).mViewPager.setCurrentItem(2);			
			//Intent intent = new Intent(getActivity(), ActivityTwo.class);
			//intent.putExtra("mode", "messages");
			//startActivity(intent);
		}
		
	};
	
	
	OnScrollListener DlgScrollListener = new OnScrollListener(){

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if ( !dlg_maxed && ( (totalItemCount - (firstVisibleItem + visibleItemCount)) < 5 ) && !dlg_isLoading) {
				app.requestLastDialogs(loaded_dlgs_from_each, 20, async_complete_listener_dlg);
				loaded_dlgs_from_each += 20;
				dlg_isLoading = true;
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	OnItemClickListener MsgClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		}
		
	};
	
	
	OnScrollListener MsgScrollListener = new OnScrollListener(){

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if(supposedFVI != -1){
				if(supposedFVI != firstVisibleItem){
					Log.d("MsgScrollListener", "Wrong firstVisibleItem!!");
					firstVisibleItem = supposedFVI;
				}
				supposedFVI = -1;
			}
			
			//Log.d("MsgScrollListener", String.valueOf(firstVisibleItem) + ", " + String.valueOf(listview.getFirstVisiblePosition()));
			if (visibleItemCount == 0) return;
			if ( !msg_maxed && ( firstVisibleItem == 0 ) && !msg_isLoading ) {
				
				msg_isLoading = true;

				MessageService ms = app.getActiveService();
				ms.requestMessages(ms.getActiveDialog(), showing_messages.size(), 20, async_complete_listener_msg);
				showing_messages.add(0, null);
				msg_adapter.isLoading = true;
				msg_adapter.notifyDataSetChanged();
				listview.setSelectionFromTop(firstVisibleItem  + 1, listview.getChildAt(firstVisibleItem).getTop());
				//Log.d("MsgScrollListener", String.valueOf(firstVisibleItem + lmsgs.size()) + ", " + String.valueOf(listview.getChildAt(firstVisibleItem).getTop()));
				
				//supposedFVI = firstVisibleItem + lmsgs.size();
				//Log.d("MsgScrollListener", String.valueOf(firstVisibleItem + lmsgs.size()) + ", " + String.valueOf(listview.getChildAt(firstVisibleItem).getTop()));
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			
		}
		
	};

	

}
