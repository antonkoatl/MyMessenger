package com.example.mymessenger.services;

import android.os.AsyncTask;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

import java.util.ArrayList;
import java.util.List;

public class MSDBHelper {
    protected final MessageService ms;

    public MSDBHelper(MessageService messageService) {
        this.ms = messageService;
    }

    // Загрузка данных контактов из бд, выполняется асинхронно, без cb
    protected final void getContactsFromDB(final List<mContact> cnts) {
        ms.msHandler.post(new Runnable() {
            @Override
            public void run() {
                load_cnts_from_db(cnts);
            }
        });
    }

    protected final void getMessagesFromDB(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb) {
        // Обновление информации о количестве потоков загрузки
        ms.updateMsgsThreadCount(dlg, 1);

        new load_msgs_from_db_async(cb, dlg).execute(count, offset);
    }

    // Загрузка диалогов из бд, выполняется асинхронно
    protected final void getDialogsFromDB(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb) {
        // Обновление информации о количестве потоков загрузки
        ms.dlgs_thread_count += 1;
        new load_dlgs_from_db_async(cb).execute(count, offset);
    }

    protected boolean updateCntInDB(mContact cnt){
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
    protected void load_cnts_from_db(List<mContact> cnts) {
        ms.msApp.dbHelper.loadCnts(cnts, ms);
        ms.msApp.triggerCntsUpdaters();
    }

    private final class load_msgs_from_db_async extends AsyncTask<Integer, Void, List<mMessage>> {
        private AsyncTaskCompleteListener<List<mMessage>> callback;
        private mDialog dlg;

        public load_msgs_from_db_async(AsyncTaskCompleteListener<List<mMessage>> cb, mDialog dialog) {
            this.callback = cb;
            this.dlg = dialog;
        }

        protected void onPostExecute(List<mMessage> result) {
            ms.updateMsgsThreadCount(dlg, -1);
            if (callback != null) callback.onTaskComplete(result);
        }

        @Override
        protected List<mMessage> doInBackground(Integer... params) {
            return load_msgs_from_db(dlg, params[0], params[1]);
        }
    }

    // Загрузка сообщений из бд
    protected List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset) {
        List<mMessage> result = ms.msApp.dbHelper.loadMsgs(ms, dlg, count, offset);

        return result;
    }

    protected final class load_dlgs_from_db_async extends AsyncTask<Integer, Void, List<mDialog>> {
        private AsyncTaskCompleteListener<List<mDialog>> callback;

        public load_dlgs_from_db_async(AsyncTaskCompleteListener<List<mDialog>> cb) {
            this.callback = cb;
        }

        protected void onPostExecute(List<mDialog> result) {
            ms.dlgs_thread_count--;
            if (callback != null) callback.onTaskComplete(result);
        }

        @Override
        protected List<mDialog> doInBackground(Integer... params) {
            return load_dialogs_from_db(params[0], params[1]);
        }
    }

    // Загрузка диалогов из бд
    protected List<mDialog> load_dialogs_from_db(int count, int offset) {
        return ms.msApp.dbHelper.loadDlgs(ms, count, offset);
    }

    public mDialog updateDlgInDB(mMessage msg, long chat_id) {
        mDialog dlg;
        if (chat_id != 0) {
            int dlg_key = ms.msApp.dbHelper.getDlgIdOrCreate(chat_id, ms);
            dlg = ms.msApp.update_db_dlg(msg, dlg_key);
        } else {
            int dlg_key = ms.msApp.dbHelper.getDlgIdOrCreate(msg.respondent.address, ms);
            dlg = ms.msApp.update_db_dlg(msg, dlg_key);
        }


        return dlg;
    }

    public void updateMsgInDB(mMessage msg, long chat_id) {
        updateMsgInDB(msg, updateDlgInDB(msg, chat_id));
    }

    public void updateMsgInDB(mMessage msg, mDialog dlg) {
        ms.msApp.update_db_msg(msg, dlg);
        ms.msApp.triggerMsgUpdaters(msg, dlg);
    }

    public mMessage getMsgByIdFromDB(int msg_id) {
        return ms.msApp.dbHelper.getMsgByMsgId(msg_id, ms);
    }

    public void updateMsgInDBById(mMessage msg, int msg_id) {
        ms.msApp.dbHelper.updateMsgById(msg_id,msg, ms);
        mDialog dlg = ms.msApp.dbHelper.getDlgById(ms.msApp.dbHelper.getDlgIdByMsgId(msg_id, ms), ms);
        List<mDialog> dlgs = new ArrayList<mDialog>();
        dlgs.add(dlg);
        ms.msApp.triggerDlgsUpdaters(dlgs);
        ms.msApp.triggerMsgUpdaters(msg, dlg);
    }


}