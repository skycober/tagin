package ca.idrc.tagin.cloud;

import java.io.IOException;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tagin.model.URN;
import com.google.api.services.tagin.model.URNCollection;

import ca.idrc.tagin.cloud.tag.Tag;
import ca.idrc.tagin.cloud.util.TagMap;
import ca.idrc.tagin.lib.TaginManager;
import ca.idrc.tagin.lib.TaginService;
import ca.idrc.tagin.lib.tags.GetLabelTask;
import ca.idrc.tagin.lib.tags.GetLabelTaskListener;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;

public class LauncherActivity extends Activity implements GetLabelTaskListener {

	public final static String EXTRA_TAGS = "ca.idrc.tagin.cloud.EXTRA_TAGS";
	private final String MAX_NEIGHBOURS = "10";
	private Integer mNeighboursCounter;
	private String mInitialURN;
	
	private LauncherActivity mInstance;
	private TagMap mTagMap;
	private TaginManager mTaginManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		mInstance = this;
		mTaginManager = new TaginManager(this);
		mTagMap = new TagMap();
		mTaginManager.apiRequest(TaginService.REQUEST_URN);
	}
	
	public void startCloud() {
		Intent intent = new Intent(LauncherActivity.this, CloudActivity.class);
		intent.putExtra(EXTRA_TAGS, mTagMap);
		startActivity(intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        registerReceiver(mReceiver, new IntentFilter(TaginService.ACTION_URN_READY));
        registerReceiver(mReceiver, new IntentFilter(TaginService.ACTION_NEIGHBOURS_READY));
	}
	
	@Override
	public void onGetLabelTaskComplete(String urn, String label) {
		if (urn.equals(mInitialURN)) {
			Tag tag = new Tag(urn, label, 20);
			mTagMap.put(urn, tag);
		} else {
			synchronized(mNeighboursCounter) {
				mNeighboursCounter--;
				if (label != null) {
					Tag tag = new Tag(urn, label, 20);
					mTagMap.put(urn, tag);
				}
				if (mNeighboursCounter == 0) {
					startCloud();
				}
			}
		}
	}
	
	public void handleNeighboursReady(String result) {
		URNCollection urns = null;
		if (result != null) {
			try {
				urns = new GsonFactory().fromString(result, URNCollection.class);
			} catch (IOException e) {
				Log.e("tagin", "Deserialization error: " + e.getMessage());
			}
		}
		
		if (urns != null && urns.getItems() != null && urns.getItems().size() > 0) {
			mNeighboursCounter = urns.getItems().size();
			for (URN urn : urns.getItems()) {
				GetLabelTask<LauncherActivity> task = new GetLabelTask<LauncherActivity>(this, urn.getValue());
				task.execute();
			}
		} else {
			Log.d("tagin", "No neighbours found");
			startCloud();
		}
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(TaginService.ACTION_URN_READY)) {
				String urn = intent.getStringExtra(TaginService.EXTRA_QUERY_RESULT);
				if (urn != null) {
					mTaginManager.apiRequest(TaginService.REQUEST_NEIGHBOURS, urn, MAX_NEIGHBOURS);
					mInitialURN = urn;
					GetLabelTask<LauncherActivity> task = new GetLabelTask<LauncherActivity>(mInstance, urn);
					task.execute();
				} else {
					Log.d("tagin", "Could not submit fingerprint");
					// TODO show error dialog
					startCloud();
				}
			} else if (intent.getAction().equals(TaginService.ACTION_NEIGHBOURS_READY)) {
				String result = intent.getStringExtra(TaginService.EXTRA_QUERY_RESULT);
				handleNeighboursReady(result);
			}
		}
	};

}
