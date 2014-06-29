package com.example.mymessenger.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import java.util.Locale;

import com.example.mymessenger.mGlobal;
import com.example.mymessenger.R;

public class PagerSlidingTabStrip extends HorizontalScrollView {
    private static final int[] ATTRS;
    private boolean checkedTabWidths;
    private int currentPosition;
    private float currentPositionOffset;
    private android.widget.LinearLayout.LayoutParams defaultTabLayoutParams;
    public OnPageChangeListener delegatePageListener;
    private int dividerColor;
    private int dividerPadding;
    private Paint dividerPaint;
    private int dividerWidth;
    private android.widget.LinearLayout.LayoutParams expandedTabLayoutParams;
    private int indicatorColor;
    private int indicatorHeight;
    private int lastScrollX;
    private Locale locale;
    public final PageListener pageListener;
    private ViewPager pager;
    private Paint rectPaint;
    private int scrollOffset;
    private boolean shouldExpand;
    private int tabBackgroundResId;
    private int tabCount;
    private int tabPadding;
    private int tabTextColor;
    private int tabTextSize;
    private Typeface tabTypeface;
    private int tabTypefaceStyle;
    private LinearLayout tabsContainer;
    private boolean textAllCaps;
    private int underlineColor;
    private int underlineHeight;

    class AnonymousClass_2 implements OnClickListener {
        private final /* synthetic */ int val$position;

        AnonymousClass_2(int r2i) {
            this.val$position = r2i;
        }

        public void onClick(View v) {
            PagerSlidingTabStrip.this.pager.setCurrentItem(this.val$position);
        }
    }

    class AnonymousClass_3 implements OnClickListener {
        private final /* synthetic */ int val$position;

        AnonymousClass_3(int r2i) {
            this.val$position = r2i;
        }

        public void onClick(View v) {
            PagerSlidingTabStrip.this.pager.setCurrentItem(this.val$position);
        }
    }

    class AnonymousClass_4 implements OnClickListener {
        private final /* synthetic */ int val$position;

        AnonymousClass_4(int r2i) {
            this.val$position = r2i;
        }

        public void onClick(View v) {
            PagerSlidingTabStrip.this.pager.setCurrentItem(this.val$position);
        }
    }

    public static interface BadgeTabProvider {
        public String getPageBadgeValue(int r1i);
    }

    public static interface IconTabProvider {
        public int getPageIconResId(int r1i);
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR;
        int currentPosition;

        static {
            CREATOR = new Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentPosition = in.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.currentPosition);
        }
    }

    public class PageListener implements OnPageChangeListener {
        public void onPageScrollStateChanged(int state) {
            if (state == 0) {
                PagerSlidingTabStrip.this.scrollToChild(PagerSlidingTabStrip.this.pager.getCurrentItem(), 0);
            }
            if (PagerSlidingTabStrip.this.delegatePageListener != null) {
                PagerSlidingTabStrip.this.delegatePageListener.onPageScrollStateChanged(state);
            }
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            PagerSlidingTabStrip.this.currentPosition = position;
            PagerSlidingTabStrip.this.currentPositionOffset = positionOffset;
            PagerSlidingTabStrip.this.scrollToChild(position, (int) (((float) PagerSlidingTabStrip.this.tabsContainer.getChildAt(position).getWidth()) * positionOffset));
            PagerSlidingTabStrip.this.invalidate();
            if (PagerSlidingTabStrip.this.delegatePageListener != null) {
                PagerSlidingTabStrip.this.delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        public void onPageSelected(int position) {
            if (PagerSlidingTabStrip.this.delegatePageListener != null) {
                PagerSlidingTabStrip.this.delegatePageListener.onPageSelected(position);
            }
            int i = 0;
            while (i < PagerSlidingTabStrip.this.tabsContainer.getChildCount()) {
                PagerSlidingTabStrip.this.tabsContainer.getChildAt(i).setSelected(false);
                i++;
            }
            PagerSlidingTabStrip.this.tabsContainer.getChildAt(position).setSelected(true);
        }
    }

    static {
        ATTRS = new int[]{16842901, 16842904};
    }

    public PagerSlidingTabStrip(Context context) {
        this(context, null);
    }

    public PagerSlidingTabStrip(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerSlidingTabStrip(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.pageListener = new PageListener();
        this.currentPosition = 0;
        this.currentPositionOffset = 0.0f;
        this.checkedTabWidths = false;
        this.indicatorColor = -10066330;
        this.underlineColor = 436207616;
        this.dividerColor = 436207616;
        this.shouldExpand = false;
        this.textAllCaps = true;
        this.scrollOffset = 52;
        this.indicatorHeight = 8;
        this.underlineHeight = 2;
        this.dividerPadding = 12;
        this.tabPadding = 24;
        this.dividerWidth = 1;
        this.tabTextSize = 12;
        this.tabTextColor = -10066330;
        this.tabTypeface = null;
        this.tabTypefaceStyle = 1;
        this.lastScrollX = 0;
        this.tabBackgroundResId = R.drawable.background_tab;
        setFillViewport(true);
        setWillNotDraw(false);
        this.tabsContainer = new LinearLayout(context);
        this.tabsContainer.setOrientation(0);
        this.tabsContainer.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        addView(this.tabsContainer);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        this.scrollOffset = (int) TypedValue.applyDimension(1, (float) this.scrollOffset, dm);
        this.indicatorHeight = (int) TypedValue.applyDimension(1, (float) this.indicatorHeight, dm);
        this.underlineHeight = (int) TypedValue.applyDimension(1, (float) this.underlineHeight, dm);
        this.dividerPadding = (int) TypedValue.applyDimension(1, (float) this.dividerPadding, dm);
        this.tabPadding = (int) TypedValue.applyDimension(1, (float) this.tabPadding, dm);
        this.dividerWidth = (int) TypedValue.applyDimension(1, (float) this.dividerWidth, dm);
        this.tabTextSize = (int) TypedValue.applyDimension(1, (float) this.tabTextSize, dm);
        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        this.tabTextSize = a.getDimensionPixelSize(0, this.tabTextSize);
        this.tabTextColor = a.getColor(1, this.tabTextColor);
        a.recycle();
        /*a = context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip);
        this.indicatorColor = a.getColor(0, this.indicatorColor);
        this.underlineColor = a.getColor(1, this.underlineColor);
        this.dividerColor = a.getColor(BoardTopicsFragment.ORDER_CREATED_DESC, this.dividerColor);
        this.indicatorHeight = a.getDimensionPixelSize(Group.ADMIN_LEVEL_ADMIN, this.indicatorHeight);
        this.underlineHeight = a.getDimensionPixelSize(UserListView.TYPE_FAVE, this.underlineHeight);
        this.dividerPadding = a.getDimensionPixelSize(UserListView.TYPE_FOLLOWERS, this.dividerPadding);
        this.tabPadding = a.getDimensionPixelSize(UserListView.TYPE_POLL_VOTERS, this.tabPadding);
        this.tabBackgroundResId = a.getResourceId(UserListView.TYPE_BLACKLIST, this.tabBackgroundResId);
        this.shouldExpand = a.getBoolean(NewPostActivity.POLL_EDIT_RESULT, this.shouldExpand);
        this.scrollOffset = a.getDimensionPixelSize(UserListView.TYPE_FAVE_LINKS, this.scrollOffset);
        this.textAllCaps = a.getBoolean(UserListView.TYPE_USER_SUBSCRIPTIONS, this.textAllCaps);*/
        a.recycle();
        this.rectPaint = new Paint();
        this.rectPaint.setAntiAlias(true);
        this.rectPaint.setStyle(Style.FILL);
        this.dividerPaint = new Paint();
        this.dividerPaint.setAntiAlias(true);
        this.dividerPaint.setStrokeWidth((float) this.dividerWidth);
        this.defaultTabLayoutParams = new android.widget.LinearLayout.LayoutParams(-2, -1, 0.0f);
        this.expandedTabLayoutParams = new android.widget.LinearLayout.LayoutParams(0, -1, 1.0f);
        if (this.locale == null) {
            this.locale = getResources().getConfiguration().locale;
        }
    }

    private void addBadgeTextTab(int position, CharSequence title, String badge) {
        if (badge == null) {
            addTextTab(position, title);
        } else {
        	TextView tab = new TextView(getContext());
            tab.setText(title);
            tab.setFocusable(true);
            tab.setGravity(Gravity.CENTER);
            tab.setSingleLine();
            tab.setPadding(0, 0, mGlobal.scale(5.0f), 0);
            TextView bv = new TextView(getContext());
            bv.setText(badge);
            bv.setTextColor(-1);
            bv.setTextSize(13.0f);
            bv.setTypeface(Typeface.DEFAULT_BOLD);
            bv.setBackgroundResource(R.drawable.badge_tab);
            LinearLayout ll = new LinearLayout(getContext());
            ll.setOrientation(0);
            ll.setGravity(Gravity.CENTER);
            ll.addView(tab);
            ll.addView(bv);
            ll.setOnClickListener(new AnonymousClass_3(position));
            this.tabsContainer.addView(ll);
        }
    }

    private void addIconTab(int position, int resId) {
    	ImageButton tab = new ImageButton(getContext());
        tab.setFocusable(true);
        tab.setImageResource(resId);
        tab.setOnClickListener(new AnonymousClass_4(position));
        this.tabsContainer.addView(tab);
    }

    private void addTextTab(int position, CharSequence title) {
    	TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setFocusable(true);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();
        tab.setOnClickListener(new AnonymousClass_2(position));
        this.tabsContainer.addView(tab);
    }

    private void doUpdateExpanded() {
        boolean expand;
        int w = getWidth();
        int wSum = 0;
        int i = 0;
        while (i < this.tabsContainer.getChildCount()) {
            this.tabsContainer.getChildAt(i).measure(-2147483648 | w, 1073741824 | getHeight());
            wSum += this.tabsContainer.getChildAt(i).getMeasuredWidth();
            i++;
        }
        expand = Math.abs(w - wSum) < mGlobal.scale(50.0f) || this.shouldExpand;
        i = 0;
        while (i < this.tabsContainer.getChildCount()) {
            View v = this.tabsContainer.getChildAt(i);
            if (expand) {
                v.setPadding(0, 0, 0, 0);
                v.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -1, 1.0f));
            } else {
                v.setPadding(this.tabPadding, 0, this.tabPadding, 0);
                v.setLayoutParams(this.defaultTabLayoutParams);
            }
            i++;
        }
    }

    private void scrollToChild(int position, int offset) {
        if (this.tabCount == 0 || position >= this.tabsContainer.getChildCount() || position < 0) {
        } else {
            int newScrollX = this.tabsContainer.getChildAt(position).getLeft() + offset;
            if (position > 0 || offset > 0) {
                newScrollX -= this.scrollOffset;
            }
            if (newScrollX != this.lastScrollX) {
                this.lastScrollX = newScrollX;
                scrollTo(newScrollX, 0);
            }
        }
    }

    private void setStyle(TextView tab) {
        tab.setTextSize(0, (float) this.tabTextSize);
        tab.setTypeface(this.tabTypeface, this.tabTypefaceStyle);
        tab.setTextColor(this.tabTextColor);
        if (this.textAllCaps) {
            if (VERSION.SDK_INT >= 14) {
                tab.setAllCaps(true);
            } else {
                tab.setText(tab.getText().toString().toUpperCase(this.locale));
            }
        }
    }

    private void updateExpanded() {
        if (getWidth() > 0) {
            doUpdateExpanded();
        } else {
            getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                @SuppressLint({"NewApi"})
                public boolean onPreDraw() {
                    PagerSlidingTabStrip.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    PagerSlidingTabStrip.this.doUpdateExpanded();
                    return true;
                }
            });
        }
    }

    private void updateTabStyles() {
        int i = 0;
        while (i < this.tabCount) {
            View v = this.tabsContainer.getChildAt(i);
            v.setLayoutParams(this.defaultTabLayoutParams);
            v.setBackgroundResource(this.tabBackgroundResId);
            if (this.shouldExpand) {
                v.setPadding(0, 0, 0, 0);
                v.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -1, 1.0f));
            } else {
                v.setPadding(this.tabPadding, 0, this.tabPadding, 0);
            }
            if (v instanceof TextView) {
                setStyle((TextView) v);
            }
            if (v instanceof LinearLayout) {
                setStyle((TextView) ((LinearLayout) v).getChildAt(0));
            }
            i++;
        }
    }

    public int getDividerColor() {
        return this.dividerColor;
    }

    public int getDividerPadding() {
        return this.dividerPadding;
    }

    public int getIndicatorColor() {
        return this.indicatorColor;
    }

    public int getIndicatorHeight() {
        return this.indicatorHeight;
    }

    public int getScrollOffset() {
        return this.scrollOffset;
    }

    public boolean getShouldExpand() {
        return this.shouldExpand;
    }

    public int getTabBackground() {
        return this.tabBackgroundResId;
    }

    public int getTabPaddingLeftRight() {
        return this.tabPadding;
    }

    public int getTextColor() {
        return this.tabTextColor;
    }

    public int getTextSize() {
        return this.tabTextSize;
    }

    public int getUnderlineColor() {
        return this.underlineColor;
    }

    public int getUnderlineHeight() {
        return this.underlineHeight;
    }

    public boolean isTextAllCaps() {
        return this.textAllCaps;
    }

    public void notifyDataSetChanged() {
        this.tabsContainer.removeAllViews();
        this.tabCount = this.pager.getAdapter().getCount();
        int i = 0;
        while (i < this.tabCount) {
            if (this.pager.getAdapter() instanceof IconTabProvider) {
                addIconTab(i, ((IconTabProvider) this.pager.getAdapter()).getPageIconResId(i));
            } else if (this.pager.getAdapter() instanceof BadgeTabProvider) {
                addBadgeTextTab(i, this.pager.getAdapter().getPageTitle(i), ((BadgeTabProvider) this.pager.getAdapter()).getPageBadgeValue(i));
            } else {
                addTextTab(i, this.pager.getAdapter().getPageTitle(i));
            }
            i++;
        }
        updateTabStyles();
        this.checkedTabWidths = false;
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressLint({"NewApi"})
            public void onGlobalLayout() {
                if (VERSION.SDK_INT < 16) {
                    PagerSlidingTabStrip.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    PagerSlidingTabStrip.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                PagerSlidingTabStrip.this.currentPosition = PagerSlidingTabStrip.this.pager.getCurrentItem();
                PagerSlidingTabStrip.this.scrollToChild(PagerSlidingTabStrip.this.currentPosition, 0);
            }
        });
        updateExpanded();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode() || this.tabCount == 0) {
        } else {
            int height = getHeight();
            this.rectPaint.setColor(this.indicatorColor);
            View currentTab = this.tabsContainer.getChildAt(this.currentPosition);
            if (currentTab != null) {
                int i;
                float lineLeft = (float) currentTab.getLeft();
                float lineRight = (float) currentTab.getRight();
                if (this.currentPositionOffset <= 0.0f || this.currentPosition >= this.tabCount - 1) {
                    canvas.drawRect(lineLeft, (float) (height - this.indicatorHeight), lineRight, (float) height, this.rectPaint);
                    this.rectPaint.setColor(this.underlineColor);
                    canvas.drawRect(0.0f, (float) (height - this.underlineHeight), (float) this.tabsContainer.getWidth(), (float) height, this.rectPaint);
                    this.dividerPaint.setColor(this.dividerColor);
                    i = 0;
                } else {
                    View nextTab = this.tabsContainer.getChildAt(this.currentPosition + 1);
                    lineLeft = this.currentPositionOffset * ((float) nextTab.getLeft()) + (1.0f - this.currentPositionOffset) * lineLeft;
                    lineRight = this.currentPositionOffset * ((float) nextTab.getRight()) + (1.0f - this.currentPositionOffset) * lineRight;
                    canvas.drawRect(lineLeft, (float) (height - this.indicatorHeight), lineRight, (float) height, this.rectPaint);
                    this.rectPaint.setColor(this.underlineColor);
                    canvas.drawRect(0.0f, (float) (height - this.underlineHeight), (float) this.tabsContainer.getWidth(), (float) height, this.rectPaint);
                    this.dividerPaint.setColor(this.dividerColor);
                    i = 0;
                }
                while (i < this.tabCount - 1) {
                    View tab = this.tabsContainer.getChildAt(i);
                    canvas.drawLine((float) tab.getRight(), (float) this.dividerPadding, (float) tab.getRight(), (float) (height - this.dividerPadding), this.dividerPaint);
                    i++;
                }
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if ((!this.shouldExpand) || MeasureSpec.getMode(widthMeasureSpec) == 0) {
        } else {
            this.tabsContainer.measure(1073741824 | getMeasuredWidth(), heightMeasureSpec);
        }
    }

    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.currentPosition = savedState.currentPosition;
        requestLayout();
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.currentPosition = this.currentPosition;
        return savedState;
    }

    public void onSizeChanged(int w, int h, int ow, int oh) {
        if (!this.shouldExpand) {
            post(new Runnable() {
                public void run() {
                    PagerSlidingTabStrip.this.notifyDataSetChanged();
                }
            });
        }
    }

    public void setAllCaps(boolean textAllCaps) {
        this.textAllCaps = textAllCaps;
    }

    public void setDividerColor(int dividerColor) {
        this.dividerColor = dividerColor;
        invalidate();
    }

    public void setDividerColorResource(int resId) {
        this.dividerColor = getResources().getColor(resId);
        invalidate();
    }

    public void setDividerPadding(int dividerPaddingPx) {
        this.dividerPadding = dividerPaddingPx;
        invalidate();
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
        invalidate();
    }

    public void setIndicatorColorResource(int resId) {
        this.indicatorColor = getResources().getColor(resId);
        invalidate();
    }

    public void setIndicatorHeight(int indicatorLineHeightPx) {
        this.indicatorHeight = indicatorLineHeightPx;
        invalidate();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.delegatePageListener = listener;
    }

    public void setScrollOffset(int scrollOffsetPx) {
        this.scrollOffset = scrollOffsetPx;
        invalidate();
    }

    public void setShouldExpand(boolean shouldExpand) {
        this.shouldExpand = shouldExpand;
        this.tabsContainer.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        updateTabStyles();
        requestLayout();
    }

    public void setTabBackground(int resId) {
        this.tabBackgroundResId = resId;
        updateTabStyles();
    }

    public void setTabPaddingLeftRight(int paddingPx) {
        this.tabPadding = paddingPx;
        updateTabStyles();
    }

    public void setTextColor(int textColor) {
        this.tabTextColor = textColor;
        updateTabStyles();
    }

    public void setTextColorResource(int resId) {
        this.tabTextColor = getResources().getColor(resId);
        updateTabStyles();
    }

    public void setTextSize(int textSizePx) {
        this.tabTextSize = textSizePx;
        updateTabStyles();
    }

    public void setTypeface(Typeface typeface, int style) {
        this.tabTypeface = typeface;
        this.tabTypefaceStyle = style;
        updateTabStyles();
    }

    public void setUnderlineColor(int underlineColor) {
        this.underlineColor = underlineColor;
        invalidate();
    }

    public void setUnderlineColorResource(int resId) {
        this.underlineColor = getResources().getColor(resId);
        invalidate();
    }

    public void setUnderlineHeight(int underlineHeightPx) {
        this.underlineHeight = underlineHeightPx;
        invalidate();
    }

    public void setViewPager(ViewPager pager) {
        this.pager = pager;
        if (pager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        } else {
            pager.setOnPageChangeListener(this.pageListener);
            notifyDataSetChanged();
        }
    }
}