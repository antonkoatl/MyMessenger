package com.example.mymessenger.services.Vk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.ChatMessageFormatter;
import com.example.mymessenger.DownloadService;
import com.example.mymessenger.MsgReceiver;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.UpdateService;
import com.example.mymessenger.download_waiter;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MessageService;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCaptchaDialog;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Vk extends MessageService {
    private static String sTokenKey = "VK_ACCESS_TOKEN";
    private static String[] sMyScope = new String[]{VKScope.FRIENDS, VKScope.WALL, VKScope.PHOTOS, VKScope.NOHTTPS, VKScope.MESSAGES};

    static final int MAX_REQUESTS_WAITING_FOR_AUTH = 10;

    private /**/List<VKRequest> requests_waiting_for_auth = new ArrayList<VKRequest>();


    public Vk(MyApplication app) {
        super(app, VK, R.string.service_name_vk);

        //Инициализация VkSdk
        VKSdk.initialize(sdkListener, "4161005", VKAccessToken.tokenFromSharedPreferences(this.msApp.getApplicationContext(), sTokenKey));
    }


    // Necessary methods

    @Override
    public void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb){
        VKRequest request = new VKRequest("messages.getLongPollServer", VKParameters.from(VKApiConst.COUNT, String.valueOf(1)));
        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();
        VKRequestListener rl = 	new VKRequestListenerWithCallback<RunnableAdvanced<?>>(cb, Vk.this) {

            @Override
            public void onComplete(VKResponse response) {
                Log.d("requestNewMessagesRunnable", "onComplete" );
                try {
                    JSONObject response_json = response.json.getJSONObject("response");


                    String key = response_json.getString( "key" );
                    String server = response_json.getString( "server" );
                    Integer ts = response_json.getInt( "ts" );

                    RunnableAdvanced<?> r = new LongPollRunnable(server, key, ts);
                    callback.onTaskComplete(r);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };


        request.executeWithListener(rl);
    }

    @Override
    public void authorize(Context acontext){
        if(msAuthorisationFinished){
            VKUIHelper.onResume((Activity) acontext);
            VKSdk.authorize(sMyScope, false, true);
            //VKUIHelper.onDestroy((Activity) acontext);
            msAuthorisationFinished = false;
        }
    }

    @Override
    protected void requestAccountInfoFromNet(final AsyncTaskCompleteListener<mContact> cb){
        VKRequest request = new VKRequest("users.get", VKParameters.from(VKApiConst.FIELDS, "photo_100"));
        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();

        VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    JSONArray response_json = response.json.getJSONArray("response");
                    if(response_json.length() > 0){
                        JSONObject item = response_json.getJSONObject(0);

                        mContact cnt = new mContact(item.getString("id"));

                        String name = item.getString("first_name");
                        name += " " + item.getString("last_name");

                        String photo_100_url = item.getString("photo_100");
                        cnt.icon_50_url = photo_100_url;
                        cnt.name = name;

                        cb.onTaskComplete(cnt);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };

        request.executeWithListener(rl);
    };

    @Override
    protected void getContactsFromNet(final CntsDownloadsRequest req) {
        req.onStarted();

        String uids = req.cnts.get(0).address;
        for(int i = 1; i < req.cnts.size(); i++){
            uids += "," + req.cnts.get(i).address;
        }

        VKRequest request = new VKRequest("users.get", VKParameters.from(VKApiConst.USER_IDS, uids, VKApiConst.FIELDS, "photo_50"));
        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();



        VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
            @Override
            public void onComplete(VKResponse response) {
                Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
                try {
                    JSONArray response_json = response.json.getJSONArray("response");

                    boolean updated = false;

                    for(int i = 0; i < response_json.length(); i++){
                        JSONObject item = response_json.getJSONObject(i);

                        mContact cnt = req.cnts.get(i);

                        String name = item.getString("first_name");
                        name += " " + item.getString("last_name");

                        String photo_50_url = item.getString("photo_50");

                        cnt.icon_50_url = photo_50_url;
                        cnt.name = name;

                        if(msDBHelper.updateCntInDB(cnt, Vk.this) == true)updated = true;
                    }

                    req.onFinished(updated);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }



        };

        request.executeWithListener(rl);

    }

    @Override
    public boolean sendMessage(String address, String text) {
        VKRequest request;

        long id = Long.valueOf(address);
        if(id > 2000000000){ //chat
            id -= 2000000000;
            request = new VKRequest("messages.send", VKParameters.from("chat_id", String.valueOf(id),
                    VKApiConst.MESSAGE, text));
        } else {
            request = new VKRequest("messages.send", VKParameters.from(VKApiConst.USER_ID, address,
                    VKApiConst.MESSAGE, text));
        }


        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();

        VKRequestListener rl = new VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                Log.d("VKRequestListener", "onComplete" );
            }

            @Override
            public void onError(VKError error) {
                Log.w("VKRequestListener.requestMessages", "onError " + error.errorMessage + ", " + error.apiError.errorMessage);
                if(error.apiError != null) HandleApiError(error);
                // Ошибка. Сообщаем пользователю об error.
            }
            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                Log.d("VKRequestListener", "attemptFailed" );
                // Неудачная попытка. В аргументах имеется номер попытки и общее их количество.
            }

        };

        request.executeWithListener(rl);
        return false;
    }

    @Override
    public String[] getStringsForMainViewMenu() {
        // TODO: всегда должен быть активный диалог
        String data[] = {"---", "New message", "All messages", "Status", "Wall", "Friend Wall", "News"};
        if(getActiveDialog() != null){
            data[0] = getActiveDialog().getParticipantsNames();
        }


        return data;
    }

    @Override
    public void MainViewMenu_click(int which, Context con) {
        Intent intent;
        switch(which) {
            case 0:
                openActiveDlg();
                break;
            case 1:
                openContacts(con);
                break;
            case 2:
                openDialogs();
                break;
        }
    }

    @Override
    public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb) {

        VKRequest request = new VKRequest("friends.get", VKParameters.from("order", "hints",
                VKApiConst.OFFSET, String.valueOf(offset), VKApiConst.COUNT, String.valueOf(count), VKApiConst.FIELDS, "photo_100"));

        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();

        VKRequestListener rl = new VKRequestListenerWithCallback<List<mContact>>(cb, Vk.this) {
            @Override
            public void onComplete(VKResponse response) {
                Log.d("requestContacts", "onComplete" );
                List<mContact> cnts = new ArrayList<mContact>();
                try {
                    JSONObject response_json = response.json.getJSONObject("response");
                    JSONArray items = response_json.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);

                        mContact cnt;
                        if(msContacts.get(item.getString("id")) == null){
                            cnt = new mContact(item.getString("id"));

                            String name = item.getString("first_name");
                            name += " " + item.getString("last_name");

                            cnt.name = name;

                            String photo_100_url = item.getString("photo_100");

                            Intent intent = new Intent(msApp.getApplicationContext(), DownloadService.class);
                            intent.putExtra("url", photo_100_url);
                            msApp.getApplicationContext().startService(intent);

                            download_waiter tw = new download_waiter(photo_100_url){
                                mContact cnt;

                                @Override
                                public void onDownloadComplete() {
                                    cnt.icon_50 = BitmapFactory.decodeFile(filepath);
                                }

                                public download_waiter setParams(mContact cnt){
                                    this.cnt = cnt;
                                    return this;
                                }

                            }.setParams(cnt);
                            msApp.dl_waiters.add(tw);

                            msContacts.put(item.getString("id"), cnt);
                        }
                        cnt = msContacts.get(item.getString("id"));

                        cnt.online = item.getInt("online") == 1 ? true : false;

                        cnts.add(cnt);
                    }
                    callback.onTaskComplete(cnts);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };

        request.executeWithListener(rl);

    }

    @Override
    public void unsetup() {
        VKSdk.logout();

        Intent intent = new Intent(msApp.getApplicationContext(), UpdateService.class);
        intent.putExtra("specific_service", getServiceType());
        intent.putExtra("remove", true);
        msApp.startService(intent);
    }

    @Override
    public void requestMarkAsReaded(mMessage msg, final mDialog dlg){
        VKRequest request = new VKRequest("messages.markAsRead", VKParameters.from("message_ids", msg.id, VKApiConst.USER_ID, msg.respondent.address));

        msg.setFlag(mMessage.LOADING, true);

        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();

        final mMessage tmsg = msg;

        VKRequestListener rl = new VKRequestListenerWithCallback<Void>(null, Vk.this) {
            @Override
            public void onComplete(VKResponse response) {
                Log.d("requestMarkAsReaded", "onComplete" );
                try {
                    int resp = response.json.getInt("response");
                    if(resp == 1)tmsg.setFlag(mMessage.READED, true);
                    tmsg.setFlag(mMessage.LOADING, false);
                    msApp.triggerMsgUpdaters(tmsg, dlg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };

        request.executeWithListener(rl);
    }

    @Override
    protected void getDialogsFromNet(final DlgsDownloadsRequest req) {
        // Обновление информации о количестве потоков загрузки
        req.onStarted();

        // Скачивание из интернета
        VKRequest request = new VKRequest("messages.getDialogs", VKParameters.from(VKApiConst.COUNT, String.valueOf(req.count),
                VKApiConst.OFFSET, String.valueOf(req.offset),
                VKApiConst.FIELDS, "first_name,last_name,photo_50"));
        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();

        VKRequestListener rl = new VKRequestListenerWithCallback<List<mDialog>>(req.cb, Vk.this) {

            @Override
            public void onComplete(VKResponse response) {
                Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
                int count = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.COUNT) );
                int offset = Integer.valueOf( (String) response.request.getMethodParameters().get( VKApiConst.OFFSET) );
                List<mDialog> dlgs = new ArrayList<mDialog>();

                try {
                    JSONObject response_json = response.json.getJSONObject("response");
                    JSONArray items = response_json.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i).getJSONObject("message");

                        mDialog mdl = new mDialog();

                        if(item.has("chat_id")){
                            mdl.chat_id = item.getLong("chat_id");
                            JSONArray ids = item.getJSONArray("chat_active");
                            for(int i1 = 0; i1 < ids.length(); i1++){
                                mdl.participants.add( getContact( ids.getString(i1) ) );
                            }
                        } else {
                            mdl.participants.add( getContact( item.getString( "user_id" ) ) );
                        }

                        mdl.title = item.getString("title");
                        mdl.snippet = item.getString( "body" );
                        mdl.snippet_out = item.getInt( "out" );
                        mdl.last_msg_time.set(item.getLong("date")*1000);
                        mdl.msg_service_type = MessageService.VK;

                        dlgs.add(mdl);
                    }

                    req.onFinished(dlgs);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };

        request.executeWithListener(rl);
    }

    @Override
    protected void logout_from_net() {
        try{
            VKSdk.logout();
        } catch (Exception ex) {
            Log.d("logout_from_net", Log.getStackTraceString(ex));
        }
    }

    @Override
    protected void getMessagesFromNet(final MsgsDownloadsRequest req) {
        // Обновление информации о количестве потоков загрузки
        req.onStarted();

        VKRequest request;

        if(req.dlg.chat_id == 0){
            request = new VKRequest("messages.getHistory", VKParameters.from(VKApiConst.COUNT, String.valueOf(req.count),
                    VKApiConst.OFFSET, String.valueOf(req.offset), VKApiConst.USER_ID, req.dlg.getParticipants()));
        } else {
            request = new VKRequest("messages.getHistory", VKParameters.from(VKApiConst.COUNT, String.valueOf(req.count),
                    VKApiConst.OFFSET, String.valueOf(req.offset), "chat_id", String.valueOf(req.dlg.chat_id)));
        }
        request.secure = false;
        VKParameters preparedParameters = request.getPreparedParameters();

        VKRequestListener rl = new VKRequestListenerWithCallback<List<mMessage>>(req.cb, Vk.this) {
            @Override
            public void onComplete(VKResponse response) {
                Log.d("VKRequestListener", response.request.methodName +  " :: onComplete");
                List<mMessage> msgs = new ArrayList<mMessage>();
                boolean all_new = true;
                try {
                    JSONObject response_json = response.json.getJSONObject("response");
                    JSONArray items = response_json.getJSONArray("items");

                    mDialog dlg = (mDialog) params.get(0);

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);

                        mMessage msg = new mMessage();
                        msg.setFlag(mMessage.OUT, item.getInt("out") == 1 ?	true : false);

                        msg.respondent = getContact( item.getString( "user_id" ) );
                        msg.text = item.getString( "body" );
                        msg.sendTime.set(item.getLong( "date" )*1000);
                        msg.setFlag(mMessage.READED, item.getInt( "read_state" ) == 1 ? true : false);
                        msg.id = item.getString("id");
                        msg.msg_service = getServiceType();


                        msgs.add(msg);
                    }


                    Log.d("requestMessages", "onTaskComplete - net :: " + String.valueOf(isLoadingMsgsForDlg(dlg)));
                    req.onFinished(msgs);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };

        ((VKRequestListenerWithCallback<List<mMessage>>) rl).setParams(req.dlg);
        request.executeWithListener(rl);

    }


    @Override
    public String getEmojiUrl(long code){
        char[] chars = ChatMessageFormatter.charsFromLong(code).toCharArray();
        String res = "";
        for(char c : chars){
            res += String.format("%04X", (int) c);
        }
        return "http://vk.com/images/emoji/" + res + ".png";
    }
















    private VKSdkListener sdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
            Log.d("VKSdkListener", "onCaptchaError" );
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
            Log.d("VKSdkListener", "onTokenExpired" );
            if(msApp.getMainActivity() != null) authorize(msApp.getMainActivity());
            //VKSdk.authorize(sMyScope, false, false);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
            msAuthorisationFinished = true;
            Log.d("VKSdkListener", "onAccessDenied" );
            /*new AlertDialog.Builder(msApp.getApplicationContext())
                    .setMessage(authorizationError.errorMessage)
                    .show();*/
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
            Log.d("VKSdkListener", "onReceiveNewToken " + newToken.accessToken + " :: " + sTokenKey + " :: " + VKSdk.getAccessToken().accessToken);
            newToken.saveTokenToSharedPreferences(msApp.getApplicationContext(), sTokenKey);
            onAuthorize();
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
            Log.d("VKSdkListener", "onAcceptUserToken" );
            onAuthorize();
        }
    };

    @Override
    protected void onAuthorize(){
        super.onAuthorize();

        if(!isInitFinished()) onInitFinish();
        execRequestsWaitingForAuth();
    }

    public void HandleApiError(VKError error){
        Log.d("HandleApiError", String.valueOf(error.apiError.errorCode) + " :: " + error.apiError.errorMessage + " :: " + error.apiError.requestParams.toString());
        if(error.apiError.errorCode == 5){ // User authorization failed.
            if(msAuthorisationFinished && check_access_toten(error) ){
                //Log.d("HandleApiError", "VKSdk.authorize: " + error.apiError.errorMessage);
                if(msApp.getMainActivity() != null) authorize(msApp.getMainActivity());
            }

            addRequestWaitingForAuth(error.request);


        }

        if(error.apiError.errorCode == 6){ // Too many requests per second.
            Runnable r = new RunnableAdvanced<VKError>(){

                @Override
                public void run() {
                    param.request.repeat();
                }

            }.setParam(error);

            msHandler.postDelayed(r, 5000);

        }
    }

    private boolean check_access_toten(VKError error) {
        for(Map<String, String> m : error.apiError.requestParams){
            if(m.get("key").equals(VKApiConst.ACCESS_TOKEN)){
                Log.d("check_access_toten", m.get("value") + " :: " + VKSdk.getAccessToken().accessToken);
                return m.get("value").equals(VKSdk.getAccessToken().accessToken);
            }
        }
        return false;
    }

    class LongPollRunnable extends RunnableAdvanced<Void> {
        String server;
        String key;
        Integer ts;

        LongPollRunnable(String server, String key, Integer ts) {
            this.server = server;
            this.key = key;
            this.ts = ts;
        }

        @Override
        public void run_iteration() {
            Log.d("LongPollRunnable", "start");
            try {
                URL url = new URL("http://"+server+"?act=a_check&key="+key+"&ts="+ts.toString()+"&wait=25&mode=2");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //new LongPoll_async().execute(con.getInputStream());
                BufferedReader reader = null;
                String line = "";
                String page = "";
                try {
                    reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        page += line;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                Log.d("LongPollRunnable", page);

                JSONObject response_json = new JSONObject(page);

                if(response_json.has("failed")){
                    kill();
                    return;
                }

                ts = response_json.getInt( "ts" );

                JSONArray updates = response_json.getJSONArray("updates");
                for (int i = 0; i < updates.length(); i++) {
                    JSONArray item = updates.getJSONArray(i);

                    if(item.getInt(0) == 1) { // 1,$message_id,$flags -- замена флагов сообщения (FLAGS:=$flags)
                        int message_id = item.getInt(1);
                        int flags = item.getInt(2);

                        Intent intent = new Intent(MsgReceiver.ACTION_UPDATE);
                        intent.putExtra("msg_flags", flags);
                        intent.putExtra("msg_id", message_id);
                        intent.putExtra("msg_mode", MsgReceiver.UPDATE_REPLACE);
                        intent.putExtra("service_type", getServiceType());
                        msApp.sendBroadcast(intent);
                    }

                    if(item.getInt(0) == 2) { // 2,$message_id,$mask[,$user_id] -- установка флагов сообщения (FLAGS|=$mask)
                        int message_id = item.getInt(1);
                        int flags = item.getInt(2);

                        Intent intent = new Intent(MsgReceiver.ACTION_UPDATE);
                        intent.putExtra("msg_flags", flags);
                        intent.putExtra("msg_id", message_id);
                        intent.putExtra("msg_mode", MsgReceiver.UPDATE_INSTALL);
                        intent.putExtra("service_type", getServiceType());
                        msApp.sendBroadcast(intent);
                    }

                    if(item.getInt(0) == 3) { // 3,$message_id,$mask[,$user_id] -- сброс флагов сообщения (FLAGS&=~$mask)
                        int message_id = item.getInt(1);
                        int flags = item.getInt(2);

                        Intent intent = new Intent(MsgReceiver.ACTION_UPDATE);
                        intent.putExtra("msg_flags", flags);
                        intent.putExtra("msg_id", message_id);
                        intent.putExtra("msg_mode", MsgReceiver.UPDATE_RESET);
                        intent.putExtra("service_type", getServiceType());
                        msApp.sendBroadcast(intent);

                    }

                    if (item.getInt(0) == 4) { // 4,$message_id,$flags,$from_id,$timestamp,$subject,$text,$attachments -- добавление нового сообщения
                        String msg_id = item.getString(1);
                        int flags = item.getInt(2);
                        String from_id = item.getString(3);
                        long timestamp = item.getInt(4);
                        String subject = item.getString(5);
                        String text = item.getString(6);
                        JSONObject attachments = item.getJSONObject(7);

                        if(attachments.has("from")){ //chat
                            long chat_id = Long.valueOf(from_id) - 2000000000; //hint
                            mMessage msg = new mMessage();
                            msg.respondent = getContact( attachments.getString("from") );
                            msg.setOut((flags & 2) == 2);
                            msg.text = text;
                            msg.sendTime.set(timestamp*1000);
                            msg.id = msg_id;
                            msg.msg_service = getServiceType();

                            Intent intent = new Intent(MsgReceiver.ACTION_RECEIVE);
                            intent.putExtra("msg", msg);
                            intent.putExtra("chat_id", chat_id);
                            msApp.sendBroadcast(intent);
                        } else {
                            mMessage msg = new mMessage();
                            msg.respondent = getContact( from_id );
                            msg.setOut((flags & 2) == 2);
                            msg.text = text;
                            msg.sendTime.set(timestamp*1000);
                            msg.id = msg_id;
                            msg.msg_service = getServiceType();

                            Intent intent = new Intent(MsgReceiver.ACTION_RECEIVE);
                            intent.putExtra("msg", msg);
                            msApp.sendBroadcast(intent);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void addRequestWaitingForAuth(VKRequest r){
        if(requests_waiting_for_auth.size() < MAX_REQUESTS_WAITING_FOR_AUTH){
            requests_waiting_for_auth.add(r);
            Log.d("addRequestWaitingForAuth", "request added to requests_waiting_for_auth: " + r.methodName);
        } else {
            Log.d("addRequestWaitingForAuth", "request not added to requests_waiting_for_auth, dropped: " + r.methodName);
        }
    }

    private void execRequestsWaitingForAuth(){
        VKRequest r_new = null;
        if(requests_waiting_for_auth.size() > 0){
            r_new = new VKRequest(requests_waiting_for_auth.get(0).methodName, requests_waiting_for_auth.get(0).getMethodParameters());
            r_new.executeWithListener(requests_waiting_for_auth.get(0).requestListener);
            Log.d("addRequestWaitingForAuth", "Executing request from requests_waiting_for_auth: " + r_new.methodName);
        }
        for(int i = 1; i < requests_waiting_for_auth.size(); i++){
            VKRequest r_new_t = new VKRequest(requests_waiting_for_auth.get(i).methodName, requests_waiting_for_auth.get(i).getMethodParameters());
            r_new_t.executeAfterRequest(r_new, requests_waiting_for_auth.get(i).requestListener);
            Log.d("addRequestWaitingForAuth", "Executing request from requests_waiting_for_auth: " + r_new_t.methodName);
            r_new = r_new_t;
        }
        requests_waiting_for_auth.clear();
    }











}