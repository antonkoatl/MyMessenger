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


    private class EmojiGridAdapter extends BaseAdapter {
        long[] data;

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
            iv.setImageDrawable(ChatMessageFormatter.getEmojiDrawableVk( convertToHexStr(this.data[position]), Global.scale(30) ));
            iv.setTag(Long.valueOf(this.data[position]));
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


    static {
        long[][] r0_longAA = new long[6][];
        r0_longAA[0] = new long[0];
        r0_longAA[1] = new long[]{3627933188L, 3627933187L, 3627933184L, 3627933194L, 9786, 3627933193L, 3627933197L, 3627933208L, 3627933210L, 3627933207L, 3627933209L, 3627933212L, 3627933213L, 3627933211L, 3627933235L, 3627933185L, 3627933204L, 3627933196L, 3627933202L, 3627933214L, 3627933219L, 3627933218L, 3627933186L, 3627933229L, 3627933226L, 3627933221L, 3627933232L, 3627933189L, 3627933203L, 3627933225L, 3627933227L, 3627933224L, 3627933233L, 3627933216L, 3627933217L, 3627933220L, 3627933206L, 3627933190L, 3627933195L, 3627933239L, 3627933198L, 3627933236L, 3627933237L, 3627933234L, 3627933215L, 3627933222L, 3627933223L, 3627933192L, 3627932799L, 3627933230L, 3627933228L, 3627933200L, 3627933205L, 3627933231L, 3627933238L, 3627933191L, 3627933199L, 3627933201L, 3627932786L, 3627932787L, 3627932782L, 3627932791L, 3627932802L, 3627932790L, 3627932774L, 3627932775L, 3627932776L, 3627932777L, 3627932788L, 3627932789L, 3627932785L, 3627932796L, 3627932792L, 3627933242L, 3627933240L, 3627933243L, 3627933245L, 3627933244L, 3627933248L, 3627933247L, 3627933241L, 3627933246L, 3627932793L, 3627932794L, 3627933256L, 3627933257L, 3627933258L, 3627932800L, 3627932797L, 3627932841L, 3627932965L, 10024, 3627867935L, 3627932843L, 3627932837L, 3627932834L, 3627932838L, 3627932839L, 3627932836L, 3627932840L, 3627932738L, 3627932736L, 3627932739L, 3627932741L, 3627932740L, 3627932749L, 3627932750L, 3627932748L, 3627932746L, 9994, 9996, 3627932747L, 9995, 3627932752L, 3627932742L, 3627932743L, 3627932745L, 3627932744L, 3627933260L, 3627933263L, 9757, 3627932751L, 3627932842L, 3627933366L, 3627868099L, 3627932803L, 3627932779L, 3627932778L, 3627932780L, 3627932781L, 3627932815L, 3627932817L, 3627932783L, 3627933254L, 3627933253L, 3627932801L, 3627933259L, 3627932806L, 3627932807L, 3627932805L, 3627932784L, 3627933262L, 3627933261L, 3627933255L, 3627868073L, 3627932753L, 3627932754L, 3627932767L, 3627932766L, 3627932769L, 3627932768L, 3627932770L, 3627932757L, 3627932756L, 3627932762L, 3627932759L, 3627868093L, 3627932758L, 3627932760L, 3627932761L, 3627932860L, 3627932764L, 3627932765L, 3627932763L, 3627932755L, 3627868032L, 3627867906L, 3627932804L, 3627932827L, 3627932825L, 3627932828L, 3627932826L, 10084, 3627932820L, 3627932823L, 3627932819L, 3627932821L, 3627932822L, 3627932830L, 3627932824L, 3627932812L, 3627932811L, 3627932813L, 3627932814L, 3627932772L, 3627932773L, 3627932844L, 3627932771L, 3627932845L};
        r0_longAA[2] = new long[]{3627932726L, 3627932730L, 3627932721L, 3627932717L, 3627932729L, 3627932720L, 3627932728L, 3627932719L, 3627932712L, 3627932731L, 3627932727L, 3627932733L, 3627932718L, 3627932695L, 3627932725L, 3627932690L, 3627932724L, 3627932689L, 3627932696L, 3627932732L, 3627932711L, 3627932710L, 3627932708L, 3627932709L, 3627932707L, 3627932692L, 3627932685L, 3627932706L, 3627932699L, 3627932701L, 3627932700L, 3627932702L, 3627932684L, 3627932697L, 3627932698L, 3627932704L, 3627932703L, 3627932716L, 3627932723L, 3627932683L, 3627932676L, 3627932687L, 3627932672L, 3627932675L, 3627932677L, 3627932679L, 3627932681L, 3627932686L, 3627932688L, 3627932691L, 3627932693L, 3627932694L, 3627932673L, 3627932674L, 3627932722L, 3627932705L, 3627932682L, 3627932715L, 3627932714L, 3627932678L, 3627932680L, 3627932713L, 3627932734L, 3627932816L, 3627867960L, 3627867959L, 3627867968L, 3627867961L, 3627867963L, 3627867962L, 3627867969L, 3627867971L, 3627867970L, 3627867967L, 3627867966L, 3627867972L, 3627867957L, 3627867956L, 3627867954L, 3627867955L, 3627867952L, 3627867953L, 3627867964L, 3627867920L, 3627867934L, 3627867933L, 3627867930L, 3627867921L, 3627867922L, 3627867923L, 3627867924L, 3627867925L, 3627867926L, 3627867927L, 3627867928L, 3627867932L, 3627867931L, 3627867929L, 3627867917L, 3627867918L, 3627867919L, 3627867915L, 3627867916L, 3627867936L, 11088, 9728, 9925, 9729, 9889, 9748, 10052, 9924, 3627867904L, 3627867905L, 3627867912L, 3627867914L};
        r0_longAA[3] = new long[]{3627868045L, 3627932829L, 3627868046L, 3627868050L, 3627868051L, 3627868047L, 3627868038L, 3627868039L, 3627868048L, 3627868049L, 3627868035L, 3627932795L, 3627868037L, 3627868036L, 3627868033L, 3627868043L, 3627868041L, 3627868042L, 3627868040L, 3627868044L, 3627932974L, 3627868069L, 3627932919L, 3627932921L, 3627932924L, 3627932863L, 3627932864L, 3627932861L, 3627932862L, 3627932859L, 3627932913L, 9742, 3627932894L, 3627932895L, 3627932896L, 3627932897L, 3627932922L, 3627932923L, 3627932938L, 3627932937L, 3627932936L, 3627932935L, 3627932948L, 3627932948L, 3627932898L, 3627932899L, 9203, 8987, 9200, 8986, 3627932947L, 3627932946L, 3627932943L, 3627932944L, 3627932945L, 3627932942L, 3627932833L, 3627932966L, 3627932934L, 3627932933L, 3627932940L, 3627932939L, 3627932941L, 3627933376L, 3627933375L, 3627933373L, 3627932967L, 3627932969L, 3627932968L, 3627933354L, 3627933356L, 3627932835L, 3627932971L, 3627932970L, 3627932810L, 3627932809L, 3627932848L, 3627932852L, 3627932853L, 3627932855L, 3627932854L, 3627932851L, 3627932856L, 3627932914L, 3627932903L, 3627932901L, 3627932900L, 9993, 3627932905L, 3627932904L, 3627932911L, 3627932907L, 3627932906L, 3627932908L, 3627932909L, 3627932910L, 3627932902L, 3627932893L, 3627932868L, 3627932867L, 3627932881L, 3627932874L, 3627932872L, 3627932873L, 3627932892L, 3627932875L, 3627932869L, 3627932870L, 3627932871L, 3627932865L, 3627932866L, 9986, 3627932876L, 3627932878L, 10002, 9999, 3627932879L, 3627932880L, 3627932885L, 3627932887L, 3627932888L, 3627932889L, 3627932883L, 3627932884L, 3627932882L, 3627932890L, 3627932886L, 3627932950L, 3627932891L, 3627932972L, 3627932973L, 3627932912L, 3627868072L, 3627868076L, 3627868068L, 3627868071L, 3627868092L, 3627868085L, 3627868086L, 3627868089L, 3627868091L, 3627868090L, 3627868087L, 3627868088L, 3627932798L, 3627868078L, 3627867343L, 3627868084L, 3627867140L, 3627868082L, 3627868079L, 3627868104L, 3627868096L, 9917, 9918, 3627868094L, 3627868081L, 3627868105L, 3627868083L, 9971, 3627933365L, 3627933364L, 3627868097L, 3627868103L, 3627868102L, 3627868095L, 3627868098L, 3627868106L, 3627868100L, 3627868067L, 9749, 3627868021L, 3627868022L, 3627868028L, 3627868026L, 3627868027L, 3627868024L, 3627868025L, 3627868023L, 3627868020L, 3627867989L, 3627867988L, 3627867999L, 3627867991L, 3627867990L, 3627867997L, 3627867995L, 3627868004L, 3627868017L, 3627868003L, 3627868005L, 3627867993L, 3627867992L, 3627867994L, 3627867996L, 3627868018L, 3627868002L, 3627868001L, 3627868019L, 3627867998L, 3627868009L, 3627868014L, 3627868006L, 3627868008L, 3627868007L, 3627868034L, 3627868016L, 3627868010L, 3627868011L, 3627868012L, 3627868013L, 3627868015L, 3627867982L, 3627867983L, 3627867978L, 3627867979L, 3627867986L, 3627867975L, 3627867977L, 3627867987L, 3627867985L, 3627867976L, 3627867980L, 3627867984L, 3627867981L, 3627868000L, 3627867974L, 3627867973L, 3627867965L};
        r0_longAA[4] = new long[]{3627868128L, 3627868129L, 3627868139L, 3627868130L, 3627868131L, 3627868133L, 3627868134L, 3627868138L, 3627868137L, 3627868136L, 3627932818L, 9962, 3627868140L, 3627868132L, 3627867911L, 3627867910L, 3627868143L, 3627868144L, 9978, 3627868141L, 3627933180L, 3627933182L, 3627933179L, 3627867908L, 3627867909L, 3627867907L, 3627933181L, 3627867913L, 3627868064L, 3627868065L, 9970, 3627868066L, 3627933346L, 9973, 3627933348L, 3627933347L, 9875, 3627933312L, 9992, 3627932858L, 3627933313L, 3627933314L, 3627933322L, 3627933321L, 3627933342L, 3627933318L, 3627933316L, 3627933317L, 3627933320L, 3627933319L, 3627933341L, 3627933323L, 3627933315L, 3627933326L, 3627933324L, 3627933325L, 3627933337L, 3627933336L, 3627933335L, 3627933333L, 3627933334L, 3627933339L, 3627933338L, 3627933352L, 3627933331L, 3627933332L, 3627933330L, 3627933329L, 3627933328L, 3627933362L, 3627933345L, 3627933343L, 3627933344L, 3627933340L, 3627932808L, 3627933327L, 3627868075L, 3627933350L, 3627933349L, 9888, 3627933351L, 3627932976L, 9981, 3627868142L, 3627868080L, 9832, 3627933183L, 3627868074L, 3627868077L, 3627932877L, 3627933353L, -2865171240719688203L, -2865171236424720905L, -2865171266489491990L, -2865171270784459277L, -2865171193475047944L, -2865171257899557385L, -2865171262194524680L, -2865171245014655495L, -2865171206359949830L, -2865171253604590105L};
        r0_longAA[5] = new long[]{3219683, 3285219, 3350755, 3416291, 3481827, 3547363, 3612899, 3678435, 3743971, 3154147, 3627932959L, 3627932962L, 2302179, 3627932963L, 11014, 11015, 11013, 10145, 3627932960L, 3627932961L, 3627932964L, 8599, 8598, 8600, 8601, 8596, 8597, 3627932932L, 9664, 9654, 3627932988L, 3627932989L, 8617, 8618, 8505, 9194, 9193, 9195, 9196, 10549, 10548, 3627867543L, 3627932928L, 3627932929L, 3627932930L, 3627867541L, 3627867545L, 3627867538L, 3627867539L, 3627867542L, 3627932918L, 3627868070L, 3627867649L, 3627867695L, 3627867699L, 3627867701L, 3627867698L, 3627867700L, 3627867698L, 3627867728L, 3627867705L, 3627867706L, 3627867702L, 3627867674L, 3627933371L, 3627933369L, 3627933370L, 3627933372L, 3627933374L, 3627933360L, 3627933358L, 3627867519L, 9855, 3627933357L, 3627867703L, 3627867704L, 3627867650L, 9410, 3627867729L, 12953, 12951, 3627867537L, 3627867544L, 3627867540L, 3627933355L, 3627932958L, 3627932917L, 3627933359L, 3627933361L, 3627933363L, 3627933367L, 3627933368L, 9940, 10035, 10055, 10062, 9989, 10036, 3627932831L, 3627867546L, 3627932915L, 3627932916L, 3627867504L, 3627867505L, 3627867534L, 3627867518L, 3627932832L, 10175, 9851, 9800, 9801, 9802, 9803, 9804, 9805, 9806, 9807, 9808, 9809, 9810, 9811, 9934, 3627932975L, 3627868135L, 3627932857L, 3627932850L, 3627932849L, 169, 174, 8482, 12349, 12336, 3627932957L, 3627932954L, 3627932953L, 3627932955L, 3627932956L, 10060, 11093, 10071, 10067, 10069, 10068, 3627932931L, 3627933019L, 3627933031L, 3627933008L, 3627933020L, 3627933009L, 3627933021L, 3627933010L, 3627933022L, 3627933011L, 3627933023L, 3627933012L, 3627933024L, 3627933013L, 3627933014L, 3627933015L, 3627933016L, 3627933017L, 3627933018L, 3627933025L, 3627933026L, 3627933027L, 3627933028L, 3627933029L, 3627933030L, 10006, 10133, 10134, 10135, 9824, 9829, 9827, 9830, 3627932846L, 3627932847L, 10004, 9745, 3627932952L, 3627932951L, 10160, 3627932977L, 3627932978L, 3627932979L, 9724, 9723, 9726, 9725, 9642, 9643, 3627932986L, 11036, 11035, 9899, 9898, 3627932980L, 3627932981L, 3627932987L, 3627932982L, 3627932983L, 3627932984L, 3627932985L};
        data = r0_longAA;
        permFailedPacks = new ArrayList();
    }

    public EmojiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.views = new ArrayList();
        this.adapters = new ArrayList();
        this.icons = new int[]{R.drawable.ic_emoji_recent, R.drawable.ic_emoji_smile, R.drawable.ic_emoji_flower, R.drawable.ic_emoji_bell, R.drawable.ic_emoji_car, R.drawable.ic_emoji_symbol};
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
        this.tempFailedPacks = new ArrayList();
        this.stickersPreloading = false;
        this.runAfterPreload = null;
        init();
    }

    public EmojiView(Context context, AttributeSet attrs, int defStyle) {
        //super(context, attrs, defStyle);
    	super(context, attrs);
        this.views = new ArrayList();
        this.adapters = new ArrayList();
        this.icons = new int[]{R.drawable.ic_emoji_recent, R.drawable.ic_emoji_smile, R.drawable.ic_emoji_flower, R.drawable.ic_emoji_bell, R.drawable.ic_emoji_car, R.drawable.ic_emoji_symbol};
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
        this.tempFailedPacks = new ArrayList();
        this.stickersPreloading = false;
        this.runAfterPreload = null;
        init();
    }

    public EmojiView(Context context, boolean stickers) {
        super(context);
        this.views = new ArrayList();
        this.adapters = new ArrayList();
        this.icons = new int[]{R.drawable.ic_emoji_recent, R.drawable.ic_emoji_smile, R.drawable.ic_emoji_flower, R.drawable.ic_emoji_bell, R.drawable.ic_emoji_car, R.drawable.ic_emoji_symbol};
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
        this.tempFailedPacks = new ArrayList();
        this.stickersPreloading = false;
        this.runAfterPreload = null;
        this.showStickers = stickers;
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
        v.setLayoutParams(new LayoutParams(Global.scale(r4f), Global.scale(r4f)));
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
            gv.setColumnWidth(Global.scale(45.0f));
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
        this.tabs.setIndicatorHeight(Global.scale(2.0f));
        this.tabs.setUnderlineHeight(Global.scale(2.0f));
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
        this.emojiTabsWrap.addView(bsBtn, new LayoutParams(Global.scale(61.0f), -1));
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
        addView(this.emojiTabsWrap, new LayoutParams(-1, Global.scale(48.0f)));
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
        getContext().getSharedPreferences("emoji", 0).edit().putString("recents", TextUtils.join(",", sdata)).commit();
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
        String r = getContext().getSharedPreferences("emoji", 0).getString("recents", "");
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