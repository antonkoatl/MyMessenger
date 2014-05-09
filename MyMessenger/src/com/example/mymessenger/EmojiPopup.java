package com.example.mymessenger;

import com.example.mymessenger.EmojiView.Listener;
import com.example.mymessenger.SoftKeyboardStateHelper.SoftKeyboardStateListener;

import android.Manifest.permission;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

public class EmojiPopup {
    public static final String ACTION_HIDE_POPUP = "com.vkontakte.andoroid.HIDE_EMOJI_POPUP";
    private int btnRes;
    private View contentView;
    private Context context;
    private PopupWindow emojiPopup;
    private EmojiView emojiView;
    private int keyboardHeight;
    private boolean keyboardVisible;
    private BroadcastReceiver receiver;
    private boolean showStickers;
	private Listener stickerListener;

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
	
    private static class BackgroundDrawable extends Drawable {
        private static final int ARROW_SIZE;
        private static final int PADDING;
        private int arrowX;
        private Bitmap bitmap;
        private Paint paint;

        static {
            PADDING = Global.scale(5.0f);
            ARROW_SIZE = Global.scale(7.0f);
        }

        public BackgroundDrawable(int color) {
            this.bitmap = null;
            this.arrowX = 200;
            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setShadowLayer((float) Global.scale(4.0f), 0.0f, (float) Global.scale(1.0f), 1426063360);
            this.paint.setAntiAlias(true);
        }

        public void draw(Canvas canvas) {
            Rect rect = copyBounds();
            if (this.bitmap != null && rect.width() == this.bitmap.getWidth() && rect.height() == this.bitmap.getHeight()) {
                canvas.drawBitmap(this.bitmap, new Rect(0, 0, rect.width(), PADDING), new Rect(rect.left, rect.top, rect.right, rect.top + PADDING), this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(0, rect.height() - PADDING - ARROW_SIZE, rect.width(), rect.height()), new Rect(rect.left, rect.bottom - PADDING - ARROW_SIZE, rect.right, rect.bottom), this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(0, PADDING, PADDING, rect.bottom - PADDING - ARROW_SIZE), new Rect(rect.left, rect.top + PADDING, rect.left + PADDING, rect.bottom - PADDING - ARROW_SIZE), this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(rect.width() - PADDING, PADDING, rect.width(), rect.bottom - PADDING - ARROW_SIZE), new Rect(rect.right - PADDING, rect.top + PADDING, rect.right, rect.bottom - PADDING - ARROW_SIZE), this.paint);
            } else {
                this.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
                Canvas c = new Canvas(this.bitmap);
                Rect r = new Rect(rect);
                r.offsetTo(0, 0);
                r.inset(PADDING, PADDING);
                r.bottom -= ARROW_SIZE;
                Path path = new Path();
                path.addRect(new RectF(r), Direction.CW);
                path.moveTo((float) (this.arrowX - ARROW_SIZE), (float) r.bottom);
                path.lineTo((float) this.arrowX, (float) (r.bottom + ARROW_SIZE));
                path.lineTo((float) (this.arrowX + ARROW_SIZE), (float) r.bottom);
                path.close();
                c.drawPath(path, this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(0, 0, rect.width(), PADDING), new Rect(rect.left, rect.top, rect.right, rect.top + PADDING), this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(0, rect.height() - PADDING - ARROW_SIZE, rect.width(), rect.height()), new Rect(rect.left, rect.bottom - PADDING - ARROW_SIZE, rect.right, rect.bottom), this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(0, PADDING, PADDING, rect.bottom - PADDING - ARROW_SIZE), new Rect(rect.left, rect.top + PADDING, rect.left + PADDING, rect.bottom - PADDING - ARROW_SIZE), this.paint);
                canvas.drawBitmap(this.bitmap, new Rect(rect.width() - PADDING, PADDING, rect.width(), rect.bottom - PADDING - ARROW_SIZE), new Rect(rect.right - PADDING, rect.top + PADDING, rect.right, rect.bottom - PADDING - ARROW_SIZE), this.paint);
            }
        }

        public int getOpacity() {
            return -3;
        }

        public boolean getPadding(Rect out) {
            out.set(PADDING, PADDING, PADDING, PADDING + ARROW_SIZE);
            return true;
        }

        public void setAlpha(int alpha) {
        }

        public void setArrowX(int x) {
            this.arrowX = Global.scale(5.0f) + x;
            this.bitmap = null;
        }

        public void setColorFilter(ColorFilter cf) {
        }
    }

 

    public EmojiPopup(Context c, View content, int icon, boolean stickers) {
        this.receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (ACTION_HIDE_POPUP.equals(intent.getAction())) {
                    EmojiPopup.this.hide();
                }

            }
        };
        this.context = c;
        this.contentView = content;
        this.btnRes = icon;
        this.showStickers = stickers;
        
        SoftKeyboardStateHelper softKeyboardStateHelper = new SoftKeyboardStateHelper(content);
        softKeyboardStateHelper.addSoftKeyboardStateListener(sListener);
    }

    private void createEmojiPopup() {
        this.emojiView = new EmojiView(this.context, this.showStickers);
        this.emojiView.setListener(new EmojiView.Listener() {
            public void onBackspace() {
                ((EditText) EmojiPopup.this.contentView.findViewById(R.id.msg_entertext)).dispatchKeyEvent(new KeyEvent(0, 67));
            }

            public void onEmojiSelected(String s) {
                EditText edit = (EditText) EmojiPopup.this.contentView.findViewById(R.id.msg_entertext);
                int pos = edit.getSelectionEnd();
                CharSequence em = ChatMessageFormatter.getSmiledText(context, s, 0);
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
                MyApplication.context.unregisterReceiver(this.receiver);
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
        if (Global.isTablet) {
        } else {
            this.keyboardVisible = visible;
            this.keyboardHeight = h;
            if (this.keyboardHeight <= Global.scale(100.0f) || (!visible)) {
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
            
            /*
            if (visible && this.contentView.getPaddingBottom() == 0 && this.emojiPopup != null && this.emojiPopup.isShowing()) {
                this.emojiPopup.setHeight(h);
                this.emojiPopup.dismiss();
                this.emojiPopup.showAtLocation(((Activity) this.context).getWindow().getDecorView(), 83, 0, 0);
                if ((!visible) || this.contentView.getPaddingBottom() <= 0) {
                    if (visible || this.emojiPopup == null || (!this.emojiPopup.isShowing())) {
                    } else {
                        showEmojiPopup(false);
                    }
                } else {
                    showEmojiPopup(false);
                    if (visible || this.emojiPopup == null || this.emojiPopup.isShowing()) {
                    } else {
                        showEmojiPopup(false);
                    }
                }
            } else if (visible || this.contentView.getPaddingBottom() <= 0) {
                if (visible || this.emojiPopup == null || this.emojiPopup.isShowing()) {
                } else {
                    showEmojiPopup(false);
                }
            } else {
                showEmojiPopup(false);
                if (visible || this.emojiPopup == null || this.emojiPopup.isShowing()) {
                } else {
                    showEmojiPopup(false);
                }
            }*/
        }
    }

    public void setEmojiClickListener(EmojiView.Listener l) {
        this.stickerListener = l;
    }

    public void showEmojiPopup(boolean show) {
        Log.i("vk", new StringBuilder("show emoji popup ").append(show).append(", ime fullscreen=").append(((InputMethodManager) this.context.getSystemService("input_method")).isFullscreenMode()).toString());
        
        
        IntentFilter filter;
        if (Global.isTablet) { /*isTablet 
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
            filter = new IntentFilter();
            filter.addAction(ACTION_HIDE_POPUP);

            MyApplication.context.registerReceiver(this.receiver, filter, null, null);
            
            if (this.emojiPopup == null) {
                createEmojiPopup();
            }
            if (this.keyboardHeight <= Global.scale(100.0f)) {
                this.keyboardHeight = this.context.getSharedPreferences("emoji", 0).getInt(new StringBuilder("kbd_height").append(this.context.getResources().getDisplayMetrics().widthPixels).append("_").append(this.context.getResources().getDisplayMetrics().heightPixels).toString(), Global.scale(200.0f));
            }
            Log.i("vk", new StringBuilder("PP Keyboard height = ").append(this.keyboardHeight).toString());
            if (this.keyboardHeight < Global.scale(200.0f)) {
                this.keyboardHeight = Global.scale(200.0f);
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
                        	MyApplication.context.unregisterReceiver(EmojiPopup.this.receiver);
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
                    	MyApplication.context.unregisterReceiver(EmojiPopup.this.receiver);
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