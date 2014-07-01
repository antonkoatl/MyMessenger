package com.example.mymessenger.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.ChatMessageFormatter;
import com.example.mymessenger.DBHelper;
import com.example.mymessenger.MainActivity;
import com.example.mymessenger.MyApplication;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.UpdateService;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mGlobal.IntegerMutable;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.ui.ServicesMenuFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MessageService {
    public static final int SMS = 10;
    public static final int VK = 11;
    public static final int TW = 12;

    public static final int MSGS_DOWNLOAD_COUNT = 20;
    public static final int DLGS_DOWNLOAD_COUNT = 20;
    public static final int CNTS_REQUEST_ACCUM_TIME = 1000;

    protected static final String PREFS_ACTIVE_ACCOUNT = "active_account";

    protected final static Handler msHandler; //Для отложенного запроса данных о пользователях

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
    private boolean dl_all_msgs_downloaded = true;
    protected int dlgs_thread_count = 0; //Количество потоков, загружающих диалоги в данных момент
    protected Map<mDialog, IntegerMutable> msgs_thread_count; //Количество потоков, загружающих сообщения для определённого диалога в данных момент
    protected SharedPreferences sPref;

    boolean msIsSetupFinished = true;
    protected int msSetupStage;
    protected AsyncTaskCompleteListener<MessageService> cbms_for_setup = null;

    /*
         Контакт активного пользователь
         Авторизация
         Обновление сообщений
     */
    boolean msIsInitFinished = false;


    static {
        msHandler = new Handler();
    }

    protected MessageService(MyApplication app, int ser_type, int ser_name_id) {
        this.msApp = app;
        msContacts = new HashMap<String, mContact>();
        msgs_thread_count = new HashMap<mDialog, IntegerMutable>(); //индикаторы загрузки сообщений для диалогов

        msAccumCnts = new ArrayList<mContact>();


        msServiceName = msApp.getString(ser_name_id);
        msServiceType = ser_type;

        sPref = app.getSharedPreferences(String.valueOf(msServiceType), Context.MODE_PRIVATE); //загрузка конфигов

        msSelfContact = new mContact(sPref.getString(PREFS_ACTIVE_ACCOUNT, ""));

        setupEmoji();
    }

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

    // Подготовить сервис для работы
    public final /**/void setup(AsyncTaskCompleteListener<MessageService> asms) {
        msIsSetupFinished = false;
        msSetupStage = 1;
        cbms_for_setup = asms;
        setupStages();
    }

    // Удалить сервис
    public abstract void unsetup();

    // Обновить информацию об аккаунте
    public void requestAccountInfo() {
        requestAccountInfoFromNet(self_contact_cb);
    }

    // Запросить список контактов - не кешируется в бд, абстрактный
    public abstract void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);

    // Запросить данные контакта - контакт обновится, вызовутся триггеры контактов. Перед запросом контакты накапливаются CNTS_REQUEST_ACCUM_TIME миллисекунд
    public final void requestContactData(mContact cnt) {
        msAccumCnts.add(cnt);

        if (!ms_accum_cnt_handler_isRunning) {
            ms_accum_cnt_handler_isRunning = true;

            msHandler.postDelayed(cnts_request_runnable, CNTS_REQUEST_ACCUM_TIME);
        }
    }

    // Запросить данные контактов
    public final void requestContactsData(List<mContact> cnts) {
        getContactsFromDB(cnts);
        if(isOnline()) getContactsFromNet(cnts);
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

    // Запросить список диалогов
    public final void requestDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
        if(!msIsSetupFinished)return; // TODO: отложить, тоже самое для авторизации
        int dlgs_in_db = msApp.dbHelper.getDlgsCount(MessageService.this);

        if (offset + count < dlgs_in_db) {
            getDialogsFromDB(count, offset, cb);
            if(isOnline()) refreshDialogsFromNet(cb, 0);
        } else {
            getDialogsFromDB(count, offset, cb);
            if(isOnline()) {
                msUpdateDlgsDB_cb up_cb = new msUpdateDlgsDB_cb(cb);
                getDialogsFromNet(count, offset, up_cb);
            }

        }
    }

    // TODO: Пересмотреть
    public final void refreshDialogsFromNet(AsyncTaskCompleteListener<List<mDialog>> cb, int count) {
        msRefreshDlgsCb.addRefresh(cb, count);
    }

    // Запросить список сообщений для диалога
    public final void requestMessages(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
        int msgs_in_db = msApp.dbHelper.getMsgsCount(dlg, MessageService.this);

        if (offset + count < msgs_in_db) {
            getMessagesFromDB(dlg, count, offset, cb);
            if(isOnline()) refreshMessagesFromNet(dlg, cb, 0);
        } else {
            getMessagesFromDB(dlg, count, offset, cb);
            if(isOnline()) {
                msUpdateMsgsDB_cb up_cb = new msUpdateMsgsDB_cb(dlg, cb);
                getMessagesFromNet(dlg, count, offset, up_cb);
            }

        }
    }

    // TODO: Пересмотреть
    public final void refreshMessagesFromNet(mDialog dlg, AsyncTaskCompleteListener<List<mMessage>> cb, int count) {
        msRefreshMsgsCb.addRefresh(dlg, cb, count);
    }

    // Получить контакт, создастся, обновится
    // TODO: Периодически обновлять
    public final mContact getContact(String address) {
        mContact cnt = msContacts.get(address);

        if (cnt == null) {
            cnt = new mContact(address);
            requestContactData(cnt);
            msContacts.put(address, cnt);
        }

        return cnt;
    }

    // Получить диалог для общения с контактом
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

    // Отправка сообщения
    public abstract boolean sendMessage(String address, String text);

    // TODO: Переработать, создать механизм проверки успешности передачи данных через интернет перед сохранением в бд
    public abstract void requestMarkAsReaded(mMessage msg, mDialog dlg);

    // Запросить алгоритм для отслеживания новых сообщений
    // TODO: Организовать работу в UpdateService через ThreadPool
    public abstract void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb);


    public final String getServiceName() {
        return msServiceName;
    }

    public final int getServiceType() {
        return msServiceType;
    }

    public final mDialog getActiveDialog() {
        return msActiveDialog;
    }

    public final mContact getMyContact() {
        return msSelfContact;
    }


    public final void setActiveDialog(mDialog dlg) {
        msActiveDialog = dlg;
        msApp.sPref.edit().putInt("active_dialog", msApp.dbHelper.getDlgId(dlg, this)).commit();
    }


    // TODO: Пересмотреть
    public final boolean isAllMsgsDownloaded() {
        return dl_all_msgs_downloaded;
    }

    // Есть ли потоки, загружающие диалоги
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


    // Функции для интерфейса
    public abstract String[] getStringsForMainViewMenu();

    public abstract void MainViewMenu_click(int which, Context context);





















    // Получение информации о текущем аккауте
    protected abstract void requestAccountInfoFromNet(final AsyncTaskCompleteListener<mContact> cb);

    // Загрузка данных контактов из бд, выполняется асинхронно, без cb
    protected final void getContactsFromDB(final List<mContact> cnts) {
        msHandler.post(new Runnable() {
            @Override
            public void run() {
                load_cnts_from_db(cnts);
            }
        });
    }

    // Загрузка данных контактов из интернета, после завершения должно проверятся msAccumCnts. Если что то обновилось - вызываться триггеры
    protected abstract void getContactsFromNet(final List<mContact> cnts);

    // Загрузка сообщений из бд, выполняется асинхронно
    protected final void getMessagesFromDB(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
        // Обновление информации о количестве потоков загрузки
        updateMsgsThreadCount(dlg, 1);

        new load_msgs_from_db_async(cb, dlg).execute(count, offset);
    }

    // Загрузка сообщений из интернета
    protected abstract void getMessagesFromNet(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb);

    // Загрузка диалогов из бд, выполняется асинхронно
    protected final void getDialogsFromDB(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
        // Обновление информации о количестве потоков загрузки
        dlgs_thread_count += 1;
        new load_dlgs_from_db_async(cb).execute(count, offset);
    }

    // Загрузка диалогов из интернета
    protected abstract void getDialogsFromNet(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb);

    // Обновить информацию о потоках загрузки сообщений
    protected final void updateMsgsThreadCount(mDialog dlg, int count) {
        IntegerMutable lm_count = msgs_thread_count.get(dlg);
        if (lm_count == null) {
            lm_count = new IntegerMutable(count);
            msgs_thread_count.put(dlg, lm_count);
        } else lm_count.value += count;
    }

    public boolean isLoading() {
        return !(msIsSetupFinished && msIsInitFinished);
    }

    protected final class load_dlgs_from_db_async extends AsyncTask<Integer, Void, List<mDialog>> {
        private AsyncTaskCompleteListener<List<mDialog>> callback;

        public load_dlgs_from_db_async(AsyncTaskCompleteListener<List<mDialog>> cb) {
            this.callback = cb;
        }

        protected void onPostExecute(List<mDialog> result) {
            dlgs_thread_count--;
            if (callback != null) callback.onTaskComplete(result);
        }

        @Override
        protected List<mDialog> doInBackground(Integer... params) {
            return load_dialogs_from_db(params[0], params[1]);
        }
    }

    protected final class load_msgs_from_db_async extends AsyncTask<Integer, Void, List<mMessage>> {
        private AsyncTaskCompleteListener<List<mMessage>> callback;
        private mDialog dlg;

        public load_msgs_from_db_async(AsyncTaskCompleteListener<List<mMessage>> cb, mDialog dialog) {
            this.callback = cb;
            this.dlg = dialog;
        }

        protected void onPostExecute(List<mMessage> result) {
            updateMsgsThreadCount(dlg, -1);
            if (callback != null) callback.onTaskComplete(result);
        }

        @Override
        protected List<mMessage> doInBackground(Integer... params) {
            return load_msgs_from_db(dlg, params[0], params[1]);
        }
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

    // Загрузка контактов из бд
    protected void load_cnts_from_db(List<mContact> cnts) {
        msApp.dbHelper.loadCnts(cnts, MessageService.this);
        msApp.triggerCntsUpdaters();
    }

    // Загрузка диалогов из бд
    protected List<mDialog> load_dialogs_from_db(int count, int offset) {
        return msApp.dbHelper.loadDlgs(this, count, offset);
    }

    // Загрузка сообщений из бд
    protected List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset) {
        List<mMessage> result = msApp.dbHelper.loadMsgs(this, dlg, count, offset);

        return result;
    }

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

            result = msApp.update_db_msgs(result, MessageService.this, dlg);

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

            for (mDialog mdl : result) {
                String selection = DBHelper.colParticipants + " = ?";
                String[] selectionArgs = {mdl.getParticipantsAddresses()};
                Cursor c = db.query(my_table_name, null, selection, selectionArgs, null, null, null);

                if (c.moveToFirst()) {
                    Time last_time_in_db = new Time();
                    last_time_in_db.set(c.getLong(c.getColumnIndex(DBHelper.colLastmsgtime)));

                    if (mdl.last_msg_time.after(last_time_in_db)) {
                        //update
                        int id = c.getInt(c.getColumnIndex(DBHelper.colId));
                        c.close();

                        msApp.dbHelper.updateDlg(id, mdl, MessageService.this);

                        update_dlgs.add(mdl);
                    } else {
                        //not update
                        c.close();
                        all_new = false;
                        continue;
                    }
                } else {
                    //add
                    c.close();

                    msApp.dbHelper.insertDlg(mdl, MessageService.this);

                    update_dlgs.add(mdl);
                }
            }

            if (all_new && result.size() > 0) {
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

                    getDialogsFromNet(count, offset, this);
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
                getDialogsFromNet(this.count, this.offset, this);
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

            if (result.size() < cp.count) dl_all_msgs_downloaded = true;
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

                    getMessagesFromNet(cp.dlg, cp.count, cp.offset, this);
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
                getMessagesFromNet(cp.dlg, cp.count, cp.offset, this);
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
                getMessagesFromNet(np.dlg, np.count, np.offset, this);
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























































    // Emoji
    public abstract long[][] getEmojiCodes();
    public abstract String getEmojiUrl(long code);
    public abstract int[] getEmojiGroupsIcons();

    public void setupEmoji(){
        for(long[] group : getEmojiCodes()){
            for(long code : group){
                String scode = ChatMessageFormatter.long_to_hex_string(code);
                String res_url = getEmojiUrl(code);
                String ccode = ChatMessageFormatter.string_from_hex_string(scode);
                ChatMessageFormatter.addPattern(getServiceType(), res_url, ccode);
            }
        }
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

    protected boolean updateCntInDB(mContact cnt){
        mContact cnt_in_db = msApp.dbHelper.getCnt(cnt.address, MessageService.this);

        if(cnt_in_db != null){
            //update
            if(!cnt.name.equals( cnt_in_db.name )){
                msApp.dbHelper.updateCnt(cnt, MessageService.this);
                return true;
            }
            if(!cnt.icon_50_url.equals( cnt_in_db.icon_50_url)){
                msApp.dbHelper.updateCnt(cnt, MessageService.this);
                return true;
            }

        } else {
            // add
            msApp.dbHelper.insertCnt(cnt, MessageService.this);
            return true;
        }

        return false;
    }

    protected void setNotAuthorised(){
        msAuthorised = false;
    }

    public boolean isOnline(){
        return msAuthorised && msIsInitFinished && msIsSetupFinished;
    }

}
