package org.jtb.quakealert;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ListQuakesActivity extends Activity {
	private static final int REFRESH_MENU = 0;
	private static final int PREFS_MENU = 1;

	private static final int PREFS_REQUEST = 0;

	static final int REFRESH_DIALOG = 0;
	static final int LOCATION_ERROR_DIALOG = 1;
	static final int NETWORK_ERROR_DIALOG = 2;

	private AlertDialog mServiceWarnDialog;
	private ProgressDialog mRefreshDialog;
	private AlertDialog mLocationErrorDialog;
	private AlertDialog mNetworkErrorDialog;

	private ListView mList;
	private LinearLayout mNoQuakesLayout;
	private ListReceiver listUpdateReceiver;
	private ListQuakesActivity mThis;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);

		mThis = this;
		listUpdateReceiver = new ListReceiver(this);

		upgrade();

		mNoQuakesLayout = (LinearLayout) findViewById(R.id.no_quakes_layout);
		mList = (ListView) findViewById(R.id.list);
		mList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Intent i = new Intent(mThis, QuakeMapActivity.class);
				i.putExtra("org.jtb.quakealert.quake.position", new Integer(
						position));
				mThis.startActivity(i);
			}
		});

		QuakePrefs qp = new QuakePrefs(this);
		if (qp.isNotificationsEnabled()) {
			sendBroadcast(new Intent("schedule", null, this,
					QuakeRefreshReceiver.class));
		}
		sendBroadcast(new Intent("refresh", null, this,
				QuakeRefreshReceiver.class));

		WarnDialog.Builder builder = new WarnDialog.Builder(this,
				"serviceWarn", R.string.service_warn);
		if (builder.isWarn()) {
			mServiceWarnDialog = builder.create();
			mServiceWarnDialog.show();
		}
		
		Uri alertSoundUri = qp.getNotificationAlertSound();
		if (alertSoundUri == null) {
			Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			qp.setNotificationAlertSound(defaultUri);
		}
		
		Log.d(getClass().getSimpleName(), "created");
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(listUpdateReceiver);
		Log.d(getClass().getSimpleName(), "paused");
	}

	@Override
	public void onResume() {
		super.onResume();

		updateList();

		registerReceiver(listUpdateReceiver, new IntentFilter("updateList"));
		registerReceiver(listUpdateReceiver, new IntentFilter(
				"showRefreshDialog"));
		registerReceiver(listUpdateReceiver, new IntentFilter(
				"dismissRefreshDialog"));
		registerReceiver(listUpdateReceiver, new IntentFilter(
				"showLocationErrorDialog"));
		registerReceiver(listUpdateReceiver, new IntentFilter(
				"showNetworkErrorDialog"));
		
		Log.d(getClass().getSimpleName(), "resumed");
	}

	public void updateList() {
		if (QuakeRefreshService.matchQuakes != null
				&& QuakeRefreshService.matchQuakes.size() > 0) {
			QuakeAdapter qa = new QuakeAdapter(this,
					QuakeRefreshService.matchQuakes,
					QuakeRefreshService.location);
			mNoQuakesLayout.setVisibility(View.GONE);
			mList.setVisibility(View.VISIBLE);
			mList.setAdapter(qa);
			
			new QuakePrefs(this).clearNewIds();
			Log.d(getClass().getSimpleName(), "updated list (visible)");
		} else {
			mNoQuakesLayout.setVisibility(View.VISIBLE);
			mList.setVisibility(View.GONE);
			Log.d(getClass().getSimpleName(), "updated list (gone)");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH_MENU, 0, R.string.refresh_menu).setIcon(
				R.drawable.refresh);
		menu.add(0, PREFS_MENU, 1, R.string.prefs_menu).setIcon(
				R.drawable.prefs);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH_MENU:
			sendBroadcast(new Intent("refresh", null, this,
					QuakeRefreshReceiver.class));
			return true;
		case PREFS_MENU:
			Intent prefsActivity = new Intent(this, PrefsActivity.class);
			startActivityForResult(prefsActivity, PREFS_REQUEST);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case PREFS_REQUEST:
			if (resultCode == PrefsActivity.CHANGED_RESULT) {
				sendBroadcast(new Intent("refresh", null, this,
						QuakeRefreshReceiver.class));
			}
			break;
		}
	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case REFRESH_DIALOG:
			mRefreshDialog = new ProgressDialog(this);
			mRefreshDialog.setMessage("Refreshing, please wait.");
			mRefreshDialog.setIndeterminate(true);
			mRefreshDialog.setCancelable(false);
			return mRefreshDialog;
		case LOCATION_ERROR_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Unknown Location");
			builder
					.setMessage("Ensure that you have at least one location source enabled in Settings > Security & location.");
			builder.setNeutralButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dismissDialog(LOCATION_ERROR_DIALOG);
						}
					});
			mLocationErrorDialog = builder.create();
			return mLocationErrorDialog;
		case NETWORK_ERROR_DIALOG:
			builder = new AlertDialog.Builder(this);
			builder.setTitle("Network Error");
			builder
					.setMessage("Could not retrieve recent quakes. Ensure that you have a network connection (mobile or wifi).");
			builder.setNeutralButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dismissDialog(NETWORK_ERROR_DIALOG);
						}
					});
			mNetworkErrorDialog = builder.create();
			return mNetworkErrorDialog;
		}
		return null;
	}

	private void upgrade() {
		PackageManager manager = getPackageManager();
		PackageInfo info;
		try {
			info = manager.getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e(getClass().getSimpleName(), "could not get version", e);
			return;
		}

		QuakePrefs qp = new QuakePrefs(this);
		if (!qp.isUpgradedTo(4)) {
			// upgrade range from km to m
			int km = qp.getRange();
			if (km > 0) {
				qp.setRange(km * 1000);
			}
		}
		if (!qp.isUpgradedTo(16)) {
			// upgrade interval
			Interval i = null;
			long interval = qp.getLong("interval", 0);
			if (interval < 60 * 60 * 1000) {
				// was set to 5 or 10 mins
				i = Interval.FIFTEEN_MINUTES;
			} else {
				// was set to 1 or 3 hours
				i = Interval.HOUR;
			}
			qp.setInterval(i);
		}

		qp.setUpgradedTo(info.versionCode);
	}
}
