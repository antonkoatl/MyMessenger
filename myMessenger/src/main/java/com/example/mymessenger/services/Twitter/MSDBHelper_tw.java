package com.example.mymessenger.services.Twitter;

import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;
import com.example.mymessenger.services.MessageService.MSDBHelper;
import com.example.mymessenger.services.MessageService.MessageService;

import java.util.List;

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

        return ms.getMsApp().dbHelper.loadMsgs(ms, dlg_no_chat, count, offset);
    }

    @Override
    public void updateMsgInDB(mMessage msg, long chat_id, MessageService ms) {
        mDialog dlg = updateDlgInDB(msg, chat_id, ms);
        dlg.chat_id = 0; // Ignore chat_id, it's for dlgs view only
        updateMsgInDB(msg, dlg, ms);
    }
}
