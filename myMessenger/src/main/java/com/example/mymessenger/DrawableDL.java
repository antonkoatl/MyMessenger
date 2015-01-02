package com.example.mymessenger;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;

public class DrawableDL extends Drawable {
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private DrawableDLState mDrawableState;
    Bitmap mBitmap;
    String url;
    Context context;
    boolean requested_bitmap = false;

    private static Paint placeholderPaint;
    private static Paint bitmapPaint;
    private int mBitmapWidth = -1;
    private int mBitmapHeight = -1;
    private boolean mMutated;

    DrawableDL(String url, int width, int height, Context context){
        this.url = url;
        this.context = context;
        this.mBitmapWidth = width;
        this.mBitmapHeight = height;
        mDrawableState = new DrawableDLState((Bitmap) null);
        mDrawableState.mTargetDensity = mTargetDensity;
    }

    static {
        placeholderPaint = new Paint();
        placeholderPaint.setColor(1426063360);
        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
    }

    public DrawableDL(DrawableDL drawable) {
        this.url = drawable.url;
        this.mBitmap = drawable.mBitmap;
        this.context = drawable.context;
        mDrawableState = new DrawableDLState(drawable.mDrawableState);
        mDrawableState.mTargetDensity = drawable.mTargetDensity;
    }



    @Override
    public void draw(Canvas canvas) {
        if(mBitmap == null){
            canvas.drawRect(getBounds(), placeholderPaint);
            if(url != null && !requested_bitmap) {
                download_waiter tw = new download_waiter(url) {

                    @Override
                    public void onDownloadComplete() {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inDensity = DisplayMetrics.DENSITY_LOW;
                        options.inScaled = true;
                        options.inTargetDensity = MyApplication.context.getResources().getDisplayMetrics().densityDpi;
                        Bitmap bitmap = BitmapFactory.decodeFile(filepath, options);
                        setBitmap(bitmap);
                        //DrawableDL.this.invalidateSelf();
                    }

                    public download_waiter setParams() {
                        return this;
                    }


                }.setParams();

                MyApplication app = (MyApplication) context.getApplicationContext();

                app.dl_waiters.add(tw);

                Intent intent = new Intent(context, DownloadService.class);
                intent.putExtra("url", url);
                app.startService(intent);

                requested_bitmap = true;
            }

        } else {
            Rect b = copyBounds();
            canvas.drawBitmap(this.mBitmap, null, b, bitmapPaint);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    private void computeBitmapSize() {
        mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
        mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap != mBitmap) {
            mBitmap = bitmap;
            if (bitmap != null) {
                computeBitmapSize();
            } else {
                mBitmapWidth = mBitmapHeight = -1;
            }
            invalidateSelf();
        }
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mDrawableState = new DrawableDLState(mDrawableState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mDrawableState.mChangingConfigurations;
    }

    @Override
    public final ConstantState getConstantState() {
        mDrawableState.mChangingConfigurations = getChangingConfigurations();
        return mDrawableState;
    }

    final static class DrawableDLState extends ConstantState {
        Bitmap mBitmap;
        int mChangingConfigurations;
        int mGravity = Gravity.FILL;
        Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        Shader.TileMode mTileModeX = null;
        Shader.TileMode mTileModeY = null;
        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        boolean mRebuildShader;

        DrawableDLState(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        DrawableDLState(DrawableDLState bitmapState) {
            this(bitmapState.mBitmap);
            mChangingConfigurations = bitmapState.mChangingConfigurations;
            mGravity = bitmapState.mGravity;
            mTileModeX = bitmapState.mTileModeX;
            mTileModeY = bitmapState.mTileModeY;
            mTargetDensity = bitmapState.mTargetDensity;
            mPaint = new Paint(bitmapState.mPaint);
            mRebuildShader = bitmapState.mRebuildShader;
        }

        @Override
        public Drawable newDrawable() {
            return new DrawableDL(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new DrawableDL(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }

    private DrawableDL(DrawableDLState state, Resources res) {
        mDrawableState = state;
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = state.mTargetDensity;
        }
        setBitmap(state != null ? state.mBitmap : null);
    }
}
