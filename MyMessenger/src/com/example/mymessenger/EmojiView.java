package com.example.mymessenger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.media.TransportMediator;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.example.mymessenger.services.MessageService;
import com.example.mymessenger.ui.PagerSlidingTabStrip;
import com.example.mymessenger.ui.PagerSlidingTabStrip.IconTabProvider;

public class EmojiView extends LinearLayout {
    private static final int ITERATION_LEN = 5;
    public static long[][] data;
    private static ArrayList<Integer> permFailedPacks;
    private ArrayList<EmojiGridAdapter> adapters;
    private HorizontalScrollView bottomTabs;
    private LinearLayout btmTabsContent;
    private int currentTab;
    private LinearLayout emojiTabsWrap;
    private int[] icons;
    private Listener listener;
    private ViewPager pager;
    private OnClickListener promotedClickListener;
    private FrameLayout recentsWrap;
    private Runnable runAfterPreload;
    private boolean showStickers;
    private OnClickListener stickerClickListener;
    private LinearLayout stickerPagerDots;
    private int stickerRows;
    private int stickerSize;

    private ViewPager stickersPager;
    private int stickersPerRow;
    private boolean stickersPreloading;
    private LinearLayout stickersWrap;
    private OnClickListener storeClickListener;
    private OnClickListener tabClickListener;
    private PagerSlidingTabStrip tabs;
    public ArrayList<Integer> tempFailedPacks;
    private HashMap<Long, Float> useCounts;
    private ArrayList<GridView> views;
	private MessageService ms;
	private Context context;


    private class EmojiGridAdapter extends BaseAdapter {
        long[] data;
        Drawable[] data2;

        class ImageView1 extends ImageView {
            ImageView1(Context $anonymous0) {
                super($anonymous0);
            }

            public void onMeasure(int wms, int hms) {
                setMeasuredDimension(MeasureSpec.getSize(wms), MeasureSpec.getSize(wms));
            }
        }

        public EmojiGridAdapter(long[] d) {
            this.data = d;
            this.data2 = new Drawable[d.length];
        }

        public int getCount() {
            return this.data.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return data[position];
        }

        class RunnableGetEmojiItem implements Runnable{
        	ImageView iv;
        	int position;
        	boolean valid = true;
        	
        	RunnableGetEmojiItem(ImageView iv, int position){
        		this.iv = iv;
        		this.position = position;
        	}
        	
			@Override
			public void run() {

			    AsyncTaskCompleteListener<Drawable> cbcf = new AsyncTaskCompleteListener<Drawable>(){
					@Override
					public void onTaskComplete(Drawable result) {
						if(valid){
							iv.setImageDrawable(result);
							runners.remove(iv);
						}
					}
			    	
			    };
			    
				ChatMessageFormatter.getDownload(ms.getEmojiUrl( EmojiGridAdapter.this.data[position] ), cbcf, mGlobal.scale(30));
				
			}

			public void makeInvalid() {
				valid = false;
			}
        	
        };
        
        Map<ImageView, Runnable> runners = new HashMap<ImageView, Runnable>();
        
        
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv;
            if (convertView != null) {
                iv = (ImageView) convertView;
            } else {
                iv = new ImageView1(EmojiView.this.getContext());
                iv.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (EmojiView.this.listener != null) {
                        	EmojiView.this.listener.onEmojiSelected(EmojiView.this.convert(((Long) v.getTag()).longValue()));
                        }
                        EmojiView.this.addToRecent(((Long) v.getTag()).longValue());
                    }
                });
                iv.setBackgroundResource(R.drawable.highlight);
                iv.setScaleType(ScaleType.CENTER);
            }
            
            iv.setTag(Long.valueOf(this.data[position]));
            iv.setImageDrawable(null);
            
            if(runners.containsKey(iv)){
            	RunnableGetEmojiItem r = (RunnableGetEmojiItem) runners.get(iv);
            	r.makeInvalid();            	
            }
            
            RunnableGetEmojiItem r = new RunnableGetEmojiItem(iv, position);
            runners.put(iv, r);
            MyApplication.handler1.post(r);
            return iv;
        }
    }

    public static interface Listener {
        public void onBackspace();

        public void onEmojiSelected(String r1_String);
    }

    private static class RecentItem {
        long code;
        float count;

        private RecentItem() {
        }
    }



    private class EmojiPagesAdapter extends PagerAdapter implements IconTabProvider {
        private EmojiPagesAdapter() {
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(position == 0 ? EmojiView.this.recentsWrap : (ViewGroup) EmojiView.this.views.get(position));
        }

        public int getCount() {
            return EmojiView.this.views.size();
        }

        public int getPageIconResId(int position) {
            return EmojiView.this.icons[position];
        }

        public Object instantiateItem(ViewGroup container, int position) {
            View view;
            view = position == 0 ? EmojiView.this.recentsWrap : (ViewGroup) EmojiView.this.views.get(position);
            container.addView(view);
            return view;
        }

        public boolean isViewFromObject(View v, Object o) {
            return v == o;
        }
    }


    
    public EmojiView(Context context, MessageService ms) {
        super(context);
        this.context = context;
        this.views = new ArrayList();
        this.adapters = new ArrayList();
        this.ms = ms;

        this.icons = new int[ms.getEmojiGroupsIcons().length + 1];
        icons[0] = R.drawable.ic_emoji_recent;
        System.arraycopy(ms.getEmojiGroupsIcons(), 0, icons, 1, ms.getEmojiGroupsIcons().length);
        this.data = new long[ms.getEmojiCodes().length + 1][];
        this.data[0] = new long[]{};
        System.arraycopy(ms.getEmojiCodes(), 0, data, 1, ms.getEmojiCodes().length);
        this.useCounts = new HashMap();
        this.tabClickListener = new OnClickListener() {
            public void onClick(View v) {
                int i = 0;
                while (i < EmojiView.this.btmTabsContent.getChildCount()) {
                    View ch = EmojiView.this.btmTabsContent.getChildAt(i);
                    ch.setSelected(ch == v);
                    if (ch == v) {
                        EmojiView.this.setTab(i - 1);
                    }
                    i++;
                }
            }
        };
       
        

        this.currentTab = 0;

        init();
    }


    private void addToRecent(long c) {
        int updateCount = getContext().getSharedPreferences("emoji", 0).getInt("update_count", 0) + 1;
        //getContext().getSharedPreferences("emoji", 0).edit().putInt("update_count", updateCount).apply();
        getContext().getSharedPreferences("emoji", 0).edit().putInt("update_count", updateCount).commit();

        if (updateCount <= 0 || updateCount % 5 != 0) {
            if (!this.useCounts.containsKey(Long.valueOf(c))) {
                this.useCounts.put(Long.valueOf(c), Float.valueOf(1.0f));
            } else {
                this.useCounts.put(Long.valueOf(c), Float.valueOf(((Float) this.useCounts.get(Long.valueOf(c))).floatValue() + 1.0f));
            }
            if (this.pager.getCurrentItem() == 0) {
                updateRecents();
            }
            saveRecents();
        } else {
            Iterator r5_Iterator = this.useCounts.keySet().iterator();
            while (r5_Iterator.hasNext()) {
                long key = ((Long) r5_Iterator.next()).longValue();
                this.useCounts.put(Long.valueOf(key), Float.valueOf(((Float) this.useCounts.get(Long.valueOf(key))).floatValue() / 1.3f));
            }
            if (this.useCounts.containsKey(Long.valueOf(c))) {
                this.useCounts.put(Long.valueOf(c), Float.valueOf(((Float) this.useCounts.get(Long.valueOf(c))).floatValue() + 1.0f));
            } else {
                this.useCounts.put(Long.valueOf(c), Float.valueOf(1.0f));
            }
            if (this.pager.getCurrentItem() == 0) {
                saveRecents();
            } else {
                updateRecents();
                saveRecents();
            }
        }
    }

    private String convert(long code) {
        String s = "";
        int i = 0;
        while (i < 4) {
            int cc = (int) ((code >> ((3 - i) * 16)) & 65535);
            if (cc != 0) {
                s = new StringBuilder(String.valueOf(s)).append((char) cc).toString();
            }
            i++;
        }
        return s;
    }
    
    private String convertToHexStr(long code) {
        int i = 0; 
        while (i <= 4) {
            int cc = (int) ((code >> ((i) * 16)) & 65535);
            if (cc == 0) {
                break;
            }
            i++;
        }

        if(i == 1)return String.format("%04X", code);
        if(i == 2)return String.format("%08X", code);
        if(i == 3)return String.format("%12X", code);
        if(i == 4)return String.format("%16X", code);
        return "";
    }

    private ImageView createTabView(Drawable d, OnClickListener listener) {
        //float r4f = GalleryPickerFooterView.SIZE;
    	float r4f = 48.0f;
        ImageView v = new ImageView(getContext());
        v.setScaleType(ScaleType.CENTER);
        if (d != null) {
            v.setImageDrawable(d);
        }
        v.setLayoutParams(new LayoutParams(mGlobal.scale(r4f), mGlobal.scale(r4f)));
        v.setOnClickListener(listener);
        StateListDrawable bg = new StateListDrawable();
        int[] r2_intA = new int[1];
        r2_intA[0] = 16842913;
        bg.addState(r2_intA, new ColorDrawable(0));
        bg.addState(new int[0], new ColorDrawable(-1));
        v.setBackgroundDrawable(bg);
        return v;
    }

    private void init() {
        float r8f = 1.0f;
        setOrientation(1);
        int i = 0;
        while (i < data.length) {
            GridView gv = new GridView(getContext());
            gv.setColumnWidth(mGlobal.scale(45.0f));
            gv.setNumColumns(-1);
            EmojiGridAdapter adapter = new EmojiGridAdapter(data[i]);
            gv.setAdapter(adapter);
            this.adapters.add(adapter);
            this.views.add(gv);
            i++;
        }
        this.pager = new ViewPager(getContext());
        this.pager.setAdapter(new EmojiPagesAdapter());
        this.tabs = new PagerSlidingTabStrip(getContext());
        this.tabs.setViewPager(this.pager);
        this.tabs.setShouldExpand(true);
        this.tabs.setIndicatorColor(-9275523);
        this.tabs.setUnderlineColor(1284347553);
        this.tabs.setIndicatorHeight(mGlobal.scale(2.0f));
        this.tabs.setUnderlineHeight(mGlobal.scale(2.0f));
        this.tabs.setDividerColor(864850327);
        this.tabs.setTabBackground(0);
        setBackgroundColor(-1315086);
        this.emojiTabsWrap = new LinearLayout(getContext());
        this.emojiTabsWrap.setOrientation(0);
        this.emojiTabsWrap.addView(this.tabs, new LayoutParams(-1, -1, 1.0f));
        ImageView bsBtn = new ImageView(getContext());
        bsBtn.setImageResource(R.drawable.ic_emoji_backspace);
        bsBtn.setBackgroundResource(R.drawable.bg_emoji_bs);
        bsBtn.setScaleType(ScaleType.CENTER);
        bsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (EmojiView.this.listener != null) {
                    EmojiView.this.listener.onBackspace();
                }
            }
        });
        this.emojiTabsWrap.addView(bsBtn, new LayoutParams(mGlobal.scale(61.0f), -1));
        this.recentsWrap = new FrameLayout(getContext());
        this.recentsWrap.addView((View) this.views.get(0));
        TextView empty = new TextView(getContext());
        empty.setText("No recent");
        empty.setTextSize(18.0f);
        empty.setTextColor(-7829368);
        empty.setGravity(Gravity.CENTER);
        this.recentsWrap.addView(empty);
        ((GridView) this.views.get(0)).setEmptyView(empty);
        //addView(this.emojiTabsWrap, new LayoutParams(-1, Global.scale(GalleryPickerFooterView.SIZE)));
        addView(this.emojiTabsWrap, new LayoutParams(-1, mGlobal.scale(48.0f)));
        addView(this.pager, new LayoutParams(-1, -1, 1.0f));
        loadRecents();
        this.tabs.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int arg0) {
            }

            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            public void onPageSelected(int pos) {
                if (pos != 0) {
                    EmojiView.this.updateRecents();
                }
            }
        });
      
    }



    private void saveRecents() {
        ArrayList<String> sdata = new ArrayList();
        int i = 0;
        Iterator r7_Iterator = this.useCounts.keySet().iterator();
        while (r7_Iterator.hasNext()) {
            long key = ((Long) r7_Iterator.next()).longValue();
            if (i < 50) {
                sdata.add(this.useCounts.get(Long.valueOf(key)) + "\t" + key);
                i++;
            } else {
                break;
            }
        }
        getContext().getSharedPreferences("emoji", 0).edit().putString("recents" + String.valueOf(ms.getServiceType()), TextUtils.join(",", sdata)).commit();
    }



    private void setTab(int tab) {
        this.currentTab = tab;
        if (tab == 0) {
            this.emojiTabsWrap.setVisibility(0);
            this.pager.setVisibility(0);
            setBackgroundColor(-1315086);
        } else {
            this.emojiTabsWrap.setVisibility(8);
            this.pager.setVisibility(8);
            this.stickersWrap.setVisibility(0);

            //updateStickerPagerDots();
            
            setBackgroundColor(-1);

        }
    }



    private void updateRecents() {
        ArrayList<RecentItem> r = new ArrayList();
        Iterator r6_Iterator = this.useCounts.keySet().iterator();
        while (r6_Iterator.hasNext()) {
            long key = ((Long) r6_Iterator.next()).longValue();
            RecentItem i = new RecentItem();
            i.code = key;
            i.count = ((Float) this.useCounts.get(Long.valueOf(key))).floatValue();
            r.add(i);
        }
        Collections.sort(r, new Comparator<RecentItem>() {
            public int compare(RecentItem lhs, RecentItem rhs) {
                if (lhs.count == rhs.count) {
                    return 0;
                }
                if (lhs.count > rhs.count) {
                    return -1;
                }
                return 1;
            }
        });
        data[0] = new long[r.size()];
        int i_2 = 0;
        while (i_2 < r.size()) {
            data[0][i_2] = ((RecentItem) r.get(i_2)).code;
            i_2++;
        }
        ((EmojiGridAdapter) this.adapters.get(0)).data = data[0];
        ((EmojiGridAdapter) this.adapters.get(0)).notifyDataSetChanged();
    }



    public void loadRecents() {
        String r = getContext().getSharedPreferences("emoji", 0).getString("recents" + String.valueOf(ms.getServiceType()), "");
        if (r == null || r.length() <= 0 || (!r.contains("\t"))) {
        } else {
            List<String> sp = Arrays.asList(r.split(","));
            Collections.sort(sp);
            Collections.reverse(sp);
            data[0] = new long[sp.size()];
            this.useCounts.clear();
            int i = 0;
            Iterator r8_Iterator = sp.iterator();
            while (r8_Iterator.hasNext()) {
                String[] ss = ((String) r8_Iterator.next()).split("\t");
                long code = Long.parseLong(ss[1]);
                this.useCounts.put(Long.valueOf(code), Float.valueOf(Float.parseFloat(ss[0])));
                data[0][i] = code;
                i++;
            }
            ((EmojiGridAdapter) this.adapters.get(0)).data = data[0];
            ((EmojiGridAdapter) this.adapters.get(0)).notifyDataSetChanged();
        }
    }

    public void onMeasure(int wms, int hms) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(wms), 1073741824), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(hms), 1073741824));
    }




    public void setListener(Listener l) {
        this.listener = l;
    }

}