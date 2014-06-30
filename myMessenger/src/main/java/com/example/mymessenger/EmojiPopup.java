package com.example.mymessenger;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

import com.example.mymessenger.SoftKeyboardStateHelper.SoftKeyboardStateListener;
import com.example.mymessenger.services.MessageService;

public class EmojiPopup {
    public static final String ACTION_HIDE_POPUP = "com.vkontakte.andoroid.HIDE_EMOJI_POPUP";
    private int btnRes;
    private View contentView;
    private Context context;
    private PopupWindow emojiPopup;
    private EmojiView emojiView;
    private int keyboardHeight;
    private boolean keyboardVisible;
    //private BroadcastReceiver receiver;
	private MessageService ms;

	SoftKeyboardStateHelper softKeyboardStateHelper;
	SoftKeyboardStateListener sListener = new SoftKeyboardStateListener(){

		@Override
		public void onSoftKeyboardOpened(int keyboardHeightInPx) {
			onKeyboardStateChanged(true, keyboardHeightInPx);
		}

		@Override
		public void onSoftKeyboardClosed() {
			onKeyboardStateChanged(false, -1);
		}
		
	};
	 

    public EmojiPopup(Context c, View content, int icon, MessageService ms) {
        /*this.receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (ACTION_HIDE_POPUP.equals(intent.getAction())) {
                    EmojiPopup.this.hide();
                }

            }
        };*/
        this.context = c;
        this.contentView = content;
        this.btnRes = icon;
        this.ms = ms;
        
        SoftKeyboardStateHelper softKeyboardStateHelper = new SoftKeyboardStateHelper(content);
        softKeyboardStateHelper.addSoftKeyboardStateListener(sListener);
    }

    private void createEmojiPopup() {
        this.emojiView = new EmojiView(this.context, ms);
        this.emojiView.setListener(new EmojiView.Listener() {
            public void onBackspace() {
                ((EditText) EmojiPopup.this.contentView.findViewById(R.id.msg_entertext)).dispatchKeyEvent(new KeyEvent(0, 67));
            }

            public void onEmojiSelected(String s) {
                EditText edit = (EditText) EmojiPopup.this.contentView.findViewById(R.id.msg_entertext);
                int pos = edit.getSelectionEnd();
                CharSequence em = ChatMessageFormatter.getSmiledText(context, s, ms.getServiceType(), 0);
                edit.setText(edit.getText().insert(pos, em));
                pos += em.length();
                edit.setSelection(pos, pos);
            }

        });
        this.emojiPopup = new PopupWindow(this.emojiView);
    }

    public void hide() {
        if (this.emojiPopup == null || (!this.emojiPopup.isShowing())) {
        } else {
            try {
                showEmojiPopup(false);
            } catch (Exception e) {
                //MyApplication.context.unregisterReceiver(this.receiver);
            }
        }
    }

    public boolean isShowing() {
        return this.emojiPopup != null && this.emojiPopup.isShowing();
    }

    public void loadRecents() {
        if (this.emojiView != null) {
            this.emojiView.loadRecents();
        }
    }


    public void onKeyboardStateChanged(boolean visible, int h) {
        if (mGlobal.isTablet) {
        } else {
            this.keyboardVisible = visible;
            this.keyboardHeight = h;
            if (this.keyboardHeight <= mGlobal.scale(100.0f) || (!visible)) {
                Log.i("vk", new StringBuilder("ST Keyboard height = ").append(this.keyboardHeight).append(", visible = ").append(visible).toString());
            } else {
                this.context.getSharedPreferences("emoji", 0).edit().putInt(new StringBuilder("kbd_height").append(this.context.getResources().getDisplayMetrics().widthPixels).append("_").append(this.context.getResources().getDisplayMetrics().heightPixels).toString(), this.keyboardHeight).commit();
                Log.w("vk", new StringBuilder("SAVED: kbd_height").append(this.context.getResources().getDisplayMetrics().widthPixels).append("_").append(this.context.getResources().getDisplayMetrics().heightPixels).append(" = ").append(this.keyboardHeight).toString());
                Log.i("vk", new StringBuilder("ST Keyboard height = ").append(this.keyboardHeight).append(", visible = ").append(visible).toString());
            }
            
            if(visible && this.emojiPopup != null && this.emojiPopup.isShowing()){
            	showEmojiPopup(false);
            }
            
            if(!visible && this.emojiPopup != null && this.emojiPopup.isShowing()){
            	showEmojiPopup(false);
            }
        }
    }

    public void showEmojiPopup(boolean show) {
        Log.i("vk", new StringBuilder("show emoji popup ").append(show).append(", ime fullscreen=").append(((InputMethodManager) this.context.getSystemService("input_method")).isFullscreenMode()).toString());
        
        
        IntentFilter filter;
        if (mGlobal.isTablet) { /*isTablet 
            if (show) {
                if (this.emojiPopup == null) {
                    createEmojiPopup();
                }
                filter = new IntentFilter();
                filter.addAction(ACTION_HIDE_POPUP);
                if (this.showStickers) {
                    filter.addAction(Stickers.ACTION_STICKERS_UPDATED);
                }
                VKApplication.context.registerReceiver(this.receiver, filter, permission.ACCESS_DATA, null);
                this.emojiPopup.setWidth(Global.scale(350.0f));
                this.emojiPopup.setHeight(Global.scale(260.0f));
                Drawable bd = new BackgroundDrawable(this.showStickers ? -1 : -1315086);
                this.emojiPopup.setBackgroundDrawable(bd);
                this.emojiPopup.setOutsideTouchable(true);
                View anchor = this.contentView.findViewById(R.id.writebar_attach);
                this.emojiPopup.showAsDropDown(anchor, -(this.emojiPopup.getWidth() / 2 - anchor.getWidth() / 2), 0);
                int[] aloc = new int[2];
                anchor.getLocationOnScreen(aloc);
                this.emojiView.getViewTreeObserver().addOnPreDrawListener(new AnonymousClass_4(new int[2], bd, aloc, anchor));
                this.emojiPopup.setOnDismissListener(new OnDismissListener() {
                    public void onDismiss() {
                        try {
                            VKApplication.context.unregisterReceiver(EmojiPopup.this.receiver);
                        } catch (Exception e) {
                        }
                    }
                });
            } else {
                this.emojiPopup.dismiss();
            }*/
        } else if (show) {
            //filter = new IntentFilter();
            //filter.addAction(ACTION_HIDE_POPUP);
            //MyApplication.context.registerReceiver(this.receiver, filter, null, null);
            
            if (this.emojiPopup == null) {
                createEmojiPopup();
            }
            if (this.keyboardHeight <= mGlobal.scale(100.0f)) {
                this.keyboardHeight = this.context.getSharedPreferences("emoji", 0).getInt(new StringBuilder("kbd_height").append(this.context.getResources().getDisplayMetrics().widthPixels).append("_").append(this.context.getResources().getDisplayMetrics().heightPixels).toString(), mGlobal.scale(200.0f));
            }
            Log.i("vk", new StringBuilder("PP Keyboard height = ").append(this.keyboardHeight).toString());
            if (this.keyboardHeight < mGlobal.scale(200.0f)) {
                this.keyboardHeight = mGlobal.scale(200.0f);
            }
            if (this.keyboardVisible || this.keyboardHeight <= this.contentView.getHeight() / 2) {
                this.emojiPopup.setHeight(MeasureSpec.makeMeasureSpec(this.keyboardHeight, MeasureSpec.EXACTLY));
                this.emojiPopup.setWidth(MeasureSpec.makeMeasureSpec(this.context.getResources().getDisplayMetrics().widthPixels, 1073741824));
                this.emojiPopup.showAtLocation(((Activity) this.context).getWindow().getDecorView(), 83, 0, 0);
                if (!this.keyboardVisible) {
                    this.contentView.setPadding(0, 0, 0, this.keyboardHeight);
                    ((ImageView) this.contentView.findViewById(R.id.msg_keyboard)).setImageResource(R.drawable.ic_msg_panel_hide);
                } else {
                    ((ImageView) this.contentView.findViewById(R.id.msg_keyboard)).setImageResource(R.drawable.ic_msg_panel_kb);
                }
                this.emojiPopup.setOnDismissListener(new OnDismissListener() {
                    public void onDismiss() {
                        try {
                        	//MyApplication.context.unregisterReceiver(EmojiPopup.this.receiver);
                        } catch (Exception e) {
                        }
                    }
                });
            } else {
                this.keyboardHeight = this.contentView.getHeight() / 2;
                this.emojiPopup.setHeight(MeasureSpec.makeMeasureSpec(this.keyboardHeight, 1073741824));
                this.emojiPopup.setWidth(MeasureSpec.makeMeasureSpec(this.context.getResources().getDisplayMetrics().widthPixels, 1073741824));
                this.emojiPopup.showAtLocation(((Activity) this.context).getWindow().getDecorView(), 83, 0, 0);
                if (this.keyboardVisible) {
                    ((ImageView) this.contentView.findViewById(R.id.msg_keyboard)).setImageResource(R.drawable.ic_msg_panel_kb);
                } else {
                    this.contentView.setPadding(0, 0, 0, this.keyboardHeight);
                    ((ImageView) this.contentView.findViewById(R.id.msg_keyboard)).setImageResource(R.drawable.ic_msg_panel_hide);
                }
                this.emojiPopup.setOnDismissListener(new OnDismissListener() {
                    public void onDismiss() {
                    	//MyApplication.context.unregisterReceiver(EmojiPopup.this.receiver);
                    }
                });
            }
        } else {
            ((ImageView) this.contentView.findViewById(R.id.msg_keyboard)).setImageResource(this.btnRes);
            this.emojiPopup.dismiss();
            this.contentView.post(new Runnable() {
                public void run() {
                    EmojiPopup.this.contentView.setPadding(0, 0, 0, 0);
                }
            });
        }

    }
}