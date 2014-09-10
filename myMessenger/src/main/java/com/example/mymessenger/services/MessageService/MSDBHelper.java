package com.example.mymessenger.services.MessageService;

import android.os.AsyncTask;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

import java.util.ArrayList;
import java.util.List;

public class MSDBHelper {
    private static MSDBHelper instance = new MSDBHelper();

    public static MSDBHelper getInstance(){
        return instance;
    }

    // Загрузка данных контактов из бд, выполняется асинхронно, без cb
    protected final void getContactsFromDB(final List<mContact> cnts, final MessageService ms) {
        ms.msHandler.post(new Runnable() {
            @Override
            public void run() {
                load_cnts_from_db(cnts, ms);
            }
        });
    }

    protected final void getMessagesFromDB(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb, MessageService ms) {
        // Обновление информации о количестве потоков загрузки
        ms.updateMsgsThreadCount(dlg, 1);

        new load_msgs_from_db_async(cb, dlg, ms).execute(count, offset);
    }

    // Загрузка диалогов из бд, выполняется асинхронно
    protected final void getDialogsFromDB(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb, MessageService ms) {
        // Обновление информации о количестве потоков загрузки
        ms.dlgs_thread_count += 1;
        new load_dlgs_from_db_async(cb, ms).execute(count, offset);
    }

    public boolean updateCntInDB(mContact cnt, MessageService ms){
        mContact cnt_in_db = ms.msApp.dbHelper.getCnt(cnt.address, ms);

        if(cnt_in_db != null){
            //update
            if(!cnt.name.equals( cnt_in_db.name )){
                ms.msApp.dbHelper.updateCnt(cnt, ms);
                return true;
            }
            if(!cnt.icon_50_url.equals( cnt_in_db.icon_50_url)){
                ms.msApp.dbHelper.updateCnt(cnt, ms);
                return true;
            }

        } else {
            // add
            ms.msApp.dbHelper.insertCnt(cnt, ms);
            return true;
        }

        return false;
    }

    // Загрузка контактов из бд
    protected void load_cnts_from_db(List<mContact> cnts, MessageService ms) {
        ms.msApp.dbHelper.loadCnts(cnts, ms);
        ms.msApp.triggerCntsUpdaters();
    }

    private final class load_msgs_from_db_async extends AsyncTask<Integer, Void, List<mMessage>> {
        private AsyncTaskCompleteListener<List<mMessage>> callback;
        private mDialog dlg;
        private MessageService ms;

        public load_msgs_from_db_async(AsyncTaskCompleteListener<List<mMessage>> cb, mDialog dialog, MessageService ms) {
            this.callback = cb;
            this.dlg = dialog;
            this.ms = ms;
        }

        protected void onPostExecute(List<mMessage> result) {
            ms.updateMsgsThreadCount(dlg, -1);
            if (callback != null) callback.onTaskComplete(result);
        }

        @Override
        protected List<mMessage> doInBackground(Integer... params) {
            return load_msgs_from_db(dlg, params[0], params[1], ms);
        }
    }

    // Загрузка сообщений из бд
    protected List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset, MessageService ms) {
        List<mMessage> result = ms.msApp.dbHelper.loadMsgs(ms, dlg, count, offset);

        return result;
    }

    protected final class load_dlgs_from_db_async extends AsyncTask<Integer, Void, List<mDialog>> {
        private AsyncTaskCompleteListener<List<mDialog>> callback;
        MessageService ms;

        public load_dlgs_from_db_async(AsyncTaskCompleteListener<List<mDialog>> cb, MessageService ms) {
            this.callback = cb;
            this.ms = ms;
        }

        protected void onPostExecute(List<mDialog> result) {
            ms.dlgs_thread_count--;
            if (callback != null) callback.onTaskComplete(result);
        }

        @Override
        protected List<mDialog> doInBackground(Integer... params) {
            return load_dialogs_from_db(params[0], params[1], ms);
        }
    }

    // Загрузка диалогов из бд
    protected List<mDialog> load_dialogs_from_db(int count, int offset, MessageService ms) {
        return ms.msApp.dbHelper.loadDlgs(ms, count, offset);
    }

    public mDialog updateDlgInDB(mMessage msg, long chat_id, MessageService ms) {
        mDialog dlg;
        if (chat_id != 0) {
            int dlg_key = ms.msApp.dbHelper.getDlgIdOrCreate(chat_id, ms);
            dlg = ms.msApp.dbHelper.update_db_dlg(msg, dlg_key, ms);
        } else {
            long dlg_key = ms.msApp.dbHelper.getDlgIdOrCreate(msg.respondent.address, ms);
            dlg = ms.msApp.dbHelper.update_db_dlg(msg, dlg_key, ms);
        }


        return dlg;
    }

    public mDialog getDlgFromDBOrCreate(mMessage msg, long chat_id, MessageService ms){
        mDialog dlg;
        if (chat_id != 0) {
            int dlg_key = ms.msApp.dbHelper.getDlgIdOrCreate(chat_id, ms);
            dlg = ms.msApp.dbHelper.getDlgById(dlg_key, ms);
        } else {
            long dlg_key = ms.msApp.dbHelper.getDlgIdOrCreate(msg.respondent.address, ms);
            dlg = ms.msApp.dbHelper.getDlgById(dlg_key, ms);
        }

        return dlg;
    }

    public void updateMsgInDB(mMessage msg, long chat_id, MessageService ms) {
        updateMsgInDB(msg, updateDlgInDB(msg, chat_id, ms), ms);
    }

    public void updateMsgInDB(mMessage msg, mDialog dlg, MessageService ms) {
        ms.msApp.dbHelper.update_db_msg(msg, dlg, ms);
        ms.msApp.triggerMsgUpdaters(msg, dlg);
    }

    public mMessage getMsgByIdFromDB(int msg_id, MessageService ms) {
        return ms.msApp.dbHelper.getMsgByMsgId(msg_id, ms);
    }

    public void updateMsgInDBById(mMessage msg, int msg_id, MessageService ms) {
        ms.msApp.dbHelper.updateMsgById(msg_id,msg, ms);
        mDialog dlg = ms.msApp.dbHelper.getDlgById(ms.msApp.dbHelper.getDlgIdByMsgId(msg_id, ms), ms);
        List<mDialog> dlgs = new ArrayList<mDialog>();
        dlgs.add(dlg);
        ms.msApp.triggerDlgsUpdaters(dlgs);
        ms.msApp.triggerMsgUpdaters(msg, dlg);
    }



}