package com.example.mymessenger.services.MessageService;

import com.example.mymessenger.AsyncTaskCompleteListener;
import com.example.mymessenger.RunnableAdvanced;
import com.example.mymessenger.mContact;
import com.example.mymessenger.mDialog;
import com.example.mymessenger.mMessage;

import java.util.List;

public interface msInterfaceMS extends msInterfaceGeneral {

    public void init();

    // Подготовить сервис для работы
    public void setup(AsyncTaskCompleteListener<MessageService> asms);

    // Удалить сервис
    public void unsetup();


    // Запросить список контактов - не кешируется в бд, абстрактный
    public void requestContacts(int offset, int count, AsyncTaskCompleteListener<List<mContact>> cb);

    // Запросить данные контакта - контакт обновится, вызовутся триггеры контактов. Перед запросом контакты накапливаются CNTS_REQUEST_ACCUM_TIME миллисекунд
    public void requestContactData(mContact cnt);

    // Запросить данные контактов
    public void requestContactsData(List<mContact> cnts);

    // Запросить список диалогов
    public void requestDialogs(int count, int offset, AsyncTaskCompleteListener<List<mDialog>> cb);

    // Запросить список сообщений для диалога
    public void requestMessages(mDialog dlg, int count, int offset, AsyncTaskCompleteListener<List<mMessage>> cb);

    public void refreshDialogsFromNet(AsyncTaskCompleteListener<List<mDialog>> cb, int count);

    // Запросить алгоритм для отслеживания новых сообщений
    public void requestNewMessagesRunnable(AsyncTaskCompleteListener<RunnableAdvanced<?>> cb);

    // TODO: Переработать, создать механизм проверки успешности передачи данных через интернет перед сохранением в бд
    public void requestMarkAsReaded(mMessage msg, mDialog dlg);

    // Отправка сообщения
    public boolean sendMessage(String address, String text);




    public mContact getMyContact();

    public mDialog getActiveDialog();

    // Получить контакт, создастся, обновится
    public mContact getContact(String address);

    // Force load from DB in current thread
    public mContact getContactCheckDB(String address);

    // Получить диалог для общения с контактом
    public mDialog getDialog(mContact cnt);


    public void setActiveDialog(mDialog dlg);


    // Есть ли потоки, загружающие диалоги
    public boolean isLoadingDlgs();
}
