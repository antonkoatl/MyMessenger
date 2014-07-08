package com.example.mymessenger.services;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.format.Time;
import android.util.Log;

import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Azteki on 05.07.2014.
 */
public class MSDBHelper_tw extends MSDBHelper {
    private static MSDBHelper instance = new MSDBHelper_tw();

    public static MSDBHelper getInstance(){
        return instance;
    }

    @Override
    protected List<mMessage> load_msgs_from_db(mDialog dlg, int count, int offset, MessageService ms) {
        // Ignore chat_id, it's for dlgs view only
        mDialog dlg_no_chat = new mDialog(dlg);
        dlg_no_chat.chat_id = 0;
        List<mMessage> result = ms.msApp.dbHelper.loadMsgs(ms, dlg_no_chat, count, offset);

        return result;
    }

    @Override
    public void updateMsgInDB(mMessage msg, long chat_id, MessageService ms) {
        mDialog dlg = updateDlgInDB(msg, chat_id, ms);
        dlg.chat_id = 0; // Ignore chat_id, it's for dlgs view only
        updateMsgInDB(msg, dlg, ms);
    }
}
