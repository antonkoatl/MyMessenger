package com.example.mymessenger.services.Facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MessageService;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Page;
import com.sromku.simple.fb.listeners.OnActionListener;
import com.sromku.simple.fb.listeners.OnLoginListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class mFacebook extends MessageService {
    public static final String TAG = "mFacebook";
    public static final String APP_ID = "1477753472465048";
    public static final String NAMESPACE = "mymessenger_yyns";
    Permission[] permissions = new Permission[] {
            Permission.USER_PHOTOS,
            Permission.EMAIL,
            Permission.PUBLISH_ACTION,
            Permission.READ_MAILBOX,
    };
    private SimpleFacebook mSimpleFacebook;
    private boolean auth_on_resume = false;

    public mFacebook(MyApplication app) {
        super(app, FB, R.string.service_name_fb);

        SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                .setAppId(APP_ID)
                .setNamespace(NAMESPACE)
                .setPermissions(permissions)
                .build();

        SimpleFacebook.setConfiguration(configuration);
    }

    @Override
    public void authorize(Context context) {
        if(mSimpleFacebook == null){
            auth_on_resume = true;
        } else {
            mSimpleFacebook.login(onLoginListener);
        }
    }

    @Override
    public void unsetup() {

    }

    @Override
    public void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb) {

    }

    @Override
    public void requestMarkAsReaded(mMessage msg, mDialog dlg) {

    }

    @Override
    public boolean sendMessage(String address, String text) {
        return false;
    }

    @Override
    protected void requestAccountInfoFromNet(AsyncTaskCompleteListener<mContact> cb) {

    }

    @Override
    protected void getContactsDataFromNet(CntsDataDownloadsRequest req) {

    }

    @Override
    protected void getContactsFromNet(CntsDownloadsRequest req) {

    }

    @Override
    protected void getMessagesFromNet(MsgsDownloadsRequest req) {

    }

    @Override
    protected void getDialogsFromNet(DlgsDownloadsRequest req) {
        req.onStarted();
        mSimpleFacebook.get("me", "inbox", null, new OnActionListener<List<Page>>() {

            @Override
            public void onComplete(List<Page> response) {
                //Log.i(TAG, "Number of music pages I like = " + response.size());
                List<mDialog> dlgs = new ArrayList<mDialog>();
                for(Page p : response) {
                    try {
                        mDialog dlg = new mDialog();
                        dlg.chat_id = Long.valueOf((String) response.get(0).getGraphObject().getProperty("id"));

                        JSONArray resps = response.get(0).getGraphObject().getInnerJSONObject().getJSONObject("to").getJSONArray("data");
                        for(int i = 0; i < resps.length(); i++){
                            JSONObject resp = resps.getJSONObject(i);
                            mContact cnt = getContact( resp.getString("id") );
                            cnt.name = resp.getString("name");
                            dlg.participants.add(cnt);
                        }




                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

        });
    }

    @Override
    protected void logout_from_net() {

    }

    @Override
    public String getEmojiUrl(long code) {
        return null;
    }

    @Override
    public String[] getStringsForMainViewMenu() {
        return new String[0];
    }

    @Override
    public void MainViewMenu_click(int which, Context context) {

    }





    OnLoginListener onLoginListener = new OnLoginListener() {
        @Override
        public void onLogin() {
            // change the state of the button or do whatever you want
            Log.i(TAG, "Logged in");
            onAuthorize();
        }

        @Override
        public void onNotAcceptingPermissions(Permission.Type type) {

        }

        @Override
        public void onThinking() {

        }

        @Override
        public void onException(Throwable throwable) {

        }

        @Override
        public void onFail(String reason) {

        }

    /*
     * You can override other methods here:
     * onThinking(), onFail(String reason), onException(Throwable throwable)
     */
    };


    @Override
    public void onResume(Activity activity) {
        mSimpleFacebook = SimpleFacebook.getInstance(activity);
        if(auth_on_resume){
            auth_on_resume = false;
            authorize(activity);
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, final Intent data){
        mSimpleFacebook.onActivityResult(activity, requestCode, resultCode, data);
    }


}
