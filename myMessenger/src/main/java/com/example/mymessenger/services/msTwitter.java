package com.example.mymessenger.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;


import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class msTwitter extends MessageService {
    public static final String CALLBACK_URI = "https://mymessenger.callback";
    public static final String CANCEL_URI = "twitter://cancel";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SECRET_TOKEN = "secret_token";
    protected static String OAUTH_REQUEST_TOKEN = "https://api.twitter.com/oauth/request_token";
    protected static String OAUTH_ACCESS_TOKEN = "https://api.twitter.com/oauth/access_token";
    protected static String OAUTH_AUTHORIZE = "https://api.twitter.com/oauth/authorize";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    public static final int TWITTER_REQUEST_CODE = 1012;


    class double_auth {
        RequestToken mRequestToken;
        AccessToken at;
        String auth_url;
        String verifier;
        Context context;

        double_auth(Context context){
            this.context = context;
        }

        public void getRequestToken(){
            try {
                mRequestToken = mTwitter.getOAuthRequestToken();
                auth_url = mRequestToken.getAuthorizationURL();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }

        public void openUrlForAuth(){
            Intent intent = new Intent(context, SimpleOpenAuthActivity.class);
            intent.putExtra(SimpleOpenAuthActivity.URL_TO_LOAD, auth_url);
            msApp.getMainActivity().startActivityForResult(intent, TWITTER_REQUEST_CODE);
        }

        public void getAccessToken(String verifier){
            try {
                at = mTwitter.getOAuthAccessToken(mRequestToken, verifier);
                mTwitter = factory.getInstance(at);
                Log.d("msTwitter", at.getTokenSecret());
                sPref.edit().putString(ACCESS_TOKEN, at.getToken()).commit();
                sPref.edit().putString(SECRET_TOKEN, at.getTokenSecret()).commit();
                onAuthorize();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }



    }



    Twitter mTwitter;
    TwitterFactory factory;

    public msTwitter(MyApplication app) {
        super(app, TW, R.string.service_name_tw);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .setOAuthConsumerKey("vnZ85Cl5BvVxdUaPSP9sDy8TG")
                .setOAuthConsumerSecret("22fb2mPjzP7WoDr6TmSrubmF2svrQgG5Z6ZNmUfHWCKRmCrJRZ");

        factory = new TwitterFactory((configurationBuilder.build()));
        mTwitter = factory.getInstance();

    }

    @Override
    public void init(){
        String access_token = sPref.getString(ACCESS_TOKEN, null);
        String access_token_secret = sPref.getString(SECRET_TOKEN, null);

        if(access_token != null && access_token_secret != null){
            AccessToken at = new AccessToken(access_token, access_token_secret);
            //configurationBuilder.setOAuthAccessToken(access_token);
            mTwitter.setOAuthAccessToken(at);
            //configurationBuilder.setOAuthAccessTokenSecret(access_token_secret);
            msAuthorised = true;

            onInitFinish();
        } else {
            msAuthorised = false;
            authorize(MyApplication.getMainActivity());
        }
    }


    public void authOnActivityResult(Activity activity, int requestCode, int resultCode, final Intent result) {
        if (requestCode == TWITTER_REQUEST_CODE) {
            if (result != null) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    //Пользователь отменил (нажал назад)
                }
                if (resultCode == Activity.RESULT_OK) {
                    //Получен токен

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            da.getAccessToken( result.getStringExtra(URL_TWITTER_OAUTH_VERIFIER) );
                        }
                    };

                    MyApplication.handler1.post(r);
                }
            }

        }
    }

    double_auth da;

    @Override
    protected void onAuthorize(){
        super.onAuthorize();
        if(!msIsInitFinished) onInitFinish();
        da = null;
    }

    @Override
    public void authorize(Context context) {
        mTwitter = factory.getInstance();

        msAuthorised = false;
        if(msAuthorisationFinished) {
            msAuthorisationFinished = false;
            da = new double_auth(context);
            MyApplication.handler1.post(new Runnable() {
                @Override
                public void run() {
                    da.getRequestToken();
                    da.openUrlForAuth();
                }
            });
        }
    }









    @Override
    public void unsetup() {

    }

    @Override
    public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb) {

    }

    @Override
    public boolean sendMessage(String address, String text) {
        return false;
    }

    @Override
    public void requestMarkAsReaded(mMessage msg, mDialog dlg) {

    }

    @Override
    public void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb) {

    }

    @Override
    public String[] getStringsForMainViewMenu() {
        return new String[0];
    }

    @Override
    public void MainViewMenu_click(int which, Context context) {

    }

    @Override
    protected void requestAccountInfoFromNet(final AsyncTaskCompleteListener<mContact> cb) {


        Runnable r = new Runnable() {
            @Override
            public void run() {
                User user = null;
                try {
                    user = mTwitter.showUser( mTwitter.getId() );
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return;
                }
                mContact cnt = new mContact(user.getScreenName());
                cnt.name = user.getName();
                cnt.icon_50_url = user.getProfileImageURL();
                if (cb != null) cb.onTaskComplete(cnt);
            }
        };

        MyApplication.handler1.post(r);
    }

    @Override
    protected void getContactsFromNet(final List<mContact> cnts) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(mContact cnt : cnts) {
                    User user = null;
                    try {
                        user = mTwitter.showUser(cnt.address);
                    } catch (TwitterException e) {
                        if(e.getMessage().equals("Received authentication challenge is null")){
                            setNotAuthorised();

                            if(MyApplication.getMainActivity() != null){
                                authorize(MyApplication.getMainActivity());
                            }

                        }
                        e.printStackTrace();
                        continue;
                    }
                    cnt.name = user.getName();
                    cnt.icon_50_url = user.getProfileImageURL();

                    updateCntInDB(cnt);
                }
            }
        };

        MyApplication.handler1.post(r);
    }

    @Override
    protected void getMessagesFromNet(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {

    }

    @Override
    protected void getDialogsFromNet(final int count, final int offset, final AsyncTaskCompleteListener<List<mDialog>> cb) {
        dlgs_thread_count += 1;


        Runnable r = new Runnable() {
            @Override
            public void run() {
                int page = offset / count + 1;
                ResponseList<Status> statuses;

                try {
                    statuses = mTwitter.getHomeTimeline(new Paging(page, count));
                } catch (TwitterException e) {
                    if(e.getMessage().equals("Received authentication challenge is null")){
                        setNotAuthorised();

                        if(MyApplication.getMainActivity() != null){
                            authorize(MyApplication.getMainActivity());
                        }
                    }
                    e.printStackTrace();
                    return;
                }

                List<mDialog> dlgs = new ArrayList<mDialog>();
                int i = 0;
                for (Status status : statuses) {
                    if (i < (offset % count)) {
                        i++;
                        continue;
                    }

                    mDialog dlg = new mDialog();

                    dlg.chat_id = status.getId();
                    dlg.participants.add(getContact(status.getUser().getScreenName()));

                    //dlg.title;
                    dlg.snippet = status.getText();
                    //dlg.snippet_out = item.getInt( "out" );
                    dlg.last_msg_time.set(status.getCreatedAt().getTime());
                    dlg.msg_service_type = MessageService.TW;

                    dlgs.add(dlg);
                    //Log.d("msTwitter", status.getUser().getName() + ":" + status.getText());
                }

                dlgs_thread_count--;
                if (cb != null) {
                    ((MainActivity) MyApplication.getMainActivity()).runOnUiThread(new Runnable() {
                        List<mDialog> dlgs;

                        Runnable setDlgs(List<mDialog> dlgs){
                            this.dlgs = dlgs;
                            return this;
                        }

                        @Override
                        public void run() {
                            cb.onTaskComplete(dlgs);
                        }
                    }.setDlgs(dlgs));

                }
            }
        };

        MyApplication.handler1.post(r);
    }

    @Override
    protected void logout_from_net() {

    }

    @Override
    public long[][] getEmojiCodes() {
        return new long[0][];
    }

    @Override
    public String getEmojiUrl(long code) {
        return null;
    }

    @Override
    public int[] getEmojiGroupsIcons() {
        return new int[0];
    }
}
