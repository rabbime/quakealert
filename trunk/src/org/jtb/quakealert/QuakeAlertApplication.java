package org.jtb.quakealert;

import android.app.Application;
import android.os.StrictMode;

public class QuakeAlertApplication extends Application {
	private static final boolean DEBUG = false;

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectAll().penaltyLog().penaltyDeath().build());
		}
	}

}
