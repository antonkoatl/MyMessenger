package com.example.mymessenger;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.Sms.Sms;
import com.example.mymessenger.services.Vk.Vk;
import com.example.mymessenger.services.Twitter.mTwitter;
import com.example.mymessenger.ui.ListViewSimpleFragment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyApplication extends Application {
    public static final String PREF_NAME = "MyPref";


    public static Context context;
    public MessageServiceManager msManager;

    PendingIntent pi;
    public SharedPreferences sPref;


    public List<AsyncTaskCompleteListener<Void>> cnts_updaters;
    public List<AsyncTaskCompleteListener<List<mDialog>>> dlgs_updaters;
    public List<AsyncTaskCompleteListenerMsg> msg_updaters;

    public List<download_waiter> dl_waiters;
    public DBHelper dbHelper;

    public boolean msgs_loading_maxed = false;
    public boolean dlgs_loading_maxed = false;

    private static Activity mMainActivity = null;

    static HandlerThread thread1 = new HandlerThread("MsgListHandlerThread");
    static public Handler handler1 = null;

    public int active_user_action = 0;
    public static final int UA_SERVICES_MENU = 1;
    public static final int UA_DLGS_LIST = 2;
    public static final int UA_MSGS_LIST = 3;

    public void setUA(int action){
        active_user_action = action;
    }

    public int getUA(){
        return active_user_action;
    }

    static{
        thread1.start();
        handler1 = new Handler(thread1.getLooper());
    }

    class DoublePrintStream extends PrintStream {

        PrintStream origStream 	= null;

        public DoublePrintStream(OutputStream out, PrintStream orig) {
            super(out);
            this.origStream = orig;
        }

        public void flush(){
            super.flush();
            this.origStream.flush();
        }

        public void write(byte[] buf, int off, int len){
            super.write(buf, off, len);
            this.origStream.write(buf, off, len);
        }

        public void write(int b){
            super.write(b);
            this.origStream.write(b);
        }

        public void write(byte[] b) throws IOException {
            super.write(b);
            this.origStream.write(b);
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();

        FileOutputStream fos = null;
        try {
            fos = openFileOutput("errors.txt", MODE_WORLD_READABLE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintStream ps = new DoublePrintStream(fos, System.err);

        System.setErr(ps);

        if (context == null) {
            context = getApplicationContext();
        }

        dbHelper = new DBHelper(this); //Класс для работы с бд

        cnts_updaters = new ArrayList<AsyncTaskCompleteListener<Void>>(); //Обработчики обвновлений контактных данных
        dlgs_updaters = new ArrayList<AsyncTaskCompleteListener<List<mDialog>>>(); //Обработчики обвновлений диалогов
        msg_updaters = new ArrayList<AsyncTaskCompleteListenerMsg>(); //Обработчики обвновлений сообщений

        dl_waiters = new ArrayList<download_waiter>(); //Обработчики завершения загрузок

        sPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE); //загрузка конфигов

        msManager = new MessageServiceManager(this);
        msManager.init();

        //Запуск сервиса обновлений        
        Intent intent1 = new Intent(this, UpdateService.class);
        startService(intent1);

    }



    public static Activity getMainActivity(){
        return mMainActivity;
    }

    public static void setMainActivity(Activity mActivity){
        mMainActivity = mActivity;
    }







    public void registerCntsUpdater(AsyncTaskCompleteListener<Void> updater){
        if(!cnts_updaters.contains(updater))cnts_updaters.add(updater);
    }

    public void registerDlgsUpdater(AsyncTaskCompleteListener<List<mDialog>> updater){
        if(!dlgs_updaters.contains(updater))dlgs_updaters.add(updater);
    }

    public void registerMsgUpdater(AsyncTaskCompleteListenerMsg updater){
        if(!msg_updaters.contains(updater))msg_updaters.add(updater);
    }

    public void unregisterCntsUpdater(AsyncTaskCompleteListener<Void> updater){
        cnts_updaters.remove(updater);
    }

    public void unregisterDlgsUpdater(AsyncTaskCompleteListener<List<mDialog>> updater){
        dlgs_updaters.remove(updater);
    }

    public void unregisterMsgUpdater(AsyncTaskCompleteListenerMsg updater){
        msg_updaters.remove(updater);
    }

    public void triggerCntsUpdaters(){
        if(getMainActivity() != null){
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(AsyncTaskCompleteListener<Void> updater : cnts_updaters)
                        updater.onTaskComplete(null);
                }
            });
        }
    }

    public void triggerDlgsUpdaters(final List<mDialog> dlgs){
        if(getMainActivity() != null){
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(AsyncTaskCompleteListener<List<mDialog>> updater : dlgs_updaters)
                        updater.onTaskComplete(dlgs);
                }
            });
        }
    }

    public void triggerMsgUpdaters(final mMessage msg, final mDialog dlg){

        if(getMainActivity() != null){
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(AsyncTaskCompleteListenerMsg updater : msg_updaters)
                        updater.onTaskComplete(msg, dlg);
                }
            });
        }
    }



    public List<download_waiter> getDownloadWaiters(String url_path) {
        List<download_waiter> dws = new ArrayList<download_waiter>();

        Iterator<download_waiter> it = dl_waiters.iterator();
        while (it.hasNext()) {
            download_waiter dw = it.next();
            if(dw.url.equals(url_path)){
                dws.add(dw);
                it.remove();
            }
        }
        return dws;
    }





    public interface AsyncTaskCompleteListenerMsg{

        public void onTaskComplete(mMessage msg, mDialog dlg);
    }
}
