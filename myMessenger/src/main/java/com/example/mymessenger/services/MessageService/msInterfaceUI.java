package com.example.mymessenger.services.MessageService;

import android.content.Context;

public interface msInterfaceUI extends msInterfaceGeneral {
    public boolean isLoading();

    // Функции для интерфейса
    public String[] getStringsForMainViewMenu();

    public void MainViewMenu_click(int which, Context context);
}
