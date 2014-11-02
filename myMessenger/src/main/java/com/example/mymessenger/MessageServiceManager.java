package com.example.mymessenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.mymessenger.services.Facebook.mFacebook;
import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.msInterfaceMS;
import com.example.mymessenger.services.MessageService.msInterfaceUI;
import com.example.mymessenger.services.Sms.Sms;
import com.example.mymessenger.services.Twitter.mTwitter;
import com.example.mymessenger.services.Vk.Vk;
import com.example.mymessenger.ui.ListViewSimpleFragment;

import java.util.ArrayList;
import java.util.List;

public class MessageServiceManager {
    public static final String PREF_USING_SERVICES = "usingservices";
    public static final String PREF_ACTIVE_SERVICE = "active_service";

    public List<MessageService> myMsgServices;
    MyApplication msApp;
    public int active_service;

    MessageServiceManager(MyApplication app) {
        this.msApp = app;
        myMsgServices = new ArrayList<MessageService>(); //Активные сервисы сообщений
    }

    public void init(){
        //загрузка сервисов
        for(MessageService ms : myMsgServices)
            ms.init();
    }

    public boolean newService(int service_type) {
        boolean isExist = false;

        for(MessageService ms : myMsgServices){
            if(ms.getServiceType() == service_type){
                isExist = true;
                return false;
            }
        }

        MessageService ms = MessageService.createServiceByType(service_type, msApp);
        addMsgService(ms);

        String usingservices = "";
        for(msInterfaceMS mst : myMsgServices){
            if(usingservices.length() == 0){
                usingservices = String.valueOf(mst.getServiceType());
            } else {
                usingservices += "," + String.valueOf(mst.getServiceType());
            }
        }
        msApp.sPref.edit().putString(PREF_USING_SERVICES, usingservices).commit();



        AsyncTaskCompleteListener<MessageService> asms = new AsyncTaskCompleteListener<MessageService>(){

            @Override
            public void onTaskComplete(MessageService ms) {
                ListViewSimpleFragment fr2 = (ListViewSimpleFragment) ((MainActivity) msApp.getMainActivity()).pagerAdapter.getRegisteredFragment(1);
                fr2.POSITION = FragmentPagerAdapter.POSITION_NONE;

                ((MainActivity) msApp.getMainActivity()).runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        ((MainActivity) msApp.getMainActivity()).pagerAdapter.notifyDataSetChanged();
                    }
                });
            }

        };

        ms.setup(asms);

        ((MainActivity) msApp.getMainActivity()).pagerAdapter.recreateFragment(0);

        return true;

    }

    public void deleteService(int service_type){
        MessageService ms = null;
        for(MessageService mst : myMsgServices){
            if(mst.getServiceType() == service_type){
                ms = mst;
                break;
            }
        }
        if(ms == null)return;

        ms.unsetup();
        myMsgServices.remove(ms);

        String usingservices = "";
        for(MessageService mst : myMsgServices){
            if(usingservices.length() == 0) {
                usingservices += String.valueOf(mst.getServiceType());
            } else {
                usingservices += "," + String.valueOf(mst.getServiceType());
            }
        }

        msApp.sPref.edit().putString(PREF_USING_SERVICES, usingservices).commit();
    }


    public void addMsgService(MessageService mServive){
        myMsgServices.add(mServive);
    }

    public MessageService getService(int typeId) {
        for(MessageService ms : myMsgServices){
            if (ms.getServiceType() == typeId ) return ms;
        }
        return null;
    }

    public MessageService getActiveService() {
        return getService(active_service);
    }

    public void setActiveService(int msgService) {
        active_service = msgService;
        msApp.sPref.edit().putInt(PREF_ACTIVE_SERVICE, active_service).commit();
    }

    public void requestDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
        for(MessageService msg : myMsgServices){
            msg.requestDialogs(count, offset, cb);
        }
    }

    public void refreshDialogsFromNet(AsyncTaskCompleteListener<List<mDialog>> cb, int count) {
        for(MessageService msg : myMsgServices){
            msg.refreshDialogsFromNet(cb, count);
        }
    }

    public boolean isLoadingDlgs() {
        boolean res = false;
        for(MessageService ms : myMsgServices)if(ms.isLoadingDlgs())res = true;
        return res;
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onActivityResult(activity, requestCode, resultCode, data);
        }
    }

    public void onSaveInstanceState(Activity activity, Bundle outState) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onSaveInstanceState(activity, outState);
        }
    }

    public void onCreate(Activity activity, Bundle savedInstanceState) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onCreate(activity, savedInstanceState);
        }
    }

    public void onStart(Activity activity) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onStart(activity);
        }
    }

    public void onResume(Activity activity) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onResume(activity);
        }
    }

    public void onPause(Activity activity) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onPause(activity);
        }
    }

    public void onStop(Activity activity) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onStop(activity);
        }
    }

    public void onDestroy(Activity activity) {
        for(msInterfaceUI ms : myMsgServices){
            ms.onDestroy(activity);
        }
    }

    public void load() {
        //загрузка сервисов
        String using_services[] = msApp.getSharedPreferences(MyApplication.PREF_NAME, Context.MODE_PRIVATE).getString(PREF_USING_SERVICES, "10").split(",");
        for(String i : using_services){
            MessageService ms = MessageService.createServiceByType(Integer.valueOf(i), msApp);
            if(ms != null){
                addMsgService(ms);
            }
        }

        active_service = msApp.getSharedPreferences(MyApplication.PREF_NAME, Context.MODE_PRIVATE).getInt(PREF_ACTIVE_SERVICE, 0);
    }
}
