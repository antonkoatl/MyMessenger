package com.example.mymessenger.services.Facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MessageService;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.widget.WebDialog;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Page;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.listeners.OnActionListener;
import com.sromku.simple.fb.listeners.OnLoginListener;
import com.sromku.simple.fb.listeners.OnProfileListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        Bundle params = new Bundle();
        params.putString("message", text);
        //params.putString("data", json);
        final Context context = MyApplication.context;
        WebDialog requestsDialog = (new WebDialog.RequestsDialogBuilder(
                context, mSimpleFacebook.getSession(), params)).setOnCompleteListener(
                new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {

                        if (error != null) {
                            if (error instanceof FacebookOperationCanceledException) {
                                Toast.makeText(context, "Request cancelled",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Network Error",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            final String requestId = values
                                    .getString("request");
                            if (requestId != null) {
                                Toast.makeText(context, "Request sent",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Request cancelled",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                }).build();
        requestsDialog.show();
        return false;
    }

    @Override
    protected void requestAccountInfoFromNet(final AsyncTaskCompleteListener<mContact> cb) {
        Profile.Properties properties = new Profile.Properties.Builder()
                .add(Profile.Properties.ID)
                .add(Profile.Properties.NAME)
                .add(Profile.Properties.PICTURE)
                .build();

        mSimpleFacebook.getProfile(properties, new OnProfileListener() {

            @Override
            public void onComplete(Profile profile) {
                mContact cnt = new mContact(profile.getId());
                cnt.name = profile.getName();
                cnt.icon_50_url = profile.getPicture();
                if (cb != null) cb.onTaskComplete(cnt);
            }

        });
    }

    @Override
    protected void getContactsDataFromNet(final CntsDataDownloadsRequest req) {
        req.onStarted();
        final List<mContact> cnts = new ArrayList<mContact>();

        Profile.Properties properties = new Profile.Properties.Builder()
                .add(Profile.Properties.ID)
                .add(Profile.Properties.NAME)
                .add(Profile.Properties.PICTURE)
                .build();

        for(int i = 0; i < req.cnts.size(); i++) {
            mContact cnt = req.cnts.get(i);
            boolean isLast = false;
            if(i == (req.cnts.size() - 1))isLast = true;

            mSimpleFacebook.getProfile(cnt.address, properties, new OnProfileListener() {
                mContact cnt;
                boolean isLast = false;

                public OnProfileListener setMyParams(mContact cnt, boolean isLast){
                    this.cnt = cnt;
                    this.isLast = isLast;
                    return this;
                }

                @Override
                public void onComplete(Profile profile) {
                    cnt.name = profile.getName();
                    cnt.icon_50_url = profile.getPicture();
                    cnts.add(cnt);
                    if(isLast)req.onFinished(cnts);
                }

            }.setMyParams(cnt, isLast));
        }
    }

    @Override
    protected void getContactsFromNet(CntsDownloadsRequest req) {

    }

    @Override
    protected void getMessagesFromNet(final MsgsDownloadsRequest req) {
        req.onStarted();

        Bundle params = new Bundle();
        params.putString("limit", String.valueOf(req.count));
        params.putString("offset", String.valueOf(req.offset));

        mSimpleFacebook.get(String.valueOf(req.dlg.chat_id), "comments", params, new OnActionListener<Page>() {

            @Override
            public void onComplete(Page response) {
                List<mMessage> msgs = new ArrayList<mMessage>();
                try {
                    JSONObject jThread = response.getGraphObject().getInnerJSONObject();
                    JSONArray comments = jThread.getJSONArray("data");

                    for(int i = 0; i < comments.length(); i++){
                        JSONObject jMessage = comments.getJSONObject(i);
                        mMessage msg = new mMessage();

                        JSONObject jFrom = jMessage.getJSONObject("from");
                        msg.respondent = getContact(jFrom.getString("id"));
                        if(msg.respondent.equals(getMyContact())){
                            msg.setOut(true);
                        } else {
                            msg.setOut(false);
                        }

                        msg.text = jMessage.getString("message");

                        SimpleDateFormat incomingFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                        Date date = incomingFormat.parse(jMessage.getString("created_time"));

                        msg.sendTime.set( date.getTime() );
                        msg.id = jMessage.getString("id");
                        msg.msg_service = getServiceType();

                        msgs.add(msg);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                req.onFinished(msgs);

                if (hasNext()) {
                    getNext();
                }
            }

        });
    }

    @Override
    protected void getDialogsFromNet(final DlgsDownloadsRequest req) {
        req.onStarted();

        Bundle params = new Bundle();
        params.putString("limit", String.valueOf(req.count));
        params.putString("offset", String.valueOf(req.offset));

        mSimpleFacebook.get("me", "inbox", params, new OnActionListener<List<Page>>() {

            @Override
            public void onComplete(List<Page> response) {
                //Log.i(TAG, "Number of music pages I like = " + response.size());
                List<mDialog> dlgs = new ArrayList<mDialog>();
                for(Page pThread : response) {
                    try {
                        mDialog dlg = new mDialog();
                        JSONObject jThread = pThread.getGraphObject().getInnerJSONObject();

                        dlg.chat_id = jThread.getLong("id");

                        JSONArray resps = jThread.getJSONObject("to").getJSONArray("data");
                        for(int i = 0; i < resps.length(); i++){
                            JSONObject resp = resps.getJSONObject(i);
                            mContact cnt = getContact( resp.getString("id") );
                            cnt.name = resp.getString("name");
                            dlg.participants.add(cnt);
                        }

                        dlg.last_msg_time.set( Long.valueOf(jThread.getString("updated_time"))*1000 );

                        JSONObject last_message = jThread.getJSONObject("comments").getJSONArray("data").getJSONObject(0);
                        dlg.snippet = last_message.getString("message");
                        if(last_message.getJSONObject("from").getString("id") == getMyContact().address){
                            dlg.snippet_out = 1;
                        } else {
                            dlg.snippet_out = 0;
                        }

                        dlgs.add(dlg);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                req.onFinished(dlgs);

                if (hasNext()) {
                    getNext();
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
