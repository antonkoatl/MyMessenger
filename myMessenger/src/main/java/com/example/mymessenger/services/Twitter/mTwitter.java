package com.example.mymessenger.services.Twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mGlobal;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MessageService;
import com.example.mymessenger.services.MessageService.SimpleOpenAuthActivity;

import java.util.ArrayList;
import java.util.List;

import twitter4j.PagableResponseList;
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

public class mTwitter extends MessageService {
    public static final String CALLBACK_URI = "https://mymessenger.callback";
    public static final String CANCEL_URI = "twitter://cancel";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SECRET_TOKEN = "secret_token";
    protected static String OAUTH_REQUEST_TOKEN = "https://api.twitter.com/oauth/request_token";
    protected static String OAUTH_ACCESS_TOKEN = "https://api.twitter.com/oauth/access_token";
    protected static String OAUTH_AUTHORIZE = "https://api.twitter.com/oauth/authorize";

    // Twitter oauth urls
    public static final String URL_TWITTER_AUTH = "auth_url";
    public static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    public static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    public static final int TWITTER_REQUEST_CODE = 1012;

    public static final int MAX_STATUSES_FROM_HOME_TIMELINE = 1000;

    // Очень долго проходят запросы, отдельный поток для этого:
    protected Handler handler1; //Для отложенного запроса данных о пользователях
    protected HandlerThread thread1 = new HandlerThread("MessageServiceThread");


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

    public mTwitter(MyApplication app) {
        super(app, TW, R.string.service_name_tw);

        thread1.start();
        handler1 = new Handler(thread1.getLooper());

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .setOAuthConsumerKey("vnZ85Cl5BvVxdUaPSP9sDy8TG")
                .setOAuthConsumerSecret("22fb2mPjzP7WoDr6TmSrubmF2svrQgG5Z6ZNmUfHWCKRmCrJRZ");

        factory = new TwitterFactory((configurationBuilder.build()));
        mTwitter = factory.getInstance();

    }

    @Override
    protected void setupDBHelper(){
        msDBHelper = MSDBHelper_tw.getInstance();
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



    double_auth da;

    @Override
    protected void onAuthorize(){
        super.onAuthorize();
        if(!isInitFinished()) onInitFinish();
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
    public void getContactsFromNet(final CntsDownloadsRequest req) {
        req.onStarted();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                long cursor = -1;
                PagableResponseList<User> users;
                List<mContact> cnts = new ArrayList<mContact>();
                int i = 0;
                do {
                    try {
                        users = mTwitter.getFriendsList(mTwitter.getId(), cursor, 100);
                    } catch (TwitterException e) {
                        handleTwitterException(e, this, req);
                        return;
                    }


                    for (User user : users) {
                        if(i < req.offset)continue;
                        if(i > (req.offset + req.count) )continue;
                        mContact cnt = new mContact(user.getScreenName());
                        cnt.name = user.getName();
                        cnt.icon_50_url = user.getProfileImageURL();
                        cnts.add(cnt);
                    }
                } while ( ((cursor = users.getNextCursor()) != 0) && (i < (req.offset + req.count)) );
                req.onFinished(cnts);
            }
        };

        handler1.post(r);
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
        String data[] = {"---", "New message", "All messages", "Status"};
        if(getActiveDialog() != null){
            data[0] = getActiveDialog().getParticipantsNames();
        }


        return data;
    }

    @Override
    public void MainViewMenu_click(int which, Context context) {
        switch(which) {
            case 0:
                openActiveDlg();
                break;
            case 1:
                openContacts(context);
                break;
            case 2:
                openDialogs();
                break;
        }
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

        //MyApplication.handler1.post(r);
        handler1.post(r);
    }

    @Override
    protected void getContactsDataFromNet(final CntsDataDownloadsRequest req) {
        req.onStarted();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                List<mContact> cnts = new ArrayList<mContact>();
                for(mContact cnt : req.cnts) {
                    User user = null;
                    try {
                        user = mTwitter.showUser(cnt.address);
                    } catch (TwitterException e) {
                        handleTwitterException(e, this, req);
                        continue;
                    }
                    cnt.name = user.getName();
                    cnt.icon_50_url = user.getProfileImageURL();
                    cnts.add(cnt);

                }
                req.onFinished(cnts);
            }
        };

        //MyApplication.handler1.post(r);
        handler1.post(r);
    }

    @Override
    protected void getMessagesFromNet(final MsgsDownloadsRequest req) {
        req.onStarted();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                int page = req.offset / req.count + 1;
                ResponseList<Status> statuses;

                try {
                    statuses = mTwitter.getUserTimeline(req.dlg.participants.get(0).address, new Paging(page, req.count));
                } catch (TwitterException e) {
                    handleTwitterException(e, this, req);
                    return;
                }

                List<mMessage> msgs = new ArrayList<mMessage>();

                int i = 0;
                for (Status status : statuses) {
                    if (i < (req.offset % req.count)) {
                        i++;
                        continue;
                    }

                    msgs.add( get_msg_from_status(status) );
                }

                req.onFinished(msgs);
            }
        };

        //MyApplication.handler1.post(r);
        handler1.post(r);
    }

    private mMessage get_msg_from_status(Status status){
        mMessage msg = new mMessage();

        msg.respondent = getContact( status.getUser().getScreenName() );
        msg.text = status.getText();
        msg.sendTime.set( status.getCreatedAt().getTime() );
        msg.id = String.valueOf( status.getId() );
        msg.msg_service = getServiceType();

        return  msg;
    }

    @Override
    protected void getDialogsFromNet(final DlgsDownloadsRequest req) {
        req.onStarted();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                if((req.offset + req.count) > MAX_STATUSES_FROM_HOME_TIMELINE){
                    req.onFinished(new ArrayList<mDialog>());
                    return;
                }
                int page = req.offset / req.count + 1;
                ResponseList<Status> statuses;

                try {
                    statuses = mTwitter.getHomeTimeline(new Paging(page, req.count));
                } catch (TwitterException e) {
                    handleTwitterException(e, this, req);
                    return;
                }

                List<mDialog> dlgs = new ArrayList<mDialog>();
                int i = 0;
                for (Status status : statuses) {
                    if (i < (req.offset % req.count)) {
                        i++;
                        continue;
                    }

                    mDialog dlg = new mDialog();

                    //dlg.chat_id = status.getId();
                    dlg.participants.add(getContact(status.getUser().getScreenName()));
                    dlg.title = status.getUser().getName();
                    dlg.setLastMsg(get_msg_from_status(status));
                    msDBHelper.updateOrInsertMsgById(dlg.last_msg_id, dlg.last_msg, dlg, mTwitter.this);
                    dlg.msg_service_type = MessageService.TW;

                    dlgs.add(dlg);
                    //Log.d("msTwitter", status.getUser().getName() + ":" + status.getText());
                }

                req.onFinished(dlgs);
            }
        };

        //MyApplication.handler1.post(r);
        handler1.post(r);
    }

    private void handleTwitterException(TwitterException e, Runnable runnable, DownloadsRequest req) {
        if(e.getMessage().equals("Received authentication challenge is null")){
            setNotAuthorised();
            if(MyApplication.getMainActivity() != null){
                authorize(MyApplication.getMainActivity());
            }
        } else {
            req.onError();
        }
        e.printStackTrace();
    }

    @Override
    protected void logout_from_net() {

    }











    @Override
    public String getEmojiUrl(long code) {
        String scode = mGlobal.LongToHexStr32nozero(code);
        if( code == 0x3020e3 || code == 0x3120e3 || code == 0x3220e3 || code == 0x3320e3 || code == 0x3420e3 ||
                code == 0x3520e3 || code == 0x3620e3 || code == 0x3720e3 || code == 0x3820e3 || code == 0x3920e3 || code == 0x2320e3){
            scode = scode.substring(0, 2) + "-" + scode.substring(2);
        }

        return "https://abs.twimg.com/emoji/v1/72x72/" + scode + ".png";
    }




    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, final Intent data){
        if (requestCode == TWITTER_REQUEST_CODE) {
            if (data != null) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    //Пользователь отменил (нажал назад)
                }
                if (resultCode == Activity.RESULT_OK) {
                    //Получен токен

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            da.getAccessToken( data.getStringExtra(URL_TWITTER_OAUTH_VERIFIER) );
                        }
                    };

                    MyApplication.handler1.post(r);
                }
            }

        }
    }

}
