package com.example.mymessenger.services.MessageService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public interface msInterfaceUI extends msInterfaceGeneral {
    public boolean isLoading();

    // Функции для интерфейса
    public String[] getStringsForMainViewMenu();

    public void MainViewMenu_click(int which, Context context);

    public void onCreate(Activity activity, Bundle savedInstanceState);

    public void onStart(Activity activity);

    public void onResume(Activity activity);

    public void onPause(Activity activity);

    public void onStop(Activity activity);

    public void onDestroy(Activity activity);

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);

    public void onSaveInstanceState(Activity activity, Bundle outState);
}
