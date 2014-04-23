package com.example.mymessenger;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

public class SelectServiceActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.select_service);

		GridView gridview = (GridView) findViewById(R.id.selectservice_grid);
	    gridview.setAdapter(new SelectServiceAdapter(this));
	    gridview.setNumColumns(GridView.AUTO_FIT);
	    gridview.setColumnWidth(128);
	    //gridview.setVerticalSpacing(8);
	    //gridview.setHorizontalSpacing(8);
	    gridview.setStretchMode(GridView.NO_STRETCH);
	    
	    gridview.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	            Toast.makeText(SelectServiceActivity.this, "" + position, Toast.LENGTH_SHORT).show();
	        }
	    });
	}

}
