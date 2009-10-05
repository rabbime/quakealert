package org.jtb.quakealert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.maps.GeoPoint;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;

public class Quake {
	private static final Pattern QUAKE_PATTERN = Pattern
			.compile("([^,]+),([^,]+),([^,]+),\"([^\"]+) UTC\",([^,]+),([^,]+),([^,]+),([^,]+),\\s?([^,]+),\"([^\"]+)\"");
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"EEEE, MMMM d, yyyy HH:mm:ss");
	private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat(
			"EEE, h:mm a");
	private static final SimpleDateFormat LIST_DATE_FORMAT = new SimpleDateFormat(
			"EEEE, h:mm a");
	private static final int ZONE_OFFSET = Calendar.getInstance().get(
			Calendar.ZONE_OFFSET);
	private static final int DST_OFFSET = Calendar.getInstance().get(
			Calendar.DST_OFFSET);

	static class ListComparator implements Comparator<Quake> {
		private HashSet<String> newIds;

		public ListComparator(HashSet<String> newIds) {
			this.newIds = newIds;
		}

		public int compare(Quake q1, Quake q2) {
			if (newIds != null && newIds.size() > 0) {
				boolean q1New = newIds.contains(q1.getId());
				boolean q2New = newIds.contains(q2.getId());

				if (q2New && !q1New) {
					return 1;
				}
				if (q1New && !q2New) {
					return -1;
				}
			}

			return q2.getDate().compareTo(q1.getDate());
		}
	}

	private String source;
	private String id;
	private String version;
	private Date date;
	private double latitude;
	private int latitudeE6;
	private double longitude;
	private int longitudeE6;
	private float magnitude;
	private float depth;
	private int nst;
	private String region;
	private int color = 0;
	private GeoPoint geoPoint;

	public Quake(String line) {
		// line = line.replace("  ", " ");
		Matcher m = QUAKE_PATTERN.matcher(line);
		if (!m.matches()) {
			return;
		}

		try {
			source = m.group(1);
			id = m.group(2);
			version = m.group(3);
			date = parseDate(m.group(4));
			latitude = Double.parseDouble(m.group(5));
			longitude = Double.parseDouble(m.group(6));
			magnitude = Float.parseFloat(m.group(7));
			depth = Float.parseFloat(m.group(8));
			nst = Integer.parseInt(m.group(9));
			region = m.group(10);
		} catch (NumberFormatException nfe) {
			// TODO: android log
			return;
		}

		latitudeE6 = (int) (latitude * Math.pow(10, 6));
		longitudeE6 = (int) (longitude * Math.pow(10, 6));

		if (magnitude < 2) {
			color = Color.parseColor("#00ff00");
		} else if (magnitude < 3) {
			color = Color.parseColor("#ccff66");
		} else if (magnitude < 4) {
			color = Color.parseColor("#ffff33");
		} else if (magnitude < 5) {
			color = Color.parseColor("#ffcc00");
		} else if (magnitude < 6) {
			color = Color.parseColor("#ff9900");
		} else {
			color = Color.parseColor("#ff3300");
		}

		geoPoint = new GeoPoint(latitudeE6, longitudeE6);
	}

	private static Date parseDate(String s) {
		try {
			// Log.d(Quake.class.getSimpleName(), "s: + " + s);
			Date d = DATE_FORMAT.parse(s);
			// Log.d(Quake.class.getSimpleName(), "d: + " + getDateString(d));
			d = new Date(d.getTime() + ZONE_OFFSET + DST_OFFSET);
			// Log.d(Quake.class.getSimpleName(), "d: + " + getDateString(d));

			return d;
		} catch (ParseException pe) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "{ source=" + source + ", id=" + id + ", version=" + version
				+ ", date=" + getListDateString() + ", latitude=" + latitude
				+ ",longitude=" + longitude + ", magnitude=" + magnitude
				+ ", depth=" + depth + ", nst=" + nst + ", region=" + region
				+ " }";
	}

	public String getListDateString() {
		return getListDateString(date);
	}

	public String getShortDateString() {
		return getShortDateString(date);
	}

	public static String getListDateString(Date d) {
		return LIST_DATE_FORMAT.format(d);
	}

	public static String getShortDateString(Date d) {
		return SHORT_DATE_FORMAT.format(d);
	}

	public boolean matches(float magnitude, int range, Location location) {
		if (this.magnitude < magnitude) {
			return false;
		}
		if (range > 0) {
			float distance = getDistance(location);
			// Log.d(getClass().getSimpleName(), "range: " + range +
			// ", distance: "
			// + distance);
			if (distance > range) {
				return false;
			}
		}

		return true;
	}

	public float getDistance(Location location) {
		Location quakeLocation = new Location(location);
		quakeLocation.setLatitude(latitude);
		quakeLocation.setLongitude(longitude);
		float distance = location.distanceTo(quakeLocation);
		return distance;

	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Quake)) {
			return false;
		}
		Quake other = (Quake) o;
		return id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public Date getDate() {
		return date;
	}

	public float getMagnitude() {
		return magnitude;
	}

	public String getRegion() {
		return region;
	}

	public int getColor() {
		return color;
	}

	public String getId() {
		return id;
	}

	public int getLatitudeE6() {
		return latitudeE6;
	}

	public int getLongitudeE6() {
		return longitudeE6;
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}
	
	public float getDepth() {
		return depth;
	}
}