package com.example.mymessenger.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.EmojiPopup;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.MyApplication.AsyncTaskCompleteListenerMsg;
import com.example.mymessenger.MyDialogsAdapter;
import com.example.mymessenger.MyMsgAdapter;
import com.example.mymessenger.R;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.msInterfaceMS;
import com.example.mymessenger.ui.PullToRefreshListView.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;


public class ListViewSimpleFragment extends Fragment implements OnClickListener, OnTouchListener {
    public static int DIALOGS = 1;
    public static int MESSAGES = 2;
    public int mode;

    private boolean listview_refreshing_for_dlgs = false;

    int async_complete_listener_msg_update_total_offset;
    MyMsgAdapter msg_adapter;
    MyDialogsAdapter dlg_adapter;
    private PullToRefreshListView listview;
    MyApplication app;

    private int supposedFVI;

    int loaded_dlgs_from_each;
    int last_requested_msgs_size = 0;

    List<Integer> lv_dpos = new ArrayList<Integer>();
    boolean lv_update_pos_running = false;
    private View rootView;

    public EmojiPopup emojiPopup;

    protected boolean keyboardVisible = false;

    public int POSITION = FragmentPagerAdapter.POSITION_UNCHANGED;

    public int selected_service_for_dialogs = 0;

    boolean dl_all_msgs_downloaded = false;

    // newInstance constructor for creating fragment with arguments
    public static ListViewSimpleFragment newInstance(int mode) {
        ListViewSimpleFragment fragmentFirst = new ListViewSimpleFragment();
        fragmentFirst.mode = mode;
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ListViewSimpleFragment", "onCreate()");
        setHasOptionsMenu(true);
        if(savedInstanceState != null)
            mode = savedInstanceState.getInt("fragment_mode");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = null;

        POSITION = FragmentPagerAdapter.POSITION_UNCHANGED;



        if (mode == MESSAGES) {
            rootView = inflater.inflate(R.layout.msg_list, container, false);

            listview = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);
            listview.setDivider(null);
            Button send_button = ((Button) rootView.findViewById(R.id.msg_sendbutton));
            send_button.setOnClickListener(this);

            msg_adapter = new MyMsgAdapter(getActivity(), new ArrayList<mMessage>());
            listview.setAdapter(msg_adapter);


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

            app.registerMsgUpdater(async_complete_listener_msg_update);

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

            this.emojiPopup = new EmojiPopup(getActivity(), rootView, R.drawable.ic_msg_panel_smiles, (MessageService) app.msManager.getActiveService());


            msInterfaceMS ms = app.msManager.getActiveService();

            if(ms != null){
                setDialog(ms.getActiveDialog());
            } else {
                setDialog(null);
            }

        }

        if (mode == DIALOGS) {
            rootView = inflater.inflate(R.layout.listview_simple, container, false);
            listview = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);

            dlg_adapter = new MyDialogsAdapter(getActivity(), null);
            listview.setAdapter(dlg_adapter);

            selected_service_for_dialogs = app.sPref.getInt("selected_service_for_dialogs", 0);

            if(app.msManager.getService(selected_service_for_dialogs) == null) selected_service_for_dialogs = 0;

            if(selected_service_for_dialogs == 0) {
                app.msManager.requestDialogs(20, 0, dlg_receiver);
            } else {
                app.msManager.getService(selected_service_for_dialogs).requestDialogs(20, 0, dlg_receiver);
            }
            loaded_dlgs_from_each = 20;

            listview.setOnItemClickListener(DlgClickListener);
            listview.setOnScrollListener(DlgScrollListener);

            final PullToRefreshListView listView = (PullToRefreshListView) rootView.findViewById(R.id.listview_object);
            listView.setOnRefreshListener(new OnRefreshListener() {

                @Override
                public void onRefresh() {
                    listview_refreshing_for_dlgs = true;
                    if(selected_service_for_dialogs == 0) {
                        app.msManager.refreshDialogsFromNet(dlg_receiver, 0);
                    } else {
                        app.msManager.getService(selected_service_for_dialogs).refreshDialogsFromNet(dlg_receiver, 0);
                    }
                }
            });

            app.registerDlgUpdater(async_complete_listener_dlg);
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

                /*
                InputMethodManager inputManager = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(
                        app.getMainActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                */

                Log.d("ActivityTwo.onClick.msg_sendbutton", text);
                msInterfaceMS ms = app.msManager.getActiveService();
                mDialog dlg = ms.getActiveDialog();

                if(dlg.chat_id == 0){
                    ms.sendMessage(dlg.participants.get(0).address, text);
                } else {
                    ms.sendMessage(String.valueOf(2000000000 + dlg.chat_id), text);
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
        if(mode == DIALOGS)app.setUA(MyApplication.UA_DLGS_LIST);
        if(mode == MESSAGES)app.setUA(MyApplication.UA_MSGS_LIST);
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
        app.setUA(0);
        // Apply any required UI change now that the Fragment is visible.
    }

    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }


    public AsyncTaskCompleteListener<List<mDialog>> dlg_receiver = new AsyncTaskCompleteListener<List<mDialog>>(){

        @Override
        public void onTaskComplete(List<mDialog> result) {
            if(listview_refreshing_for_dlgs && !app.msManager.isLoadingDlgs()){
                listview.onRefreshComplete();
                listview_refreshing_for_dlgs = false;
            }

            for(mDialog dlg : result)app.triggerDlgUpdaters(dlg);
        }
    };

    AsyncTaskCompleteListener<Void> updater = new AsyncTaskCompleteListener<Void>(){
        @Override
        public void onTaskComplete(Void result) {
            if(msg_adapter != null)msg_adapter.notifyDataSetChanged();
            if(dlg_adapter != null)dlg_adapter.notifyDataSetChanged();
        }

    };

    public void setSelectedService(int ser_type) {
        if(mode != DIALOGS) return;
        if(selected_service_for_dialogs == ser_type) return;
        selected_service_for_dialogs = ser_type;

        dlg_adapter.clear();
        if(selected_service_for_dialogs == 0) {
            app.msManager.requestDialogs(20, 0, dlg_receiver);
        } else {
            app.msManager.getService(selected_service_for_dialogs).requestDialogs(20, 0, dlg_receiver);
        }

        app.sPref.edit().putInt("selected_service_for_dialogs", selected_service_for_dialogs).commit();
    }

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
            Log.d("async_complete_listener_msg", "completed :: " + String.valueOf(((MessageService) app.msManager.getActiveService()).isLoadingMsgsForDlg(app.msManager.getActiveService().getActiveDialog())));
            //Log.d("async_complete_listener_msg", "start size = " + String.valueOf(showing_messages.size()) + " :: " + String.valueOf(result.size()));
            boolean changed = false;
            int append_count = 0;

            if(result.size() == 0)dl_all_msgs_downloaded = true;

            for(mMessage msg : result){
                boolean added = false;

                int tind = msg_adapter.indexOf(msg);
                if(tind != -1){
                    added = true;
                } else {
                    for(int i = 0; i < msg_adapter.getCount(); i++){
                        if( Time.compare(msg.sendTime, ((mMessage) msg_adapter.getItem(i)).sendTime ) <= 0 ){
                            msg_adapter.add(i, msg);
                            if(i == 0)append_count++;
                            added = true;
                            break;
                        }
                    }
                }
                if(!added){
                    msg_adapter.add(msg);
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

            if(!((MessageService) app.msManager.getActiveService()).isLoadingMsgsForDlg(app.msManager.getActiveService().getActiveDialog())){
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

    AsyncTaskCompleteListener<mDialog> async_complete_listener_dlg = new AsyncTaskCompleteListener<mDialog>(){

        @Override
        public void onTaskComplete(mDialog dlg) {
            boolean changed = false;

            if(selected_service_for_dialogs != 0 && selected_service_for_dialogs != dlg.getMsgServiceType())return;

            /*
            if(dlg.last_msg_time.before( ((mDialog) dlg_adapter.getItem(dlg_adapter.getCount() - 1)).last_msg_time ) ){ // Поступивший диалог был позже, чем последний отображаемый
                return;
            }
            */

            boolean added = false;

            int tind = dlg_adapter.indexOf(dlg);
            if(tind != -1){
                mDialog dlg2 = dlg_adapter.remove(tind);
                dlg2.update(dlg);
                dlg = dlg2;
            }

            for(int i = 0; i < dlg_adapter.getCount(); i++){
                if(((mDialog) dlg_adapter.getItem(i)).getLastMessageTime() == null || dlg.getLastMessageTime() == null){
                    Log.d("smth", "wrong");
                }
                if( dlg.getLastMessageTime().after( ((mDialog) dlg_adapter.getItem(i)).getLastMessageTime() ) ){
                    dlg_adapter.add(i, dlg);
                    added = true;
                    break;
                }
            }

            if(!added){
                dlg_adapter.add(dlg);
                changed = true;
            }

            if(changed)dlg_adapter.notifyDataSetChanged();
        }

    };

    AsyncTaskCompleteListenerMsg async_complete_listener_msg_update = new AsyncTaskCompleteListenerMsg(){
        @Override
        public void onTaskComplete(mMessage msg, mDialog dlg) {
            if(msg_adapter.getCount() == 0)return;

            if(msg.msg_service != app.msManager.getActiveService().getServiceType()){ // Не тот сервис - источник
                return;
            }

            if(!dlg.equals(app.msManager.getActiveService().getActiveDialog())){ // Не тот диалог
                return;
            }

            if(msg.sendTime.before( ((mMessage) msg_adapter.getItem(0)).sendTime ) ){ // Поступившее сообщение было позже, чем последнее отображаемое
                return;
            }

            int tind = msg_adapter.indexOf(msg);
            if(tind != -1){
                mMessage msg2 = (mMessage) msg_adapter.getItem(tind);
                msg2.update(msg);
            } else {
                for (int i = msg_adapter.getCount() - 1; i >= 0; i--) {
                    if (msg.sendTime.after(((mMessage) msg_adapter.getItem(0)).sendTime)) {
                        msg_adapter.add(i + 1, msg);
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
            mDialog dlg = ((mDialog) dlg_adapter.getItem(position));
            app.msManager.setActiveService(dlg.getMsgServiceType());
            app.msManager.getService(dlg.getMsgServiceType()).setActiveDialog(dlg);
            ((MainActivity) getActivity()).pagerAdapter.recreateFragment(2);

            //fr.refresh_data();
            ((MainActivity) getActivity()).mViewPager.setCurrentItem(2);
            //Intent intent = new Intent(getActivity(), ActivityTwo.class);
            //intent.putExtra("mode", "messages");
            //startActivity(intent);
        }

    };

    OnScrollListener DlgScrollListener = new OnScrollListener(){

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if ( !app.dlgs_loading_maxed && ( (totalItemCount - (firstVisibleItem + visibleItemCount)) < 5 ) && !app.msManager.isLoadingDlgs()) {
                app.msManager.requestDialogs(20, loaded_dlgs_from_each, dlg_receiver);
                loaded_dlgs_from_each += 20;
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


            if ( ( firstVisibleItem == 0 ) && app.msManager.getActiveService() != null && app.msManager.getActiveService().getActiveDialog() != null && !dl_all_msgs_downloaded){
                if(!listview.isRefreshing()){
                    msInterfaceMS ms = app.msManager.getActiveService();
                    //last_requested_msgs_size = showing_messages.size();
                    ms.requestMessages(ms.getActiveDialog(), 20, msg_adapter.getCount(), async_complete_listener_msg);
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
        if(mode == MESSAGES){
            int firstVisibleRow = listview.getFirstVisiblePosition();
            int lastVisibleRow = listview.getLastVisiblePosition();

            for(int i=firstVisibleRow;i<=lastVisibleRow;i++){
                mMessage msg = ((mMessage) listview.getItemAtPosition(i));
                if(msg == null)continue;
                if(!msg.getFlag(mMessage.LOADING) && !msg.getFlag(mMessage.OUT) && !msg.getFlag(mMessage.READED)){
                    app.msManager.getActiveService().requestMarkAsReaded(msg, app.msManager.getActiveService().getActiveDialog());
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

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("fragment_mode", mode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //Log.d("ListViewSimpleFragment", "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);
    }


    public void setDialog(mDialog dlg){
        if(mode == MESSAGES){
            msg_adapter.setDlg(dlg);
            Button send_button = ((Button) rootView.findViewById(R.id.msg_sendbutton));
            View myLayout = rootView.findViewById( R.id.msg_footer );
            ImageView b = (ImageView) myLayout.findViewById(R.id.msg_keyboard);
            EditText et = (EditText) myLayout.findViewById(R.id.msg_entertext);
            msInterfaceMS ms = app.msManager.getActiveService();
            if(dlg != null && ms != null){
                if(ms.getServiceType() == MessageService.FB){
                    ms.requestMessages(ms.getActiveDialog(), 20, 0, async_complete_listener_msg);
                    et.setEnabled(false);
                    b.setEnabled(false);
                    send_button.setEnabled(false);
                    listview.setRefreshing();
                } else {
                    ms.requestMessages(ms.getActiveDialog(), 20, 0, async_complete_listener_msg);
                    listview.setRefreshing();
                }
            } else {
                et.setEnabled(false);
                b.setEnabled(false);
                send_button.setEnabled(false);
                listview.onRefreshComplete();
            }
        }
    }



}
