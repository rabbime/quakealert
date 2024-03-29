package org.jtb.quakealert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.util.Log;

public class Quakes {
	private static final String QUAKES_URL = "http://earthquake.usgs.gov/eqcenter/catalogs/eqs7day-M1.txt";
	//private static final String QUAKES_URL = "http://earthquake.usgs.gov/eqcenter/catalogs/eqs1day-M1.txt";
	// private static final String QUAKES_URL =
	// "http://192.168.0.100:8080/eqs1day-M1.txt";

	private ArrayList<Quake> quakes = null;

	public synchronized void update(long lastUpdate) {
		BufferedReader reader = null;
		String line;

		try {
			URL u = new URL(QUAKES_URL);
			HttpURLConnection uc = (HttpURLConnection) u.openConnection();
			uc.setConnectTimeout(60 * 1000); // 60 seconds
			uc.setReadTimeout(60 * 1000); // 60 seconds
			
			if (uc.getResponseCode() != 200) {
				Log.e("quakealert",
						"URL read failed, response code: "
								+ uc.getResponseCode());
				return;
			}

			reader = new BufferedReader(new InputStreamReader(
					uc.getInputStream(), "ISO-8859-1"), 8192);
			reader.readLine();

			quakes = new ArrayList<Quake>();
			while ((line = reader.readLine()) != null) {
				try {
					Quake quake = new Quake(line, lastUpdate);
					quakes.add(quake);
				} catch (Exception e) {
					Log.e("quakalert",
							"error parsing quake from line: " + line, e);
				}
				// Log.d("quakealert", "read quake: " + quake);
			}
		} catch (Throwable t) {
			Log.e("quakealert", "read failed", t);
			quakes = null;
			return;
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ioe) {
				Log.e("quakealert", "close failed", ioe);
			}
		}
	}

	public synchronized ArrayList<Quake> get() {
		return quakes;
	}
}
