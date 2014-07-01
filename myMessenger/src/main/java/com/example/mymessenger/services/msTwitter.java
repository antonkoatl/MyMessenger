package com.example.mymessenger.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieSyncManager;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import twitter4j.AccountSettings;
import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Category;
import twitter4j.DirectMessage;
import twitter4j.Friendship;
import twitter4j.IDs;
import twitter4j.Location;
import twitter4j.OEmbed;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Place;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.api.HelpResources;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuth2Token;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import com.sugree.twitter.DialogError;
import com.sugree.twitter.TwDialog;
import com.sugree.twitter.Twitter.DialogListener;
import com.sugree.twitter.TwitterError;

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
                mRequestToken = asyncTwitter.getOAuthRequestToken();
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
                at = asyncTwitter.getOAuthAccessToken(mRequestToken, verifier);
                asyncTwitter = factory.getInstance(at);
                Log.d("msTwitter", at.getTokenSecret());
                onAuthorize();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }



    }


    TwitterListener timelineListener = new TwitterAdapter() {

        @Override
        public void gotHomeTimeline(ResponseList<Status> statuses) {
            for (Status status : statuses) {
                Log.d("msTwitter", status.getUser().getName() + ":" + status.getText());
            }
        }

        @Override
        public void onException(TwitterException te, TwitterMethod method) {
            Log.d("msTwitter", "exep");
        }
    };

    AsyncTwitter asyncTwitter;
    AsyncTwitterFactory factory;

    public msTwitter(MyApplication app) {
        super(app, TW, R.string.service_name_tw);



        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .setOAuthConsumerKey("vnZ85Cl5BvVxdUaPSP9sDy8TG")
                .setOAuthConsumerSecret("22fb2mPjzP7WoDr6TmSrubmF2svrQgG5Z6ZNmUfHWCKRmCrJRZ");

        factory = new AsyncTwitterFactory((configurationBuilder.build()));
        asyncTwitter = factory.getInstance();

        //asyncTwitter.addListener(timelineListener);
        //asyncTwitter.getHomeTimeline();



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
        da = null;
    }

    @Override
    public void authorize(Context context) {
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
        try {
            asyncTwitter.addListener(new TwitterAdapter() {
                @Override
                public void gotUserDetail(User user) {
                    mContact cnt = new mContact(user.getScreenName());
                    cnt.name = user.getName();
                    cnt.icon_100_url = user.getProfileImageURL();
                    if(cb != null)cb.onTaskComplete(cnt);
                }

            });
            asyncTwitter.showUser(asyncTwitter.getId());
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void getContactsFromNet(List<mContact> cnts) {

    }

    @Override
    protected void getMessagesFromNet(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {

    }

    @Override
    protected void getDialogsFromNet(final int count, final int offset, final AsyncTaskCompleteListener<List<mDialog>> cb) {
        dlgs_thread_count += 1;

        asyncTwitter.addListener(new TwitterAdapter() {
            @Override
            public void gotHomeTimeline(ResponseList<Status> statuses) {
                List<mDialog> dlgs = new ArrayList<mDialog>();
                int i = 0;
                for (Status status : statuses) {
                    if(i < (offset % count)){
                        i++;
                        continue;
                    }

                    mDialog dlg = new mDialog();

                    dlg.participants.add( getContact(status.getUser().getScreenName()) );

                    //dlg.title;
                    dlg.snippet = status.getText();
                    //dlg.snippet_out = item.getInt( "out" );
                    long ms = status.getCreatedAt().getSeconds() * 1000;
                    dlg.last_msg_time.set(ms);
                    dlg.msg_service_type = MessageService.TW;

                    dlgs.add(dlg);
                    //Log.d("msTwitter", status.getUser().getName() + ":" + status.getText());
                }

                dlgs_thread_count--;
                if(cb != null) {
                    cb.onTaskComplete(dlgs);
                }
            }

        });

        int page = offset / count + 1;
        asyncTwitter.getHomeTimeline(new Paging(page, count));
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
