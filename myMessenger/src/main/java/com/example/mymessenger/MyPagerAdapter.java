package com.example.mymessenger;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.example.mymessenger.ui.ListViewSimpleFragment;
import com.example.mymessenger.ui.ServicesMenuFragment;

public class MyPagerAdapter extends FragmentPagerAdapter {
	SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
	Context context;
	
	public MyPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		this.context = context;
	}

	@Override
    public Fragment getItem(int position) {
        switch (position) {
        case 0:
            registeredFragments.put(position, new ServicesMenuFragment()); break;
        case 1:
            registeredFragments.put(position, ListViewSimpleFragment.newInstance(ListViewSimpleFragment.DIALOGS)); break;
        default:
            registeredFragments.put(position, ListViewSimpleFragment.newInstance(ListViewSimpleFragment.MESSAGES)); break;
        }
        return registeredFragments.get(position);
    }

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 3;
	}
	
	// Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
    	if(position == 2){
    		MyApplication app = ((MyApplication) ((Activity) context).getApplication()); 
    		if (!(app.getActiveService() == null) && !(app.getActiveService().getActiveDialog() == null))
    			return app.getActiveService().getActiveDialog().getDialogTitle();
    		else return "---";
    	} else if(position == 1)return "Dialogs";
    	else if(position == 0)return "Services";
    	else return "";
    }
    
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }
    
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }
    
    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
    
    @Override
    public int getItemPosition(Object object)
    {
    	Log.d("MyPagerAdapter","getItemPosition");
    	if(object == null)
    		return POSITION_NONE;
    	if (object instanceof ServicesMenuFragment)
            return ((ServicesMenuFragment) object).POSITION;
    	if (object instanceof ListViewSimpleFragment)
            return ((ListViewSimpleFragment) object).POSITION;
        return POSITION_NONE;
    }

	public void recreateFragment(int n) {
		if(n == 0){
			ServicesMenuFragment fr = (ServicesMenuFragment) getRegisteredFragment(n);
			if(fr != null)
				fr.POSITION = FragmentPagerAdapter.POSITION_NONE;
		}
		
		if(n == 1 || n == 2){
			ListViewSimpleFragment fr = (ListViewSimpleFragment) getRegisteredFragment(n);
			if(fr != null)
				fr.POSITION = FragmentPagerAdapter.POSITION_NONE;
		}
		
		notifyDataSetChanged();		
	}

	
}
