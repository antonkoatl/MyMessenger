package com.example.mymessenger.ui;

import java.util.ArrayList;
import java.util.List;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.EmojiPopup;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.MyDialogsAdapter;
import com.example.mymessenger.MyMsgAdapter;
import com.example.mymessenger.R;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.R.id;
import com.example.mymessenger.R.layout;
import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.ui.PullToRefreshListView.OnRefreshListener;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.Keyboard;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;


public class ListViewSimpleFragment extends Fragment implements OnClickListener, OnTouchListener {
	String mode;

	private boolean dlg_isLoading = false; //Индикатор загрузки, для автоматической подгрузки диалогов при пролистывании
	private boolean listview_refreshing_for_dlgs = false;
	
	int async_complete_listener_msg_update_total_offset;
	MyMsgAdapter msg_adapter;
	MyDialogsAdapter dlg_adapter;
	private PullToRefreshListView listview;
	MyApplication app;
	List<mMessage> showing_messages;
	List<mDialog> showing_dialogs;
	private int supposedFVI;
	
	int loaded_dlgs_from_each;
	int last_requested_msgs_size = 0;
	
	List<Integer> lv_dpos = new ArrayList<Integer>();
	boolean lv_update_pos_running = false;
	private View rootView;

	public EmojiPopup emojiPopup;

	protected boolean keyboardVisible = false;
	
	// newInstance constructor for creating fragment with arguments
    public static ListViewSimpleFragment newInstance(String mode) {
    	ListViewSimpleFragment fragmentFirst = new ListViewSimpleFragment();
    	fragmentFirst.mode = mode;
        return fragmentFirst;
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = null;

	    //app.active_service = MessageService.VK;
	    
	    
	    if (mode.equals("messages")) {
	    	rootView = inflater.inflate(R.layout.msg_list, container, false);

	    	listview = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);
	    	((Button) rootView.findViewById(R.id.msg_sendbutton)).setOnClickListener(this);
			showing_messages = new ArrayList<mMessage>();
			
			msg_adapter = new MyMsgAdapter(getActivity(), showing_messages);
			listview.setAdapter(msg_adapter);
			
			MessageService ms = app.getActiveService();
			if(ms != null)ms.requestMessages(ms.getActiveDialog(), 20, 0, async_complete_listener_msg);
			
	
			//msg_adapter.isLoading = true;
			//msg_adapter.notifyDataSetChanged();
			
	        listview.setOnItemClickListener(MsgClickListener);
	        listview.setOnScrollListener(MsgScrollListener);
	        
	        
	        supposedFVI = -1;
	        
	        //setTitle(ms.getActiveDialog().getParticipantsNames());
	        
	        final PullToRefreshListView listView = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);
	        listView.setOnRefreshListener(new OnRefreshListener() {

	            @Override
	            public void onRefresh() {
	                // Your code to refresh the list contents

	                // ...

	                // Make sure you call listView.onRefreshComplete()
	                // when the loading is done. This can be done from here or any
	                // other place, like on a broadcast receive from your loading
	                // service or the onPostExecute of your AsyncTask.

	                listView.onRefreshComplete();
	            }
	        });
	    
	        listview.setOnTouchListener(this);
	        View myLayout = rootView.findViewById( R.id.msg_footer );
	        EditText et = (EditText) myLayout.findViewById(R.id.msg_entertext);
	        et.setOnTouchListener(this);
	        
	        et.setOnKeyListener(new OnKeyListener(){

				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if (keyCode != 4 || ListViewSimpleFragment.this.keyboardVisible  || ListViewSimpleFragment.this.emojiPopup == null || (!ListViewSimpleFragment.this.emojiPopup.isShowing())) {
	                    return false;
	                }
	                if (event.getAction() != 1) {
	                    return true;
	                }
	                ListViewSimpleFragment.this.emojiPopup.showEmojiPopup(false);
	                return true;
	            }

	        	
	        });
	        
	        app.registerMsgsUpdater(async_complete_listener_msg_update);
	        
	        ImageView b = (ImageView) myLayout.findViewById(R.id.msg_keyboard);
	        b.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View view) {
					boolean shown;
	                boolean r2z = false;
	                shown = ListViewSimpleFragment.this.emojiPopup != null && ListViewSimpleFragment.this.emojiPopup.isShowing();
	                EmojiPopup r3_EmojiPopup = ListViewSimpleFragment.this.emojiPopup;
	                if (shown) {
	                    r3_EmojiPopup.showEmojiPopup(r2z);
	                } else {
	                    r2z = true;
	                    r3_EmojiPopup.showEmojiPopup(r2z);
	                }

				}
	        	
	        });
	    
	        this.emojiPopup = new EmojiPopup(getActivity(), rootView, R.drawable.ic_msg_panel_smiles, true);
	    }
	    
	    if (mode.equals("dialogs")) {
	    	rootView = inflater.inflate(R.layout.listview_simple, container, false);
	    	listview = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);
	    	
	    	showing_dialogs = new ArrayList<mDialog>();
			
			dlg_adapter = new MyDialogsAdapter(getActivity(), showing_dialogs);
			listview.setAdapter(dlg_adapter);

			app.requestLastDialogs(20, 0, async_complete_listener_dlg);
			loaded_dlgs_from_each = 20;
			
	        listview.setOnItemClickListener(DlgClickListener);
	        listview.setOnScrollListener(DlgScrollListener);
	        
	        final PullToRefreshListView listView = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);
	        listView.setOnRefreshListener(new OnRefreshListener() {

	            @Override
	            public void onRefresh() {
	            	listview_refreshing_for_dlgs = true;
	            	app.refreshtDialogsFromNet(async_complete_listener_dlg, 0);	            	               
	            }
	        });
	        
	        app.registerDlgsUpdater(async_complete_listener_dlg_update);
	    }
	    rootView.setOnTouchListener(this);
	    
	    
	    
        return rootView;
    }
	
	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case R.id.msg_sendbutton :
				EditText textLabel = (EditText) rootView.findViewById(R.id.msg_entertext);
				String text = textLabel.getText().toString();
				textLabel.setText("");
				
				InputMethodManager inputManager = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(
				        app.getCurrentActivity().getCurrentFocus().getWindowToken(),
				        InputMethodManager.HIDE_NOT_ALWAYS); 
				
				Log.d("ActivityTwo.onClick.msg_sendbutton", text);
				MessageService ms = app.getActiveService();
				mDialog dlg = ms.getActiveDialog();
				
				for(mContact cnt : dlg.participants){
					ms.sendMessage(cnt.address, text);
				}
				
				ms.requestMessages(dlg, 1, 0, async_complete_listener_msg);
	
				break;
			}
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
			last_requested_msgs_size = 0;

			msg_adapter.notifyDataSetChanged();
			
			MessageService ms = app.getActiveService();
			if(ms != null){
				ms.requestMessages(ms.getActiveDialog(), 20, 0, async_complete_listener_msg);
				listview.setRefreshing();
			}
			
		}
		
		if (mode.equals("dialogs")) {
			showing_dialogs.clear();		

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
	
	class lvRunnable implements Runnable{
		ListViewSimpleFragment lf;
		
		lvRunnable(ListViewSimpleFragment lf){
			this.lf = lf;					
		}

		@Override
		public void run() {
			if(listview.getLastVisiblePosition() > 0){
				int firstVisibleItem = listview.getFirstVisiblePosition();
				int seltop_pos = firstVisibleItem;
				for(Integer dp : lf.lv_dpos)seltop_pos += dp;
				lf.lv_dpos.clear();
				listview.setSelectionFromTop(seltop_pos, listview.getChildAt(0).getTop());
				lf.lv_update_pos_running = false;
				//listView.getChildAt(i) works where 0 is the very first visible row and (n-1) is the last visible row (where n is the number of visible views you see).
				Log.d("async_complete_listener_msg", String.valueOf(seltop_pos) + " :: " + String.valueOf(firstVisibleItem));
			}
		}
		
	};
	
	public void change_lv_pos(int pos){
		lv_dpos.add(pos);
		if(!lv_update_pos_running){
			listview.post(new lvRunnable(ListViewSimpleFragment.this));
			lv_update_pos_running = true;
		}
	}

	
	// TODO: Merge them:
	AsyncTaskCompleteListener<List<mMessage>> async_complete_listener_msg = new AsyncTaskCompleteListener<List<mMessage>>(){

		@Override
		public void onTaskComplete(List<mMessage> result) {
			Log.d("async_complete_listener_msg", "completed :: " + String.valueOf( app.getActiveService().isLoadingMsgsForDlg(app.getActiveService().getActiveDialog()) ));
			//Log.d("async_complete_listener_msg", "start size = " + String.valueOf(showing_messages.size()) + " :: " + String.valueOf(result.size()));
			boolean changed = false;
			int append_count = 0;

			for(mMessage msg : result){
				boolean added = false;
				
				int tind = showing_messages.indexOf(msg);
				if(tind != -1){
					added = true;
				} else {				
					for(int i = 0; i < showing_messages.size(); i++){
						if( Time.compare(msg.sendTime, showing_messages.get(i).sendTime ) <= 0 ){
							showing_messages.add(i, msg);
							if(i == 0)append_count++;
							added = true;
							break;
						}	
					}
				}
				if(!added){
					showing_messages.add(msg);
					append_count++;
				}
	        }
			//Log.d("async_complete_listener_msg", "scroll for = " + String.valueOf(append_count));
			
			if(result.size() > 0){
				msg_adapter.notifyDataSetChanged();
			}
			
			if(append_count > 0){
				listview.scrollItems(append_count);
			}			
			
			if(!app.getActiveService().isLoadingMsgsForDlg(app.getActiveService().getActiveDialog())){
				listview.onRefreshCompleteNoAnimation();
				//Log.d("async_complete_listener_msg", "finishing refresh");
			}
			
			//change_lv_pos(result.size());		
			
			/*
			final int result_size = result.size();
			final int current_ic = listview.getCount();
				
			listview.post(new Runnable() {
	            @Override
	            public void run() {
	            	if(listview.getLastVisiblePosition() > 1){
	            		int di = listview.getCount() - current_ic;
		            	int firstVisibleItem = listview.getFirstVisiblePosition();
		            	int seltop_pos = msg_adapter.isLoading ? firstVisibleItem  + result_size + 1 : firstVisibleItem  + result_size;
		            	seltop_pos += di;
		            	Log.d("async_complete_listener_msg", String.valueOf(seltop_pos));
		            	listview.setSelectionFromTop(seltop_pos, listview.getChildAt(1).getTop()); //listView.getChildAt(i) works where 0 is the very first visible row and (n-1) is the last visible row (where n is the number of visible views you see).
	            	}
	            }
	        });*/
				
			
			
			//msg_adapter.isLoading = false;			
		}
		
	};
	
	AsyncTaskCompleteListener<List<mDialog>> async_complete_listener_dlg = new AsyncTaskCompleteListener<List<mDialog>>(){

		@Override
		public void onTaskComplete(List<mDialog> result) {
			boolean changed = false;
			for(mDialog dlg : result){
				boolean added = false;
				
				int tind = showing_dialogs.indexOf(dlg);
				if(tind != -1){
					//showing_dialogs.set(tind, dlg);
					mDialog dlg2 = showing_dialogs.remove(tind);
					dlg2.update(dlg);
					dlg = dlg2;
					changed = true;
				}
				
				for(int i = 0; i < showing_dialogs.size(); i++){
					if(showing_dialogs.get(i).getLastMessageTime() == null || dlg.getLastMessageTime() == null){
						Log.d("smth", "wrong");
					}
					if( dlg.getLastMessageTime().after( showing_dialogs.get(i).getLastMessageTime() ) ){
						showing_dialogs.add(i, dlg);
						changed = true;
						added = true;
						break;
					}	
				}

				if(!added){
					showing_dialogs.add(dlg);
					changed = true;
				}
	        }
			
			if(changed)
				dlg_adapter.notifyDataSetChanged();			
			
			if(listview_refreshing_for_dlgs && !app.isLoadingDlgs()){
				listview.onRefreshComplete();
				listview_refreshing_for_dlgs = false;
			}
		}
		
	};
	
	AsyncTaskCompleteListener<List<mDialog>> async_complete_listener_dlg_update = new AsyncTaskCompleteListener<List<mDialog>>(){

		@Override
		public void onTaskComplete(List<mDialog> result) {
			if(showing_dialogs.size() == 0)return;
			for(mDialog dlg : result){
				if(dlg.last_msg_time.before( showing_dialogs.get(showing_dialogs.size() - 1).last_msg_time ) ){ // Поступивший диалог был позже, чем последний отображаемый
					continue;
				}
				
				boolean updated = false;
				
				int tind = showing_dialogs.indexOf(dlg);
				if(tind != -1){
					mDialog dlg2 = showing_dialogs.remove(tind);
					dlg2.update(dlg);
					dlg = dlg2;
					updated = true;
				}
				
				for(int i = 0; i < showing_dialogs.size(); i++){
					if(showing_dialogs.get(i).getLastMessageTime() == null || dlg.getLastMessageTime() == null){
						Log.d("smth", "wrong");
					}
					if( dlg.getLastMessageTime().after( showing_dialogs.get(i).getLastMessageTime() ) ){
						showing_dialogs.add(i, dlg);
						if(!updated){
							showing_dialogs.remove(showing_dialogs.size() - 1);
						}
						break;
					}	
				}
	        }
			
			dlg_adapter.notifyDataSetChanged();			
		}
		
	};
	
	AsyncTaskCompleteListener<List<mMessage>> async_complete_listener_msg_update = new AsyncTaskCompleteListener<List<mMessage>>(){
		@Override
		public void onTaskComplete(List<mMessage> result) {
			if(showing_messages.size() == 0)return;
			
			for(mMessage msg : result){
				if(msg.msg_service != app.getActiveService().getServiceType()){ // Не тот сервис - источник
					continue;
				}
				
				if(msg.sendTime.before( showing_messages.get(0).sendTime ) ){ // Поступившее сообщение было позже, чем последнее отображаемое
					continue;
				}
				
				boolean updated = false;
				
				int tind = showing_messages.indexOf(msg);
				if(tind != -1){
					mMessage msg2 = showing_messages.remove(tind);
					msg2.update(msg);
					msg = msg2;
					updated = true;
				}
				
				for(int i = showing_messages.size() - 1; i >= 0; i--){
					if( msg.sendTime.after( showing_messages.get(i).sendTime) ){
						showing_messages.add(i+1, msg);
						if(!updated){
							showing_messages.remove(0);
						}
						break;
					}	
				}
	        }
			
			msg_adapter.notifyDataSetChanged();		
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
			if ( !app.dlgs_loading_maxed && ( (totalItemCount - (firstVisibleItem + visibleItemCount)) < 5 ) && !dlg_isLoading) {
				app.requestLastDialogs(20, loaded_dlgs_from_each, async_complete_listener_dlg);
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


		/* Если после открытия быстро перемотать наверх, то будет висеть загрузка, не загружаясь из бд
		 * Возникает, когда достигнут верх экрана, а предыдущий запрос всё ещё висит.
		 * Предыдущий запрос завершается -> Лист не обвновляется -> Тут же onScroll и лист снова обновляется, загружая новую порцию данных
		 */
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if(supposedFVI != -1){ // ??
				if(supposedFVI != firstVisibleItem){
					Log.d("MsgScrollListener", "Wrong firstVisibleItem!! :: " + String.valueOf(supposedFVI) + " :: " + String.valueOf(firstVisibleItem));
					firstVisibleItem = supposedFVI;
				} else {
					supposedFVI = -1;
				}
			}
			
			//Log.d("MsgScrollListener", String.valueOf(firstVisibleItem) + ", " + String.valueOf(listview.getFirstVisiblePosition()));
			if (visibleItemCount == 0) return;
			
			
			if ( ( firstVisibleItem == 0 ) && app.getActiveService() != null && app.getActiveService().getActiveDialog() != null && !app.getActiveService().isAllMsgsDownloaded()){
				if(!listview.isRefreshing()){	
					MessageService ms = app.getActiveService();
					//last_requested_msgs_size = showing_messages.size();
					ms.requestMessages(ms.getActiveDialog(), 20, showing_messages.size(), async_complete_listener_msg);		
					listview.setRefreshingNoAnimation();
				}
				
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

	public void update_dlgs(List<mDialog> dlgs) {
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(mode.equals("messages")){
			int firstVisibleRow = listview.getFirstVisiblePosition();
		    int lastVisibleRow = listview.getLastVisiblePosition();
		    
		    for(int i=firstVisibleRow;i<=lastVisibleRow;i++){
		    	mMessage msg = (mMessage) listview.getItemAtPosition(i);
		    	if(msg == null)continue;
		    	if(!msg.getFlag(mMessage.LOADING) && !msg.getFlag(mMessage.OUT) && !msg.getFlag(mMessage.READED)){
		    		app.getActiveService().requestMarkAsReaded(msg);
		    	}
		    }
		}
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration cfg) {
        super.onConfigurationChanged(cfg);
        if (this.emojiPopup == null || (!this.emojiPopup.isShowing())) {
        } else {
            this.emojiPopup.showEmojiPopup(false);
            this.emojiPopup.onKeyboardStateChanged(false, -1);
        }
    }

}
