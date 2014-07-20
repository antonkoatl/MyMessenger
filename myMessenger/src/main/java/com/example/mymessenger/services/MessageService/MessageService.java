package com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.mymessenger.ActivityTwo;
import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.ChatMessageFormatter;
import com.example.mymessenger.DBHelper;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.R;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.UpdateService;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mGlobal.IntegerMutable;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.Facebook.mFacebook;
import com.example.mymessenger.services.Sms.Sms;
import com.example.mymessenger.services.Twitter.mTwitter;
import com.example.mymessenger.services.Vk.Vk;
import com.example.mymessenger.ui.ServicesMenuFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MessageService implements msInterfaceMS, msInterfaceDB, msInterfaceUI, msInterfaceEM {
    public static final int SMS = 10;
    public static final int VK = 11;
    public static final int TW = 12;
    public static final int FB = 13;

    public static final int MSGS_DOWNLOAD_COUNT = 20;
    public static final int DLGS_DOWNLOAD_COUNT = 20;
    public static final int CNTS_REQUEST_ACCUM_TIME = 1000;

    protected static final String PREFS_ACTIVE_ACCOUNT = "active_account";

    protected final static Handler msHandler; //Для отложенного запроса данных о пользователях
    protected final static HandlerThread msThread = new HandlerThread("MessageServiceThread");


    protected MyApplication msApp;
    protected mContact msSelfContact;
    protected mDialog msActiveDialog;
    protected Map<String, mContact> msContacts;
    protected String msServiceName;

    protected boolean ms_accum_cnt_handler_isRunning = false;
    protected List<mContact> msAccumCnts;

    protected boolean msAuthorised = false;
    protected boolean msAuthorisationFinished = true;
    protected int msServiceType;

    private boolean dl_all_dlgs_downloaded = false; //Все диалоги загружены из сети
    protected int dlgs_thread_count = 0; //Количество потоков, загружающих диалоги в данных момент
    protected Map<mDialog, IntegerMutable> msgs_thread_count; //Количество потоков, загружающих сообщения для определённого диалога в данных момент
    protected SharedPreferences sPref;

    boolean msIsSetupFinished = true;
    protected int msSetupStage;
    protected AsyncTaskCompleteListener<MessageService> cbms_for_setup = null;

    public MSDBHelper msDBHelper;

    /*
         Контакт активного пользователь
         Авторизация
         Обновление сообщений
     */
    boolean msIsInitFinished = false;


    static {
        msThread.start();
        msHandler = new Handler(msThread.getLooper());
    }

    protected MessageService(MyApplication app, int ser_type, int ser_name_id) {
        this.msApp = app;
        setupDBHelper();
/**/        msContacts = new HashMap<String, mContact>();
        msgs_thread_count = new HashMap<mDialog, IntegerMutable>(); //индикаторы загрузки сообщений для диалогов

        msAccumCnts = new ArrayList<mContact>();


        msServiceName = msApp.getString(ser_name_id);
        msServiceType = ser_type;

        sPref = app.getSharedPreferences(String.valueOf(msServiceType), Context.MODE_PRIVATE); //загрузка конфигов

        msSelfContact = new mContact(sPref.getString(PREFS_ACTIVE_ACCOUNT, ""));

        setupEmoji();
    }

    protected void setupDBHelper(){
        msDBHelper = MSDBHelper.getInstance();
    }

    @Override
    public void init(){
        onInitFinish();
    }


    //Запросить данные - может быть возвращено 2 раза (из бд, затем из интернета)
	/* Если данные загружаются из интернета, то алгоритм таков:
	 * 1. Возвращаются данные из бд
	 * 2. Загружаются данные из интернета
	 * 3. Проверка являются ли загруженные данные новыми
	 * 4. Возвращение новых данных, обновление бд
	 * 5. Если все загруженные данные - новые, то продолжить обновление пока количество обновлённых данных не достигнет количества данных в бд
	 *  * При дополнительной загрузке данных для обновления данные не возвращаются, только обновляется бд
	 */

    public abstract void authorize(Context context);

    // Логаут
    public void logout() {
        msAuthorised = false;
    }

    @Override
    public final void setup(AsyncTaskCompleteListener<MessageService> asms) {
        msIsSetupFinished = false;
        msSetupStage = 1;
        cbms_for_setup = asms;
        setupStages();
    }

    @Override
    public abstract void unsetup();

    // Обновить информацию об аккаунте
    public void requestAccountInfo() {
        requestAccountInfoFromNet(self_contact_cb);
    }


    @Override
    public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb){
        if(isOnline()) getContactsFromNet(new CntsDownloadsRequest(count, offset, cb));
    }

    @Override
    public final void requestContactData(mContact cnt) {
        msAccumCnts.add(cnt);

        if (!ms_accum_cnt_handler_isRunning) {
            ms_accum_cnt_handler_isRunning = true;

            msHandler.postDelayed(cnts_request_runnable, CNTS_REQUEST_ACCUM_TIME);
        }
    }

    @Override
    public final void requestContactsData(List<mContact> cnts) {
        msDBHelper.getContactsFromDB(cnts, this);
        if(isOnline()) getContactsDataFromNet(new CntsDataDownloadsRequest(cnts));
    }

    // Запросить активный диалог. Загружается из памяти, если нет - из интернета последний
    public void requestActiveDlg() {
        //if(getActiveDialog() != null)return;
        int dialog_id = sPref.getInt("active_dialog", 0);
        if(dialog_id > 0){
            mDialog dlg = msApp.dbHelper.getDlgById(dialog_id, this);
            if(dlg != null){
                setActiveDialog(dlg);
                return;
            }
        }

        final AsyncTaskCompleteListener<List<mDialog>> acb = new AsyncTaskCompleteListener<List<mDialog>>(){

            @Override
            public void onTaskComplete(List<mDialog> result) {
                if(result.size() > 0)setActiveDialog(result.get(0));
            }

        };

        requestDialogs(1, 0, acb);
    }

    @Override
    public final void requestDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
        if(!msIsSetupFinished)return; // TODO: отложить, тоже самое для авторизации
        int dlgs_in_db = msApp.dbHelper.getDlgsCount(MessageService.this);

        if (offset + count < dlgs_in_db) {
            msDBHelper.getDialogsFromDB(count, offset, cb, this);
            if(isOnline()) refreshDialogsFromNet(cb, 0);
        } else {
            msDBHelper.getDialogsFromDB(count, offset, cb, this);
            if(isOnline()) {
                msUpdateDlgsDB_cb up_cb = new msUpdateDlgsDB_cb(cb);
                getDialogsFromNet(new DlgsDownloadsRequest(count, offset, up_cb));
            }

        }
    }

    // TODO: Пересмотреть
    @Override
    public final void refreshDialogsFromNet(AsyncTaskCompleteListener<List<mDialog>> cb, int count) {
        msRefreshDlgsCb.addRefresh(cb, count);
    }

    @Override
    public final void requestMessages(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
        int msgs_in_db = msApp.dbHelper.getMsgsCount(dlg, MessageService.this);

        if (offset + count < msgs_in_db) {
            msDBHelper.getMessagesFromDB(dlg, count, offset, cb, this);
            if(isOnline()) refreshMessagesFromNet(dlg, cb, 0);
        } else {
            msDBHelper.getMessagesFromDB(dlg, count, offset, cb, this);
            if(isOnline()) {
                msUpdateMsgsDB_cb up_cb = new msUpdateMsgsDB_cb(dlg, cb);
                getMessagesFromNet(new MsgsDownloadsRequest(dlg, count, offset, up_cb));
            }

        }
    }

    // TODO: Пересмотреть
    public final void refreshMessagesFromNet(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb, int count) {
        msRefreshMsgsCb.addRefresh(dlg, cb, count);
    }

    // TODO: Периодически обновлять
    @Override
    public final mContact getContact(String address) {
        mContact cnt = msContacts.get(address);

        if (cnt == null) {
            cnt = new mContact(address);
            requestContactData(cnt);
            msContacts.put(address, cnt);
        }

        return cnt;
    }

    @Override
    public final mDialog getDialog(mContact cnt) {
        // Есть ли в базе? - Загрузить : Создасть. Обновить
        mDialog dlg = msApp.dbHelper.getDlg(cnt.address, this);

        if (dlg == null) {
            dlg = new mDialog(cnt);
            msApp.dbHelper.insertDlg(dlg, this);
            refreshMessagesFromNet(dlg, null, 0);
        }

        return dlg;
    }



    // TODO: Организовать работу в UpdateService через ThreadPool
    @Override
    public abstract void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb);

    @Override
    public final String getServiceName() {
        return msServiceName;
    }

    @Override
    public final int getServiceType() {
        return msServiceType;
    }

    @Override
    public final mDialog getActiveDialog() {
        return msActiveDialog;
    }

    @Override
    public final mContact getMyContact() {
        return msSelfContact;
    }


    public final void setActiveDialog(mDialog dlg) {
        msActiveDialog = dlg;
        msApp.sPref.edit().putInt("active_dialog", msApp.dbHelper.getDlgId(dlg, this)).commit();
    }

    @Override
    public final boolean isLoadingDlgs() {
        return dlgs_thread_count > 0;
    }

    // Загружаются ли сообщения для данного диалога
    public final boolean isLoadingMsgsForDlg(mDialog dlg) {
        IntegerMutable count = msgs_thread_count.get(dlg);
        if (count == null) return false;
        if (count.value == 0) {
            msgs_thread_count.remove(dlg);
            return false;
        }
        return true;
    }


























    // Получение информации о текущем аккауте
    protected abstract void requestAccountInfoFromNet(final AsyncTaskCompleteListener<mContact> cb);




    // Загрузка данных контактов из интернета, после завершения должно проверятся msAccumCnts. Если что то обновилось - вызываться триггеры
    protected abstract void getContactsDataFromNet(final CntsDataDownloadsRequest req);

    protected abstract void getContactsFromNet(final CntsDownloadsRequest req);

    // Загрузка сообщений из бд, выполняется асинхронно


    // Загрузка сообщений из интернета
    protected abstract void getMessagesFromNet(MsgsDownloadsRequest req);



    // Загрузка диалогов из интернета
    protected abstract void getDialogsFromNet(DlgsDownloadsRequest req);

    // Обновить информацию о потоках загрузки сообщений
    protected final void updateMsgsThreadCount(mDialog dlg, int count) {
        IntegerMutable lm_count = msgs_thread_count.get(dlg);
        if (lm_count == null) {
            lm_count = new IntegerMutable(count);
            msgs_thread_count.put(dlg, lm_count);
        } else lm_count.value += count;
    }

    @Override
    public boolean isLoading() {
        return !(msIsSetupFinished && msIsInitFinished);
    }





    protected final Runnable cnts_request_runnable = new Runnable() {

        @Override
        public void run() {
            List<mContact> cnt_temp = new ArrayList<mContact>(msAccumCnts);

            if (cnt_temp.size() == 0) {
                Log.e("cnts_request_runnable", "error");
            }

            msAccumCnts.clear();

            requestContactsData(cnt_temp);
        }
    };

    // Логаут
    protected abstract void logout_from_net();







    // Поэтапная настройка сервиса
    protected void setupStages() {
        switch (msSetupStage) {
            case 1:
                logout();
                authorize(MyApplication.getMainActivity());
                break;
            case 2:
                requestAccountInfo();
                break;
            case 3:
                msApp.dbHelper.createTables(this);
                requestActiveDlg();

                // Обновления
                Intent intent = new Intent(msApp.getApplicationContext(), UpdateService.class);
                intent.putExtra("specific_service", getServiceType());
                msApp.startService(intent);

                msSetupStage++;
                setupStages();
                break;
            case 4:
                msIsSetupFinished = true;
                if (cbms_for_setup != null) cbms_for_setup.onTaskComplete(this);
                init();
                break;
        }
    }














    // TODO: Что это, зачем это?
    public final void refresh() { //сбросить все индикаторы завершения загрузок
        dl_all_dlgs_downloaded = false;
    }


    protected class msUpdateDlgsDB_cb implements AsyncTaskCompleteListener<List<mDialog>> {
        AsyncTaskCompleteListener<List<mDialog>> cb;

        public msUpdateDlgsDB_cb(AsyncTaskCompleteListener<List<mDialog>> cb) {
            this.cb = cb;
        }

        @Override
        public void onTaskComplete(List<mDialog> result) {
            SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();
            String my_table_name = msApp.dbHelper.getTableNameDlgs(MessageService.this);
            List<mDialog> dlgs = new ArrayList<mDialog>();

            for (mDialog mdl : result) {
                int dlg_key = msApp.dbHelper.getDlgId(mdl, MessageService.this);

                if(dlg_key != 0){
                    mDialog dlg_in_db = msApp.dbHelper.getDlgById(dlg_key, MessageService.this);
                    if (mdl.last_msg_time.after(dlg_in_db.getLastMessageTime())) {
                        // Update
                        msApp.dbHelper.updateDlg(dlg_key, mdl, MessageService.this);
                        dlgs.add(mdl);
                    } else {
                        // Not Update
                        continue;
                    }

                } else {
                    // Add
                    msApp.dbHelper.insertDlg(mdl, MessageService.this);
                    dlgs.add(mdl);
                }

            }

            if (cb != null) {
                cb.onTaskComplete(dlgs);
            }
        }

    }

    protected class msUpdateMsgsDB_cb implements AsyncTaskCompleteListener<List<mMessage>> {
        AsyncTaskCompleteListener<List<mMessage>> cb;
        mDialog dlg;

        public msUpdateMsgsDB_cb(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb) {
            this.cb = cb;
            this.dlg = dlg;
        }

        @Override
        public void onTaskComplete(List<mMessage> result) {

            result = msApp.dbHelper.update_db_msgs(result, MessageService.this, dlg);

            if (cb != null) {
                cb.onTaskComplete(result);
            }
        }

    }


    protected class msRefreshDlgs_cb implements AsyncTaskCompleteListener<List<mDialog>> {
        int count;
        int max_count;
        int offset;
        boolean running = false;
        List<mDialog> update_dlgs = new ArrayList<mDialog>();
        List<AsyncTaskCompleteListener<List<mDialog>>> update_cbs = new ArrayList<AsyncTaskCompleteListener<List<mDialog>>>(0);

        @Override
        public void onTaskComplete(List<mDialog> result) {
            SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();
            String my_table_name = msApp.dbHelper.getTableNameDlgs(MessageService.this);
            boolean all_new = true;

            if (result.size() < count) dl_all_dlgs_downloaded = true;

            int res_size = result.size();

            result = msApp.dbHelper.updateDlgs(result, MessageService.this);



            if (result.size() > 0 && result.size() == res_size) {
                int dlgs_to_update = msApp.dbHelper.getDlgsCount(MessageService.this) - (offset + count);
                if (max_count > 0 && (offset + dlgs_to_update) > max_count) {
                    dlgs_to_update = max_count - offset;
                }

                if (dlgs_to_update > 0) {
                    offset = offset + count;

                    if (dlgs_to_update > DLGS_DOWNLOAD_COUNT) {
                        count = DLGS_DOWNLOAD_COUNT;
                    } else {
                        count = dlgs_to_update;
                    }

                    getDialogsFromNet(new DlgsDownloadsRequest(count, offset, this));
                } else {
                    run_cbs();
                }
            } else {
                run_cbs();
            }

        }

        private void run_cbs() {
            for (AsyncTaskCompleteListener<List<mDialog>> cb : update_cbs) {
                if (cb != null) cb.onTaskComplete(update_dlgs);
            }
            update_cbs.clear();
            update_dlgs = new ArrayList<mDialog>();
            running = false;
        }

        public void addRefresh(AsyncTaskCompleteListener<List<mDialog>> cb, int count) {
            update_cbs.add(cb);
            if (!running) {
                running = true;
                max_count = count;
                this.count = DLGS_DOWNLOAD_COUNT;
                offset = 0;
                getDialogsFromNet(new DlgsDownloadsRequest(this.count, this.offset, this));
            }
        }

    };

    protected class msRefreshMsgs_cb implements AsyncTaskCompleteListener<List<mMessage>> {
        class Params {
            int count;
            int max_count;
            int offset;
            List<mMessage> update_msgs = new ArrayList<mMessage>();
            List<AsyncTaskCompleteListener<List<mMessage>>> update_cbs = new ArrayList<AsyncTaskCompleteListener<List<mMessage>>>();
            mDialog dlg;
        }

        boolean running = false;
        List<Params> Psets = new ArrayList<Params>();


        @Override
        public void onTaskComplete(List<mMessage> result) {
            SQLiteDatabase db = msApp.dbHelper.getReadableDatabase();
            String my_table_name = msApp.dbHelper.getTableNameMsgs(MessageService.this);
            boolean all_new = true;

            Params cp = Psets.get(0);

            //if (result.size() < cp.count) dl_all_msgs_downloaded = true;
            int dlg_key = msApp.dbHelper.getDlgId(cp.dlg, MessageService.this);

            for (mMessage msg : result) {
                String selection = DBHelper.colDlgkey + " = ? AND " + DBHelper.colSendtime + " = ? AND " + DBHelper.colBody + " = ?";
                String[] selectionArgs = {String.valueOf(dlg_key), String.valueOf(msg.sendTime.toMillis(false)), msg.text};
                Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);

                if (c.moveToFirst()) {
                    int flags_in_db = c.getInt(c.getColumnIndex(DBHelper.colFlags));

                    if (msg.flags != flags_in_db) {
                        //update
                        int id = c.getInt(c.getColumnIndex(DBHelper.colId));
                        c.close();

                        msApp.dbHelper.updateMsg(id, msg, MessageService.this);

                        cp.update_msgs.add(msg);
                    } else {
                        //not update
                        c.close();
                        all_new = false;
                        continue;
                    }
                } else {
                    //add
                    c.close();

                    msApp.dbHelper.insertMsg(msg, my_table_name, dlg_key);

                    cp.update_msgs.add(msg);
                }
            }

            if (all_new && result.size() > 0) {
                int msgs_to_update = msApp.dbHelper.getMsgsCount(dlg_key, MessageService.this) - (cp.offset + cp.count);
                if (cp.max_count > 0 && (cp.offset + msgs_to_update) > cp.max_count) {
                    msgs_to_update = cp.max_count - cp.offset;
                }

                cp.offset = cp.offset + cp.count;
                if (msgs_to_update > 0) {
                    if (msgs_to_update > MSGS_DOWNLOAD_COUNT) {
                        cp.count = MSGS_DOWNLOAD_COUNT;
                    } else {
                        cp.count = msgs_to_update;
                    }

                    getMessagesFromNet(new MsgsDownloadsRequest(cp.dlg, cp.count, cp.offset, this));
                } else {
                    run_cbs();
                }
            } else {
                run_cbs();
            }

        }

        private void run_cbs() {
            Params cp = Psets.remove(0);
            for (AsyncTaskCompleteListener<List<mMessage>> cb : cp.update_cbs) {
                if (cb != null) cb.onTaskComplete(cp.update_msgs);
            }

            if (Psets.size() == 0) {
                running = false;
            } else {
                cp = Psets.get(0);
                getMessagesFromNet(new MsgsDownloadsRequest(cp.dlg, cp.count, cp.offset, this));
            }
        }

        public void addRefresh(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb, int count) {
            Params np = null;
            for (Params pp : Psets) {
                if (pp.dlg.equals(dlg)) {
                    np = pp;
                    break;
                }
            }
            if (np == null) {
                np = new Params();
                np.dlg = dlg;
                np.max_count = count;
                np.update_cbs.add(cb);
                np.count = MSGS_DOWNLOAD_COUNT;
                np.offset = 0;
                Psets.add(np);
            } else {
                np.update_cbs.add(cb);
            }

            if (!running) {
                running = true;
                getMessagesFromNet(new MsgsDownloadsRequest(np.dlg, np.count, np.offset, this));
            }
        }

    };



    msRefreshDlgs_cb msRefreshDlgsCb = new msRefreshDlgs_cb();
    msRefreshMsgs_cb msRefreshMsgsCb = new msRefreshMsgs_cb();


    AsyncTaskCompleteListener<mContact> self_contact_cb = new AsyncTaskCompleteListener<mContact>() {

        @Override
        public void onTaskComplete(mContact result) {
            if (msSelfContact == null) msSelfContact = result;
            else msSelfContact.update(result);

            SharedPreferences.Editor ed = sPref.edit();
            ed.putString(PREFS_ACTIVE_ACCOUNT, msSelfContact.address);
            ed.commit();

            if (!msIsSetupFinished) {
                msSetupStage++;
                setupStages();
            }
        }
    };






























    public final MyApplication getMsApp(){
        return this.msApp;
    }

    public final boolean isInitFinished(){
        return this.msIsInitFinished;
    }























    // Emoji
    public static long[][] emoji;
    public static int[] emoji_group_icons = new int[]{R.drawable.ic_emoji_smile, R.drawable.ic_emoji_flower, R.drawable.ic_emoji_bell, R.drawable.ic_emoji_car, R.drawable.ic_emoji_symbol};
    static {
        emoji = new long[5][];
        emoji[0] = new long[]{0x1f604, 0x1f603, 0x1f600, 0x1f60a, 0x263a, 0x1f609, 0x1f60d, 0x1f618, 0x1f61a, 0x1f617, 0x1f619, 0x1f61c, 0x1f61d, 0x1f61b, 0x1f633, 0x1f601, 0x1f614, 0x1f60c, 0x1f612, 0x1f61e, 0x1f623, 0x1f622, 0x1f602, 0x1f62d, 0x1f62a, 0x1f625, 0x1f630, 0x1f605, 0x1f613, 0x1f629, 0x1f62b, 0x1f628, 0x1f631, 0x1f620, 0x1f621, 0x1f624, 0x1f616, 0x1f606, 0x1f60b, 0x1f637, 0x1f60e, 0x1f634, 0x1f635, 0x1f632, 0x1f61f, 0x1f626, 0x1f627, 0x1f608, 0x1f47f, 0x1f62e, 0x1f62c, 0x1f610, 0x1f615, 0x1f62f, 0x1f636, 0x1f607, 0x1f60f, 0x1f611, 0x1f472, 0x1f473, 0x1f46e, 0x1f477, 0x1f482, 0x1f476, 0x1f466, 0x1f467, 0x1f468, 0x1f469, 0x1f474, 0x1f475, 0x1f471, 0x1f47c, 0x1f478, 0x1f63a, 0x1f638, 0x1f63b, 0x1f63d, 0x1f63c, 0x1f640, 0x1f63f, 0x1f639, 0x1f63e, 0x1f479, 0x1f47a, 0x1f648, 0x1f649, 0x1f64a, 0x1f480, 0x1f47d, 0x1f4a9, 0x1f525, 0x2728, 0x1f31f, 0x1f4ab, 0x1f4a5, 0x1f4a2, 0x1f4a6, 0x1f4a7, 0x1f4a4, 0x1f4a8, 0x1f442, 0x1f440, 0x1f443, 0x1f445, 0x1f444, 0x1f44d, 0x1f44e, 0x1f44c, 0x1f44a, 0x270a, 0x270c, 0x1f44b, 0x270b, 0x1f450, 0x1f446, 0x1f447, 0x1f449, 0x1f448, 0x1f64c, 0x1f64f, 0x261d, 0x1f44f, 0x1f4aa, 0x1f6b6, 0x1f3c3, 0x1f483, 0x1f46b, 0x1f46a, 0x1f46c, 0x1f46d, 0x1f48f, 0x1f491, 0x1f46f, 0x1f646, 0x1f645, 0x1f481, 0x1f64b, 0x1f486, 0x1f487, 0x1f485, 0x1f470, 0x1f64e, 0x1f64d, 0x1f647, 0x1f3a9, 0x1f451, 0x1f452, 0x1f45f, 0x1f45e, 0x1f461, 0x1f460, 0x1f462, 0x1f455, 0x1f454, 0x1f45a, 0x1f457, 0x1f3bd, 0x1f456, 0x1f458, 0x1f459, 0x1f4bc, 0x1f45c, 0x1f45d, 0x1f45b, 0x1f453, 0x1f380, 0x1f302, 0x1f484, 0x1f49b, 0x1f499, 0x1f49c, 0x1f49a, 0x2764, 0x1f494, 0x1f497, 0x1f493, 0x1f495, 0x1f496, 0x1f49e, 0x1f498, 0x1f48c, 0x1f48b, 0x1f48d, 0x1f48e, 0x1f464, 0x1f465, 0x1f4ac, 0x1f463, 0x1f4ad};
        emoji[1] = new long[]{0x1f436, 0x1f43a, 0x1f431, 0x1f42d, 0x1f439, 0x1f430, 0x1f438, 0x1f42f, 0x1f428, 0x1f43b, 0x1f437, 0x1f43d, 0x1f42e, 0x1f417, 0x1f435, 0x1f412, 0x1f434, 0x1f411, 0x1f418, 0x1f43c, 0x1f427, 0x1f426, 0x1f424, 0x1f425, 0x1f423, 0x1f414, 0x1f40d, 0x1f422, 0x1f41b, 0x1f41d, 0x1f41c, 0x1f41e, 0x1f40c, 0x1f419, 0x1f41a, 0x1f420, 0x1f41f, 0x1f42c, 0x1f433, 0x1f40b, 0x1f404, 0x1f40f, 0x1f400, 0x1f403, 0x1f405, 0x1f407, 0x1f409, 0x1f40e, 0x1f410, 0x1f413, 0x1f415, 0x1f416, 0x1f401, 0x1f402, 0x1f432, 0x1f421, 0x1f40a, 0x1f42b, 0x1f42a, 0x1f406, 0x1f408, 0x1f429, 0x1f43e, 0x1f490, 0x1f338, 0x1f337, 0x1f340, 0x1f339, 0x1f33b, 0x1f33a, 0x1f341, 0x1f343, 0x1f342, 0x1f33f, 0x1f33e, 0x1f344, 0x1f335, 0x1f334, 0x1f332, 0x1f333, 0x1f330, 0x1f331, 0x1f33c, 0x1f310, 0x1f31e, 0x1f31d, 0x1f31a, 0x1f311, 0x1f312, 0x1f313, 0x1f314, 0x1f315, 0x1f316, 0x1f317, 0x1f318, 0x1f31c, 0x1f31b, 0x1f319, 0x1f30d, 0x1f30e, 0x1f30f, 0x1f30b, 0x1f30c, 0x1f320, 0x2b50, 0x2600, 0x26c5, 0x2601, 0x26a1, 0x2614, 0x2744, 0x26c4, 0x1f300, 0x1f301, 0x1f308, 0x1f30a};
        emoji[2] = new long[]{0x1f38d, 0x1f49d, 0x1f38e, 0x1f392, 0x1f393, 0x1f38f, 0x1f386, 0x1f387, 0x1f390, 0x1f391, 0x1f383, 0x1f47b, 0x1f385, 0x1f384, 0x1f381, 0x1f38b, 0x1f389, 0x1f38a, 0x1f388, 0x1f38c, 0x1f52e, 0x1f3a5, 0x1f4f7, 0x1f4f9, 0x1f4fc, 0x1f4bf, 0x1f4c0, 0x1f4bd, 0x1f4be, 0x1f4bb, 0x1f4f1, 0x260e, 0x1f4de, 0x1f4df, 0x1f4e0, 0x1f4e1, 0x1f4fa, 0x1f4fb, 0x1f50a, 0x1f509, 0x1f508, 0x1f507, 0x1f514, 0x1f515, 0x1f4e2, 0x1f4e3, 0x23f3, 0x231b, 0x23f0, 0x231a, 0x1f513, 0x1f512, 0x1f50f, 0x1f510, 0x1f511, 0x1f50e, 0x1f4a1, 0x1f526, 0x1f506, 0x1f505, 0x1f50c, 0x1f50b, 0x1f50d, 0x1f6c1, 0x1f6c0, 0x1f6bf, 0x1f6bd, 0x1f527, 0x1f529, 0x1f528, 0x1f6aa, 0x1f6ac, 0x1f4a3, 0x1f52b, 0x1f52a, 0x1f48a, 0x1f489, 0x1f4b0, 0x1f4b4, 0x1f4b5, 0x1f4b7, 0x1f4b6, 0x1f4b3, 0x1f4b8, 0x1f4f2, 0x1f4e7, 0x1f4e5, 0x1f4e4, 0x2709, 0x1f4e9, 0x1f4e8, 0x1f4ef, 0x1f4eb, 0x1f4ea, 0x1f4ec, 0x1f4ed, 0x1f4ee, 0x1f4e6, 0x1f4dd, 0x1f4c4, 0x1f4c3, 0x1f4d1, 0x1f4ca, 0x1f4c8, 0x1f4c9, 0x1f4dc, 0x1f4cb, 0x1f4c5, 0x1f4c6, 0x1f4c7, 0x1f4c1, 0x1f4c2, 0x2702, 0x1f4cc, 0x1f4ce, 0x2712, 0x270f, 0x1f4cf, 0x1f4d0, 0x1f4d5, 0x1f4d7, 0x1f4d8, 0x1f4d9, 0x1f4d3, 0x1f4d4, 0x1f4d2, 0x1f4da, 0x1f4d6, 0x1f516, 0x1f4db, 0x1f52c, 0x1f52d, 0x1f4f0, 0x1f3a8, 0x1f3ac, 0x1f3a4, 0x1f3a7, 0x1f3bc, 0x1f3b5, 0x1f3b6, 0x1f3b9, 0x1f3bb, 0x1f3ba, 0x1f3b7, 0x1f3b8, 0x1f47e, 0x1f3ae, 0x1f0cf, 0x1f3b4, 0x1f004, 0x1f3b2, 0x1f3af, 0x1f3c8, 0x1f3c0, 0x26bd, 0x26be, 0x1f3be, 0x1f3b1, 0x1f3c9, 0x1f3b3, 0x26f3, 0x1f6b5, 0x1f6b4, 0x1f3c1, 0x1f3c7, 0x1f3c6, 0x1f3bf, 0x1f3c2, 0x1f3ca, 0x1f3c4, 0x1f3a3, 0x2615, 0x1f375, 0x1f376, 0x1f37c, 0x1f37a, 0x1f37b, 0x1f378, 0x1f379, 0x1f377, 0x1f374, 0x1f355, 0x1f354, 0x1f35f, 0x1f357, 0x1f356, 0x1f35d, 0x1f35b, 0x1f364, 0x1f371, 0x1f363, 0x1f365, 0x1f359, 0x1f358, 0x1f35a, 0x1f35c, 0x1f372, 0x1f362, 0x1f361, 0x1f373, 0x1f35e, 0x1f369, 0x1f36e, 0x1f366, 0x1f368, 0x1f367, 0x1f382, 0x1f370, 0x1f36a, 0x1f36b, 0x1f36c, 0x1f36d, 0x1f36f, 0x1f34e, 0x1f34f, 0x1f34a, 0x1f34b, 0x1f352, 0x1f347, 0x1f349, 0x1f353, 0x1f351, 0x1f348, 0x1f34c, 0x1f350, 0x1f34d, 0x1f360, 0x1f346, 0x1f345, 0x1f33d};
        emoji[3] = new long[]{0x1f3e0, 0x1f3e1, 0x1f3eb, 0x1f3e2, 0x1f3e3, 0x1f3e5, 0x1f3e6, 0x1f3ea, 0x1f3e9, 0x1f3e8, 0x1f492, 0x26ea, 0x1f3ec, 0x1f3e4, 0x1f307, 0x1f306, 0x1f3ef, 0x1f3f0, 0x26fa, 0x1f3ed, 0x1f5fc, 0x1f5fe, 0x1f5fb, 0x1f304, 0x1f305, 0x1f303, 0x1f5fd, 0x1f309, 0x1f3a0, 0x1f3a1, 0x26f2, 0x1f3a2, 0x1f6a2, 0x26f5, 0x1f6a4, 0x1f6a3, 0x2693, 0x1f680, 0x2708, 0x1f4ba, 0x1f681, 0x1f682, 0x1f68a, 0x1f689, 0x1f69e, 0x1f686, 0x1f684, 0x1f685, 0x1f688, 0x1f687, 0x1f69d, 0x1f68b, 0x1f683, 0x1f68e, 0x1f68c, 0x1f68d, 0x1f699, 0x1f698, 0x1f697, 0x1f695, 0x1f696, 0x1f69b, 0x1f69a, 0x1f6a8, 0x1f693, 0x1f694, 0x1f692, 0x1f691, 0x1f690, 0x1f6b2, 0x1f6a1, 0x1f69f, 0x1f6a0, 0x1f69c, 0x1f488, 0x1f68f, 0x1f3ab, 0x1f6a6, 0x1f6a5, 0x26a0, 0x1f6a7, 0x1f530, 0x26fd, 0x1f3ee, 0x1f3b0, 0x2668, 0x1f5ff, 0x1f3aa, 0x1f3ad, 0x1f4cd, 0x1f6a9, 0x1f1ef, 0x1f1f0, 0x1f1e9, 0x1f1e8, 0x1f1fa, 0x1f1eb, 0x1f1ea, 0x1f1ee, 0x1f1f7, 0x1f1ec};
        emoji[4] = new long[]{0x3120e3, 0x3220e3, 0x3320e3, 0x3420e3, 0x3520e3, 0x3620e3, 0x3720e3, 0x3820e3, 0x3920e3, 0x3020e3, 0x1f51f, 0x1f522, 0x2320e3, 0x1f523, 0x2b06, 0x2b07, 0x2b05, 0x27a1, 0x1f520, 0x1f521, 0x1f524, 0x2197, 0x2196, 0x2198, 0x2199, 0x2194, 0x2195, 0x1f504, 0x25c0, 0x25b6, 0x1f53c, 0x1f53d, 0x21a9, 0x21aa, 0x2139, 0x23ea, 0x23e9, 0x23eb, 0x23ec, 0x2935, 0x2934, 0x1f197, 0x1f500, 0x1f501, 0x1f502, 0x1f195, 0x1f199, 0x1f192, 0x1f193, 0x1f196, 0x1f4f6, 0x1f3a6, 0x1f201, 0x1f22f, 0x1f233, 0x1f235, 0x1f234, 0x1f232, 0x1f250, 0x1f239, 0x1f23a, 0x1f236, 0x1f21a, 0x1f6bb, 0x1f6b9, 0x1f6ba, 0x1f6bc, 0x1f6be, 0x1f6b0, 0x1f6ae, 0x1f17f, 0x267f, 0x1f6ad, 0x1f237, 0x1f238, 0x1f202, 0x24c2, 0x1f6c2, 0x1f6c4, 0x1f6c5, 0x1f6c3, 0x1f251, 0x3299, 0x3297, 0x1f191, 0x1f198, 0x1f194, 0x1f6ab, 0x1f51e, 0x1f4f5, 0x1f6af, 0x1f6b1, 0x1f6b3, 0x1f6b7, 0x1f6b8, 0x26d4, 0x2733, 0x2747, 0x274e, 0x2705, 0x2734, 0x1f49f, 0x1f19a, 0x1f4f3, 0x1f4f4, 0x1f170, 0x1f171, 0x1f18e, 0x1f17e, 0x1f4a0, 0x27bf, 0x267b, 0x2648, 0x2649, 0x264a, 0x264b, 0x264c, 0x264d, 0x264e, 0x264f, 0x2650, 0x2651, 0x2652, 0x2653, 0x26ce, 0x1f52f, 0x1f3e7, 0x1f4b9, 0x1f4b2, 0x1f4b1, 0xa9, 0xae, 0x2122, 0x274c, 0x203c, 0x2049, 0x2757, 0x2753, 0x2755, 0x2754, 0x2b55, 0x1f51d, 0x1f51a, 0x1f519, 0x1f51b, 0x1f51c, 0x1f503, 0x1f55b, 0x1f567, 0x1f550, 0x1f55c, 0x1f551, 0x1f55d, 0x1f552, 0x1f55e, 0x1f553, 0x1f55f, 0x1f554, 0x1f560, 0x1f555, 0x1f556, 0x1f557, 0x1f558, 0x1f559, 0x1f55a, 0x1f561, 0x1f562, 0x1f563, 0x1f564, 0x1f565, 0x1f566, 0x2716, 0x2795, 0x2796, 0x2797, 0x2660, 0x2665, 0x2663, 0x2666, 0x1f4ae, 0x1f4af, 0x2714, 0x2611, 0x1f518, 0x1f517, 0x27b0, 0x3030, 0x303d, 0x1f531, 0x25fc, 0x25fb, 0x25fe, 0x25fd, 0x25aa, 0x25ab, 0x1f53a, 0x1f532, 0x1f533, 0x26ab, 0x26aa, 0x1f534, 0x1f535, 0x1f53b, 0x2b1c, 0x2b1b, 0x1f536, 0x1f537, 0x1f538, 0x1f539};

        for(long[] group : emoji){
            for(long code : group){

                ChatMessageFormatter.addPattern(code, ChatMessageFormatter.charsFromLong(code));
            }
        }
    }

    public int[] getEmojiGroupsIcons(){
        return emoji_group_icons;
    }

    public long[][] getEmojiCodes(){
        return emoji;
    }




    public void setupEmoji(){

    }







    protected void onAuthorize(){
        msAuthorisationFinished = true;
        msAuthorised = true;
        if(!msIsSetupFinished){
            msSetupStage++;
            setupStages();
        }
    }

    protected void onInitFinish(){
        msIsInitFinished = true;
        setLoading(isLoading());
    }

    public void setLoading(boolean fl){
        if(MyApplication.getMainActivity() != null) {
            ServicesMenuFragment fr = (ServicesMenuFragment) ((MainActivity) MyApplication.getMainActivity()).pagerAdapter.getRegisteredFragment(0);
            if(fr != null)
                fr.setServiceLoading(this, fl);
        }
    }



    protected void setNotAuthorised(){
        msAuthorised = false;
    }

    public boolean isOnline(){
        return msAuthorised && msIsInitFinished && msIsSetupFinished;
    }



    public interface DownloadsRequest<T> {
        public void onStarted();
        public void onFinished(T result);
        public void onError();
    }


    public class DlgsDownloadsRequest implements DownloadsRequest<List<mDialog>> {
        public AsyncTaskCompleteListener<List<mDialog>> cb;
        public int count;
        public int offset;

        DlgsDownloadsRequest(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb){
            this.count = count;
            this.offset = offset;
            this.cb = cb;
        }

        @Override
        public void onStarted(){
            dlgs_thread_count += 1;
        }

        @Override
        public void onFinished(List<mDialog> result){
            dlgs_thread_count -= 1;
            if(cb != null) {
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
                }.setDlgs(result));
            }
        }

        @Override
        public void onError(){
            dlgs_thread_count -= 1;
        }

    };

    public class MsgsDownloadsRequest implements DownloadsRequest<List<mMessage>> {
        public AsyncTaskCompleteListener<List<mMessage>> cb;
        public int count;
        public int offset;
        public mDialog dlg;

        MsgsDownloadsRequest(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
            this.cb = cb;
            this.count = count;
            this.offset = offset;
            this.dlg = dlg;
        }

        @Override
        public void onStarted() {
            updateMsgsThreadCount(dlg, 1);
        }

        @Override
        public void onFinished(List<mMessage> result) {
            updateMsgsThreadCount(dlg, -1);
            if (cb != null) {
                ((MainActivity) MyApplication.getMainActivity()).runOnUiThread(new Runnable() {
                    List<mMessage> msgs;

                    Runnable setMsgs(List<mMessage> msgs) {
                        this.msgs = msgs;
                        return this;
                    }

                    @Override
                    public void run() {
                        cb.onTaskComplete(msgs);
                    }
                }.setMsgs(result));
            }
        }

        @Override
        public void onError() {
            updateMsgsThreadCount(dlg, -1);
        }
    }

    public class CntsDataDownloadsRequest implements DownloadsRequest<List<mContact>> {
        public List<mContact> cnts;

        CntsDataDownloadsRequest(List<mContact> cnts) {
            this.cnts = cnts;
        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onFinished(List<mContact> result) {
            if(msAccumCnts.size() > 0) msHandler.postDelayed(cnts_request_runnable, 500);
            else ms_accum_cnt_handler_isRunning = false;

            boolean updated = false;

            for(mContact cnt : result){
                if(msDBHelper.updateCntInDB(cnt, MessageService.this) == true)updated = true;
            }

            if(updated)msApp.triggerCntsUpdaters();
        }

        @Override
        public void onError() {

        }
    }

    public class CntsDownloadsRequest implements DownloadsRequest<List<mContact>> {
        public AsyncTaskCompleteListener<List<mContact>> cb;
        public int count;
        public int offset;

        CntsDownloadsRequest(int count, int offset, AsyncTaskCompleteListener<List<mContact>> cb) {
            this.cb = cb;
            this.count = count;
            this.offset = offset;
        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onFinished(List<mContact> result) {
            if (cb != null) {
                ((MainActivity) MyApplication.getMainActivity()).runOnUiThread(new Runnable() {
                    List<mContact> cnts;

                    Runnable setMsgs(List<mContact> msgs) {
                        this.cnts = msgs;
                        return this;
                    }

                    @Override
                    public void run() {
                        cb.onTaskComplete(cnts);
                    }
                }.setMsgs(result));
            }
        }

        @Override
        public void onError() {

        }
    }





    // Общие действия для меню
    protected void openActiveDlg(){
        if(getActiveDialog() != null){
            msApp.msManager.setActiveService(getServiceType());
            ((MainActivity) MyApplication.getMainActivity()).mViewPager.setCurrentItem(2);
        }
    }

    protected void openContacts(Context context){
        Intent intent = new Intent(context, ActivityTwo.class);
        intent.putExtra("mode", "contacts");
        intent.putExtra("msg_service", getServiceType());
        msApp.getMainActivity().startActivityForResult(intent, ActivityTwo.REQUEST_CODE);
    }

    protected void openDialogs(){
        msApp.sPref.edit().putInt("selected_service_for_dialogs", getServiceType());
        ((MainActivity) MyApplication.getMainActivity()).pagerAdapter.recreateFragment(1);
        ((MainActivity) MyApplication.getMainActivity()).mViewPager.setCurrentItem(1);
    }






    public void onCreate(Activity activity, Bundle savedInstanceState){

    }

    public void onStart(Activity activity){

    }

    public void onResume(Activity activity){

    }

    public void onPause(Activity activity){

    }

    public void onStop(Activity activity){

    }

    public void onDestroy(Activity activity){

    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data){

    }

    public void onSaveInstanceState(Activity activity, Bundle outState) {

    }



    // Static functions


    public static MessageService createServiceByType(int service_type, MyApplication app){
        MessageService ms = null;
        switch(service_type){
            case MessageService.SMS: ms = new Sms(app); break;
            case MessageService.VK: ms = new Vk(app); break;
            case MessageService.TW: ms = new mTwitter(app); break;
            case MessageService.FB: ms = new mFacebook(app); break;
        }
        return ms;
    }

    public static String getCacheFolder(int ser_type){
        switch (ser_type) {
            case SMS: return "SMSCache";
            case VK: return "VKCache";
            case TW: return "TWCache";
            case FB: return "FBCache";
        }
        return "ALLCache";
    }

}
